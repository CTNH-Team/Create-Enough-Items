# Create-Enough-Items

Create Enough Items（CEI）是为整合包 [Create: New Horizon（CTNH）](https://github.com/CTNH-Team/CTNH-Modules) 准备的物品与 EMI 体验增强模组。

当前项目主要围绕 EMI 的物品侧栏、配方页和 GTCEu 配方浏览做定制，让 CTNH 中数量庞大的物品和配方更容易检索、折叠和过滤。

## 基本信息

- Minecraft: `1.20.1`
- Mod Loader: Forge `47.4.1+`
- Java: `17`
- Mod ID: `cei`
- 依赖：`minecraft`、`forge`、`gtceu`、`emi`
- CTNH 模块依赖：`CTNH-Lib`

## 功能

### EMI 侧栏折叠组

CEI 会根据内置规则把 EMI INDEX 侧栏中的同类物品折叠为分组，例如工具、原木、木板、羊毛、药水、唱片、矿石、Sophisticated Storage 容器等。

- 搜索框左侧会显示 `G` 按钮。
- 左键 `G`：智能展开/折叠全部分组。
- 右键 `G`：折叠全部分组。
- `Alt + 左键` 点击侧栏中的分组代表项或成员：切换单个分组。
- 分组展开状态会保存到 `config/cei/collapsible_emi_groups.json`。

折叠组规则位于：

```text
src/main/resources/assets/cei/emi/emi_collapsible_groups.json
```

### EMI 配方页过滤

在 EMI 配方页底部，CEI 添加了多个轻量按钮：

- `回收配方`：显示或隐藏内置精选规则匹配到的回收类配方。
- `重复配方`：显示或隐藏部分 Create 派生/自动化重复配方。
- `关联搜索`：切换 EMI 配方/用途查询时是否展示关联物品与流体配方。
- 电压范围按钮：按 GTCEu 配方电压等级过滤配方。左键或滚轮递增，右键递减。

这些状态会分别保存到：

```text
config/cei/featured_emi_recipes.json
config/cei/duplicate_emi_recipes.json
config/cei/associated_emi_search.json
config/cei/voltage_emi_recipes.json
```

内置精选配方过滤规则位于：

```text
src/main/resources/assets/cei/emi/emi_featured_recipes.json
```

当前默认匹配 GTCEu 的粉碎机、电弧炉和提取机回收配方分类。

### EMI 搜索与性能增强

项目中还包含 EMI 搜索与配方管理相关优化：

- 为 EMI 配方管理器建立按分类、输入、输出和配方 ID 查询的索引。
- 记录最近打开的配方/用途查询对象，用于关联搜索开关刷新当前页面。
- 支持将从 EMI 侧栏拖出的物品名填入悬停的搜索框。
- 在 EMI 搜索重建、可见性变化时重新扫描折叠分组。

## 规则格式

折叠组和精选配方过滤都使用 JSON 定义。顶层 key 是规则组 GUID，值可以是字符串、字符串数组，或包含 `key` / `rules` 的对象。

折叠组示例：

```json
{
  "cei:tools/swords": {
    "key": [
      "#minecraft:swords",
      "#forge:tools/swords"
    ],
    "priority": 0
  },
  "cei:blocks/glazed_terracotta": {
    "key": "regex:minecraft:.*_glazed_terracotta$"
  }
}
```

精选配方过滤示例：

```json
{
  "cei:gtceu/macerator_recycling": {
    "key": "category:gtceu:macerator_recycling"
  }
}
```

常用规则写法：

- `minecraft:stone`：匹配指定物品 ID。
- `#forge:ores`：匹配物品或方块标签。
- `$ingots/iron`：简写为 `#forge:ingots/iron`。
- `regex:minecraft:.*_spawn_egg` 或 `r/minecraft:.*_spawn_egg/`：按物品 ID 正则匹配。
- `!minecraft:air`：取反匹配。
- `ruleA ruleB`：同时满足多个条件。
- `ruleA|ruleB` 或 `ruleA,ruleB`：满足任一条件。
- `0` 或 `0-15`：按物品 damage 范围匹配，主要用于兼容旧配置风格。

精选配方过滤额外支持：

- `recipe:<id>`：匹配配方 ID。
- `recipe_regex:<pattern>`：按配方 ID 正则匹配。
- `category:<id>`：匹配 EMI 配方分类。
- `category_regex:<pattern>`：按 EMI 配方分类正则匹配。
- `input:<item-rule>`：匹配输入。
- `output:<item-rule>`：匹配输出。
- `catalyst:<item-rule>`：匹配催化物。
- `item:<item-rule>`：匹配输入、输出或催化物。

折叠组对象字段：

- `key` / `rules`：规则字符串或规则字符串数组。
- `priority`：非负整数。多个分组同时命中同一物品时，优先级更高的分组生效；同优先级按 JSON 顺序。
- `expanded`：默认展开状态。玩家本地状态文件中已有记录时，以本地状态为准。

## 项目结构

```text
src/main/java/com/moguang/cei
├─ CreateEnoughItems.java              # 模组入口
├─ client/                             # 客户端代理
├─ common/                             # 通用代理
├─ mixin/                              # Forge / EMI / GTCEu mixin 注入
├─ registry/                           # Registrate 初始化
└─ utils/emi/                          # EMI 功能实现
   ├─ collapsible/                     # 侧栏折叠组
   ├─ duplicate/                       # Create 派生重复配方过滤
   ├─ featured/                        # 精选/回收配方过滤
   ├─ search/                          # 关联搜索、快速配方索引、拖拽搜索填充
   └─ voltage/                         # GTCEu 电压范围过滤

src/main/resources
├─ META-INF/mods.toml                  # Forge 模组元数据与依赖
├─ cei.mixins.json                     # Mixin 配置
└─ assets/cei
   ├─ emi/                             # EMI 内置规则
   └─ lang/                            # 本地化文本
```

## 构建

本模块应放在 [CTNH-Team/CTNH-Modules](https://github.com/CTNH-Team/CTNH-Modules) 仓库中构建。

在 CTNH-Modules 根目录运行：

```bash
./gradlew :modules:Create-Enough-Items:build
```

在 Windows PowerShell 中：

```powershell
.\gradlew :modules:Create-Enough-Items:build
```

构建产物位于：

```text
modules/Create-Enough-Items/build/libs/
```

## 开发提示

- 修改 EMI 折叠分组时，优先编辑 `emi_collapsible_groups.json`。
- 修改回收/精选配方隐藏规则时，优先编辑 `emi_featured_recipes.json`。
- 本地运行中产生的开关状态在 `config/cei/` 下；调试默认行为时可以删除对应状态文件。
- 侧栏折叠依赖 EMI 当前 INDEX 列表中的对象身份，搜索刷新和 EMI 可见性变化会触发重新扫描。
- GTCEu 电压过滤只处理 `GTEmiRecipe`，非 GT 配方不会被电压范围过滤隐藏。

## License

All code is licensed under the [GNU LGPL v3 License](https://www.gnu.org/licenses/lgpl-3.0.en.html).

All artwork (images, textures, models, animations, etc.) is licensed under the [Creative Commons Attribution-NonCommercial 4.0 International License](http://creativecommons.org/licenses/by-nc/4.0/), unless stated otherwise.
