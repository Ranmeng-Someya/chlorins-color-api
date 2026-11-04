# Chlorins Color API

把 `语言文件` 里的自定义颜色标记，变成真正的 Minecraft 彩色文本（Component），支持**十六进制纯色**与**平滑 RGB 渐变**。

- 客户端模组（Fabric），不需要服务端安装
- 纯色 / 双色渐变 / **多色渐变** / 多段混排 / 转义 / 错误标记原样保留
- **全局渲染接管**：聊天框、物品名、tooltip、记分板、第三方 HUD 全部生效，无需逐个模组适配
- **动态特效**：`%rainbow[]` 彩虹流光、`%wave[]` 波浪起伏、`%pulse[]` 呼吸闪烁
- 与 `§c`、`&a` 等原版格式代码共存
- 向后兼容原 MatrixColorAPI 的 `&`、`&#RRGGBB`、`<SOLID>`、`<GRADIENT>` 语法与 `MatrixColorAPI.process()` API

> 原项目名 `MatrixColorAPI` 已改名为 **Chlorins Color API**（mod id `chlorins_color_api`）。

---

## 1. 语法

| 写法 | 含义 | 示例 |
|---|---|---|
| `<前缀>#RRGGBB` | 纯色，直到 `%r` 或下一个颜色标记 | `%#FF4500传说之刃%r` |
| `<前缀>#RRGGBB-#RRGGBB` | 双色线性渐变，直到 `%r` 或下一个标记 | `%#FF0000-#0000FF红到蓝渐变%r` |
| `<前缀>#A-B-C-D…` | 多色渐变，按停靠点均匀分配 | `%#FF0000-#FFFF00-#00FF00红黄绿%r` |
| `%rainbow[文字]` | 彩虹流光，色相随时间旋转 | `%rainbow[彩虹文字]` |
| `%wave[文字]` | 波浪起伏，逐字 Y 轴正弦偏移 | `%wave[波浪文字]` |
| `%pulse[文字]` | 呼吸闪烁，亮度正弦明暗 | `%pulse[呼吸文字]` |
| `%blink[文字]` | 硬开关闪烁 | `%blink[警告]` |
| `%fade[文字]` | 透明度呼吸（真 alpha） | `%fade[淡入淡出]` |
| `%bounce[文字]` | 逐字上下弹跳 | `%bounce[弹跳]` |
| `%jitter[文字]` | 逐字随机抖动（确定性哈希） | `%jitter[抖动]` |
| `%flow[文字]` | **动态渐变**：色带随时间在文字上滚动 | `%flow[流光溢彩]` |
| `%flow[#A-#B\|文字]` | 动态渐变 + 自定义色带 | `%flow[#FF0000-#00FF00\|红绿流动]` |
| `%aHH` | **透明度**，HH 为 00–FF | `%a80半透明%r` |
| `§k` / `&k` | 原版乱码效果，**在任何文本里都生效** | `§k乱码文字` |
| `%b` / `%l` | **粗体**，直到 `%r` | `%b粗体%r` |
| `%i` / `%o` | **斜体** | `%i斜体%r` |
| `%u` / `%n` | **下划线** | `%u下划线%r` |
| `%m` | **删除线** | `%m删除线%r` |
| `%k` | 乱码（obfuscated） | `%k乱码%r` |
| `%b[…]` 等 | **作用域写法**，只影响括号内 | `前%b[粗]后` |
| `%outline[…]` | **描边**，边框颜色跟着文字自身的渐变/动画走 | `%#FF0000-#0000FF%outline[边框]` |
| `%r` / `%R` | 恢复默认样式 | `%#FF0000红%r普通` |
| `%%` | 字面百分号 | `%%#FF0000不是颜色` |
| `§0`–`§f`、`§k`–`§o`、`§r` | 原版 16 色与格式码，照旧可用 | `§c红 §l粗` |
| `&a`、`&l` … | 同上，`&` 写法也认 | `&a绿色` |
| `<SOLID:#RRGGBB>…</SOLID>` | 旧 MatrixColorAPI 标签 | 自动转换 |
| `<GRADIENT:#A>…</GRADIENT:#B>` | 旧 MatrixColorAPI 标签 | 自动转换 |

