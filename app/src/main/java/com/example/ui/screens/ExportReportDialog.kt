package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.domain.export.ReportGenerator
import com.example.domain.export.ReportType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.AppFontFamily
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    uiState: UiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedReportType by remember { mutableStateOf(ReportType.FULL_SETTLEMENT) }
    var exportFormat by remember { mutableStateOf("TEXT") } // TEXT or CSV
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val reportText = remember(selectedReportType, exportFormat, uiState) {
        if (exportFormat == "CSV") {
            ReportGenerator.generateCsvReport(
                trip = uiState.currentTrip,
                members = uiState.members,
                summary = uiState.financialSummary,
                statuses = uiState.memberStatuses,
                settlementTransfers = uiState.settlementTransfers,
                expenses = uiState.expenses,
                funds = uiState.fundContributions,
                splits = uiState.allSplits
            )
        } else {
            ReportGenerator.generateTextReport(
                trip = uiState.currentTrip,
                members = uiState.members,
                financialSummary = uiState.financialSummary,
                memberStatuses = uiState.memberStatuses,
                settlementTransfers = uiState.settlementTransfers,
                expenses = uiState.expenses,
                fundContributions = uiState.fundContributions,
                splits = uiState.allSplits,
                reportType = selectedReportType,
                creatorName = uiState.currentMember?.name ?: "Thành viên"
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = IndigoSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xuất Báo Cáo Tài Chính",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Format Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportFormat == "TEXT",
                        onClick = { exportFormat = "TEXT" },
                        label = {
                            Text(
                                "Văn bản báo cáo",
                                fontSize = 11.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                    FilterChip(
                        selected = exportFormat == "CSV",
                        onClick = { exportFormat = "CSV" },
                        label = {
                            Text(
                                "Excel / CSV (UTF-8)",
                                fontSize = 11.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // If in TEXT mode, select report sub-type
                if (exportFormat == "TEXT") {
                    Text(
                        text = "Loại báo cáo tiếng Việt:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportType.values().forEach { rType ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedReportType == rType) Color(0xFFE0E7FF) else Color(0xFFF8FAFC),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedReportType = rType }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedReportType == rType,
                                        onClick = { selectedReportType = rType }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = rType.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = AppFontFamily,
                                            color = if (selectedReportType == rType) Color(0xFF312E81) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = rType.description,
                                            fontSize = 10.sp,
                                            fontFamily = AppFontFamily,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Copy Notification
                if (copiedNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = copiedNotice!!,
                                fontSize = 12.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }

                // Live Preview Canvas
                Text(
                    text = "Xem trước nội dung báo cáo:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFontFamily,
                    color = Color(0xFF475569)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFAFBFD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                ) {
                    val horizontalScrollState = rememberScrollState()
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            item {
                                Text(
                                    text = reportText,
                                    fontFamily = AppFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy Button
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportText))
                        copiedNotice = "Đã sao chép toàn bộ báo cáo vào bộ nhớ tạm!"
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_report_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sao chép", fontSize = 12.sp, fontFamily = AppFontFamily)
                }

                // Share Button
                Button(
                    onClick = {
                        shareReport(context, reportText, uiState.currentTrip?.title ?: "Báo cáo đoàn", exportFormat)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chia sẻ báo cáo", fontSize = 12.sp, fontFamily = AppFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", fontFamily = AppFontFamily)
            }
        }
    )
}

private fun shareReport(context: Context, reportContent: String, tripTitle: String, format: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_SUBJECT, "Báo cáo quyết toán tài chính - $tripTitle")
        putExtra(Intent.EXTRA_TEXT, reportContent)
        type = if (format == "CSV") "text/comma-separated-values" else "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ báo cáo tài chính qua...")
    context.startActivity(shareIntent)
}
