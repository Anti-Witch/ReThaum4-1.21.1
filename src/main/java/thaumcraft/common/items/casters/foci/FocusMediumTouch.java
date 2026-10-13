package thaumcraft.common.items.casters.foci;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Touch Medium - Basic delivery that hits what the player is looking at.
 * Range is limited to player's reach distance.
 */
public class FocusMediumTouch extends FocusMediumRoot {

    @Override
    public String getResearch() {
        return "BASEAUROMANCY";
    }

    @Override
    public String getKey() {
        return "thaumcraft.TOUCH";
    }

    @Override
    public int getComplexity() {
        return 2;
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY, EnumSupplyType.TARGET };
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AVERSION;
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        if (getPackage() == null || getPackage().world == null) {
            return new Trajectory[0];
        }

        Entity caster = getCaster();
        if (caster == null) {
            return new Trajectory[0];
        }

        double range = getRange(caster);
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // Try to hit an entity first
        HitResult hit = rayTraceEntities(caster, eyePos, endPos, range);
        
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

    @Override
    public HitResult[] supplyTargets() {
        if (getPackage() == null || getPackage().world == null) {
            return new HitResult[0];
        }

        Entity caster = getCaster();
        if (caster == null) {
            return new HitResult[0];
        }

        double range = getRange(caster);
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // Try to hit an entity first
        HitResult hit = rayTraceEntities(caster, eyePos, endPos, range);
        
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

    protected double getRange(Entity caster) {
        if (caster instanceof Player player) {
            // Use player's reach distance (default 4.5 in survival, 5 in creative)
            return player.getAbilities().instabuild ? 5.0 : 4.5;
        }
        return 4.5;
    }

    protected Entity getCaster() {
        if (getPackage() == null || getPackage().getCasterUUID() == null) {
            return null;
        }
        // Find entity by UUID
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
     * Raytrace to find entities along the path.
     */
    protected HitResult rayTraceEntities(Entity caster, Vec3 start, Vec3 end, double range) {
        AABB searchBox = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        
        Predicate<Entity> filter = e -> !e.isSpectator() && e.isPickable() && e != caster;
        
        double closestDist = range;
        Entity closestEntity = null;
        Vec3 closestHit = null;

        for (Entity entity : getPackage().world.getEntities(caster, searchBox, filter)) {
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

    @Override
    public boolean execute(Trajectory trajectory) {
        // TC4 FocusMediumTouch.execute: draw a short burst of the focus effect
        // particles at the caster's hand so every nearby player sees the cast.
        if (getPackage() != null && getPackage().world != null) {
            FocusEffect[] effects = getPackage().getFocusEffects();
            if (effects.length > 0) {
                String[] keys = new String[effects.length];
                for (int i = 0; i < effects.length; i++) {
                    keys[i] = effects[i].getKey();
                }
                PacketHandler.sendToAllAround(
                        new PacketFXFocusEffect(
                                (float) trajectory.source.x, (float) trajectory.source.y, (float) trajectory.source.z,
                                (float) trajectory.direction.x / 2.0f,
                                (float) trajectory.direction.y / 2.0f,
                                (float) trajectory.direction.z / 2.0f,
                                keys),
                        getPackage().world,
                        trajectory.source.x, trajectory.source.y, trajectory.source.z, 64.0);
            }
        }
        return true;
    }
}




