package thaumcraft.common.items.casters.foci;

import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMod;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Scatter Modifier - Splits a single trajectory into multiple trajectories
 * spread across a cone angle. Each forked trajectory has reduced power.
 */
public class FocusModScatter extends FocusMod {

    @Override
    public String getResearch() {
        return "FOCUSSCATTER";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SCATTER";
    }

    @Override
    public int getComplexity() {
        // More forks = more complex, but wider cone = easier (less precise)
        return (int) Math.max(2.0f, 2.0f * (getSettingValue("forks") - getSettingValue("cone") / 45.0f));
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] angles = { 10, 30, 60, 90, 180, 270, 360 };
        String[] anglesDesc = { "10", "30", "60", "90", "180", "270", "360" };
        
        return new NodeSetting[] {
            new NodeSetting("forks", "focus.scatter.forks", 
                new NodeSetting.NodeSettingIntRange(2, 10)),
            new NodeSetting("cone", "focus.scatter.cone", 
                new NodeSetting.NodeSettingIntList(angles, anglesDesc))
        };
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        if (getParent() == null || getPackage() == null || getPackage().world == null) {
            return new Trajectory[0];
        }
        
        Trajectory[] parentTrajectories = getParent().supplyTrajectories();
        if (parentTrajectories == null || parentTrajectories.length == 0) {
            return new Trajectory[0];
        }
        
        List<Trajectory> result = new ArrayList<>();
        int forks = getSettingValue("forks");
        int angle = getSettingValue("cone");
        
        // Randomness multiplier based on cone angle
        // angle / 180 gives us a value where 180 degrees = full hemisphere
        double spread = angle * 0.0075; // ~0.75% of angle as gaussian spread
        
        for (Trajectory sourceTrajectory : parentTrajectories) {
            Vec3 source = sourceTrajectory.source;
            Vec3 direction = sourceTrajectory.direction.normalize();
            
            for (int i = 0; i < forks; i++) {
                // Add random deviation to the direction vector
                double dx = getPackage().world.random.nextGaussian() * spread;
                double dy = getPackage().world.random.nextGaussian() * spread;
                double dz = getPackage().world.random.nextGaussian() * spread;
                
                Vec3 newDirection = direction.add(dx, dy, dz).normalize();
                result.add(new Trajectory(source, newDirection));
            }
        }
        
        return result.toArray(new Trajectory[0]);
    }

    @Override
    public float getPowerMultiplier() {
        // Power is divided among the forks
        return 1.0f / (getSettingValue("forks") / 2.0f);
    }

    @Override
    public boolean execute() {
        return true;
    }

    @Override
    public boolean isExclusive() {
        // Only one scatter modifier can be in a focus
        return true;
    }
}




