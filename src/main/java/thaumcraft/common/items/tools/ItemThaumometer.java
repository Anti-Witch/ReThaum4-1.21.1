package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.items.ItemTC;
import thaumcraft.common.lib.research.ScanGeneric;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.init.ModSounds;

/**
 * Thaumometer - the basic scanning tool of Thaumcraft (TC4 ItemThaumometer).
 * <p>
 * TC4 behaviour (1:1):
 * - Right-click starts a scan: the initially aimed target is remembered
 *   ({@code startScan}); the item goes into 25-tick use.
 * - Every tick while using, the aimed target is re-checked. If it changes, the
 *   scan is cancelled. When the counter runs out (remaining &lt;= 5) the scan
 *   completes; a camera-tick sound plays every 2 ticks.
 * - Scanned object aspects are learned only once per object type (the server
 *   side dedupes via ScanGeneric); the found aspects are drawn on the
 *   thaumometer's own screen in first person (ThaumometerItemRenderer).
 *
 * @author Azanor
 */
public class ItemThaumometer extends ItemTC {

    /** TC4: EntityUtils.getPointedEntity(p, 0.5F, 10.0F, ...) / block reach. */
    public static final double SCAN_RANGE = 10.0;

    /** TC4: startScan - target acquired on right-click, cancellation anchor (client). */
    private ScanTarget startScan = null;

    /**
     * Port: per-player server-side scan state (the item instance is shared, so
     * this cannot be a plain field like the client-only TC4 {@code startScan}).
     */
    private static final class ServerScan {
        ScanTarget target;
        boolean done;
    }

