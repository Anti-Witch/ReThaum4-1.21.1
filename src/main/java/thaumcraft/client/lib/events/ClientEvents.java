package thaumcraft.client.lib.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;

/**
 * Client-side event handlers for Thaumcraft.
 * Registered on the FORGE event bus.
 * 
 * Ported to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid=Thaumcraft.MODID, value=Dist.CLIENT)
public class ClientEvents {
    
    /**
     * Handle client tick events.
     * Used for:
     * - Key input processing
     * - Client-side particle/effect updates
     * - HUD updates
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        // Process key bindings
        KeyHandler.onClientTick(event);
        
        // TODO: Add other client tick processing as needed
        // - Radial menu updates
        // - Goggle/HUD overlay updates
        // - Client-side particle systems
    }
    

}




