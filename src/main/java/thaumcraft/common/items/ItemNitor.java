package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.init.ModBlocks;

/**
 * Nitor (TC4 ItemResource damage 1): the alchemical light source.
 *
 * In TC4 using the nitor item placed the nitor block on the clicked surface.
 * The port registered a plain ItemMaterial which could not be placed at all,
 * so the nitor "did not stick to blocks".
 *
 * The colour variants are separate blocks in this port; the plain item places
 * the white one (the TC4 default recipe result).
 */
public class ItemNitor extends Item {

    public ItemNitor() {
        super(new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().relative(face);
        var player = context.getPlayer();
        var stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, placePos)
                || !player.mayUseItemAt(placePos, face, stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        BlockState state = ModBlocks.NITOR_WHITE.get().defaultBlockState();
        level.setBlockAndUpdate(placePos, state);
        level.playSound(null, placePos, state.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