    private final java.util.Map<Player, ServerScan> serverScans =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public ItemThaumometer() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // TC4 getMaxItemUseDuration = 25
        return 25;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // TC4 getItemUseAction = none
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // TC4: onItemRightClick -> doScan; the item always goes into use, the
            // runes effect plays when a valid target was found.
            ScanTarget scan = doScan(level, player);
            this.startScan = scan;
            if (scan != null) {
                playRunes(level, player, scan);
            }
        } else {
            // Port: remember the target so the completion can verify it did not change.
            ServerScan state = new ServerScan();
            state.target = doScan(level, player);
            this.serverScans.put(player, state);
        }
        // Vanilla/TC4: startUsingItem must run on BOTH sides, otherwise the
        // server never ticks the item (onUseTick) and the scan is never applied.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!(living instanceof Player player)) {
            return;
        }

        if (level.isClientSide()) {
            // TC4 onUsingTick: only the client scans; the target must stay the same
            ScanTarget scan = doScan(level, player);
            if (scan != null && scan.equals(startScan)) {
                if (remaining <= 5) {
                    this.startScan = null;
                    player.stopUsingItem();
                    // The scan result is drawn on the thaumometer screen itself
                    // (ThaumometerItemRenderer, TC4 ItemThaumometerRenderer style).
                }
                if (remaining % 2 == 0) {
                    // TC4: "thaumcraft:cameraticks" at 0.45 + rand * 0.1
                    level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            ModSounds.CAMERATICKS.get(), SoundSource.PLAYERS,
                            0.2F, 0.45F + level.random.nextFloat() * 0.1F, false);
                }
            } else {
                this.startScan = null;
            }
        } else {
            // TC4 sends a completion packet from the client; the port scans on the
            // server once the hold completes. The target must not have changed.
            ServerScan state = this.serverScans.get(player);
            if (state != null && !state.done && remaining <= 5 && remaining > 0) {
                ScanTarget scan = doScan(level, player);
                if (scan != null && scan.equals(state.target)) {
                    state.done = true;
                    doServerScan(level, player, scan);
                }
            }
        }
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        // TC4 onPlayerStoppedUsing
        this.startScan = null;
        if (!entity.level().isClientSide() && entity instanceof Player player) {
            ServerScan state = this.serverScans.remove(player);
            // Fallback for laggy clients: the client stops using at remaining <= 5,
            // so a release with count <= 5 means the hold completed.
            if (state != null && !state.done && count > 0 && count <= 5) {
                ScanTarget scan = doScan(entity.level(), player);
                if (scan != null && scan.equals(state.target)) {
                    state.done = true;
                    doServerScan(entity.level(), player, scan);
                }
            }
        }
    }

    // ==================== Scanning ====================

    /**
     * A remembered scan target (TC4 ScanResult equivalent). Public so the
     * client-side item renderer can draw the live target info on the lens
     * (TC4 ItemThaumometerRenderer.doScan).
     */
    public static final class ScanTarget {
        public final Entity entity;
        public final BlockPos pos;
        public final String name;
        public final AspectList aspects;

        public ScanTarget(Entity entity, BlockPos pos, String name, AspectList aspects) {
            this.entity = entity;
            this.pos = pos;
            this.name = name;
            this.aspects = aspects;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ScanTarget o)) return false;
            if (this.entity != null || o.entity != null) {
                return this.entity == o.entity;
            }
            return this.pos != null && this.pos.equals(o.pos);
        }

        @Override
        public int hashCode() {
            return entity != null ? System.identityHashCode(entity) : (pos != null ? pos.hashCode() : 0);
        }
    }

    /**
     * Client-side scan: find the pointed target and remember its aspects.
     * Mirrors TC4 ItemThaumometer.doScan.
     */
    public static ScanTarget doScan(Level level, Player player) {
        // Try entities first (TC4: EntityUtils.getPointedEntity with 0.5 padding)
        Entity targetEntity = getTargetEntity(level, player, SCAN_RANGE);
        if (targetEntity != null) {
            AspectList aspects = AspectHelper.getEntityAspects(targetEntity);
            return new ScanTarget(targetEntity, null, targetEntity.getDisplayName().getString(), aspects);
        }

        // Then blocks
        BlockHitResult blockHit = getTargetBlock(level, player, SCAN_RANGE);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileNode node) {
                String tn = node.getNodeType() != null ? node.getNodeType().name().toLowerCase() : "normal";
                String first = tn.isEmpty() ? "N" : Character.toUpperCase(tn.charAt(0)) + tn.substring(1);
                return new ScanTarget(null, pos, first + " Node", node.getAspects());
            }
            BlockState state = level.getBlockState(pos);
            ItemStack blockItem = state.getBlock().asItem().getDefaultInstance();
            if (!blockItem.isEmpty()) {
                return new ScanTarget(null, pos, blockItem.getHoverName().getString(),
                        AspectHelper.getObjectAspects(blockItem));
            }
        }

        return null;
    }

    /**
     * TC4: Thaumcraft.proxy.blockRunes at the scan target. Different positions
     * for entities (eye height) and blocks (+0.25).
     */
    private void playRunes(Level level, Player player, ScanTarget scan) {
        float r = 0.3F + level.random.nextFloat() * 0.7F;
        float b = 0.3F + level.random.nextFloat() * 0.7F;
        if (scan.entity != null) {
            thaumcraft.client.fx.FXDispatcher.INSTANCE.blockRunes(
                    scan.entity.getX() - 0.5D,
                    scan.entity.getY() + scan.entity.getEyeHeight() / 2.0D,
                    scan.entity.getZ() - 0.5D,
                    r, 0.0F, b, (int) (scan.entity.getBbHeight() * 15.0F), 0.03F);
        } else if (scan.pos != null) {
            thaumcraft.client.fx.FXDispatcher.INSTANCE.blockRunes(
                    scan.pos.getX(), scan.pos.getY() + 0.25D, scan.pos.getZ(),
                    r, 0.0F, b, 15, 0.03F);
        }
    }

    /**
     * Server-side scan: apply the knowledge once the hold completes.
     */
    public static void doServerScan(Level level, Player player, ScanTarget scan) {
        if (scan == null) {
            ScanningManager.scanTheThing(player, (BlockPos) null);
            return;
        }
        if (scan.entity != null) {
            ScanningManager.scanTheThing(player, scan.entity);
            return;
        }
        if (scan.pos != null) {
            // Give the block's item stack for generic scans; tile entities (nodes)
            // are handled through their own scan keys by the registry.
            BlockEntity be = level.getBlockEntity(scan.pos);
            if (be instanceof TileNode) {
                ScanningManager.scanTheThing(player, scan.pos);
            } else {
                ItemStack blockItem = level.getBlockState(scan.pos).getBlock().asItem().getDefaultInstance();
                if (!blockItem.isEmpty()) {
                    ScanningManager.scanTheThing(player, blockItem);
                    if (ScanGeneric.getScanKey(player, blockItem) == null) {
                        ScanningManager.scanTheThing(player, scan.pos);
                    }
                }
            }
            return;
        }
        ScanningManager.scanTheThing(player, (BlockPos) null);
    }

    /**
     * Get the entity the player is looking at (TC4 EntityUtils.getPointedEntity).
     */
    public static Entity getTargetEntity(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(range));

        var aabb = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
        var entities = level.getEntities(player, aabb, e -> e != player && e.isPickable());

        Entity closest = null;
        double closestDist = range;

        for (Entity entity : entities) {
            var entityAABB = entity.getBoundingBox().inflate(entity.getPickRadius());
            var optional = entityAABB.clip(eyePos, targetPos);
            if (optional.isPresent()) {
                double dist = eyePos.distanceTo(optional.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }

    /**
     * Get the block the player is looking at.
     */
    public static BlockHitResult getTargetBlock(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(range));

        return level.clip(new ClipContext(eyePos, targetPos,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        // TC4 has no tooltip lines for the thaumometer
    }
}
