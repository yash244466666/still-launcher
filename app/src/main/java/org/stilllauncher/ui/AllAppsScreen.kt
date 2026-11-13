package org.stilllauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.AppFolder
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings

@Composable
fun AllAppsScreen(
    viewModel: StillViewModel,
    settings: Settings,
    focusSearch: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetLimit: (String) -> Unit
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var managedFolder by remember { mutableStateOf<AppFolder?>(null) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(focusSearch, settings.autoKeyboard) {
        if (focusSearch && settings.autoKeyboard) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val hidden = settings.hidden.toSet()
    val favoriteSet = settings.favorites.toSet()
    val folderMembers = settings.folders.flatMap { it.packages }.toSet()
    val visibleApps = apps.filter { it.packageName !in hidden }
    val favoriteApps = settings.favorites.mapNotNull { pkg ->
        visibleApps.firstOrNull { it.packageName == pkg }
    }
    val rest = visibleApps.filter {
        it.packageName !in favoriteSet && it.packageName !in folderMembers
    }

    val nameOf: (String) -> String = { viewModel.displayName(it) }

    val filtered = if (query.isBlank()) emptyList() else visibleApps.filter {
        nameOf(it.packageName).contains(query, ignoreCase = true)
    }

    val grouped = rest.groupBy { firstLetter(nameOf(it.packageName)) }
        .toSortedMap(compareBy { if (it == "#") "{" else it })

    ScreenScaffold(
        title = "All apps",
        onBack = onBack,
        trailing = {
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (query.isNotBlank()) {
                if (filtered.isEmpty()) {
                    item { EmptyHint("No apps match \"$query\"") }
                } else {
                    items(filtered, key = { "search-${it.packageName}" }) { app ->
                        AppRow(
                            label = nameOf(app.packageName),
                            onClick = { viewModel.launch(app) },
                            onLongClick = { selected = app }
                        )
                    }
                }
            } else {
                if (favoriteApps.isNotEmpty()) {
                    item { SectionLabel("Favourites") }
                    items(favoriteApps, key = { "fav-${it.packageName}" }) { app ->
                        AppRow(
                            label = nameOf(app.packageName),
                            onClick = { viewModel.launch(app) },
                            onLongClick = { selected = app }
                        )
                    }
                }

                if (settings.folders.isNotEmpty()) {
                    item { SectionLabel("Folders") }
                    settings.folders.forEach { folder ->
                        item(key = "folder-${folder.name}") {
                            AppRow(
                                label = "${folder.name}  (${folder.packages.count { pkg -> visibleApps.any { it.packageName == pkg } }})",
                                onClick = {
                                    expandedFolders = if (folder.name in expandedFolders) {
                                        expandedFolders - folder.name
                                    } else {
                                        expandedFolders + folder.name
                                    }
                                },
                                onLongClick = { managedFolder = folder },
                                expanded = folder.name in expandedFolders
                            )
                        }
                        if (folder.name in expandedFolders) {
                            folder.packages.mapNotNull { pkg ->
                                visibleApps.firstOrNull { it.packageName == pkg }
                            }.forEach { app ->
                                item(key = "folder-${folder.name}-${app.packageName}") {
                                    AppRow(
                                        label = nameOf(app.packageName),
                                        indent = 16,
                                        onClick = { viewModel.launch(app) },
                                        onLongClick = { selected = app }
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionLabel("All apps") }
                grouped.forEach { (letter, list) ->
                    item(key = "header-$letter") {
                        Text(
                            letter,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp)
                        )
                    }
                    items(list, key = { "app-${it.packageName}" }) { app ->
                        AppRow(
                            label = nameOf(app.packageName),
                            onClick = { viewModel.launch(app) },
                            onLongClick = { selected = app }
                        )
                    }
                }
                if (grouped.isEmpty() && favoriteApps.isEmpty() && settings.folders.isEmpty()) {
                    item {
                        EmptyHint(
                            "No launchable apps found. Grant the launcher role and check app visibility settings."
                        )
                    }
                }
            }
        }
    }

    selected?.let { app ->
        AppActionsDialog(
            viewModel = viewModel,
            settings = settings,
            app = app,
            onDismiss = { selected = null },
            onSetLimit = {
                selected = null
                onSetLimit(it)
            }
        )
    }

    managedFolder?.let { folder ->
        FolderMenuDialog(
            folder = folder,
            onDismiss = { managedFolder = null },
            onRename = { newName ->
                viewModel.renameFolder(folder.name, newName)
                managedFolder = null
            },
            onDelete = {
                viewModel.deleteFolder(folder.name)
                managedFolder = null
            }
        )
    }
}

private fun firstLetter(label: String): String {
    val c = label.firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

@Composable
private fun FolderMenuDialog(
    folder: AppFolder,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var mode by remember { mutableStateOf("menu") }
    when (mode) {
        "menu" -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(folder.name) },
            text = {
                Column {
                    TextButton(
                        onClick = { mode = "rename" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename folder", Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = { mode = "delete" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete folder", Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )

        "rename" -> TextInputDialog(
            title = "Rename folder",
            initial = folder.name,
            label = "Folder name",
            onDismiss = onDismiss,
            onConfirm = onRename
        )

        else -> ConfirmDialog(
            title = "Delete folder?",
            message = "The apps in \"${folder.name}\" stay installed and return to the main list.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = onDismiss,
            onConfirm = onDelete
        )
    }
}
