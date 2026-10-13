package thaumcraft.common.blocks.basic;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.blocks.BlockTC;

/**
 * Stone blocks used in Thaumcraft structures.
 * Includes arcane stone, ancient stone, eldritch tile, etc.
 */
public class BlockStoneTC extends BlockTC {

    private final boolean canSpawnMobs;

    public BlockStoneTC(Properties properties, boolean canSpawnMobs) {
        super(properties.isValidSpawn((state, level, pos, entityType) -> canSpawnMobs));
        this.canSpawnMobs = canSpawnMobs;
    }

    /**
     * Creates a standard arcane/ancient stone block.
     */
    public static BlockStoneTC create(boolean canSpawnMobs) {
        return new BlockStoneTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            canSpawnMobs
        );
    }

    /**
     * Creates an unbreakable stone block (like ancient rock).
     */
    public static BlockStoneTC createUnbreakable() {
        return new BlockStoneTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0f, 3600000.0f)
                .sound(SoundType.STONE)
                .noLootTable(),
            false
        );
    }

    /**
     * Creates a reinforced stone block (like eldritch tile).
     */
    public static BlockStoneTC createReinforced() {
        return new BlockStoneTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(15.0f, 1000.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            true
        );
    }
}
