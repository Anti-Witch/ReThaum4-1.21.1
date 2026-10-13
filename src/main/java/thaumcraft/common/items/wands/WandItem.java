package thaumcraft.common.items.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.wands.IWandable;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.utils.NBTHelper;
import thaumcraft.init.ModDataComponents;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;

/**
 * WandItem - The TC4-style wand: a rod + cap combination item that stores
 * vis internally and casts installed foci.
 *
 * The wand stores its rod/cap tags, its installed focus ItemStack and its
 * per-aspect vis reserves as Data Components on the stack:
 * - {@code wand_rod} / {@code wand_cap}: string tags (written by the wand
 *   recipes), with legacy NBT fallback for old stacks.
 * - {@code wand_focus}: serialized focus ItemStack.
 * - {@code wand_vis}: aspect tag -> float reserves.
 *
 * Right-clicking an {@link IWandable} block or tile forwards the interaction
 * to it; otherwise the block is checked against the {@link WandTriggerRegistry}
 * and finally the installed focus is cast (TC4 behaviour). Right-clicking air
 * casts the focus directly. Vis capacity per aspect is the rod's
 * {@link WandRod#getCapacity()}; base costs are reduced by the cap's
 * {@link WandCap#getBaseCostModifier()} (special aspects use
 * {@link WandCap#getSpecialCostModifier()}). A wand is charged by draining vis
 * from an aura node it is used on (right-click and hold, see
 * {@link IWandable#onUsingWandTick}) and by rod regeneration
 * ({@link WandRod#getOnUpdate()}).
 *
 * NOTE: This class has no counterpart in the reference 1.20.1 project (which
 * uses a FocusEngine/caster system). It is part of the TC4-style wand system
 * this port implements; casting reuses the existing FocusEngine pipeline via
 * {@link ItemFocus} (same as the Caster Gauntlet).
 */
public class WandItem extends Item implements ICaster {

    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.#");
    private static final String TAG_ROD = "rod";
    private static final String TAG_CAP = "cap";
    private static final String TAG_VIS = "vis";
    private static final String TAG_FOCUS = "focus";

    public WandItem(Properties properties) {
        super(properties);
    }

    // ==================== Factory ====================

    /**
     * Create a wand with the given rod and cap tags (for creative tab / commands).
     */
    public static ItemStack createWand(String rodTag, String capTag) {
        ItemStack stack = new ItemStack(thaumcraft.init.ModItems.WAND.get());
        setRod(stack, rodTag);
        setCap(stack, capTag);
        return stack;
    }

    // ==================== Rod / Cap storage ====================

    /**
     * Set the rod tag on this wand stack.
     */
    public static void setRod(ItemStack stack, String rodTag) {
        stack.set(ModDataComponents.WAND_ROD.get(), rodTag);
    }

    /**
     * Set the cap tag on this wand stack.
     */
    public static void setCap(ItemStack stack, String capTag) {
        stack.set(ModDataComponents.WAND_CAP.get(), capTag);
    }

    /**
     * Get the rod definition for this wand stack, or null if none set.
     * Reads the string Data Component first (written by the wand recipes),
     * falling back to the legacy NBT tag for stacks created before the
     * Data Component migration.
     */
    @Nullable
    public static WandRod getRod(ItemStack stack) {
        String tag = stack.get(ModDataComponents.WAND_ROD.get());
        if ((tag == null || tag.isEmpty()) && NBTHelper.hasTag(stack)) {
            tag = NBTHelper.getTag(stack).getString(TAG_ROD);
        }
        // TC4 ItemWandCasting.getRod: stacks without a rod tag (creative tab,
        // /give) fall back to the wooden rod instead of being inert.
        if (tag == null || tag.isEmpty()) {
            return WandRod.rods.get("wood");
        }
        return WandRod.rods.get(tag);
    }

    /**
     * Get the cap definition for this wand stack, or null if none set.
     */
    @Nullable
    public static WandCap getCap(ItemStack stack) {
        String tag = stack.get(ModDataComponents.WAND_CAP.get());
        if ((tag == null || tag.isEmpty()) && NBTHelper.hasTag(stack)) {
            tag = NBTHelper.getTag(stack).getString(TAG_CAP);
        }
        // TC4 ItemWandCasting.getCap: default to the iron cap.
        if (tag == null || tag.isEmpty()) {
            return WandCap.caps.get("iron");
        }
        return WandCap.caps.get(tag);
    }

    // ==================== ICaster: focus slot ====================

    @Override
    @Nullable
    public Item getFocus(ItemStack stack) {
        ItemStack focus = getFocusStack(stack);
        return (focus == null || focus.isEmpty()) ? null : focus.getItem();
    }

