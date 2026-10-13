package thaumcraft.api.casters;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Central registry and engine for focus elements.
 * Handles registration, lookup, and color mapping for all focus nodes.
 */
public class FocusEngine {
    
    /** Registry of all focus node factories by key */
    private static final Map<String, Supplier<? extends FocusNode>> FOCUS_REGISTRY = new HashMap<>();
    
    /** 
     * Legacy compatible map - provides direct access to focus element classes.
     * Keys are focus keys (e.g., "thaumcraft.FIRE"), values are the FocusNode classes.
     */
    public static final Map<String, Class<? extends FocusNode>> elements = new HashMap<>();
    
    /** Color mappings for focus elements (for visual effects) */
    private static final Map<String, Integer> ELEMENT_COLORS = new HashMap<>();
    
    /** Default colors for aspect-based elements */
    private static final Map<Aspect, Integer> ASPECT_COLORS = new HashMap<>();
    
    static {
        // Initialize default aspect colors
        initAspectColors();
    }
    
    /**
     * Register a focus node type.
     * @param key The unique key for this focus type (e.g., "thaumcraft.FIRE")
     * @param factory A supplier that creates new instances of this focus node
     */
    public static void registerFocusNode(String key, Supplier<? extends FocusNode> factory) {
        FOCUS_REGISTRY.put(key, factory);
        // Also update legacy elements map
        FocusNode sample = factory.get();
        if (sample != null) {
            elements.put(key, sample.getClass());
        }
    }
    
    /**
     * Register a focus node type with a custom color.
     */
    public static void registerFocusNode(String key, Supplier<? extends FocusNode> factory, int color) {
        FOCUS_REGISTRY.put(key, factory);
        ELEMENT_COLORS.put(key, color);
        // Also update legacy elements map
        FocusNode sample = factory.get();
        if (sample != null) {
            elements.put(key, sample.getClass());
        }
    }
    
