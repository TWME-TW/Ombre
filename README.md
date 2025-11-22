# Ombre

![Minecraft Version](https://img.shields.io/badge/minecraft-1.21.5-brightgreen.svg) ![Java Version](https://img.shields.io/badge/java-21-orange.svg) ![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

A powerful Minecraft plugin for creating beautiful color gradients and managing block palettes | 功能強大的 Minecraft 插件，用於創建美麗的顏色漸變和管理方塊調色板

[English](#english) | [繁體中文](#繁體中文)

---

## English

### About

Ombre is a comprehensive Minecraft server-side plugin that provides advanced color gradient creation tools and block palette management. It integrates three major features: an interactive gradient builder, BlockColors.app color matching, and Block Palettes community browsing. Players can create stunning color gradients using any Minecraft blocks without requiring client-side modifications.

The plugin uses sophisticated algorithms to calculate smooth color transitions between blocks and provides an intuitive GUI interface for easy gradient creation and customization.

### Features

#### Gradient Builder
- **Interactive GUI** - 9×6 grid interface for placing blocks and creating gradients
- **Smart Algorithm** - Weighted average algorithm for smooth color transitions
- **Seed System** - Place blocks as color seed points; the system automatically calculates gradients between them
- **Block Filtering** - Support for block exclusion lists and custom palettes
- **Save & Load** - Save favorite gradients and load them anytime
- **Real-time Preview** - See gradient effects immediately after placing blocks

#### BlockColors.app Integration
- **Color Matching** - Find the closest matching Minecraft blocks for any RGB color
- **Color Picker** - Pick colors from existing blocks in the world
- **Palette Management** - Create and manage personal color palettes
- **Delta E 2000** - Advanced color similarity algorithm for accurate matching
- **Similarity Search** - Adjustable similarity threshold for flexible results
- **API Integration** - Real-time data from BlockColors.app with local caching

#### Block Palettes Community
- **Browse Palettes** - Explore community-created block palettes from blockpalettes.com
- **Favorites System** - Save favorite palettes for quick access
- **Multiple Sorting** - Sort by recent, popular, oldest, or trending
- **Search Function** - Search palettes by keywords
- **Terms Tracking** - Integrated terms of service acceptance system
- **Offline Caching** - Local cache for improved performance

#### Multi-language Support
- **Automatic Detection** - Automatically detects player's client language
- **Manual Override** - Players can manually change interface language
- **Supported Languages**:
  - English (en_US)
  - Traditional Chinese (zh_TW)

#### Configuration System
- **Server Defaults** - Global configuration in `config.yml`
- **Per-player Settings** - Individual player configurations
- **Hot Reload** - Reload configuration without server restart
- **Block Lists** - Customizable exclusion lists and palettes

### Requirements

- Minecraft 1.21.5 (Paper)
- Java 21 or higher
- No client-side mods required

### Installation

1. Download `Ombre.jar` from the releases page
2. Place it in your server's `plugins` folder
3. Restart the server
4. The plugin will automatically generate configuration files
5. (Optional) Edit `plugins/Ombre/config.yml` to customize settings
6. Use `/ombre reload` to reload the configuration

### Commands

#### Gradient Builder Commands

`/ombre` - Open the gradient builder GUI
- Opens an interactive 9×6 grid for creating gradients
- Permission: `ombre.use`

`/ombre library` - Open the shared gradient library
- Permission: `ombre.library`

`/ombre favorites` - View your favorite gradients
- Permission: `ombre.use`

`/ombre palette` - Manage block palettes
- `list` - List all available palettes
- `enable <id>` - Enable a specific palette
- `disable <id>` - Disable a specific palette
- `reset` - Reset palette settings

`/ombre exclusion` - Manage block exclusion lists
- `list` - List all exclusion lists
- `enable <id>` - Enable an exclusion list
- `disable <id>` - Disable an exclusion list

`/ombre reload` - Reload plugin configuration
- Permission: `ombre.admin`

#### BlockColors.app Commands

`/blockcolorsapp` or `/bca` - Open the color matching GUI
- Permission: `ombre.blockcolorsapp.use`

`/bca reload` - Reload color cache
- Permission: `ombre.blockcolorsapp.reload`

`/bca cache` - View cache status
- Permission: `ombre.blockcolorsapp.admin`

`/bca clear-cache` - Clear color cache
- Permission: `ombre.blockcolorsapp.admin`

#### Block Palettes Commands

`/blockpalettes` or `/bp` - Open the palette browser
- Permission: `ombre.blockpalettes.use`

`/bp favorites` - View your favorite palettes
- Permission: `ombre.blockpalettes.use`

`/bp reload` - Reload palette cache
- Permission: `ombre.blockpalettes.reload`

`/bp cache` - View cache status
- Permission: `ombre.blockpalettes.reload`

`/bp terms` - View terms agreement status
- Permission: `ombre.blockpalettes.terms.manage`

`/bp help` - Display help information
- Permission: `ombre.blockpalettes.use`

### Configuration

The main configuration file is `config.yml`:

```yaml
# Plugin Settings
settings:
  max-gradients-per-player: -1  # Maximum gradients per player (-1 = unlimited)
  
  gradient:
    min-blocks: 2                # Minimum blocks required for gradient
    ignore-transparent: false    # Ignore transparent blocks
    ignore-same-color: true      # Ignore blocks with same color
    block-selection-mode: all    # Block selection mode: all/colorful/natural
  
  prevent-pickup-when-full: true # Prevent block pickup when inventory is full
  language: en_US                # Default language

# BlockColors Feature
blockcolors:
  cache:
    expiry-time: 604800000       # Cache expiry (7 days in milliseconds)
    auto-update-on-startup: true
  
  api:
    url: "https://blockcolors.app/assets/color_data.json"
    timeout: 15
    retry-count: 3
  
  matching:
    default-results: 18          # Number of results to display
    min-similarity: 30           # Minimum similarity threshold (0-100)
    use-delta-e-2000: true       # Use Delta E 2000 algorithm
  
  gui:
    rgb-step: 10                 # RGB adjustment step
    blocks-per-page: 18          # Blocks per page
    max-palette-size: 18         # Maximum palette size

# Block Palettes Feature
block-palettes:
  enabled: true                  # Enable Block Palettes feature
  cache-duration: 300            # Cache duration in seconds
  palettes-per-page: 20          # Palettes per page
  api-timeout: 10000             # API timeout in milliseconds
  default-sort: "recent"         # Default sort: recent/popular/oldest/trending
  enable-favorites: true
  max-favorites: 100             # Maximum favorites per player
```

Each feature has its own configuration section for fine-tuned control.

### How It Works

#### Gradient Algorithm

The plugin uses a weighted average algorithm to create smooth color transitions:

1. **Extract Seed Points** - Identify blocks placed by the player as color seeds
2. **Calculate Distances** - Compute distance from each position to all seed points
3. **Apply Weights** - Use inverse square distance as weight (closer seeds have more influence)
4. **Color Interpolation** - Calculate weighted average of RGB values
5. **Block Matching** - Find the closest matching Minecraft block for each calculated color

#### Color Matching Technology

BlockColors.app integration provides:
- **Delta E 2000** - Industry-standard color difference algorithm that accounts for human color perception
- **LAB Color Space** - Converts RGB to LAB for more accurate color comparison
- **Similarity Scoring** - Calculates similarity percentage for each block
- **Material Filtering** - Filters unmapped textures to ensure valid block matches

#### Block Palettes System

- **API Integration** - Fetches community palettes from blockpalettes.com
- **Caching System** - Local cache reduces API calls and improves performance
- **Favorites Storage** - Per-player favorites stored in `plugins/Ombre/blockpalettes/favorites/`
- **Terms Tracking** - Tracks terms acceptance per player with version control

### Permissions

```
# Gradient Builder Permissions
ombre.use                         # Use gradient builder (default: true)
ombre.library                     # Access shared library (default: true)
ombre.publish                     # Publish gradients (default: true)
ombre.admin                       # Admin commands (default: op)

# BlockColors.app Permissions
ombre.blockcolorsapp.use          # Use BlockColors features (default: true)
ombre.blockcolorsapp.reload       # Reload cache (default: op)
ombre.blockcolorsapp.admin        # Admin commands (default: op)

# Block Palettes Permissions
ombre.blockpalettes.use           # Browse palettes (default: true)
ombre.blockpalettes.reload        # Reload settings (default: op)
ombre.blockpalettes.terms.manage  # Manage terms (default: op)
```

### Usage

#### Creating a Gradient

1. Use `/ombre` to open the gradient builder GUI
2. Place blocks in the grid as color seed points
3. Click "Calculate Gradient" to generate the gradient
4. The system will fill empty slots with gradient blocks
5. Click "Save Gradient" to save your creation
6. Use "Take All" to move blocks to your inventory

#### Using BlockColors.app

1. Use `/bca` to open the color matching GUI
2. Adjust RGB sliders to select your desired color
3. View matched blocks sorted by similarity
4. Click blocks to add them to your palette
5. Use the palette for quick access to favorite colors
6. Export palette blocks to your inventory

#### Browsing Block Palettes

1. Use `/bp` to open the palette browser
2. Browse community-created palettes
3. Use sorting options to find interesting palettes
4. Click the favorite button (star) to save palettes
5. Access favorites with `/bp favorites`
6. Terms acceptance required on first use

### Building

To build the plugin from source:

```bash
mvn clean package
```

The compiled jar will be in `target/Ombre-1.0.0.jar`

#### Project Structure

```
src/main/java/dev/twme/ombre/
├── Ombre.java                    # Main plugin class
├── algorithm/
│   └── GradientAlgorithm.java    # Gradient calculation algorithm
├── blockcolors/                  # BlockColors.app feature
│   ├── BlockColorsFeature.java   # Feature manager
│   ├── BlockColorCache.java      # Color data cache
│   ├── ColorMatcher.java         # Color matching engine
│   ├── command/                  # Command handlers
│   ├── data/                     # Data models
│   └── gui/                      # GUI interfaces
├── blockpalettes/                # Block Palettes feature
│   ├── BlockPalettesFeature.java # Feature manager
│   ├── api/                      # API client
│   ├── cache/                    # Cache system
│   ├── command/                  # Command handlers
│   └── gui/                      # GUI interfaces
├── color/
│   ├── BlockColor.java           # Block color data
│   ├── ColorDataGenerator.java   # Color data generator
│   └── ColorService.java         # Color service
├── command/
│   └── CommandHandler.java       # Main command handler
├── gui/
│   ├── GUIManager.java           # GUI manager
│   ├── OmbreGUI.java             # Gradient builder GUI
│   ├── LibraryGUI.java           # Shared library GUI
│   └── FavoritesGUI.java         # Favorites GUI
├── i18n/
│   ├── MessageManager.java       # Message localization
│   └── PlayerLocaleListener.java # Locale detection
└── manager/
    └── ConfigManager.java        # Configuration manager
```

### Contributing

Issues and pull requests are welcome! Please ensure your code follows the existing style and includes appropriate documentation.

### License

Copyright 2025 TWME-TW

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

### Credits

- **TWME-TW** - Developer
- **BlockColors.app** - Color data API provider
- **Block Palettes** - Community palette platform

### Related Projects

- [BlockColors.app](https://blockcolors.app/) - Minecraft block color matching tool
- [Block Palettes](https://www.blockpalettes.com/) - Minecraft community palette sharing platform
- [Paper](https://papermc.io/) - High-performance Minecraft server

---

## 繁體中文

### 關於

Ombre 是一個全面性的 Minecraft 伺服器端插件，提供先進的顏色漸層創建工具和方塊調色板管理功能。它整合了三個主要功能：互動式漸層建構器、BlockColors.app 顏色匹配，以及 Block Palettes 社群瀏覽。玩家可以使用任何 Minecraft 方塊創建驚人的顏色漸層，無需安裝客戶端模組。

此插件使用複雜的演算法計算方塊之間的平滑顏色過渡，並提供直觀的 GUI 介面，便於輕鬆創建和自訂漸層。

### 功能

#### 漸層建構器
- **互動式 GUI** - 9×6 網格介面，可放置方塊並創建漸層
- **智能演算法** - 使用加權平均演算法實現平滑顏色過渡
- **種子系統** - 放置方塊作為顏色種子點，系統自動計算它們之間的漸層
- **方塊過濾** - 支援方塊排除列表和自訂調色板
- **儲存與載入** - 儲存喜愛的漸層，隨時載入使用
- **即時預覽** - 放置方塊後立即看到漸層效果

#### BlockColors.app 整合
- **顏色匹配** - 為任何 RGB 顏色找到最接近的 Minecraft 方塊
- **顏色選擇器** - 從世界中現有的方塊選擇顏色
- **調色板管理** - 創建和管理個人顏色調色板
- **Delta E 2000** - 先進的顏色相似度演算法，提供精確匹配
- **相似度搜尋** - 可調整相似度門檻，靈活獲取結果
- **API 整合** - 從 BlockColors.app 即時獲取資料，並在本地快取

#### Block Palettes 社群
- **瀏覽調色板** - 探索來自 blockpalettes.com 的社群創建調色板
- **收藏系統** - 儲存喜愛的調色板以快速存取
- **多種排序** - 按最新、熱門、最舊或趨勢排序
- **搜尋功能** - 使用關鍵字搜尋調色板
- **條款追蹤** - 整合服務條款接受系統
- **離線快取** - 本地快取提升效能

#### 多語言支援
- **自動偵測** - 自動偵測玩家的客戶端語言
- **手動覆寫** - 玩家可手動變更介面語言
- **支援語言**：
  - 英文 (en_US)
  - 繁體中文 (zh_TW)

#### 配置系統
- **伺服器預設值** - 在 `config.yml` 中的全域配置
- **玩家專屬設定** - 個別玩家配置
- **熱重載** - 無需重啟伺服器即可重新載入配置
- **方塊列表** - 可自訂排除列表和調色板

### 需求

- Minecraft 1.21.5（Paper）
- Java 21 或更高版本
- 不需要客戶端模組

### 安裝

1. 從發布頁面下載 `Ombre.jar`
2. 將其放入伺服器的 `plugins` 資料夾
3. 重啟伺服器
4. 插件將自動生成配置檔案
5. （可選）編輯 `plugins/Ombre/config.yml` 以自訂設定
6. 使用 `/ombre reload` 重新載入配置

### 指令

#### 漸層建構器指令

`/ombre` - 開啟漸層建構器 GUI
- 開啟一個互動式 9×6 網格用於創建漸層
- 權限：`ombre.use`

`/ombre library` - 開啟共享漸層庫
- 權限：`ombre.library`

`/ombre favorites` - 查看您收藏的漸層
- 權限：`ombre.use`

`/ombre palette` - 管理方塊調色板
- `list` - 列出所有可用調色板
- `enable <id>` - 啟用特定調色板
- `disable <id>` - 停用特定調色板
- `reset` - 重置調色板設定

`/ombre exclusion` - 管理方塊排除列表
- `list` - 列出所有排除列表
- `enable <id>` - 啟用排除列表
- `disable <id>` - 停用排除列表

`/ombre reload` - 重新載入插件配置
- 權限：`ombre.admin`

#### BlockColors.app 指令

`/blockcolorsapp` 或 `/bca` - 開啟顏色匹配 GUI
- 權限：`ombre.blockcolorsapp.use`

`/bca reload` - 重新載入顏色快取
- 權限：`ombre.blockcolorsapp.reload`

`/bca cache` - 查看快取狀態
- 權限：`ombre.blockcolorsapp.admin`

`/bca clear-cache` - 清除顏色快取
- 權限：`ombre.blockcolorsapp.admin`

#### Block Palettes 指令

`/blockpalettes` 或 `/bp` - 開啟調色板瀏覽器
- 權限：`ombre.blockpalettes.use`

`/bp favorites` - 查看您收藏的調色板
- 權限：`ombre.blockpalettes.use`

`/bp reload` - 重新載入調色板快取
- 權限：`ombre.blockpalettes.reload`

`/bp cache` - 查看快取狀態
- 權限：`ombre.blockpalettes.reload`

`/bp terms` - 查看條款同意狀態
- 權限：`ombre.blockpalettes.terms.manage`

`/bp help` - 顯示幫助資訊
- 權限：`ombre.blockpalettes.use`

### 配置

主要配置檔案為 `config.yml`：

```yaml
# 插件設定
settings:
  max-gradients-per-player: -1  # 每位玩家最大漸層數量（-1 = 無限）
  
  gradient:
    min-blocks: 2                # 漸層所需最少方塊數量
    ignore-transparent: false    # 忽略透明方塊
    ignore-same-color: true      # 忽略相同顏色的方塊
    block-selection-mode: all    # 方塊選擇模式：all/colorful/natural
  
  prevent-pickup-when-full: true # 背包滿時防止拾取方塊
  language: en_US                # 預設語言

# BlockColors 功能
blockcolors:
  cache:
    expiry-time: 604800000       # 快取過期時間（7 天，單位毫秒）
    auto-update-on-startup: true
  
  api:
    url: "https://blockcolors.app/assets/color_data.json"
    timeout: 15
    retry-count: 3
  
  matching:
    default-results: 18          # 顯示的結果數量
    min-similarity: 30           # 最小相似度門檻（0-100）
    use-delta-e-2000: true       # 使用 Delta E 2000 演算法
  
  gui:
    rgb-step: 10                 # RGB 調整步長
    blocks-per-page: 18          # 每頁方塊數量
    max-palette-size: 18         # 最大調色板大小

# Block Palettes 功能
block-palettes:
  enabled: true                  # 啟用 Block Palettes 功能
  cache-duration: 300            # 快取持續時間（秒）
  palettes-per-page: 20          # 每頁調色板數量
  api-timeout: 10000             # API 逾時時間（毫秒）
  default-sort: "recent"         # 預設排序：recent/popular/oldest/trending
  enable-favorites: true
  max-favorites: 100             # 每位玩家最大收藏數量
```

每個功能都有自己的配置區段以進行精細控制。

### 運作原理

#### 漸層演算法

插件使用加權平均演算法創建平滑的顏色過渡：

1. **提取種子點** - 識別玩家放置的方塊作為顏色種子
2. **計算距離** - 計算每個位置到所有種子點的距離
3. **應用權重** - 使用距離平方的倒數作為權重（較近的種子影響更大）
4. **顏色插值** - 計算 RGB 值的加權平均
5. **方塊匹配** - 為每個計算出的顏色找到最接近的 Minecraft 方塊

#### 顏色匹配技術

BlockColors.app 整合提供：
- **Delta E 2000** - 業界標準的顏色差異演算法，考慮人類色彩感知
- **LAB 色彩空間** - 將 RGB 轉換為 LAB 以進行更準確的顏色比較
- **相似度評分** - 為每個方塊計算相似度百分比
- **材質過濾** - 過濾未映射的材質以確保有效的方塊匹配

#### Block Palettes 系統

- **API 整合** - 從 blockpalettes.com 獲取社群調色板
- **快取系統** - 本地快取減少 API 呼叫並提升效能
- **收藏儲存** - 每位玩家的收藏儲存在 `plugins/Ombre/blockpalettes/favorites/`
- **條款追蹤** - 追蹤每位玩家的條款接受狀態，具版本控制

### 權限

```
# 漸層建構器權限
ombre.use                         # 使用漸層建構器（預設：true）
ombre.library                     # 存取共享庫（預設：true）
ombre.publish                     # 發布漸層（預設：true）
ombre.admin                       # 管理員指令（預設：op）

# BlockColors.app 權限
ombre.blockcolorsapp.use          # 使用 BlockColors 功能（預設：true）
ombre.blockcolorsapp.reload       # 重新載入快取（預設：op）
ombre.blockcolorsapp.admin        # 管理員指令（預設：op）

# Block Palettes 權限
ombre.blockpalettes.use           # 瀏覽調色板（預設：true）
ombre.blockpalettes.reload        # 重新載入設定（預設：op）
ombre.blockpalettes.terms.manage  # 管理條款（預設：op）
```

### 使用方式

#### 創建漸層

1. 使用 `/ombre` 開啟漸層建構器 GUI
2. 在網格中放置方塊作為顏色種子點
3. 點擊「計算漸層」生成漸層
4. 系統將用漸層方塊填充空白位置
5. 點擊「儲存漸層」保存您的創作
6. 使用「全部取出」將方塊移至您的背包

#### 使用 BlockColors.app

1. 使用 `/bca` 開啟顏色匹配 GUI
2. 調整 RGB 滑桿選擇您想要的顏色
3. 查看按相似度排序的匹配方塊
4. 點擊方塊將其添加到您的調色板
5. 使用調色板快速存取喜愛的顏色
6. 將調色板方塊匯出到您的背包

#### 瀏覽 Block Palettes

1. 使用 `/bp` 開啟調色板瀏覽器
2. 瀏覽社群創建的調色板
3. 使用排序選項找到有趣的調色板
4. 點擊收藏按鈕（星星）儲存調色板
5. 使用 `/bp favorites` 存取收藏
6. 首次使用需要接受條款

### 建置

從原始碼建置插件：

```bash
mvn clean package
```

編譯後的 jar 檔案位於 `target/Ombre-1.0.0.jar`

#### 專案結構

```
src/main/java/dev/twme/ombre/
├── Ombre.java                    # 主插件類別
├── algorithm/
│   └── GradientAlgorithm.java    # 漸層計算演算法
├── blockcolors/                  # BlockColors.app 功能
│   ├── BlockColorsFeature.java   # 功能管理器
│   ├── BlockColorCache.java      # 顏色資料快取
│   ├── ColorMatcher.java         # 顏色匹配引擎
│   ├── command/                  # 指令處理器
│   ├── data/                     # 資料模型
│   └── gui/                      # GUI 介面
├── blockpalettes/                # Block Palettes 功能
│   ├── BlockPalettesFeature.java # 功能管理器
│   ├── api/                      # API 客戶端
│   ├── cache/                    # 快取系統
│   ├── command/                  # 指令處理器
│   └── gui/                      # GUI 介面
├── color/
│   ├── BlockColor.java           # 方塊顏色資料
│   ├── ColorDataGenerator.java   # 顏色資料生成器
│   └── ColorService.java         # 顏色服務
├── command/
│   └── CommandHandler.java       # 主指令處理器
├── gui/
│   ├── GUIManager.java           # GUI 管理器
│   ├── OmbreGUI.java             # 漸層建構器 GUI
│   ├── LibraryGUI.java           # 共享庫 GUI
│   └── FavoritesGUI.java         # 收藏 GUI
├── i18n/
│   ├── MessageManager.java       # 訊息本地化
│   └── PlayerLocaleListener.java # 語言偵測
└── manager/
    └── ConfigManager.java        # 配置管理器
```

### 貢獻

歡迎提交 Issue 和 Pull Request！請確保您的程式碼遵循現有風格並包含適當的文件。

### 授權

版權所有 2025 TWME-TW

根據 Apache License 2.0 授權。詳見 [LICENSE](LICENSE) 檔案。

### 製作

- **TWME-TW** - 開發者
- **BlockColors.app** - 顏色資料 API 提供者
- **Block Palettes** - 社群調色板平台

### 相關專案

- [BlockColors.app](https://blockcolors.app/) - Minecraft 方塊顏色匹配工具
- [Block Palettes](https://www.blockpalettes.com/) - Minecraft 社群調色板分享平台
- [Paper](https://papermc.io/) - 高效能 Minecraft 伺服器

---

Made with ❤️ by TWME-TW
