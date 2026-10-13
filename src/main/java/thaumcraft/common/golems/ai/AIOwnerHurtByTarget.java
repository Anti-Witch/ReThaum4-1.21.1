package thaumcraft.common.golems.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

import java.util.EnumSet;

/**
 * AIOwnerHurtByTarget - Target goal that makes the golem attack
 * whatever entity last attacked the owner.
 * 
 * This allows golems to defend their owner by targeting
 * entities that harm them.
 * 
 * Ported from 1.12.2.
 */
public class AIOwnerHurtByTarget extends TargetGoal {
    
    private final EntityOwnedConstruct ownedEntity;
    private LivingEntity ownerAttacker;
    private int timestamp;
    
    public AIOwnerHurtByTarget(EntityOwnedConstruct entity) {
        super(entity, false);
        this.ownedEntity = entity;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }
    
    @Override
    public boolean canUse() {
        if (!ownedEntity.isOwned()) {
            return false;
        }
        
        LivingEntity owner = ownedEntity.getOwner();
        if (owner == null) {
            return false;
        }
        
        // Get the entity that last attacked the owner
        ownerAttacker = owner.getLastHurtByMob();
        int lastHurtTime = owner.getLastHurtByMobTimestamp();
        
        // Only target if this is a new attack (timestamp changed) and target is valid
        return lastHurtTime != timestamp && canAttack(ownerAttacker, TargetingConditions.DEFAULT);
    }
    
    @Override
    public void start() {
        mob.setTarget(ownerAttacker);
        
        LivingEntity owner = ownedEntity.getOwner();
        if (owner != null) {
            timestamp = owner.getLastHurtByMobTimestamp();
        }
        
        super.start();
    }
}




