package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldPrimary
import java.util.UUID

@Composable
fun CreateTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        joinCode: String,
        startDate: Long,
        endDate: Long,
        adminName: String,
        adminBankName: String?,
        adminBankAccount: String?,
        adminBankHolder: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var joinCode by remember {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        mutableStateOf(code)
    }
    var adminName by remember { mutableStateOf("") }
    var adminBankName by remember { mutableStateOf("Vietcombank") }
    var adminBankAccount by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val safeDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    val isValid = title.isNotBlank() && joinCode.length == 6 && adminName.isNotBlank()

    AlertDialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tạo Đoàn Công Tác / Du Lịch Mới", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên đoàn (VD: Du lịch Phú Quốc 2026) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_trip_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả / Mục đích đoàn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { if (it.length <= 6) joinCode = it.uppercase() },
                    label = { Text("Mã tham gia 6 ký tự *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Họ tên bạn (Trưởng đoàn) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adminBankName,
                    onValueChange = { adminBankName = it },
                    label = { Text("Ngân hàng nhận tiền quyết toán") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adminBankAccount,
                    onValueChange = { adminBankAccount = it },
                    label = { Text("Số tài khoản nhận tiền") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val now = System.currentTimeMillis()
                    onConfirm(
                        title,
                        description,
                        joinCode,
                        now,
                        now + 86400000L * 5,
                        adminName,
                        adminBankName.takeIf { it.isNotBlank() },
                        adminBankAccount.takeIf { it.isNotBlank() },
                        adminName
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("submit_create_trip_button")
            ) {
                Text("Tạo đoàn")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun JoinTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, userName: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val safeDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    val isValid = code.length == 6 && userName.isNotBlank()

    AlertDialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tham Gia Đoàn Bằng Mã", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nhập mã 6 ký tự được Trưởng đoàn cung cấp:", fontSize = 12.sp)

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = { Text("Mã đoàn (VD: DN2026)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_code_input")
                )

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Họ và tên của bạn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_user_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onConfirm(code, userName)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("submit_join_trip_button")
            ) {
                Text("Tham gia ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Hủy") }
        }
    )
}
