package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.*;
import thaumcraft.client.gui.widgets.ImageButton;
import thaumcraft.client.gui.widgets.SliderWidget;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.tiles.crafting.FocusElementNode;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.*;

/**
 * FocalManipulatorScreen - Client-side GUI for the Focal Manipulator.
 * 
 * Allows players to:
 * - Build focus spell configurations using a node tree
 * - Select focus parts (mediums, effects, modifiers)
 * - Configure node settings
 * - Name the focus
 * - View complexity, cost, and required crystals
 * 
 * Ported from GuiFocalManipulator in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    
    private static final ResourceLocation TEX_MAIN = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_wandtable.png");
    private static final ResourceLocation TEX_BG = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_wandtable2.png");
    private static final ResourceLocation TEX_SIDE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_wandtable3.png");
    private static final ResourceLocation TEX_BASE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    private static final ResourceLocation TEX_MEDIUM = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/_medium.png");
    private static final ResourceLocation TEX_EFFECT = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/_effect.png");
    
    // GUI state
    private EditBox nameField;
    private ImageButton confirmButton;
    private SliderWidget partsScrollbar;
    private SliderWidget mainScrollbarX;
    private SliderWidget mainScrollbarY;
    
    // Parts list
    private final List<String> shownParts = new ArrayList<>();
    private int partsStart = 0;
    
    // Node selection
    private int selectedNode = -1;
    private int lastNodeHover = -1;
    private int nodeID = 0;
    
    // Scroll state
    private int scrollX = 0;
    private int scrollY = 0;
    private int sMinX, sMinY, sMaxX, sMaxY;
    
    // Mouse dragging
    private boolean isDragging = false;
    private int lastMouseX, lastMouseY;
    
    // Cost calculations
    private int totalComplexity = 0;
    private int maxComplexity = 0;
    private float costCast = 0;
    private int costXp = 0;
    private int costVis = 0;
    private ItemStack[] components = null;
    private boolean valid = false;
    
    private final DecimalFormat formatter = new DecimalFormat("#######.##");
    private static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
    
    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 231;
        this.imageHeight = 231;
    }
    
    private TileFocalManipulator getTile() {
        return menu.getBlockEntity();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Create name field
        nameField = new EditBox(font, leftPos + 30, topPos + 11, 170, 12, Component.empty());
        nameField.setBordered(false);
        nameField.setMaxLength(50);
        nameField.setTextColor(0xFFFFFF);
        nameField.setValue(getTile().focusName);
        nameField.setResponder(this::onNameChanged);
        addWidget(nameField);
        
        // Create confirm button
        confirmButton = new ImageButton(
            leftPos + 242, topPos + 18, 24, 16,
            "wandtable.text3", Component.translatable("wandtable.text3").getString(),
            TEX_BASE, 232, 240, 24, 16,
            btn -> onConfirmClicked()
        );
        addRenderableWidget(confirmButton);
        
        // Initialize data if needed
        if (getTile().data.isEmpty() && !getTile().getItem(0).isEmpty()) {
            resetNodes();
        } else {
            gatherInfo(false);
        }
    }
    
    private void onNameChanged(String newName) {
        getTile().focusName = newName;
        PacketHandler.sendToServer(new PacketFocusNameToServer(getTile().getBlockPos(), newName));
    }
    
    private void onConfirmClicked() {
        if (confirmButton.isButtonActive() && valid) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Draw parts list
        drawPartsList(graphics, mouseX, mouseY);
        
        // Draw name field
        if (!getTile().data.isEmpty() && nameField != null) {
            nameField.render(graphics, mouseX, mouseY, partialTick);
        }
        
        // Draw tooltips
        drawPartsTooltips(graphics, mouseX, mouseY);
        drawNodeTooltips(graphics, mouseX, mouseY);
        
        // Update confirm button state
        confirmButton.setButtonActive(getTile().vis <= 0.0f && valid);
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int x = leftPos;
        int y = topPos;
        
        // Draw background layers
        graphics.blit(TEX_BG, x, y, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEX_SIDE, x - 71, y - 3, 0, 0, 71, 239);
        
        // Handle GUI reset
        TileFocalManipulator tile = getTile();
        if (tile.getItem(0).isEmpty() || tile.doGuiReset) {
            if (tile.doGuiReset) {
                resetNodes();
            } else {
                tile.data.clear();
                gatherInfo(false);
            }
            tile.focusName = "";
            if (!tile.getItem(0).isEmpty()) {
                tile.focusName = tile.getItem(0).getHoverName().getString();
                if (nameField != null) {
                    nameField.setValue(tile.focusName);
                }
            }
            tile.doGuiReset = false;
        }
        
        if (tile.doGather) {
            gatherInfo(false);
            tile.doGather = false;
        }
        
        // Draw nodes
        drawNodes(graphics, leftPos + 132 - scrollX, topPos + 48 - scrollY, mouseX, mouseY);
        
        // Draw foreground overlay
        graphics.blit(TEX_MAIN, x, y, 0, 0, imageWidth, imageHeight);
        
        // Draw complexity
        if (maxComplexity > 0) {
            int complexColor = totalComplexity > maxComplexity ? 0xF65858 : 0xFFE59F;
            graphics.drawString(font, totalComplexity + "/" + maxComplexity, x + 242, y + 36, complexColor, true);
        }
        
        // Draw XP cost
        int xpColor = costXp > minecraft.player.experienceLevel ? 0xF65858 : 0x9A1B8D;
        graphics.drawString(font, String.valueOf(costXp), x + 242, y + 50, xpColor, true);
        
        // Draw vis cost
        int visCost = getTile().vis > 0 ? (int) getTile().vis : costVis;
        graphics.drawString(font, ChatFormatting.AQUA + String.valueOf(visCost), x + 242, y + 64, 0x9A1B8D, true);
        
        // Draw cast cost
        if (costCast > 0) {
            String cost = formatter.format(costCast);
            graphics.drawString(font, ChatFormatting.AQUA + Component.translatable("item.Focus.cost1").getString() + ": " + cost, 
                x + 230, y + 80, 0x9A1B8D, true);
        }
        
        // Draw component crystals label
        if (components != null && components.length > 0) {
            graphics.drawString(font, ChatFormatting.GOLD + Component.translatable("wandtable.text4").getString(), 
                x + 230, y + 92, 0x9A1B8D, true);
        }
        
        // Draw name field background
        if (!tile.data.isEmpty() && tile.focusName != null) {
            graphics.blit(TEX_BASE, leftPos + 24, topPos + 8, 192, 224, 8, 14);
            for (int i = 1; i < 22; i++) {
                graphics.blit(TEX_BASE, leftPos + 24 + i * 8, topPos + 8, 200, 224, 8, 14);
            }
            graphics.blit(TEX_BASE, leftPos + 24 + 22 * 8, topPos + 8, 208, 224, 8, 14);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't draw default title
    }
    
    // ==================== Parts List ====================
    
    private void drawPartsList(GuiGraphics graphics, int mouseX, int mouseY) {
        int gx = leftPos;
        int gy = topPos;
        
        int count = 0;
        int index = 0;
        
        for (String key : shownParts) {
            if (++count - 1 < partsStart) continue;
            
            FocusNode node = (FocusNode) FocusEngine.getElement(key);
            if (node != null) {
                float scale = node.getType() == IFocusElement.EnumUnitType.MOD ? 24.0f : 32.0f;
                boolean hover = isInRegion(gx + 28, gy + 32 + 24 * index, 20, 20, mouseX, mouseY);
                drawPart(graphics, node, gx + 38, 43 + gy + 25 * index, scale, hover);
            }
            
            if (++index > 5) break;
        }
    }
    
    private void drawPartsTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int gx = leftPos;
        int gy = topPos;
        
        int count = 0;
        int index = 0;
        
        for (String key : shownParts) {
            if (++count - 1 < partsStart) continue;
            
            FocusNode node = (FocusNode) FocusEngine.getElement(key);
            if (node != null && isInRegion(gx + 28, gy + 32 + 24 * index, 20, 20, mouseX, mouseY)) {
                List<Component> tooltip = generatePartTooltip(node, -1);
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            
            if (++index > 5) break;
        }
    }
    
    private List<Component> generatePartTooltip(FocusNode node, int idx) {
        List<Component> list = new ArrayList<>();
        if (node == null) return list;
        
        FocusElementNode placed = idx >= 0 ? getTile().data.get(idx) : null;
        
        // Name
        list.add(Component.translatable(node.getUnlocalizedName()));
        
        // Description
        list.add(Component.translatable(node.getUnlocalizedText()).withStyle(ChatFormatting.DARK_PURPLE));
        
        // Complexity
        int complexity = node.getComplexity();
        if (placed != null) {
            complexity = (int)(node.getComplexity() * placed.complexityMultiplier);
        }
        ChatFormatting complexFormat = placed != null && placed.complexityMultiplier > 1.0f ? ChatFormatting.RED : ChatFormatting.GOLD;
        list.add(Component.translatable("focuspart.com").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(" " + complexity).withStyle(complexFormat)));
        
        // Power multiplier
        float power = node.getPowerMultiplier();
        if (placed != null) {
            power = placed.getPower(getTile().data);
        }
        if (power != 1.0f) {
            ChatFormatting powerFormat = power < 1.0f ? ChatFormatting.RED : ChatFormatting.GREEN;
            list.add(Component.translatable("focuspart.eff").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" x" + ATTRIBUTE_MODIFIER_FORMAT.format(power)).withStyle(powerFormat)));
        }
        
        // Damage/healing for effects
        if (node instanceof FocusEffect effect) {
            float damage = effect.getDamageForDisplay(placed == null ? 1.0f : placed.getPower(getTile().data));
            if (damage > 0) {
                list.add(Component.literal(ChatFormatting.DARK_RED + "" + 
                    ATTRIBUTE_MODIFIER_FORMAT.format(damage) + " " +
                    Component.translatable("attribute.name.generic.attack_damage").getString()));
            } else if (damage < 0) {
                list.add(Component.literal(ChatFormatting.DARK_GREEN + "" + 
                    ATTRIBUTE_MODIFIER_FORMAT.format(-damage) + " " +
                    Component.translatable("focus.heal.power").getString()));
            }
        }
        
        return list;
    }
    
    // ==================== Node Rendering ====================
    
    private void drawNodes(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        TileFocalManipulator tile = getTile();
        if (tile.data == null || tile.data.isEmpty()) return;
        
        int hover = -1;
        
        for (FocusElementNode fn : tile.data.values()) {
            int xx = x + fn.x * 24;
            int yy = y + fn.y * 32;
            
            boolean isInMainArea = isInRegion(leftPos + 63, topPos + 31, 136, 160, mouseX, mouseY);
            boolean isOverNode = isInRegion(xx - 10, yy - 10, 20, 20, mouseX, mouseY);
            boolean mouseover = isInMainArea && isOverNode;
            
            if (mouseover && fn.parent >= 0) {
                hover = fn.id;
            }
            
            // Draw node
            if (fn.node != null) {
                if (isInRegion(leftPos + 48, topPos + 16, 154, 192, xx - 8, yy - 8)) {
                    drawPart(graphics, fn.node, xx, yy, 32.0f, mouseover);
                }
            } else {
                // Empty slot
                graphics.blit(TEX_MAIN, xx - 12, yy - 12, 120, 232, 24, 24);
            }
            
            // Draw selection highlight
            if (selectedNode == fn.id || (mouseover && fn.parent >= 0)) {
                graphics.blit(TEX_MAIN, xx - 12, yy - 12, 96, 232, 24, 24);
            }
            
            // Draw connections
            FocusElementNode parent = tile.data.get(fn.parent);
            if (parent != null) {
                graphics.blit(TEX_MAIN, xx - 6, yy - 22, 54, 232, 12, 12);
                
                // Handle split node connections
                if (parent.node instanceof FocusModSplit) {
                    int dist = Math.abs(fn.x - parent.x);
                    for (int i = 0; i < dist; i++) {
                        if (fn.x < parent.x) {
                            if (i == 0) {
                                graphics.blit(TEX_MAIN, xx - 4, yy - 36, 8, 240, 16, 16);
                            } else {
                                graphics.blit(TEX_MAIN, xx - 12 + i * 24, yy - 36, 72, 240, 24, 16);
                            }
                        } else {
                            if (i == 0) {
                                graphics.blit(TEX_MAIN, xx - 12, yy - 36, 24, 240, 16, 16);
                            } else {
                                graphics.blit(TEX_MAIN, xx - 12 - i * 24, yy - 36, 72, 240, 24, 16);
                            }
                        }
                    }
                }
                
                // Draw supply type indicators for empty slots
                if (fn.node == null && isInRegion(leftPos + 48, topPos + 16, 168, 192, xx - 4, yy - 4)) {
                    int offset = (parent.target && parent.trajectory) ? 4 : 0;
                    
                    graphics.pose().pushPose();
                    if (parent.target) {
                        graphics.pose().translate(xx - offset, yy, 0);
                        graphics.pose().scale(0.5f, 0.5f, 0.5f);
                        graphics.blit(TEX_MAIN, -8, -8, 152, 240, 16, 16);
                        graphics.pose().popPose();
                        graphics.pose().pushPose();
                    }
                    if (parent.trajectory) {
                        graphics.pose().translate(xx + offset, yy, 0);
                        graphics.pose().scale(0.5f, 0.5f, 0.5f);
                        graphics.blit(TEX_MAIN, -8, -8, 168, 240, 16, 16);
                    }
                    graphics.pose().popPose();
                }
            }
        }
        
        // Update hover state
        if (hover >= 0 && lastNodeHover != hover) {
            // Play rollover sound
            lastNodeHover = hover;
        }
        if (hover < 0) {
            lastNodeHover = -1;
        }
        
        // Draw node settings
        if (selectedNode >= 0) {
            drawNodeSettings(graphics, selectedNode);
        }
    }
    
    private void drawNodeTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (lastNodeHover >= 0) {
            FocusElementNode fn = getTile().data.get(lastNodeHover);
            if (fn != null && fn.node != null) {
                List<Component> tooltip = generatePartTooltip(fn.node, lastNodeHover);
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }
    
    private void drawNodeSettings(GuiGraphics graphics, int nodeId) {
        FocusElementNode fn = getTile().data.get(nodeId);
        if (fn == null || fn.node == null || fn.node.getSettingList().isEmpty()) return;
        
        int idx = 0;
        for (String key : fn.node.getSettingList()) {
            NodeSetting setting = fn.node.getSetting(key);
            if (setting.getResearch() != null && !ThaumcraftCapabilities.knowsResearchStrict(minecraft.player, setting.getResearch())) {
                continue;
            }
            
            int settingX = leftPos + imageWidth;
            int settingY = topPos + imageHeight - 10 - fn.node.getSettingList().size() * 26 + idx * 26;
            
            graphics.drawString(font, ChatFormatting.GOLD + setting.getLocalizedName(), settingX, settingY, 0xFFFFFF, true);
            idx++;
        }
    }
    
    private void drawPart(GuiGraphics graphics, FocusNode node, int x, int y, float scale, boolean hover) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZN.rotationDegrees(90.0f));
        
        boolean isRoot = node.getType() == IFocusElement.EnumUnitType.MOD || node.getKey().equals("thaumcraft.ROOT");
        if (isRoot) {
            scale *= 2.0f;
        }
        
        Color color = new Color(FocusEngine.getElementColor(node.getKey()));
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        
        float actualScale = (scale * 0.9f + (hover ? 2 : 0)) / 32.0f;
        
        // Draw type background (effect or medium)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        if (node.getType() == IFocusElement.EnumUnitType.EFFECT) {
            RenderSystem.setShaderColor(r, g, b, 0.8f);
            graphics.pose().pushPose();
            graphics.pose().scale(actualScale, actualScale, 1.0f);
            graphics.blit(TEX_EFFECT, -16, -16, 0, 0, 32, 32, 32, 32);
            graphics.pose().popPose();
        } else if (node.getType() == IFocusElement.EnumUnitType.MEDIUM && !isRoot) {
            RenderSystem.setShaderColor(r, g, b, 0.8f);
            graphics.pose().pushPose();
            graphics.pose().scale(actualScale, actualScale, 1.0f);
            graphics.blit(TEX_MEDIUM, -16, -16, 0, 0, 32, 32, 32, 32);
            graphics.pose().popPose();
        }
        
        // Draw icon
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        float iconScale = (scale / 2.0f + (hover ? 2 : 0)) / 16.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1);
        graphics.pose().scale(iconScale, iconScale, 1.0f);
        ResourceLocation icon = FocusEngine.getElementIcon(node.getKey());
        graphics.blit(icon, -8, -8, 0, 0, 16, 16, 16, 16);
        graphics.pose().popPose();
        
        graphics.pose().popPose();
    }
    
    // ==================== Node Manipulation ====================
    
    private void resetNodes() {
        nodeID = 0;
        getTile().data.clear();
        addNodeAt(FocusMediumRoot.class, 0, false);
        selectedNode = 1;
        calcNodeTreeLayout();
        gatherInfo(true);
    }
    
    private List<Integer> addNodeAt(Class<? extends FocusNode> nodeClass, int idx, boolean gather) {
        List<Integer> ret = new ArrayList<>();
        TileFocalManipulator tile = getTile();
        
        boolean same = false;
        FocusElementNode previous = null;
        
        if (tile.data.containsKey(idx)) {
            cullChildren(idx);
            if (tile.data.get(idx).node != null && tile.data.get(idx).node.getClass() == nodeClass) {
                same = true;
            } else {
                previous = tile.data.remove(idx);
            }
        }
        
        try {
            FocusElementNode fn;
            FocusNode node;
            
            if (!same) {
                fn = new FocusElementNode();
                node = nodeClass.getDeclaredConstructor().newInstance();
                fn.node = node;
                
                if (previous != null) {
                    fn.y = previous.y;
                    fn.x = previous.x;
                }
                
                fn.id = getNextId();
                ret.add(fn.id);
                selectedNode = fn.id;
                
                if (previous != null && tile.data.containsKey(previous.parent)) {
                    fn.parent = previous.parent;
                    int[] c = tile.data.get(previous.parent).children;
                    for (int i = 0; i < c.length; i++) {
                        if (c[i] == previous.id) {
                            tile.data.get(previous.parent).children[i] = fn.id;
                            break;
                        }
                    }
                }
                
                fn.target = node.canSupply(FocusNode.EnumSupplyType.TARGET);
                fn.trajectory = node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
                tile.data.put(nodeID, fn);
            } else {
                fn = tile.data.get(idx);
                node = fn.node;
            }
            
            // Add child slots
            if (fn.target || fn.trajectory) {
                if (node instanceof FocusModSplit) {
                    // Split creates two branches
                    FocusElementNode blank1 = new FocusElementNode();
                    blank1.parent = fn.id;
                    blank1.id = getNextId();
                    ret.add(nodeID);
                    blank1.x = fn.x - 1;
                    blank1.y = fn.y + 1;
                    tile.data.put(nodeID, blank1);
                    selectedNode = nodeID;
                    
                    FocusElementNode blank2 = new FocusElementNode();
                    blank2.parent = fn.id;
                    blank2.x = fn.x + 1;
                    blank2.y = fn.y + 1;
                    blank2.id = getNextId();
                    ret.add(nodeID);
                    tile.data.put(nodeID, blank2);
                    
                    fn.children = new int[] { blank1.id, blank2.id };
                } else {
                    // Single child
                    FocusElementNode blank = new FocusElementNode();
                    blank.parent = fn.id;
                    blank.x = fn.x;
                    blank.y = fn.y + 1;
                    blank.id = getNextId();
                    ret.add(nodeID);
                    tile.data.put(nodeID, blank);
                    fn.children = new int[] { blank.id };
                    selectedNode = nodeID;
                }
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Error adding focus node", e);
        }
        
        if (gather) {
            calcNodeTreeLayout();
            gatherInfo(true);
        }
        
        return ret;
    }
    
    private void cullChildren(int idx) {
        TileFocalManipulator tile = getTile();
        if (tile.data.containsKey(idx)) {
            for (int childId : tile.data.get(idx).children) {
                cullChildren(childId);
                tile.data.remove(childId);
            }
        }
    }
    
    private int getNextId() {
        while (getTile().data.containsKey(nodeID)) {
            nodeID++;
        }
        return nodeID;
    }
    
    private void calcNodeTreeLayout() {
        TileFocalManipulator tile = getTile();
        
        // Find first split node
        int splitId = -1;
        for (FocusElementNode node : tile.data.values()) {
            if (splitId < 0 && node.node instanceof FocusModSplit) {
                splitId = node.id;
            }
        }
        
        // Reposition nodes based on split
        if (splitId >= 0) {
            // Simple layout algorithm - would need full tree traversal for complex layouts
        }
        
        // Center split node children
        for (FocusElementNode node : tile.data.values()) {
            if (node.node instanceof FocusModSplit) {
                if (tile.data.containsKey(node.parent) && tile.data.get(node.parent).node != null 
                    && !(tile.data.get(node.parent).node instanceof FocusModSplit)) {
                    node.x = tile.data.get(node.parent).x;
                } else {
                    int xx = 0;
                    for (int childId : node.children) {
                        xx += tile.data.get(childId).x;
                    }
                    if (node.children.length > 0) {
                        xx /= node.children.length;
                    }
                    node.x = xx;
                }
            }
        }
        
        if (selectedNode >= 0 && !tile.data.containsKey(selectedNode)) {
            selectedNode = -1;
        }
    }
    
    // ==================== Info Gathering ====================
    
    private void gatherInfo(boolean sync) {
        TileFocalManipulator tile = getTile();
        
        // Clear parts list
        shownParts.clear();
        components = null;
        
        if (tile.getItem(0).isEmpty()) return;
        
        Map<String, Integer> compCount = new HashMap<>();
        totalComplexity = 0;
        maxComplexity = 0;
        
        // Get max complexity from focus item
        ItemStack focusStack = menu.getSlot(0).getItem();
        if (!focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus focus) {
            maxComplexity = focus.getMaxComplexity();
        }
        
        boolean emptyNodes = false;
        AspectList crystals = new AspectList();
        
        if (tile.data != null && !tile.data.isEmpty()) {
            for (FocusElementNode node : tile.data.values()) {
                if (node.node != null) {
                    int count = compCount.getOrDefault(node.node.getKey(), 0) + 1;
                    node.complexityMultiplier = 0.5f * (count + 1);
                    compCount.put(node.node.getKey(), count);
                    totalComplexity += (int)(node.node.getComplexity() * node.complexityMultiplier);
                    
                    if (node.node.getAspect() != null) {
                        crystals.add(node.node.getAspect(), 1);
                    }
                } else {
                    emptyNodes = true;
                }
            }
        }
        
        costCast = totalComplexity / 5.0f;
        costVis = totalComplexity * 10 + maxComplexity / 5;
        costXp = (int)Math.max(1, Math.round(Math.sqrt(totalComplexity)));
        
        // Check crystal requirements
        boolean validCrystals = true;
        if (crystals.getAspects().length > 0) {
            components = new ItemStack[crystals.getAspects().length];
            int idx = 0;
            for (Aspect aspect : crystals.getAspects()) {
                components[idx] = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));
                // Check if player has crystal
                if (!playerHasItem(components[idx])) {
                    validCrystals = false;
                }
                idx++;
            }
        }
        
        // Gather available parts
        gatherPartsList();
        
        // Calculate scroll bounds
        calcScrollBounds();
        
        // Update validity
        valid = totalComplexity <= maxComplexity && !emptyNodes && validCrystals 
                && costXp <= minecraft.player.experienceLevel;
        
        // Update name field
        if (tile.focusName.isEmpty() && !tile.getItem(0).isEmpty()) {
            tile.focusName = tile.getItem(0).getHoverName().getString();
        }
        if (nameField != null) {
            nameField.setValue(tile.focusName);
        }
        
        // Sync to server
        if (sync) {
            PacketHandler.sendToServer(new PacketFocusNodesToServer(tile.getBlockPos(), tile.data, tile.focusName));
        }
    }
    
    private void gatherPartsList() {
        shownParts.clear();
        
        if (selectedNode < 0 || !getTile().data.containsKey(selectedNode)) return;
        
        partsStart = 0;
        List<String> mediums = new ArrayList<>();
        List<String> effects = new ArrayList<>();
        List<String> mods = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        
        boolean hasExclusive = false;
        boolean hasMedium = false;
        
        // Check for exclusive mediums
        for (FocusElementNode fn : getTile().data.values()) {
            if (fn.node instanceof FocusMedium medium) {
                hasMedium = !(fn.node instanceof FocusMediumRoot);
                if (medium.isExclusive()) {
                    hasExclusive = true;
                    break;
                }
            }
            if (fn.node != null && fn.node.isExclusive()) {
                excluded.add(fn.node.getKey());
            }
        }
        
        FocusElementNode node = getTile().data.get(selectedNode);
        FocusElementNode parent = getTile().data.get(node.parent);
        
        if (parent != null && parent.node != null) {
            for (String key : FocusEngine.elements.keySet()) {
                IFocusElement element = FocusEngine.getElement(key);
                if (element == null) continue;
                
                // Check research
                if (!ThaumcraftCapabilities.knowsResearchStrict(minecraft.player, element.getResearch())) {
                    continue;
                }
                
                // Skip root
                if (element.getKey().equals("thaumcraft.ROOT")) continue;
                
                // Skip non-nodes
                if (!(element instanceof FocusNode focusNode)) continue;
                
                // Skip excluded
                if (excluded.contains(focusNode.getKey())) continue;
                
                // Check supply type compatibility
                FocusNode.EnumSupplyType[] mustBeSupplied = focusNode.mustBeSupplied();
                if (mustBeSupplied == null) continue;
                
                boolean compatible = false;
                for (FocusNode.EnumSupplyType type : mustBeSupplied) {
                    if (parent.node.canSupply(type)) {
                        compatible = true;
                        break;
                    }
                }
                
                if (!compatible) continue;
                
                // Categorize
                switch (element.getType()) {
                    case EFFECT -> effects.add(key);
                    case MEDIUM -> {
                        if (!hasExclusive && (!(element instanceof FocusMedium medium) || !medium.isExclusive() || !hasMedium)) {
                            mediums.add(key);
                        }
                    }
                    case MOD -> mods.add(key);
                }
            }
        }
        
        // Sort and combine
        Collections.sort(mediums);
        Collections.sort(effects);
        Collections.sort(mods);
        
        shownParts.addAll(mediums);
        shownParts.addAll(effects);
        shownParts.addAll(mods);
    }
    
    private void calcScrollBounds() {
        sMinX = sMinY = sMaxX = sMaxY = 0;
        
        for (FocusElementNode fn : getTile().data.values()) {
            if (fn.x < sMinX) sMinX = fn.x;
            if (fn.y < sMinY) sMinY = fn.y;
            if (fn.x > sMaxX) sMaxX = fn.x;
            if (fn.y > sMaxY) sMaxY = fn.y;
        }
    }
    
    private boolean playerHasItem(ItemStack required) {
        if (minecraft == null || minecraft.player == null) return false;
        int needed = required.getCount();
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (ItemStack.isSameItem(stack, required)) {
                needed -= stack.getCount();
                if (needed <= 0) return true;
            }
        }
        return false;
    }
    
    // ==================== Input Handling ====================
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Name field click
        if (nameField != null && nameField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        TileFocalManipulator tile = getTile();
        
        if (tile.vis <= 0.0f && tile.data != null && !tile.data.isEmpty()) {
            // Node selection
            if (lastNodeHover >= 0) {
                selectedNode = lastNodeHover;
                
                // Right-click to remove/replace node
                if (button == 1 && tile.data.get(selectedNode).node != null) {
                    FocusElementNode fn = tile.data.get(selectedNode);
                    if (tile.data.get(fn.parent).node != null) {
                        addNodeAt(tile.data.get(fn.parent).node.getClass(), fn.parent, true);
                    }
                }
                
                gatherInfo(false);
                return true;
            }
            
            // Parts list click
            int gx = leftPos;
            int gy = topPos;
            int count = 0;
            int index = 0;
            
            if (selectedNode >= 0) {
                for (String key : shownParts) {
                    if (++count - 1 < partsStart) continue;
                    
                    if (isInRegion(gx + 28, gy + 32 + 24 * index, 20, 20, (int)mouseX, (int)mouseY)) {
                        Class<? extends FocusNode> nodeClass = FocusEngine.elements.get(key);
                        if (nodeClass != null) {
                            addNodeAt(nodeClass, selectedNode, true);
                        }
                        return true;
                    }
                    
                    if (++index > 5) break;
                }
            }
        }
        
        // Check for main area drag
        if (isInRegion(leftPos + 63, topPos + 31, 136, 160, (int)mouseX, (int)mouseY)) {
            isDragging = true;
            lastMouseX = (int)mouseX;
            lastMouseY = (int)mouseY;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && isInRegion(leftPos + 63, topPos + 31, 136, 160, (int)mouseX, (int)mouseY)) {
            scrollX -= (int)mouseX - lastMouseX;
            scrollY -= (int)mouseY - lastMouseY;
            
            // Clamp scroll
            scrollX = Math.max(sMinX * 24, Math.min(sMaxX * 24, scrollX));
            scrollY = Math.max(0, Math.min((sMaxY - 3) * 32, scrollY));
            
            lastMouseX = (int)mouseX;
            lastMouseY = (int)mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll parts list
        if (shownParts.size() > 6 && isInRegion(leftPos + 24, topPos + 24, 32, 157, (int)mouseX, (int)mouseY)) {
            if (scrollY > 0 && partsStart > 0) {
                partsStart--;
            } else if (scrollY < 0 && partsStart < shownParts.size() - 6) {
                partsStart++;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField != null && nameField.isFocused()) {
            return nameField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameField != null && nameField.isFocused()) {
            return nameField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    // ==================== Utility ====================
    
    private boolean isInRegion(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}