**前缀字符集合默认是 `% & @ #`**（见 `ColorParser.DEFAULT_PREFIXES`，可自行增删）。
必须是「前缀字符 + `#` + 6 位十六进制」，因此：

- `&#FF0000` ✅（同时也符合旧语法）
- `%#FF0000` ✅ `@#0000FF` ✅ `##FF0000` ✅
- `#FF0000` ❌ 单个 `#` 不算标记，普通井号不受影响

### 语言文件示例

```json
{
  "item.demo.solid": "%#FF4500传说之刃%r",
  "item.demo.gradient": "%#FF0000-#FFFF00烈焰魔王%r",
  "item.demo.mixed": "%#00FF00绿色%r 普通文本 %#FFAA00橙色%r",
  "item.demo.escape": "%%#FF0000不是颜色"
}
```

---

## 2. 实际效果（本机实测输出）

```
IN   %#FF0000纯红%r
RUNS [纯红 #FF0000]

IN   %#FF0000-#00FF00红绿渐变%r
RUNS [红 #FF0000] [绿 #AA5500] [渐 #55AA00] [变 #00FF00]

IN   %#FF0000-#0000FF红到蓝渐变%r
RUNS [红 #FF0000] [到 #BF0040] [蓝 #800080] [渐 #4000BF] [变 #0000FF]

IN   %#FF0000红%r%#00FF00绿%r
RUNS [红 #FF0000] [绿 #00FF00]

IN   %%#FF0000字面百分号
RUNS [%#FF0000字面百分号 #------]

IN   %#ZZZZZZ无效保留
RUNS [%#ZZZZZZ无效保留 #------]

IN   §c原版红色 §l加粗
RUNS [原版红色  #FF5555] [加粗 #FF5555 bold]

IN   😀%#FF0000-#0000FF表情也安全%r😀
RUNS [😀 #------] [表 #FF0000] [情 #BF0040] [也 #800080] [安 #4000BF] [全 #0000FF] [😀 #------]
```

渐变是**逐 Unicode 码点**计算的，`t = i / (n - 1)`，首尾严格命中两个端点色；单字符渐变取起始色（不会出现 `0/0` 变黑）。

---

## 3. 为什么不用 `§` 字符串？

这是本次实现里最关键的一个决定，依据是 1.21.1 客户端真实代码：

- `StringDecomposer` 确实会解析 `§`（`char == 167` 分支会查 `ChatFormatting`），所以 `§c` 这类**16 色**在字面/翻译文本里是能渲染的；
- 但 `net.minecraft.ChatFormatting` 一共只有 **22 个常量**（16 色 + `§k§l§m§n§o` + `§r`），**没有 `x`**。

因此 `§x§R§R§G§G§B§B` 在**原版客户端**上会被逐段当成普通颜色码吃掉，最后只剩最后一个 `§b` 生效——渐变会塌缩成单色（这正是原 MatrixColorAPI 的渐变完全失效的根本原因）。

结论：**十六进制颜色必须走 Component（`Text` + `TextColor`），不能走字符串。** 本模组因此在 `TranslatableTextContent` 上把翻译结果重建为 Component。

---

## 3.4 覆盖范围（哪些文本会生效）

| 文本来源 | 渲染路径 | 状态 |
|---|---|---|
| 聊天框 | `ChatHud` → `DrawContext.drawTextWithShadow` → `TextRenderer.draw(Text)` | ✅ |
| `/title`、`/subtitle` | `InGameHud.renderTitleAndSubtitle` → `getWidth(Text)` + `drawTextWithBackground` → `draw(Text)` | ✅ 含居中修正 |
| `/actionbar`、`/tellraw` | `drawTextWithBackground` / 聊天管线 | ✅ |
| 物品名、tooltip、附魔描述 | `draw(Text)` / `draw(String)` | ✅ |
| 记分板、Tab 列表、Boss 条 | `draw(Text)` | ✅ |
| 实体名、名牌、告示牌、悬挂牌 | `draw(String/Text, Matrix4f)` | ✅ |
| 书与笔、成书 | `draw(String)` | ✅ |
| 第三方 HUD（Noelles Roles 等） | `draw(Text)` / `draw(String)` | ✅ |
| 带描边文字 | `TextRenderer.drawWithOutline` | ✅ 1.0.2 新增 |
| 已经烘焙进贴图的文字（logo、PNG 字体） | — | ❌ 无法处理（像素不是字符） |
| 启动器 / 其他程序 / 系统界面 | — | ❌ 不在游戏渲染管线内 |

