package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Arcane Ear tile entity - detects note block sounds and emits redstone.
 * Can be configured to respond to specific notes and instruments.
 * Has pulse and toggle variants.
 */
public class TileArcaneEar extends TileThaumcraft {

    // Note block event tracking (dimension -> list of [x, y, z, instrument, note])
    public static WeakHashMap<Level, ArrayList<int[]>> noteBlockEvents = new WeakHashMap<>();

    public static final int MAX_NOTE = 24;
    public static final int MAX_RANGE_SQ = 4096; // 64 blocks

    public byte note = 0;
    public byte instrument = 0;
    public int redstoneSignal = 0;
    
    // For toggle variant
    private boolean isToggle = false;
    private boolean toggleState = false;

    public TileArcaneEar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileArcaneEar(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ARCANE_EAR.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Note", note);
        tag.putByte("Instrument", instrument);
        tag.putBoolean("Toggle", isToggle);
        tag.putBoolean("ToggleState", toggleState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        note = tag.getByte("Note");
        instrument = tag.getByte("Instrument");
        if (note < 0) note = 0;
        if (note > MAX_NOTE) note = MAX_NOTE;
        isToggle = tag.getBoolean("Toggle");
        toggleState = tag.getBoolean("ToggleState");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileArcaneEar tile) {
        // Decrease redstone signal (pulse mode)
        if (tile.redstoneSignal > 0) {
            tile.redstoneSignal--;
            if (tile.redstoneSignal == 0) {
                tile.updateRedstone(state, false);
            }
        }

        // Check for matching note block events
        ArrayList<int[]> events = noteBlockEvents.get(level);
        if (events != null && !events.isEmpty()) {
            for (int[] data : events) {
                // data = [x, y, z, instrument, note]
                if (data[3] == tile.instrument && data[4] == tile.note) {
                    double distSq = pos.distSqr(new BlockPos(data[0], data[1], data[2]));
                    if (distSq <= MAX_RANGE_SQ) {
                        tile.onNoteDetected(state);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Called when a matching note is detected.
     */
    private void onNoteDetected(BlockState state) {
        if (level == null) return;

        // Play response sound
        triggerNote(true);

        if (isToggle) {
            // Toggle mode - flip state
            toggleState = !toggleState;
            updateRedstone(state, toggleState);
            setChanged();
        } else {
            // Pulse mode - emit signal for 10 ticks
            redstoneSignal = 10;
            updateRedstone(state, true);
        }
    }

    /**
     * Update redstone output state.
     */
    private void updateRedstone(BlockState state, boolean powered) {
        if (level == null) return;

        Direction facing = getFacing().getOpposite();
        
        // Update block state if it has ENABLED property
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            boolean currentEnabled = state.getValue(BlockStateProperties.ENABLED);
            if (currentEnabled != powered) {
                level.setBlock(worldPosition, state.setValue(BlockStateProperties.ENABLED, powered), 3);
            }
        }

        // Notify neighbors
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighborsAt(worldPosition.relative(facing), state.getBlock());
    }

    // ==================== Configuration ====================

    /**
     * Cycle to the next note.
     */
    public void changePitch() {
        note = (byte) ((note + 1) % (MAX_NOTE + 1));
        setChanged();
    }

    /**
     * Update the instrument based on the block behind the ear.
     */
    public void updateInstrument() {
        if (level == null) return;

        try {
            Direction facing = getFacing().getOpposite();
            BlockPos behindPos = worldPosition.relative(facing);
            BlockState behindState = level.getBlockState(behindPos);
            
            // Determine instrument from block behind
            instrument = getInstrumentFromBlock(behindState);
            setChanged();
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Map block to note block instrument ID.
     */
    private byte getInstrumentFromBlock(BlockState state) {
        // In 1.20.1, we use NoteBlockInstrument
        NoteBlockInstrument noteInstrument = state.instrument();
        
        // Map to legacy instrument IDs for compatibility
        return switch (noteInstrument) {
            case HARP -> 0;           // Default (air, most blocks)
            case BASEDRUM -> 1;       // Stone
            case SNARE -> 2;          // Sand
            case HAT -> 3;            // Glass
            case BASS -> 4;           // Wood
            case FLUTE -> 5;          // Clay
            case BELL -> 6;           // Gold
            case GUITAR -> 7;         // Wool
            case CHIME -> 8;          // Packed Ice
            case XYLOPHONE -> 9;      // Bone Block
            case IRON_XYLOPHONE -> 10;
            case COW_BELL -> 11;
            case DIDGERIDOO -> 12;
            case BIT -> 13;
            case BANJO -> 14;
            case PLING -> 15;
            default -> 0;
        };
    }

    /**
     * Play the note sound as feedback.
     */
    public void triggerNote(boolean playSound) {
        if (level == null || !playSound) return;

        Direction facing = getFacing().getOpposite();
        BlockPos behindPos = worldPosition.relative(facing);
        BlockState behindState = level.getBlockState(behindPos);
        
        // Get instrument for sound
        byte soundInstrument = getInstrumentFromBlock(behindState);
        
        // Send block event to play sound
        level.blockEvent(worldPosition, getBlockState().getBlock(), soundInstrument, note);
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    // ==================== Static Event Handling ====================

    /**
     * Called when a note block plays. Register the event for arcane ears to detect.
     */
    public static void registerNoteBlockEvent(Level level, BlockPos pos, int instrument, int note) {
        ArrayList<int[]> events = noteBlockEvents.computeIfAbsent(level, k -> new ArrayList<>());
        events.add(new int[] { pos.getX(), pos.getY(), pos.getZ(), instrument, note });
    }

    /**
     * Clear note block events at end of tick.
     */
    public static void clearNoteBlockEvents(Level level) {
        ArrayList<int[]> events = noteBlockEvents.get(level);
        if (events != null) {
            events.clear();
        }
    }

    // ==================== Getters ====================

    public byte getNote() {
        return note;
    }

    public byte getInstrument() {
        return instrument;
    }

    public boolean isToggleMode() {
        return isToggle;
    }

    public void setToggleMode(boolean toggle) {
        this.isToggle = toggle;
        setChanged();
    }

    public boolean getToggleState() {
        return toggleState;
    }

    /**
     * Get redstone power output.
     */
    public int getRedstoneOutput() {
        if (isToggle) {
            return toggleState ? 15 : 0;
        }
        return redstoneSignal > 0 ? 15 : 0;
    }
}




