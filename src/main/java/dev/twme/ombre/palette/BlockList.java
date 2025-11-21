package dev.twme.ombre.palette;

import java.util.ArrayList;
import java.util.List;

/**
 * 方塊列表（排除列表或色表）
 */
public class BlockList {
    
    private final String id;
    private String name;
    private String description;
    private boolean enabled;
    private final List<String> blocks;
    
    public BlockList(String id) {
        this.id = id;
        this.blocks = new ArrayList<>();
        this.enabled = false;
    }
    
    public BlockList(String id, String name, String description, boolean enabled, List<String> blocks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.blocks = new ArrayList<>(blocks);
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public List<String> getBlocks() {
        return new ArrayList<>(blocks);
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void addBlock(String block) {
        if (!blocks.contains(block)) {
            blocks.add(block);
        }
    }
    
    public void removeBlock(String block) {
        blocks.remove(block);
    }
    
    public void clearBlocks() {
        blocks.clear();
    }
    
    @Override
    public String toString() {
        return String.format("BlockList{id='%s', name='%s', enabled=%s, blocks=%d}", 
            id, name, enabled, blocks.size());
    }
}
