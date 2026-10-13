package thaumcraft.init;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * ModSpawnPlacements - Registers spawn placement rules for Thaumcraft entities.
 * 
 * In 1.20.1, spawn rules are registered via RegisterSpawnPlacementsEvent.
 * Actual biome-based spawning is handled via BiomeModifier JSONs in:
 * data/thaumcraft/neoforge/biome_modifier/
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModSpawnPlacements {

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        Thaumcraft.LOGGER.info("Registering Thaumcraft spawn placements");

        // Brainy Zombie - spawns like regular zombies (on ground, in dark)
        event.register(
            ModEntities.BRAINY_ZOMBIE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Giant Brainy Zombie
        event.register(
            ModEntities.GIANT_BRAINY_ZOMBIE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Wisp - spawns in the air
        event.register(
            ModEntities.WISP.get(),
            SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityWisp::checkWispSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Fire Bat - spawns in the Nether
        event.register(
            ModEntities.FIRE_BAT.get(),
            SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityFireBat::checkFireBatSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Pech - spawns in magical biomes
        event.register(
            ModEntities.PECH.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityPech::checkPechSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Taintacle (was missing -> NeoForge warned about an unregistered
        // spawn placement for thaumcraft:taintacle)
        event.register(
            ModEntities.TAINTACLE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Thaumic Slime - Note: doesn't spawn naturally, only from flux effects
        event.register(
            ModEntities.THAUMIC_SLIME.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityThaumicSlime::checkThaumicSlimeSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Mind Spider
        event.register(
            ModEntities.MIND_SPIDER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Eldritch Crab
        event.register(
            ModEntities.ELDRITCH_CRAB.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Eldritch Guardian
        event.register(
            ModEntities.ELDRITCH_GUARDIAN.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Cultist entities
        event.register(
            ModEntities.CULTIST.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        event.register(
            ModEntities.CULTIST_KNIGHT.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        event.register(
            ModEntities.CULTIST_CLERIC.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        // Inhabited Zombie
        event.register(
            ModEntities.INHABITED_ZOMBIE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.AND
        );

        Thaumcraft.LOGGER.info("Registered Thaumcraft spawn placements");
    }
}