    /**
     * Register a focus node type using its class directly.
     * @param key The unique key for this focus type
     * @param nodeClass The class of the focus node
     */
    public static void registerFocusNode(String key, Class<? extends FocusNode> nodeClass) {
        elements.put(key, nodeClass);
        FOCUS_REGISTRY.put(key, () -> {
            try {
                return nodeClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Create a new instance of a focus node by key.
     * @param key The focus key
     * @return A new focus node instance, or null if not found
     */
    @Nullable
    public static FocusNode createFocusNode(String key) {
        Supplier<? extends FocusNode> factory = FOCUS_REGISTRY.get(key);
        if (factory != null) {
            return factory.get();
        }
        return null;
    }
    
    /**
     * Check if a focus key is registered.
     */
    public static boolean isRegistered(String key) {
        return FOCUS_REGISTRY.containsKey(key);
    }
    
    /**
     * Get all registered focus keys.
     */
    public static Iterable<String> getRegisteredKeys() {
        return FOCUS_REGISTRY.keySet();
    }
    
    /**
     * Get the visual color for a focus element.
     * Falls back to aspect color if no specific color is set.
     */
    public static int getElementColor(String key) {
        // Check for explicit color mapping
        if (ELEMENT_COLORS.containsKey(key)) {
            return ELEMENT_COLORS.get(key);
        }
        
        // Try to get color from the focus node's aspect
        FocusNode node = createFocusNode(key);
        if (node != null) {
            Aspect aspect = node.getAspect();
            if (aspect != null && ASPECT_COLORS.containsKey(aspect)) {
                return ASPECT_COLORS.get(aspect);
            }
        }
        
        // Default color (white)
        return 0xFFFFFF;
    }
    
    /**
     * Set a custom color for a focus element.
     */
    public static void setElementColor(String key, int color) {
        ELEMENT_COLORS.put(key, color);
    }
    
    /**
     * Get the color for an aspect.
     */
    public static int getAspectColor(Aspect aspect) {
        return ASPECT_COLORS.getOrDefault(aspect, 0xFFFFFF);
    }
    
    /**
     * Initialize default aspect colors.
     */
    private static void initAspectColors() {
        // Primal aspects
        ASPECT_COLORS.put(Aspect.AIR, 0xFFFF7E);
        ASPECT_COLORS.put(Aspect.EARTH, 0x56C000);
        ASPECT_COLORS.put(Aspect.FIRE, 0xFF5A01);
        ASPECT_COLORS.put(Aspect.WATER, 0x3CD4FC);
        ASPECT_COLORS.put(Aspect.ORDER, 0xD5D4EC);
        ASPECT_COLORS.put(Aspect.ENTROPY, 0x404040);
        
        // Compound aspects
        ASPECT_COLORS.put(Aspect.LIFE, 0xDE0005);
        ASPECT_COLORS.put(Aspect.DEATH, 0x6A0005);
        ASPECT_COLORS.put(Aspect.COLD, 0x00BFFF);
        ASPECT_COLORS.put(Aspect.FLUX, 0x800080);
        ASPECT_COLORS.put(Aspect.MOTION, 0xCDCCF4);
        ASPECT_COLORS.put(Aspect.ENERGY, 0xFFFF00);
        ASPECT_COLORS.put(Aspect.AVERSION, 0xC05050);
        ASPECT_COLORS.put(Aspect.ALCHEMY, 0x23AC64);
        ASPECT_COLORS.put(Aspect.TRAP, 0x9A8080);
        ASPECT_COLORS.put(Aspect.CRAFT, 0x6991C7);
        ASPECT_COLORS.put(Aspect.EXCHANGE, 0x87CEEB);
        ASPECT_COLORS.put(Aspect.ELDRITCH, 0xAA00AA);
    }
    
    /**
     * Combine multiple colors for mixed effects.
     */
    public static int combineColors(int... colors) {
        if (colors.length == 0) return 0xFFFFFF;
        if (colors.length == 1) return colors[0];
        
        int r = 0, g = 0, b = 0;
        for (int color : colors) {
            Color c = new Color(color);
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        
        r /= colors.length;
        g /= colors.length;
        b /= colors.length;
        
        return new Color(r, g, b).getRGB();
    }
    
    /**
     * Clear all registrations (for reloading).
     */
    public static void clearRegistry() {
        FOCUS_REGISTRY.clear();
        ELEMENT_COLORS.clear();
        elements.clear();
    }
    
    /**
     * Get the icon texture for a focus element.
     * @param key The focus element key
     * @return ResourceLocation for the icon texture
     */
    public static ResourceLocation getElementIcon(String key) {
        // Icons are stored in textures/foci/ directory
        // Key format: "thaumcraft.FIRE" -> texture: "thaumcraft:textures/foci/fire.png"
        String[] parts = key.split("\\.");
        if (parts.length >= 2) {
            String modid = parts[0].toLowerCase();
            String name = parts[1].toLowerCase();
            return ResourceLocation.fromNamespaceAndPath(modid, "textures/foci/" + name + ".png");
        }
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/_unknown.png");
    }
    
    /**
     * Get a focus element (creates a new instance) by key.
     * This is an alias for createFocusNode() for backwards compatibility.
     */
    @Nullable
    public static IFocusElement getElement(String key) {
        return createFocusNode(key);
    }
    
    /**
     * Check if a FocusPackage contains a specific element type.
     * Searches through all nodes in the package.
     * 
     * @param pack The focus package to search
     * @param elementKey The element key to look for (e.g., "thaumcraft.PLAN")
     * @return true if the element is found anywhere in the package
     */
    public static boolean doesPackageContainElement(FocusPackage pack, String elementKey) {
        if (pack == null || elementKey == null || pack.nodes == null) {
            return false;
        }
        
        for (IFocusElement element : pack.nodes) {
            if (element != null && elementKey.equals(element.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Execute a focus package.
     * @param caster The entity casting the spell
     * @param pack The focus package to execute
     * @return true if execution started successfully
     */
    public static boolean castFocusPackage(LivingEntity caster, FocusPackage pack) {
        if (pack == null || caster == null) return false;
        
        // Set context
        pack.setCaster(caster);
        pack.world = caster.level();
        
        if (pack.nodes.isEmpty()) return false;
        
        // TC4 FocusEngine.runFocusPackage binds every node to the package before
        // executing it. Nodes that are not bound can neither supply targets nor
        // spawn a projectile (getPackage()/getRemainingPackage() are null).
        bindNodes(pack);

        // TC4 FocusEngine.castFocusPackage: every effect is notified that the
        // spell was cast. This is where the per-effect casting sounds live.
        for (FocusEffect effect : pack.getFocusEffects()) {
            effect.onCast(caster);
        }
        
        // Find root node
        IFocusElement root = pack.nodes.get(0);
        if (root instanceof FocusMedium medium) {
            if (root instanceof FocusMediumRoot rootMedium) {
                // Root mediums (touch, bolt) generate their own targets and pass
                // them to the rest of the chain. TC4 feeds the caster's look
                // trajectory into the chain and calls execute() on the medium
                // first - that call is what draws the cast beam / lightning zap.
                Vec3 look = caster.getLookAngle();
                Vec3 eyePos = caster.getEyePosition().subtract(0.0, 0.1, 0.0);
                rootMedium.execute(new Trajectory(eyePos, look));

                FocusPackage rest = ((FocusNode) root).getRemainingPackage();
                if (rest == null) {
                    return true;
                }
                runFocusPackage(rest, rootMedium.supplyTrajectories(), rootMedium.supplyTargets());
                return true;
            }
            // Create initial trajectory from caster look vector
            Vec3 look = caster.getLookAngle();
            Vec3 eyePos = caster.getEyePosition();
            Trajectory trajectory = new Trajectory(eyePos, look);
            return medium.execute(trajectory);
        }
        
        return false;
    }
    
    /**
     * Bind unbound focus nodes to the given package (TC4 contract - see
     * {@link FocusNode#getPackage()}).
     */
    private static void bindNodes(FocusPackage pack) {
        for (IFocusElement element : pack.nodes) {
            if (element instanceof FocusNode node && node.getPackage() == null) {
                node.setPackage(pack);
            }
        }
    }

    /**
     * Continue execution of a focus package.
     * @param pack The focus package (remaining part)
     * @param trajectories Trajectories supplied by previous node
     * @param targets Targets supplied by previous node
     */
    public static void runFocusPackage(FocusPackage pack, Trajectory[] trajectories, HitResult[] targets) {
        if (pack == null || pack.nodes.isEmpty()) return;
        
        bindNodes(pack);
        
        IFocusElement element = pack.nodes.get(0);
        if (element instanceof FocusNode node) {
            if (node instanceof FocusEffect effect) {
                // Execute effect on targets
                if (targets != null) {
                    for (int i = 0; i < targets.length; i++) {
                        float power = 1.0f * pack.getPower();
                        effect.execute(targets[i], (trajectories != null && i < trajectories.length) ? trajectories[i] : null, power, i);
                    }
                }
            } else if (node instanceof FocusMedium medium) {
                // Execute medium with trajectories
                if (trajectories != null) {
                    for (Trajectory traj : trajectories) {
                        medium.execute(traj);
                    }
                }
            }
        }
    }
}




