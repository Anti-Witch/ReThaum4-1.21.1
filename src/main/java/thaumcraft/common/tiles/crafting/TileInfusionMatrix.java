package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.common.lib.utils.TCCompat;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.api.crafting.IInfusionStabiliserExt;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.blocks.basic.BlockPillarTC;
import thaumcraft.common.blocks.devices.BlockPedestal;
import thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.enchantment.InfusionEnchantment;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.network.fx.PacketFXInfusionSource;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.init.ModSounds;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Infusion altar matrix tile entity - the central block of the infusion altar.
 * Manages infusion crafting, stability, and instability events.
 */
public class TileInfusionMatrix extends TileThaumcraft implements IAspectContainer, IWandable {

    // Altar state
    public boolean active = false;
    public boolean crafting = false;
    public boolean checkSurroundings = true;

    // Stability system
    public int stabilityCap = 25;
    public float stability = 0.0f;
    public float stabilityReplenish = 0.0f;
    public float costMult = 1.0f;

    // Recipe tracking
    private AspectList recipeEssentia = new AspectList();
    private List<ItemStack> recipeIngredients = new ArrayList<>();
    private ItemStack recipeInput = ItemStack.EMPTY;
    private Object recipeOutput = null;
    private String recipeOutputLabel = null;
    private int recipeInstability = 0;
    private int recipeXP = 0;
    private int recipeType = 0;
    private String recipePlayer = null;

    // Tick counter
    private int count = 0;
    private int cycleTime = 20;
    private int countDelay = 10;
    private int itemCount = 0;
    private int dangerCount = 0;

    // Client-side animation
    public int craftCount = 0;
    public float startUp = 0.0f;
    public HashMap<String, SourceFX> sourceFX = new HashMap<>();

    // Cached pedestal positions
    private List<BlockPos> pedestals = new ArrayList<>();
    private List<BlockPos> problemBlocks = new ArrayList<>();
    private HashMap<Block, Integer> tempBlockCount = new HashMap<>();

    private static final DecimalFormat FORMATTER = new DecimalFormat("#######.##");

