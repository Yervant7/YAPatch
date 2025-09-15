package me.yervant.yapatch.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.yervant.yapatch.Natives
import me.yervant.yapatch.apApp
import me.yervant.yapatch.util.getRootShell
import java.io.File
import java.io.IOException

class HideViewModel : ViewModel() {
    companion object {
        private const val TAG = "HideViewModel"

        // Uname field constants
        const val UNAME_SYSNAME = 0x1001
        const val UNAME_NODENAME = 0x1002
        const val UNAME_RELEASE = 0x1003
        const val UNAME_VERSION = 0x1004
        const val UNAME_MACHINE = 0x1005
        const val UNAME_DOMAINNAME = 0x1006

        private val APP_FILES_DIR: File = apApp.filesDir
        val UNAME_CONFIG_FILE = File(APP_FILES_DIR, ".uname_config")
        val HIDE_FILES_CONFIG_FILE = File(APP_FILES_DIR, ".hide_files")

        private fun escapeShellArg(arg: String): String {
            return arg.replace("'", """'\''""")
        }
    }

    // Hide files
    val hideFiles = mutableStateListOf<String>()
    var newHideFile by mutableStateOf("")

    // Spoof uname fields
    var sysname by mutableStateOf("")
    var nodename by mutableStateOf("")
    var release by mutableStateOf("")
    var version by mutableStateOf("")
    var machine by mutableStateOf("")
    var domainname by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        if (!APP_FILES_DIR.exists()) {
            if (APP_FILES_DIR.mkdirs()) {
                Log.i(TAG, "Application files directory created: ${APP_FILES_DIR.absolutePath}")
            } else {
                Log.e(TAG, "Failed to create application files directory: ${APP_FILES_DIR.absolutePath}")
            }
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val (hideFilesData, unameData) = withContext(Dispatchers.IO) {
                    loadHideFilesConfigData() to loadUnameConfigData()
                }

                hideFiles.clear()
                hideFiles.addAll(hideFilesData)
                sysname = unameData.sysname
                nodename = unameData.nodename
                release = unameData.release
                version = unameData.version
                machine = unameData.machine
                domainname = unameData.domainname

                Log.d(TAG, "Configuration loaded and state updated.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configuration", e)
                errorMessage = "Failed to load configuration: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadHideFilesConfigData(): List<String> {
        val file = HIDE_FILES_CONFIG_FILE
        try {
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        Log.i(TAG, "Created new hide_files config: ${file.absolutePath}")
                    } else {
                        Log.w(TAG, "createNewFile for hide_files config returned false, though file reported as not existing initially.")
                    }
                    return emptyList()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException creating hide_files config '${file.absolutePath}': ${e.message}", e)
                    return emptyList() // Failed to create
                }
            }

            if (!file.canRead()) {
                Log.e(TAG, "Cannot read hide_files config (check permissions): ${file.absolutePath}")
                return emptyList()
            }

            val hideFilesL = mutableListOf<String>()
            file.forEachLine { line ->
                if (line.isNotBlank()) {
                    hideFilesL.add(line.trim())
                }
            }
            return hideFilesL
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading hide_files config '${file.absolutePath}': ${e.message}", e)
            return emptyList()
        }
    }

    private data class UnameConfig(
        val sysname: String = "",
        val nodename: String = "",
        val release: String = "",
        val version: String = "",
        val machine: String = "",
        val domainname: String = ""
    )

    private fun loadUnameConfigData(): UnameConfig {
        val file = UNAME_CONFIG_FILE
        try {
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        Log.i(TAG, "Created new uname_config: ${file.absolutePath}")
                    } else {
                        Log.w(TAG, "createNewFile for uname_config returned false, though file reported as not existing initially.")
                    }
                    return UnameConfig()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException creating uname_config '${file.absolutePath}': ${e.message}", e)
                    return UnameConfig()
                }
            }

            if (!file.canRead()) {
                Log.e(TAG, "Cannot read uname_config (check permissions): ${file.absolutePath}")
                return UnameConfig()
            }

            var config = UnameConfig()
            file.forEachLine { line ->
                if (line.isNotBlank() && line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        config = when (key) {
                            "sysname" -> config.copy(sysname = value)
                            "nodename" -> config.copy(nodename = value)
                            "release" -> config.copy(release = value)
                            "version" -> config.copy(version = value)
                            "machine" -> config.copy(machine = value)
                            "domainname" -> config.copy(domainname = value)
                            else -> config
                        }
                    }
                }
            }
            return config
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading uname_config '${file.absolutePath}': ${e.message}", e)
            return UnameConfig()
        }
    }

    fun addHideFile() {
        val fileToAdd = newHideFile.trim()
        if (fileToAdd.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = Natives.androidHideFilesAdd(fileToAdd)
                    if (result == 0) {
                        withContext(Dispatchers.Main) {
                            if (!hideFiles.contains(fileToAdd)) {
                                hideFiles.add(fileToAdd)
                            }
                            newHideFile = ""
                        }
                        saveHideFilesConfig()
                        Log.d(TAG, "Hide file added: $fileToAdd")
                    } else {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Failed to add hide file (error code: $result)"
                        }
                        Log.e(TAG, "Failed to add hide file: $fileToAdd, error code: $result")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to add hide file: ${e.message}"
                    }
                    Log.e(TAG, "Exception while adding hide file", e)
                }
            }
        }
    }

    fun removeHideFile(file: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = Natives.androidHideFilesRemove(file)
                if (result == 0) {
                    withContext(Dispatchers.Main) {
                        hideFiles.remove(file)
                    }
                    saveHideFilesConfig()
                    Log.d(TAG, "Hide file removed: $file")
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to remove hide file (error code: $result)"
                    }
                    Log.e(TAG, "Failed to remove hide file: $file, error code: $result")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to remove hide file: ${e.message}"
                }
                Log.e(TAG, "Exception while removing hide file", e)
            }
        }
    }

    private fun saveHideFilesConfig() {
        val filePath = HIDE_FILES_CONFIG_FILE.absolutePath
        val appUid = apApp.applicationInfo.uid
        try {
            val shell = getRootShell()
            val commands = mutableListOf<String>()

            if (hideFiles.isEmpty()) {
                commands.add("echo -n > '${escapeShellArg(filePath)}'")
            } else {
                commands.add("echo '${escapeShellArg(hideFiles.first())}' > '${escapeShellArg(filePath)}'")
                hideFiles.drop(1).forEach { fileEntry ->
                    commands.add("echo '${escapeShellArg(fileEntry)}' >> '${escapeShellArg(filePath)}'")
                }
            }

            commands.add("chown $appUid:$appUid '${escapeShellArg(filePath)}'")
            commands.add("chmod 600 '${escapeShellArg(filePath)}'")

            shell.newJob().add(*commands.toTypedArray()).exec()
            Log.d(TAG, "Hide files config saved to $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save hide files config to $filePath", e)
            viewModelScope.launch(Dispatchers.Main) {
                errorMessage = "Failed to save hide files config: ${e.message}"
            }
        }
    }

    fun saveUnameField(field: Int, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resultCode = Natives.androidSpoofUnameSet(field, value)
                if (resultCode == 0) {
                    withContext(Dispatchers.Main) {
                        when (field) {
                            UNAME_SYSNAME -> sysname = value
                            UNAME_NODENAME -> nodename = value
                            UNAME_RELEASE -> release = value
                            UNAME_VERSION -> version = value
                            UNAME_MACHINE -> machine = value
                            UNAME_DOMAINNAME -> domainname = value
                        }
                    }
                    saveUnameConfigToFile()
                    Log.d(TAG, "Uname field saved successfully")
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to save uname field (error code: $resultCode)"
                    }
                    Log.e(TAG, "Failed to save uname field, error code: $resultCode")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to save uname field: ${e.message}"
                }
                Log.e(TAG, "Exception while saving uname field", e)
            }
        }
    }

    fun clearUnameField(field: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val valueToRemove = when (field) {
                    UNAME_SYSNAME -> sysname
                    UNAME_NODENAME -> nodename
                    UNAME_RELEASE -> release
                    UNAME_VERSION -> version
                    UNAME_MACHINE -> machine
                    UNAME_DOMAINNAME -> domainname
                    else -> ""
                }

                val resultCode = Natives.androidSpoofUnameRemove(field)
                if (resultCode == 0) {
                    val updateFile = valueToRemove.isNotEmpty()
                    withContext(Dispatchers.Main) {
                        when (field) {
                            UNAME_SYSNAME -> sysname = ""
                            UNAME_NODENAME -> nodename = ""
                            UNAME_RELEASE -> release = ""
                            UNAME_VERSION -> version = ""
                            UNAME_MACHINE -> machine = ""
                            UNAME_DOMAINNAME -> domainname = ""
                        }
                    }
                    if (updateFile) {
                        saveUnameConfigToFile()
                    }
                    Log.d(TAG, "Uname field cleared successfully")
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to clear uname field (error code: $resultCode)"
                    }
                    Log.e(TAG, "Failed to clear uname field, error code: $resultCode")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to clear uname field: ${e.message}"
                }
                Log.e(TAG, "Exception while clearing uname field", e)
            }
        }
    }

    private fun saveUnameConfigToFile() {
        val filePath = UNAME_CONFIG_FILE.absolutePath
        val appUid = apApp.applicationInfo.uid
        try {
            val shell = getRootShell()
            val commands = arrayOf(
                "echo 'sysname=${escapeShellArg(sysname)}' > '${escapeShellArg(filePath)}'",
                "echo 'nodename=${escapeShellArg(nodename)}' >> '${escapeShellArg(filePath)}'",
                "echo 'release=${escapeShellArg(release)}' >> '${escapeShellArg(filePath)}'",
                "echo 'version=${escapeShellArg(version)}' >> '${escapeShellArg(filePath)}'",
                "echo 'machine=${escapeShellArg(machine)}' >> '${escapeShellArg(filePath)}'",
                "echo 'domainname=${escapeShellArg(domainname)}' >> '${escapeShellArg(filePath)}'",
                "chown $appUid:$appUid '${escapeShellArg(filePath)}'",
                "chmod 600 '${escapeShellArg(filePath)}'"
            )
            shell.newJob().add(*commands).exec()
            Log.d(TAG, "Uname config saved to file $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save uname config to file $filePath", e)
            viewModelScope.launch(Dispatchers.Main) {
                errorMessage = "Failed to save uname config: ${e.message}"
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
