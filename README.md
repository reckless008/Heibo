<div align="center">

<img src="favicon.png" width="96" height="96" alt="Heibo" />

# Heibo

**第三方微博客户端** · Kotlin + Jetpack Compose · UI 基于 COUI（ColorOS 17 设计语言）

> 本仓库基于 [Costben/COBO](https://github.com/Costben/COBO) 二次开发，应用更名为 **Heibo**，
> 在原作完整的微博功能之上重做了整套 UI 交互。原作的构建方式、功能与文档全部保留，见下文。

[![Release](https://img.shields.io/github/v/release/reckless008/Heibo?label=release&color=1f6feb)](https://github.com/reckless008/Heibo/releases/latest)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-1.11-4285F4?logo=jetpackcompose&logoColor=white)
![COUI](https://img.shields.io/badge/UI-COUI%20%C2%B7%20ColorOS%2017-111111)

</div>

<div align="center">
  <img src="docs/images/heibo-feed.jpg" width="18%" alt="信息流" />
  <img src="docs/images/heibo-detail.jpg" width="18%" alt="详情评论" />
  <img src="docs/images/heibo-share.jpg" width="18%" alt="分享面板" />
  <img src="docs/images/heibo-settings.jpg" width="18%" alt="设置" />
  <img src="docs/images/heibo-profile.jpg" width="18%" alt="个人主页" />
</div>

<div align="center"><sub>信息流 · 详情评论 · 分享面板 · 设置 · 个人主页</sub></div>

---

## 相比原仓库的改动

在 [Costben/COBO](https://github.com/Costben/COBO) 完整功能的基础上，围绕 **液态玻璃（Liquid Glass）**
设计语言重做了 UI 与交互，并同步了原仓库 1.0.21 的新功能（微博投票、分类胶囊动效）。

**视觉**

- 顶栏改为单行悬浮件：搜索（左）· 分段胶囊（中）· 发布（右），去掉标题文字与上滑时的大面积模糊带
- 底部导航改为液态玻璃悬浮胶囊，去掉刷新 / 发布圆钮（刷新走下拉，发布移到顶栏）
- 微博卡片动作（转发 / 评论 / 点赞）改为居中的玻璃小胶囊，固定尺寸、两端对齐图片边缘
- 全套玻璃材质支持「玻璃透明度」调节（轻透磨砂 ↔ 柔光玻璃），亮暗双套基准浓度
- 强调蓝整体调灰，话题 / @ / 链接去下划线，黑白正文上更协调

**信息流与详情**

- 卡片信息整合：时间 / 机型 / IP 合并到昵称下方；时间规则为「刚刚 / X分钟前 / 今天 18:43」
- 展开动画保持原样，返回动画重做（识别按钮返回的瞬移路径、补全被截短的行程）
- 评论区：头像 / 昵称可点进主页；整条评论可点回复；楼中楼去掉缩小的回复按钮，点赞按钮统一尺寸、右对齐并按层级淡化
- 「全部」分类两段式：先选中、再点才弹筛选菜单

**功能**

- 分享面板：复制全文 / 复制链接 / 生成图片（含头像、昵称、原文与二维码）/ 私信好友（按关注头像选人）/ 系统分享
- 图片浏览器支持下滑退出
- 设置新增：外观模式三选一（跟随系统 / 浅色 / 深色）、字号滑杆、玻璃透明度
- 「我」页标题随滚动上滑隐藏；分享卡片与相册目录品牌更新为 Heibo

**品牌**

- 新图标（黑柴犬）与应用名 **Heibo**，主题图标（单色）同步适配

## 特性

- **信息流**：推荐（`containerid=102803`）、关注（`feed/friends`）、好友圈（`feed/circle`），`since_id` / `max_id` 分页，下拉刷新
- **微博卡片**：正文富文本（@ / 话题 / 链接 / 表情）、九宫格图片、视频、转发块、来源与时间
- **详情页**：正文、评论（热度 / 时间双排序）、点赞、评论、转发
- **发布**：文字 + 多图上传、@ / 话题 / 表情工具条、草稿箱、编辑历史
- **图片**：查看器、原图加载、长按玻璃菜单保存（菜单从按下点展开、收回按下点）
- **主页与关系**：个人主页、用户微博、关注 / 粉丝
- **搜索**：热搜、关键词搜索、话题页
- **消息**：私信列表、通知
- **本地数据**：浏览历史、草稿、多账号、设置（深色模式 / 字号）
- **登录**：全屏官方网页登录，自动抓取并保存 Cookie

## 技术栈

| 层 | 选型 |
| :--- | :--- |
| 语言 / UI | Kotlin 2.4 · Jetpack Compose 1.11 |
| 设计系统 | [COUI](https://github.com/suqi8/coui)（ColorOS 17 组件库，本地快照） |
| 构建 | AGP 9.3.1 · Gradle 9.8.2 · JDK 21 |
| 网络 | OkHttp 4.12 |
| 图片 | Coil 3.6 |
| 视频 | Media3 ExoPlayer 1.11 |
| 数据层 | `m.weibo.cn` Web API（Cookie 复用） |

## 工程信息

| 项 | 值 |
| :--- | :--- |
| applicationId | `com.cobo.app` |
| 应用名 | Heibo |
| 版本 | 1.0.0（versionCode 10000） |
| minSdk / targetSdk | 26 / 37 |
| 工程 | `frontend/`（独立 Gradle 工程） |

## 目录

```
Cobo COUI/
├── frontend/                     # Android 应用（com.cobo.app）
│   ├── app/                      # 应用模块
│   │   └── src/main/kotlin/com/cobo/app/
│   │       ├── data/             # API、模型、缓存、设置
│   │       ├── screen/           # 页面
│   │       └── ui/               # 导航、主题、组件
│   ├── scripts/publish-coui.sh   # 发布本地 COUI 到本地 Maven 仓库
│   ├── scripts/e2e.sh            # 真机端到端测试
│   └── gradlew                   # Gradle 9.8.2
├── docs/                         # 设计文档与截图
└── README.md
```

## 构建

```bash
cd frontend
./scripts/publish-coui.sh          # 首次构建前：把 COUI 快照发布到本地 Maven 仓库
./gradlew :app:assembleRelease     # 产物：app/build/outputs/apk/release/app-release.apk
```

`settings.gradle.kts` 从同级目录 `../../coui/build/repository/local` 解析 COUI，构建前先确保该仓库已 `publishToMavenLocal`。

## 安装

```bash
adb install -r --no-incremental app/build/outputs/apk/release/app-release.apk
```

## 测试

```bash
cd frontend
./scripts/e2e.sh                   # 真机端到端测试（需已登录）
```

## 文档

| 文档 | 内容 |
| :--- | :--- |
| [`01-微博API.md`](docs/01-微博API.md) | Host、公共参数、接口目录、上传、返回结构与错误码 |
| [`02-技术方案.md`](docs/02-技术方案.md) | 技术栈、模块拆分、数据模型、里程碑、关键实现 |
| [`03-功能清单.md`](docs/03-功能清单.md) | 已实现功能与对应接口、已知边界 |
| [`04-前端架构.md`](docs/04-前端架构.md) | COUI 主题、导航、页面清单与实测记录 |
| [`05-端到端测试.md`](docs/05-端到端测试.md) | E2E 套件、覆盖范围与运行方式 |

## 说明

第三方客户端，与微博官方无关，仅个人使用。
