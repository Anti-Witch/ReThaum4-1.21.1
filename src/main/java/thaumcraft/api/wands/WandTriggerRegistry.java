package thaumcraft.api.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.common.items.essentia.ItemJarNode;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * WandTriggerRegistry - Registry for wand block interaction triggers.
 * Handles special interactions like jarring nodes, deconstructing warded blocks, etc.
 *
 * Ported from TC4, adapted for 1.21.1.
 */
public class WandTriggerRegistry {

    private static final Map<Block, IWandTrigger> triggers = new HashMap<>();

    /**
     * Register a wand trigger for a specific block.
     */
    public static void registerTrigger(Block block, IWandTrigger trigger) {
        triggers.put(block, trigger);
    }

    /**
     * Check if a block has a wand trigger.
     */
    public static boolean hasTrigger(Block block) {
        return triggers.containsKey(block);
    }

    /**
     * Perform the wand trigger for a block.
     *
     * @return true if trigger was performed successfully
     */
    public static boolean performTrigger(Level world, ItemStack wandStack, Player player, BlockPos pos, Direction side, Block block, BlockState state) {
        IWandTrigger trigger = triggers.get(block);
        if (trigger != null) {
            return trigger.onWandRightClick(world, wandStack, player, pos, side, state);
        }
        return false;
    }

    /**
     * Initialize all wand triggers.
     * Call during mod initialization.
     */
    public static void init() {
        // Warded Jar trigger - for jarring nodes
        registerTrigger(ModBlocks.JAR_NORMAL.get(), new IWandTrigger() {
            @Override
            public boolean onWandRightClick(Level world, ItemStack wandStack, Player player, BlockPos pos, Direction side, BlockState state) {
                return tryJarNode(world, wandStack, player, pos);
            }
        });

        // Bookshelf - TC4: the first right-click with a wand on a bookshelf
        // creates the Thaumonomicon (WandTrigger from ConfigRecipes).
        registerTrigger(Blocks.BOOKSHELF, new IWandTrigger() {
            @Override
            public boolean onWandRightClick(Level world, ItemStack wandStack, Player player, BlockPos pos, Direction side, BlockState state) {
                if (player.getInventory().contains(new ItemStack(ModItems.THAUMONOMICON.get()))) {
                    return false;
                }
                if (!world.isClientSide) {
                    ItemStack book = new ItemStack(ModItems.THAUMONOMICON.get());
                    if (!player.addItem(book)) {
                        player.drop(book, false);
                    }
                    world.playSound(null, pos,
                            net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("got.thaumonomicon"),
                            true);
                }
                return true;
            }
        });

        // Warded blocks - deconstruction (if player is owner)
        // TODO: Add when warded blocks are implemented
    }

    /**
     * Try to jar a node - TC4 mechanic:
     * 3x3x3 glass cube with node in center, wooden slabs platform on top (4th layer),
     * empty warded jar on center slab, player has "Jarring Node" research.
     */
    private static boolean tryJarNode(Level world, ItemStack wandStack, Player player, BlockPos jarPos) {
        // 1. Check if jar is empty warded jar
        if (!world.isClientSide) {
            // Get the tile entity of the jar
            if (world.getBlockEntity(jarPos) instanceof thaumcraft.common.tiles.essentia.TileJar jar) {
                // Check if jar is empty (no essentia)
                if (jar.getEssentiaAmount(Direction.UP) > 0) {
                    return false; // Jar not empty
                }
            }
        }

        // 2. Check player has research
        if (!ThaumcraftCapabilities.isResearchComplete(player, "NODEJAR")) {
            return false;
        }

        // 3. Check structure: node should be 3 blocks below jar center
        BlockPos nodePos = jarPos.below(3); // jar at y+3, node at y (center of 3x3x3 cube)

        if (!(world.getBlockEntity(nodePos) instanceof TileNode node)) {
            return false; // No node at expected position
        }

        // 4. Check 3x3x3 glass cube around node
        if (!checkGlassCube(world, nodePos)) {
            return false;
        }

        // 5. Check wooden slabs platform on layer directly above node
        // (jar sits on top of this platform, so it is at jarPos.below())
        BlockPos platformLevel = nodePos.above(2);
        if (!checkWoodenSlabsPlatform(world, platformLevel)) {
            return false;
        }

        // 6. Check jar is on center slab
        if (!world.getBlockState(jarPos.below()).getBlock().equals(Blocks.OAK_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.SPRUCE_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.BIRCH_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.JUNGLE_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.ACACIA_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.DARK_OAK_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.CRIMSON_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.WARPED_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.MANGROVE_SLAB) &&
            !world.getBlockState(jarPos.below()).getBlock().equals(Blocks.BAMBOO_SLAB)) {
            return false;
        }

        // Check slab is top half
        BlockState slabState = world.getBlockState(jarPos.below());
        if (slabState.hasProperty(BlockStateProperties.HALF) &&
            slabState.getValue(BlockStateProperties.HALF) != Half.TOP) {
            return false;
        }

        // ALL CHECKS PASSED - Perform jarring!
        if (!world.isClientSide) {
            return performJarring(world, player, wandStack, jarPos, nodePos, node);
        }

        return true; // Client side - just show it worked
    }

