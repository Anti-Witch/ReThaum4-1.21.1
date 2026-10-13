package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.common.lib.utils.TCCompat;

import javax.annotation.Nullable;

/**
 * Exchange Focus Effect - Replaces blocks with a selected block type.
 * The replacement block is selected via the caster gauntlet's block picker.
 * Supports silk touch and fortune options for the replaced block's drops.
 */
public class FocusEffectExchange extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSEXCHANGE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.EXCHANGE";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.EXCHANGE;
    }

    @Override
    public int getComplexity() {
        int fortuneValue = getSettingValue("fortune");
        int fortuneComplexity = (fortuneValue == 0) ? 0 : ((fortuneValue + 1) * 3);
        return 5 + getSettingValue("silk") * 4 + fortuneComplexity;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        if (target.getType() != HitResult.Type.BLOCK || !(target instanceof BlockHitResult blockHit)) {
            return false;
        }
        
        Level world = getPackage().world;
        Entity caster = getCaster();
        
        if (!(caster instanceof ServerPlayer player)) {
            return false;
        }
        
        BlockPos pos = blockHit.getBlockPos();
        BlockState oldState = world.getBlockState(pos);
        
        // Check if we can modify this block
        if (!canModifyBlock(player, world, pos, oldState)) {
            return false;
        }
        
        // TODO: Get picked block from caster gauntlet
        // ItemStack casterStack = player.getMainHandItem();
        // if (casterStack.getItem() instanceof ItemCaster caster) {
        //     ItemStack pickedBlock = caster.getPickedBlock(casterStack);
        //     ...
        // }
        
        // For now, just break the block with enchants as placeholder
        // Full implementation needs ItemCaster.getPickedBlock() and block swapper system
        boolean silk = getSettingValue("silk") > 0;
        int fortune = getSettingValue("fortune");
        
        // Drop the original block with enchantments
        dropBlockWithEnchants(player, (ServerLevel) world, pos, oldState, silk, fortune);
        
        // TODO: Place the picked block
        // For now, just leave it empty (placeholder)
        world.removeBlock(pos, false);
        
        return true;
    }
    
    /**
     * Checks if the player can modify the block at the given position.
     */
    private boolean canModifyBlock(ServerPlayer player, Level world, BlockPos pos, BlockState state) {
        // Check if block is unbreakable
        if (state.getDestroySpeed(world, pos) < 0) {
            return false;
        }
        
        // Check if the block is protected
        if (!world.mayInteract(player, pos)) {
            return false;
        }
        
        // Check game rules and adventure mode
        if (!player.mayBuild()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Drops the block with silk touch and fortune enchantment effects.
     */
    private void dropBlockWithEnchants(ServerPlayer player, ServerLevel world, BlockPos pos, 
                                       BlockState state, boolean silk, int fortune) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        Block block = state.getBlock();
        
        // Create a fake tool with enchantments
        ItemStack fakeTool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        if (silk) {
            fakeTool.enchant(TCCompat.enchantment(world, Enchantments.SILK_TOUCH), 1);
        }
        if (fortune > 0) {
            fakeTool.enchant(TCCompat.enchantment(world, Enchantments.FORTUNE), fortune);
        }
        
        // Drop items using the fake tool
        block.playerDestroy(world, player, pos, state, blockEntity, fakeTool);
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] silk = { 0, 1 };
        String[] silkDesc = { "focus.common.no", "focus.common.yes" };
        
        int[] fortune = { 0, 1, 2, 3, 4 };
        String[] fortuneDesc = { "focus.common.no", "I", "II", "III", "IV" };
        
        return new NodeSetting[] {
            new NodeSetting("fortune", "focus.common.fortune", 
                new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
            new NodeSetting("silk", "focus.common.silk", 
                new NodeSetting.NodeSettingIntList(silk, silkDesc))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        FocusFX.exchange(level, posX, posY, posZ, motionX, motionY, motionZ);
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 
                0.2f, 2.0f + (float)(caster.level().random.nextGaussian() * 0.05));
        }
    }
    
    /**
     * Gets the caster entity from the focus package.
     */
    private Entity getCaster() {
        if (getPackage() == null || getPackage().getCasterUUID() == null) {
            return null;
        }
        if (getPackage().world != null) {
            for (Player player : getPackage().world.players()) {
                if (player.getUUID().equals(getPackage().getCasterUUID())) {
                    return player;
                }
            }
        }
        return null;
    }
}




