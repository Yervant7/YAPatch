package me.yervant.yapatch.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.yervant.yapatch.APApplication
import me.yervant.yapatch.Natives
import me.yervant.yapatch.util.getRootShell
import java.io.File

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
        
        // Config file paths
        const val HIDE_FILES_CONFIG = "${APApplication.YAPatch_FOLDER}hide_files"
        const val UNAME_CONFIG = "${APApplication.YAPatch_FOLDER}uname_config"
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
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val hideFilesData = withContext(Dispatchers.IO) {
                    loadHideFilesConfigData()
                }
                withContext(Dispatchers.Main) {
                    hideFiles.clear()
                    hideFiles.addAll(hideFilesData)
                }

                // Carregar configuração uname
                val unameData = withContext(Dispatchers.IO) {
                    loadUnameConfigData()
                }
                withContext(Dispatchers.Main) {
                    sysname = unameData.sysname
                    nodename = unameData.nodename
                    release = unameData.release
                    version = unameData.version
                    machine = unameData.machine
                    domainname = unameData.domainname
                }

                withContext(Dispatchers.IO) {
                    Natives.androidSpoofUnameSet(UNAME_SYSNAME, unameData.sysname)
                    Natives.androidSpoofUnameSet(UNAME_NODENAME, unameData.nodename)
                    Natives.androidSpoofUnameSet(UNAME_RELEASE, unameData.release)
                    Natives.androidSpoofUnameSet(UNAME_VERSION, unameData.version)
                    Natives.androidSpoofUnameSet(UNAME_MACHINE, unameData.machine)
                    Natives.androidSpoofUnameSet(UNAME_DOMAINNAME, unameData.domainname)
                }

                Log.d(TAG, "Configuration loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configuration", e)
                errorMessage = "Failed to load configuration: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadHideFilesConfigData(): List<String> {
        return try {
            val shell = getRootShell()
            val result = shell.newJob().add("cat $HIDE_FILES_CONFIG").exec()
            if (result.isSuccess) {
                result.out.filter { it.isNotBlank() }.map { it.trim() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load hide files config", e)
            emptyList()
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
        return try {
            val shell = getRootShell()
            val result = shell.newJob().add("cat $UNAME_CONFIG").exec()
            var config = UnameConfig()
            if (result.isSuccess) {
                result.out.filter { it.isNotBlank() && it.contains("=") }.forEach { line ->
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
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load uname config from file", e)
            UnameConfig()
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
                            hideFiles.add(fileToAdd)
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
        try {
            val shell = getRootShell()
            // Create the config file with all hide files
            val commands = mutableListOf<String>()
            commands.add("echo -n > $HIDE_FILES_CONFIG") // Clear the file
            hideFiles.forEach { file ->
                commands.add("echo '$file' >> $HIDE_FILES_CONFIG")
            }
            shell.newJob().add(*commands.toTypedArray()).exec()
            Log.d(TAG, "Hide files config saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save hide files config", e)
            errorMessage = "Failed to save hide files config: ${e.message}"
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
                val resultCode = Natives.androidSpoofUnameRemove(field)
                if (resultCode == 0) {
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
                    saveUnameConfigToFile()
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
        try {
            val shell = getRootShell()
            // Create the uname config file
            val configContent = """
                sysname=$sysname
                nodename=$nodename
                release=$release
                version=$version
                machine=$machine
                domainname=$domainname
            """.trimIndent()
            
            val commands = arrayOf(
                "echo '$configContent' > $UNAME_CONFIG"
            )
            shell.newJob().add(*commands).exec()
            Log.d(TAG, "Uname config saved to file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save uname config to file", e)
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
