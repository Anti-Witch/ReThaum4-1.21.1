package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GUI particle effect - particles that exist only while a GUI is open.
 * Automatically expires when the game gains focus (GUI closes).
 */
@OnlyIn(Dist.CLIENT)
public class FXGenericGui extends FXGeneric {
    
    private boolean wasInGui;
    
    public FXGenericGui(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.wasInGui = Minecraft.getInstance().screen != null;
    }
    
    public FXGenericGui(ClientLevel level, double x, double y, double z, 
                        double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.wasInGui = Minecraft.getInstance().screen != null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // If we were in a GUI but now the screen is null (closed), expire
        if (this.wasInGui && Minecraft.getInstance().screen == null) {
            this.remove();
        }
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}