    /**
     * Check 3x3x3 glass cube around node position.
     * Node is at center (0,0,0 relative), glass at all 26 surrounding positions.
     */
    private static boolean checkGlassCube(Level world, BlockPos nodePos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Center is node

                    BlockPos checkPos = nodePos.offset(x, y, z);
                    BlockState state = world.getBlockState(checkPos);

                    // Must be glass or stained glass
                    if (!state.is(Blocks.GLASS) && !state.is(Blocks.WHITE_STAINED_GLASS) &&
                        !state.is(Blocks.ORANGE_STAINED_GLASS) && !state.is(Blocks.MAGENTA_STAINED_GLASS) &&
                        !state.is(Blocks.LIGHT_BLUE_STAINED_GLASS) && !state.is(Blocks.YELLOW_STAINED_GLASS) &&
                        !state.is(Blocks.LIME_STAINED_GLASS) && !state.is(Blocks.PINK_STAINED_GLASS) &&
                        !state.is(Blocks.GRAY_STAINED_GLASS) && !state.is(Blocks.LIGHT_GRAY_STAINED_GLASS) &&
                        !state.is(Blocks.CYAN_STAINED_GLASS) && !state.is(Blocks.PURPLE_STAINED_GLASS) &&
                        !state.is(Blocks.BLUE_STAINED_GLASS) && !state.is(Blocks.BROWN_STAINED_GLASS) &&
                        !state.is(Blocks.GREEN_STAINED_GLASS) && !state.is(Blocks.RED_STAINED_GLASS) &&
                        !state.is(Blocks.BLACK_STAINED_GLASS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check 3x3 wooden slabs platform at given level.
     * All 9 positions must be wooden slabs (top half).
     */
    private static boolean checkWoodenSlabsPlatform(Level world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.offset(x, 0, z);
                BlockState state = world.getBlockState(checkPos);

                // Check if it's a wooden slab
                Block block = state.getBlock();
                boolean isWoodenSlab = block.equals(Blocks.OAK_SLAB) || block.equals(Blocks.SPRUCE_SLAB) ||
                    block.equals(Blocks.BIRCH_SLAB) || block.equals(Blocks.JUNGLE_SLAB) ||
                    block.equals(Blocks.ACACIA_SLAB) || block.equals(Blocks.DARK_OAK_SLAB) ||
                    block.equals(Blocks.CRIMSON_SLAB) || block.equals(Blocks.WARPED_SLAB) ||
                    block.equals(Blocks.MANGROVE_SLAB) || block.equals(Blocks.BAMBOO_SLAB);

                if (!isWoodenSlab) return false;

                // Check it's top half
                if (state.hasProperty(BlockStateProperties.HALF) &&
                    state.getValue(BlockStateProperties.HALF) != Half.TOP) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Actually perform the jarring - capture node into jar.
     */
    private static boolean performJarring(Level world, Player player, ItemStack wandStack, BlockPos jarPos, BlockPos nodePos, TileNode node) {
        // 1. Create filled jar item
        ItemStack filledJar = new ItemStack(ModItems.JAR_NODE.get());
        ItemJarNode jarItem = (ItemJarNode) filledJar.getItem();

        // Copy node data to jar
        jarItem.setAspects(filledJar, node.getAspects());
        jarItem.setNodeAttributes(filledJar, node.getNodeType(), node.getNodeModifier(), node.getId());

        // 2. Reduce node aspectsBase by 10% (TC4 mechanic)
        for (var aspect : node.getAspectsBase().getAspects()) {
            int baseAmount = node.getNodeVisBase(aspect);
            int newBase = (int) (baseAmount * 0.9);
            node.setNodeVisBase(aspect, (short) newBase);
        }

        // 3. Remove node block (replace with air)
        world.setBlockAndUpdate(nodePos, Blocks.AIR.defaultBlockState());

        // 4. Replace jar block with air (it becomes item)
        world.setBlockAndUpdate(jarPos, Blocks.AIR.defaultBlockState());

        // 5. Give filled jar to player
        if (!player.getAbilities().instabuild) {
            // Try to place in off-hand first if empty
            ItemStack offhand = player.getOffhandItem();
            if (offhand.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, filledJar);
            } else {
                // Try main hand
                ItemStack mainhand = player.getMainHandItem();
                if (mainhand.isEmpty() || mainhand.getItem() == wandStack.getItem()) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filledJar);
                } else {
                    // Drop at player feet
                    player.drop(filledJar, false);
                }
            }
        }

        // 6. Play effects
        world.playSound(null, jarPos.getX(), jarPos.getY(), jarPos.getZ(),
                net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        world.playSound(null, jarPos.getX(), jarPos.getY(), jarPos.getZ(),
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        // 7. Particles - explosion + enchant particles
        for (int i = 0; i < 30; i++) {
            double px = jarPos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 2;
            double py = jarPos.getY() + 0.5 + (world.random.nextDouble() - 0.5) * 2;
            double pz = jarPos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 2;
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, px, py, pz, 0, 0.1, 0);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, px, py, pz, 0, 0, 0);
        }

        // 8. Notify player
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("thaumcraft.node.jarred", node.getNodeType().name())
                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);

        return true;
    }

    /**
     * Interface for wand triggers.
     */
    public interface IWandTrigger {
        boolean onWandRightClick(Level world, ItemStack wandStack, Player player, BlockPos pos, Direction side, BlockState state);
    }
}
