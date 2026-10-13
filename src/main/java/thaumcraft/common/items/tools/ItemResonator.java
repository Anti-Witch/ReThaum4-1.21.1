package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * Resonator - A diagnostic tool for essentia transport systems.
 * Right-click on essentia pipes/containers to see their contents and suction.
 */
public class ItemResonator extends Item {

    public ItemResonator() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction side = context.getClickedFace();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if (tile == null || !(tile instanceof IEssentiaTransport)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        IEssentiaTransport transport = (IEssentiaTransport) tile;

        // Display essentia type and amount
        if (transport.getEssentiaType(side) != null) {
            player.sendSystemMessage(Component.translatable("tc.resonator1",
                    String.valueOf(transport.getEssentiaAmount(side)),
                    transport.getEssentiaType(side).getName()));
        }

        // Display suction info
        String suctionType = "tc.resonator3"; // "None"
        if (transport.getSuctionType(side) != null) {
            suctionType = transport.getSuctionType(side).getName();
        }
        player.sendSystemMessage(Component.translatable("tc.resonator2",
                String.valueOf(transport.getSuctionAmount(side)),
                suctionType));

        // Play sound
        level.playSound(null, pos, SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS,
                0.5f, 1.9f + level.random.nextFloat() * 0.1f);

        return InteractionResult.SUCCESS;
    }
}
