package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalBackupHelper {

    private const val TAG = "LocalBackupHelper"
    private const val PREFS_NAME = "kharchu_prefs"
    private const val KEY_FOLDER_URI = "local_backup_folder_uri"
    private const val KEY_FOLDER_NAME = "local_backup_folder_name"
    private const val KEY_LAST_LOCAL_BACKUP = "last_local_backup_time"
    private const val KEY_LAST_LOCAL_DATE = "last_local_backup_date"
    private const val KEY_SETUP_ASKED = "local_backup_setup_asked"

    fun isSetupAsked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_ASKED, false)
    }

    fun markSetupAsked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_ASKED, true).apply()
    }

    fun getLocalBackupFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (_: Exception) {
            null
        }
    }

    fun getFolderName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_FOLDER_NAME, null)
        if (!savedName.isNullOrBlank()) return savedName

        val uri = getLocalBackupFolderUri(context) ?: return "No folder selected"
        return try {
            val doc = DocumentFile.fromTreeUri(context, uri)
            val name = doc?.name ?: "Selected Local Folder"
            prefs.edit().putString(KEY_FOLDER_NAME, name).apply()
            name
        } catch (_: Exception) {
            "Selected Local Folder"
        }
    }

    fun getLastLocalBackupTime(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_LOCAL_BACKUP, null)
    }

    fun setLocalBackupFolder(context: Context, treeUri: Uri): Result<String> {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val name = doc?.name ?: "Khata Backup Folder"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_FOLDER_URI, treeUri.toString())
                .putString(KEY_FOLDER_NAME, name)
                .putBoolean(KEY_SETUP_ASKED, true)
                .apply()

            Log.d(TAG, "Persisted local backup folder: $name ($treeUri)")
            Result.success(name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist folder permission", e)
            Result.failure(e)
        }
    }

    suspend fun performLocalBackup(
        context: Context,
        backupJson: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val treeUri = getLocalBackupFolderUri(context)
            ?: return@withContext Result.failure(IllegalStateException("No local backup folder configured"))

        try {
            val folder = DocumentFile.fromTreeUri(context, treeUri)
            if (folder == null || !folder.exists() || !folder.canWrite()) {
                return@withContext Result.failure(IllegalStateException("Cannot write to selected backup folder. Please re-select the folder in Settings."))
            }

            // Always create a new file every time instead of replacing previous backup
            // Date tag the backup file in yyyymmdd format (e.g. khata_backup_20261005.json)
            val dateTag = DateUtils.getBackupDateTag()
            val baseName = "khata_backup_$dateTag"
            var uniqueFileName = "$baseName.json"
            var counter = 1
            while (folder.findFile(uniqueFileName) != null) {
                uniqueFileName = "${baseName}_$counter.json"
                counter++
            }

            val targetFile = folder.createFile("application/json", uniqueFileName)
                ?: return@withContext Result.failure(IllegalStateException("Could not create backup file in folder"))

            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { os: OutputStream ->
                os.write(backupJson.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val todayStr = DateUtils.getTodayDateString()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_LOCAL_BACKUP, nowStr)
                .putString(KEY_LAST_LOCAL_DATE, todayStr)
                .apply()

            val actualName = targetFile.name ?: uniqueFileName
            Log.d(TAG, "Successfully created new local backup: $actualName")
            Result.success(actualName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed local backup", e)
            Result.failure(e)
        }
    }

    fun isDailyBackupDueToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_LOCAL_DATE, null)
        val todayStr = DateUtils.getTodayDateString()
        return lastDate != todayStr
    }
}
