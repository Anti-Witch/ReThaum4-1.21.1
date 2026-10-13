package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * EntityUtils - Utility methods for working with entities.
 * 
 * Provides common operations like finding entities, checking entity properties,
 * and entity targeting.
 * 
 * Ported from 1.12.2
 */
public class EntityUtils {

    /**
     * Get all entities of a type within a radius of a position.
     */
    public static <T extends Entity> List<T> getEntitiesInRange(Level level, BlockPos pos, double radius, Class<T> type) {
        AABB box = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1
        );
        return level.getEntitiesOfClass(type, box);
    }

    /**
     * Get all entities of a type within a radius of a position, with a filter.
     */
    public static <T extends Entity> List<T> getEntitiesInRange(Level level, BlockPos pos, double radius, 
            Class<T> type, Predicate<T> filter) {
        AABB box = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1
        );
        return level.getEntitiesOfClass(type, box, filter);
    }

    /**
     * Get the nearest entity of a type to a position.
     */
    @Nullable
    public static <T extends Entity> T getNearestEntity(Level level, BlockPos pos, double radius, Class<T> type) {
        List<T> entities = getEntitiesInRange(level, pos, radius, type);
        if (entities.isEmpty()) return null;
        
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                .orElse(null);
    }

    /**
     * Get the nearest living entity to a position, excluding a specific entity.
     */
    @Nullable
    public static LivingEntity getNearestLivingEntity(Level level, BlockPos pos, double radius, 
            @Nullable Entity exclude) {
        List<LivingEntity> entities = getEntitiesInRange(level, pos, radius, LivingEntity.class);
        if (entities.isEmpty()) return null;
        
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entities.stream()
                .filter(e -> e != exclude && e.isAlive())
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                .orElse(null);
    }

    /**
     * Check if an entity is hostile.
     */
    public static boolean isHostile(Entity entity) {
        return entity instanceof Enemy;
    }

    /**
     * Check if an entity is a player.
     */
    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    /**
     * Check if an entity is alive and valid.
     */
    public static boolean isAliveAndValid(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    /**
     * Get the look direction vector for an entity.
     */
    public static Vec3 getLookVector(Entity entity) {
        return entity.getLookAngle();
    }

    /**
     * Get the eye position of an entity.
     */
    public static Vec3 getEyePosition(Entity entity) {
        return entity.getEyePosition();
    }

    /**
     * Push an entity away from a position.
     * 
     * @param entity Entity to push
     * @param pos Position to push away from
     * @param force Strength of the push
     */
    public static void pushAway(Entity entity, BlockPos pos, double force) {
        Vec3 entityPos = entity.position();
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        Vec3 direction = entityPos.subtract(center).normalize();
        entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(force)));
        entity.hurtMarked = true;
    }

    /**
     * Pull an entity toward a position.
     * 
     * @param entity Entity to pull
     * @param pos Position to pull toward
     * @param force Strength of the pull
     */
    public static void pullToward(Entity entity, BlockPos pos, double force) {
        Vec3 entityPos = entity.position();
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        Vec3 direction = center.subtract(entityPos).normalize();
        entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(force)));
        entity.hurtMarked = true;
    }

    /**
     * Get the max health of a living entity.
     */
    public static double getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    /**
     * Get the current health of a living entity.
     */
    public static double getCurrentHealth(LivingEntity entity) {
        return entity.getHealth();
    }

    /**
     * Get the attack damage of a living entity.
     */
    public static double getAttackDamage(LivingEntity entity) {
        var attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        return attr != null ? attr.getValue() : 1.0;
    }

    /**
     * Check if a player is in creative mode.
     */
    public static boolean isCreative(Player player) {
        return player.getAbilities().instabuild;
    }

    /**
     * Check if a player is in survival mode.
     */
    public static boolean isSurvival(Player player) {
        return !player.getAbilities().instabuild;
    }

    /**
     * Check if the entity is a server player.
     */
    public static boolean isServerPlayer(Entity entity) {
        return entity instanceof ServerPlayer;
    }

    /**
     * Get players within range of a position.
     */
    public static List<Player> getPlayersInRange(Level level, BlockPos pos, double radius) {
        return getEntitiesInRange(level, pos, radius, Player.class);
    }

    /**
     * Get the nearest player to a position.
     */
    @Nullable
    public static Player getNearestPlayer(Level level, BlockPos pos, double radius) {
        return getNearestEntity(level, pos, radius, Player.class);
    }

    /**
     * Check if an entity can see a position (no blocks in the way).
     */
    public static boolean canSee(LivingEntity entity, BlockPos pos) {
        Vec3 eyePos = entity.getEyePosition();
        Vec3 targetPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return entity.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, targetPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                entity
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    /**
     * Set an entity's target (if it's a mob).
     */
    public static void setTarget(Entity attacker, @Nullable LivingEntity target) {
        if (attacker instanceof Mob mob) {
            mob.setTarget(target);
        }
    }

    /**
     * Check if an entity can see a specific coordinate (line of sight check).
     */
    public static boolean canEntityBeSeen(Entity entity, double x, double y, double z) {
        Level level = entity.level();
        Vec3 entityPos = entity.getEyePosition();
        Vec3 targetPos = new Vec3(x, y, z);
        
        return level.clip(new net.minecraft.world.level.ClipContext(
                entityPos, targetPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                entity
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
    
    /**
     * Check if an entity is friendly to another entity.
     * An entity is considered friendly if:
     * - It's the same entity
     * - It's on the same team
     * - It's a pet/tamed creature owned by the source
     * - It's a non-hostile mob when the source is not hostile
     * 
     * @param source The source entity (usually the attacker)
     * @param target The target entity to check
     * @return true if target is friendly to source
     */
    public static boolean isFriendly(Entity source, Entity target) {
        if (source == null || target == null) return false;
        if (source == target) return true;
        
        // Same team check
        if (source.isAlliedTo(target)) return true;
        
        // Check if target is tamed by source
        if (target instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            if (tamable.isTame() && tamable.getOwner() == source) {
                return true;
            }
        }
        
        // Check if source is tamed by target
        if (source instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            if (tamable.isTame() && tamable.getOwner() == target) {
                return true;
            }
        }
        
        // If source is a player's pet, don't attack player's other pets
        if (source instanceof net.minecraft.world.entity.TamableAnimal sourceTamable && 
            target instanceof net.minecraft.world.entity.TamableAnimal targetTamable) {
            if (sourceTamable.isTame() && targetTamable.isTame() && 
                sourceTamable.getOwner() == targetTamable.getOwner()) {
                return true;
            }
        }
        
        return false;
    }
}




