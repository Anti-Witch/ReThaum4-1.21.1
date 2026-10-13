package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * INode - Interface for aura nodes.
 * Nodes are sources of vis that regenerate over time.
 */
public interface INode extends IAspectContainer {

    /**
     * Unique identifier for this node.
     * Normal nodes use dimension:x:y:z format.
     */
    String getId();

    /**
     * Current vis levels in the node.
     */
    AspectList getAspects();

    /**
     * Maximum vis capacity for each aspect (base values).
     */
    AspectList getAspectsBase();

    /**
     * Set current vis levels.
     */
    void setAspects(AspectList aspects);

    /**
     * Get the type of this node.
     */
    NodeType getNodeType();

    /**
     * Set the type of this node.
     */
    void setNodeType(NodeType nodeType);

    /**
     * Set the modifier affecting regeneration and capacity.
     */
    void setNodeModifier(NodeModifier nodeModifier);

    /**
     * Get the current modifier.
     */
    NodeModifier getNodeModifier();

    /**
     * Get maximum capacity for a specific aspect.
     */
    int getNodeVisBase(Aspect aspect);

    /**
     * Set maximum capacity for a specific aspect.
     */
    void setNodeVisBase(Aspect aspect, short nodeVisBase);
}