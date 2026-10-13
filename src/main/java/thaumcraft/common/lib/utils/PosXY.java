package thaumcraft.common.lib.utils;

import java.util.Objects;

/**
 * Simple 2D integer position class used primarily for chunk coordinate lookups.
 * This is more efficient than using ChunkPos for map keys.
 */
public class PosXY implements Comparable<PosXY> {

    public int x;
    public int y;

    public PosXY() {
    }

    public PosXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public PosXY(PosXY other) {
        this.x = other.x;
        this.y = other.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PosXY other)) return false;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public int compareTo(PosXY other) {
        if (y != other.y) {
            return y - other.y;
        }
        return x - other.x;
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public float getDistanceSquared(int x, int y) {
        float dx = this.x - x;
        float dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public float getDistanceSquaredTo(PosXY other) {
        return getDistanceSquared(other.x, other.y);
    }

    @Override
    public String toString() {
        return "PosXY{x=" + x + ", y=" + y + '}';
    }
}




