package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchEntry;

/**
 * ResearchToast - Toast notification shown when research is completed or unlocked.
 * 
 * Displays the research name and icon as a popup notification.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchToast implements Toast {
    
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/toasts.png");
    private static final ResourceLocation TC_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research_browser.png");
    
    private static final long DISPLAY_TIME = 5000L; // 5 seconds
    
    private final ResearchEntry research;
    private final Component title;
    private final Component description;
    private final ItemStack icon;
    private long firstDrawTime;
    private boolean hasPlayedSound;
    
    /**
     * Create a toast for completing research.
     */
    public ResearchToast(ResearchEntry research) {
        this.research = research;
        this.title = Component.translatable("tc.research.toast.complete");
        this.description = research.getLocalizedName();
        
        // Get icon from research
        if (research.getIcons() != null && research.getIcons().length > 0) {
            Object iconObj = research.getIcons()[0];
            if (iconObj instanceof ItemStack stack) {
                this.icon = stack;
            } else {
                this.icon = ItemStack.EMPTY;
            }
        } else {
            this.icon = ItemStack.EMPTY;
        }
    }
    
    /**
     * Create a toast for discovering new research.
     */
    public static ResearchToast discovered(ResearchEntry research) {
        return new ResearchToast(research, 
                Component.translatable("tc.research.toast.discovered"),
                research.getLocalizedName());
    }
    
    private ResearchToast(ResearchEntry research, Component title, Component description) {
        this.research = research;
        this.title = title;
        this.description = description;
        
        // Get icon from research
        if (research.getIcons() != null && research.getIcons().length > 0) {
            Object iconObj = research.getIcons()[0];
            if (iconObj instanceof ItemStack stack) {
                this.icon = stack;
            } else {
                this.icon = ItemStack.EMPTY;
            }
        } else {
            this.icon = ItemStack.EMPTY;
        }
    }
    
    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        if (firstDrawTime == 0) {
            firstDrawTime = timeSinceLastVisible;
            // Play sound on first draw
            if (!hasPlayedSound) {
                // TODO: Play TC discovery sound
                // Minecraft.getInstance().getSoundManager().play(...)
                hasPlayedSound = true;
            }
        }
        
        // Draw toast background
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());
        
        // Draw purple tint for Thaumcraft flavor
        graphics.fill(0, 0, this.width(), this.height(), 0x20800080);
        
        // Draw title
        graphics.drawString(toastComponent.getMinecraft().font, title, 30, 7, 0x8000A0, false);
        
        // Draw research name
        graphics.drawString(toastComponent.getMinecraft().font, description, 30, 18, 0x404040, false);
        
        // Draw icon
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, 8, 8);
        } else {
            // Draw a placeholder icon
            graphics.fill(8, 8, 24, 24, 0x40800080);
        }
        
        // Check if we should hide
        long elapsed = timeSinceLastVisible - firstDrawTime;
        return elapsed >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }
    
    @Override
    public int width() {
        return 160;
    }
    
    @Override
    public int height() {
        return 32;
    }
}




