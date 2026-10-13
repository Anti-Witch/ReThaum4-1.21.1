package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;

import java.util.List;

/**
 * WarpEvents - Handles warp effects and warp-related events.
 * 
 * Ported from 1.12.2. Major warp effects include:
 * - Creepy sounds and visual distortions
 * - Negative potion effects (exhaustion, blindness, etc.)
 * - Eldritch guardian spawning
 * - Mind spider spawning (hallucination or real)
 * - Cultist portal spawning
 * 
 * API changes:
 * - EntityPlayer -> Player
 * - EntityPlayerMP -> ServerPlayer
 * - world.rand -> level.random
 * - world.spawnEntity -> level.addFreshEntity
 * - player.sendStatusMessage -> player.displayClientMessage
 * - I18n.translateToLocal -> Component.translatable
 * - PotionEffect -> MobEffectInstance
 * - MathHelper -> Mth
 * 
 * NOTE: Some effects require custom potions (PotionVisExhaust, PotionThaumarhia, etc.)
 * that haven't been ported yet. These will use vanilla effects as placeholders.
 */
public class WarpEvents {
    
    /**
     * Main warp event check - called every 2000 ticks for players.
     * Reduces temporary warp by 1 and may trigger warp effects based on total warp.
     */
    public static void checkWarpEvent(Player player) {
        if (player.level().isClientSide) return;
        
        IPlayerWarp wc = ThaumcraftCapabilities.getWarp(player);
        if (wc == null) return;
        
        // Reduce temporary warp by 1
        ThaumcraftApi.internalMethods.addWarpToPlayer(player, -1, IPlayerWarp.EnumWarpType.TEMPORARY);
        
        int tw = wc.get(IPlayerWarp.EnumWarpType.TEMPORARY);
        int nw = wc.get(IPlayerWarp.EnumWarpType.NORMAL);
        int pw = wc.get(IPlayerWarp.EnumWarpType.PERMANENT);
        int warp = tw + nw + pw;
        int actualwarp = pw + nw;
        int gearWarp = getWarpFromGear(player);
        warp += gearWarp;
        
        int warpCounter = wc.getCounter();
        int r = player.level().random.nextInt(100);
        
        // Check if we should trigger a warp effect
        if (warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter)) {
            warp = Math.min(100, (warp + warp + warpCounter) / 3);
            warpCounter -= (int) Math.max(5.0, Math.sqrt(warpCounter) * 2.0 - gearWarp * 2);
            wc.setCounter(warpCounter);
            
            int eff = player.level().random.nextInt(warp) + gearWarp;
            
            // Grinning Devil faceplate (mask 0): damps the severity of warp events
            // (TC4 WarpEvents: eff -= 2 + random.nextInt(4))
            ItemStack helm = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (helm.getItem() instanceof thaumcraft.common.items.armor.ItemFortressArmor
                    && thaumcraft.common.items.armor.ItemFortressArmor.getMaskType(helm) == 0) {
                eff -= 2 + player.level().random.nextInt(4);
            }
            
            // Send warp event packet (visual/audio distortion on client)
            // TODO: PacketHandler.sendTo(new PacketMiscEvent((byte)0), serverPlayer);
            
            if (eff > 0) {
                triggerWarpEffect(player, eff, warp, nw);
            }
            
            // Unlock research based on warp level
            if (actualwarp > 10 && !ThaumcraftCapabilities.knowsResearch(player, "BATHSALTS") 
                    && !ThaumcraftCapabilities.knowsResearch(player, "!BATHSALTS")) {
                player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(8)), true);
                ThaumcraftApi.internalMethods.completeResearch(player, "!BATHSALTS");
            }
            if (actualwarp > 25 && !ThaumcraftCapabilities.knowsResearch(player, "ELDRITCHMINOR")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "ELDRITCHMINOR");
            }
            if (actualwarp > 50 && !ThaumcraftCapabilities.knowsResearch(player, "ELDRITCHMAJOR")) {
                ThaumcraftApi.internalMethods.completeResearch(player, "ELDRITCHMAJOR");
            }
        }
        
        // Sync changes to client
        if (player instanceof ServerPlayer serverPlayer) {
            wc.sync(serverPlayer);
        }
    }
    
    /**
     * Trigger a warp effect based on the effect level.
     */
    private static void triggerWarpEffect(Player player, int eff, int warp, int nw) {
        Level level = player.level();
        
        if (eff <= 4) {
            // Creepy sound
            // TODO: Check config for nostress mode
            level.playSound(null, player.blockPosition(), SoundEvents.CREEPER_PRIMED, 
                    SoundSource.AMBIENT, 1.0f, 0.5f);
        } else if (eff <= 8) {
            // Random explosion sound nearby
            double rx = player.getX() + (level.random.nextFloat() - level.random.nextFloat()) * 10.0f;
            double ry = player.getY() + (level.random.nextFloat() - level.random.nextFloat()) * 10.0f;
            double rz = player.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * 10.0f;
            level.playSound(null, rx, ry, rz, SoundEvents.GENERIC_EXPLODE, 
                    SoundSource.AMBIENT, 4.0f, (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
        } else if (eff <= 12) {
            // Creepy message
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(11)), true);
        } else if (eff <= 16) {
            // Vis Exhaustion
            MobEffectInstance pe = new MobEffectInstance(ModEffects.VIS_EXHAUST, 5000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(1)), true);
        } else if (eff <= 20) {
            // Thaumarhia
            MobEffectInstance pe = new MobEffectInstance(ModEffects.THAUMARHIA, Math.min(32000, 10 * warp), 0, true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(15)), true);
        } else if (eff <= 24) {
            // Unnatural Hunger
            MobEffectInstance pe = new MobEffectInstance(ModEffects.UNNATURAL_HUNGER, 5000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(2)), true);
        } else if (eff <= 28) {
            // Creepy message 2
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(12)), true);
        } else if (eff <= 32) {
            // Light mist + possible guardian
            spawnMist(player, warp, 1);
        } else if (eff <= 36) {
            // Blurred Vision
            try {
                player.addEffect(new MobEffectInstance(ModEffects.BLURRED_VISION, Math.min(32000, 10 * warp), 0, true, true));
            } catch (Exception ignored) {}
        } else if (eff <= 40) {
            // Sun Scorned
            MobEffectInstance pe = new MobEffectInstance(ModEffects.SUN_SCORNED, 5000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(5)), true);
        } else if (eff <= 44) {
            // Mining fatigue
            try {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, Math.min(3, warp / 15), true, true));
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(9)), true);
        } else if (eff <= 48) {
            // Infectious Vis Exhaust
            MobEffectInstance pe = new MobEffectInstance(ModEffects.INFECTIOUS_VIS_EXHAUST, 6000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(1)), true);
        } else if (eff <= 52) {
            // Night vision
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true));
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(10)), true);
        } else if (eff <= 56) {
            // Death Gaze
            MobEffectInstance pe = new MobEffectInstance(ModEffects.DEATH_GAZE, 6000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(4)), true);
        } else if (eff <= 60) {
            // Hallucination spiders
            suddenlySpiders(player, warp, false);
        } else if (eff <= 64) {
            // Creepy message 3
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(13)), true);
        } else if (eff <= 68) {
            // Heavy mist + guardians
            spawnMist(player, warp, warp / 30);
        } else if (eff <= 72) {
            // Blindness
            try {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
            } catch (Exception ignored) {}
        } else if (eff == 76) {
            // Rare: reduce normal warp
            if (nw > 0) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, -1, IPlayerWarp.EnumWarpType.NORMAL);
            }
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(14)), true);
        } else if (eff <= 80) {
            // Heavy unnatural hunger
            MobEffectInstance pe = new MobEffectInstance(ModEffects.UNNATURAL_HUNGER, 6000, Math.min(3, warp / 15), true, true);
            try {
                player.addEffect(pe);
            } catch (Exception ignored) {}
            player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(2)), true);
        } else if (eff <= 88) {
            // Spawn cultist portal
            spawnPortal(player);
        } else if (eff <= 92) {
            // Real spiders!
            suddenlySpiders(player, warp, true);
        } else {
            // Maximum mist + many guardians
            spawnMist(player, warp, warp / 15);
        }
    }
    
    /**
     * Spawn mist effect and optional eldritch guardians.
     */
    private static void spawnMist(Player player, int warp, int guardian) {
        // TODO: Send mist packet to client
        // PacketHandler.sendTo(new PacketMiscEvent((byte)1), serverPlayer);
        
        if (guardian > 0) {
            guardian = Math.min(8, guardian);
            for (int a = 0; a < guardian; ++a) {
                spawnGuardian(player);
            }
        }
        player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(6)), true);
    }
    
    /**
     * Spawn a lesser cultist portal near the player.
     */
    private static void spawnPortal(Player player) {
        Level level = player.level();
        EntityCultistPortalLesser portal = new EntityCultistPortalLesser(ModEntities.CULTIST_PORTAL_LESSER.get(), level);
        
        int i = Mth.floor(player.getX());
        int j = Mth.floor(player.getY());
        int k = Mth.floor(player.getZ());
        
        for (int l = 0; l < 50; ++l) {
            int i2 = i + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            int j2 = j + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            int k2 = k + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            
            BlockPos pos = new BlockPos(i2, j2, k2);
            portal.setPos(i2 + 0.5, j2 + 1.0, k2 + 0.5);
            
            if (level.getBlockState(pos.below()).isSolidRender(level, pos.below()) 
                    && level.noCollision(portal) 
                    && !level.containsAnyLiquid(portal.getBoundingBox())) {
                portal.finalizeSpawn(
                        (net.minecraft.server.level.ServerLevel) level,
                        level.getCurrentDifficultyAt(pos),
                        net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED,
                        null);
                level.addFreshEntity(portal);
                player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(16)), true);
                break;
            }
        }
    }
    
    /**
     * Spawn an eldritch guardian near the player.
     */
    private static void spawnGuardian(Player player) {
        Level level = player.level();
        EntityEldritchGuardian guardian = new EntityEldritchGuardian(ModEntities.ELDRITCH_GUARDIAN.get(), level);
        
        int i = Mth.floor(player.getX());
        int j = Mth.floor(player.getY());
        int k = Mth.floor(player.getZ());
        
        for (int l = 0; l < 50; ++l) {
            int i2 = i + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            int j2 = j + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            int k2 = k + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
            
            BlockPos pos = new BlockPos(i2, j2, k2);
            
            if (level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                guardian.setPos(i2, j2, k2);
                
                if (level.noCollision(guardian) 
                        && !level.containsAnyLiquid(guardian.getBoundingBox())) {
                    guardian.setTarget(player);
                    level.addFreshEntity(guardian);
                    break;
                }
            }
        }
    }
    
    /**
     * Spawn mind spiders near the player.
     * @param real If true, spiders are hostile. If false, they're hallucinations.
     */
    private static void suddenlySpiders(Player player, int warp, boolean real) {
        Level level = player.level();
        int spawns = Math.min(50, warp);
        
        for (int a = 0; a < spawns; ++a) {
            EntityMindSpider spider = new EntityMindSpider(ModEntities.MIND_SPIDER.get(), level);
            
            int i = Mth.floor(player.getX());
            int j = Mth.floor(player.getY());
            int k = Mth.floor(player.getZ());
            boolean success = false;
            
            for (int l = 0; l < 50; ++l) {
                int i2 = i + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
                int j2 = j + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
                int k2 = k + Mth.nextInt(level.random, 7, 24) * Mth.nextInt(level.random, -1, 1);
                
                BlockPos pos = new BlockPos(i2, j2, k2);
                
                if (level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                    spider.setPos(i2, j2, k2);
                    
                    if (level.noCollision(spider) 
                            && !level.containsAnyLiquid(spider.getBoundingBox())) {
                        success = true;
                        break;
                    }
                }
            }
            
            if (success) {
                spider.setTarget(player);
                if (!real) {
                    spider.setViewer(player.getName().getString());
                    spider.setHarmless(true);
                }
                level.addFreshEntity(spider);
            }
        }
        player.displayClientMessage(Component.literal("В§5В§o" + getWarpText(7)), true);
    }
    
    /**
     * Check for Death Gaze effect and apply wither to nearby entities the player looks at.
     */
    public static void checkDeathGaze(Player player) {
        if (player.level().isClientSide) return;
        
        MobEffectInstance effect = player.getEffect(ModEffects.DEATH_GAZE);
        if (effect == null) return;
        
        int amplifier = effect.getAmplifier();
        int range = Math.min(8 + amplifier * 3, 24);
        
        Level level = player.level();
        AABB searchBox = player.getBoundingBox().inflate(range, range, range);
        List<Entity> entities = level.getEntities(player, searchBox);
        
        Vec3 playerLook = player.getLookAngle();
        Vec3 playerEyes = player.getEyePosition();
        
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!entity.isAlive()) continue;
            if (living.hasEffect(MobEffects.WITHER)) continue;
            
            // Check if player can see the entity
            if (!player.hasLineOfSight(entity)) continue;
            
            // Check if player is looking at the entity (within ~45 degree cone)
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(playerEyes).normalize();
            double dot = playerLook.dot(toEntity);
            if (dot < 0.75) continue; // Not looking at entity
            
            // Check PvP for players
            if (entity instanceof Player targetPlayer) {
                if (level.getServer() != null && !level.getServer().isPvpAllowed()) {
                    continue;
                }
            }
            
            // Apply wither and aggro
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            living.setLastHurtByPlayer(player);
            
            if (living instanceof PathfinderMob mob) {
                mob.setTarget(player);
            }
        }
    }
    
    /**
     * Get warp value from equipped gear.
     */
    private static int getWarpFromGear(Player player) {
        int w = PlayerEvents.getFinalWarp(player.getMainHandItem(), player);
        
        // Check armor
        for (ItemStack armor : player.getInventory().armor) {
            w += PlayerEvents.getFinalWarp(armor, player);
        }
        
        // TODO: Check baubles when Curios integration is added
        // IInventory baubles = BaublesApi.getBaubles(player);
        // for (int a = 0; a < baubles.getSizeInventory(); ++a) {
        //     w += PlayerEvents.getFinalWarp(baubles.getStackInSlot(a), player);
        // }
        
        return w;
    }
    
    /**
     * Get localized warp text message.
     */
    private static String getWarpText(int index) {
        // Map legacy index to existing localization keys (warp.text.1..16)
        return Component.translatable("warp.text." + index).getString();
    }
}




