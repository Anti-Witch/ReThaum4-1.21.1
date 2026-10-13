package thaumcraft.client.lib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

/**
 * Client-only helper methods that used to live on the common {@code Thaumcraft}
 * class.
 * <p>
 * IMPORTANT (NeoForge dist safety): the dedicated server must never load this
 * class. The common mod class previously declared {@code getClientWorld()} whose
 * bytecode verifies {@code Minecraft.level : ClientLevel}, which crashed the
 * dedicated server ("Attempted to load class ... ClientLevel for invalid dist
 * DEDICATED_SERVER"). Keep all client-touching helpers here and only call them
 * from client-side code paths.
 */
public final class ClientHelper {

    private ClientHelper() {
    }

    /**
     * The client world (client side only).
     */
    public static Level getClientWorld() {
        return Minecraft.getInstance().level;
    }

    /**
     * True while the shift key is held (client side only).
     */
    public static boolean isShiftKeyDown() {
        return Screen.hasShiftDown();
    }
}
