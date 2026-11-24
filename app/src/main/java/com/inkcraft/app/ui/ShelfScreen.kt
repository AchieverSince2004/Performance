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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
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
    var selectedFilter by remember { mutableStateOf("ALL") }
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

    val currentSectionTitle = when (selectedFilter) {
        "ALL" -> "All Notebooks"
        "FAVORITES" -> "Favorites"
        else -> workspace.folders.find { it.id == selectedFilter }?.name ?: "Folder"
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // macOS Finder-style Frosted Left Sidebar
        Column(
            modifier = Modifier
                .width(256.dp)
                .fillMaxHeight()
                .background(Color(0xFFF0F0F5))
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            // macOS Traffic-Light Window Dots + Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF5F57)))
                Spacer(Modifier.width(7.dp))
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFEBC2E)))
                Spacer(Modifier.width(7.dp))
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF28C840)))
                Spacer(Modifier.width(14.dp))
                Text(
                    "InkCraft",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "LIBRARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )

            MacSidebarItem(
                icon = Icons.Default.Book,
                label = "All Notebooks",
                badge = workspace.notebooks.size.toString(),
                selected = selectedFilter == "ALL",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = { selectedFilter = "ALL" }
            )
            MacSidebarItem(
                icon = Icons.Default.Star,
                label = "Favorites",
                badge = workspace.notebooks.count { it.isFavorite }.toString(),
                selected = selectedFilter == "FAVORITES",
                iconTint = Color(0xFFFF9500),
                onClick = { selectedFilter = "FAVORITES" }
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FOLDERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(workspace.folders) { folder ->
                    MacSidebarItem(
                        icon = Icons.Default.Folder,
                        label = folder.name,
                        badge = workspace.notebooks.count { it.folderId == folder.id }.toString(),
                        selected = selectedFilter == folder.id,
                        iconTint = Color(folder.colorArgb),
                        onClick = { selectedFilter = folder.id }
                    )
                }
            }

            // Bottom macOS-style New Notebook pill button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showNewNotebookDialog = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Notebook", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 1dp Hairline Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        )

        // Main Content Pane
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // macOS Unified Toolbar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        currentSectionTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${filteredNotebooks.size} ${if (filteredNotebooks.size == 1) "notebook" else "notebooks"} • Apple-grade Stylus Studio",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // macOS Search Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(220.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Import PDF Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onImportPdfAsNotebook)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import PDF", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Quick Note Primary Pill
                    Button(
                        onClick = onQuickNote,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Quick Note", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

            if (filteredNotebooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No notebooks in $currentSectionTitle", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap + New Notebook or Quick Note to start writing", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 196.dp),
                    contentPadding = PaddingValues(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        MacNotebookCard(
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
            title = { Text("New Folder", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (folderName.isNotBlank()) onCreateFolder(folderName.trim())
                    showNewFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MacSidebarItem(
    icon: ImageVector,
    label: String,
    badge: String,
    selected: Boolean,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFE0E0E8) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            badge,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacNotebookCard(
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
                .aspectRatio(0.76f)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 14.dp, bottomEnd = 14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 14.dp, bottomEnd = 14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(cover.primaryArgb), Color(cover.primaryArgb).copy(alpha = 0.90f))
                    )
                )
        ) {
            // Subtle Book Spine
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(Color(cover.spineArgb))
            )

            // Crisp Accent Stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 32.dp)
                    .height(4.dp)
                    .background(Color(cover.accentArgb))
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (notebook.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (notebook.isFavorite) Color(0xFFFFD60A) else Color(cover.textArgb).copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color(cover.textArgb).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Export as PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { menuExpanded = false; onExportPdf() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Notebook", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 26.dp, end = 16.dp, bottom = 22.dp)
            ) {
                Text(
                    text = notebook.title,
                    color = Color(cover.textArgb),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${notebook.pages.size} ${if (notebook.pages.size == 1) "page" else "pages"} • ${notebook.defaultTemplate.displayName}",
                    color = Color(cover.textArgb).copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun NewNotebookDialog(
    initialFolderId: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String?, CoverTheme, PaperTemplate, PaperColor) -> Unit
) {
    var title by remember { mutableStateOf("Untitled Notebook") }
    var selectedCover by remember { mutableStateOf(CoverTheme.LEATHER_NAVY) }
    var selectedTemplate by remember { mutableStateOf(PaperTemplate.RULED_WIDE) }
    var selectedPaperColor by remember { mutableStateOf(PaperColor.CREAM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Notebook", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cover Theme", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CoverTheme.entries) { cover ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Text("Paper Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaperColor.entries.forEach { pc ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                onCreate(title.ifBlank { "Untitled Notebook" }, initialFolderId, selectedCover, selectedTemplate, selectedPaperColor)
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
