package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;

/**
 * Registry for all Thaumcraft sound events.
 * Uses DeferredRegister for 1.20.1 Forge.
 * 
 * Sound files are in assets/thaumcraft/sounds/
 * Sound definitions are in assets/thaumcraft/sounds.json
 */
public class ModSounds {
    
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
            DeferredRegister.create(Registries.SOUND_EVENT, Thaumcraft.MODID);
    
    // ==================== Player Sounds ====================
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = registerSound("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNIC_SHIELD_EFFECT = registerSound("runicshieldeffect");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNIC_SHIELD_CHARGE = registerSound("runicshieldecharge");
    
    // ==================== Block Sounds ====================
    public static final DeferredHolder<SoundEvent, SoundEvent> SPILL = registerSound("spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUST = registerSound("dust");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE = registerSound("bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREAK = registerSound("creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUEEK = registerSound("squeek");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAR = registerSound("jar");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP = registerSound("pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL = registerSound("crystal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GORE = registerSound("gore");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFUSER = registerSound("infuser");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFUSER_START = registerSound("infuserstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> URN_BREAK = registerSound("urnbreak");
    public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_PORTAL = registerSound("evilportal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIND = registerSound("grind");
    
    // ==================== Ambient Sounds ====================
    public static final DeferredHolder<SoundEvent, SoundEvent> FLY = registerSound("fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> KEY = registerSound("key");
    public static final DeferredHolder<SoundEvent, SoundEvent> TICKS = registerSound("ticks");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLACK = registerSound("clack");  // Golem ambient/hurt
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERACLACK = registerSound("cameraclack");  // Research table button (TC4)
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERATICKS = registerSound("cameraticks");  // Thaumometer scanning (TC4)
    public static final DeferredHolder<SoundEvent, SoundEvent> POOF = registerSound("poof");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRAIN = registerSound("brain");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUMBLE = registerSound("rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> JACOBS = registerSound("jacobs");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND = registerSound("wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPERS = registerSound("whispers");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONOLITH = registerSound("monolith");
    
    // ==================== Master/UI Sounds ====================
    public static final DeferredHolder<SoundEvent, SoundEvent> PAGE = registerSound("page");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAGE_TURN = registerSound("pageturn");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEARN = registerSound("learn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WRITE = registerSound("write");
    public static final DeferredHolder<SoundEvent, SoundEvent> ERASE = registerSound("erase");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAND = registerSound("wand");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAND_FAIL = registerSound("wandfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE = registerSound("ice");
    public static final DeferredHolder<SoundEvent, SoundEvent> HH_OFF = registerSound("hhoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> HH_ON = registerSound("hhon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCK = registerSound("shock");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAP = registerSound("zap");  // Golem interaction
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFT_FAIL = registerSound("craftfail");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN = registerSound("scan");  // Golem mode toggle
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAFT_START = registerSound("craftstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOOL = registerSound("tool");  // Golem death
    public static final DeferredHolder<SoundEvent, SoundEvent> UPGRADE = registerSound("upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> COINS = registerSound("coins");
    
    // ==================== Hostile/Entity Sounds ====================
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM = registerSound("swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_ATTACK = registerSound("swarmattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_DEAD = registerSound("wispdead");
    public static final DeferredHolder<SoundEvent, SoundEvent> WISP_LIVE = registerSound("wisplive");
    public static final DeferredHolder<SoundEvent, SoundEvent> TENTACLE = registerSound("tentacle");
    
    // Pech Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_IDLE = registerSound("pech_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_TRADE = registerSound("pech_trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_DICE = registerSound("pech_dice");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_HIT = registerSound("pech_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_DEATH = registerSound("pech_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PECH_CHARGE = registerSound("pech_charge");
    
    // Eldritch Guardian Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> EG_IDLE = registerSound("egidle");
    public static final DeferredHolder<SoundEvent, SoundEvent> EG_ATTACK = registerSound("egattack");
    public static final DeferredHolder<SoundEvent, SoundEvent> EG_DEATH = registerSound("egdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> EG_SCREECH = registerSound("egscreech");
    
    // Taint Crab Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_CLAW = registerSound("crabclaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_DEATH = registerSound("crabdeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_TALK = registerSound("crabtalk");
    
    // Cultist Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> CHANT = registerSound("chant");
    
    // ==================== Helper Method ====================
    
    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name)));
    }
    
    // ==================== Custom SoundTypes for Blocks ====================
    // These must be initialized lazily since they reference DeferredHolders
    // Use DeferredSoundType which accepts suppliers
    
    public static final SoundType GORE_TYPE = new DeferredSoundType(0.5f, 1.0f, 
            GORE::get, GORE::get, GORE::get, GORE::get, GORE::get);
    
    public static final SoundType CRYSTAL_TYPE = new DeferredSoundType(0.5f, 1.0f, 
            CRYSTAL::get, CRYSTAL::get, CRYSTAL::get, CRYSTAL::get, CRYSTAL::get);
    
    public static final SoundType JAR_TYPE = new DeferredSoundType(0.5f, 1.0f, 
            JAR::get, JAR::get, JAR::get, JAR::get, JAR::get);
    
    public static final SoundType URN_TYPE = new DeferredSoundType(0.5f, 1.5f, 
            URN_BREAK::get, URN_BREAK::get, URN_BREAK::get, URN_BREAK::get, URN_BREAK::get);
}








