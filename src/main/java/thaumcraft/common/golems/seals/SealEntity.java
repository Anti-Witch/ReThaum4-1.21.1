package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;

/**
 * SealEntity - Represents a placed seal in the world.
 * Contains the seal type, position, configuration, and state.
 * 
 * Ported from 1.12.2. Key changes:
 * - NBTTagCompound -> CompoundTag
 * - EnumFacing -> Direction
 * - world.provider.getDimension() -> level.dimension()
 * - Network packet sync is stubbed (TODO: implement when network is ready)
 */
public class SealEntity implements ISealEntity {
    
    SealPos sealPos;
    ISeal seal;
    byte priority = 0;
    byte color = 0;
    boolean locked = false;
    boolean redstone = false;
    String owner = "";
    boolean stopped = false;
    private BlockPos area = new BlockPos(1, 1, 1);
    
    public SealEntity() {
    }
    
    public SealEntity(Level level, SealPos sealPos, ISeal seal) {
        this.sealPos = sealPos;
        this.seal = seal;
        
        // Initialize area based on seal type and face
        if (seal instanceof ISealConfigArea) {
            int x = (sealPos.face.getStepX() == 0) ? 3 : 1;
            int y = (sealPos.face.getStepY() == 0) ? 3 : 1;
            int z = (sealPos.face.getStepZ() == 0) ? 3 : 1;
            area = new BlockPos(x, y, z);
        }
    }
    
    @Override
    public void tickSealEntity(Level level) {
        if (seal == null) return;
        
        if (isStoppedByRedstone(level)) {
            if (!stopped) {
                // Suspend all tasks from this seal
                for (Task task : TaskHandler.getTasks(level.dimension()).values()) {
                    if (task.getSealPos() != null && task.getSealPos().equals(sealPos)) {
                        task.setSuspended(true);
                    }
                }
            }
            stopped = true;
            return;
        }
        
        stopped = false;
        seal.tickSeal(level, this);
    }
    
    @Override
    public boolean isStoppedByRedstone(Level level) {
        if (!isRedstoneSensitive()) return false;
        
        BlockPos pos = getSealPos().pos;
        Direction face = getSealPos().face;
        
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.relative(face));
    }
    
    @Override
    public ISeal getSeal() {
        return seal;
    }
    
    @Override
    public SealPos getSealPos() {
        return sealPos;
    }
    
    @Override
    public byte getPriority() {
        return priority;
    }
    
    @Override
    public void setPriority(byte priority) {
        this.priority = priority;
    }
    
    @Override
    public byte getColor() {
        return color;
    }
    
    @Override
    public void setColor(byte color) {
        this.color = color;
    }
    
    @Override
    public String getOwner() {
        return owner;
    }
    
    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    @Override
    public boolean isLocked() {
        return locked;
    }
    
    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    @Override
    public boolean isRedstoneSensitive() {
        return redstone;
    }
    
    @Override
    public void setRedstoneSensitive(boolean redstone) {
        this.redstone = redstone;
    }
    
    @Override
    public void readNBT(CompoundTag nbt) {
        BlockPos pos = BlockPos.of(nbt.getLong("pos"));
        Direction face = Direction.values()[nbt.getByte("face")];
        sealPos = new SealPos(pos, face);
        
        setPriority(nbt.getByte("priority"));
        setColor(nbt.getByte("color"));
        setLocked(nbt.getBoolean("locked"));
        setRedstoneSensitive(nbt.getBoolean("redstone"));
        setOwner(nbt.getString("owner"));
        
        // Reconstruct seal from type key
        try {
            ISeal template = SealHandler.getSeal(nbt.getString("type"));
            if (template != null) {
                seal = template.getClass().getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            // Failed to instantiate seal
        }
        
        if (seal != null) {
            seal.readCustomNBT(nbt);
            
            if (seal instanceof ISealConfigArea) {
                area = BlockPos.of(nbt.getLong("area"));
            }
            
            if (seal instanceof ISealConfigToggles toggleSeal) {
                for (ISealConfigToggles.SealToggle toggle : toggleSeal.getToggles()) {
                    if (nbt.contains(toggle.getKey())) {
                        toggle.setValue(nbt.getBoolean(toggle.getKey()));
                    }
                }
            }
        }
    }
    
    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        nbt.putLong("pos", sealPos.pos.asLong());
        nbt.putByte("face", (byte) sealPos.face.ordinal());
        nbt.putString("type", seal.getKey());
        nbt.putByte("priority", getPriority());
        nbt.putByte("color", getColor());
        nbt.putBoolean("locked", isLocked());
        nbt.putBoolean("redstone", isRedstoneSensitive());
        nbt.putString("owner", getOwner());
        
        if (seal != null) {
            seal.writeCustomNBT(nbt);
            
            if (seal instanceof ISealConfigArea) {
                nbt.putLong("area", area.asLong());
            }
            
            if (seal instanceof ISealConfigToggles toggleSeal) {
                for (ISealConfigToggles.SealToggle toggle : toggleSeal.getToggles()) {
                    nbt.putBoolean(toggle.getKey(), toggle.getValue());
                }
            }
        }
        
        return nbt;
    }
    
    @Override
    public void syncToClient(Level level) {
        if (!level.isClientSide) {
            PacketHandler.sendToDimension(new PacketSealToClient(this), level.dimension());
        }
    }
    
    @Override
    public BlockPos getArea() {
        return area;
    }
    
    @Override
    public void setArea(BlockPos area) {
        this.area = area;
    }
}




