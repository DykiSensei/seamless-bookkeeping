package com.bookkeeping.app.ui.screen.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bookkeeping.app.data.local.entity.TransactionCategory

// 编辑预算对话框
// pickCategory = true：选分类 + 输金额；false：仅输金额（用于总预算）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetDialog(
    title: String,
    initialYuan: Double = 0.0,
    pickCategory: Boolean = false,
    excludeCategories: Set<TransactionCategory> = emptySet(),
    initialCategory: TransactionCategory? = null,
    onDismiss: () -> Unit,
    onConfirm: (category: TransactionCategory?, yuan: Double) -> Unit,
) {
    var amountText by remember {
        mutableStateOf(if (initialYuan > 0) "%.2f".format(initialYuan) else "")
    }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var menuExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()
    val amountValid = amount != null && amount >= 0
    val categoryValid = !pickCategory || selectedCategory != null

    val availableCategories = remember(excludeCategories) {
        TransactionCategory.entries.filter {
            it !in excludeCategories && it != TransactionCategory.SALARY && it != TransactionCategory.TRANSFER
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pickCategory) {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.displayName ?: "选择分类",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分类") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        selectedCategory = cat
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("金额（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountText.isNotEmpty() && !amountValid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCategory, amount ?: 0.0) },
                enabled = amountValid && categoryValid,
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
