# CMAL — Citrine Minecraft Android Launcher

[![Build](https://github.com/cszy-app/CMAL/actions/workflows/build.yml/badge.svg)](https://github.com/cszy-app/CMAL/actions/workflows/build.yml)

**CMAL**（Citrine Minecraft Android Launcher）是一个面向 Android 的 Minecraft Bedrock 启动器，
由 **cszy-app** 开发。

> 本项目为 **非官方** 工具，与 Mojang / Microsoft 无任何关联。
> Minecraft 是 Mojang AB 的商标，所有游戏素材版权归其所有者所有。

---

## 功能特性

- 🚀 **官方版安装 + 一键启动**：下载官方 Minecraft Bedrock APK，标准流程安装后直接启动
- 📦 **多版本下载与切换**：多线程分片下载（断点续传），多个版本随意切换
- ⚡ **极限并发下载**：单文件分片 + 多文件并行，速度拉满
- 🎨 **资源包 / 行为包**：导入 `.mcpack` / `.mcaddon` / `.mcworld`
- 🧍 **皮肤管理**：本地皮肤 + 联网皮肤库
- 🌍 **世界管理**：存档浏览、导入、导出、删除
- 🖥 **服务器列表**：自定义服务器 + 内置精选服务器，一键加入
- 💾 **备份 / 恢复**：数据库一键备份
- 🔄 **应用自更新**：GitHub Releases 检查更新
- 🌐 **中英双语**：跟随系统 / 手动切换
- 🎨 **Material 3**：动态取色 + 自定义主题色

## 界面语言

| 中文 | English |
|------|---------|
| 首页 | Home |
| 下载 | Downloads |
| 资源 | Resources |
| 服务器 | Servers |
| 我的 | Mine |

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material 3）
- **MVVM** 架构
- **Room** 本地数据库 + SharedPreferences
- **OkHttp** 多线程分片下载（Range 断点续传）
- **Coil** 图片加载
- **GitHub Actions** 云端构建

## 项目结构

```
CMAL/
├── app/
│   ├── build.gradle.kts          # 应用构建配置（签名/混淆/ABI）
│   ├── proguard-rules.pro        # R8 混淆规则
│   └── src/
│       ├── main/
│       │   ├── java/com/cszyapp/cmal/
│       │   │   ├── CMalApp.kt        # Application + DI 容器
│       │   │   ├── MainActivity.kt   # 主 Activity（处理打开/分享）
│       │   │   ├── data/             # 数据层（Room/下载/安装/更新/源）
│       │   │   │   ├── download/     # 多线程下载引擎
│       │   │   │   ├── install/      # 安装管理（FileProvider）
│       │   │   │   ├── repo/         # Repository
│       │   │   │   ├── source/       # 下载源管理
│       │   │   │   └── update/       # 更新检查
│       │   │   └── ui/               # UI 层（Compose）
│       │   │       ├── navigation/   # 底部导航
│       │   │       ├── home/         # 首页
│       │   │       ├── download/     # 下载页
│       │   │       ├── resources/    # 资源页
│       │   │       ├── servers/      # 服务器页
│       │   │       ├── profile/      # 我的页
│       │   │       ├── onboarding/   # 引导页
│       │   │       └── imports/      # 文件导入
│       │   └── res/                  # 资源（中英 strings、图标、主题）
│       └── test/                     # 单元测试
├── gradle/
│   └── libs.versions.toml        # 版本目录
├── .github/workflows/build.yml   # GitHub Actions 云构建
├── mc_versions.json              # 版本索引示例
├── CHANGELOG.md
└── README.md
```

## 快速开始（开发者）

### 1. 本机构建

需要 JDK 17、Android SDK（Android Studio）。

```bash
gradle wrapper
./gradlew :app:assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

### 2. 云端构建（推荐，无需本机环境）

项目已配置 **GitHub Actions**，push 到 `main` 分支后自动出包：

1. 将仓库推送到 GitHub
2. 在仓库 **Settings → Secrets and variables → Actions** 添加以下 Secrets：
   - `KEYSTORE_BASE64`：keystore 文件的 Base64 编码
   - `KEYSTORE_PASSWORD`：keystore 密码
   - `KEY_ALIAS`：密钥别名
   - `KEY_PASSWORD`：密钥密码
3. 前往 **Actions** 标签页查看构建，完成后在 Artifacts 下载 APK

> **未配置 Secrets 时**：自动使用 debug 签名构建，可正常安装测试。

### 3. 生成正式签名 keystore

需要 JDK 环境（`keytool`）：

```bash
keytool -genkeypair -v \
  -keystore cmal-release.jks \
  -alias cmal \
  -keyalg RSA -keysize 2048 -validity 10000

# 转 Base64（Linux/macOS）
base64 -w 0 cmal-release.jks

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cmal-release.jks"))
```

### 4. 配置 Minecraft 版本源

版本索引来自 `mc_versions.json`（默认指向本仓库）。格式：

```json
[
  {
    "name": "1.21.40",
    "code": 10000000,
    "url": "https://example.com/minecraft-1.21.40.apk",
    "size": 1048576
  }
]
```

- `name`：版本名（也作为下载文件名）
- `code`：唯一版本号（建议用日期/序号）
- `url`：APK 直链（需支持 Range 请求以启用分片下载）
- `size`：字节大小

可在应用内「我的 → 下载源」添加自定义源。

## 版权与免责声明

- 本应用不内置、不传播盗版游戏内容。
- Minecraft 游戏本体为 Mojang / Microsoft 版权产品，请使用官方渠道购买安装。
- 本工具仅供学习交流，请遵守当地法律法规。

## 许可

```
Copyright (c) 2026 cszy-app

本项目仅供个人学习交流使用。
未经允许，请勿将本项目用于商业用途。
```

## 联系方式

- 开发者：cszy-app
- 项目主页：https://github.com/cszy-app/CMAL
