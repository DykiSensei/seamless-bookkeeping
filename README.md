# 无感记账（Seamless Bookkeeping）

一款面向中国大陆用户的 Android 自动记账 App。核心目标是**不用手动输入每一笔账** —— 当你支付宝/微信付款，或银行卡刷卡时，App 在后台读取系统通知和短信，自动把交易记到账本里。

> ⚠️ **状态**：MVP 阶段。功能完整可用，但解析规则覆盖率约 80%，部分边角场景（尤其是微信扫码收款 + vivo 系统的无障碍/通知拦截）暂未支持，详见 [已知限制](#-已知限制) 章节。

## ✨ 已实现功能

### 自动抓账
- 🟢 **支付宝通知**：抓取付款成功 / 收款 / 退款 / "你有一笔 X 元的支出"
- 🟢 **微信支付通知**：抓取转账（"向你/您付款"为收入；"向 商户 付款"为支出）
- 🟢 **银行交易短信**：覆盖工/招/建/中/农/交/邮储/中信/光大/民生/浦发/平安/广发/兴业/华夏等 15+ 主流银行；自动识别银行名、卡尾号、金额、收/支方向
- 🟡 **无障碍兜底**（实验性）：识别微信红包 / 微信转账 / 支付宝转账入账等通知文本不含金额的场景；用户主动开启，仅在微信、支付宝两个 app 内激活。**在 vivo OriginOS 上验证为系统级拦截不可用**（详见已知限制）
- 🔁 **自动去重**：60 秒内同来源同金额的交易视为重复，跳过入库（覆盖通知 + 短信 + 无障碍多路径抓到同一笔）

### 账本管理
- 📋 **账单列表**：按日期分组（今天 / 昨天 / 06月15日），每日合计显示
- 🏷️ **分类筛选**：横向滚动 chip，单击切换某分类查看
- ✍️ **手动记账**：自动抓不到的场景（如现金交易）一键添加
- 🗑️ **删除**：错抓的或测试数据一键清掉

### 智能分类
- 🧠 100+ 关键词规则，覆盖 10 个分类：餐饮 / 交通 / 购物 / 娱乐 / 住房 / 水电 / 医疗 / 教育 / 工资 / 转账
- 例如：商户名含「星巴克 / 麦当劳 / 美团 / 饿了么」自动归为「餐饮」；含「滴滴 / 12306 / 中石化」归为「交通」

### 统计与可视化
- 📊 **本月统计页**：支出 / 收入切换
- 🥧 **分类占比饼图**：Canvas 自绘，每个分类一个固定色
- 📈 **每日趋势柱状图**：当月每日支出/收入柱状对比
- 📑 **分类明细**：金额 + 占比 + 笔数

### 预算管理
- 💰 **月总预算 + 分类预算**
- 📉 **使用进度**：< 80% 绿、80-100% 橙、> 100% 红
- ⏳ **剩余天数提示**

### 设置 & 后台保活
- 🛡️ **常驻通知保活**（默认关闭）：开启后挂一条前台服务通知，显著降低国产 ROM 杀进程导致通知监听掉线的概率
- 📲 **权限授权一键跳转**：通知使用权 / 短信权限 / 无障碍 都有引导按钮
- 📋 **国产 ROM 后台白名单指南**：内置小米/华为/OPPO/vivo/三星/原生 6 家系统的具体设置路径，免去用户自己摸索

### 数据安全
- 🔒 **完全本地存储**：所有交易数据存在本机 SQLite，无任何云端上传
- 🚫 **不联网**：App 本身不需要网络权限（如果未来加云同步会另外申请）
- 🚷 **排除自动备份**：DB 文件已在 `backup_rules.xml` 显式排除，不会被 Google Drive 备份或迁移到新设备

## 🛠 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose（Material 3，BOM 2024.10.01）|
| 架构 | MVVM + Repository |
| 数据库 | Room 2.6.1（KSP 编译） |
| 依赖注入 | Hilt 2.52 |
| 异步 | Coroutines 1.9 + Flow |
| 偏好存储 | DataStore Preferences 1.1 |
| 图表 | Compose Canvas 自绘（不依赖图表库） |
| 构建 | AGP 8.7.3 + Gradle 8.9 + Java 17 |
| 最低 Android 版本 | API 24 (Android 7.0) |
| 目标 SDK | API 35 (Android 15) |

## 📂 项目结构

```
app/src/main/java/com/bookkeeping/app/
├── BookkeepingApplication.kt       # Hilt entry point
├── MainActivity.kt                 # 处理 POST_NOTIFICATIONS 运行时申请 + 启动保活
├── data/
│   ├── budget/                     # 预算（DataStore）
│   ├── settings/                   # 全局设置（前台保活开关等）
│   ├── local/                      # Room DB / DAO / Entity
│   └── repository/                 # 业务层（含 60s 同源同金额去重）
├── di/
│   └── DatabaseModule.kt           # Hilt 模块
├── notification/                   # 自动抓取核心
│   ├── PaymentNotificationListenerService.kt  # 通知监听 Service
│   ├── NotificationParser.kt                  # 支付宝/微信解析
│   ├── SmsBroadcastReceiver.kt                # SMS 广播接收
│   ├── SmsParser.kt                           # 银行短信解析
│   ├── CategoryClassifier.kt                  # 自动分类
│   ├── KeepAliveService.kt                    # 前台保活服务（默认关）
│   └── accessibility/                         # 无障碍兜底（实验性）
│       ├── AccessibilityCaptureService.kt
│       ├── AlipayTransferExtractor.kt
│       ├── WechatTransferExtractor.kt
│       └── WechatRedPacketExtractor.kt
└── ui/
    ├── navigation/RootNavigation.kt           # 底部 4-tab NavHost
    └── screen/
        ├── transactions/                      # 账单 tab
        ├── stats/                             # 统计 tab
        ├── budget/                            # 预算 tab
        └── settings/                          # 设置 tab（权限引导 + ROM 指南）
```

## ⚠️ 已知限制

### 1. 微信扫码收款抓不到

**仍是当前最大的功能缺口**。微信从 2020 年起停止为扫码收款发系统通知（改为在 app 内提示），所以 `NotificationListenerService` 抓不到。

**影响范围**：
- ❌ 别人扫你的收款码付钱给你 → 抓不到
- ❌ 商家用收款码收你扫码的钱（部分场景）→ 抓不到
- ✅ 微信转账（"向你付款"）→ 能抓
- ✅ 微信红包到账（部分版本）→ 能抓

**临时方案**：手动补录。本项目实现了无障碍兜底（见下条），但 vivo 上不可用。

### 2. vivo OriginOS 上无障碍兜底不工作

经实测（vivo OriginOS / Android 14+），系统对第三方 app 注册的 `AccessibilityService` 做了**静默拦截**：
- ✅ `dumpsys accessibility` 显示服务在 Bound list、Crashed list 是空的
- ❌ 但 `onServiceConnected` 永远不被调用，`onAccessibilityEvent` 收不到任何事件
- 用户手动 toggle / adb 强制激活 / 完成 vivo 二次确认 都无效

**结论**：vivo 上无障碍兜底**完全不可用**，微信红包 / 微信转账入账 / 支付宝转账入账 这类无金额通知场景只能手动补录。其他厂商（小米 / 华为 / OPPO / Pixel）理论上正常，但本项目暂未在这些设备上验证。

### 3. vivo 默认禁用第三方 app 通知

新装应用的 `importance=NONE`，系统级屏蔽所有通知发送。导致：
- **常驻保活通知发不出**（前台服务能起来，但通知被拦）
- 未来的预算超支提醒也会被拦

**解决**：手动去 设置 → 应用 → BookkeepingApp → 通知 → 允许通知。

### 4. 通知解析规则会随版本失效

支付宝/微信会不定期改通知文本格式。本项目当前规则基于 2024-2026 年间的样本（参考 v2ex、CFANZ、AutoAccountingOrg 等社区数据）调研而成，覆盖率约 80%。如果某天某条交易没自动抓上，多半是文本格式变了 —— 欢迎提 Issue 附上原文。

### 5. 银行短信只支持中国大陆主流银行

短信解析规则只针对 15+ 主流大陆银行。地方性银行、信用卡渠道、第三方支付平台（如花呗、白条）暂未覆盖。

### 6. 没有云同步

数据完全本地存储。换手机需要手动导出/导入（目前未做导出功能，**短期 TODO**）。

## 🚀 安装

### 直接装 APK
1. 到 [Releases 页](../../releases) 下载最新 `app-debug.apk`
2. 手机打开「未知来源应用安装」权限
3. 装好后**必须授权这两类权限**才能自动抓账：
   - **通知使用权**：设置 → 通知 → 通知使用权 → 找到 "BookkeepingApp" → 打开
   - **短信权限**：App 启动后会弹窗请求
4. **vivo 用户额外做一步**：设置 → 应用 → BookkeepingApp → 通知 → **允许通知**（vivo 默认会关）
5. 国产 ROM 用户建议把本应用加进「后台白名单 / 自启动 / 锁定到内存」（设置页内置各家路径指南）

### 自己编译
```bash
git clone https://github.com/DykiSensei/seamless-bookkeeping.git
cd seamless-bookkeeping
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 隐私 & 权限说明

| 权限 | 用途 | 是否必须 |
|---|---|---|
| 通知使用权 | 读取支付宝/微信付款通知 | 必须（否则无法自动抓账） |
| `RECEIVE_SMS` / `READ_SMS` | 接收并读取银行交易短信 | 必须（同上） |
| `POST_NOTIFICATIONS` | 前台保活的常驻通知 + 未来发预算超支提醒 | 推荐（Android 13+ 启动 app 时会弹窗） |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | 前台保活服务 | 用户开启保活时使用 |
| `BIND_ACCESSIBILITY_SERVICE` | 无障碍兜底（微信红包/转账 / 支付宝转账入账） | 可选，只在用户主动到系统无障碍设置勾选时生效 |

**关于隐私**：
- 所有数据 **只存在你本机的 SQLite**，没有任何后端服务器
- App **不联网**（未来如果加云同步会单独申请权限并明确说明）
- 短信和通知 **不会被发送到任何第三方**
- 代码完全开源，可自行审计

## 🙏 致谢

通知文本格式调研参考了以下社区资源：
- [V2EX - 监听微信支付宝收款](https://www.v2ex.com/t/539192) — 支付宝个人收款通知格式
- [AutoAccountingOrg](https://github.com/AutoAccountingOrg) — 自动记账规则社区
- [CFANZ - 无障碍服务监听微信收款](https://www.cfanz.cn/resource/detail/ygjpQoJEJgDkX) — 微信扫码收款不发通知这一关键事实
- [dxkite/notification-dispatcher](https://github.com/dxkite/notification-dispatcher)
- [c2s/AndroidNotificationDispatcher](https://github.com/c2s/AndroidNotificationDispatcher)

## 📄 License

[MIT](LICENSE) © 2026 Dyki

---

**项目状态**：MVP 已完成，欢迎提 Issue 反馈通知格式变化或 bug。Star ⭐ 一下表示支持。
