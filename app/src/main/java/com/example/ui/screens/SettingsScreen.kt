package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryItem
import com.example.data.model.CategoryRegistry
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.UpiIndigoAccent
import com.example.ui.viewmodel.ExpenseUiState
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.AppDateFormat
import com.example.util.DateUtils
import com.example.util.LocalBackupHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: ExpenseUiState,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedThemeColor by viewModel.selectedThemeColor.collectAsStateWithLifecycle()
    val defaultCategoryId by viewModel.defaultCategoryId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val defaultCategory = categories.firstOrNull { it.id == defaultCategoryId }
        ?: CategoryRegistry.getCategory(context, defaultCategoryId)

    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsStateWithLifecycle()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()
    val promptBackupOnOpen by viewModel.promptBackupOnOpen.collectAsStateWithLifecycle()

    val localBackupFolderName by viewModel.localBackupFolderName.collectAsStateWithLifecycle()
    val lastLocalBackupTime by viewModel.lastLocalBackupTime.collectAsStateWithLifecycle()
    val isLocalFolderConfigured by viewModel.isLocalFolderConfigured.collectAsStateWithLifecycle()

    val paymentModes by viewModel.paymentModes.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val defaultPaymentModeName by viewModel.defaultPaymentModeName.collectAsStateWithLifecycle()

    var showClearConfirmation by remember { mutableStateOf(false) }
    var showCategoriesModal by remember { mutableStateOf(false) }
    var showAddCategoryModal by remember { mutableStateOf(false) }
    var showPaymentModesModal by remember { mutableStateOf(false) }
    var showAddPaymentModeModal by remember { mutableStateOf(false) }
    var showCurrencyModal by remember { mutableStateOf(false) }
    var showThemeColorModal by remember { mutableStateOf(false) }
    var showRestoreSuccessDialog by remember { mutableStateOf<String?>(null) }

    // Add category form state
    var newCategoryTitle by remember { mutableStateOf("") }
    var newCategoryColorHex by remember { mutableLongStateOf(CategoryItem.PALETTE_COLORS[0]) }
    var newCategoryIconName by remember { mutableStateOf(CategoryItem.AVAILABLE_ICONS[0].first) }

    // Edit category form state
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var editCategoryTitle by remember { mutableStateOf("") }
    var editCategoryColorHex by remember { mutableLongStateOf(CategoryItem.PALETTE_COLORS[0]) }
    var editCategoryIconName by remember { mutableStateOf(CategoryItem.AVAILABLE_ICONS[0].first) }

    // Add payment mode form state
    var newModeLabel by remember { mutableStateOf("") }
    var newModeColorHex by remember { mutableLongStateOf(CategoryItem.PALETTE_COLORS[0]) }
    var newModeIconName by remember { mutableStateOf(PaymentMode.AVAILABLE_ICONS[0].first) }

    // Edit payment mode form state
    var editingPaymentMode by remember { mutableStateOf<PaymentMode?>(null) }
    var editModeLabel by remember { mutableStateOf("") }
    var editModeColorHex by remember { mutableLongStateOf(CategoryItem.PALETTE_COLORS[0]) }
    var editModeIconName by remember { mutableStateOf(PaymentMode.AVAILABLE_ICONS[0].first) }

    // Local Folder Selector Launcher (Untouched on Uninstall)
    val selectLocalFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setLocalBackupFolder(uri)
            Toast.makeText(context, "Local backup folder updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Export Backup File to local storage / download
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(viewModel.generateBackupJson().toByteArray(Charsets.UTF_8))
                }
                val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                viewModel.updateLastBackupTime(nowStr)
                Toast.makeText(context, "Backup file exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Restore Backup from File
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!jsonString.isNullOrBlank()) {
                    viewModel.restoreBackupJson(jsonString) { result ->
                        if (result.isSuccess) {
                            val count = result.getOrNull() ?: 0
                            showRestoreSuccessDialog = "Successfully restored $count expenses from backup file!"
                        } else {
                            val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                            Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_scroll_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App branding card
            item(key = "app_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_logo_badge),
                            contentDescription = "Khata Logo",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Khata",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Minimal Cash & UPI Expense & Income Ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Version 1.0 • Offline & Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = CashGreenAccent
                            )
                        }
                    }
                }
            }

            // UNIFIED BACKUP & RESTORE SECTION
            item(key = "local_backup_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CashGreenAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Daily Default",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CashGreenAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            item(key = "local_backup_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_backup_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Folder Path & Status
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectLocalFolderLauncher.launch(null) }
                                .testTag("select_local_folder_row"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isLocalFolderConfigured) CashGreenAccent else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Backup Folder",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = localBackupFolderName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                TextButton(onClick = { selectLocalFolderLauncher.launch(null) }) {
                                    Text(if (isLocalFolderConfigured) "Change" else "Choose", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Last Backup Time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last Backup:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lastLocalBackupTime ?: "Pending next auto-backup",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (lastLocalBackupTime != null) CashGreenAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Three buttons in the same row with 3 different colors: Backup, Export, Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Backup Button (Green)
                            Button(
                                onClick = {
                                    if (isLocalFolderConfigured) {
                                        viewModel.performLocalBackup { res ->
                                            if (res.isSuccess) {
                                                val createdName = res.getOrNull() ?: "backup file"
                                                Toast.makeText(context, "Saved new backup: $createdName", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Backup failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        selectLocalFolderLauncher.launch(null)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981), // Emerald Green
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_local_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Backup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            // 2. Export Button (Blue)
                            Button(
                                onClick = {
                                    val fileName = "khata_backup_${DateUtils.getBackupDateTag()}.json"
                                    exportBackupLauncher.launch(fileName)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB), // Royal Blue
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Export",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            // 3. Restore Button (Purple)
                            Button(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "text/*"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C3AED), // Vivid Purple
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Restore",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // BACKUP PROMPT ON OPEN SETTING
            item(key = "backup_prompt_setting_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_prompt_setting_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CashGreenAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = CashGreenAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Prompt backup on app open",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show a reminder dialog to back up whenever the app is opened",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = promptBackupOnOpen,
                            onCheckedChange = { viewModel.setPromptBackupOnOpen(it) },
                            modifier = Modifier.testTag("prompt_backup_on_open_switch")
                        )
                    }
                }
            }

            // Preferences & Theme
            item(key = "preferences_header") {
                Text(
                    text = "Preferences & Categories",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item(key = "pref_items") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        // Theme Color picker option
                        SettingsOptionRow(
                            icon = Icons.Default.Palette,
                            iconTint = selectedThemeColor.primaryColor,
                            title = "Theme Color",
                            subtitle = "${selectedThemeColor.displayName} (Tap to change)",
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(selectedThemeColor.primaryColor)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            },
                            onClick = { showThemeColorModal = true }
                        )

                        // Categories row: Single option to manage, edit, delete, and set default category
                        SettingsOptionRow(
                            icon = Icons.Default.Category,
                            iconTint = Color(0xFFF59E0B),
                            title = "Categories",
                            subtitle = "${categories.size} categories • Default: ${defaultCategory.title}",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Manage Categories",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { showCategoriesModal = true }
                        )

                        // Payment Modes row: Single option to manage, edit, delete, and set default payment mode
                        val defModeLabel = paymentModes.firstOrNull { it.name == defaultPaymentModeName }?.label ?: "UPI"
                        SettingsOptionRow(
                            icon = Icons.Default.Payments,
                            iconTint = CashGreenAccent,
                            title = "Payment Modes",
                            subtitle = "${paymentModes.size} modes • Default: $defModeLabel",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Manage Payment Modes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { showPaymentModesModal = true }
                        )

                        SettingsOptionRow(
                            icon = Icons.Default.CurrencyRupee,
                            iconTint = CashGreenAccent,
                            title = "Currency",
                            subtitle = selectedCurrency.displayName,
                            onClick = { showCurrencyModal = true }
                        )
                    }
                }
            }

            // Data Management
            item(key = "data_header") {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item(key = "data_items") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsOptionRow(
                            icon = Icons.Default.PlayArrow,
                            iconTint = CashGreenAccent,
                            title = "Load Sample Data",
                            subtitle = "Populate realistic sample expenses to explore stats",
                            onClick = { viewModel.populateSampleData() }
                        )

                        SettingsOptionRow(
                            icon = Icons.Default.DeleteForever,
                            iconTint = DangerRed,
                            title = "Clear All Expenses",
                            subtitle = "Permanently remove all expense records",
                            isDestructive = true,
                            onClick = { showClearConfirmation = true }
                        )
                    }
                }
            }

            // Privacy & Architecture
            item(key = "privacy_header") {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item(key = "privacy_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CashGreenAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CashGreenAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "100% On-Device & Private",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your expense records stay on your device and inside your chosen backup folder. No third-party servers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Developer Credit Card at very bottom of Settings page
            item(key = "developer_header") {
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item(key = "developer_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_info_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Developer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Prashanth Shetty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Creator & Developer of Khata",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // CATEGORY MANAGEMENT DIALOG (ONLY HERE CAN USERS ADD/REMOVE CATEGORIES)
    if (showCategoriesModal) {
        AlertDialog(
            onDismissRequest = { showCategoriesModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            newCategoryTitle = ""
                            newCategoryColorHex = CategoryItem.PALETTE_COLORS[0]
                            newCategoryIconName = CategoryItem.AVAILABLE_ICONS[0].first
                            showAddCategoryModal = true
                        },
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = "Add new categories or delete existing ones. Ability to add/remove categories is available only here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(cat.color.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = cat.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        val isDefault = cat.id == defaultCategoryId
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cat.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isDefault) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Default",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD97706),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (cat.isCustom) "Custom" else "System",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        val isDefault = cat.id == defaultCategoryId

                                        // Set as default button
                                        IconButton(
                                            onClick = {
                                                viewModel.setDefaultCategory(cat.id)
                                                Toast.makeText(context, "'${cat.title}' is now default category", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("set_default_cat_${cat.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = if (isDefault) "Default category" else "Set as default",
                                                tint = if (isDefault) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                editingCategory = cat
                                                editCategoryTitle = cat.title
                                                editCategoryColorHex = cat.colorHex
                                                editCategoryIconName = cat.iconName
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("edit_cat_${cat.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit category",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete button (allowed only if more than 1 category exists)
                                        if (categories.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteCategory(cat.id)
                                                    Toast.makeText(context, "Deleted '${cat.title}'", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("delete_cat_${cat.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete category",
                                                    tint = DangerRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newCategoryTitle = ""
                        newCategoryColorHex = CategoryItem.PALETTE_COLORS[0]
                        newCategoryIconName = CategoryItem.AVAILABLE_ICONS[0].first
                        showAddCategoryModal = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoriesModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    // ADD NEW CATEGORY MODAL
    if (showAddCategoryModal) {
        AlertDialog(
            onDismissRequest = { showAddCategoryModal = false },
            title = { Text("Add Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCategoryTitle,
                        onValueChange = { newCategoryTitle = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Rent, Gym, Books") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Choose Icon
                    Column {
                        Text(
                            text = "Choose Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CategoryItem.AVAILABLE_ICONS) { (name, iconVector) ->
                                val isSelected = newCategoryIconName == name
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(newCategoryColorHex).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, if (isSelected) Color(newCategoryColorHex) else Color.Transparent, CircleShape)
                                        .clickable { newCategoryIconName = name },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = name,
                                        tint = if (isSelected) Color(newCategoryColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Choose Color
                    Column {
                        Text(
                            text = "Choose Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CategoryItem.PALETTE_COLORS) { colorHex ->
                                val isSelected = newCategoryColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                        .clickable { newCategoryColorHex = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryTitle.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.addCategory(trimmed, newCategoryColorHex, newCategoryIconName)
                            showAddCategoryModal = false
                            Toast.makeText(context, "Category '$trimmed' added!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_new_category_button")
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT CATEGORY MODAL
    editingCategory?.let { catToEdit ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editCategoryTitle,
                        onValueChange = { editCategoryTitle = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Rent, Gym, Books") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_category_name_input")
                    )

                    // Choose Icon
                    Column {
                        Text(
                            text = "Choose Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CategoryItem.AVAILABLE_ICONS) { (name, iconVector) ->
                                val isSelected = editCategoryIconName == name
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(editCategoryColorHex).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, if (isSelected) Color(editCategoryColorHex) else Color.Transparent, CircleShape)
                                        .clickable { editCategoryIconName = name },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = name,
                                        tint = if (isSelected) Color(editCategoryColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Choose Color
                    Column {
                        Text(
                            text = "Choose Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CategoryItem.PALETTE_COLORS) { colorHex ->
                                val isSelected = editCategoryColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                        .clickable { editCategoryColorHex = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editCategoryTitle.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.editCategory(catToEdit.id, trimmed, editCategoryColorHex, editCategoryIconName)
                            editingCategory = null
                            Toast.makeText(context, "Category '$trimmed' updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_edit_category_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Color Picker Dialog
    if (showThemeColorModal) {
        AlertDialog(
            onDismissRequest = { showThemeColorModal = false },
            title = { Text("Choose Theme Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeColor.entries.forEach { themeColorOption ->
                        val isSelected = selectedThemeColor == themeColorOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeColor(themeColorOption)
                                    showThemeColorModal = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(themeColorOption.primaryColor)
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = themeColorOption.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = themeColorOption.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeColorModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Restore Success Dialog
    showRestoreSuccessDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = null },
            title = { Text("Restore Completed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { showRestoreSuccessDialog = null }) {
                    Text("Great")
                }
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Data?") },
            text = {
                Text("This will permanently delete all ${uiState.expenses.size} expenses. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAll()
                        showClearConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CURRENCY SELECTION MODAL
    if (showCurrencyModal) {
        AlertDialog(
            onDismissRequest = { showCurrencyModal = false },
            title = {
                Text(
                    text = "Select Currency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .testTag("currency_selection_dialog")
                ) {
                    Text(
                        text = "Choose your preferred currency symbol and formatting for all expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AppCurrency.entries) { currencyItem ->
                            val isSelected = currencyItem == selectedCurrency
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setCurrency(currencyItem)
                                        showCurrencyModal = false
                                        Toast.makeText(context, "Currency set to ${currencyItem.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("currency_option_${currencyItem.code.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currencyItem.symbol,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = currencyItem.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Example: ${currencyItem.format(1250.0)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    // PAYMENT MODES MANAGEMENT MODAL (ABILITY TO ADD/EDIT/DELETE/SET-DEFAULT UNDER SINGLE OPTION)
    if (showPaymentModesModal) {
        AlertDialog(
            onDismissRequest = { showPaymentModesModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Modes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            newModeLabel = ""
                            newModeColorHex = CategoryItem.PALETTE_COLORS[0]
                            newModeIconName = PaymentMode.AVAILABLE_ICONS[0].first
                            showAddPaymentModeModal = true
                        },
                        modifier = Modifier.testTag("add_payment_mode_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Payment Mode", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .testTag("payment_modes_dialog")
                ) {
                    Text(
                        text = "Manage payment modes. You can edit, delete, or set any mode as the default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(paymentModes, key = { it.name }) { mode ->
                            val isDefault = mode.name.equals(defaultPaymentModeName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(mode.color.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = mode.icon,
                                                contentDescription = null,
                                                tint = mode.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isDefault) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Default",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD97706),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (mode.isCustom) "Custom mode" else "System mode",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Set as default button
                                        IconButton(
                                            onClick = {
                                                viewModel.setDefaultPaymentMode(mode.name)
                                                Toast.makeText(context, "'${mode.label}' is now default payment mode", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("default_mode_${mode.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = if (isDefault) "Default payment mode" else "Set as default",
                                                tint = if (isDefault) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                editingPaymentMode = mode
                                                editModeLabel = mode.label
                                                editModeColorHex = mode.colorHex
                                                editModeIconName = mode.iconName
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("edit_mode_${mode.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit payment mode",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete button (allowed only if more than 1 mode exists)
                                        if (paymentModes.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deletePaymentMode(mode.name)
                                                    Toast.makeText(context, "Deleted payment mode '${mode.label}'", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("delete_mode_${mode.name.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete payment mode",
                                                    tint = DangerRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newModeLabel = ""
                        newModeColorHex = CategoryItem.PALETTE_COLORS[0]
                        newModeIconName = PaymentMode.AVAILABLE_ICONS[0].first
                        showAddPaymentModeModal = true
                    },
                    modifier = Modifier.testTag("open_add_payment_mode_dialog_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentModesModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    // EDIT PAYMENT MODE MODAL
    editingPaymentMode?.let { modeToEdit ->
        AlertDialog(
            onDismissRequest = { editingPaymentMode = null },
            title = { Text("Edit Payment Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_payment_mode_dialog")
                ) {
                    OutlinedTextField(
                        value = editModeLabel,
                        onValueChange = { editModeLabel = it },
                        label = { Text("Payment Mode Name") },
                        placeholder = { Text("e.g. Credit Card, Debit Card, Net Banking") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_payment_mode_input")
                    )

                    // Choose Icon
                    Column {
                        Text(
                            text = "Choose Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PaymentMode.AVAILABLE_ICONS.forEach { (iconKey, iconVec) ->
                                val isSelected = editModeIconName == iconKey
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { editModeIconName = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = iconKey,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Choose Color
                    Column {
                        Text(
                            text = "Choose Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryItem.PALETTE_COLORS.take(7).forEach { colorLong ->
                                val isSelected = editModeColorHex == colorLong
                                val color = Color(colorLong)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { editModeColorHex = colorLong },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editModeLabel.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.editPaymentMode(
                                name = modeToEdit.name,
                                label = trimmed,
                                iconName = editModeIconName,
                                colorHex = editModeColorHex
                            )
                            Toast.makeText(context, "Updated '$trimmed'", Toast.LENGTH_SHORT).show()
                            editingPaymentMode = null
                        } else {
                            Toast.makeText(context, "Please enter a mode name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_edit_payment_mode_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPaymentMode = null },
                    modifier = Modifier.testTag("cancel_edit_payment_mode_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD NEW PAYMENT MODE MODAL
    if (showAddPaymentModeModal) {
        AlertDialog(
            onDismissRequest = { showAddPaymentModeModal = false },
            title = { Text("Add Payment Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_payment_mode_dialog")
                ) {
                    OutlinedTextField(
                        value = newModeLabel,
                        onValueChange = { newModeLabel = it },
                        label = { Text("Payment Mode Name") },
                        placeholder = { Text("e.g. Credit Card, Debit Card, Net Banking") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_payment_mode_input")
                    )

                    // Choose Icon
                    Column {
                        Text(
                            text = "Choose Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PaymentMode.AVAILABLE_ICONS.forEach { (iconKey, iconVec) ->
                                val isSelected = newModeIconName == iconKey
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newModeIconName = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = iconKey,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Choose Color
                    Column {
                        Text(
                            text = "Choose Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CategoryItem.PALETTE_COLORS.take(7).forEach { colorLong ->
                                val isSelected = newModeColorHex == colorLong
                                val color = Color(colorLong)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newModeColorHex = colorLong },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newModeLabel.isNotBlank()) {
                            viewModel.addPaymentMode(
                                label = newModeLabel.trim(),
                                iconName = newModeIconName,
                                colorHex = newModeColorHex
                            )
                            Toast.makeText(context, "Added '${newModeLabel.trim()}'", Toast.LENGTH_SHORT).show()
                            showAddPaymentModeModal = false
                        } else {
                            Toast.makeText(context, "Please enter a mode name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_payment_mode_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddPaymentModeModal = false },
                    modifier = Modifier.testTag("cancel_add_payment_mode_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) DangerRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent?.let {
            Spacer(modifier = Modifier.width(8.dp))
            it()
        }
    }
}
