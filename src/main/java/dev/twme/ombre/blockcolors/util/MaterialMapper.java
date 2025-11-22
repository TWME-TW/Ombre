package dev.twme.ombre.blockcolors.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.bukkit.Material;

import dev.twme.ombre.blockcolors.data.BlockColorData;

/**
 * Material Mapping Utility
 * Maps API texture_name to Minecraft Material
 */
public class MaterialMapper {
    private static Logger logger;

    public static void setLogger(Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * Intelligently map texture_name to Material
     * Strategy: Start from full name, progressively remove last "_state" until matching Material is found
     * 
     * @param textureName Texture name provided by API
     * @return Corresponding Material, or null if mapping fails
     */
    public static Material mapTextureName(String textureName) {
        if (textureName == null || textureName.isEmpty()) {
            return null;
        }
        
        String[] parts = textureName.split("_");
        
        // Try starting from full name
        for (int i = parts.length; i > 0; i--) {
            String materialName = String.join("_", 
                Arrays.copyOfRange(parts, 0, i)).toUpperCase();
            
            try {
                Material material = Material.valueOf(materialName);
                
                // Verify it's a block, not an item
                if (material.isBlock()) {
                    return material;
                }
            } catch (IllegalArgumentException ignored) {
                // Continue trying next combination
            }
        }
        
        return null;
    }

    /**
     * Batch map and log unmappable textures
     * 
     * @param blocks Collection of block color data
     * @param logUnmapped Whether to log unmappable textures
     * @return Mapping result (textureName -> Material)
     */
    public static Map<String, Material> mapAllTextures(
        Collection<BlockColorData> blocks,
        boolean logUnmapped
    ) {
        Map<String, Material> mapping = new HashMap<>();
        List<String> unmapped = new ArrayList<>();
        
        for (BlockColorData block : blocks) {
            Material material = mapTextureName(block.getTextureName());
            
            if (material != null) {
                mapping.put(block.getTextureName(), material);
                // Also update the material field in BlockColorData
                block.setMaterial(material);
            } else if (logUnmapped) {
                unmapped.add(block.getTextureName());
            }
        }
        
        if (logUnmapped && !unmapped.isEmpty() && logger != null) {
            logger.warning(
                "Unable to map " + unmapped.size() + " textures: " + 
                String.join(", ", unmapped)
            );
        }
        
        return mapping;
    }

    /**
     * Verify if Material is valid and is a block
     * 
     * @param material Material to verify
     * @return Whether it's valid
     */
    public static boolean isValidBlockMaterial(Material material) {
        return material != null && material.isBlock();
    }

    /**
     * Get mapping statistics
     * 
     * @param blocks Collection of block color data
     * @return Statistics string
     */
    public static String getMappingStats(Collection<BlockColorData> blocks) {
        int total = blocks.size();
        long mapped = blocks.stream()
            .map(BlockColorData::getMaterial)
            .filter(Objects::nonNull)
            .count();
        
        return String.format("Mapped %d/%d blocks (%.1f%%)", 
            mapped, total, (mapped * 100.0 / total));
    }
}
