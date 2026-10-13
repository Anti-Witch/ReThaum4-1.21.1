package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;

/**
 * A scan thing that checks for entities of a specific class.
 */
public class ScanEntity implements IScanThing {
    
    private final String research;
    private final Class<?> entityClass;
    private final boolean inheritedClasses;
    
    /**
     * Create a new entity scanner.
     * @param research The research key to unlock
     * @param entityClass The entity class to match
     * @param inheritedClasses If true, matches any entity that inherits from the class
     */
    public ScanEntity(String research, Class<?> entityClass, boolean inheritedClasses) {
        this.research = research;
        this.entityClass = entityClass;
        this.inheritedClasses = inheritedClasses;
    }
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;
        
        if (!inheritedClasses) {
            return entityClass == obj.getClass();
        } else {
            return entityClass.isInstance(obj);
        }
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        return research;
    }
}




