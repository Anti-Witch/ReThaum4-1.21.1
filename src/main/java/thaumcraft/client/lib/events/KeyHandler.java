package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.golems.ItemGolemBell;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;

/**
 * Handles Thaumcraft-specific keybindings on the client side.
 * 
 * Keybindings:
 * - F key (keyF): Change caster focus / radial menu / golem bell toggle
 * - G key (keyG): Misc caster toggle / elemental shovel orientation
 * 
 * Ported to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
public class KeyHandler {
    
    // Key category for Thaumcraft
    public static final String KEY_CATEGORY = "key.categories.thaumcraft";
    
    // Key mappings
    public static KeyMapping keyF;
    public static KeyMapping keyG;
    
    // State tracking
    private static boolean keyPressedF = false;
    private static boolean keyPressedG = false;
    
    // Radial menu state
    public static boolean radialActive = false;
    public static boolean radialLock = false;
    
    // Timestamps for key presses (for detecting hold vs tap)
    public static long lastPressF = 0L;
    public static long lastPressG = 0L;
    
    /**
     * Initialize key mappings. Call this during mod construction.
     */
    public static void init() {
        // Create key mappings with GLFW key codes
        // F key = GLFW_KEY_F = 70 (was Keyboard.KEY_F = 33 in LWJGL2)
        // G key = GLFW_KEY_G = 71 (was Keyboard.KEY_G = 34 in LWJGL2)
        keyF = new KeyMapping(
                "key.thaumcraft.focus",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                KEY_CATEGORY
        );
        
        keyG = new KeyMapping(
                "key.thaumcraft.misc",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY
        );
    }
    
    /**
     * Register key mappings with Forge.
     * Called during RegisterKeyMappingsEvent (on the mod event bus).
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (keyF == null || keyG == null) {
            init();
        }
        event.register(keyF);
        event.register(keyG);
        Thaumcraft.LOGGER.info("Registered Thaumcraft key mappings");
    }
    
    /**
     * Handle client tick for key input processing.
     * This runs every client tick to check key states.
     */
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        
        // Only process when in-game and focused
        if (mc.player == null || mc.screen != null) {
            // Reset states when not in game or a screen is open
            if (keyPressedF) {
                radialActive = false;
                keyPressedF = false;
            }
            if (keyPressedG) {
                keyPressedG = false;
            }
            return;
        }
        
        Player player = mc.player;
        
        // Process F key (focus change / radial menu)
        processKeyF(player);
        
        // Process G key (misc toggle)
        processKeyG(player);
    }
    
    /**
     * Process the F key for focus changing and radial menu.
     * Tap F (short press) cycles to the next focus; hold F shows the radial
     * menu; Shift+F removes the current focus.
     */
    private static void processKeyF(Player player) {
        if (keyF == null) return;

        if (keyF.isDown()) {
            if (!keyPressedF) {
                // Key just pressed
                lastPressF = System.currentTimeMillis();
                radialLock = false;

                ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

                boolean hasCaster = (!mainHand.isEmpty() && mainHand.getItem() instanceof ICaster) ||
                                   (!offHand.isEmpty() && offHand.getItem() instanceof ICaster);

                if (!hasCaster && !mainHand.isEmpty() && mainHand.getItem() instanceof ItemGolemBell) {
                    // F tap while holding golem bell
                    PacketHandler.sendToServer(new PacketItemKeyToServer(0));
                } else if (hasCaster && player.isShiftKeyDown()) {
                    // Shift+F: Remove focus
                    PacketHandler.sendToServer(new PacketFocusChangeToServer("REMOVE"));
                } else if (hasCaster) {
                    // F held: show radial menu
                    radialActive = true;
                }
            }
            keyPressedF = true;
        } else {
            // Key released
            radialActive = false;
            if (keyPressedF) {
                long pressDuration = System.currentTimeMillis() - lastPressF;
                lastPressF = System.currentTimeMillis();

                // Short tap (<250ms): cycle to the next focus
                if (pressDuration < 250L && !player.isShiftKeyDown()) {
                    ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                    ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
                    ItemStack casterStack = (!mainHand.isEmpty() && mainHand.getItem() instanceof ICaster)
                            ? mainHand
                            : (!offHand.isEmpty() && offHand.getItem() instanceof ICaster ? offHand : ItemStack.EMPTY);

                    if (!casterStack.isEmpty()) {
                        ICaster caster = (ICaster) casterStack.getItem();
                        String focusKey = "";
                        ItemStack current = caster.getFocusStack(casterStack);
                        if (current != null && !current.isEmpty()
                                && current.getItem() instanceof thaumcraft.common.items.casters.ItemFocus focus) {
                            String sh = focus.getSortingHelper(current);
                            if (sh != null) {
                                focusKey = sh;
                            }
                        }
                        PacketHandler.sendToServer(new PacketFocusChangeToServer(focusKey));
                    }
                }
            }
            keyPressedF = false;
        }
    }
    
    /**
     * Process the G key for misc toggles.
     */
    private static void processKeyG(Player player) {
        if (keyG == null) return;
        
        if (keyG.isDown()) {
            if (!keyPressedF) {
                // Key just pressed - send packet with modifier state
                lastPressG = System.currentTimeMillis();
                
                // Check modifier keys
                // In 1.20.1, we use Screen.hasControlDown() and Screen.hasShiftDown()
                // but those require a screen context. Use GLFW directly instead.
                long windowHandle = Minecraft.getInstance().getWindow().getWindow();
                boolean ctrlDown = InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                                   InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean shiftDown = InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                    InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);
                
                int modifier = ctrlDown ? 1 : (shiftDown ? 2 : 0);
                PacketHandler.sendToServer(new PacketItemKeyToServer(1, modifier));
            }
            keyPressedG = true;
        } else {
            // Key released
            if (keyPressedG) {
                lastPressG = System.currentTimeMillis();
            }
            keyPressedG = false;
        }
    }
    
    /**
     * Check if the radial menu should be displayed.
     */
    public static boolean isRadialMenuActive() {
        return radialActive && !radialLock;
    }
    
    /**
     * Lock the radial menu in place (user made a selection).
     */
    public static void lockRadialMenu() {
        radialLock = true;
        radialActive = false;
    }
}




