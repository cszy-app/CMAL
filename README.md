# CMAL — Citrine Minecraft Android Launcher

[![Build](https://github.com/cszy-app/CMAL/actions/workflows/build.yml/badge.svg)](https://github.com/cszy-app/CMAL/actions/workflows/build.yml)

**CMAL**（Citrine Minecraft Android Launcher）是一个面向 Android 的 Minecraft Bedrock 启动器，
由 **cszy-app** 开发。

> 本项目为 **非官方** 工具，与 Mojang / Microsoft 无任何关联。
> Minecraft 是 Mojang AB 的商标，所有游戏素材版权归其所有者所有。

---

## 功能特性

- **官方版安装 + 一键启动**：从本地 APK 安装官方 Minecraft Bedrock，标准流程安装后直接启动
- **Xbox 登录**：内置 Microsoft 账户授权（设备码流），登录后管理 Xbox 账户
- **资源包 / 行为包**：导入 `.mcpack` / `.mcaddon` / `.mcworld`
- **皮肤管理**：本地皮肤 + 联网皮肤库
- **世界管理**：存档浏览、导入、导出、删除
- **服务器列表**：自定义服务器 + 内置精选服务器，一键加入
- **备份 / 恢复**：数据库一键备份
- **应用自更新**：GitHub Releases 检查更新
- **中英双语**：跟随系统 / 手动切换
- **Material 3**：动态取色 + 自定义主题色

## 界面语言

| 中文 | English |
|------|---------|
| 首页 | Home |
| 本地APK | Local APK |
| 资源 | Resources |
| 服务器 | Servers |
| 我的 | Mine |

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material 3）
- **MVVM** 架构
- **Room** 本地数据库 + SharedPreferences
- **OkHttp** 网络请求（Xbox 授权 / 更新检查）
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
│       │   │   ├── data/             # 数据层（Room/安装/更新/Xbox）
│       │   │   │   ├── install/      # 安装管理（FileProvider）
│       │   │   │   ├── repo/         # Repository
│       │   │   │   ├── update/       # 更新检查
│       │   │   │   └── xbox/         # Xbox 登录（设备码流）
│       │   │   └── ui/               # UI 层（Compose）
│       │   │       ├── navigation/   # 底部导航
│       │   │       ├── home/         # 首页
│       │   │       ├── localapk/     # 本地 APK 安装页
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

### 4. 安装 Minecraft（用户自备 APK）

CMAL **不分发 Minecraft APK**。请从官方渠道（如 Google Play）购买并获取 APK 文件：

1. 将 Minecraft APK 文件放到设备上（可通过浏览器下载或从其他设备传输）
2. 打开 CMAL → 「本地APK」页 → 选择 APK 文件 → 系统完成安装
3. 返回「首页」即可一键启动

### 5. 配置 Xbox 登录

CMAL 使用 Microsoft OAuth 设备码流登录 Xbox：

1. 在 [Azure 门户](https://portal.azure.com/) 注册一个应用，开启「允许公共客户端流」
2. 将应用的 **Client ID** 填入 `app/src/main/java/com/cszyapp/cmal/data/xbox/XboxAuthManager.kt` 的 `CLIENT_ID` 常量
3. 在应用「我的」页点击 Xbox 登录，按提示在浏览器中确认即可

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
