package thaumcraft.api.golems.parts;

import thaumcraft.api.golems.IGolemAPI;

/**
 * Generic function interface for golem parts.
 */
public interface IGenericFunction {
    void onUpdateTick(IGolemAPI golem);
}