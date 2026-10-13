package thaumcraft.common.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealHandler;

import java.util.List;

/**
 * ItemGolemBell - Tool for managing golems and seals.
 *
 * Right-click: Open seal GUI (when pointing at seal) or toggle golem follow mode
 * Shift+Right-click: Remove seal or open logistics GUI
 *
 * Also makes seals visible while held in hand.
 */
public class ItemGolemBell extends Item implements ISealDisplayer {

    public ItemGolemBell() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        player.swing(context.getHand());

        if (!level.isClientSide()) {
            // Check for seal at clicked position
            ISealEntity sealEntity = SealHandler.getSealEntity(level.dimension(), new SealPos(pos, side));

            if (sealEntity != null) {
                if (player.isShiftKeyDown()) {
                    // Remove the seal
                    SealHandler.removeSealEntity(level, sealEntity.getSealPos(), false);
                    level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.5f, 1.0f);
                } else {
                    // TODO: Open seal GUI
                    // For now just play a sound
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }

            // Check for golems nearby to toggle follow mode
            if (player.isShiftKeyDown()) {
                // TODO: Open logistics GUI
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.swing(hand);

        if (!level.isClientSide()) {
            // Try to find a seal by raytracing
            ISealEntity sealEntity = getSealFromRaytrace(player);

            if (sealEntity != null) {
                if (player.isShiftKeyDown()) {
                    // Remove the seal
                    SealHandler.removeSealEntity(level, sealEntity.getSealPos(), false);
                    level.playSound(null, sealEntity.getSealPos().pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.5f, 1.0f);
                } else {
                    // TODO: Open seal GUI
                    level.playSound(null, sealEntity.getSealPos().pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
                }
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            // Try to toggle golem follow mode for nearby golems
            List<EntityThaumcraftGolem> golems = level.getEntitiesOfClass(
                    EntityThaumcraftGolem.class,
                    new AABB(player.blockPosition()).inflate(16.0),
                    golem -> golem.isOwner(player));

            if (!golems.isEmpty()) {
                for (EntityThaumcraftGolem golem : golems) {
                    golem.setFollowingOwner(!golem.isFollowingOwner());
                }
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.BLOCKS, 0.6f, 1.0f + level.random.nextFloat() * 0.1f);
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            if (player.isShiftKeyDown()) {
                // TODO: Open logistics GUI
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        } else {
            // Client side - play bell sound
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f + level.random.nextFloat() * 0.1f);
        }

        return super.use(level, player, hand);
    }

    /**
     * Find a seal by raytracing from the player's view.
     */
    private ISealEntity getSealFromRaytrace(Player player) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();

        Vec3 eyePos = player.getEyePosition();

        // Calculate look direction
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float cosPitch = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float sinPitch = Mth.sin(-pitch * Mth.DEG_TO_RAD);

        float lookX = sinYaw * cosPitch;
        float lookY = sinPitch;
        float lookZ = cosYaw * cosPitch;

        double range = 5.0;
        Vec3 lookVec = new Vec3(lookX * range, lookY * range, lookZ * range);
        Vec3 endPos = eyePos.add(lookVec);

        // Step along the ray checking for seals
        Vec3 step = lookVec.scale(0.1);
        Vec3 checkPos = eyePos;

        for (int i = 0; i < (int)(range * 10); i++) {
            BlockPos blockPos = BlockPos.containing(checkPos);

            // Raytrace to find which face we're looking at
            BlockHitResult hitResult = player.level().clip(new ClipContext(
                    eyePos, checkPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                // Check for seal on the hit face
                ISealEntity sealEntity = SealHandler.getSealEntity(
                        player.level().dimension(),
                        new SealPos(hitResult.getBlockPos(), hitResult.getDirection()));

                if (sealEntity != null) {
                    return sealEntity;
                }
            }

            checkPos = checkPos.add(step);
        }

        return null;
    }
}