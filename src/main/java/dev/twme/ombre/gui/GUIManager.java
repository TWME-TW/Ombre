package dev.twme.ombre.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.data.GradientConfig;

/**
 * GUI 管理器
 * 負責管理所有玩家的 GUI 實例和事件處理
 */
public class GUIManager implements Listener {
    
    private final Ombre plugin;
    private final Map<UUID, OmbreGUI> activeOmbreGUIs;
    private final Map<UUID, LibraryGUI> activeLibraryGUIs;
    private final Map<UUID, FavoritesGUI> activeFavoritesGUIs;
    private final Map<UUID, MyGradientsGUI> activeMyGradientsGUIs;
    
    public GUIManager(Ombre plugin) {
        this.plugin = plugin;
        this.activeOmbreGUIs = new HashMap<>();
        this.activeLibraryGUIs = new HashMap<>();
        this.activeFavoritesGUIs = new HashMap<>();
        this.activeMyGradientsGUIs = new HashMap<>();
        
        // 註冊事件監聽器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 打開漸層製作 GUI
     */
    public void openOmbreGUI(Player player) {
        OmbreGUI gui = new OmbreGUI(plugin, player);
        activeOmbreGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    /**
     * 打開漸層製作 GUI 並載入配置
     */
    public void openOmbreGUI(Player player, GradientConfig config) {
        OmbreGUI gui = new OmbreGUI(plugin, player, config);
        activeOmbreGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    /**
     * 打開漸層製作 GUI 並載入配置（帶返回按鈕）
     */
    public void openOmbreGUI(Player player, GradientConfig config, String previousGUI) {
        OmbreGUI gui = new OmbreGUI(plugin, player, config, previousGUI);
        activeOmbreGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    /**
     * 打開共享庫 GUI
     */
    public void openLibraryGUI(Player player) {
        LibraryGUI gui = new LibraryGUI(plugin, player);
        activeLibraryGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    /**
     * 打開我的最愛 GUI
     */
    public void openFavoritesGUI(Player player) {
        FavoritesGUI gui = new FavoritesGUI(plugin, player, this, plugin.getConfigManager());
        activeFavoritesGUIs.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
    }
    
    /**
     * 打開我的漸層 GUI
     */
    public void openMyGradientsGUI(Player player) {
        MyGradientsGUI gui = new MyGradientsGUI(plugin, player, this, plugin.getConfigManager());
        activeMyGradientsGUIs.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
    }
    
    /**
     * 獲取玩家當前的 Ombre GUI
     */
    public OmbreGUI getOmbreGUI(Player player) {
        return activeOmbreGUIs.get(player.getUniqueId());
    }
    
    /**
     * 處理庫存點擊事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // 處理 OmbreGUI 點擊
        if (holder instanceof OmbreGUI ombreGUI) {
            ombreGUI.handleClick(event);
        }
        // 處理 LibraryGUI 點擊
        else if (holder instanceof LibraryGUI libraryGUI) {
            libraryGUI.handleClick(event);
        }
        // 處理 FavoritesGUI 點擊
        else if (holder instanceof FavoritesGUI favoritesGUI) {
            favoritesGUI.handleClick(event);
        }
        // 處理 MyGradientsGUI 點擊
        else if (holder instanceof MyGradientsGUI myGradientsGUI) {
            myGradientsGUI.handleClick(event);
        }
    }
    
    /**
     * 處理庫存關閉事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // 移除已關閉的 GUI
        if (holder instanceof OmbreGUI) {
            activeOmbreGUIs.remove(player.getUniqueId());
        } else if (holder instanceof LibraryGUI) {
            activeLibraryGUIs.remove(player.getUniqueId());
        } else if (holder instanceof FavoritesGUI) {
            activeFavoritesGUIs.remove(player.getUniqueId());
        } else if (holder instanceof MyGradientsGUI) {
            activeMyGradientsGUIs.remove(player.getUniqueId());
        }
    }
    
    /**
     * 清理所有 GUI
     */
    public void cleanup() {
        activeOmbreGUIs.clear();
        activeLibraryGUIs.clear();
        activeFavoritesGUIs.clear();
        activeMyGradientsGUIs.clear();
    }
}
