package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.entities.projectile.EntityFireball;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.entities.projectile.EntityFocusMine;
import thaumcraft.common.entities.projectile.EntityFocusProjectile;
import thaumcraft.common.entities.projectile.EntityGolemDart;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.entities.projectile.EntityHomingShard;
import thaumcraft.common.entities.projectile.EntityLightningOrb;
import thaumcraft.common.entities.projectile.EntityRiftBlast;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;
import thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall;
import thaumcraft.common.entities.monster.tainted.EntityTaintSwarm;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.entities.EntityFollowingItem;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntitySpellBat;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;
import thaumcraft.common.entities.monster.boss.EntityTaintacleGiant;
import thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;
import thaumcraft.common.entities.monster.boss.EntityEldritchWarden;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.construct.EntityArcaneBore;
import thaumcraft.common.golems.EntityThaumcraftGolem;

/**
 * Registry for all Thaumcraft entities.
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModEntities {
    
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(Registries.ENTITY_TYPE, Thaumcraft.MODID);
    
    // ==================== Focus Projectile Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFocusProjectile>> FOCUS_PROJECTILE = 
            ENTITY_TYPES.register("focus_projectile", () -> 
                EntityType.Builder.<EntityFocusProjectile>of(EntityFocusProjectile::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("focus_projectile"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFocusCloud>> FOCUS_CLOUD = 
            ENTITY_TYPES.register("focus_cloud", () -> 
                EntityType.Builder.<EntityFocusCloud>of(EntityFocusCloud::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .fireImmune()
                    .build("focus_cloud"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFocusMine>> FOCUS_MINE =
            ENTITY_TYPES.register("focus_mine", () ->
                EntityType.Builder.<EntityFocusMine>of(EntityFocusMine::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("focus_mine"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityFireball>> FIREBALL =
            ENTITY_TYPES.register("fireball", () ->
                EntityType.Builder.<EntityFireball>of(EntityFireball::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityLightningOrb>> LIGHTNING_ORB =
            ENTITY_TYPES.register("lightning_orb", () ->
                EntityType.Builder.<EntityLightningOrb>of(EntityLightningOrb::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("lightning_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityAspectOrb>> ASPECT_ORB =
            ENTITY_TYPES.register("aspect_orb", () ->
                EntityType.Builder.<EntityAspectOrb>of(EntityAspectOrb::new, MobCategory.MISC)
                    // TC4 EntityAspectOrb: 0.125 x 0.125 block hitbox, tracked from
                    // far away (ConfigEntities registered range 120) so orbs are
                    // visible while they fly towards the player.
                    .sized(0.125f, 0.125f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("aspect_orb"));

    // ==================== Other Projectile Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityAlumentum>> ALUMENTUM = 
            ENTITY_TYPES.register("alumentum", () -> 
                EntityType.Builder.<EntityAlumentum>of(EntityAlumentum::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("alumentum"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBottleTaint>> BOTTLE_TAINT = 
            ENTITY_TYPES.register("bottle_taint", () -> 
                EntityType.Builder.<EntityBottleTaint>of(EntityBottleTaint::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("bottle_taint"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityHomingShard>> HOMING_SHARD = 
            ENTITY_TYPES.register("homing_shard", () -> 
                EntityType.Builder.<EntityHomingShard>of(EntityHomingShard::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("homing_shard"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGolemDart>> GOLEM_DART = 
            ENTITY_TYPES.register("golem_dart", () -> 
                EntityType.Builder.<EntityGolemDart>of(EntityGolemDart::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("golem_dart"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGolemOrb>> GOLEM_ORB = 
            ENTITY_TYPES.register("golem_orb", () -> 
                EntityType.Builder.<EntityGolemOrb>of(EntityGolemOrb::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("golem_orb"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchOrb>> ELDRITCH_ORB = 
            ENTITY_TYPES.register("eldritch_orb", () -> 
                EntityType.Builder.<EntityEldritchOrb>of(EntityEldritchOrb::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("eldritch_orb"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityRiftBlast>> RIFT_BLAST = 
            ENTITY_TYPES.register("rift_blast", () -> 
                EntityType.Builder.<EntityRiftBlast>of(EntityRiftBlast::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("rift_blast"));
    
    // ==================== Monster Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityWisp>> WISP = 
            ENTITY_TYPES.register("wisp", () -> 
                EntityType.Builder.<EntityWisp>of(EntityWisp::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("wisp"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityFireBat>> FIRE_BAT = 
            ENTITY_TYPES.register("fire_bat", () -> 
                EntityType.Builder.<EntityFireBat>of(EntityFireBat::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.9f)
                    .clientTrackingRange(5)
                    .updateInterval(3)
                    .fireImmune()
                    .build("fire_bat"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityBrainyZombie>> BRAINY_ZOMBIE = 
            ENTITY_TYPES.register("brainy_zombie", () -> 
                EntityType.Builder.<EntityBrainyZombie>of(EntityBrainyZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("brainy_zombie"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityMindSpider>> MIND_SPIDER = 
            ENTITY_TYPES.register("mind_spider", () -> 
                EntityType.Builder.<EntityMindSpider>of(EntityMindSpider::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("mind_spider"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityThaumicSlime>> THAUMIC_SLIME = 
            ENTITY_TYPES.register("thaumic_slime", () -> 
                EntityType.Builder.<EntityThaumicSlime>of(EntityThaumicSlime::new, MobCategory.MONSTER)
                    .sized(2.04f, 2.04f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("thaumic_slime"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityGiantBrainyZombie>> GIANT_BRAINY_ZOMBIE = 
            ENTITY_TYPES.register("giant_brainy_zombie", () -> 
                EntityType.Builder.<EntityGiantBrainyZombie>of(EntityGiantBrainyZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("giant_brainy_zombie"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityInhabitedZombie>> INHABITED_ZOMBIE = 
            ENTITY_TYPES.register("inhabited_zombie", () -> 
                EntityType.Builder.<EntityInhabitedZombie>of(EntityInhabitedZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("inhabited_zombie"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchCrab>> ELDRITCH_CRAB = 
            ENTITY_TYPES.register("eldritch_crab", () -> 
                EntityType.Builder.<EntityEldritchCrab>of(EntityEldritchCrab::new, MobCategory.MONSTER)
                    .sized(0.8f, 0.6f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("eldritch_crab"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntitySpellBat>> SPELL_BAT = 
            ENTITY_TYPES.register("spell_bat", () -> 
                EntityType.Builder.<EntitySpellBat>of(EntitySpellBat::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.9f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("spell_bat"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchGuardian>> ELDRITCH_GUARDIAN = 
            ENTITY_TYPES.register("eldritch_guardian", () -> 
                EntityType.Builder.<EntityEldritchGuardian>of(EntityEldritchGuardian::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.25f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("eldritch_guardian"));
    
    // ==================== Cult Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultist>> CULTIST = 
            ENTITY_TYPES.register("cultist", () -> 
                EntityType.Builder.<EntityCultist>of(EntityCultist::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cultist"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistKnight>> CULTIST_KNIGHT = 
            ENTITY_TYPES.register("cultist_knight", () -> 
                EntityType.Builder.<EntityCultistKnight>of(EntityCultistKnight::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cultist_knight"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistCleric>> CULTIST_CLERIC = 
            ENTITY_TYPES.register("cultist_cleric", () -> 
                EntityType.Builder.<EntityCultistCleric>of(EntityCultistCleric::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("cultist_cleric"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistPortalLesser>> CULTIST_PORTAL_LESSER = 
            ENTITY_TYPES.register("cultist_portal_lesser", () -> 
                EntityType.Builder.<EntityCultistPortalLesser>of(EntityCultistPortalLesser::new, MobCategory.MONSTER)
                    .sized(1.5f, 3.0f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("cultist_portal_lesser"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityPech>> PECH = 
            ENTITY_TYPES.register("pech", () -> 
                EntityType.Builder.<EntityPech>of(EntityPech::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.2f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("pech"));
    
    // ==================== Tainted Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSwarm>> TAINT_SWARM = 
            ENTITY_TYPES.register("taint_swarm", () -> 
                EntityType.Builder.<EntityTaintSwarm>of(EntityTaintSwarm::new, MobCategory.MONSTER)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("taint_swarm"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacle>> TAINTACLE = 
            ENTITY_TYPES.register("taintacle", () -> 
                EntityType.Builder.<EntityTaintacle>of(EntityTaintacle::new, MobCategory.MONSTER)
                    .sized(0.8f, 3.0f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("taintacle"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleSmall>> TAINTACLE_SMALL = 
            ENTITY_TYPES.register("taintacle_small", () -> 
                EntityType.Builder.<EntityTaintacleSmall>of(EntityTaintacleSmall::new, MobCategory.MONSTER)
                    .sized(0.22f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("taintacle_small"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSeed>> TAINT_SEED = 
            ENTITY_TYPES.register("taint_seed", () -> 
                EntityType.Builder.<EntityTaintSeed>of(EntityTaintSeed::new, MobCategory.MONSTER)
                    .sized(1.5f, 1.25f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("taint_seed"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintSeedPrime>> TAINT_SEED_PRIME = 
            ENTITY_TYPES.register("taint_seed_prime", () -> 
                EntityType.Builder.<EntityTaintSeedPrime>of(EntityTaintSeedPrime::new, MobCategory.MONSTER)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("taint_seed_prime"));
    
    // ==================== Boss Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistLeader>> CULTIST_LEADER = 
            ENTITY_TYPES.register("cultist_leader", () -> 
                EntityType.Builder.<EntityCultistLeader>of(EntityCultistLeader::new, MobCategory.MONSTER)
                    .sized(0.75f, 2.25f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("cultist_leader"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityTaintacleGiant>> TAINTACLE_GIANT = 
            ENTITY_TYPES.register("taintacle_giant", () -> 
                EntityType.Builder.<EntityTaintacleGiant>of(EntityTaintacleGiant::new, MobCategory.MONSTER)
                    .sized(1.1f, 6.0f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("taintacle_giant"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityCultistPortalGreater>> CULTIST_PORTAL_GREATER = 
            ENTITY_TYPES.register("cultist_portal_greater", () -> 
                EntityType.Builder.<EntityCultistPortalGreater>of(EntityCultistPortalGreater::new, MobCategory.MONSTER)
                    .sized(1.5f, 3.0f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("cultist_portal_greater"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchGolem>> ELDRITCH_GOLEM = 
            ENTITY_TYPES.register("eldritch_golem", () -> 
                EntityType.Builder.<EntityEldritchGolem>of(EntityEldritchGolem::new, MobCategory.MONSTER)
                    .sized(1.75f, 3.5f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("eldritch_golem"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityEldritchWarden>> ELDRITCH_WARDEN = 
            ENTITY_TYPES.register("eldritch_warden", () -> 
                EntityType.Builder.<EntityEldritchWarden>of(EntityEldritchWarden::new, MobCategory.MONSTER)
                    .sized(1.5f, 3.5f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("eldritch_warden"));
    
    // ==================== Construct Entities ====================
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityThaumcraftGolem>> THAUMCRAFT_GOLEM = 
            ENTITY_TYPES.register("thaumcraft_golem", () -> 
                EntityType.Builder.<EntityThaumcraftGolem>of(EntityThaumcraftGolem::new, MobCategory.MISC)
                    .sized(0.7f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("thaumcraft_golem"));
    
    public static final DeferredHolder<EntityType<?>, EntityType<EntityArcaneBore>> ARCANE_BORE = 
            ENTITY_TYPES.register("arcane_bore", () -> 
                EntityType.Builder.<EntityArcaneBore>of(EntityArcaneBore::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("arcane_bore"));
    
    // ==================== Misc Entities ====================

    public static final DeferredHolder<EntityType<?>, EntityType<EntitySpecialItem>> SPECIAL_ITEM =
            ENTITY_TYPES.register("special_item", () ->
                EntityType.Builder.<EntitySpecialItem>of(EntitySpecialItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("special_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityFollowingItem>> FOLLOWING_ITEM =
            ENTITY_TYPES.register("following_item", () ->
                EntityType.Builder.<EntityFollowingItem>of(EntityFollowingItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("following_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityFluxRift>> FLUX_RIFT =
            ENTITY_TYPES.register("flux_rift", () ->
                EntityType.Builder.<EntityFluxRift>of(EntityFluxRift::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .build("flux_rift"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityFallingTaint>> FALLING_TAINT =
            ENTITY_TYPES.register("falling_taint", () ->
                EntityType.Builder.<EntityFallingTaint>of(EntityFallingTaint::new, MobCategory.MISC)
                    .sized(0.98f, 0.98f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("falling_taint"));

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name));
    }
}