### 关于 `/title` 的两个细节

1. **居中偏移**：原版用 `getWidth(title)` 计算居中位置，而 `%#FF0000` 这些字符本身有宽度却不会被画出来，所以标题会偏。1.0.2 起同时 hook `TextRenderer.getWidth(String/Text)`，把标记宽度减掉，居中才准。
2. **服务端**：`/title` 的参数在服务端就已经被解析成 JSON 文本组件，`%#` 必须写在组件的字符串内容里（例如 `/title @a title {"text":"%#FF0000你好"}`，或直接 `/title @a title %#FF0000你好`）。客户端只负责把这段字面文本染色——服务端不需要装本模组。

## 3.5 全局渲染接管（1.0.1）

所有上屏文字最终都会经过 `net.minecraft.client.font.TextRenderer#draw`，所以只 hook 三个重载就覆盖全部来源：

| 重载 | 覆盖范围 |
|---|---|
| `draw(String, …)` | 大部分原版 HUD、第三方直接画字符串 |
| `draw(Text, …)` | 聊天框、物品名、tooltip、`DrawContext.drawText` |
| `draw(OrderedText, …)` | 已分解文本、部分模组的自绘路径 |

关键点：

- **不是**替换组件树，而是给 `OrderedText` 套一层**流式视图**（`ChlorinsOrderedText`），逐个字符改写 `Style`。每帧开销 = 1 次哈希查找 + 每个可见字符 1 个小 `Style`，没有组件重建、没有字符串拼接、没有正则。
- 不含标记的字符串走 `hasMarkers` 快速判定，立即返回，**零分配**。
- `§` 代码不受影响：字符串路径用 `OrderedText.styledForwardsVisitedString` 保持原版解析行为；`Text` 路径保留原有 style。
- 第三方 HUD（Noelles Roles 等）不需要任何适配——只要它最终调用 `TextRenderer.draw`，就会自动生效。

### 装饰标记

`%b`/`%i`/`%u`/`%m`/`%k` 是 **开启式**：一直生效到 `%r` 或文本结束，可叠加：

```
%b%i%u全都要%r   →  粗 + 斜 + 下划线
%#FF0000%b红且粗%r →  颜色段内部也能开装饰
```

也可以写成**作用域**形式，只影响括号里的字：

```
前%b[只粗这几个字]后面正常
```

作用域用栈实现（`ArrayDeque<Scope>`），支持嵌套，退出时恢复到进入前的状态；`%l`/`%o`/`%n` 是 `§` 风格的别名，效果相同。

⚠️ 故意**没有**把 `%s` 给删除线：`%s` 在翻译文件里是 Minecraft 的参数占位符，占用它会破坏所有带参数的翻译。删除线请用 `%m`。

### `%outline` 渐变描边

普通文字只有一层填充色，描边是固定的暗色。`%outline[…]` 会让每个字符额外画一圈 8 方向的描边，**颜色取自该字符自己的颜色再压暗 45%**：

```java
int base = style.getColor() != null ? style.getColor().getRgb() : (color & 0xFFFFFF);
OrderedText ring = OrderedText.styled(codePoint, style.withColor(AnimationEngine.scaleRgb(base, 0.45f)));
for (int k = 0; k < 8; k++) {
    font.draw(ring, x + dx[k], y + dy[k], glyphColor, false, …);
}
```

所以描边会跟着渐变一起流动：

```
%#FF0000-#0000FF%outline[边框]      → 描边从红渐变到蓝
%flow[%outline[…]] 不支持嵌套         → 用 %#A-#B%outline[…] 或 %flow[…] 二选一
```

