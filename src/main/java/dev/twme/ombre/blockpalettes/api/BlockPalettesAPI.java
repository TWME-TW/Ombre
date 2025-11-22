package dev.twme.ombre.blockpalettes.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.twme.ombre.Ombre;

/**
 * Block Palettes API 客戶端
 * 處理與 blockpalettes.com API 的通訊
 */
public class BlockPalettesAPI {
    
    private static final String BASE_URL = "https://www.blockpalettes.com/api";
    private static final String ALL_PALETTES_ENDPOINT = "/palettes/all_palettes.php";
    // 注意：PALETTE_DETAILS_ENDPOINT 在 blockpalettes.com 上不存在（返回 404）
    
    private final Ombre plugin;
    private final Gson gson;
    private final int timeout;
    
    public BlockPalettesAPI(Ombre plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.timeout = plugin.getConfig().getInt("block-palettes.api-timeout", 10000);
    }
    
    /**
     * 取得調色板列表
     */
    public CompletableFuture<APIResponse> getPalettes(PaletteFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String urlStr = BASE_URL + ALL_PALETTES_ENDPOINT + "?" + filter.toQueryString();
                
                String jsonResponse = makeRequest(urlStr);
                
                JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
                APIResponse response = new APIResponse();
                
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    response.setSuccess(true);
                    response.setTotalResults(json.get("total_results").getAsInt());
                    response.setCurrentPage(json.get("current_page").getAsInt());
                    response.setTotalPages(json.get("total_pages").getAsInt());
                    
                    JsonArray palettesArray = json.getAsJsonArray("palettes");
                    List<PaletteData> palettes = new ArrayList<>();
                    
                    for (int i = 0; i < palettesArray.size(); i++) {
                        JsonObject paletteJson = palettesArray.get(i).getAsJsonObject();
                        palettes.add(PaletteData.fromApiResponse(paletteJson));
                    }
                    
                    response.setPalettes(palettes);
                } else {
                    response.setSuccess(false);
                    response.setError(json.has("error") ? json.get("error").getAsString() : "Unknown error");
                    response.setErrorCode(json.has("error_code") ? json.get("error_code").getAsString() : "UNKNOWN");
                }
                
                return response;
                
            } catch (Exception e) {
                plugin.getLogger().warning("API request failed: " + e.getMessage());
                APIResponse errorResponse = new APIResponse();
                errorResponse.setSuccess(false);
                errorResponse.setError("Connection error: " + e.getMessage());
                errorResponse.setErrorCode("CONNECTION_ERROR");
                return errorResponse;
            }
        });
    }
    
    /**
     * 取得單一調色板詳細資訊
     * 注意：blockpalettes.com 不提供詳細資訊 API，此方法僅返回基本結構
     */
    public CompletableFuture<PaletteData> getPaletteDetails(int id) {
        return CompletableFuture.supplyAsync(() -> {
            // API 不提供詳細資訊端點，直接返回基本物件
            // 詳細資訊應該從列表 API 中獲取
            
            PaletteData basicData = new PaletteData();
            basicData.setId(id);
            return basicData;
            
            /* 原始程式碼 - API 端點不存在
            try {
                String urlStr = BASE_URL + PALETTE_DETAILS_ENDPOINT + "?id=" + id;
                plugin.getLogger().info("API 詳細請求: " + urlStr);
                String jsonResponse = makeRequest(urlStr);
                
                JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
                
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    JsonObject paletteJson = json.getAsJsonObject("palette");
                    PaletteData data = PaletteData.fromApiResponse(paletteJson);
                    
                    // 處理相似調色板
                    if (json.has("similar_palettes")) {
                        JsonArray similarArray = json.getAsJsonArray("similar_palettes");
                        List<PaletteData> similarPalettes = new ArrayList<>();
                        
                        for (int i = 0; i < similarArray.size(); i++) {
                            JsonObject similarJson = similarArray.get(i).getAsJsonObject();
                            similarPalettes.add(PaletteData.fromApiResponse(similarJson));
                        }
                        
                        data.setSimilarPalettes(similarPalettes);
                    }
                    
                    return data;
                }
                
                return null;
            } catch (Exception e) {
                return null;
            }
            */
        });
    }
    
    /**
     * 發送 HTTP 請求
     */
    private String makeRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", "Ombre-Minecraft-Plugin/1.0");
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            return response.toString();
        } else {
            throw new Exception("HTTP error code: " + responseCode);
        }
    }
}
