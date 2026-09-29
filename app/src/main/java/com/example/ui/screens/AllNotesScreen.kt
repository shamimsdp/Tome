package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.ui.components.ANNOTATION_TAGS
import com.example.ui.components.EditAnnotationDialog
import com.example.ui.viewmodel.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotesScreen(
    viewModel: PdfViewModel
) {
    val allAnnotations by viewModel.allAnnotationsAcrossDocs.collectAsState()
    val documents by viewModel.libraryDocuments.collectAsState()
    val editingAnnotation by viewModel.editingAnnotation.collectAsState()
    val context = LocalContext.current

    var searchNotesQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("All") }

    val filteredAnnotations = allAnnotations.filter { ann ->
        val matchesTag = selectedTagFilter == "All" || ann.tag == selectedTagFilter
        val matchesQuery = searchNotesQuery.isBlank() ||
                ann.note.contains(searchNotesQuery, ignoreCase = true) ||
                ann.highlightedText.contains(searchNotesQuery, ignoreCase = true)
        matchesTag && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NoteAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading Notes & Highlights", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("# All Reading Notes & Highlights\n\n")
                            filteredAnnotations.forEach { ann ->
                                val doc = documents.find { it.id == ann.documentId }
                                sb.append("### ${doc?.title ?: "Document"} (p. ${ann.pageNumber + 1})\n")
                                sb.append("> \"${ann.highlightedText}\"\n\n")
                                if (ann.note.isNotBlank()) {
                                    sb.append("**Note (${ann.tag}):** ${ann.note}\n\n")
                                }
                                sb.append("---\n\n")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Exported Reading Notes")
                                putExtra(Intent.EXTRA_TEXT, sb.toString())
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Notes"))
                        },
                        modifier = Modifier.testTag("export_all_notes_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Notes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.testTag("all_notes_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchNotesQuery,
                onValueChange = { searchNotesQuery = it },
                placeholder = { Text("Filter notes or highlights...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("notes_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Tag chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTagFilter == "All",
                    onClick = { selectedTagFilter = "All" },
                    label = { Text("All (${allAnnotations.size})", fontSize = 12.sp) }
                )
                ANNOTATION_TAGS.take(3).forEach { tag ->
                    FilterChip(
                        selected = selectedTagFilter == tag,
                        onClick = { selectedTagFilter = tag },
                        label = { Text(tag, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAnnotations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes match your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAnnotations, key = { it.id }) { ann ->
                        val doc = documents.find { it.id == ann.documentId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_card_${ann.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    try {
                                                        Color(android.graphics.Color.parseColor(ann.colorHex))
                                                    } catch (_: Exception) {
                                                        Color.Yellow
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${doc?.title ?: "Document"}  •  p. ${ann.pageNumber + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { viewModel.openEditAnnotation(ann) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteAnnotation(ann.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "“${ann.highlightedText}”",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (ann.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = ann.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tag: ${ann.tag}  •  ${if (ann.isSynced) "Synced to Cloud" else "Local Only"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Edit Annotation Dialog
    editingAnnotation?.let { annotation ->
        EditAnnotationDialog(
            annotation = annotation,
            onDismiss = { viewModel.dismissEditAnnotation() },
            onSave = { updatedNote, updatedTag, updatedColor ->
                viewModel.saveEditedAnnotation(annotation, updatedNote, updatedTag, updatedColor)
            },
            onDelete = { id ->
                viewModel.deleteAnnotation(id)
            }
        )
    }
}
