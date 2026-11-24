package com.inkcraft.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkcraft.app.model.CoverTheme
import com.inkcraft.app.model.Folder
import com.inkcraft.app.model.InkWorkspace
import com.inkcraft.app.model.Notebook
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    workspace: InkWorkspace,
    onOpenNotebook: (Notebook) -> Unit,
    onCreateNotebook: (title: String, folderId: String?, cover: CoverTheme, template: PaperTemplate, paperColor: PaperColor) -> Unit,
    onQuickNote: () -> Unit,
    onDeleteNotebook: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExportPdf: (Notebook) -> Unit,
    onImportPdfAsNotebook: () -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, FAVORITES, or folderId
    var searchQuery by remember { mutableStateOf("") }
    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val filteredNotebooks = remember(workspace.notebooks, selectedFilter, searchQuery) {
        workspace.notebooks.filter { nb ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "FAVORITES" -> nb.isFavorite
                else -> nb.folderId == selectedFilter
            }
            val matchesSearch = searchQuery.isBlank() ||
                nb.title.contains(searchQuery, ignoreCase = true) ||
                nb.tags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesFilter && matchesSearch
        }.sortedByDescending { it.updatedAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Draw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("InkCraft", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("Tablet Stylus Studio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search notebooks...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .width(260.dp)
                            .padding(end = 12.dp)
                    )
                    FilledTonalButton(
                        onClick = onImportPdfAsNotebook,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import PDF")
                    }
                    Button(
                        onClick = onQuickNote,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Quick Note")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewNotebookDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Notebook")
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left Tablet Sidebar
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text(
                            "LIBRARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Book, contentDescription = null) },
                            label = { Text("All Notebooks (${workspace.notebooks.size})") },
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" }
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            label = { Text("Favorites") },
                            selected = selectedFilter == "FAVORITES",
                            onClick = { selectedFilter = "FAVORITES" }
                        )
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "FOLDERS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { showNewFolderDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    items(workspace.folders) { folder ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color(folder.colorArgb)
                                )
                            },
                            label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = selectedFilter == folder.id,
                            onClick = { selectedFilter = folder.id }
                        )
                    }
                }
            }

            // Main Notebook Bookshelf Grid
            if (filteredNotebooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No notebooks here yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showNewNotebookDialog = true }) {
                            Text("Create Notebook")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        NotebookCoverCard(
                            notebook = notebook,
                            onClick = { onOpenNotebook(notebook) },
                            onToggleFavorite = { onToggleFavorite(notebook.id) },
                            onExportPdf = { onExportPdf(notebook) },
                            onDelete = { onDeleteNotebook(notebook.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNewNotebookDialog) {
        NewNotebookDialog(
            folders = workspace.folders,
            initialFolderId = selectedFilter.takeIf { it != "ALL" && it != "FAVORITES" },
            onDismiss = { showNewNotebookDialog = false },
            onCreate = { title, folderId, cover, template, paperColor ->
                onCreateNotebook(title, folderId, cover, template, paperColor)
                showNewNotebookDialog = false
            }
        )
    }

    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            onCreateFolder(folderName.trim())
                        }
                        showNewFolderDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotebookCoverCard(
    notebook: Notebook,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cover = notebook.coverTheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.74f)
                .shadow(8.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(cover.primaryArgb), Color(cover.primaryArgb).copy(alpha = 0.88f))
                    )
                )
        ) {
            // Book Spine
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(Color(cover.spineArgb))
            )

            // Accent Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 36.dp)
                    .height(6.dp)
                    .background(Color(cover.accentArgb))
            )

            // Favorite & Menu
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (notebook.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (notebook.isFavorite) Color(0xFFFBBF24) else Color(cover.textArgb).copy(alpha = 0.7f)
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(cover.textArgb)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Export as PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExportPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Notebook", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Title Plate
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp, end = 16.dp)
            ) {
                Text(
                    text = notebook.title,
                    color = Color(cover.textArgb),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${notebook.pages.size} ${if (notebook.pages.size == 1) "page" else "pages"} • ${notebook.defaultTemplate.displayName}",
                    color = Color(cover.textArgb).copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun NewNotebookDialog(
    folders: List<Folder>,
    initialFolderId: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String?, CoverTheme, PaperTemplate, PaperColor) -> Unit
) {
    var title by remember { mutableStateOf("Untitled Notebook") }
    var selectedFolderId by remember { mutableStateOf(initialFolderId) }
    var selectedCover by remember { mutableStateOf(CoverTheme.LEATHER_NAVY) }
    var selectedTemplate by remember { mutableStateOf(PaperTemplate.RULED_WIDE) }
    var selectedPaperColor by remember { mutableStateOf(PaperColor.CREAM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Notebook", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cover Style", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CoverTheme.entries) { cover ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(cover.primaryArgb))
                                .border(
                                    width = if (cover == selectedCover) 3.dp else 1.dp,
                                    color = if (cover == selectedCover) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedCover = cover }
                        )
                    }
                }

                Text("Paper Template", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaperTemplate.entries) { tpl ->
                        val selected = tpl == selectedTemplate
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { selectedTemplate = tpl }
                        ) {
                            Text(
                                tpl.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Text("Paper Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaperColor.entries.forEach { pc ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(pc.bgArgb))
                                .border(
                                    width = if (pc == selectedPaperColor) 3.dp else 1.dp,
                                    color = if (pc == selectedPaperColor) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedPaperColor = pc }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onCreate(title.ifBlank { "Untitled Notebook" }, selectedFolderId, selectedCover, selectedTemplate, selectedPaperColor)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
