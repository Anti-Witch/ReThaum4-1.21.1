package thaumcraft.api.nodes;

/**
 * NodeModifier - Modifiers that can be applied to aura nodes.
 * These affect regeneration rate and capacity.
 *
 * BRIGHT  - 50% faster regeneration, 20% more capacity
 * PALE    - 50% slower regeneration, 20% less capacity
 * FADING  - No regeneration, slowly loses capacity
 */
public enum NodeModifier {
    BRIGHT,
    PALE,
    FADING
}