package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Position and face for a seal.
 */
public class SealPos {
    public BlockPos pos;
    public Direction face;

    public SealPos() {
        this.pos = BlockPos.ZERO;
        this.face = Direction.DOWN;
    }

    public SealPos(BlockPos pos, Direction face) {
        this.pos = pos;
        this.face = face;
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putLong("pos", pos.asLong());
        nbt.putByte("face", (byte) face.ordinal());
    }

    public static SealPos readFromNBT(CompoundTag nbt) {
        SealPos sp = new SealPos();
        sp.pos = BlockPos.of(nbt.getLong("pos"));
        sp.face = Direction.values()[nbt.getByte("face")];
        return sp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SealPos sealPos = (SealPos) o;
        return pos.equals(sealPos.pos) && face == sealPos.face;
    }

    @Override
    public int hashCode() {
        return pos.hashCode() * 31 + face.hashCode();
    }

    @Override
    public String toString() {
        return pos.toString() + ":" + face.toString();
    }
}
