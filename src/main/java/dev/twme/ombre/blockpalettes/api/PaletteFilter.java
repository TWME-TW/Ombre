package dev.twme.ombre.blockpalettes.api;

/**
 * 調色板篩選條件
 */
public class PaletteFilter {
    
    private String sortBy;      // recent, popular, oldest, trending
    private String color;       // all, red, orange, yellow, etc.
    private String blockSearch; // 方塊名稱 (snake_case)
    private int page;           // 頁碼 (從 1 開始)
    private int limit;          // 每頁數量 (預設 20)
    
    public PaletteFilter() {
        this.sortBy = "popular";
        this.color = "all";
        this.blockSearch = "";
        this.page = 1;
        this.limit = 20;
    }
    
    /**
     * 轉換為 API 查詢字串
     */
    public String toQueryString() {
        StringBuilder query = new StringBuilder();
        query.append("page=").append(page);
        query.append("&limit=").append(limit);
        query.append("&sort=").append(sortBy);
        
        if (!"all".equals(color)) {
            query.append("&color=").append(color);
        }
        
        if (blockSearch != null && !blockSearch.isEmpty()) {
            query.append("&block=").append(blockSearch);
        }
        
        return query.toString();
    }
    
    // Getters and Setters
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getBlockSearch() {
        return blockSearch;
    }
    
    public void setBlockSearch(String blockSearch) {
        this.blockSearch = blockSearch;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
