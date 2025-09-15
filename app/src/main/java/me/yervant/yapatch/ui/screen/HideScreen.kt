package me.yervant.yapatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import me.yervant.yapatch.ui.component.LoadingDialog
import me.yervant.yapatch.ui.viewmodel.HideViewModel
import me.yervant.yapatch.R

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun HideScreen() {
    val viewModel: HideViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadConfig()
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hide_config_title)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (viewModel.isLoading) {
            LoadingDialog()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            InformationSection()
            HideFilesSection(viewModel)
            SpoofUnameSection(viewModel)
        }
    }
}

@Composable
fun InformationSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.hidden_config_title),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(R.string.hidden_config_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.hidden_config_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun HideFilesSection(viewModel: HideViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.hide_files_title),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(R.string.hide_files_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.hide_files_example),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.newHideFile,
                    onValueChange = { viewModel.newHideFile = it },
                    label = { Text(stringResource(R.string.hide_files_input_label)) },
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { viewModel.addHideFile() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
            
            if (viewModel.hideFiles.isNotEmpty()) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                Text(
                    text = stringResource(R.string.currently_hidden_files),
                    style = MaterialTheme.typography.labelMedium
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.hideFiles) { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(file)
                            IconButton(onClick = { viewModel.removeHideFile(file) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoofUnameSection(viewModel: HideViewModel) {
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.spoof_uname_title),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(R.string.spoof_uname_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.uname_fields_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // sysname field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.sysname,
                    onValueChange = { viewModel.sysname = it },
                    label = { Text(stringResource(R.string.sysname_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_SYSNAME, viewModel.sysname) },
                    enabled = viewModel.sysname.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_SYSNAME) },
                    enabled = viewModel.sysname.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_sysname),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_linux),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // nodename field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.nodename,
                    onValueChange = { viewModel.nodename = it },
                    label = { Text(stringResource(R.string.nodename_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_NODENAME, viewModel.nodename) },
                    enabled = viewModel.nodename.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_NODENAME) },
                    enabled = viewModel.nodename.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_nodename),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_nodename),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // release field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.release,
                    onValueChange = { viewModel.release = it },
                    label = { Text(stringResource(R.string.release_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_RELEASE, viewModel.release) },
                    enabled = viewModel.release.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_RELEASE) },
                    enabled = viewModel.release.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_release),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_release),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // version field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.version,
                    onValueChange = { viewModel.version = it },
                    label = { Text(stringResource(R.string.version_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_VERSION, viewModel.version) },
                    enabled = viewModel.version.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_VERSION) },
                    enabled = viewModel.version.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_version),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // machine field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.machine,
                    onValueChange = { viewModel.machine = it },
                    label = { Text(stringResource(R.string.machine_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_MACHINE, viewModel.machine) },
                    enabled = viewModel.machine.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_MACHINE) },
                    enabled = viewModel.machine.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_machine),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_machine),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // domainname field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.domainname,
                    onValueChange = { viewModel.domainname = it },
                    label = { Text(stringResource(R.string.domainname_label)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.saveUnameField(HideViewModel.UNAME_DOMAINNAME, viewModel.domainname) },
                    enabled = viewModel.domainname.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_button))
                }
                IconButton(
                    onClick = { viewModel.clearUnameField(HideViewModel.UNAME_DOMAINNAME) },
                    enabled = viewModel.domainname.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_domainname),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = stringResource(R.string.example_domainname),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
