package thaumcraft.common.golems;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ItemGolemPlacer - Item used to spawn Thaumcraft golems.
 * 
 * Stores golem properties (material, head, arms, legs, addon) in NBT
 * and spawns a configured golem when used on a block.
 * 
 * Also stores XP for smart golems that have been picked up.
 */
public class ItemGolemPlacer extends Item implements ISealDisplayer {

    public ItemGolemPlacer() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        // Check if clicked block is solid
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.isSolid()) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Get spawn position (offset by clicked face)
        BlockPos spawnPos = pos.relative(side);

        // Check if player can edit at this position
        if (!player.mayUseItemAt(spawnPos, side, stack)) {
            return InteractionResult.FAIL;
        }

        // Spawn the golem
        ServerLevel serverLevel = (ServerLevel) level;
        EntityThaumcraftGolem golem = ModEntities.THAUMCRAFT_GOLEM.get().create(serverLevel);
        
        if (golem != null) {
            // Position the golem
            golem.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0f, 0.0f);
            
            // Mark as valid spawn (prevents instant despawn)
            golem.setValidSpawn();
            
            // Set ownership
            golem.setOwned(true);
            golem.setOwnerUUID(player.getUUID());
            
            // Load properties from NBT
            if (NBTHelper.hasTag(stack) && NBTHelper.getTag(stack).contains("props")) {
                IGolemProperties props = GolemProperties.fromLong(NBTHelper.getTag(stack).getLong("props"));
                golem.setProperties(props);
            }
            
            // Load XP for smart golems
            if (NBTHelper.hasTag(stack) && NBTHelper.getTag(stack).contains("xp")) {
                golem.setRankXp(NBTHelper.getTag(stack).getInt("xp"));
            }
            
            // Set home position
            golem.restrictTo(spawnPos, 32);
            
            // Finalize spawn
            golem.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.SPAWN_EGG, null);
            
            // Add to world
            if (serverLevel.addFreshEntity(golem)) {
                // Consume item if not in creative
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.FAIL;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        if (NBTHelper.hasTag(stack) && NBTHelper.getTag(stack).contains("props")) {
            IGolemProperties props = GolemProperties.fromLong(NBTHelper.getTag(stack).getLong("props"));
            
            // Show rank for smart golems
            if (props.hasTrait(EnumGolemTrait.SMART)) {
                int rank = props.getRank();
                if (rank >= 10) {
                    tooltip.add(Component.translatable("golem.rank")
                            .append(" " + rank)
                            .withStyle(ChatFormatting.GOLD));
                } else {
                    int xp = NBTHelper.getTag(stack).contains("xp") ? NBTHelper.getTag(stack).getInt("xp") : 0;
                    int xpNeeded = (rank + 1) * (rank + 1) * EntityThaumcraftGolem.XP_MULTIPLIER;
                    tooltip.add(Component.translatable("golem.rank")
                            .append(" " + rank + " ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal("(" + xp + "/" + xpNeeded + ")")
                                    .withStyle(ChatFormatting.DARK_GREEN)));
                }
            }
            
            // Show material
            tooltip.add(props.getMaterial().getLocalizedName().copy()
                    .withStyle(ChatFormatting.GREEN));
            
            // Show traits
            for (EnumGolemTrait trait : props.getTraits()) {
                tooltip.add(Component.literal("-").append(trait.getLocalizedName())
                        .withStyle(ChatFormatting.BLUE));
            }
        }
        
        super.appendHoverText(stack, ctx, tooltip, flag);
    }

    /**
     * Create an ItemStack for a golem with the given properties.
     */
    public static ItemStack createGolemStack(Item item, IGolemProperties props, int xp) {
        ItemStack stack = new ItemStack(item);
        NBTHelper.putLong(stack, "props", props.toLong());
        if (xp > 0) {
            NBTHelper.putInt(stack, "xp", xp);
        }
        return stack;
    }

    /**
     * Create a default golem ItemStack (wooden golem, basic parts).
     */
    public static ItemStack createDefaultStack(Item item) {
        return createGolemStack(item, new GolemProperties(), 0);
    }
}




