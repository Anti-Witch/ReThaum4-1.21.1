package thaumcraft.common.lib.events;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * EntityEvents - Handles entity-related events.
 * 
 * Features:
 * - Bath salts creating purifying fluid when expired in water
 * - Research triggers from damage types (fire, projectiles)
 * - Fortress armor mask effects (life leech, wither)
 * - Runic shield visual effects
 * - Champion mob system (TODO: needs full port)
 * - Zombie brain drops
 * - Dissolve damage dropping crystals
 * - Preventing fake player item pickup
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EntityEvents {

    /**
     * Handle bath salts expiring in water - create purifying fluid.
     */
    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity itemEntity = event.getEntity();
        if (itemEntity == null) return;
        
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;
        
        // Check if it's bath salts
        if (ModItems.BATH_SALTS != null && stack.getItem() == ModItems.BATH_SALTS.get()) {
            BlockPos pos = itemEntity.blockPosition();
            BlockState state = itemEntity.level().getBlockState(pos);
            
            // Check if in water source block
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                // TODO: Replace with purifying fluid when block is ported
                // if (ModBlocks.PURIFYING_FLUID != null) {
                //     itemEntity.level().setBlock(pos, ModBlocks.PURIFYING_FLUID.get().defaultBlockState(), 3);
                // }
            }
        }
    }

    /**
     * Handle living entity tick - champion mob effects.
     * TODO: Full champion mod system needs to be ported
     */
    @SubscribeEvent
    public static void onLivingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        // Champion mob tick effects would go here
        // For now, this is a placeholder for future implementation
    }

    /**
     * Handle entity hurt events.
     * - Trigger research from damage types
     * - Fortress armor effects
     * - Runic shield visuals
     */
    @SubscribeEvent
    public static void onEntityHurt(LivingIncomingDamageEvent event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();
        
        // Player-specific hurt handling
        if (entity instanceof Player player) {
            handlePlayerHurt(player, source, event.getAmount());
        }
        
        // Attacker-related effects
        Entity attacker = source.getEntity();
        if (attacker instanceof Player attackingPlayer) {
            handlePlayerAttack(attackingPlayer, event.getEntity(), event.getAmount());
        }
    }

    /**
     * Handle player being hurt - research triggers and armor effects.
     */
    private static void handlePlayerHurt(Player player, DamageSource source, float amount) {
        if (player.level().isClientSide) return;
        
        // Fire damage research trigger
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            if (ThaumcraftCapabilities.knowsResearchStrict(player, "BASEAUROMANCY@2") &&
                !ThaumcraftCapabilities.knowsResearch(player, "f_onfire")) {
                IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                if (knowledge != null) {
                    knowledge.addResearch("f_onfire");
                    if (player instanceof ServerPlayer serverPlayer) {
                        knowledge.sync(serverPlayer);
                    }
                    // TODO: Send status message about research discovery
                }
            }
        }
        
        // Projectile damage research triggers
        Entity directSource = source.getDirectEntity();
        if (directSource != null && ThaumcraftCapabilities.knowsResearchStrict(player, "FOCUSPROJECTILE@2")) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null) {
                if (directSource instanceof AbstractArrow && 
                    !ThaumcraftCapabilities.knowsResearch(player, "f_arrow")) {
                    knowledge.addResearch("f_arrow");
                    if (player instanceof ServerPlayer sp) knowledge.sync(sp);
                }
                if (directSource instanceof Fireball && 
                    !ThaumcraftCapabilities.knowsResearch(player, "f_fireball")) {
                    knowledge.addResearch("f_fireball");
                    if (player instanceof ServerPlayer sp) knowledge.sync(sp);
                }
            }
        }
        
        // Fortress armor wither mask effect
        // TODO: Check for fortress armor with wither mask
        
        // Runic shield effect visuals
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
            // TODO: Send PacketFXShield when we have it
        }
    }

    /**
     * Handle player attacking - fortress armor life leech.
     */
    private static void handlePlayerAttack(Player player, LivingEntity target, float damage) {
        // Fortress armor life leech mask
        // TODO: Check for fortress helm with leech mask and heal player
    }

    /**
     * Prevent fake players from picking up items.
     * Thaumcraft uses fake players for some automation.
     */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player != null && player.getName().getString().startsWith("FakeThaumcraft")) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /**
     * Handle living entity drops.
     * - Zombie brain drops
     * - Dissolve damage crystal drops
     * - Champion loot bags (TODO)
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        
        DamageSource source = event.getSource();
        boolean fakePlayer = source.getEntity() instanceof FakePlayer;

        // Looting level from the attacker's weapon (1.21: read from the ItemEnchantments data component)
        int looting = 0;
        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            ItemStack weapon = livingAttacker.getMainHandItem();
            ItemEnchantments enchants = weapon.get(DataComponents.ENCHANTMENTS);
            if (enchants != null) {
                Registry<Enchantment> enchReg = livingAttacker.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                looting = enchants.getLevel(enchReg.getHolderOrThrow(Enchantments.LOOTING));
            }
        }

        // Zombie brain drops
        if (entity instanceof Zombie && !(entity.getType().getDescriptionId().contains("brainy"))) {
            if (event.isRecentlyHit() && entity.level().random.nextInt(10) - looting < 1) {
                if (ModItems.ZOMBIE_BRAIN != null) {
                    ItemEntity brainDrop = new ItemEntity(
                            entity.level(),
                            entity.getX(),
                            entity.getY() + entity.getEyeHeight(),
                            entity.getZ(),
                            new ItemStack(ModItems.ZOMBIE_BRAIN.get()));
                    event.getDrops().add(brainDrop);
                }
            }
        }
        
        // Dissolve damage - drop aspect crystals
        if (source.getMsgId().equals("thaumcraft.dissolve")) {
            AspectList aspects = AspectHelper.getEntityAspects(entity);
            if (aspects != null && aspects.size() > 0) {
                Aspect[] aspectArray = aspects.getAspects();
                int dropCount = Math.min(1 + aspects.visSize() / 10, 5);
                dropCount = Math.max(1, entity.level().random.nextInt(dropCount + 1));
                
                for (int i = 0; i < dropCount && aspectArray.length > 0; i++) {
                    Aspect aspect = aspectArray[entity.level().random.nextInt(aspectArray.length)];
                    ItemStack crystal = ThaumcraftApiHelper.makeCrystal(aspect);
                    if (!crystal.isEmpty()) {
                        ItemEntity crystalDrop = new ItemEntity(
                                entity.level(),
                                entity.getX(),
                                entity.getY() + entity.getEyeHeight(),
                                entity.getZ(),
                                crystal);
                        event.getDrops().add(crystalDrop);
                    }
                }
            }
        }
        
        // Champion mob loot bags
        // TODO: Implement when champion system is ported

        // TC4 EventHandlerEntity.livingTick(LivingDeathEvent): when a mob that
        // was killed by a player dies it releases primal aspect orbs. The mob's
        // registered aspects are reduced to primals (TC4
        // ResearchManager.reduceToPrimals) and each primal has a 50% chance to
        // spawn an EntityAspectOrb carrying 1..N vis of that aspect.
        spawnAspectOrbs(event);
    }

    /**
     * TC4 aspect orb drop on mob death.
     * <p>
     * Ported from OpenTC4 {@code EventHandlerEntity.livingTick(LivingDeathEvent)}:
     * <pre>
     * if (!world.isRemote &amp;&amp; EntityUtils.getRecentlyHit(mob) &gt; 0) {
     *    AspectList aspects = ResearchManager.reduceToPrimals(ScanManager.generateEntityAspects(mob));
     *    for (Aspect aspect : aspects.getAspects())
     *       if (world.rand.nextBoolean())
     *          spawn(new EntityAspectOrb(world, x, y, z, aspect, 1 + rand.nextInt(aspects.getAmount(aspect))));
     * }
     * </pre>
     * In 1.21 the "recently hit" flag is {@link LivingDropsEvent#isRecentlyHit()}.
     * Tainted mobs are excluded (they turn into tainted entities instead of
     * dropping loot, TC4 {@code !(mob instanceof ITaintedMob)}).
     */
    private static void spawnAspectOrbs(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player) {
            return;
        }
        if (!event.isRecentlyHit()) {
            return;
        }
        if (entity instanceof thaumcraft.api.entities.ITaintedMob) {
            return;
        }

        AspectList aspectsCompound = AspectHelper.getEntityAspects(entity);
        if (aspectsCompound == null || aspectsCompound.size() == 0) {
            return;
        }

        AspectList aspects = thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(aspectsCompound);
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null) {
                continue;
            }
            if (entity.level().random.nextBoolean()) {
                int value = 1 + entity.level().random.nextInt(Math.max(1, aspects.getAmount(aspect)));
                thaumcraft.common.entities.EntityAspectOrb orb =
                        new thaumcraft.common.entities.EntityAspectOrb(
                                entity.level(),
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                aspect,
                                value);
                entity.level().addFreshEntity(orb);
            }
        }
    }

    /**
     * Handle entity spawning - champion mob assignment.
     * TODO: Full implementation requires champion modifier system
     */
    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        
        Entity entity = event.getEntity();
        if (!(entity instanceof Monster mob)) return;
        
        // Champion mob assignment would go here
        // This requires porting the ChampionModifier system
    }

    /**
     * Handle entity construction - register custom attributes.
     * TODO: Custom attributes for champion system
     */
    @SubscribeEvent
    public static void onEntityConstruct(EntityEvent.EntityConstructing event) {
        // Champion attributes would be registered here
        // This requires the full attribute system port
    }
}




