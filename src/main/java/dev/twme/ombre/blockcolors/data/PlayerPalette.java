package dev.twme.ombre.blockcolors.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家調色盤資料
 */
public class PlayerPalette {
    private final List<Material> blocks;
    private final int maxSize;

    public PlayerPalette(int maxSize) {
        this.blocks = new ArrayList<>();
        this.maxSize = maxSize;
    }

    /**
     * 新增方塊到調色盤
     */
    public boolean addBlock(Material material) {
        if (blocks.size() >= maxSize) {
            return false;
        }
        if (blocks.contains(material)) {
            return false;
        }
        return blocks.add(material);
    }

    /**
     * 從調色盤移除方塊
     */
    public boolean removeBlock(Material material) {
        return blocks.remove(material);
    }

    /**
     * 清空調色盤
     */
    public void clear() {
        blocks.clear();
    }

    /**
     * 檢查調色盤是否已滿
     */
    public boolean isFull() {
        return blocks.size() >= maxSize;
    }

    /**
     * 檢查調色盤是否為空
     */
    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /**
     * 取得調色盤中的方塊數量
     */
    public int size() {
        return blocks.size();
    }

    /**
     * 取得所有方塊
     */
    public List<Material> getBlocks() {
        return new ArrayList<>(blocks);
    }

    /**
     * 設定調色盤內容
     */
    public void setBlocks(List<Material> newBlocks) {
        blocks.clear();
        for (Material material : newBlocks) {
            if (blocks.size() >= maxSize) {
                break;
            }
            blocks.add(material);
        }
    }

    /**
     * 取得最大容量
     */
    public int getMaxSize() {
        return maxSize;
    }
}