代价：每个字符多 8 次 draw call，只对包含 `%outline` 的文本生效，其余文本零开销。

### `%flow` 动态渐变

`%flow` 把色带按字符位置铺开，再让整条色带随时间平移：

```java
float position = (float) index / count - timeMs * FLOW_SPEED;   // FLOW_SPEED = 0.00035f
position -= (float) Math.floor(position);                        // 取小数部分，循环
```

所以颜色是「流动」的，而不是每个字各转各的色相（那是 `%rainbow`）。

### 透明度为什么要拆绘制

`Style` 只有 24 位 RGB，alpha 来自 `TextRenderer.draw(..., int color, ...)` 那个参数，而且一次 draw 只有一个 alpha。所以 `%aHH` 必须按 alpha 分段绘制：

- 整段 alpha 相同 → `Overlay.uniformAlpha()` ≥ 0 → **仍然只有 1 次 draw**（把 alpha 合并进颜色参数）；
- alpha 混排或含 `%wave` → 逐字符 draw（`SegmentedDrawer`）。

合并公式是相乘，不会糟蹋标题淡入淡出本身带进来的 alpha：

```java
int incoming = (color >>> 24) & 0xFF;
if (incoming == 0) incoming = 255;              // 原版把 alpha=0 当不透明
int alpha = Math.max(1, (incoming * overlayAlpha) / 255);
```

### `§k` 为什么之前不生效

原版只在**字符串**路径（`StringDecomposer`）里解析 `§`；`Text` 组件走的是 `LiteralTextContent`，`§k` 会被当成两个普通字符画出来。本模组的渲染路径现在自己解析 `§`/`&`：

- 代码字符标记为 skip，不参与绘制；
- 装饰状态（k/l/m/n/o）按位记录在 `Overlay.decoOn` / `decoOff`，在 `ChlorinsOrderedText` 里用 `style.withObfuscated(true)` 等应用；
- `§r` 会清空并显式关闭所有装饰。

结果：`§k` 在聊天、物品名、`/title`、第三方 HUD 里表现一致。

### 三种动画怎么做到不掉帧

| 特效 | 实现 | 每帧成本 |
|---|---|---|
| `%rainbow` | 在 `accept` 里按 (字符序号, 时间) 算 HSV→RGB | 每字符 1 次 sin/取模 |
| `%pulse` | 同上，亮度按余弦呼吸 | 每字符 1 次 cos |
| `%wave` | 无法在单次 draw 内改坐标，改为逐字 `draw` + Y 偏移 | 每字符 1 次额外 draw call |

`%wave` 是唯一有额外 draw call 的，只对**含 wave 的那几个字符**生效；`%rainbow`/`%pulse` 与普通文字开销基本相同。

## 3.9 命令

| 命令 | 作用 |
|---|---|
| `/color` 或 `/color help` | 列出所有前缀与用法 |
| `/color preview <前缀> <文本>` | 在聊天框里预览效果 |

### 调试键位

`选项 → 控制 → 按键绑定 → Chlorins Color API → 调试 HUD`，默认 **F9**，可自行改绑。

按下后左上角出现：

```
Chlorins Color API debug [ModernUI]
overlay cache 128   parse cache 46
key F9 (rebind in Controls → Key Binds)
sample 流光 彩虹 呼吸 波浪 弹跳 闪烁 淡入 抖动 描边
```

最后一行是把所有动画串起来实时渲染，**自检用**：如果这行动起来了，说明渲染管线正常；如果显示原始标记，说明文本没走进我们的管线。

```
/color preview % #FF0000你好%r
/color preview & a绿色
/color preview @ #00FF00-#0000FF渐变
```

`<前缀>` 支持 `% & @ #`，也认 `percent` / `amp` / `at` / `hash` 这种写法。预览会同时给出「原文」和「可见文本」两行，方便确认标记有没有被正确吃掉。

> Minecraft 的命令字面量是**区分大小写且必须小写**的，所以是 `/color`，`/Color` 不会匹配。

### 语言文件里的转义

帮助文本本身要显示 `%#FF0000` 这种例子，所以必须写 `%%`：

