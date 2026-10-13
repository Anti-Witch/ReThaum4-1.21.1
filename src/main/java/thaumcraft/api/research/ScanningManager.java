package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.events.PlayerEvents;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the scanning system for the Thaumometer.
 * Register scannable things here and use scanTheThing to process scans.
 */
public class ScanningManager {

    private static final List<IScanThing> things = new ArrayList<>();

    /**
     * Add things to scan.
     * Example:
     * <pre>
     * ScanningManager.addScannableThing(new ScanItem("HIPSTER", new ItemStack(Items.APPLE)));
     * </pre>
     * This will unlock the HIPSTER research if you scan any kind of apple.
     *
     * @param obj the scannable thing to add
     */
    public static void addScannableThing(IScanThing obj) {
        things.add(obj);
    }

    /**
     * Remove a scannable thing.
     * @param obj the scannable thing to remove
     * @return true if it was removed
     */
    public static boolean removeScannableThing(IScanThing obj) {
        return things.remove(obj);
    }

    /**
     * Process a scan attempt.
     *
     * @param player the player scanning
     * @param object the object being scanned (Entity, BlockPos, ItemStack, or null)
     */
    public static void scanTheThing(Player player, Object object) {
        boolean found = false;
        boolean suppress = false;

        for (IScanThing thing : things) {
            if (thing.checkThing(player, object)) {
                String researchKey = thing.getResearchKey(player, object);

                if (researchKey == null || researchKey.isEmpty() || progressResearch(player, researchKey)) {
                    if (researchKey == null || researchKey.isEmpty()) {
                        suppress = true;
                    }
                    found = true;
                    thing.onSuccess(player, object);
                }
            }
        }

        if (!suppress) {
            if (!found) {
                player.displayClientMessage(
                    Component.literal("§5§o")
                        .append(Component.translatable("tc.unknownobject")),
                    true);
            } else {
                player.displayClientMessage(
                    Component.literal("§a§o")
                        .append(Component.translatable("tc.knownobject")),
                    true);
            }
        }

        // Scan contents of inventories
        if (object instanceof BlockPos pos) {
            Level level = player.level();
            level.getBlockEntity(pos);

            // Try to get item handler capability
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
                if (handler != null) {
                    int scanned = 0;
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot);
                        if (!stack.isEmpty()) {
                            scanTheThing(player, stack);
                            scanned++;
                        }
                        if (scanned >= 100) {
                            player.displayClientMessage(
                                Component.literal("§5§o")
                                    .append(Component.translatable("tc.invtoolarge")),
                                true);
                            break; // Prevent lag with massive inventories
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if an object can be scanned for research the player hasn't discovered yet.
     *
     * @param player the player
     * @param object the object to check
     * @return true if the object can be scanned for new research
     */
    public static boolean isThingStillScannable(Player player, Object object) {
        for (IScanThing thing : things) {
            if (thing.checkThing(player, object)) {
                try {
                    String key = thing.getResearchKey(player, object);
                    if (!ThaumcraftCapabilities.isResearchKnown(player, key)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Attempts to extract an ItemStack from scan parameters.
     *
     * @param player the player
     * @param obj the object (ItemStack, ItemEntity, or BlockPos)
     * @return the extracted ItemStack, or ItemStack.EMPTY
     */
    public static ItemStack getItemFromParams(Player player, Object obj) {
        ItemStack result = ItemStack.EMPTY;

        if (obj instanceof ItemStack stack) {
            result = stack;
        } else if (obj instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            result = itemEntity.getItem();
        } else if (obj instanceof BlockPos pos) {
            Level level = player.level();
            BlockState state = level.getBlockState(pos);

            // Try to get the item form of the block
            result = state.getBlock().getCloneItemStack(level, pos, state);

            // Handle water and lava (can't be registered as regular item stacks)
            if (result.isEmpty()) {
                var fluidState = level.getFluidState(pos);
                if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) {
                    result = new ItemStack(Items.WATER_BUCKET);
                } else if (fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)) {
                    result = new ItemStack(Items.LAVA_BUCKET);
                }
            }
        }

        return result;
    }

    /**
     * Performs a ray trace from the player's eye position.
     */
    private static BlockHitResult rayTrace(Player player) {
        Vec3 eyePos = player.getEyePosition(0);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.x * 4, lookVec.y * 4, lookVec.z * 4);

        return player.level().clip(new ClipContext(
            eyePos, endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.ANY,
            player
        ));
    }

    /**
     * Progress research for a player.
     * Calls ResearchManager to properly handle research progression and sync.
     *
     * @param player the player
     * @param researchKey the research key to progress
     * @return true if the research was progressed
     */
    private static boolean progressResearch(Player player, String researchKey) {
        if (player.level().isClientSide) {
            return false;
        }

        boolean success = ResearchManager.progressResearch(player, researchKey, true);

        if (success && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Mark for sync so client gets updated research data
            PlayerEvents.markForSync(serverPlayer);
        }

        return success;
    }

    /**
     * Check if a player knows a specific research.
     * Convenience method that wraps the capability check.
     */
    public static boolean knowsResearch(Player player, String researchKey) {
        return ThaumcraftCapabilities.isResearchKnown(player, researchKey);
    }

    /**
     * Get the number of registered scannable things.
     */
    public static int getScannableCount() {
        return things.size();
    }
}