    @Override
    @Nullable
    public ItemStack getFocusStack(ItemStack stack) {
        CompoundTag tag = stack.get(ModDataComponents.WAND_FOCUS.get());
        if (tag != null && !tag.isEmpty()) {
            return ItemStack.parseOptional(registryAccess(), tag);
        }
        return null;
    }

    @Override
    public void setFocus(ItemStack stack, ItemStack focus) {
        if (focus == null || focus.isEmpty()) {
            stack.remove(ModDataComponents.WAND_FOCUS.get());
        } else {
            stack.set(ModDataComponents.WAND_FOCUS.get(), (CompoundTag) focus.saveOptional(registryAccess()));
        }
    }

    @Override
    public ItemStack getPickedBlock(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    /**
     * Obtain a registry lookup provider for (de)serializing embedded item
     * stacks. On a dedicated/server instance the server registry access is
     * used; on the client the synced connection registry access is used.
     */
    private static net.minecraft.core.HolderLookup.Provider registryAccess() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        return net.minecraft.client.Minecraft.getInstance().getConnection().registryAccess();
    }

    // ==================== ICaster: vis ====================

    /**
     * TC4 CalculateWandConsumption: the cap's base modifier REPLACES 1.0 (iron
     * caps make crafting more expensive, void caps cheaper). Sceptres get an
     * extra -0.1. The result is clamped to a minimum of 0.1.
     */
    @Override
    public float getConsumptionModifier(ItemStack stack, Player player, boolean crafting) {
        return getConsumptionModifier(stack, (Aspect) null, crafting);
    }

    /**
     * Per-aspect TC4 consumption modifier: caps may define special aspects
     * (copper: order/entropy, silver: the four elementals) whose cost uses the
     * special modifier instead of the base one.
     */
    public float getConsumptionModifier(ItemStack stack, @Nullable Aspect aspect, boolean crafting) {
        float modifier = 1.0f;
        WandCap cap = getCap(stack);
        if (cap != null) {
            if (aspect != null && cap.getSpecialCostModifierAspects() != null
                    && cap.getSpecialCostModifierAspects().contains(aspect)) {
                modifier = cap.getSpecialCostModifier();
            } else {
                modifier = cap.getBaseCostModifier();
            }
        }
        if (isSceptre(stack)) {
            modifier -= 0.1f;
        }
        return Math.max(modifier, 0.1f);
    }

