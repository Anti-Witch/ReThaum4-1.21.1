package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.projectile.EntityFocusMine;

/**
 * Mine Medium - Places a proximity mine that triggers when entities approach.
 * Can be configured to target enemies or friendlies.
 */
public class FocusMediumMine extends FocusMedium {

    /** Target hostile entities */
    public static final int TARGET_ENEMY = 0;
    /** Target friendly entities */
    public static final int TARGET_FRIEND = 1;

    @Override
    public String getResearch() {
        return "FOCUSMINE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.MINE";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.TRAP;
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY };
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
        
        boolean targetFriendly = getSettingValue("target") == TARGET_FRIEND;

        // Spawn the proximity mine entity
        EntityFocusMine mine = new EntityFocusMine(remainingPackage, trajectory, targetFriendly);
        return getPackage().world.addFreshEntity(mine);
    }

    @Override
    public boolean hasIntermediary() {
        // Mine has an intermediary step (waiting to trigger)
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] target = { TARGET_ENEMY, TARGET_FRIEND };
        String[] targetDesc = { "focus.common.enemy", "focus.common.friend" };
        
        return new NodeSetting[] {
            new NodeSetting("target", "focus.common.target", 
                new NodeSetting.NodeSettingIntList(target, targetDesc))
        };
    }
}




