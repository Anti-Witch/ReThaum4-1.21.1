package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.items.essentia.ItemJarNode;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

import thaumcraft.common.lib.utils.NBTHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TileNode - The main aura node tile entity.
 * Contains logic for all 6 node types: NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE
 * Handles vis regeneration, node discharge, taint/pure biome conversion, hungry node consumption, etc.
 *
 * Ported from TC4 (OpenTC4-master) to 1.21.1
 */
public class TileNode extends BlockEntity implements INode, IAspectContainer, IWandable {

    // Static tracking of all node locations
    private static final Map<String, List<Integer>> locations = new HashMap<>();

    // Node properties
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier = null;
    private final AspectList aspects = new AspectList();
    private final AspectList aspectsBase = new AspectList();
    private String id = null;
    private byte nodeLock = 0;
    private int regeneration = -1;
    private int wait = 0;
    private long lastActive = 0L;
    private int count = 0;
    private boolean catchUp = false;

    // Draining state (when player uses wand on node)
    private Entity drainEntity = null;
    private Vec3 drainCollision = null;
    private int drainColor = 0xFFFFFF;
    private java.awt.Color targetColor = new java.awt.Color(0xFFFFFF);
    private java.awt.Color color = new java.awt.Color(0xFFFFFF);

    // ============================================================
    // Energized Node Mechanics (TC4)
    // ============================================================

    // Energized state - when true, node produces cVis per tick
    private boolean energized = false;

    // Cached cVis values for each primal aspect (calculated from aspectsBase)
    private final int[] cachedCVis = new int[6]; // One per primal aspect

    // Last time cVis was produced (for wand charging via workbench + relay)
    private long lastCVisProduction = 0;