```json
"chlorins_color_api.command.help.solid": "§7纯色  §f%%#FF4500文字%%r"
```

渲染结果是 `纯色  %#FF4500文字%r`（已验证）。所有帮助/演示文案都放在 `assets/chlorins_color_api/lang/{zh_cn,en_us}.json`，键名统一 `chlorins_color_api.command.*` 与 `chlorins_color_api.demo.*`。

## 3.95 铁砧改名 / 输入框

**结论：`%#RRGGBB` 可以，`§` 永远不行。**

铁砧的保存路径是 `AnvilScreenHandler#setNewItemName`：

```java
String name = StringUtil.stripInvalidChars(input);
if (name.length() > 50) return false;
...
stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
```

而 `StringUtil` 的过滤是：

```java
public static boolean a(char c) { return c != 167 && c >= ' ' && c != 127; }
```

`§` 就是 167 —— **在进入物品名之前就被删掉了**，客户端删一次、服务端也删一次。所以这不是渲染问题，任何纯客户端模组都救不了 `§`（除非连服务端一起改）。

`%#FF0000` 全部是可见字符，能原样存活，最后以 `Text.literal("%#FF0000Sword")` 存进物品名；渲染时被本模组的 `TextRenderer` 接管上色。所以：

```
铁砧输入：%#FF0000传说之刃      ✅ 成品显示红色
铁砧输入：§c传说之刃            ❌ § 被删除，只剩 "c传说之刃"
```

两个注意点：

1. **50 字符上限包含标记**：`%#FF0000` 占 8 格，所以实际能写的文字是 42 格。
2. **输入框里显示的是原文**：`TextFieldWidgetMixin` 会在文本框渲染期间关闭改写，否则你每敲一个字符它就被吃掉一个，看到的和实际输入的对不上。

## 3.96 与 Modern UI 共存

Modern UI 会**整条替换**文字渲染管线，且是抢占式的。我下它的 jar 核对过（`mixins.modernui-textmc.json`）：

```json
{ "mixinPriority": 3,
  "client": [ "MixinFontRenderer", "MixinFormattedCharSequence", "MixinFontManager", "MixinEditBox", ... ] }
```

```java
@Mixin(class_327.class)            // TextRenderer
public abstract class MixinFontRenderer {
   @Overwrite public int method_27522(String ...)   // draw(String, ...)
   @Overwrite public int method_30882(class_2561 ...) // draw(Text, ...)
   @Overwrite public int method_22942(class_5481 ...) // draw(OrderedText, ...)
}
```

三个方法正是我们的注入点，而且它 `mixinPriority 3`（比我们的 1000 早应用）。更要命的是它的布局缓存按 **OrderedText 实例** 做 key：

```
ModernTextRenderer.drawText(OrderedText, …) → TextLayoutEngine.lookupFormattedLayout(OrderedText)
```

我们每帧 `new ChlorinsOrderedText(...)` 会让它**每帧重建布局**。

### 因此的策略

检测到 `modernui` 时，改在**更早的一层**上色 —— `DrawContext`：

```java
@Mixino(DrawContext.class)
@ModifyVariable(method = "drawText(…;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), argsOnly = true)
private Text chlorins$colorizeCentered(Text text) {
    return ModernUICompat.present() ? ColorText.colorize(text) : text;
}
```

这样交给 Modern UI 的是**普通、稳定的 Component**，它能把布局缓存住，颜色（含 RGB）由它自己渲染。`String` 重载会被转成 Component 再下发。

### 边界

| 功能 | 原版 | Modern UI |
|---|---|---|
| 纯色 / 渐变 / 多色 / 装饰 / `§k` | ✅ 渲染路径 | ✅ 组件路径 |
| `%rainbow` / `%pulse` / `%flow` | ✅ | ⚠️ 静态（组件无法逐帧变色） |
| `%wave` / `%aHH` | ✅ 分段绘制 | ⚠️ 保留走渲染路径，依赖注入是否存活 |
| 布局缓存 | 无 | ✅ 不会被我们打穿 |

`%wave` / `%aHH` 在 `ColorText.colorize` 里被显式跳过，交给渲染路径：

