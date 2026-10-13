package thaumcraft.common.items.tools;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

/**
 * Void Metal Axe - Powerful but warping axe that applies weakness and self-repairs.
 */
public class ItemVoidAxe extends AxeItem implements IWarpingGear {

    public ItemVoidAxe() {
        super(ThaumcraftMaterials.TOOLMAT_VOID, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, 5.0F, -3.0F))
                .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Self-repair: repair 1 durability every second (20 ticks)
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply weakness effect on hit
        if (!attacker.level().isClientSide()) {
            if (!(target instanceof Player) || isPvPEnabled(attacker.level())) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private boolean isPvPEnabled(Level level) {
        if (level.getServer() != null) {
            return level.getServer().isPvpAllowed();
        }
        return true;
    }

    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
