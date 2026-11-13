package org.stilllauncher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.stilllauncher.StillViewModel
import org.stilllauncher.data.AppFolder
import org.stilllauncher.data.LauncherApp
import org.stilllauncher.data.Settings

private sealed interface ListEntry {
    val id: String
}

private data class HeaderEntry(val title: String, override val id: String) : ListEntry

private data class LetterEntry(val letter: String) : ListEntry {
    override val id: String = "letter-$letter"
}

private data class AppEntryItem(val app: LauncherApp, override val id: String) : ListEntry

private data class FolderEntry(val folder: AppFolder, val expanded: Boolean) : ListEntry {
    override val id: String = "folder-${folder.name}"
}

private data class FolderChildEntry(val folderName: String, val app: LauncherApp) : ListEntry {
    override val id: String = "folder-$folderName-${app.packageName}"
}

private data class LetterTarget(val letter: String, val index: Int)

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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var managedFolder by remember { mutableStateOf<AppFolder?>(null) }
    val focusRequester = remember { FocusRequester() }

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
    val recentApps = settings.recentApps.mapNotNull { rec ->
        visibleApps.firstOrNull { it.packageName == rec.packageName }
    }.take(5)
    val rest = visibleApps.filter {
        it.packageName !in favoriteSet && it.packageName !in folderMembers
    }

    val nameOf: (String) -> String = { viewModel.displayName(it) }

    val filtered = if (query.isBlank()) emptyList() else visibleApps.filter {
        nameOf(it.packageName).contains(query, ignoreCase = true)
    }

    val grouped = rest.groupBy { firstLetter(nameOf(it.packageName)) }
        .toSortedMap(compareBy { if (it == "#") "{" else it })

    val entries = buildList {
        if (query.isBlank()) {
            if (recentApps.isNotEmpty()) {
                add(HeaderEntry("Recent apps", "header-recent"))
                recentApps.forEach { add(AppEntryItem(it, "recent-${it.packageName}")) }
            }
            if (favoriteApps.isNotEmpty()) {
                add(HeaderEntry("Favourites", "header-favourites"))
                favoriteApps.forEach { add(AppEntryItem(it, "fav-${it.packageName}")) }
            }
            if (settings.folders.isNotEmpty()) {
                add(HeaderEntry("Folders", "header-folders"))
                settings.folders.forEach { folder ->
                    add(FolderEntry(folder, folder.name in expandedFolders))
                    if (folder.name in expandedFolders) {
                        folder.packages.mapNotNull { pkg ->
                            visibleApps.firstOrNull { it.packageName == pkg }
                        }.forEach { app ->
                            add(FolderChildEntry(folder.name, app))
                        }
                    }
                }
            }
            if (rest.isNotEmpty()) {
                add(HeaderEntry("All apps", "header-all"))
                grouped.forEach { (letter, list) ->
                    add(LetterEntry(letter))
                    list.forEach { add(AppEntryItem(it, "all-${it.packageName}")) }
                }
            }
        }
    }

    val letters = entries.mapIndexedNotNull { index, entry ->
        if (entry is LetterEntry) LetterTarget(entry.letter, index) else null
    }

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

        if (query.isNotBlank()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
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
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        when (entry) {
                            is HeaderEntry -> SectionLabel(entry.title)

                            is LetterEntry -> Text(
                                entry.letter,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp)
                            )

                            is AppEntryItem -> AppRow(
                                label = nameOf(entry.app.packageName),
                                onClick = { viewModel.launch(entry.app) },
                                onLongClick = { selected = entry.app }
                            )

                            is FolderEntry -> {
                                val count = entry.folder.packages.count { pkg ->
                                    visibleApps.any { it.packageName == pkg }
                                }
                                AppRow(
                                    label = "${entry.folder.name}  ($count)",
                                    onClick = {
                                        expandedFolders = if (entry.folder.name in expandedFolders) {
                                            expandedFolders - entry.folder.name
                                        } else {
                                            expandedFolders + entry.folder.name
                                        }
                                    },
                                    onLongClick = { managedFolder = entry.folder },
                                    expanded = entry.expanded
                                )
                            }

                            is FolderChildEntry -> AppRow(
                                label = nameOf(entry.app.packageName),
                                indent = 16,
                                onClick = { viewModel.launch(entry.app) },
                                onLongClick = { selected = entry.app }
                            )
                        }
                    }
                    if (entries.isEmpty()) {
                        item {
                            EmptyHint(
                                "No launchable apps found. Grant the launcher role and check app visibility settings."
                            )
                        }
                    }
                }

                if (entries.isNotEmpty() && letters.size >= 3) {
                    AlphabetBar(
                        letters = letters,
                        onSelect = { index ->
                            scope.launch { listState.scrollToItem(index) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(34.dp)
                            .padding(vertical = 8.dp)
                    )
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
private fun AlphabetBar(
    letters: List<LetterTarget>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf<LetterTarget?>(null) }
    var heightPx by remember { mutableStateOf(1f) }

    Box(modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(letters) {
                    awaitEachGesture {
                        fun pick(y: Float) {
                            val fraction = (y / heightPx).coerceIn(0f, 0.999f)
                            val target = letters.getOrNull((fraction * letters.size).toInt())
                                ?: return
                            if (active?.letter != target.letter) {
                                active = target
                                onSelect(target.index)
                            }
                        }

                        val down = awaitFirstDown(requireUnconsumed = false)
                        pick(down.position.y)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            pick(change.position.y)
                            change.consume()
                        }
                        active = null
                    }
                },
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { target ->
                Text(
                    target.letter,
                    fontSize = if (letters.size > 20) 9.sp else 10.sp,
                    color = if (active?.letter == target.letter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        active?.let { target ->
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    target.letter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
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
