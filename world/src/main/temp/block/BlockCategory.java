package com.mirror.block;

public class BlockCategory {
    public static String getColor(BlockType type) {
        return switch (type) {
            case COMMAND -> "#2196F3";
            case CONDITION -> "#FF9800";
            case LOOP -> "#4CAF50";
            case EVENT -> "#9C27B0";
            case FUNCTION -> "#009688";
        };
    }
}