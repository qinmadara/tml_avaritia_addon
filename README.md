[English](README_EN.md) | [简体中文](README.md)
# 车万女仆：无尽贪婪

![icon](src/main/resources/icon.png)

> 一个让车万女仆（Touhou Little Maid）能够真正使用无尽贪婪：重生（Re:Avaritia）武器的联动附属模组。

| 项 | 值 |
|---|---|
| 当前版本 | 0.1.4 |
| 加载器 | NeoForge 1.21.1（21.1.248+） |
| Java | 21 |
| 许可 | MIT |

---

## 功能特性 Features

### 近战 Melee
- **无尽之剑 Infinity Sword**：触发无尽剑特效（秒杀 + AOE）；TLM 自带攻击任务不触发本模组特效。
### 远程 Ranged
- **无尽之弓 Infinity Bow**：无需背包箭矢即可射击；默认射**天界箭**，开启**追踪模式**后射**追踪箭**。
- **无尽之弩 Infinity Crossbow**：默认单发**天界箭**，开启**多重射击模式**后一次发射 **5 发扇形箭**。
- **无尽三叉戟 Infinity Trident**：投掷无尽三叉戟并触发**落雷**。

> 各无尽武器任务只接受对应的无尽系列武器；手持普通弓/弩/三叉戟时这些任务不会攻击。TLM 自带的弓/弩/三叉戟任务也不会触发本模组特效。

### 武器模式切换 GUI
- 弓/弩专属「武器攻击模式切换」任务设置界面，用于切换追踪/多重射击模式。
- 模式数据写入武器 NBT `{mode:{...}}`，与原版无尽贪婪的模式系统一致。

### 无尽护甲·女仆被动适配
- **全套**：穿戴全套无尽护甲时完全免伤（一切伤害均被取消，含无尽武器伤害）
- **头盔**：夜视（可配置）+ 水下呼吸
- **胸甲**：持续清除负面效果
- **护腿**：防火
- **靴子**：台阶高度 1.0625（可走上 1 格）+ 移速加成（`MOVEMENT_SPEED` 属性修饰符实现，穿脱即时生效、不干扰女仆 AI 寻路）
- 移速 / 游泳 / 冲刺倍率均可配置

---

## 前置需求 Requirements

| 项 Item | 版本 Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248+ |
| Java | 21 |
| Touhou Little Maid 车万女仆 | 1.5.3（NeoForge 1.21.1） |
| Re:Avaritia 无尽贪婪：重生 | 1.4.1（1.21.1） |

> 客户端与服务端均需安装本模组及两个前置。**缺少任一前置时，NeoForge 会在启动阶段直接报错并拒绝加载本模组。**

> **关于无尽贪婪的兼容**：本模组**仅针对「无尽贪婪：重生」这一分支适配**，不代表能兼容其他无尽贪婪分支。

---

## 加载器与版本兼容性 Loader & Version Compatibility

| 加载器 Loader | 状态 Status |
|---|---|
| NeoForge 1.21.1（21.1.248+） | ✅ 完整支持（本分支的开发与实测环境） |
| Forge | ❌ 不支持（Forge 无 1.21.1 分支；1.20.1 版本见旧版发布） |

---

## 安装 Installation

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.248+（见「加载器与版本兼容性」）
2. 安装前置：Touhou Little Maid 1.5.3（NeoForge 1.21.1）、Re:Avaritia 1.4.1+（1.21.1）
3. 将本模组 jar 放入 `mods/` 文件夹
4. 启动游戏

---

## 配置 Configuration

配置文件位于 `config/tml_avaritia_addon-common.toml`，主要选项：

| 键 Key | 默认 Default | 说明 Description |
|---|---|---|
| `maid_sword_aoe` | `true` | 无尽剑每次攻击是否附带 Re-Avaritia 右键 AOE（默认开） |
| `force_kill_mode` | `false` | 强制女仆无尽剑 AOE 进入杀戮模式（默认关，开启后 AOE 不分敌我） |
| `infinity_armor_night_vision` | `true` | 无尽护甲头盔：夜视 |
| `boot_speed_base` | `0.1` | 无尽靴子移速（≈ 使女仆基础移速 ×2，与玩家穿靴同倍率） |
| `boot_speed_swimming_multiplier` | `1.2` | 靴子游泳倍率 |
| `boot_speed_sprinting_multiplier` | `0.2` | 靴子冲刺倍率（TLM 女仆 AI 本身不冲刺，默认不触发） |

> `boot_speed_flying/sneaking/backward/strafing_multiplier` 为与 Re-Avaritia 配置对齐的项，当前未使用。

---

## 从源码构建 Building

环境要求：JDK 21

```bash
# 前置依赖默认从 Modrinth Maven 自动下载，无需手动准备 jar；
# 如需使用本地 jar，用 ./gradlew -PuseLocalDeps build，并把两个前置 jar 放入 libs/
# （libs/ 已 gitignore 不提交）

./gradlew build        # 构建产物在 build/libs/TML_Avaritia_Addon-<版本>-MC1.21.1+NeoForge.jar
./gradlew runClient    # 启动开发客户端
./gradlew runServer    # 启动开发服务端
```

---

## 已知限制 Known Limitations

- **三叉戟投出后不回归**：女仆投出的无尽三叉戟不会像玩家那样自动回收（`loyalty` 对女仆无效），命中后插地。
- **部分玩家专属特效对女仆不适用**（如三叉戟潮涌模式等）。
- **冲刺倍率已接入逻辑**，但 TLM 女仆 AI 本身不会设置冲刺，因此该倍率默认处于休眠状态。

---

## 许可与致谢 License & Credits

本模组代码以 **MIT License** 发布，Copyright © 2026 qinmadara。详见 [LICENSE](LICENSE)。

本模组依赖并参考了以下开源项目，特此致谢：

| 项目 | 许可 | 版权 |
|---|---|---|
| [Touhou Little Maid](https://github.com/tartaricacid/TouhouLittleMaid) | 代码 MIT / 资源 CC BY-NC-SA 4.0 | © tartaric_acid |
| [Re:Avaritia](https://github.com/Nova-Committee/Re-Avaritia) | 代码 MIT / 资源 CC BY-NC-SA 4.0 | © cnlimiter（Nova Committee） |

- 本模组仅编译期/运行期引用上述模组的公共 API，**未打包其任何代码或资源**。
- 本模组为**非官方附属模组**，与 Touhou Little Maid、Re:Avaritia 的作者无任何关联。

---

## AI 使用声明 AI Disclosure

- 本模组在开发过程中**大幅使用 AI 辅助**，包括：代码生成与重构、Bug 排查、代码审查、文档（含本 README）编写等。
- 所有 AI 产出的代码均经过**人工审阅、修改与游戏内实测**后整合，最终代码以 MIT 许可发布。
- 使用 AI 的目的在于提升开发效率；代码质量、功能正确性与许可合规均由项目作者负责。