    public TileInfusionMatrix(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileInfusionMatrix(BlockPos pos, BlockState state) {
        this(ModBlockEntities.INFUSION_MATRIX.get(), pos, state);
    }

    // ==================== Stability ====================

    private enum EnumStability {
        VERY_STABLE, STABLE, UNSTABLE, VERY_UNSTABLE
    }

    private EnumStability getStability() {
        if (stability > stabilityCap / 2) return EnumStability.VERY_STABLE;
        if (stability >= 0.0f) return EnumStability.STABLE;
        if (stability > -25.0f) return EnumStability.UNSTABLE;
        return EnumStability.VERY_UNSTABLE;
    }

    private float getModFromCurrentStability() {
        return switch (getStability()) {
            case VERY_STABLE -> 5.0f;
            case STABLE -> 6.0f;
            case UNSTABLE -> 7.0f;
            case VERY_UNSTABLE -> 8.0f;
        };
    }

    private float getLossPerCycle() {
        return recipeInstability / getModFromCurrentStability();
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putBoolean("Active", active);
        tag.putBoolean("Crafting", crafting);
        tag.putFloat("Stability", stability);
        tag.putInt("RecipeInstability", recipeInstability);
        recipeEssentia.writeToNBT(tag);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        active = tag.getBoolean("Active");
        crafting = tag.getBoolean("Crafting");
        stability = tag.getFloat("Stability");
        recipeInstability = tag.getInt("RecipeInstability");
        recipeEssentia.readFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save recipe ingredients
        if (recipeIngredients != null && !recipeIngredients.isEmpty()) {
            ListTag ingredientList = new ListTag();
            for (ItemStack stack : recipeIngredients) {
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    stack.save(registries, itemTag);
                    ingredientList.add(itemTag);
                }
            }
            tag.put("RecipeIngredients", ingredientList);
        }

        if (!recipeInput.isEmpty()) {
            tag.put("RecipeInput", recipeInput.save(registries, new CompoundTag()));
        }
        if (recipeOutput != null && recipeOutput instanceof ItemStack outStack) {
            tag.putString("RecipeOutputType", "@");
            tag.put("RecipeOutput", outStack.save(registries, new CompoundTag()));
        }
        if (recipeOutput instanceof Holder<?> outHolder && outHolder.unwrapKey().isPresent()) {
            tag.putString("RecipeOutputType", "E");
            tag.putString("RecipeOutputEnchantment", outHolder.unwrapKey().get().location().toString());
        }
        if (recipePlayer != null) {
            tag.putString("RecipePlayer", recipePlayer);
        }
        tag.putInt("RecipeType", recipeType);
        tag.putInt("RecipeXP", recipeXP);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        recipeIngredients = new ArrayList<>();
        if (tag.contains("RecipeIngredients")) {
            ListTag ingredientList = tag.getList("RecipeIngredients", 10);
            for (int i = 0; i < ingredientList.size(); i++) {
                recipeIngredients.add(ItemStack.parseOptional(registries, ingredientList.getCompound(i)));
            }
        }

        if (tag.contains("RecipeInput")) {
            recipeInput = ItemStack.parseOptional(registries, tag.getCompound("RecipeInput"));
        }
        String outputType = tag.getString("RecipeOutputType");
        if (outputType.equals("@") && tag.contains("RecipeOutput")) {
            recipeOutput = ItemStack.parseOptional(registries, tag.getCompound("RecipeOutput"));
        } else if (outputType.equals("E") && tag.contains("RecipeOutputEnchantment")) {
            ResourceLocation enchId = ResourceLocation.tryParse(tag.getString("RecipeOutputEnchantment"));
            if (enchId != null) {
                recipeOutput = registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .get(ResourceKey.create(Registries.ENCHANTMENT, enchId))
                        .orElse(null);
            }
        }
        recipePlayer = tag.getString("RecipePlayer");
        if (recipePlayer.isEmpty()) {
            recipePlayer = null;
        }
        recipeType = tag.getInt("RecipeType");
        recipeXP = tag.getInt("RecipeXP");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileInfusionMatrix tile) {
        tile.count++;
        
        if (tile.checkSurroundings) {
            tile.checkSurroundings = false;
            tile.scanSurroundings();
        }

        // Validate location periodically
        if (tile.count % (tile.crafting ? 20 : 100) == 0 && !tile.validLocation()) {
            tile.active = false;
            tile.setChanged();
            tile.syncTile(false);
            return;
        }

        // Replenish stability when active but not crafting
        if (tile.active && !tile.crafting && tile.stability < tile.stabilityCap && tile.count % Math.max(5, tile.countDelay) == 0) {
            tile.stability += Math.max(0.1f, tile.stabilityReplenish);
            if (tile.stability > tile.stabilityCap) {
                tile.stability = tile.stabilityCap;
            }
            tile.setChanged();
            tile.syncTile(false);
        }

        // Process crafting
        if (tile.active && tile.crafting && tile.count % tile.countDelay == 0) {
            tile.craftCycle();
            tile.setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileInfusionMatrix tile) {
        tile.doEffects();
    }

    private void doEffects() {
        // Handle crafting animation
        if (crafting) {
            if (craftCount == 0) {
                // TC4: infuserstart - played once when the infusion begins
                level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        ModSounds.INFUSER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            } else if (craftCount % 65 == 0) {
                // TC4: infuser - looping infusion sound
                level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        ModSounds.INFUSER.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            }
            craftCount++;
            // Rune particles swirling over the pedestal below the matrix
            FXDispatcher.INSTANCE.blockRunes(
                    worldPosition.getX(), worldPosition.getY() - 2, worldPosition.getZ(),
                    0.5f + level.random.nextFloat() * 0.2f, 0.1f, 0.7f + level.random.nextFloat() * 0.3f,
                    25, -0.03f);
        } else if (craftCount > 0) {
            craftCount -= 2;
            if (craftCount < 0) craftCount = 0;
            if (craftCount > 50) craftCount = 50;
        }

        // Startup animation
        if (active && startUp < 1.0f) {
            startUp += Math.max(startUp / 10.0f, 0.001f);
            if (startUp > 0.999f) startUp = 1.0f;
        }
        if (!active && startUp > 0.0f) {
            startUp -= startUp / 10.0f;
            if (startUp < 0.001f) startUp = 0.0f;
        }

        // Essentia/item streams flowing from the pedestals (and players) into the matrix
        for (String fxk : sourceFX.keySet().toArray(new String[0])) {
            SourceFX fx = sourceFX.get(fxk);
            if (fx.ticks <= 0) {
                sourceFX.remove(fxk);
                continue;
            }

            if (fx.loc.equals(worldPosition)) {
                // "loc == matrix" means the colour field carries an entity id (XP drain)
                Entity entity = level.getEntity(fx.color);
                if (entity != null) {
                    for (int a = 0; a < 4; a++) {
                        FXDispatcher.INSTANCE.drawInfusionParticles4(
                                entity.getX() + (level.random.nextFloat() - level.random.nextFloat()) * entity.getBbWidth(),
                                entity.getBoundingBox().minY + level.random.nextFloat() * entity.getBbHeight(),
                                entity.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * entity.getBbWidth(),
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                    }
                }
            } else {
                BlockEntity tile = level.getBlockEntity(fx.loc);
                if (tile instanceof TilePedestal pedestal) {
                    ItemStack is = pedestal.getItem(0);
                    if (!is.isEmpty()) {
                        if (level.random.nextInt(3) == 0) {
                            FXDispatcher.INSTANCE.drawInfusionParticles3(
                                    fx.loc.getX() + level.random.nextFloat(),
                                    fx.loc.getY() + level.random.nextFloat() + 1.0f,
                                    fx.loc.getZ() + level.random.nextFloat(),
                                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                        } else if (is.getItem() instanceof BlockItem blockItem) {
                            for (int a = 0; a < 4; a++) {
                                FXDispatcher.INSTANCE.drawInfusionParticles2(
                                        fx.loc.getX() + level.random.nextFloat(),
                                        fx.loc.getY() + level.random.nextFloat() + 1.0f,
                                        fx.loc.getZ() + level.random.nextFloat(),
                                        worldPosition, blockItem.getBlock().defaultBlockState(), 0);
                            }
                        } else {
                            for (int a = 0; a < 4; a++) {
                                FXDispatcher.INSTANCE.drawInfusionParticles1(
                                        fx.loc.getX() + 0.4f + level.random.nextFloat() * 0.2f,
                                        fx.loc.getY() + 1.23f + level.random.nextFloat() * 0.2f,
                                        fx.loc.getZ() + 0.4f + level.random.nextFloat() * 0.2f,
                                        worldPosition, is);
                            }
                        }
                    }
                } else {
                    fx.ticks = 0;
                }
            }

            fx.ticks--;
            sourceFX.put(fxk, fx);
        }

        // Sparks while the altar is unstable (TC4/TC6)
        if (crafting && stability < 0.0f && level.random.nextInt(250) <= Math.abs(stability)) {
            FXDispatcher.INSTANCE.spark(
                    worldPosition.getX() + level.random.nextFloat(),
                    worldPosition.getY() + level.random.nextFloat(),
                    worldPosition.getZ() + level.random.nextFloat(),
                    3.0f + level.random.nextFloat() * 2.0f,
                    0.7f + level.random.nextFloat() * 0.1f, 0.1f, 0.65f + level.random.nextFloat() * 0.1f, 0.8f);
        }

        // Sparks on asymmetric / problematic stabilizer blocks
        if (active && !problemBlocks.isEmpty() && level.random.nextInt(25) == 0) {
            BlockPos p = problemBlocks.get(level.random.nextInt(problemBlocks.size()));
            FXDispatcher.INSTANCE.spark(
                    p.getX() + level.random.nextFloat(),
                    p.getY() + level.random.nextFloat(),
                    p.getZ() + level.random.nextFloat(),
                    2.0f + level.random.nextFloat(),
                    0.7f + level.random.nextFloat() * 0.1f, 0.1f, 0.65f + level.random.nextFloat() * 0.1f, 0.8f);
        }
    }

    // ==================== Crafting ====================

    /**
     * Check if the altar structure is valid.
     */
    public boolean validLocation() {
        if (level == null) return false;
        
        // Check for pedestal 2 blocks below
        BlockState below2 = level.getBlockState(worldPosition.below(2));
        if (!(below2.getBlock() instanceof BlockPedestal)) {
            return false;
        }

        // Check for 4 pillars at corners
        BlockPos[] pillarPositions = {
            worldPosition.offset(-1, -2, -1),
            worldPosition.offset(1, -2, -1),
            worldPosition.offset(1, -2, 1),
            worldPosition.offset(-1, -2, 1)
        };

        for (BlockPos pillarPos : pillarPositions) {
            BlockState pillarState = level.getBlockState(pillarPos);
            if (!(pillarState.getBlock() instanceof BlockPillarTC)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Start crafting when activated by a player with a caster.
     */
    public void startCrafting(Player player) {
        if (!validLocation()) {
            active = false;
            syncTile(false);
            return;
        }

        scanSurroundings();

        // Get input from center pedestal
        BlockEntity centerTE = level.getBlockEntity(worldPosition.below(2));
        recipeInput = ItemStack.EMPTY;
        if (centerTE instanceof TilePedestal pedestal) {
            ItemStack centerItem = pedestal.getItem(0);
            if (!centerItem.isEmpty()) {
                recipeInput = centerItem.copy();
            }
        }

        if (recipeInput.isEmpty()) return;

        // Gather components from surrounding pedestals
        List<ItemStack> components = new ArrayList<>();
        for (BlockPos pedestalPos : pedestals) {
            BlockEntity te = level.getBlockEntity(pedestalPos);
            if (te instanceof TilePedestal pedestal) {
                ItemStack stack = pedestal.getItem(0);
                if (!stack.isEmpty()) {
                    components.add(stack.copy());
                }
            }
        }

        if (components.isEmpty()) return;

        // Find matching infusion recipe
        InfusionRecipeType recipe = ThaumcraftCraftingManager.findMatchingInfusionRecipe(
                components, recipeInput, player, level);
        
        if (costMult < 0.5f) costMult = 0.5f;
        
        if (recipe != null) {
            recipeType = 0;
            recipeIngredients = new ArrayList<>(components);
            recipeOutput = recipe.getRecipeOutput(player, recipeInput, components);
            recipeInstability = recipe.getInstability(player, recipeInput, components);
            
            // Apply cost multiplier to aspects
            AspectList al = recipe.getAspects(player, recipeInput, components);
            AspectList al2 = new AspectList();
            for (Aspect as : al.getAspects()) {
                int adjusted = (int)(al.getAmount(as) * costMult);
                if (adjusted > 0) {
                    al2.add(as, adjusted);
                }
            }
            recipeEssentia = al2;
            recipePlayer = player.getUUID().toString();
            recipeXP = 0;
            crafting = true;
            
            level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            syncTile(false);
            setChanged();
        } else {
            // TC4: if no normal infusion recipe matches, try the infusion
            // enchantment recipes (vanilla enchantments applied to the item).
            InfusionEnchantmentRecipe enchantmentRecipe = ThaumcraftCraftingManager.findMatchingInfusionEnchantmentRecipe(
                    components, recipeInput, player, level);
            if (enchantmentRecipe != null) {
                recipeType = 1;
                recipeIngredients = new ArrayList<>(components);
                recipeOutput = enchantmentRecipe.getEnchantment();
                recipeInstability = enchantmentRecipe.calcInstability(recipeInput);

                // TC4: base essentia cost plus the enchantment modifier
                AspectList esscost = enchantmentRecipe.getAspects().copy();
                float essmod = enchantmentRecipe.getEssentiaMod(recipeInput);
                for (Aspect as : esscost.getAspects()) {
                    esscost.add(as, (int)(esscost.getAmount(as) * essmod));
                }

                // The altar cost multiplier (ancient/eldritch pedestals) applies here too
                AspectList al2 = new AspectList();
                for (Aspect as : esscost.getAspects()) {
                    int adjusted = (int)(esscost.getAmount(as) * costMult);
                    if (adjusted > 0) {
                        al2.add(as, adjusted);
                    }
                }
                recipeEssentia = al2;

                recipeXP = enchantmentRecipe.calcXP(recipeInput);
                recipePlayer = player.getUUID().toString();
                crafting = true;

                level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                syncTile(false);
                setChanged();
            }
        }
    }

    /**
     * Process one cycle of the crafting.
     */
    private void craftCycle() {
        if (level == null) {
            cancelCrafting();
            return;
        }

        boolean valid = false;

        // Apply stability loss based on instability
        float ff = level.random.nextFloat() * getLossPerCycle();
        stability -= ff;
        stability += stabilityReplenish;
        stability = Math.max(-100, Math.min(stabilityCap, stability));

        // Check if input is still valid
        BlockEntity centerTE = level.getBlockEntity(worldPosition.below(2));
        if (centerTE instanceof TilePedestal pedestal && !pedestal.getItem(0).isEmpty()) {
            ItemStack i2 = pedestal.getItem(0).copy();
            if (ThaumcraftInvHelper.areItemStacksEqualForCrafting(i2, recipeInput)) {
                valid = true;
            }
        }

        // Check for instability events
        if (!valid || (stability < 0.0f && level.random.nextInt(1500) <= Math.abs(stability))) {
            triggerInstabilityEvent();
            stability += 5.0f + level.random.nextFloat() * 5.0f;
            grantInstabilityResearch();
            if (valid) return;
        }

        if (!valid) {
            cancelCrafting();
            return;
        }

        if (countDelay < 1) countDelay = 1;

        // TC4: infusion enchantment recipes consume experience from nearby
        // players before any essentia is drained. The infusion stalls until the
        // required amount of levels has been paid.
        if (recipeType == 1 && recipeXP > 0) {
            if (drainPlayerExperience()) {
                countDelay = 20;
                return;
            }
            // No player with levels nearby - unstable waiting
            Aspect[] waiting = recipeEssentia.getAspects();
            if (waiting != null && waiting.length > 0 && level.random.nextInt(3) == 0) {
                Aspect as = waiting[level.random.nextInt(waiting.length)];
                recipeEssentia.add(as, 1);
                stability -= 0.25f;
                syncTile(false);
                setChanged();
            }
            return;
        }
        if (recipeType == 1) {
            countDelay = 10;
        }

        // Draw essentia
        if (recipeEssentia.visSize() > 0) {
            for (Aspect aspect : recipeEssentia.getAspects()) {
                int na = recipeEssentia.getAmount(aspect);
                if (na > 0) {
                    if (EssentiaHandler.drainEssentia(this, aspect, null, 12, (na > 1) ? countDelay : 0)) {
                        recipeEssentia.reduce(aspect, 1);
                        syncTile(false);
                        setChanged();
                        return;
                    }
                    // Missing essentia - reduce stability
                    stability -= 0.25f;
                    syncTile(false);
                    setChanged();
                }
            }
            checkSurroundings = true;
            return;
        }

        // Consume ingredients from pedestals
        if (recipeIngredients != null && !recipeIngredients.isEmpty()) {
            for (int a = 0; a < recipeIngredients.size(); a++) {
                for (BlockPos cc : pedestals) {
                    BlockEntity te = level.getBlockEntity(cc);
                    if (te instanceof TilePedestal ped && !ped.getItem(0).isEmpty()) {
                        if (ThaumcraftInvHelper.areItemStacksEqualForCrafting(ped.getItem(0), recipeIngredients.get(a))) {
                            if (itemCount == 0) {
                                itemCount = 5;
                                // Send FX packet for infusion crafting
                                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                    PacketHandler.sendToAllTrackingChunk(
                                            new PacketFXInfusionSource(worldPosition, cc, 0x9966FF),
                                            serverLevel, worldPosition);
                                }
                            } else if (itemCount-- <= 1) {
                                ItemStack container = ped.getItem(0).getItem().getCraftingRemainingItem(ped.getItem(0));
                                ped.setItem(0, container == null || container.isEmpty() ? ItemStack.EMPTY : container.copy());
                                te.setChanged();
                                ped.syncTile(false);
                                recipeIngredients.remove(a);
                                setChanged();
                            }
                            return;
                        }
                    }
                }
                // Missing ingredient - add stability penalty
                Aspect[] ingEss = recipeEssentia.getAspects();
                if (ingEss != null && ingEss.length > 0 && level.random.nextInt(1 + a) == 0) {
                    Aspect as = ingEss[level.random.nextInt(ingEss.length)];
                    recipeEssentia.add(as, 1);
                    stability -= 0.25f;
                    syncTile(false);
                    setChanged();
                }
            }
            return;
        }

        // All done - finish crafting
        crafting = false;
        craftingFinish();
        recipeOutput = null;
        syncTile(false);
        setChanged();
    }

    /**
     * TC4: infusion enchantment recipes draw experience levels from players
     * standing within 10 blocks of the altar. One level is consumed per call.
     *
     * @return true if a level was consumed
     */
    private boolean drainPlayerExperience() {
        List<Player> targets = level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition).inflate(10.0));
        for (Player target : targets) {
            if (target.experienceLevel > 0) {
                target.giveExperienceLevels(-1);
                recipeXP--;
                target.hurt(level.damageSources().magic(), level.random.nextInt(2));

                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    PacketHandler.sendToAllTrackingChunk(
                            new PacketFXInfusionSource(worldPosition, worldPosition, target.getId()),
                            serverLevel, worldPosition);
                }
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 2.0f + level.random.nextFloat() * 0.4f);
                return true;
            }
        }
        return false;
    }

    private void cancelCrafting() {
        crafting = false;
        recipeEssentia = new AspectList();
        recipeInstability = 0;
        recipeXP = 0;
        recipeType = 0;
        level.playSound(null, worldPosition, ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 1.0f, 0.6f);
        syncTile(false);
    }

    /**
     * Look up the player that started the infusion. Older saves stored the player
     * name, newer ones store the UUID, so both are accepted.
     */
    private Player findRecipePlayer() {
        if (recipePlayer == null || recipePlayer.isEmpty()) {
            return null;
        }
        try {
            Player p = level.getPlayerByUUID(UUID.fromString(recipePlayer));
            if (p != null) {
                return p;
            }
        } catch (IllegalArgumentException ignored) {
            // Stored value is a player name, not a UUID
        }
        for (Player pl : level.players()) {
            if (pl.getName().getString().equals(recipePlayer) || pl.getUUID().toString().equals(recipePlayer)) {
                return pl;
            }
        }
        return null;
    }

    private void craftingFinish() {
        BlockEntity te = level.getBlockEntity(worldPosition.below(2));
        if (te instanceof TilePedestal pedestal) {
            float dmg = 1.0f;
            
            if (recipeOutput instanceof ItemStack outStack) {
                ItemStack qs = outStack.copy();
                ItemStack inputStack = pedestal.getItem(0);
                
                // Preserve damage ratio
                if (inputStack.isDamageableItem() && inputStack.isDamaged()) {
                    dmg = inputStack.getDamageValue() / (float)inputStack.getMaxDamage();
                    if (qs.isDamageableItem() && !qs.isDamaged()) {
                        qs.setDamageValue((int)(qs.getMaxDamage() * dmg));
                    }
                }
                
                pedestal.setItemFromInfusion(qs);
            } else if (recipeOutput instanceof Holder<?> outHolder && outHolder.value() instanceof Enchantment) {
                // Infusion enchantment recipe: add one level of the enchantment
                // to the item that is sitting on the central pedestal.
                @SuppressWarnings("unchecked")
                Holder<Enchantment> enchHolder = (Holder<Enchantment>) outHolder;
                ItemStack temp = pedestal.getItem(0);
                InfusionEnchantment.addLevel(temp, enchHolder);
                pedestal.setChanged();
                syncTile(false);
                te.setChanged();
            } else if (recipeOutput instanceof Enchantment ench) {
                ItemStack temp = pedestal.getItem(0);
                Holder<Enchantment> enchHolder = TCCompat.enchantment(this.getLevel(), ench);
                EnchantmentHelper.updateEnchantments(temp, mutable -> {
                    int currentLevel = mutable.getLevel(enchHolder);
                    mutable.set(enchHolder, currentLevel + 1);
                });
                syncTile(false);
                te.setChanged();
            }
            
            // Fire crafting event
            if (recipePlayer != null) {
                Player p = findRecipePlayer();
                if (p != null) {
                    NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(p, pedestal.getItem(0), null));
                }
            }
            
            recipeEssentia = new AspectList();
            recipeInstability = 0;
            recipeXP = 0;
            recipeType = 0;
            syncTile(false);
            setChanged();
            
            level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            // TC4 block event 12 - bamf effect on the pedestal when the infusion completes
            BlockPos centerPos = worldPosition.below(2);
            level.blockEvent(centerPos, level.getBlockState(centerPos).getBlock(), 12, 0);
        }
    }

