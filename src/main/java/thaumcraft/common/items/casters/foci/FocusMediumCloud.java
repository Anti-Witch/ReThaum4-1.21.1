package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.projectile.EntityFocusCloud;

/**
 * Cloud Medium - Creates an area effect cloud that applies effects to entities within.
 * The cloud persists for a duration and has a configurable radius.
 */
public class FocusMediumCloud extends FocusMedium {

    @Override
    public String getResearch() {
        return "FOCUSCLOUD";
    }

    @Override
    public String getKey() {
        return "thaumcraft.CLOUD";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ALCHEMY;
    }

    @Override
    public int getComplexity() {
        return 4 + getSettingValue("radius") * 2 + getSettingValue("duration") / 5;
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }
    
    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        FocusPackage remainingPackage = getRemainingPackage();
        
        if (remainingPackage == null || getPackage() == null || getPackage().world == null) {
            return false;
        }
        
        float radius = getSettingValue("radius");
        int duration = getSettingValue("duration");

        // Spawn the area effect cloud entity
        EntityFocusCloud cloud = new EntityFocusCloud(remainingPackage, trajectory, radius, duration);
        return getPackage().world.addFreshEntity(cloud);
    }

    @Override
    public boolean hasIntermediary() {
        // Cloud has an intermediary step (the cloud persisting)
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
            new NodeSetting("radius", "focus.common.radius", 
                new NodeSetting.NodeSettingIntRange(1, 3)),
            new NodeSetting("duration", "focus.common.duration", 
                new NodeSetting.NodeSettingIntRange(5, 30))
        };
    }

    @Override
    public float getPowerMultiplier() {
        // Cloud effects are weaker since they hit multiple times
        return 0.5f;
    }
}




