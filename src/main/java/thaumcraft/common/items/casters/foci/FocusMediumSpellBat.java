package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.monster.EntitySpellBat;

/**
 * FocusMediumSpellBat - Summons a magical spell bat to deliver focus effects.
 * The bat can be set to friendly mode (targeting allies) or hostile mode (targeting enemies).
 */
public class FocusMediumSpellBat extends FocusMedium {
    
    @Override
    public String getResearch() {
        return "FOCUSSPELLBAT";
    }
    
    @Override
    public String getKey() {
        return "thaumcraft.SPELLBAT";
    }
    
    @Override
    public Aspect getAspect() {
        return Aspect.BEAST;
    }
    
    @Override
    public int getComplexity() {
        return 8;
    }
    
    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }
    
    @Override
    public boolean execute(Trajectory trajectory) {
        if (getPackage() == null || getPackage().getCaster() == null) {
            return false;
        }
        
        // Create remaining focus package for the bat to execute on hit
        FocusPackage remainingPackage = getRemainingPackage();
        boolean friendly = getSettingValue("target") == 1;
        
        EntitySpellBat bat = new EntitySpellBat(
                getPackage().getCaster().level(),
                remainingPackage,
                friendly
        );
        bat.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        
        return getPackage().getCaster().level().addFreshEntity(bat);
    }
    
    @Override
    public boolean hasIntermediary() {
        return true;
    }
    
    @Override
    public float getPowerMultiplier() {
        return 0.33f;
    }
    
    @Override
    public NodeSetting[] createSettings() {
        int[] targetModes = { 0, 1 };
        String[] targetDescs = { "focus.common.enemy", "focus.common.friend" };
        return new NodeSetting[] { 
            new NodeSetting("target", "focus.common.target", 
                new NodeSetting.NodeSettingIntList(targetModes, targetDescs)) 
        };
    }
}