    public TileNode(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NODE.get(), pos, state);
    }

    public TileNode(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ============================================================
    // INode Implementation
    // ============================================================

    @Override
    public String getId() {
        if (id == null) {
            id = generateId();
        }
        return id;
    }

    public String generateId() {
        // NOTE: id must always be assigned - this is also called during chunk save
        // when the level reference may already be gone (level == null). Previously
        // the fallback returned "" without assigning id, so saveAdditional wrote a
        // null nodeId and the chunk save crashed with an NPE.
        int dimId = (level != null) ? level.dimension().location().toString().hashCode() : 0;
        id = dimId + ":" + worldPosition.getX() + ":" + worldPosition.getY() + ":" + worldPosition.getZ();
        if (level != null && locations != null) {
            List<Integer> t = new ArrayList<>();
            t.add(dimId);
            t.add(worldPosition.getX());
            t.add(worldPosition.getY());
            t.add(worldPosition.getZ());
            locations.put(id, t);
        }
        return id;
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public AspectList getAspectsBase() {
        return aspectsBase;
    }

    @Override
    public void setAspects(AspectList aspects) {
        this.aspects.aspects.clear();
        this.aspects.add(aspects);
        this.aspectsBase.aspects.clear();
        this.aspectsBase.add(aspects.copy());
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public void setNodeModifier(NodeModifier nodeModifier) {
        this.nodeModifier = nodeModifier;
    }

    @Override
    public NodeModifier getNodeModifier() {
        return nodeModifier;
    }

    @Override
    public int getNodeVisBase(Aspect aspect) {
        return aspectsBase.getAmount(aspect);
    }

    @Override
    public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        if (aspectsBase.getAmount(aspect) < nodeVisBase) {
            aspectsBase.merge(aspect, nodeVisBase);
        } else {
            aspectsBase.remove(aspect, aspectsBase.getAmount(aspect) - nodeVisBase);
        }
    }

    /**
     * Set the node ID (used when capturing node into jar).
     */
    public void setId(String id) {
        this.id = id;
    }

    // ============================================================
    // IAspectContainer Implementation
    // ============================================================

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        int left = amount + aspects.getAmount(aspect) - aspectsBase.getAmount(aspect);
        left = Math.max(left, 0);
        aspects.add(aspect, amount - left);
        return left;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return aspects.reduce(aspect, amount);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return aspects.getAmount(tag) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        for (Aspect a : ot.getAspects()) {
            if (aspects.getAmount(a) < ot.getAmount(a)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int containerContains(Aspect tag) {
        return aspects.getAmount(tag);
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false; // Not implemented for nodes
    }

    // ============================================================
    // TileEntity Methods
    // ============================================================

    @Override
    public void onLoad() {
        super.onLoad();
        if (id == null) {
            generateId();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (locations != null && id != null) {
            locations.remove(id);
        }
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            // Client-side particle effects for DARK nodes
            if (nodeType == NodeType.DARK && count % 50 == 0) {
                // Sinister node tracking for compass
                // TODO: Implement compass integration
            }
            // Energized node client effects
            if (energized && count % 10 == 0) {
                // Client-side particle effects for energized node
                // TODO: Add particle effects
            }
            count++;
            return;
        }

        boolean change = false;

        // Handle hungry node first (particles on client, entity pull on server)
        change = handleHungryNodeFirst(change);

        count++;

        // Server-side logic
        change = handleDischarge(change);
        change = handleRecharge(change);

        // Check node lock (stabilizer) periodically
        if (count % 40 == 0) {
            checkLock();
        }

        // Energized node logic - produces cVis every tick
        change = handleEnergizedNode(change) || change;

        change = handleTaintNode(change);
        change = handleNodeStability(change);
        change = handleDarkNode(change);
        change = handlePureNode(change);
        change = handleHungryNodeSecond(change);

        if (change) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ============================================================
    // Node Behavior Handlers
    // ============================================================

    private boolean handleHungryNodeFirst(boolean change) {
        if (nodeType == NodeType.HUNGRY) {
            if (level.isClientSide) {
                // Particle effects for hungry node
                for (int a = 0; a < 2; a++) {
                    int tx = worldPosition.getX() + level.random.nextInt(16) - level.random.nextInt(16);
                    int ty = worldPosition.getY() + level.random.nextInt(16) - level.random.nextInt(16);
                    int tz = worldPosition.getZ() + level.random.nextInt(16) - level.random.nextInt(16);
                    if (ty > level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, tx, tz)) {
                        ty = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, tx, tz);
                    }
                    Vec3 v1 = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                    Vec3 v2 = new Vec3(tx + 0.5, ty + 0.5, tz + 0.5);
                    // TODO: Implement ray trace and particle effects
                }
            } else {
                // Server-side: pull entities and damage them
                AABB box = new AABB(worldPosition).inflate(15.0);
                List<Entity> ents = level.getEntities(null, box);
                for (Entity eo : ents) {
                    if (!(eo instanceof Player player) || !player.getAbilities().invulnerable) {
                        if (eo.isAlive() && !eo.isInvulnerable()) {
                            double d = worldPosition.distToCenterSqr(eo.position());
                            if (d < 4.0) {
                                eo.hurt(level.damageSources().generic(), 1.0f);
                                if (!eo.isAlive()) {
                                    // Scan drops for aspects
                                    // TODO: Implement scan manager integration
                                }
                            }
                        }
                        // Pull entities towards node
                        double var3 = (worldPosition.getX() + 0.5 - eo.getX()) / 15.0;
                        double var5 = (worldPosition.getY() + 0.5 - eo.getY()) / 15.0;
                        double var7 = (worldPosition.getZ() + 0.5 - eo.getZ()) / 15.0;
                        double var9 = Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7);
                        double var11 = 1.0 - var9;
                        if (var11 > 0.0) {
                            var11 *= var11;
                            eo.setDeltaMovement(eo.getDeltaMovement().add(var3 / var9 * var11 * 0.15, var5 / var9 * var11 * 0.25, var7 / var9 * var11 * 0.15));
                        }
                    }
                }
            }
        }
        return change;
    }

    private boolean handleDischarge(boolean change) {
        // Nodes discharge vis into nearby lower-vis nodes (TC4 oxygenation/merging check).
        // Verbatim port of OpenTC4 TileNode.handleDischarge (TC4).
        if (level == null) return change;

        // Only discharge while this node's block is the NODE block and not locked
        if (level.getBlockState(worldPosition).getBlock() != ModBlocks.NODE.get() || getLock() == 1) {
            return change;
        }

        if (nodeModifier == NodeModifier.FADING) {
            return change;
        }

        boolean shiny = nodeType == NodeType.HUNGRY || nodeModifier == NodeModifier.BRIGHT;
        int inc = nodeModifier == null ? 2 : (shiny ? 1 : (nodeModifier == NodeModifier.PALE ? 3 : 2));
        if (count % inc != 0) {
            return change;
        }

        int x = level.random.nextInt(5) - level.random.nextInt(5);
        int y = level.random.nextInt(5) - level.random.nextInt(5);
        int z = level.random.nextInt(5) - level.random.nextInt(5);

        if (nodeModifier == NodeModifier.PALE && level.random.nextBoolean()) {
            return change;
        }

        if (x != 0 || y != 0 || z != 0) {
            BlockEntity te = level.getBlockEntity(worldPosition.offset(x, y, z));
            if (te instanceof INode
                    && level.getBlockState(worldPosition.offset(x, y, z)).getBlock() == ModBlocks.NODE.get()) {
                if (te instanceof TileNode other && other.getLock() > 0) {
                    return change;
                }

                INode anotherNode = (INode) te;
                int ndavg = (anotherNode.getAspects().visSize() + anotherNode.getAspectsBase().visSize()) / 2;
                int thisavg = (this.getAspects().visSize() + this.getAspectsBase().visSize()) / 2;

                if (ndavg < thisavg && anotherNode.getAspects().size() > 0) {
                    Aspect a = anotherNode.getAspects().getAspects()[level.random.nextInt(anotherNode.getAspects().size())];
                    boolean u = false;

                    if (this.getAspects().getAmount(a) < this.getNodeVisBase(a)
                            && anotherNode.takeFromContainer(a, 1)) {
                        this.addToContainer(a, 1);
                        u = true;
                    } else if (anotherNode.takeFromContainer(a, 1)) {
                        if (level.random.nextInt(1 + (int) (this.getNodeVisBase(a) / (shiny ? 1.5 : 1.0))) == 0) {
                            this.aspectsBase.add(a, 1);
                            if (nodeModifier == NodeModifier.PALE && level.random.nextInt(100) == 0) {
                                this.setNodeModifier(null);
                                this.regeneration = -1;
                            }
                        }
                        if (level.random.nextInt(3) == 0) {
                            anotherNode.setNodeVisBase(a, (short) (anotherNode.getNodeVisBase(a) - 1));
                        }
                        u = true;
                    }

                    if (u) {
                        if (te instanceof TileNode otherTile) {
                            otherTile.wait = otherTile.regeneration / 2;
                        }
                        change = true;
                        setChanged();
                        level.sendBlockUpdated(worldPosition.offset(x, y, z),
                                level.getBlockState(worldPosition.offset(x, y, z)),
                                level.getBlockState(worldPosition.offset(x, y, z)), 3);
                    }
                }
            }
        }
        return change;
    }

    private boolean handleRecharge(boolean change) {
        // Initialize regeneration rate based on modifier
        if (regeneration < 0) {
            regeneration = 600; // base ticks per vis point
            if (nodeModifier != null) {
                switch (nodeModifier) {
                    case BRIGHT -> regeneration = 400;
                    case PALE -> regeneration = 900;
                    case FADING -> regeneration = 0;
                }
            }
            if (nodeLock == 1) regeneration *= 2;
            if (nodeLock == 2) regeneration *= 20;
        }

        // Catch up for time offline
        if (catchUp) {
            catchUp = false;
            long ct = System.currentTimeMillis();
            int inc = regeneration * 75;
            int amt = inc > 0 ? (int) ((ct - lastActive) / (long) inc) : 0;
            for (int a = 0; a < Math.min(amt, aspectsBase.size()); a++) {
                List<Aspect> lowAspects = new ArrayList<>();
                for (Aspect aspect : aspects.getAspects()) {
                    if (aspects.getAmount(aspect) < aspectsBase.getAmount(aspect)) {
                        lowAspects.add(aspect);
                    }
                }
                if (!lowAspects.isEmpty()) {
                    aspects.add(lowAspects.get(level.random.nextInt(lowAspects.size())), 1);
                    change = true;
                }
            }
        }

        // Periodic vis regeneration
        if (regeneration > 0 && wait == 0 && count % regeneration == 0) {
            lastActive = System.currentTimeMillis();
            List<Aspect> lowAspects = new ArrayList<>();
            for (Aspect aspect : aspects.getAspects()) {
                if (aspects.getAmount(aspect) < aspectsBase.getAmount(aspect)) {
                    lowAspects.add(aspect);
                }
            }
            if (!lowAspects.isEmpty()) {
                aspects.add(lowAspects.get(level.random.nextInt(lowAspects.size())), 1);
                change = true;
            }
        }

        // Aspect decay over time
        if (count % 1200 == 0) {
            for (Aspect aspect : java.util.Arrays.asList(aspects.getAspects())) {
                if (aspects.getAmount(aspect) <= 0) {
                    setNodeVisBase(aspect, (short) (getNodeVisBase(aspect) - 1));
                    if (level.random.nextInt(20) == 0 || getNodeVisBase(aspect) <= 0) {
                        aspects.remove(aspect);
                        if (level.random.nextInt(5) == 0) {
                            if (nodeModifier == NodeModifier.BRIGHT) {
                                setNodeModifier(null);
                            } else if (nodeModifier == null) {
                                setNodeModifier(NodeModifier.PALE);
                            }
                            if (nodeModifier == NodeModifier.PALE && level.random.nextInt(5) == 0) {
                                setNodeModifier(NodeModifier.FADING);
                            }
                        }
                        nodeChange();
                        break;
                    }
                    nodeChange();
                }
            }
            if (aspects.size() <= 0) {
                removeNode();
            }
        }

        if (wait > 0) wait--;

        return change;
    }

    private boolean handleTaintNode(boolean change) {
        if (nodeType == NodeType.TAINTED && count % 50 == 0) {
            // Spread taint to nearby blocks and raise flux
            int x = worldPosition.getX() + level.random.nextInt(8) - level.random.nextInt(8);
            int z = worldPosition.getZ() + level.random.nextInt(8) - level.random.nextInt(8);
            BlockPos spot = new BlockPos(x, worldPosition.getY() + level.random.nextInt(5) - level.random.nextInt(5), z);
            if (level.getBlockState(spot).isAir() && level.getBlockState(spot.below()).isSolid()) {
                thaumcraft.common.blocks.world.taint.TaintHelper.spreadFibres(level, spot, true);
            }
            // Add corruption to the aura
            if (!level.isClientSide && count % 100 == 0) {
                thaumcraft.api.aura.AuraHelper.polluteAura(level, worldPosition, 0.25f, false);
                change = true;
            }
        } else if (nodeType != NodeType.PURE && nodeType != NodeType.TAINTED && count % 100 == 0) {
            // Tainted aura nearby can convert this node
            if (thaumcraft.api.aura.AuraHelper.getFluxSaturation(level, worldPosition) > 0.75f
                    && level.random.nextInt(400) == 0) {
                setNodeType(NodeType.TAINTED);
                change = true;
            }
        }
        return change;
    }

    private boolean handlePureNode(boolean change) {
        if (nodeType == NodeType.PURE && count % 50 == 0) {
            // Cleanse nearby taint blocks and reduce flux
            int x = worldPosition.getX() + level.random.nextInt(8) - level.random.nextInt(8);
            int z = worldPosition.getZ() + level.random.nextInt(8) - level.random.nextInt(8);
            int y = worldPosition.getY() + level.random.nextInt(5) - level.random.nextInt(5);
            BlockPos spot = new BlockPos(x, y, z);
            BlockState bs = level.getBlockState(spot);
            if (isTaintBlock(bs.getBlock()) && level.random.nextBoolean()) {
                level.setBlockAndUpdate(spot, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
                change = true;
            }
            // Remove flux from the local aura
            if (!level.isClientSide && count % 100 == 0) {
                float flux = thaumcraft.api.aura.AuraHelper.getFlux(level, worldPosition);
                if (flux > 0) {
                    thaumcraft.api.aura.AuraHelper.drainFlux(level, worldPosition, 0.25f, false);
                    change = true;
                }
            }
        }
        return change;
    }

    private static boolean isTaintBlock(net.minecraft.world.level.block.Block block) {
        return block == thaumcraft.init.ModBlocks.TAINT_FIBRE.get()
                || block == thaumcraft.init.ModBlocks.TAINT_SOIL.get()
                || block == thaumcraft.init.ModBlocks.TAINT_ROCK.get()
                || block == thaumcraft.init.ModBlocks.TAINT_CRUST.get()
                || block == thaumcraft.init.ModBlocks.TAINT_LOG.get()
                || block == thaumcraft.init.ModBlocks.TAINT_FEATURE.get();
    }

    private boolean handleDarkNode(boolean change) {
        if (nodeType == NodeType.DARK && count % 50 == 0) {
            // Spread Eerie biome
            int x = worldPosition.getX() + level.random.nextInt(12) - level.random.nextInt(12);
            int z = worldPosition.getZ() + level.random.nextInt(12) - level.random.nextInt(12);
            // TODO: Implement biome setting

            // TC4 handleDarkNode: spawn Giant Brainy Zombies near players
            // (Config.hardNode defaults to true in TC4, so this is standard behaviour)
            if (level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, 24.0, false) != null) {
                AABB box = new AABB(worldPosition).inflate(10.0, 6.0, 10.0);
                if (level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityGiantBrainyZombie.class,
                        box).size() <= 3) {
                    double dx = worldPosition.getX() + 0.5
                            + (level.random.nextDouble() - level.random.nextDouble()) * 5.0;
                    double dy = worldPosition.getY() + level.random.nextInt(3) - 1;
                    double dz = worldPosition.getZ() + 0.5
                            + (level.random.nextDouble() - level.random.nextDouble()) * 5.0;

                    thaumcraft.common.entities.monster.EntityGiantBrainyZombie zombie =
                            new thaumcraft.common.entities.monster.EntityGiantBrainyZombie(
                                    ModEntities.GIANT_BRAINY_ZOMBIE.get(), level);
                    zombie.moveTo(dx, dy, dz, level.random.nextFloat() * 360.0f, 0.0f);
                    if (zombie.checkSpawnRules(level, net.minecraft.world.entity.MobSpawnType.CONVERSION)) {
                        level.addFreshEntity(zombie);
                        // TC4 playAuxSFX 2004: mob portal poof particles
                        level.levelEvent(2004, BlockPos.containing(dx, dy + 1, dz), 0);
                    }
                }
            }
        }
        return change;
    }

    private boolean handleNodeStability(boolean change) {
        if (count % 100 == 0) {
            // Unstable nodes release aspect orbs
            if (nodeType == NodeType.UNSTABLE && level.random.nextBoolean()) {
                if (nodeLock == 0) {
                    Aspect aspect = takeRandomPrimalFromSource();
                    if (aspect != null) {
                        if (!level.isClientSide) {
                            thaumcraft.common.entities.EntityAspectOrb orb =
                                    new thaumcraft.common.entities.EntityAspectOrb(level,
                                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                                            aspect, 1);
                            level.addFreshEntity(orb);
                        }
                        change = true;
                    }
                } else if (level.random.nextInt(10000 / Math.max(1, nodeLock)) == 42) {
                    setNodeType(NodeType.NORMAL);
                    change = true;
                }
            }

            // Fading nodes can recover
            if (nodeModifier == NodeModifier.FADING && nodeLock > 0 && level.random.nextInt(12500 / Math.max(1, nodeLock)) == 69) {
                setNodeModifier(NodeModifier.PALE);
                change = true;
            }
        }
        return change;
    }

    private boolean handleHungryNodeSecond(boolean change) {
        if (level == null) return change;
        if (nodeType == NodeType.HUNGRY && count % 50 == 0) {
            int tx = worldPosition.getX() + level.random.nextInt(16) - level.random.nextInt(16);
            int ty = worldPosition.getY() + level.random.nextInt(16) - level.random.nextInt(16);
            int tz = worldPosition.getZ() + level.random.nextInt(16) - level.random.nextInt(16);
            int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, tx, tz);
            if (ty > topY) {
                ty = topY;
            }

            // Ray trace from the node to the random point to find the first block in line of sight
            net.minecraft.world.phys.Vec3 v1 = new net.minecraft.world.phys.Vec3(
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            net.minecraft.world.phys.Vec3 v2 = new net.minecraft.world.phys.Vec3(tx + 0.5, ty + 0.5, tz + 0.5);
            net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                    v1, v2, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty());
            net.minecraft.world.phys.HitResult hit = level.clip(ctx);
            if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult bhr = (net.minecraft.world.phys.BlockHitResult) hit;
                BlockPos bp = bhr.getBlockPos();
                if (worldPosition.distSqr(bp) < 256.0) {
                    BlockState bs = level.getBlockState(bp);
                    if (!bs.isAir()) {
                        float h = bs.getDestroySpeed(level, bp);
                        if (h >= 0.0f && h < 5.0f) {
                            // Eat the block (drop resources)
                            level.destroyBlock(bp, true);
                            change = true;
                        }
                    }
                }
            }
        }
        return change;
    }

    // ============================================================
    // Energized Node Mechanics (TC4)
    // ============================================================

    /**
     * Handle energized node logic.
     * When energized, the node produces cVis = floor(sqrt(Vis)) per tick per primal aspect.
     * Complex aspects are decomposed to primal before calculation.
     *
     * @param change whether something changed
     * @return true if something changed
     */
    private boolean handleEnergizedNode(boolean change) {
        if (!energized) return change;

        // Recalculate cVis from aspectsBase (which contains the raw Vis values)
        // Complex aspects are decomposed to primal first
        recalculateCVis();

        // Produce cVis every tick (20 ticks = 1 second)
        // The cVis is available for wand charging via Arcane Workbench + Vis Charge Relay
        // This doesn't add to aspects - cVis is a separate "stream" that can be tapped
        // by the workbench/relay system

        lastCVisProduction = System.currentTimeMillis();
        change = true; // Mark for sync so clients can see energized state

        return change;
    }

    /**
     * Recalculate cVis values from aspectsBase.
     * TC4 Formula: cVis = floor(sqrt(Vis)) for each primal aspect.
     * Complex aspects are decomposed to their primal components first.
     */
    private void recalculateCVis() {
        // First, create a decomposed copy of aspectsBase (complex -> primal)
        AspectList decomposed = decomposeToPrimal(aspectsBase);

        // Calculate cVis for each primal aspect: floor(sqrt(Vis))
        List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < primals.size() && i < 6; i++) {
            Aspect aspect = primals.get(i);
            int vis = decomposed.getAmount(aspect);
            // TC4 formula: cVis = floor(sqrt(Vis))
            cachedCVis[i] = (int) Math.floor(Math.sqrt(vis));
        }
    }

    /**
     * Decompose an AspectList to only primal aspects.
     * Complex aspects are broken down into their primal components recursively.
     *
     * @param source the source AspectList
     * @return new AspectList containing only primal aspects
     */
    private AspectList decomposeToPrimal(AspectList source) {
        AspectList result = new AspectList();

        for (Aspect aspect : source.getAspects()) {
            int amount = source.getAmount(aspect);
            if (amount <= 0) continue;

            if (aspect.isPrimal()) {
                // Already primal, add directly
                result.add(aspect, amount);
            } else {
                // Complex aspect - decompose to primal components
                Aspect[] components = aspect.getComponents();
                if (components != null && components.length == 2) {
                    // Each component gets the full amount (TC4 behavior)
                    // The complex aspect is essentially "unpacked" into its two components
                    decomposeAspectRecursive(result, components[0], amount);
                    decomposeAspectRecursive(result, components[1], amount);
                }
            }
        }

        return result;
    }

    /**
     * Recursively decompose an aspect to primal components.
     */
    private void decomposeAspectRecursive(AspectList target, Aspect aspect, int amount) {
        if (aspect.isPrimal()) {
            target.add(aspect, amount);
        } else {
            Aspect[] components = aspect.getComponents();
            if (components != null && components.length == 2) {
                decomposeAspectRecursive(target, components[0], amount);
                decomposeAspectRecursive(target, components[1], amount);
            }
        }
    }

    /**
     * Get the cVis output for a specific primal aspect.
     * This is what the Vis Charge Relay reads when charging a wand.
     *
     * @param aspect the primal aspect
     * @return cVis per tick (floor(sqrt(Vis)))
     */
    public int getCVis(Aspect aspect) {
        if (!energized) return 0;
        if (!aspect.isPrimal()) return 0;

        // Find index in primal array
        List<Aspect> primals = Aspect.getPrimalAspects();
        for (int i = 0; i < primals.size() && i < 6; i++) {
            if (primals.get(i) == aspect) {
                return cachedCVis[i];
            }
        }
        return 0;
    }

    /**
     * Get total cVis output across all primal aspects.
     */
    public int getTotalCVis() {
        if (!energized) return 0;
        int total = 0;
        for (int cvis : cachedCVis) {
            total += cvis;
        }
        return total;
    }

    /**
     * Check if this node is energized.
     */
    public boolean isEnergized() {
        return energized;
    }

    /**
     * Set the energized state.
     * In TC4, nodes become energized by:
     * 1. Placing a Vis Charge Relay on top of an Arcane Workbench adjacent to the node
     * 2. Using a Node Transducer
     * 3. Certain node modifiers
     */
    public void setEnergized(boolean energized) {
        boolean wasEnergized = this.energized;
        this.energized = energized;

        if (energized && !wasEnergized) {
            // Just became energized - recalculate cVis
            recalculateCVis();
            nodeChange();
        } else if (!energized && wasEnergized) {
            // No longer energized
            nodeChange();
        }
    }

    /**
     * Try to consume cVis for wand charging.
     * Called by Arcane Workbench + Vis Charge Relay system.
     *
     * @param aspect the aspect to consume
     * @param amount the amount of cVis to consume (typically 1 per tick)
     * @return amount actually consumed
     */
    public int consumeCVis(Aspect aspect, int amount) {
        if (!energized) return 0;
        if (!aspect.isPrimal()) return 0;

        int available = getCVis(aspect);
        int consumed = Math.min(available, amount);

        // cVis is a "stream" - it doesn't reduce the node's Vis storage
        // The node continuously produces cVis from its base Vis
        // However, if we want to simulate the node being drained over time,
        // we could reduce aspectsBase slightly

        return consumed;
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    public void nodeChange() {
        regeneration = -1;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void removeNode() {
        setRemoved();
        if (level != null) {
            if (level.getBlockState(worldPosition).getBlock() == thaumcraft.init.ModBlocks.NODE.get()) {
                level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition.below()).getBlock().defaultBlockState());
            }
        }
    }

    public Aspect takeRandomPrimalFromSource() {
        List<Aspect> primals = Aspect.getPrimalAspects();
        if (primals.isEmpty()) return null;
        Aspect asp = primals.get(level.random.nextInt(primals.size()));
        return (asp != null && aspects.reduce(asp, 1)) ? asp : null;
    }

    public Aspect chooseRandomFilteredFromSource(AspectList filter, boolean preserve) {
        int min = preserve ? 1 : 0;
        List<Aspect> validaspects = new ArrayList<>();

        for (Aspect prim : aspects.getAspects()) {
            if (filter.getAmount(prim) > 0 && aspects.getAmount(prim) > min) {
                validaspects.add(prim);
            }
        }

        if (validaspects.isEmpty()) return null;

        Aspect asp = validaspects.get(level.random.nextInt(validaspects.size()));
        if (asp != null && aspects.getAmount(asp) > min) {
            return asp;
        }
        return null;
    }

    public void checkLock() {
        if ((count <= 1 || count % 50 == 0) && worldPosition.getY() > 0) {
            byte oldLock = nodeLock;
            nodeLock = 0;

            // Stabilized by a charged stabilizer below -> node can be locked (prevents decay,
            // doubles/halves regeneration). TC4: lock 1 = powered stabilizer, lock 2 = advanced.
            if (level != null) {
                BlockEntity below = level.getBlockEntity(worldPosition.below());
                if (below instanceof thaumcraft.common.tiles.devices.TileStabilizer stabilizer) {
                    if (stabilizer.getEnergy() > 0) {
                        // Advanced stabilizer (higher capacity) gives stronger lock
                        nodeLock = stabilizer.getCapacity() > 10 ? (byte) 2 : 1;
                    }
                }
            }

            if (oldLock != nodeLock) {
                regeneration = -1;
            }
        }
    }

    public byte getLock() {
        return nodeLock;
    }

    // ============================================================
    // NBT Serialization
    // ============================================================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("lastActive", lastActive);

        ListTag tlist = new ListTag();
        tag.put("AspectsBase", tlist);

        for (Aspect aspect : aspectsBase.getAspects()) {
            if (aspect != null) {
                CompoundTag f = new CompoundTag();
                f.putString("key", aspect.getTag());
                f.putInt("amount", aspectsBase.getAmount(aspect));
                tlist.add(f);
            }
        }

        // Save custom NBT
        if (id == null) generateId();
        tag.putString("nodeId", id);
        tag.putByte("type", (byte) nodeType.ordinal());
        tag.putByte("modifier", nodeModifier == null ? -1 : (byte) nodeModifier.ordinal());
        tag.putBoolean("energized", energized);

        aspects.writeToNBT(tag);

        if (drainEntity instanceof Player player) {
            tag.putString("drainer", player.getUUID().toString());
        }
        tag.putInt("draincolor", drainColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastActive = tag.getLong("lastActive");

        AspectList al = new AspectList();
        ListTag tlist = tag.getList("AspectsBase", Tag.TAG_COMPOUND);
        for (int j = 0; j < tlist.size(); j++) {
            CompoundTag rs = tlist.getCompound(j);
            if (rs.contains("key")) {
                Aspect aspect = Aspect.getAspect(rs.getString("key"));
                if (aspect != null) {
                    al.add(aspect, rs.getInt("amount"));
                }
            }
        }

        short oldBase = tag.getShort("nodeVisBase");
        aspectsBase.aspects.clear();
        if (oldBase > 0 && al.size() == 0) {
            for (Aspect a : aspects.getAspects()) {
                aspectsBase.merge(a, oldBase);
            }
        } else {
            aspectsBase.aspects.clear();
            aspectsBase.add(al.copy());
        }

        int regen = 600;
        if (nodeModifier != null) {
            switch (nodeModifier) {
                case BRIGHT -> regen = 400;
                case PALE -> regen = 900;
                case FADING -> regen = 0;
            }
        }

        long ct = System.currentTimeMillis();
        int inc = regen * 75;
        if (regen > 0 && lastActive > 0L && ct > lastActive + inc) {
            catchUp = true;
        }

        // Load custom NBT
        id = tag.getString("nodeId");
        if (level != null && locations != null) {
            List<Integer> t = new ArrayList<>();
            int dimId = level.dimension().location().toString().hashCode();
            t.add(dimId);
            t.add(worldPosition.getX());
            t.add(worldPosition.getY());
            t.add(worldPosition.getZ());
            locations.put(id, t);
        }

        setNodeType(NodeType.values()[tag.getByte("type")]);
        byte mod = tag.getByte("modifier");
        if (mod >= 0) {
            setNodeModifier(NodeModifier.values()[mod]);
        }

        // Load energized state
        energized = tag.getBoolean("energized");

        aspects.readFromNBT(tag);

        String de = tag.getString("drainer");
        if (de != null && !de.isEmpty() && level != null) {
            // TODO: Find player by UUID
        }

        drainColor = tag.getInt("draincolor");

        // Recalculate cVis if energized
        if (energized) {
            recalculateCVis();
        }
    }

    // ============================================================
    // Wand Interaction (for draining vis)
    // ============================================================

    public void onUsingWandTick(ItemStack wandStack, Player player, int count) {
        if (level == null || level.isClientSide) return;

        // Check if player is still looking at node
        // TODO: Implement ray trace check

        if (count % 5 == 0) {
            int tap = 1;
            // TC4: node-tapper research increases the amount drained per tick
            if (thaumcraft.api.capabilities.ThaumcraftCapabilities.isResearchComplete(player, "NODETAPPER1")) {
                tap++;
            }
            if (thaumcraft.api.capabilities.ThaumcraftCapabilities.isResearchComplete(player, "NODETAPPER2")) {
                tap++;
            }

            // TC4: preserve mode (leave 1 vis in the node) unless sneaking; needs
            // the NODEPRESERVE research and a non-starter wand (not wood + iron)
            boolean preserve = !player.isShiftKeyDown()
                    && thaumcraft.api.capabilities.ThaumcraftCapabilities.isResearchComplete(player, "NODEPRESERVE");
            if (preserve && wandStack.getItem() instanceof thaumcraft.common.items.wands.WandItem) {
                thaumcraft.api.wands.WandRod rod = thaumcraft.common.items.wands.WandItem.getRod(wandStack);
                thaumcraft.api.wands.WandCap cap = thaumcraft.common.items.wands.WandItem.getCap(wandStack);
                preserve = !(rod != null && "wood".equals(rod.getTag()))
                        && !(cap != null && "iron".equals(cap.getTag()));
            }

            boolean success = false;
            Aspect aspect = chooseRandomFilteredFromSource(getWandAspectsWithRoom(wandStack), preserve);
            if (aspect != null) {
                int amt = aspects.getAmount(aspect);
                if (tap > amt) tap = amt;
                if (preserve && tap == amt) tap--;

                if (tap > 0) {
                    int rem = addVisToWand(wandStack, aspect, tap);
                    if (rem < tap) {
                        drainColor = aspect.getColor();
                        takeFromContainer(aspect, tap - rem);
                        success = true;
                    }
                }
            }

            if (success) {
                drainEntity = player;
                targetColor = new java.awt.Color(drainColor);
            } else {
                drainEntity = null;
            }

            if (success) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        // Client-side color interpolation
        if (level.isClientSide) {
            int r = targetColor.getRed();
            int g = targetColor.getGreen();
            int b = targetColor.getBlue();
            int r2 = color.getRed() * 4;
            int g2 = color.getGreen() * 4;
            int b2 = color.getBlue() * 4;
            color = new java.awt.Color((r + r2) / 5, (g + g2) / 5, (b + b2) / 5);
        }
    }

    public void onWandStoppedUsing(ItemStack wandStack, Level world, Player player, int count) {
        drainEntity = null;
        drainCollision = null;
    }

    /**
     * TC4 TileNode: the aspects the given wand still has room for (used to pick
     * which node aspect to drain into the wand).
     */
    private AspectList getWandAspectsWithRoom(ItemStack wandStack) {
        AspectList out = new AspectList();
        if (wandStack == null || !(wandStack.getItem() instanceof thaumcraft.common.items.wands.WandItem wand)) {
            return out;
        }
        float max = wand.getMaxVis(wandStack);
        if (max <= 0.0f) {
            return out;
        }
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (wand.getVis(wandStack, aspect) < max) {
                out.add(aspect, 1);
            }
        }
        return out;
    }

    /**
     * TC4 TileNode.addVis: store up to {@code amount} vis of the aspect on the
     * wand and return the amount that did not fit.
     */
    private int addVisToWand(ItemStack wandStack, Aspect aspect, int amount) {
        if (wandStack == null || aspect == null || amount <= 0
                || !(wandStack.getItem() instanceof thaumcraft.common.items.wands.WandItem wand)) {
            return amount;
        }
        float max = wand.getMaxVis(wandStack);
        float have = wand.getVis(wandStack, aspect);
        int stored = (int) Math.min(amount, Math.max(0.0f, max - have));
        if (stored > 0) {
            wand.storeVis(wandStack, aspect, have + stored);
        }
        return amount - stored;
    }

    // Client event for particle effects
    public boolean receiveClientEvent(int id, int data) {
        if (id == 1) {
            // Node zap effect
            // TODO: Implement FX
            return true;
        }
        return super.triggerEvent(id, data);
    }

    // ==================== IWandable Implementation ====================

    /**
     * Called when player right-clicks this node with a wand.
     * If the wand has an empty warded jar equipped as focus or in off-hand,
     * captures the node into the jar.
     */
    @Override
    public int onWandRightClick(Level world, ItemStack wandStack, Player player, int x, int y, int z, int side, int md) {
        // Check if player is holding an empty warded jar in off-hand
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemJarNode jarItem) {
            // Check if jar is empty (no node data)
            if (!offhand.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) || !NBTHelper.getTag(offhand).contains("nodetype")) {
                return captureNodeIntoJar(world, player, offhand);
            }
        }

        // Check if wand has empty jar as focus
        // (This would require focus system integration - simplified for now)

        return -1; // Not handled, let normal wand draining happen
    }

    /**
     * Capture this node into an empty warded jar.
     * Returns 0 if successful, -1 if failed.
     */
    private int captureNodeIntoJar(Level world, Player player, ItemStack jarStack) {
        if (world.isClientSide) return 0;

        // Create ItemJarNode with this node's data
        ItemJarNode jarItem = (ItemJarNode) jarStack.getItem();
        jarItem.setAspects(jarStack, this.getAspects());
        jarItem.setNodeAttributes(jarStack, this.getNodeType(), this.getNodeModifier(), this.getId());

        // Reduce node size by 10% when jarred (TC4 mechanic)
        for (Aspect aspect : this.getAspectsBase().getAspects()) {
            int baseAmount = this.getNodeVisBase(aspect);
            int newBase = (int) (baseAmount * 0.9);
            this.setNodeVisBase(aspect, (short) newBase);
        }

        // Remove the node block
        world.setBlockAndUpdate(this.worldPosition, world.getBlockState(this.worldPosition.below()).getBlock().defaultBlockState());
        this.setRemoved();

        // Play sound
        world.playSound(null, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        // Consume the empty jar (replace with filled jar)
        if (!player.getAbilities().instabuild) {
            jarStack.shrink(1);
            if (jarStack.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.JAR_NODE.get()));
            } else {
                // Add filled jar to inventory
                player.getInventory().add(new ItemStack(ModItems.JAR_NODE.get()));
            }
        }

        return 0;
    }

    @Override
    public ItemStack onWandRightClick(Level world, ItemStack wandStack, Player player) {
        return null;
    }

    // ==================== Static Tick Methods ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileNode node) {
        if (node != null) {
            node.tick();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNode node) {
        // Client-side tick logic (particle effects handled in tick())
    }
}
