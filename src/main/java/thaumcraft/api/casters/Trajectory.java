package thaumcraft.api.casters;

import net.minecraft.world.phys.Vec3;

/**
 * Represents a trajectory for focus effects - a starting point and direction.
 */
public class Trajectory {
    
    /** Starting position of the trajectory */
    public Vec3 source;
    
    /** Direction vector (should be normalized) */
    public Vec3 direction;
    
    public Trajectory(Vec3 source, Vec3 direction) {
        this.source = source;
        this.direction = direction;
    }

    /**
     * Get a point along this trajectory at the given distance.
     */
    public Vec3 getPoint(double distance) {
        return source.add(direction.scale(distance));
    }
}




