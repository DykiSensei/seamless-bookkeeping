# 无感记账（Seamless Bookkeeping）

一款面向中国大陆用户的 Android 自动记账 App。核心目标是**不用手动输入每一笔账** —— 当你支付宝/微信付款，或银行卡刷卡时，App 在后台读取系统通知和短信，自动把交易记到账本里。

> ⚠️ **状态**：MVP 阶段。功能完整可用，但解析规则覆盖率约 80%，部分边角场景（尤其是微信扫码收款）暂未支持，详见 [已知限制](#-已知限制) 章节。

## ✨ 已实现功能

### 自动抓账
- 🟢 **支付宝通知**：抓取付款成功 / 收款 / 退款，提取金额、收支方向、商户、付款方式
- 🟢 **微信支付通知**：抓取转账（"向你/您付款"为收入；"向 商户 付款"为支出）
- 🟢 **银行交易短信**：覆盖工/招/建/中/农/交/邮储/中信/光大/民生/浦发/平安/广发/兴业/华夏等 15+ 主流银行；自动识别银行名、卡尾号、金额、收/支方向

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

### 数据安全
- 🔒 **完全本地存储**：所有交易数据存在本机 SQLite，无任何云端上传
- 🚫 **不联网**：App 本身不需要网络权限（如果未来加云同步会另外申请）

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
├── MainActivity.kt
├── data/
│   ├── budget/                     # 预算（DataStore）
│   ├── local/                      # Room DB / DAO / Entity
│   └── repository/                 # 业务层
├── di/
│   └── DatabaseModule.kt           # Hilt 模块
├── notification/                   # 自动抓取核心
│   ├── PaymentNotificationListenerService.kt  # 通知监听 Service
│   ├── NotificationParser.kt                  # 支付宝/微信解析
│   ├── SmsBroadcastReceiver.kt                # SMS 广播接收
│   ├── SmsParser.kt                           # 银行短信解析
│   ├── CategoryClassifier.kt                  # 自动分类
│   ├── NotificationListenerHelper.kt
│   └── SmsPermissionHelper.kt
└── ui/
    ├── navigation/RootNavigation.kt           # 底部 3-tab NavHost
    └── screen/
        ├── transactions/                      # 账单 tab
        ├── stats/                             # 统计 tab
        └── budget/                            # 预算 tab
```

## ⚠️ 已知限制

### 1. 微信扫码收款抓不到

**这是当前最大的功能缺口**。微信从 2020 年起停止为扫码收款发系统通知（改为在 app 内提示），所以 `NotificationListenerService` 抓不到。

**影响范围**：
- ❌ 别人扫你的收款码付钱给你 → 抓不到
- ❌ 商家用收款码收你扫码的钱（部分场景）→ 抓不到
- ✅ 微信转账（"向你付款"）→ 能抓
- ✅ 微信红包到账（部分版本）→ 能抓

**临时方案**：手动补录
**长期方案**：实现 `AccessibilityService`（无障碍服务）监听微信 app 内的金额变化界面。这是个独立的大功能，需要单独申请无障碍权限，且对电池消耗、Android 14+ 后台限制都有挑战。已在路线图中。

### 2. 通知解析规则会随版本失效

支付宝/微信会不定期改通知文本格式。本项目当前规则基于 2024-2026 年间的样本（参考 v2ex、CFANZ、AutoAccountingOrg 等社区数据）调研而成，覆盖率约 80%。如果某天某条交易没自动抓上，多半是文本格式变了 —— 欢迎提 Issue 附上原文。

### 3. 银行短信只支持中国大陆主流银行

短信解析规则只针对 15+ 主流大陆银行。地方性银行、信用卡渠道、第三方支付平台（如花呗、白条）暂未覆盖。

### 4. 没有云同步

数据完全本地存储。换手机需要手动导出/导入（目前未做导出功能，**短期 TODO**）。

## 🚀 安装

### 直接装 APK
1. 到 [Releases 页](../../releases) 下载最新 `app-debug.apk`
2. 手机打开「未知来源应用安装」权限
3. 装好后**必须授权这两类权限**才能自动抓账：
   - **通知使用权**：设置 → 通知 → 通知使用权 → 找到 "BookkeepingApp" → 打开
   - **短信权限**：App 启动后会弹窗请求

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
| `POST_NOTIFICATIONS` | 未来发预算超支提醒 | 可选 |

**关于隐私**：
- 所有数据 **只存在你本机的 SQLite**，没有任何后端服务器
- App **不联网**（未来如果加云同步会单独申请权限并明确说明）
- 短信和通知 **不会被发送到任何第三方**
- 代码完全开源，可自行审计

## 🗺 路线图

近期：
- [ ] 数据导出（CSV / Excel）—— 换机数据迁移
- [ ] 编辑已有交易（目前只能删除重建）
- [ ] 更智能的去重（同一笔交易可能 SMS + 通知都进来）

中期：
- [ ] 无障碍服务（AccessibilityService）补微信扫码收款
- [ ] 学习用户手动改分类，下次自动用
- [ ] 多账户（现金 / 支付宝 / 微信 / 各银行卡）单独余额

远期：
- [ ] 自建后端做云同步（可选）
- [ ] 多设备共享账本（家庭账本）
- [ ] iOS 版本

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
