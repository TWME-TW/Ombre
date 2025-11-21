package dev.twme.ombre.blockcolors.data;

/**
 * 顏色匹配結果
 */
public class ColorMatch implements Comparable<ColorMatch> {
    private final BlockColorData block;
    private final double similarity;      // 0-100，100 為完全匹配
    private final double deltaE;          // 色彩差異值

    public ColorMatch(BlockColorData block, double similarity, double deltaE) {
        this.block = block;
        this.similarity = similarity;
        this.deltaE = deltaE;
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
     * 取得相似度等級的描述
     */
    public String getSimilarityLevel() {
        if (similarity >= 95) return "§a完美匹配";
        if (similarity >= 85) return "§a極度相似";
        if (similarity >= 70) return "§e非常相似";
        if (similarity >= 50) return "§6相似";
        return "§c較不相似";
    }

    /**
     * 取得相似度百分比字串
     */
    public String getSimilarityPercentage() {
        return String.format("%.1f%%", similarity);
    }

    @Override
    public int compareTo(ColorMatch other) {
        // 相似度高的排在前面
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
