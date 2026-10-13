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
import thaumcraft.common.lib.utils.TCCompat;

import javax.annotation.Nullable;

/**
 * Break Focus Effect - Breaks blocks with configurable silk touch and fortune.
 * The breaking speed depends on block hardness and power setting.
 */
public class FocusEffectBreak extends FocusEffect {

    @Override
    public String getResearch() {
        return "FOCUSBREAK";
    }

    @Override
    public String getKey() {
        return "thaumcraft.BREAK";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY;
    }

    @Override
    public int getComplexity() {
        int fortuneValue = getSettingValue("fortune");
        int fortuneComplexity = (fortuneValue == 0) ? 0 : ((fortuneValue + 1) * 3);
        return getSettingValue("power") * 3 + getSettingValue("silk") * 4 + fortuneComplexity;
    }

    @Override
    public boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        if (target.getType() != HitResult.Type.BLOCK || !(target instanceof BlockHitResult blockHit)) {
            return true; // Not a block, but don't fail the spell
        }
        
        Level world = getPackage().world;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        
        // TODO: Send particle effect packet
        // PacketHandler.sendToAllAround(new PacketFXFocusPartImpact(...))
        
        Entity caster = getCaster();
        if (!(caster instanceof ServerPlayer player)) {
            return false;
        }
        
        // Check if we can break this block
        if (!canBreakBlock(player, world, pos, state)) {
            return false;
        }
        
        boolean silk = getSettingValue("silk") > 0;
        int fortune = getSettingValue("fortune");
        float strength = getSettingValue("power") * finalPower;
        
        // Calculate if we can break based on hardness
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0) {
            return false; // Unbreakable
        }
        
        // Higher power can break harder blocks
        float maxHardness = strength * 10.0f;
        if (hardness > maxHardness) {
            return false; // Too hard for current power level
        }
        
        // Break the block with appropriate drops
        breakBlockWithEnchants(player, (ServerLevel) world, pos, state, silk, fortune);
        
        return true;
    }
    
    /**
     * Checks if the player can break the block at the given position.
     */
    private boolean canBreakBlock(ServerPlayer player, Level world, BlockPos pos, BlockState state) {
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
     * Breaks the block with silk touch and fortune enchantment effects.
     */
    private void breakBlockWithEnchants(ServerPlayer player, ServerLevel world, BlockPos pos, 
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
        
        // Remove the block
        world.destroyBlock(pos, false);
        
        // Play break sound
        world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] silk = { 0, 1 };
        String[] silkDesc = { "focus.common.no", "focus.common.yes" };
        
        int[] fortune = { 0, 1, 2, 3, 4 };
        String[] fortuneDesc = { "focus.common.no", "I", "II", "III", "IV" };
        
        return new NodeSetting[] {
            new NodeSetting("power", "focus.break.power", 
                new NodeSetting.NodeSettingIntRange(1, 5)),
            new NodeSetting("fortune", "focus.common.fortune", 
                new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
            new NodeSetting("silk", "focus.common.silk", 
                new NodeSetting.NodeSettingIntList(silk, silkDesc))
        };
    }

    @Override
    public void renderParticleFX(Level level, double posX, double posY, double posZ,
                                  double motionX, double motionY, double motionZ) {
        // TODO: Implement particle effects
        // Original used breaking/entropy particles
        // For now, this is a placeholder - will need client-side particle system
    }

    @Override
    public void onCast(Entity caster) {
        if (caster != null && caster.level() != null) {
            caster.level().playSound(null, caster.blockPosition().above(), 
                SoundEvents.END_GATEWAY_SPAWN, SoundSource.PLAYERS, 
                0.1f, 2.0f + (float)(caster.level().random.nextGaussian() * 0.05));
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




