package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.menu.DeconstructionTableMenu;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.menu.FocusPouchMenu;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.menu.HungryChestMenu;
import thaumcraft.common.menu.LogisticsMenu;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.menu.SealMenu;
import thaumcraft.common.menu.SmelterMenu;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.menu.GolemBuilderMenu;

/**
 * Registry for all Thaumcraft menu types.
 * Menus are the 1.20.1 equivalent of 1.12.2 Containers.
 * 
 * Each menu type must be paired with a Screen class registered
 * in ClientModEvents.onClientSetup().
 */
public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
            DeferredRegister.create(Registries.MENU, Thaumcraft.MODID);
    
    // ==================== Golem System ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<GolemBuilderMenu>> GOLEM_BUILDER = 
            MENU_TYPES.register("golem_builder", 
                    () -> IMenuTypeExtension.create(GolemBuilderMenu::new));
    
    public static final DeferredHolder<MenuType<?>, MenuType<SealMenu>> SEAL = 
            MENU_TYPES.register("seal", 
                    () -> IMenuTypeExtension.create(SealMenu::new));
    
    // ==================== Crafting ====================

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH =
            MENU_TYPES.register("arcane_workbench",
                    () -> IMenuTypeExtension.create(ArcaneWorkbenchMenu::new));


    public static final DeferredHolder<MenuType<?>, MenuType<ThaumatoriumMenu>> THAUMATORIUM = 
            MENU_TYPES.register("thaumatorium", 
                    () -> IMenuTypeExtension.create(ThaumatoriumMenu::new));
    
    // ==================== Research ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE = 
            MENU_TYPES.register("research_table", 
                    () -> IMenuTypeExtension.create(ResearchTableMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DeconstructionTableMenu>> DECONSTRUCTION_TABLE =
            MENU_TYPES.register("deconstruction_table",
                    () -> IMenuTypeExtension.create(DeconstructionTableMenu::new));
    
    // ==================== Essentia Processing ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<SmelterMenu>> SMELTER = 
            MENU_TYPES.register("smelter", 
                    () -> IMenuTypeExtension.create(SmelterMenu::new));
    
    public static final DeferredHolder<MenuType<?>, MenuType<FocalManipulatorMenu>> FOCAL_MANIPULATOR =
            MENU_TYPES.register("focal_manipulator",
                    () -> IMenuTypeExtension.create(FocalManipulatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FocusPouchMenu>> FOCUS_POUCH =
            MENU_TYPES.register("focus_pouch",
                    () -> IMenuTypeExtension.create(FocusPouchMenu::new));
    
    // ==================== Tools ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<HandMirrorMenu>> HAND_MIRROR = 
            MENU_TYPES.register("hand_mirror", 
                    () -> IMenuTypeExtension.create(HandMirrorMenu::new));
    
    // ==================== Storage ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<HungryChestMenu>> HUNGRY_CHEST = 
            MENU_TYPES.register("hungry_chest", 
                    () -> IMenuTypeExtension.create(HungryChestMenu::new));
    
    // ==================== Devices ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<SpaMenu>> SPA = 
            MENU_TYPES.register("spa", 
                    () -> IMenuTypeExtension.create(SpaMenu::new));
    
    // ==================== Constructs ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneBoreMenu>> ARCANE_BORE = 
            MENU_TYPES.register("arcane_bore", 
                    () -> IMenuTypeExtension.create(ArcaneBoreMenu::new));
    
    // ==================== Trading ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<PechMenu>> PECH_TRADING = 
            MENU_TYPES.register("pech_trading", 
                    () -> IMenuTypeExtension.create(PechMenu::new));
    
    // ==================== Logistics ====================
    
    public static final DeferredHolder<MenuType<?>, MenuType<LogisticsMenu>> LOGISTICS = 
            MENU_TYPES.register("logistics", 
                    () -> IMenuTypeExtension.create(LogisticsMenu::new));
}







