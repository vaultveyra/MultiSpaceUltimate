package com.multispace.ultimate

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpacesScreen(vm: SpacesViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val spaces = vm.spaceNumbers
    val pager = rememberPagerState { spaces.size.coerceAtLeast(1) }

    var editInst by remember { mutableStateOf<Instance?>(null) }
    var editSpace by remember { mutableStateOf<Int?>(null) }

    @Composable
    fun spaceTitle(n: Int) = vm.spaceNames[n] ?: stringResource(R.string.space_default, n)

    Column(modifier.fillMaxSize().background(screenBrush()).statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                stringResource(R.string.spaces_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.spaces_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (spaces.isEmpty()) {
            EmptyState()
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(spaces) { i, n ->
                    SpaceChip(
                        title = spaceTitle(n),
                        count = vm.instances.count { it.number == n },
                        selected = pager.currentPage == i,
                        onClick = { scope.launch { pager.animateScrollToPage(i) } },
                        onLongClick = { editSpace = n }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { spaces.getOrNull(it) ?: -1 }
            ) { page ->
                val n = spaces.getOrNull(page)
                if (n != null) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                        SpaceGrid(
                            items = vm.instances.filter { it.number == n },
                            columns = 4,
                            onReorder = { vm.reorder(n, it) },
                            onOpen = { inst ->
                                val r = NoEngine.launch(ctx, inst)
                                if (r is LaunchResult.Failed)
                                    Toast.makeText(ctx, r.reason, Toast.LENGTH_LONG).show()
                            },
                            onEdit = { editInst = it }
                        )
                        Spacer(Modifier.height(110.dp))
                    }
                }
            }
        }
    }

    editInst?.let { inst ->
        EditDialog(
            title = stringResource(R.string.edit_instance),
            initial = inst.displayName,
            onSave = { vm.renameInstance(inst.id, if (it.trim() == inst.defaultName) null else it) },
            onDelete = { vm.delete(inst.id) },
            onDismiss = { editInst = null }
        )
    }
    editSpace?.let { n ->
        val default = stringResource(R.string.space_default, n)
        EditDialog(
            title = stringResource(R.string.edit_space),
            initial = vm.spaceNames[n] ?: default,
            onSave = { vm.renameSpace(n, if (it.trim() == default) null else it) },
            onDelete = null,
            onDismiss = { editSpace = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpaceChip(title: String, count: Int, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) c.primary else c.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.onPrimary else c.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) c.onPrimary.copy(alpha = 0.8f) else c.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.GridView, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EditDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = { onDelete(); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
