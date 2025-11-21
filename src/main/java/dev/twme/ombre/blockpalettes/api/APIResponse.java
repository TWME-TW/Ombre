package dev.twme.ombre.blockpalettes.api;

import java.util.ArrayList;
import java.util.List;

/**
 * API 回應模型
 */
public class APIResponse {
    
    private boolean success;
    private String error;
    private String errorCode;
    private int totalResults;
    private int currentPage;
    private int totalPages;
    private List<PaletteData> palettes;
    
    public APIResponse() {
        this.palettes = new ArrayList<>();
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public List<PaletteData> getPalettes() {
        return palettes;
    }
    
    public void setPalettes(List<PaletteData> palettes) {
        this.palettes = palettes;
    }
}
