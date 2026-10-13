package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.*;
import thaumcraft.common.menu.GolemBuilderMenu;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

import java.util.Set;

/**
 * GolemBuilderScreen - Client-side GUI for the Golem Builder.
 * 
 * Allows selecting golem parts (material, head, arms, legs, addon)
 * and initiating the crafting process.
 */
public class GolemBuilderScreen extends AbstractContainerScreen<GolemBuilderMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/guigolem.png");
    
    // Part selection buttons
    private Button matLeftBtn, matRightBtn;
    private Button headLeftBtn, headRightBtn;
    private Button armsLeftBtn, armsRightBtn;
    private Button legsLeftBtn, legsRightBtn;
    private Button addonLeftBtn, addonRightBtn;
    private Button craftBtn;
    
    public GolemBuilderScreen(GolemBuilderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 208;
        this.imageHeight = 224;
        this.inventoryLabelY = this.imageHeight - 94;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int x = this.leftPos;
        int y = this.topPos;
        
        // Material selection (left side, top)
        matLeftBtn = addArrowButton(x + 5, y + 19, true, 2);  // button ID 2 = mat left
        matRightBtn = addArrowButton(x + 33, y + 19, false, 3); // button ID 3 = mat right
        
        // Head selection (right side, top)
        headLeftBtn = addArrowButton(x + 101, y + 19, true, 0);  // button ID 0 = head left
        headRightBtn = addArrowButton(x + 129, y + 19, false, 1); // button ID 1 = head right
        
        // Arms selection (right side, middle)
        armsLeftBtn = addArrowButton(x + 101, y + 43, true, 4);  // button ID 4 = arms left
        armsRightBtn = addArrowButton(x + 129, y + 43, false, 5); // button ID 5 = arms right
        
        // Legs selection (right side, bottom)
        legsLeftBtn = addArrowButton(x + 101, y + 67, true, 6);  // button ID 6 = legs left
        legsRightBtn = addArrowButton(x + 129, y + 67, false, 7); // button ID 7 = legs right
        
        // Addon selection (left side, bottom)
        addonLeftBtn = addArrowButton(x + 5, y + 67, true, 8);  // button ID 8 = addon left
        addonRightBtn = addArrowButton(x + 33, y + 67, false, 9); // button ID 9 = addon right
        
        // Craft button
        craftBtn = addRenderableWidget(Button.builder(Component.literal("Craft"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 99);
            }
        }).bounds(x + 120, y + 104, 40, 16).build());
    }
    
    private Button addArrowButton(int x, int y, boolean left, int buttonId) {
        String text = left ? "<" : ">";
        return addRenderableWidget(Button.builder(Component.literal(text), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            }
        }).bounds(x, y, 10, 10).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        
        // Render part names and stats
        renderPartInfo(graphics);
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw main background
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Draw crafting progress bar if crafting
        TileGolemBuilder tile = menu.getBlockEntity();
        if (tile != null && tile.cost > 0) {
            int progress = (int)(46.0f * tile.getCraftingProgress());
            graphics.blit(TEXTURE, x + 145, y + 89, 209, 89, progress, 6);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't render default labels - we'll draw custom ones
    }
    
    /**
     * Render information about selected parts and stats.
     */
    private void renderPartInfo(GuiGraphics graphics) {
        TileGolemBuilder tile = menu.getBlockEntity();
        if (tile == null) return;
        
        IGolemProperties props = tile.getCurrentGolemProperties();
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw part names
        GolemMaterial mat = props.getMaterial();
        GolemHead head = props.getHead();
        GolemArm arms = props.getArms();
        GolemLeg legs = props.getLegs();
        GolemAddon addon = props.getAddon();
        
        // Material name (left top)
        if (mat != null) {
            Component matName = mat.getLocalizedName();
            graphics.drawCenteredString(this.font, matName, x + 24, y + 8, 0xFFFFFF);
        }
        
        // Head name (right top)
        if (head != null) {
            Component headName = head.getLocalizedName();
            graphics.drawCenteredString(this.font, headName, x + 120, y + 8, 0xFFFFFF);
        }
        
        // Arms name (right middle)
        if (arms != null) {
            Component armsName = arms.getLocalizedName();
            graphics.drawCenteredString(this.font, armsName, x + 120, y + 32, 0xFFFFFF);
        }
        
        // Legs name (right bottom)
        if (legs != null) {
            Component legsName = legs.getLocalizedName();
            graphics.drawCenteredString(this.font, legsName, x + 120, y + 56, 0xFFFFFF);
        }
        
        // Addon name (left bottom)
        if (addon != null && !addon.key.equalsIgnoreCase("none")) {
            Component addonName = addon.getLocalizedName();
            graphics.drawCenteredString(this.font, addonName, x + 24, y + 56, 0xFFFFFF);
        } else {
            graphics.drawCenteredString(this.font, Component.literal("No Addon"), x + 24, y + 56, 0x888888);
        }
        
        // Calculate and display stats
        int health = 10 + (mat != null ? mat.healthMod : 0);
        int armor = mat != null ? mat.armor : 0;
        int damage = mat != null ? mat.damage : 0;
        
        Set<EnumGolemTrait> traits = props.getTraits();
        if (traits.contains(EnumGolemTrait.FRAGILE)) {
            health = (int)(health * 0.75);
            armor = (int)(armor * 0.75);
        }
        if (traits.contains(EnumGolemTrait.ARMORED)) {
            armor = Math.max((int)(armor * 1.5), armor + 1);
        }
        if (traits.contains(EnumGolemTrait.FIGHTER)) {
            // Fighter trait enables damage
        } else {
            damage = 0;
        }
        if (traits.contains(EnumGolemTrait.BRUTAL)) {
            damage = (int) Math.max(damage * 1.5, damage + 1);
        }
        
        // Draw stats
        float hearts = health / 2.0f;
        float armorVal = armor / 2.0f;
        float damageVal = damage / 2.0f;
        
        graphics.drawCenteredString(this.font, String.format("%.1f", hearts), x + 48, y + 108, 0xFFFFFF);
        graphics.drawCenteredString(this.font, String.format("%.1f", armorVal), x + 72, y + 108, 0xFFFFFF);
        graphics.drawCenteredString(this.font, String.format("%.1f", damageVal), x + 97, y + 108, 0xFFFFFF);
        
        // Draw trait icons/names
        int traitX = x + 56;
        int traitY = y + 80;
        int traitCount = 0;
        for (EnumGolemTrait trait : traits) {
            if (traitCount >= 8) break; // Limit display
            int tx = traitX + (traitCount % 4) * 18;
            int ty = traitY - (traitCount / 4) * 10;
            graphics.drawString(this.font, trait.name().substring(0, Math.min(3, trait.name().length())), 
                    tx, ty, 0xFFFFFF, false);
            traitCount++;
        }
        
        // Draw cost
        int cost = menu.getCost();
        int maxCost = menu.getMaxCost();
        if (maxCost > 0) {
            graphics.drawString(this.font, "Cost: " + cost + "/" + maxCost, x + 145, y + 78, 0xFFFFFF);
        }
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        // Update craft button state based on crafting status
        TileGolemBuilder tile = menu.getBlockEntity();
        if (tile != null && craftBtn != null) {
            craftBtn.active = tile.cost <= 0; // Disable while crafting
        }
    }
}




