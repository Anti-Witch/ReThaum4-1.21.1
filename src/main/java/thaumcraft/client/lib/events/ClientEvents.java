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

        // TC4 ParticleEngine.updateParticles: spawn particles that were queued
        // with a delay (focus effects stagger their sprites this way).
        thaumcraft.client.fx.FXDispatcher.tickDelayedParticles();
    }
    

}




