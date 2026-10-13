package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Axe of the Stream - Water elemental axe.
 * When held and used, draws nearby items towards the player.
 * Has built-in Burrowing and Collector infusion enchantments.
 */
public class ItemElementalAxe extends AxeItem {

    public ItemElementalAxe() {
        super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 8.0f, -3.0f))
                .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int remainingUseDuration) {
        // Find all nearby item entities within 10 blocks
        AABB searchBox = player.getBoundingBox().inflate(10.0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox);

        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isRemoved()) {
                // Calculate direction from item to player
                double dx = itemEntity.getX() - player.getX();
                double dy = itemEntity.getY() - (player.getY() + player.getBbHeight() / 2.0);
                double dz = itemEntity.getZ() - player.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > 0.1) {
                    // Normalize and apply attraction force
                    dx /= distance;
                    dy /= distance;
                    dz /= distance;

                    double pullStrength = 0.3;

                    // Pull item towards player
                    Vec3 motion = itemEntity.getDeltaMovement();
                    double newX = motion.x - dx * pullStrength;
                    double newY = motion.y - dy * pullStrength + 0.1;
                    double newZ = motion.z - dz * pullStrength;

                    // Clamp velocity
                    newX = Mth.clamp(newX, -0.25, 0.25);
                    newY = Mth.clamp(newY, -0.25, 0.25);
                    newZ = Mth.clamp(newZ, -0.25, 0.25);

                    itemEntity.setDeltaMovement(newX, newY, newZ);
                }
            }
        }

        // TODO: Add particle effects on client side
        // TODO: Add sound effects periodically
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.thaumcraft.burrowing").withStyle(style -> style.withColor(0x4169E1)));
        tooltip.add(Component.translatable("enchantment.thaumcraft.collector").withStyle(style -> style.withColor(0x4169E1)));
        tooltip.add(Component.translatable("item.thaumcraft.elemental_axe.desc").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