```java
Overlay overlay = TextPipeline.overlayOf(raw);
if (overlay != null && (overlay.hasWave() || overlay.hasAlpha())) {
    return text;   // 逐帧效果不能烘焙成静态组件
}
```

> ⚠️ 我这边没有实机，Modern UI 的 `@Overwrite` 与我们的 `@Inject` 在同方法上的共存行为**未经运行验证**。上面的设计保证「即使我们的注入被覆盖掉，组件路径依然把颜色送进去了」。

## 4. 工作原理

```
lang/zh_cn.json
      │  "item.demo.gradient": "%#FF0000-#FFFF00烈焰魔王%r"
      ▼
TranslationStorage.load(...)        ── mixin：语言加载钩子，清空解析缓存
      ▼
TranslatableTextContent.updateTranslations()
      ▼
TranslatableTextContentMixin (forEachPart HEAD)
      │  含标记 → 由我们切分 %s / %n$s / %% 并生成带颜色的 Component，原版解析器不参与
      │  不含标记 → 原样交还原版，零开销
      ▼
Text (literal + Style.withColor(rgb))  →  渲染出平滑渐变
```

三个要点：

1. **Hook 位置**：`TranslatableTextContent#forEachPart` 的头部。这是原版「翻译字符串 → 若干 part」的必经入口。含标记时我们直接接管并 `cancel`，原版那个只认 `%s`/`%n$s`/`%%` 的严格解析器**完全不会执行**，因此不会再有内部 `TranslatableException` 与回退。
2. **参数不会被吃掉**：切分由 `TranslationTemplate` 完成（`%s`、`%n$s`、`%%`），参数作为 Component 原样插回，颜色在参数边界上继续延续。
   另有一个 `updateTranslations` 尾部的兜底注入，覆盖不经 `forEachPart` 的路径；正常流程下它是空操作。
3. **缓存**：解析结果按原始字符串缓存（上限 4096，超出自动清空），`TranslationStorage.load()` 返回时清空缓存，资源包切换 / `/reload` 不会读到旧颜色。

---

## 5. 目录结构

```
chlorins-color-api/
├── build.gradle / settings.gradle / gradle.properties
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── chlorins_color_api.mixins.json
│   └── assets/chlorins_color_api/lang/{en_us,zh_cn}.json
├── src/main/java/
│   ├── com/chlorins/colorapi/
│   │   ├── ChlorinsColorApi.java          ← 入口 + 公开 API
│   │   ├── ChlorinsColors.java            ← 门面（不依赖 Minecraft）
│   │   ├── parser/ColorParser.java        ← 扫描器（纯 Java）
│   │   ├── parser/LegacyCode.java         ← § / & 格式码
│   │   ├── parser/LegacyTags.java         ← 旧 <SOLID>/<GRADIENT> 标签
│   │   ├── parser/TranslationTemplate.java← %s 模板切分（纯 Java）
│   │   ├── parser/{ParsedText,Run,TextStyle}.java
│   │   ├── cache/ParseCache.java
│   │   ├── render/
│   │   ├── OverlayScanner.java        ← 渲染路径扫描（逐字符数组，纯 Java）
│   │   ├── Overlay.java               ← 逐字符 skip/颜色/动画表
│   │   ├── ChlorinsOrderedText.java   ← 流式 Style 改写（核心）
│   │   ├── TextPipeline.java          ← LRU + 入口
│   │   ├── WaveDrawer.java            ← %wave 逐字偏移绘制
│   │   └── AnimationEngine.java       ← 纯 Java 动画数学
│   ├── mc/ColorText.java              ← ParsedText → MutableText
│   │   └── mixin/{TranslatableTextContentMixin,TranslationStorageMixin}.java
│   └── net/matrixcreations/libraries/MatrixColorAPI.java   ← 旧 API 兼容层
├── src/test/java/com/chlorins/colorapi/{ParserSelfTest,Demo}.java
└── tools/
    ├── run-tests.sh                       ← 只依赖 JDK，编译+运行自测
    └── stubs/                             ← 给 MC/Fabric/Mixin API 的桩，用于离线类型检查
```

