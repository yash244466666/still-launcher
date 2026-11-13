package org.stilllauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.AppFolder
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings

@Composable
fun AppActionsDialog(
    viewModel: StillViewModel,
    settings: Settings,
    app: LauncherApp,
    onDismiss: () -> Unit,
    onSetLimit: (String) -> Unit
) {
    var mode by remember { mutableStateOf<String?>(null) }
    val label = viewModel.displayName(app.packageName)
    val isFavorite = app.packageName in settings.favorites

    when (mode) {
        null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(label) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Action("Remove from favourites", enabled = isFavorite) {
                        viewModel.toggleFavorite(app.packageName)
                        onDismiss()
                    }
                    Action("Add to favourites", enabled = !isFavorite) {
                        viewModel.toggleFavorite(app.packageName)
                        onDismiss()
                    }
                    Action("Daily time limit") {
                        onSetLimit(app.packageName)
                        onDismiss()
                    }
                    Action("Block for a while") {
                        mode = "block"
                    }
                    Action("Move to folder") {
                        mode = "folder"
                    }
                    Action("Rename") {
                        mode = "rename"
                    }
                    Action("Hide from app list") {
                        viewModel.hide(app.packageName)
                        onDismiss()
                    }
                    Action("App info") {
                        viewModel.openAppInfo(app.packageName)
                        onDismiss()
                    }
                    Action("Uninstall", color = MaterialTheme.colorScheme.error) {
                        viewModel.uninstall(app.packageName)
                        onDismiss()
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )

        "block" -> DurationDialog(
            appLabel = label,
            onDismiss = onDismiss,
            onSelect = { millis ->
                viewModel.addBlock(app.packageName, millis)
                onDismiss()
            }
        )

        "rename" -> TextInputDialog(
            title = "Rename app",
            initial = label,
            label = "Display name",
            onDismiss = onDismiss,
            onConfirm = { viewModel.rename(app.packageName, it) }
        )

        "folder" -> FolderPickerDialog(
            folders = settings.folders,
            onDismiss = onDismiss,
            onPick = { folder ->
                viewModel.moveToFolder(app.packageName, folder.name)
                onDismiss()
            },
            onNewFolder = {
                mode = "newfolder"
            }
        )

        "newfolder" -> TextInputDialog(
            title = "New folder",
            initial = "",
            label = "Folder name",
            onDismiss = onDismiss,
            onConfirm = { name ->
                viewModel.createFolder(name, app.packageName)
                onDismiss()
            }
        )
    }
}

@Composable
private fun Action(
    label: String,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            color = if (color == Color.Unspecified) {
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            } else color
        )
    }
}

@Composable
fun FolderPickerDialog(
    folders: List<AppFolder>,
    onDismiss: () -> Unit,
    onPick: (AppFolder) -> Unit,
    onNewFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onPick(folder) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${folder.name} (${folder.packages.size})",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                TextButton(onClick = onNewFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("New folder…", modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
