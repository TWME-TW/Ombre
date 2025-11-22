package dev.twme.ombre.blockcolors.data;

import dev.twme.ombre.i18n.MessageManager;
import net.kyori.adventure.text.Component;

/**
 * Color matching result
 */
public class ColorMatch implements Comparable<ColorMatch> {
    private final BlockColorData block;
    private final double similarity;      // 0-100, 100 is perfect match
    private final double deltaE;          // Color difference value
    private MessageManager messageManager; // Optional for localization

    public ColorMatch(BlockColorData block, double similarity, double deltaE) {
        this.block = block;
        this.similarity = similarity;
        this.deltaE = deltaE;
        this.messageManager = null;
    }
    
    public ColorMatch(BlockColorData block, double similarity, double deltaE, MessageManager messageManager) {
        this.block = block;
        this.similarity = similarity;
        this.deltaE = deltaE;
        this.messageManager = messageManager;
    }
    
    public void setMessageManager(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public BlockColorData getBlock() {
        return block;
    }

    public double getSimilarity() {
        return similarity;
    }

    public double getDeltaE() {
        return deltaE;
    }

    /**
     * Get similarity level as localized Component
     */
    public Component getSimilarityLevelComponent() {
        String key;
        if (similarity >= 95) {
            key = "blockcolors.similarity.perfect";
        } else if (similarity >= 85) {
            key = "blockcolors.similarity.very-similar";
        } else if (similarity >= 70) {
            key = "blockcolors.similarity.quite-similar";
        } else if (similarity >= 50) {
            key = "blockcolors.similarity.similar";
        } else {
            key = "blockcolors.similarity.not-similar";
        }
        
        if (messageManager != null) {
            return messageManager.getComponent(key);
        } else {
            // Fallback to English if MessageManager not available
            return getFallbackComponent(key);
        }
    }
    
    private Component getFallbackComponent(String key) {
        switch(key) {
            case "blockcolors.similarity.perfect":
                return Component.text("Perfect match").color(net.kyori.adventure.text.format.NamedTextColor.GREEN);
            case "blockcolors.similarity.very-similar":
                return Component.text("Very similar").color(net.kyori.adventure.text.format.NamedTextColor.GREEN);
            case "blockcolors.similarity.quite-similar":
                return Component.text("Quite similar").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW);
            case "blockcolors.similarity.similar":
                return Component.text("Similar").color(net.kyori.adventure.text.format.NamedTextColor.GOLD);
            case "blockcolors.similarity.not-similar":
                return Component.text("Not very similar").color(net.kyori.adventure.text.format.NamedTextColor.RED);
            default:
                return Component.text("Unknown");
        }
    }

    /**
     * Get similarity percentage as string
     */
    public String getSimilarityPercentage() {
        return String.format("%.1f%%", similarity);
    }

    @Override
    public int compareTo(ColorMatch other) {
        // Higher similarity comes first
        return Double.compare(other.similarity, this.similarity);
    }

    @Override
    public String toString() {
        return "ColorMatch{" +
                "block=" + block.getDisplayName() +
                ", similarity=" + String.format("%.2f%%", similarity) +
                ", deltaE=" + String.format("%.2f", deltaE) +
                '}';
    }
}