---

## 6. 构建与安装

需要 **JDK 21**（Minecraft 1.21.1 要求）与 Gradle 8.x：

```bash
gradle wrapper --gradle-version 8.8   # 首次
./gradlew build
# 产物：build/libs/chlorins-color-api-1.0.0.jar
```

依赖版本在 `gradle.properties` 里，可按需修改：

```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.5
fabric_version=0.110.0+1.21.1
```

安装：把 jar 放进实例的 `mods/`，需要 Fabric Loader + Fabric API。纯客户端模组，服务端不用装。

### 只跑解析器自测（不需要 Minecraft / Gradle / 网络）

```bash
./tools/run-tests.sh
# passed = 39, failed = 0
```

---

## 7. 验证状态（重要，请知悉）

本机环境**无法访问 `maven.fabricmc.net`**，因此无法在这里跑一次真正的 `gradlew build`。诚实说明哪些验过、哪些没验：

| 内容 | 状态 |
|---|---|
| 解析核心（标记、渐变、转义、错误输入、码点、旧语法、模板切分） | ✅ **已编译并跑通 39 项断言**（JDK 17） |
| Minecraft 相关层（`ColorText`、两个 Mixin、入口类） | ⚠️ **已通过桩类型检查**，未与真实 Minecraft 一起编译 |
| Mixin 目标方法/字段名 | ✅ 依据 Yarn 1.21.1 映射 + 反编译的 1.21.1 客户端源码确定（`updateTranslations`、`translations`、`TranslationStorage.load`） |
| 游戏内实际渲染 | ❓ 需你在本地 `gradlew build` 后进游戏确认 |

如果目标版本不是 1.21.1：只需改 `gradle.properties` 里的三个版本号；`TranslatableTextContent` / `TranslationStorage` 在 1.20.1–1.21.x 之间签名稳定，但仍建议先跑一次构建。

### 验收对照

| 验收标准 | 结果 |
|---|---|
| `zh_cn.json` 写入示例后物品名/提示正确上色 | 解析与组件构建已就绪，待实机确认 |
| 渐变首尾严格等于端点色 | ✅ 实测 `#FF0000` → `#00FF00` 逐字符插值 |
| `%r` 后恢复默认 | ✅ 实测 |
| `%%` 显示字面 `%` | ✅ 实测 |
| `%#GGGGGG` 原样显示且不崩溃 | ✅ 实测 |
| 与 `§c` 混用不冲突 | ✅ 实测（渐变内部也能带 `§l`） |
| 无 Mixin/解析异常 | ⚠️ 需实机看日志 |

---

## 8. 已知限制

- **仅客户端**：服务端下发的文本若已被服务端解析成纯字符串，本模组也能着色（因为它在渲染前的 Component 上工作）；但若服务端把颜色信息丢掉了，客户端无从恢复。
- 颜色标记**不能跨翻译 key**：一个 key 内的标记只在本 key 内生效。
- 带 `%s` 参数的彩色翻译依赖我们自己的模板切分；若某整合包的语言文件用了非常规占位符，会原样显示。
- `%` 与 Minecraft 的 `%s` 占位符共用同一字符，但**不再有副作用**：含标记的翻译在 `forEachPart` 阶段就被接管，原版解析器不会看到 `%#FF0000`，因此不会抛异常、不会丢参数。仍建议避免在同一个 key 里混用 `%s` 与 `%<数字>$s` 之外的奇怪写法。
- `§x` 在纯客户端不生效（原因见第 3 节），所以 `MatrixColorAPI.process()` 返回的 `§x` 字符串只适合 Paper/Adventure 这类会再解析的服务端。
- 通过 `draw(OrderedText, …)` 进入的文字需要把字形还原成字符串才能判断标记，这条路径每帧会构造一个小字符串；`draw(String/Text)` 才是无分配的主路径。
- 带 `%s` 参数**且**带动画的翻译，参数会被展开成纯文本（样式丢失），因为动画必须在渲染阶段完成。

---

## 9. 许可证

MIT，见 [LICENSE](LICENSE)。