    /** TC4: sceptre flag (ArcaneSceptreRecipe). */
    public static boolean isSceptre(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.WAND_SCEPTRE.get()));
    }

    /**
     * Get the total stored vis on this wand stack (sum of all aspect pools).
     */
    public float getVis(ItemStack stack) {
        CompoundTag vis = stack.get(ModDataComponents.WAND_VIS.get());
        if (vis == null) {
            return 0.0f;
        }
        float total = 0.0f;
        for (String key : vis.getAllKeys()) {
            total += vis.getFloat(key);
        }
        return total;
    }

    /**
     * Get the stored vis for a single aspect on this wand stack.
     */
    public float getVis(ItemStack stack, Aspect aspect) {
        CompoundTag vis = stack.get(ModDataComponents.WAND_VIS.get());
        if (vis == null) {
            return 0.0f;
        }
        return vis.getFloat(aspect.getTag());
    }

    /**
     * Set the stored vis for a single aspect on this wand stack.
     */
    public void storeVis(ItemStack stack, Aspect aspect, float amount) {
        stack.update(ModDataComponents.WAND_VIS.get(), new CompoundTag(), vis -> {
            CompoundTag copy = vis.copy();
            if (amount <= 0.0f) {
                copy.remove(aspect.getTag());
            } else {
                copy.putFloat(aspect.getTag(), amount);
            }
            return copy;
        });
    }

    /**
     * TC4 ItemWandCasting.addVis: store up to {@code amount} vis of a primal
     * aspect on the wand, clamped to {@link #getMaxVis(ItemStack)}.
     * <p>
     * Compound aspects are never stored (TC4 wands only hold the six primal
     * reserves), matching TC4's {@code if (!aspect.isPrimal()) return 0;}.
     *
     * @return the amount that did <b>not</b> fit (0 when everything was stored)
     */
    public float addVis(ItemStack stack, Aspect aspect, float amount, boolean simulate) {
        if (aspect == null || !aspect.isPrimal() || amount <= 0.0f) {
            return 0.0f;
        }
        float max = getMaxVis(stack);
        float have = getVis(stack, aspect);
        float stored = Math.min(amount, Math.max(0.0f, max - have));
        if (stored > 0.0f && !simulate) {
            storeVis(stack, aspect, have + stored);
        }
        return amount - stored;
    }

    /**
     * TC4 InventoryUtils.isWandInHotbarWithRoom: returns the hotbar slot of the
     * first wand that has room for the given aspect, or -1 when there is none.
     * Used by aspect orbs to pick which wand to charge.
     */
    public static int findWandInHotbarWithRoom(Player player, Aspect aspect, float amount) {
        if (player == null || aspect == null || amount <= 0.0f) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WandItem wand && wand.addVis(stack, aspect, amount, true) < amount) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * TC4 getMaxVis: the cap capacity is the rod's capacity PER primal aspect
     * (staff rods carry their larger capacity in the rod definition). Sceptres
     * get +50%.
     */
    public float getMaxVis(ItemStack stack) {
        WandRod rod = getRod(stack);
        if (rod == null) {
            return 0.0f;
        }
        float capacity = rod.getCapacity();
        if (isSceptre(stack)) {
            capacity *= 1.5f;
        }
        return capacity;
    }

    /**
     * Total capacity across all six primal aspects (HUD/tooltip display).
     */
    public float getMaxVisTotal(ItemStack stack) {
        return getMaxVis(stack) * Aspect.getPrimalAspects().size();
    }

    @Override
    public boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate) {
        amount *= getConsumptionModifier(stack, player, crafting);
        float stored = getVis(stack);
        if (stored < amount) {
            return false;
        }
        if (!simulate) {
            // Draw proportionally from all stored aspect pools
            float remaining = amount;
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                if (remaining <= 0.0f) break;
                float have = getVis(stack, aspect);
                if (have > 0.0f) {
                    float take = Math.min(have, remaining);
                    storeVis(stack, aspect, have - take);
                    remaining -= take;
                }
            }
        }
        return true;
    }

    /**
     * TC4-style crafting vis consumption: consume a per-aspect cost from the
     * wand (e.g. the Arcane Workbench vis cost). Returns true if every aspect
     * of the cost is available on the wand, and (unless {@code simulate})
     * deducts the amounts.
     */
    public boolean consumeAllVisCrafting(ItemStack stack, Player player, AspectList cost, boolean simulate) {
        if (cost == null || cost.size() == 0) {
            return true;
        }
        for (Aspect aspect : cost.getAspects()) {
            float modifier = getConsumptionModifier(stack, aspect, true);
            float required = cost.getAmount(aspect) * modifier;
            if (getVis(stack, aspect) < required) {
                return false;
            }
        }
        if (!simulate) {
            for (Aspect aspect : cost.getAspects()) {
                float modifier = getConsumptionModifier(stack, aspect, true);
                float required = cost.getAmount(aspect) * modifier;
                storeVis(stack, aspect, Math.max(0.0f, getVis(stack, aspect) - required));
            }
        }
        return true;
    }

    /**
     * Get the FocusPackage of the focus currently installed in this wand.
     */
    @Nullable
    private FocusPackage getInstalledPackage(ItemStack stack) {
        ItemStack focus = getFocusStack(stack);
        if (focus == null || focus.isEmpty()) {
            return null;
        }
        if (!(focus.getItem() instanceof ItemFocus)) {
            return null;
        }
        return ItemFocus.getPackage(focus);
    }

    // ==================== Interaction ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1. IWandable block/tile handling (e.g. TileNode)
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IWandable wandable) {
            int result = wandable.onWandRightClick(level, stack, player,
                    pos.getX(), pos.getY(), pos.getZ(), side.get3DDataValue(), 0);
            if (result >= 0) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // TC4 ItemWandCasting.onItemRightClick: an IWandable tile that did not
            // claim the click (e.g. an aura node) becomes the wand's "object in
            // use", so its vis is drained into the wand while the button is held
            // (see onUseTick / TileNode.onUsingWandTick).
            setUseTarget(stack, pos);
            player.startUsingItem(context.getHand());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. Registered wand triggers (e.g. jarring nodes)
        if (WandTriggerRegistry.hasTrigger(block)) {
            if (WandTriggerRegistry.performTrigger(level, stack, player, pos, side, block, state)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // 3. No wand interaction: cast the installed focus (TC4 behaviour)
        if (castFocus(level, player, stack, context.getHand())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (castFocus(level, player, stack, hand)) {
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Attempt to cast the focus installed in this wand.
     * Mirrors the Caster Gauntlet pipeline (ItemCaster#use).
     */
    private boolean castFocus(Level level, Player player, ItemStack stack, InteractionHand hand) {
        ItemStack focusStack = getFocusStack(stack);
        if (focusStack == null || focusStack.isEmpty()) {
            return false;
        }
        if (!(focusStack.getItem() instanceof ItemFocus focus)) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return true;
        }

        float visCost = focus.getVisCost(focusStack);
        if (!consumeVis(stack, player, visCost, false, false)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("thaumcraft.wand.novis").withStyle(ChatFormatting.AQUA), true);
            }
            return true;
        }

        if (!level.isClientSide()) {
            FocusPackage focusPackage = ItemFocus.getPackage(focusStack);
            if (focusPackage != null) {
                FocusEngine.castFocusPackage(player, focusPackage);
            }
        }

        player.swing(hand);
        int cooldown = focus.getActivationTime(focusStack);
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(this, cooldown);
        }
        return true;
    }

    // ==================== Node interaction (vis draining) ====================

    /** Stack tag holding the position of the IWandable the wand is being used on. */
    private static final String TAG_USE_TARGET = "tcWandTarget";

    /**
     * TC4 ItemWandCasting.onUsingTick: while the player holds the use button on
     * an IWandable tile (aura node) the wand drains vis from it every 5 ticks
     * ({@link IWandable#onUsingWandTick}). TC4 aborts the use as soon as the
     * player looks away from the target.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!(entity instanceof Player player)) {
            return;
        }
        BlockPos target = getUseTarget(stack);
        if (target == null) {
            return;
        }
        // TC4 TileNode.onUsingWandTick: stop using the item when the node is no
        // longer the block the player is looking at.
        net.minecraft.world.phys.HitResult hit =
                Item.getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
                || !blockHit.getBlockPos().equals(target)) {
            player.stopUsingItem();
            return;
        }
        if (level.getBlockEntity(target) instanceof IWandable wandable) {
            wandable.onUsingWandTick(stack, player, count);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        BlockPos target = getUseTarget(stack);
        clearUseTarget(stack);
        if (target != null && entity instanceof Player player
                && level.getBlockEntity(target) instanceof IWandable wandable) {
            wandable.onWandStoppedUsing(stack, level, player, timeLeft);
        }
    }

    private static void setUseTarget(ItemStack stack, BlockPos pos) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY, data -> {
                    CompoundTag tag = data.copyTag();
                    tag.putLong(TAG_USE_TARGET, pos.asLong());
                    return net.minecraft.world.item.component.CustomData.of(tag);
                });
    }

    @Nullable
    private static BlockPos getUseTarget(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_USE_TARGET, net.minecraft.nbt.Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(TAG_USE_TARGET)) : null;
    }

    private static void clearUseTarget(ItemStack stack) {
        if (stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) == null) {
            return;
        }
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY, data -> {
                    CompoundTag tag = data.copyTag();
                    tag.remove(TAG_USE_TARGET);
                    return net.minecraft.world.item.component.CustomData.of(tag);
                });
    }

    // ==================== Aura recharge ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // TC4 ItemWandCasting.onUpdate: the rod's onUpdate behaviour runs while
        // the wand is in the player's inventory (primal rods slowly regenerate
        // their aspect, the primal staff a random primal; see WandRodPrimalOnUpdate).
        WandRod rod = getRod(stack);
        if (rod != null && rod.getOnUpdate() != null) {
            rod.getOnUpdate().onUpdate(stack, player);
        }
    }

    // ==================== Misc ====================

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }


    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WandRod rod = getRod(stack);
        WandCap cap = getCap(stack);
        if (rod != null) {
            tooltip.add(Component.translatable("thaumcraft.wand.rod", rod.getTag())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        if (cap != null) {
            tooltip.add(Component.translatable("thaumcraft.wand.cap", cap.getTag())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        ItemStack focus = getFocusStack(stack);
        if (focus != null && !focus.isEmpty()) {
            tooltip.add(Component.translatable("thaumcraft.wand.focus", focus.getHoverName())
                    .withStyle(ChatFormatting.AQUA));
        }
        CompoundTag vis = stack.get(ModDataComponents.WAND_VIS.get());
        if (vis != null && !vis.isEmpty()) {
            for (String key : vis.getAllKeys()) {
                float amount = vis.getFloat(key);
                if (amount > 0.0f) {
                    tooltip.add(Component.literal(" " + VIS_FORMAT.format(amount) + " "
                                    + Aspect.getAspect(key).getName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    /**
     * Legacy NBT helper kept for reference; vis is now stored in the
     * {@code wand_vis} Data Component.
     */
    @SuppressWarnings("unused")
    private static void legacyMigrate(ItemStack stack) {
        if (NBTHelper.hasTag(stack)) {
            CompoundTag nbt = NBTHelper.getTag(stack);
            if (nbt.contains(TAG_VIS, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                stack.set(ModDataComponents.WAND_VIS.get(), nbt.getCompound(TAG_VIS).copy());
                nbt.remove(TAG_VIS);
            }
        }
    }
}
