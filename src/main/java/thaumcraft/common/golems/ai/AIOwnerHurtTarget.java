package thaumcraft.common.golems.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

import java.util.EnumSet;

/**
 * AIOwnerHurtTarget - Target goal that makes the golem attack
 * whatever entity the owner last attacked.
 * 
 * This allows golems to assist their owner in combat by targeting
 * the same enemy the owner is fighting.
 * 
 * Ported from 1.12.2.
 */
public class AIOwnerHurtTarget extends TargetGoal {
    
    private final EntityOwnedConstruct ownedEntity;
    private LivingEntity ownerLastHurt;
    private int timestamp;
    
    public AIOwnerHurtTarget(EntityOwnedConstruct entity) {
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
        
        // Get the entity the owner last attacked
        ownerLastHurt = owner.getLastHurtMob();
        int lastHurtTime = owner.getLastHurtMobTimestamp();
        
        // Only target if this is a new attack (timestamp changed) and target is valid
        return lastHurtTime != timestamp && canAttack(ownerLastHurt, TargetingConditions.DEFAULT);
    }
    
    @Override
    public void start() {
        mob.setTarget(ownerLastHurt);
        
        LivingEntity owner = ownedEntity.getOwner();
        if (owner != null) {
            timestamp = owner.getLastHurtMobTimestamp();
        }
        
        super.start();
    }
}




