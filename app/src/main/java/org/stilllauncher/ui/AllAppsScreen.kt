package org.stilllauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

private data class HeaderEntry(
    val title: String,
    override val id: String,
    val description: String? = null,
    val icon: ImageVector? = null
) : ListEntry

private data class LetterEntry(val letter: String) : ListEntry {
    override val id: String = "letter-$letter"
}

private data class AppEntryItem(val app: LauncherApp, override val id: String) : ListEntry

private data class FolderEntry(val folder: AppFolder, val expanded: Boolean) : ListEntry {
    override val id: String = "folder-${folder.name}"
}

private data class FolderChildEntry(val folderName: String, val app: LauncherApp) : ListEntry {
    override val id: String = "folder-$folderName-${app.profileKey}"
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
    val visibleApps = apps.filter { it.profileKey !in hidden }
    val favoriteApps = settings.favorites.mapNotNull { key ->
        visibleApps.firstOrNull { it.profileKey == key }
    }
    val recentApps = settings.recentApps.mapNotNull { rec ->
        visibleApps.firstOrNull { it.profileKey == rec.packageName }
    }.take(5)
    val rest = visibleApps.filter {
        it.profileKey !in favoriteSet && it.profileKey !in folderMembers
    }

    val nameOf: (LauncherApp) -> String = { viewModel.displayName(it) }

    val filtered = if (query.isBlank()) emptyList() else visibleApps.filter {
        nameOf(it).contains(query, ignoreCase = true)
    }

    val grouped = rest.groupBy { firstLetter(nameOf(it)) }
        .toSortedMap(compareBy { if (it == "#") "{" else it })

    val entries = buildList {
        if (query.isBlank()) {
            if (recentApps.isNotEmpty()) {
                add(
                    HeaderEntry(
                        "Recent apps",
                        "header-recent",
                        "Apps you used recently",
                        StillIcons.History
                    )
                )
                recentApps.forEach { add(AppEntryItem(it, "recent-${it.profileKey}")) }
            }
            if (favoriteApps.isNotEmpty()) {
                add(
                    HeaderEntry(
                        "Favourites",
                        "header-favourites",
                        "${favoriteApps.size} app${if (favoriteApps.size == 1) "" else "s"} on your home screen",
                        Icons.Outlined.Star
                    )
                )
                favoriteApps.forEach { add(AppEntryItem(it, "fav-${it.profileKey}")) }
            }
            if (settings.folders.isNotEmpty()) {
                add(
                    HeaderEntry(
                        "Folders",
                        "header-folders",
                        "${settings.folders.size} folder${if (settings.folders.size == 1) "" else "s"}",
                        StillIcons.Folder
                    )
                )
                settings.folders.forEach { folder ->
                    add(FolderEntry(folder, folder.name in expandedFolders))
                    if (folder.name in expandedFolders) {
                        folder.packages.mapNotNull { key ->
                            visibleApps.firstOrNull { it.profileKey == key }
                        }.forEach { app ->
                            add(FolderChildEntry(folder.name, app))
                        }
                    }
                }
            }
            if (rest.isNotEmpty()) {
                add(
                    HeaderEntry(
                        "All apps",
                        "header-all",
                        "${rest.size} app${if (rest.size == 1) "" else "s"} · A to Z",
                        StillIcons.Grid
                    )
                )
                grouped.forEach { (letter, list) ->
                    add(LetterEntry(letter))
                    list.forEach { add(AppEntryItem(it, "all-${it.profileKey}")) }
                }
            }
        }
    }

