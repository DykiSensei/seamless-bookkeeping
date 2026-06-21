package com.bookkeeping.app.ui.screen.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookkeeping.app.notification.accessibility.AccessibilityCaptureService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val keepAlive by viewModel.keepAliveEnabled.collectAsState()
    val context = LocalContext.current

    // 无障碍服务的"是否已被系统勾选"状态。
    // 用户从无障碍设置返回时（ON_RESUME）重新读一次。
    val lifecycleOwner = LocalLifecycleOwner.current
    var accessibilityEnabled by remember {
        mutableStateOf(AccessibilityCaptureService.isEnabled(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = AccessibilityCaptureService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("后台保活") }
            item {
                KeepAliveCard(
                    enabled = keepAlive,
                    onToggle = viewModel::setKeepAlive,
                )
            }

            item { SectionTitle("权限授权") }
            item {
                PermissionCard(
                    title = "通知使用权",
                    desc = "用于抓取支付宝/微信支付通知。必须授权，否则自动记账无效。",
                    buttonText = "去授权",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    },
                )
            }
            item {
                PermissionCard(
                    title = "短信权限",
                    desc = "用于读取银行交易短信。如果你不使用银行卡或不需要银行通道，可以不开。",
                    buttonText = "应用权限设置",
                    onClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                )
            }
            item {
                AccessibilityCard(
                    enabled = accessibilityEnabled,
                    onOpen = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                )
            }

            item { SectionTitle("国产 ROM 后台白名单（重要）") }
            item { RomGuideCard() }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun KeepAliveCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "常驻通知保活",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (enabled) "已开启：通知栏会有一条常驻提示"
                               else "未开启：节省一条通知，但可能被国产 ROM 杀掉",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Text(
                text = "原理：开启后 app 会启动一个前台 Service，挂在通知栏。" +
                       "Android 系统不会随便杀正在运行前台服务的进程，从而保住通知监听不掉线。" +
                       "代价是通知栏多一条常驻提示。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FilledTonalButton(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun AccessibilityCard(enabled: Boolean, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.tertiaryContainer
                             else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (enabled) "无障碍兜底：已开启" else "无障碍兜底：未开启",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (enabled)
                    "微信转账/红包、支付宝转账入账等没有通知的场景会自动抓取。"
                else
                    "微信转账、微信红包、支付宝转账入账等场景，通知里不含金额，必须靠无障碍服务兜底。" +
                    "未开启会漏抓这些交易。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // 强提示：隐私边界
            Text(
                text = "隐私说明",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "无障碍权限会让本应用看到微信、支付宝两个 app 内的所有窗口内容。" +
                       "我们已通过 packageNames 限定仅监听这两个包名（不会读取你打开的其他 app），" +
                       "金额与商户信息只保存在本机数据库，不上传任何服务器。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            FilledTonalButton(onClick = onOpen) {
                Text(if (enabled) "去无障碍设置（管理/关闭）" else "去无障碍设置（开启）")
            }
        }
    }
}

@Composable
private fun RomGuideCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "为什么需要？",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "小米、华为、OPPO、vivo 等国产 ROM 默认会激进省电，把不常用的 app 进程彻底杀掉，" +
                       "导致支付通知到达时本应用没法被系统拉起，漏抓账单。" +
                       "需要手动把本应用加进各家的「允许后台运行」/「自启动」白名单。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            RomEntry(
                brand = "小米 / Redmi",
                path = "设置 → 应用设置 → 应用管理 → 找到本应用 → 「自启动」打开 +「省电策略」选「无限制」",
            )
            RomEntry(
                brand = "华为 / 荣耀",
                path = "设置 → 应用 → 应用启动管理 → 找到本应用 → 关闭「自动管理」→ 手动管理三项全开",
            )
            RomEntry(
                brand = "OPPO / 一加 / realme",
                path = "设置 → 电池 → 应用耗电管理 → 找到本应用 → 允许后台运行 + 允许关联启动",
            )
            RomEntry(
                brand = "vivo / iQOO",
                path = "设置 → 电池 → 后台应用管理 → 找到本应用 → 允许后台运行",
            )
            RomEntry(
                brand = "三星",
                path = "设置 → 设备维护 → 电池 → 未受监视应用 → 添加本应用",
            )
            RomEntry(
                brand = "原生 Android (Pixel)",
                path = "一般不用动。如有问题：设置 → 电池 → 电池优化 → 本应用 → 不优化",
            )
        }
    }
}

@Composable
private fun RomEntry(brand: String, path: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = brand,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