    // ==================== Instability Events ====================

    private void triggerInstabilityEvent() {
        int event = level.random.nextInt(24);
        switch (event) {
            case 0, 1, 2, 3 -> instabilityEjectItem(0);
            case 4, 5, 6 -> instabilityWarp();
            case 7, 8, 9 -> instabilityZap(false);
            case 10, 11 -> instabilityZap(true);
            case 12, 13 -> instabilityEjectItem(1);
            case 14, 15 -> instabilityEjectItem(2);
            case 16 -> instabilityEjectItem(3);
            case 17 -> instabilityEjectItem(4);
            case 18, 19 -> instabilityHarm(false);
            case 20, 21 -> instabilityEjectItem(5);
            case 22 -> instabilityHarm(true);
            case 23 -> level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, 
                    worldPosition.getZ() + 0.5, 1.5f + level.random.nextFloat(), Level.ExplosionInteraction.NONE);
        }
    }

    private void instabilityZap(boolean all) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(10.0));
        for (LivingEntity target : targets) {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                PacketHandler.sendToAllTrackingChunk(
                        new PacketFXBlockArc(worldPosition, target, 0.3f - level.random.nextFloat() * 0.1f,
                                0.0f, 0.3f - level.random.nextFloat() * 0.1f),
                        serverLevel, worldPosition);
            }
            target.hurt(level.damageSources().magic(), 4 + level.random.nextInt(4));
            if (!all) break;
        }
    }

    private void instabilityHarm(boolean all) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(10.0));
        for (LivingEntity target : targets) {
            if (level.random.nextBoolean()) {
                // Flux taint effect
                if (ModEffects.FLUX_TAINT != null) {
                    target.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 120, 0, false, true));
                }
            } else {
                // Vis exhaust effect
                if (ModEffects.VIS_EXHAUST != null) {
                    MobEffectInstance pe = new MobEffectInstance(ModEffects.VIS_EXHAUST, 2400, 0, true, true);
                    target.addEffect(pe);
                }
            }
            if (!all) break;
        }
    }

    private void instabilityWarp() {
        List<Player> targets = level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition).inflate(10.0));
        if (!targets.isEmpty()) {
            Player target = targets.get(level.random.nextInt(targets.size()));
            if (level.random.nextFloat() < 0.25f) {
                ResearchManager.addWarpToPlayer(target, 1, IPlayerWarp.EnumWarpType.NORMAL);
            } else {
                ResearchManager.addWarpToPlayer(target, 2 + level.random.nextInt(4), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
        }
    }

    private void instabilityEjectItem(int type) {
        for (int retries = 0; retries < 25 && !pedestals.isEmpty(); retries++) {
            BlockPos cc = pedestals.get(level.random.nextInt(pedestals.size()));
            BlockEntity te = level.getBlockEntity(cc);
            if (te instanceof TilePedestal ped && !ped.getItem(0).isEmpty()) {
                // TC4: an inlay network connected to a charged stabilizer can
                // absorb the event and protect the item on the pedestal.
                BlockPos stabPos = ped.findInstabilityMitigator();
                if (stabPos != null) {
                    BlockEntity ste = level.getBlockEntity(stabPos);
                    if (ste instanceof TileStabilizer stabilizer) {
                        if (stabilizer.mitigate(5 + level.random.nextInt(6))) {
                            level.blockEvent(cc, level.getBlockState(cc).getBlock(), 5, 0);
                            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                PacketHandler.sendToAllTrackingChunk(
                                        new PacketFXBlockArc(worldPosition, cc.above(), 0.3f - level.random.nextFloat() * 0.1f,
                                                0.0f, 0.3f - level.random.nextFloat() * 0.1f),
                                        serverLevel, worldPosition);
                                PacketHandler.sendToAllTrackingChunk(
                                        new PacketFXBlockArc(cc.above(), stabPos, 0.3f - level.random.nextFloat() * 0.1f,
                                                0.0f, 0.3f - level.random.nextFloat() * 0.1f),
                                        serverLevel, cc);
                            }
                            ped.syncTile(false);
                            stabilizer.syncTile(false);
                            return;
                        }
                    }
                }

                if (type <= 3 || type == 5) {
                    InventoryUtils.dropItems(level, cc);
                } else {
                    ped.setItem(0, ItemStack.EMPTY);
                }
                te.setChanged();
                ped.syncTile(false);
                
                if (type == 1 || type == 3) {
                    // Place flux goo
                    if (ModBlocks.FLUX_GOO != null) {
                        level.setBlockAndUpdate(cc.above(), ModBlocks.FLUX_GOO.get().defaultBlockState());
                    }
                    level.playSound(null, cc, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.3f, 1.0f);
                } else if (type == 2 || type == 4) {
                    // Add flux to aura
                    AuraHelper.polluteAura(level, cc, 5 + level.random.nextInt(5), true);
                } else if (type == 5) {
                    level.explode(null, cc.getX() + 0.5f, cc.getY() + 0.5f, cc.getZ() + 0.5f, 
                            1.0f, Level.ExplosionInteraction.NONE);
                }
                
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    PacketHandler.sendToAllTrackingChunk(
                            new PacketFXBlockArc(worldPosition, cc.above(), 0.3f - level.random.nextFloat() * 0.1f,
                                    0.0f, 0.3f - level.random.nextFloat() * 0.1f),
                            serverLevel, worldPosition);
                }
                // TC4 block event 11 - bamf effect where the item was knocked off
                level.blockEvent(cc, level.getBlockState(cc).getBlock(), 11, 0);
                return;
            }
        }
    }

    private void grantInstabilityResearch() {
        List<Player> targets = level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition).inflate(10.0));
        for (Player player : targets) {
            if (!ThaumcraftCapabilities.knowsResearch(player, "!INSTABILITY")) {
                ResearchManager.completeResearch(player, "!INSTABILITY");
                player.displayClientMessage(
                        Component.translatable("got.instability").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), 
                        true);
            }
        }
    }

    // ==================== Surroundings ====================

    /**
     * Scan surroundings for pedestals and stabilizers.
     */
    private void scanSurroundings() {
        Set<Long> stuff = new HashSet<>();
        pedestals.clear();
        tempBlockCount.clear();
        problemBlocks.clear();
        cycleTime = 10;
        stabilityReplenish = 0.0f;
        costMult = 1.0f;

        try {
            // Scan for pedestals and stabilizers
            for (int xx = -8; xx <= 8; xx++) {
                for (int zz = -8; zz <= 8; zz++) {
                    for (int yy = -3; yy <= 7; yy++) {
                        if (xx == 0 && zz == 0) continue;
                        
                        BlockPos bp = worldPosition.offset(xx, -yy, zz);
                        Block bi = level.getBlockState(bp).getBlock();
                        
                        if (bi instanceof BlockPedestal) {
                            pedestals.add(bp);
                        }
                        
                        // Check for stabilizers - first check if block implements interface
                        if (bi instanceof IInfusionStabiliser stabiliser) {
                            if (stabiliser.canStabaliseInfusion(level, bp)) {
                                stuff.add(bp.asLong());
                            }
                        }
                        // Fallback: check for vanilla skulls and candles
                        else if (bi == Blocks.SKELETON_SKULL || bi == Blocks.WITHER_SKELETON_SKULL ||
                            bi == Blocks.ZOMBIE_HEAD || bi == Blocks.CREEPER_HEAD || bi == Blocks.PLAYER_HEAD ||
                            bi == Blocks.CANDLE || bi == Blocks.CANDLE_CAKE ||
                            bi == Blocks.WHITE_CANDLE || bi == Blocks.ORANGE_CANDLE || bi == Blocks.MAGENTA_CANDLE ||
                            bi == Blocks.LIGHT_BLUE_CANDLE || bi == Blocks.YELLOW_CANDLE || bi == Blocks.LIME_CANDLE ||
                            bi == Blocks.PINK_CANDLE || bi == Blocks.GRAY_CANDLE || bi == Blocks.LIGHT_GRAY_CANDLE ||
                            bi == Blocks.CYAN_CANDLE || bi == Blocks.PURPLE_CANDLE || bi == Blocks.BLUE_CANDLE ||
                            bi == Blocks.BROWN_CANDLE || bi == Blocks.GREEN_CANDLE || bi == Blocks.RED_CANDLE ||
                            bi == Blocks.BLACK_CANDLE) {
                            stuff.add(bp.asLong());
                        }
                    }
                }
            }

            // Process stabilizers (check symmetry)
            while (!stuff.isEmpty()) {
                Long[] posArray = stuff.toArray(new Long[0]);
                if (posArray.length == 0 || posArray[0] == null) break;
                
                long lp = posArray[0];
                BlockPos c1 = BlockPos.of(lp);
                int x2 = worldPosition.getX() - c1.getX();
                int z2 = worldPosition.getZ() - c1.getZ();
                int x3 = worldPosition.getX() + x2;
                int z3 = worldPosition.getZ() + z2;
                BlockPos c2 = new BlockPos(x3, c1.getY(), z3);
                
                Block sb1 = level.getBlockState(c1).getBlock();
                Block sb2 = level.getBlockState(c2).getBlock();
                
                // Get stabilization amounts - check for IInfusionStabiliserExt first
                float amt1 = getStabilizationAmount(sb1, c1);
                float amt2 = getStabilizationAmount(sb2, c2);
                
                // Check for custom symmetry penalty
                boolean hasCustomPenalty = false;
                float customPenalty = 0;
                if (sb1 instanceof IInfusionStabiliserExt ext1) {
                    if (ext1.hasSymmetryPenalty(level, c1, c2)) {
                        hasCustomPenalty = true;
                        customPenalty = ext1.getSymmetryPenalty(level, c1);
                    }
                }
                
                // Check for symmetry
                if (sb1 == sb2 && amt1 == amt2 && !hasCustomPenalty) {
                    // Symmetrical placement - good
                    stabilityReplenish += calcDiminishingReturns(sb1, amt1);
                } else if (hasCustomPenalty) {
                    // Custom symmetry penalty
                    stabilityReplenish -= customPenalty;
                    problemBlocks.add(c1);
                } else {
                    // Asymmetrical - penalty
                    stabilityReplenish -= Math.max(amt1, amt2);
                    problemBlocks.add(c1);
                }
                
                stuff.remove(c2.asLong());
                stuff.remove(lp);
            }

            // Check pillar types for bonuses
            BlockPos[] pillars = {
                worldPosition.offset(-1, -2, -1),
                worldPosition.offset(1, -2, -1),
                worldPosition.offset(1, -2, 1),
                worldPosition.offset(-1, -2, 1)
            };
            
            boolean allAncient = true;
            boolean allEldritch = true;
            for (BlockPos pillarPos : pillars) {
                Block pillar = level.getBlockState(pillarPos).getBlock();
                if (!pillar.equals(ModBlocks.ANCIENT_PILLAR.get())) allAncient = false;
                if (!pillar.equals(ModBlocks.ELDRITCH_PILLAR.get())) allEldritch = false;
            }
            
            if (allAncient) {
                cycleTime--;
                costMult -= 0.1f;
                stabilityReplenish -= 0.1f;
            }
            if (allEldritch) {
                cycleTime -= 3;
                costMult += 0.05f;
                stabilityReplenish += 0.2f;
            }

            // Check pedestal types
            for (BlockPos cc : pedestals) {
                Block bb = level.getBlockState(cc).getBlock();
                if (bb.equals(ModBlocks.PEDESTAL_ELDRITCH.get())) costMult += 0.0025f;
                if (bb.equals(ModBlocks.PEDESTAL_ANCIENT.get())) costMult -= 0.01f;
            }
            
            countDelay = cycleTime / 2;
        } catch (Exception e) {
            // Ignore scanning errors
        }
    }
    
    /**
     * Get the stabilization amount for a block at a position.
     */
    private float getStabilizationAmount(Block block, BlockPos pos) {
        if (block instanceof IInfusionStabiliserExt ext) {
            return ext.getStabilizationAmount(level, pos);
        }
        // Default amount for standard stabilizers (skulls, candles)
        return 0.1f;
    }

    private float calcDiminishingReturns(Block b, float base) {
        float bb = base;
        int c = tempBlockCount.getOrDefault(b, 0);
        if (c > 0) {
            bb *= (float)Math.pow(0.75, c);
        }
        tempBlockCount.put(b, c + 1);
        return bb;
    }

    // ==================== Activation ====================

    /**
     * Called when player right-clicks with a caster.
     */
    public boolean onCasterRightClick(Player player) {
        if (level.isClientSide && active && !crafting) {
            checkSurroundings = true;
        }
        
        if (!level.isClientSide && active && !crafting) {
            startCrafting(player);
            return true;
        }
        
        if (!level.isClientSide && !active && validLocation()) {
            level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            active = true;
            syncTile(false);
            setChanged();
            return true;
        }
        
        return false;
    }

    // ==================== IWandable ====================

    /**
     * Called by the wand item (ItemWandCasting/ WandItem#useOn) when the player
     * right-clicks the matrix with a wand. Mirrors TC4 TileInfusionMatrix behaviour:
     * - active & not crafting  -> start the infusion
     * - inactive & valid altar -> activate the matrix
     * - otherwise              -> let the wand fall through to other triggers
     */
    @Override
    public int onWandRightClick(Level world, ItemStack wandStack, Player player, int x, int y, int z, int side, int md) {
        return onCasterRightClick(player) ? 0 : -1;
    }

    @Override
    public ItemStack onWandRightClick(Level world, ItemStack wandStack, Player player) {
        return wandStack;
    }

    @Override
    public void onUsingWandTick(ItemStack wandStack, Player player, int count) {
        // Not used
    }

    @Override
    public void onWandStoppedUsing(ItemStack wandStack, Level world, Player player, int count) {
        // Not used
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return recipeEssentia;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Not used
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    // ==================== Goggles Display ====================

    public String[] getGogglesText() {
        float lpc = getLossPerCycle();
        if (lpc != 0.0f) {
            return new String[] {
                "stability." + getStability().name(),
                FORMATTER.format(stabilityReplenish) + " stability.gain",
                "stability.range " + FORMATTER.format(lpc) + " stability.loss"
            };
        }
        return new String[] {
            "stability." + getStability().name(),
            FORMATTER.format(stabilityReplenish) + " stability.gain"
        };
    }

    // ==================== Rendering ====================

    public AABB getCustomRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 0.1, worldPosition.getY() - 0.1, worldPosition.getZ() - 0.1,
                worldPosition.getX() + 1.1, worldPosition.getY() + 1.1, worldPosition.getZ() + 1.1
        );
    }

    // ==================== Inner Classes ====================

    public static class SourceFX {
        public BlockPos loc;
        public int ticks;
        public int color;
        public int entity;

        public SourceFX(BlockPos loc, int ticks, int color) {
            this.loc = loc;
            this.ticks = ticks;
            this.color = color;
        }
    }
}




