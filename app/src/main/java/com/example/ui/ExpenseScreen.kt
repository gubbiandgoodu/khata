package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.ui.components.AddEditExpenseSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.CashGreenAccent
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.DateUtils
import com.example.util.LocalBackupHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

enum class KharchuPage(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TRANSACTIONS("Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    STATISTICS("Statistics", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultCategoryId by viewModel.defaultCategoryId.collectAsStateWithLifecycle()
    val promptBackupOnOpen by viewModel.promptBackupOnOpen.collectAsStateWithLifecycle()
    val isLocalFolderConfigured by viewModel.isLocalFolderConfigured.collectAsStateWithLifecycle()
    val localBackupFolderName by viewModel.localBackupFolderName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkAndPerformAutoBackup(context)
    }

    var showBackupPromptDialog by rememberSaveable { mutableStateOf(true) }

    val setupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setLocalBackupFolder(uri)
            showBackupPromptDialog = false
            Toast.makeText(context, "Local daily auto-backup configured!", Toast.LENGTH_SHORT).show()
        }
    }

    val exportPromptBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(viewModel.generateBackupJson().toByteArray(Charsets.UTF_8))
                }
                val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                viewModel.updateLastBackupTime(nowStr)
                showBackupPromptDialog = false
                Toast.makeText(context, "Backup file exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedPageIndex by rememberSaveable { mutableIntStateOf(0) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                KharchuPage.entries.forEachIndexed { index, page ->
                    val isSelected = selectedPageIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedPageIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) page.selectedIcon else page.unselectedIcon,
                                contentDescription = page.title
                            )
                        },
                        label = {
                            Text(
                                text = page.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${page.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedPageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "page_transition"
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> TransactionsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAddIncome = { viewModel.openAddIncomeSheet() },
                        onEditExpense = { viewModel.openEditSheet(it) },
                        onRequestDeleteExpense = { expenseToDelete = it }
                    )
                    1 -> StatisticsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onEditExpense = { viewModel.openEditSheet(it) },
                        onRequestDeleteExpense = { expenseToDelete = it }
                    )
                    2 -> SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { expense ->
        val entryTypeLabel = if (expense.isIncome) "Income" else "Expense"
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete $entryTypeLabel?") },
            text = {
                val mode = expense.paymentMode.label
                val desc = if (expense.note.isNotBlank()) " for '${expense.note}'" else ""
                val formattedAmount = uiState.currency.format(expense.amount)
                Text("Are you sure you want to delete this $mode ${entryTypeLabel.lowercase()} of $formattedAmount$desc?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = expense
                        viewModel.deleteExpense(deleted)
                        expenseToDelete = null

                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "$entryTypeLabel deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Prompt on App Open (Appears every time app is opened; option to turn off in Settings)
    if (promptBackupOnOpen && showBackupPromptDialog) {
        AlertDialog(
            onDismissRequest = { showBackupPromptDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Backup Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isLocalFolderConfigured) {
                            "Keep your expense records safe and untouched. Would you like to create a backup now?"
                        } else {
                            "Keep your expense records safe. Choose a folder on your phone for automatic daily backups, or export an offline backup file."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isLocalFolderConfigured) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = CashGreenAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Saved Folder",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = localBackupFolderName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (!isLocalFolderConfigured) {
                        OutlinedButton(
                            onClick = {
                                val fileName = "khata_backup_${DateUtils.getBackupDateTag()}.json"
                                exportPromptBackupLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prompt_export_file_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup JSON File", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Checkbox to turn off prompt on open
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setPromptBackupOnOpen(!promptBackupOnOpen)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("prompt_backup_dont_ask_row")
                    ) {
                        Checkbox(
                            checked = !promptBackupOnOpen,
                            onCheckedChange = { viewModel.setPromptBackupOnOpen(!it) },
                            modifier = Modifier.testTag("prompt_backup_dont_ask_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Don't prompt on app open (manage in Settings)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isLocalFolderConfigured) {
                    Button(
                        onClick = {
                            viewModel.performLocalBackup { res ->
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Backup saved to phone storage!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Backup failed: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                            showBackupPromptDialog = false
                        },
                        modifier = Modifier.testTag("prompt_backup_now_button")
                    ) {
                        Text("Backup Now", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            setupFolderLauncher.launch(null)
                        },
                        modifier = Modifier.testTag("prompt_setup_folder_button")
                    ) {
                        Text("Select Folder", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupPromptDialog = false },
                    modifier = Modifier.testTag("prompt_backup_later_button")
                ) {
                    Text("Later")
                }
            }
        )
    }

    // Add / Edit Bottom Sheet
    if (uiState.isAddEditSheetOpen) {
        val resolvedDefaultCat = uiState.categories.firstOrNull { it.id == defaultCategoryId }
            ?: CategoryRegistry.getCategory(context, defaultCategoryId)

        AddEditExpenseSheet(
            sheetState = sheetState,
            initialExpense = uiState.editingExpense,
            initialIsIncome = uiState.isAddingIncome,
            defaultCategory = resolvedDefaultCat,
            currency = uiState.currency,
            paymentModes = uiState.paymentModes,
            categories = uiState.categories,
            onSetDefaultCategory = { viewModel.setDefaultCategory(it) },
            onDismiss = { viewModel.closeSheet() },
            onSave = { amount, dateString, paymentMode, category, note, isIncome ->
                viewModel.saveExpense(amount, dateString, paymentMode, category, note, isIncome)
            }
        )
    }
}
