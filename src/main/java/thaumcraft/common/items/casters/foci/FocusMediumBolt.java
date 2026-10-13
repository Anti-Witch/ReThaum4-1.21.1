package thaumcraft.common.items.casters.foci;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXZap;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Bolt Medium - Extended range instant hit delivery.
 * Similar to Touch but with longer range and a lightning/zap visual effect.
 * Extends FocusMediumTouch for shared raytrace logic.
 */
public class FocusMediumBolt extends FocusMediumTouch {
    
    /** Default range for bolt */
    private static final float BOLT_RANGE = 16.0f;

    @Override
    public String getResearch() {
        return "FOCUSBOLT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.BOLT";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENERGY;
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        if (getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        Vec3 start = trajectory.source;
        Vec3 direction = trajectory.direction.normalize();
        Vec3 end = start.add(direction.scale(BOLT_RANGE));
        
        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(start, end);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()));
            
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                end = hit.getLocation();
            }
        } else if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
            // Adjust end point to entity position
            end = start.add(direction.scale(start.distanceTo(entityHit.getEntity().position())));
        }
        
        // TC4 FocusMediumBolt.execute: lightning zap between source and target
        PacketHandler.sendToAllAround(
                new PacketFXZap(start, end, calculateEffectColor(), getPackage().getPower() * 0.66f),
                getPackage().world, start.x, start.y, start.z, 64.0);

        return true;
    }
    
    /**
     * Extended range version of supplyTargets.
     */
    @Override
    public HitResult[] supplyTargets() {
        if (getPackage() == null || getPackage().world == null) {
            return new HitResult[0];
        }

        Entity caster = getCasterEntity();
        if (caster == null) {
            return new HitResult[0];
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(BOLT_RANGE));

        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(eyePos, endPos);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    caster));
        }

        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return new HitResult[] { hit };
        }
        
        return new HitResult[0];
    }
    
    /**
     * Extended range version of supplyTrajectories.
     */
    @Override
    public Trajectory[] supplyTrajectories() {
        if (getPackage() == null || getPackage().world == null) {
            return new Trajectory[0];
        }

        Entity caster = getCasterEntity();
        if (caster == null) {
            return new Trajectory[0];
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(BOLT_RANGE));

        // Try to hit an entity first
        HitResult hit = rayTraceEntitiesFromTrajectory(eyePos, endPos);
        
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            // Fall back to block raycast
            hit = getPackage().world.clip(new ClipContext(
                    eyePos, endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    caster));
        }

        Vec3 hitPos = (hit != null) ? hit.getLocation() : endPos;
        
        return new Trajectory[] { new Trajectory(hitPos, lookVec.normalize()) };
    }
    
    /**
     * Raytrace entities along a trajectory path.
     */
    private HitResult rayTraceEntitiesFromTrajectory(Vec3 start, Vec3 end) {
        if (getPackage() == null || getPackage().world == null) {
            return null;
        }
        
        AABB searchBox = new AABB(start, end).inflate(1.0);
        
        Entity casterEntity = getCasterEntity();
        Predicate<Entity> filter = e -> !e.isSpectator() && e.isPickable() && e != casterEntity;
        
        double closestDist = BOLT_RANGE;
        Entity closestEntity = null;
        Vec3 closestHit = null;

        for (Entity entity : getPackage().world.getEntities(casterEntity, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hitOpt = entityBox.clip(start, end);
            
            if (hitOpt.isPresent()) {
                double dist = start.distanceTo(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = entity;
                    closestHit = hitOpt.get();
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestHit);
        }
        
        return null;
    }
    
    /**
     * Gets the caster entity from the focus package.
     */
    private Entity getCasterEntity() {
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
    
    /**
     * Calculate a combined color from all focus effects for the zap visual
     * (TC4 FocusMediumBolt.execute averages the effect colours).
     */
    private int calculateEffectColor() {
        if (getPackage() == null) return 0x5599FF;

        FocusEffect[] effects = getPackage().getFocusEffects();
        if (effects.length == 0) return 0x5599FF;

        int r = 0, g = 0, b = 0;
        for (FocusEffect effect : effects) {
            Color c = new Color(FocusEngine.getElementColor(effect.getKey()));
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        return new Color(r / effects.length, g / effects.length, b / effects.length).getRGB();
    }
}




