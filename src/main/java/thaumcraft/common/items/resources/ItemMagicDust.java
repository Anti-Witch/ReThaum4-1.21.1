package thaumcraft.common.items.resources;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.init.ModSounds;

import java.util.List;

public class ItemMagicDust extends ItemTCBase {

    public ItemMagicDust() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.FAIL;

        // Note: canPlayerEdit check is handled by Forge/Vanilla mostly but explicit check can be good
        // In 1.20.1 we rely on context usually.

        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }

        player.swing(hand);

        for (IDustTrigger trigger : IDustTrigger.triggers) {
            IDustTrigger.Placement place = trigger.getValidFace(level, player, pos, face);
            if (place != null) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                trigger.execute(level, player, pos, place, face);

                if (level.isClientSide) {
                    // Client side effects
                    doSparkles(player, level, pos, context.getClickLocation(), hand, trigger, place);
                } else {
                    // Server side could also trigger some effects if needed, but sparkle logic usually client
                    // However, actual block changes happen here (in trigger.execute)
                }

                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    private void doSparkles(Player player, Level level, BlockPos pos, Vec3 hitVec, InteractionHand hand, IDustTrigger trigger, IDustTrigger.Placement place) {
        // Play sound
        if (ModSounds.DUST.get() != null) {
            level.playSound(player, pos, ModSounds.DUST.get(), SoundSource.PLAYERS, 0.33f, 1.0f + (float)level.random.nextGaussian() * 0.05f);
        }

        // Calculate positions for sparkles
        List<BlockPos> sparkles = trigger.sparkle(level, player, pos, place);
        if (sparkles != null) {
            for (BlockPos p : sparkles) {
                // We use a packet here even though we are client side?
                // Actually if we are client side we can just spawn particles directly.
                // But the original code used FXDispatcher.
                // In 1.20.1 we should probably use Minecraft.getInstance().particleEngine or similar.

                // For now, let's assume we want to trigger the "bamf" effect or similar
                // But wait, PacketFXBlockBamf is for server -> client.
                // Since we are already on client, we should spawn particles directly.

                // TODO: Implement direct client particle spawning when FX system is ready
                // FXDispatcher.INSTANCE.drawBlockSparkles(p, hitVec);
            }
        }

        // Also spawn floating sparkles from hand to block
        // TODO: Implement doSparkles logic (hand to block trail)
    }
}
