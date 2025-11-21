package dev.twme.ombre.blockpalettes.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.blockpalettes.cache.TermsAcceptanceData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 條款同意介面
 * 首次使用 Block Palettes 時顯示
 */
public class TermsAcceptanceGUI implements Listener {
    
    private final Player player;
    private final Inventory inventory;
    private final Runnable onAccept;
    private final Runnable onDecline;
    
    public TermsAcceptanceGUI(Player player, Runnable onAccept, Runnable onDecline) {
        this.player = player;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
        
        // 建立 27 格箱子 (3x9)
        this.inventory = Bukkit.createInventory(null, 27, 
            Component.text("使用條款與隱私政策")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        
        setupItems();
    }
    
    /**
     * 設定介面物品
     */
    private void setupItems() {
        // 資訊說明 (格子 11)
        ItemStack infoItem = createItem(Material.BOOK,
            Component.text("Block Palettes 使用須知")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("本功能使用 Block Palettes 網站").color(NamedTextColor.GRAY),
                Component.text("提供的公開 API 來瀏覽社群創作").color(NamedTextColor.GRAY),
                Component.text("的方塊配色方案。").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("在使用前，您需要同意以下條款:").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("1. Block Palettes 使用條款").color(NamedTextColor.WHITE),
                Component.text("2. Block Palettes 隱私政策").color(NamedTextColor.WHITE),
                Component.empty(),
                Component.text("這些條款由 Block Palettes 網站").color(NamedTextColor.GRAY),
                Component.text("制定，與本伺服器無關。").color(NamedTextColor.GRAY)
            )
        );
        inventory.setItem(11, infoItem);
        
        // 使用條款 (格子 13)
        ItemStack termsItem = createItem(Material.PAPER,
            Component.text("使用條款 (Terms of Service)")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("點擊此處在聊天欄顯示條款連結").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 左鍵複製連結到聊天欄").color(NamedTextColor.YELLOW),
                Component.text("▸ 右鍵在瀏覽器開啟").color(NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("連結: ").color(NamedTextColor.GRAY)
                    .append(Component.text(TermsAcceptanceData.TERMS_URL).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("請仔細閱讀使用條款後再同意").color(NamedTextColor.GRAY)
            )
        );
        inventory.setItem(13, termsItem);
        
        // 隱私政策 (格子 15)
        ItemStack privacyItem = createItem(Material.SHIELD,
            Component.text("隱私政策 (Privacy Policy)")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("點擊此處在聊天欄顯示隱私政策連結").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 左鍵複製連結到聊天欄").color(NamedTextColor.YELLOW),
                Component.text("▸ 右鍵在瀏覽器開啟").color(NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("連結: ").color(NamedTextColor.GRAY)
                    .append(Component.text(TermsAcceptanceData.PRIVACY_URL).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("請仔細閱讀隱私政策後再同意").color(NamedTextColor.GRAY)
            )
        );
        inventory.setItem(15, privacyItem);
        
        // 我同意 (格子 21)
        ItemStack agreeItem = createItem(Material.GREEN_WOOL,
            Component.text("✓ 我同意")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("點擊表示您已閱讀並同意:").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("• Block Palettes 使用條款").color(NamedTextColor.WHITE),
                Component.text("• Block Palettes 隱私政策").color(NamedTextColor.WHITE),
                Component.empty(),
                Component.text("同意後將記錄您的選擇").color(NamedTextColor.GRAY),
                Component.text("並開啟 Block Palettes 功能").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊同意並繼續").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(21, agreeItem);
        
        // 我不同意 (格子 23)
        ItemStack declineItem = createItem(Material.RED_WOOL,
            Component.text("✗ 我不同意")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("如果不同意條款，將無法使用").color(NamedTextColor.GRAY),
                Component.text("Block Palettes 瀏覽功能。").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("您仍然可以使用伺服器的其他功能。").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊關閉並退出").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(23, declineItem);
    }
    
    /**
     * 建立物品
     */
    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 開啟介面
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * 處理點擊事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        
        if (!clicker.equals(player)) {
            return;
        }
        
        // 檢查是否點擊的是 GUI 而不是玩家背包
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        
        int slot = event.getSlot();
        
        // 使用條款連結 (格子 13)
        if (slot == 13) {
            showTermsLink(clicker);
            return;
        }
        
        // 隱私政策連結 (格子 15)
        if (slot == 15) {
            showPrivacyLink(clicker);
            return;
        }
        
        // 我同意 (格子 21)
        if (slot == 21) {
            clicker.closeInventory();
            clicker.sendMessage(
                Component.text("✓ 已同意 Block Palettes 使用條款與隱私政策")
                    .color(NamedTextColor.GREEN)
            );
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            if (onAccept != null) {
                onAccept.run();
            }
            return;
        }
        
        // 我不同意 (格子 23)
        if (slot == 23) {
            clicker.closeInventory();
            clicker.sendMessage(
                Component.text("您需要同意 Block Palettes 條款才能使用此功能")
                    .color(NamedTextColor.RED)
            );
            clicker.playSound(clicker.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            
            if (onDecline != null) {
                onDecline.run();
            }
        }
    }
    
    /**
     * 在聊天欄顯示使用條款連結
     */
    private void showTermsLink(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
        player.sendMessage(
            Component.text("Block Palettes 使用條款")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("[點擊開啟] ")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.UNDERLINED, true)
                .append(Component.text(TermsAcceptanceData.TERMS_URL)
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.UNDERLINED, true))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.TERMS_URL))
        );
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("請在瀏覽器中閱讀完整條款內容").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
    }
    
    /**
     * 在聊天欄顯示隱私政策連結
     */
    private void showPrivacyLink(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
        player.sendMessage(
            Component.text("Block Palettes 隱私政策")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("[點擊開啟] ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.UNDERLINED, true)
                .append(Component.text(TermsAcceptanceData.PRIVACY_URL)
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.UNDERLINED, true))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.PRIVACY_URL))
        );
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("請在瀏覽器中閱讀完整隱私政策內容").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
    }
}
