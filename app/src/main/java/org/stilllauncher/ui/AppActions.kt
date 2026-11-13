@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.stilllauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.AppFolder
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings

private data class AppAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun AppActionsDialog(
    viewModel: StillViewModel,
    settings: Settings,
    app: LauncherApp,
    onDismiss: () -> Unit,
    onSetLimit: (String) -> Unit
) {
    var mode by remember { mutableStateOf<String?>(null) }
    val label = viewModel.displayName(app)
    val isFavorite = app.profileKey in settings.favorites
    val isPrimaryProfile = app.isCurrentProfile
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppMonogram(label, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (isPrimaryProfile) {
                            "What would you like to do?"
                        } else {
                            "Secondary profile · blocks apply to your main profile only"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isPrimaryProfile) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "This is a cloned or work-profile app. Favourites, folders, rename and hide work here. Blocks and daily limits can only run in your main profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            val actions = buildList {
                add(
                    AppAction("Open", Icons.Outlined.PlayArrow) {
                        viewModel.launch(app)
                        onDismiss()
                    }
                )
                add(
                    AppAction(
                        if (isFavorite) "Unfavourite" else "Favourite",
                        if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder
                    ) {
                        viewModel.toggleFavorite(app.profileKey)
                        onDismiss()
                    }
                )
                if (isPrimaryProfile) {
                    add(
                        AppAction("Daily limit", StillIcons.Timer) {
                            onSetLimit(app.packageName)
                            onDismiss()
                        }
                    )
                    add(
                        AppAction("Block for a while", Icons.Outlined.Lock) {
                            mode = "block"
                        }
                    )
                }
                add(
                    AppAction("Move to folder", StillIcons.Folder) {
                        mode = "folder"
                    }
                )
                add(
                    AppAction("Rename", Icons.Outlined.Edit) {
                        mode = "rename"
                    }
                )
                add(
                    AppAction("Hide from list", StillIcons.EyeOff) {
                        viewModel.hide(app.profileKey)
                        onDismiss()
                    }
                )
                add(
                    AppAction("App info", Icons.Outlined.Info) {
                        viewModel.openAppInfo(app)
                        onDismiss()
                    }
                )
                if (isPrimaryProfile) {
                    add(
                        AppAction("Uninstall", Icons.Outlined.Delete, destructive = true) {
                            viewModel.uninstall(app)
                            onDismiss()
                        }
                    )
                }
            }

            actions.chunked(3).forEachIndexed { index, rowActions ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowActions.forEach { action ->
                        ActionTile(action)
                    }
                    repeat(3 - rowActions.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    when (mode) {
        "block" -> DurationDialog(
            appLabel = label,
            onDismiss = { mode = null },
            onSelect = { millis ->
                viewModel.addBlock(app.profileKey, millis)
                onDismiss()
            }
        )

        "rename" -> TextInputDialog(
            title = "Rename app",
            initial = label,
            label = "Display name",
            onDismiss = { mode = null },
            onConfirm = { viewModel.rename(app.profileKey, it) }
        )

        "folder" -> FolderPickerDialog(
            folders = settings.folders,
            onDismiss = { mode = null },
            onPick = { folder ->
                viewModel.moveToFolder(app.profileKey, folder.name)
                onDismiss()
            },
            onNewFolder = { mode = "newfolder" }
        )

        "newfolder" -> TextInputDialog(
            title = "New folder",
            initial = "",
            label = "Folder name",
            onDismiss = { mode = null },
            onConfirm = { name ->
                viewModel.createFolder(name, app.profileKey)
                onDismiss()
            }
        )
    }
}

@Composable
private fun RowScope.ActionTile(action: AppAction) {
    Surface(
        onClick = action.onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                tint = if (action.destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                action.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (action.destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
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
                if (folders.isEmpty()) {
                    Text(
                        "No folders yet. Create one to group related apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    folders.forEach { folder ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(folder) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconTile(StillIcons.Folder)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${folder.packages.size} app(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onNewFolder() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("New folder", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
