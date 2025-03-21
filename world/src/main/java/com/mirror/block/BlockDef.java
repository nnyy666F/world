// BlockDef.java
package com.mirror.block;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

public class BlockDef implements Serializable {
    private final String displayName;
    private final BlockType type;
    private final List<ParamType> params;
    private final int substacks;
    private static final long serialVersionUID = 1L;
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    public BlockDef(String displayName, BlockType type,
                    List<ParamType> params, int substacks) {
        this.displayName = displayName;
        this.type = type;
        this.params = params;
        this.substacks = substacks;
    }
    public String getDisplayName() { return displayName; }
    public BlockType getType() { return type; }
    public List<ParamType> getParams() { return params; }
    public int getSubstacks() { return substacks; }
}