    val letters = entries.mapIndexedNotNull { index, entry ->
        if (entry is LetterEntry) LetterTarget(entry.letter, index) else null
    }
    val showIndex = entries.isNotEmpty() && letters.size >= 3
    val currentLetter by remember(letters) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            val firstVisible = visible.firstOrNull()?.index ?: 0
            letters.lastOrNull { it.index <= firstVisible }?.letter
                ?: letters.firstOrNull { target -> visible.any { it.index >= target.index } }?.letter
        }
    }
    var dragLetter by remember { mutableStateOf<String?>(null) }
    var bubbleLetter by remember { mutableStateOf("") }

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
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
        )
        if (query.isBlank()) {
            HintRow(
                icon = Icons.Outlined.Info,
                text = "Tap to open · long-press for favourite, limits and more"
            )
        }

        if (query.isNotBlank()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "No apps match",
                            body = "Try a different name, or clear the search to see every app."
                        )
                    }
                } else {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(filtered, key = { "search-${it.profileKey}" }) { app ->
                        AppRow(
                            label = nameOf(app),
                            onClick = { viewModel.launch(app) },
                            onLongClick = { selected = app },
                            showChevron = true
                        )
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (showIndex) 36.dp else 0.dp,
                        end = if (showIndex) 36.dp else 0.dp,
                        bottom = 24.dp
                    )
                ) {
                    items(entries, key = { it.id }) { entry ->
                        when (entry) {
                            is HeaderEntry -> SectionHeader(
                                entry.title,
                                entry.description,
                                entry.icon
                            )

                            is LetterEntry -> Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.letter,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }

                            is AppEntryItem -> AppRow(
                                label = nameOf(entry.app),
                                onClick = { viewModel.launch(entry.app) },
                                onLongClick = { selected = entry.app },
                                showChevron = true
                            )

                            is FolderEntry -> {
                                val count = entry.folder.packages.count { pkg ->
                                    visibleApps.any { it.profileKey == pkg }
                                }
                                FolderRow(
                                    name = entry.folder.name,
                                    count = count,
                                    expanded = entry.expanded,
                                    onClick = {
                                        expandedFolders = if (entry.folder.name in expandedFolders) {
                                            expandedFolders - entry.folder.name
                                        } else {
                                            expandedFolders + entry.folder.name
                                        }
                                    },
                                    onLongClick = { managedFolder = entry.folder }
                                )
                            }

                            is FolderChildEntry -> AppRow(
                                label = nameOf(entry.app),
                                indent = 16,
                                onClick = { viewModel.launch(entry.app) },
                                onLongClick = { selected = entry.app },
                                showChevron = true
                            )
                        }
                    }
                    if (entries.isEmpty()) {
                        item {
                            EmptyState(
                                icon = StillIcons.Grid,
                                title = "No launchable apps found",
                                body = "Grant the launcher role and check app visibility settings."
                            )
                        }
                    }
                }

                if (showIndex) {
                    val highlighted = dragLetter ?: currentLetter
                    AlphabetBar(
                        letters = letters,
                        highlightedLetter = highlighted,
                        onDragLetter = {
                            dragLetter = it
                            if (it != null) bubbleLetter = it
                        },
                        onSelect = { index, animate ->
                            scope.launch {
                                if (animate) listState.animateScrollToItem(index)
                                else listState.scrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(32.dp)
                            .padding(vertical = 8.dp)
                    )
                    AlphabetBar(
                        letters = letters,
                        highlightedLetter = highlighted,
                        onDragLetter = {
                            dragLetter = it
                            if (it != null) bubbleLetter = it
                        },
                        onSelect = { index, animate ->
                            scope.launch {
                                if (animate) listState.animateScrollToItem(index)
                                else listState.scrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(32.dp)
                            .padding(vertical = 8.dp)
                    )
                }

                LetterBubble(
                    visible = dragLetter != null,
                    letter = bubbleLetter,
                    modifier = Modifier.align(Alignment.Center)
                )
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
private fun LetterBubble(
    visible: Boolean,
    letter: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.75f),
        exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.75f)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                letter,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AlphabetBar(
    letters: List<LetterTarget>,
    highlightedLetter: String?,
    onDragLetter: (String?) -> Unit,
    onSelect: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(letters) {
                awaitEachGesture {
                    fun targetAt(y: Float): LetterTarget? {
                        val fraction = (y / heightPx).coerceIn(0f, 0.999f)
                        return letters.getOrNull((fraction * letters.size).toInt())
                    }

                    val down = awaitFirstDown(requireUnconsumed = false)
                    var last = targetAt(down.position.y) ?: return@awaitEachGesture
                    var moved = false
                    onDragLetter(last.letter)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        if (change.positionChange().getDistance() > 2f) moved = true
                        val target = targetAt(change.position.y)
                        if (target != null && target.letter != last.letter) {
                            last = target
                            onDragLetter(target.letter)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (moved) onSelect(target.index, false)
                        }
                        change.consume()
                    }

                    if (!moved) onSelect(last.index, true)
                    onDragLetter(null)
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { target ->
                val selected = target.letter == highlightedLetter
                val color by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        idleColor
                    },
                    animationSpec = tween(140),
                    label = "alphabetLetterColor"
                )
                val background by animateColorAsState(
                    targetValue = if (selected) activeColor else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = tween(160),
                    label = "alphabetLetterBackground"
                )
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.35f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "alphabetLetterScale"
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .background(background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        target.letter,
                        fontSize = if (letters.size > 20) 9.sp else 10.sp,
                        color = color,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
