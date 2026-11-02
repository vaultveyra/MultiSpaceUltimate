package com.multispace.ultimate

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(vm: SpacesViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { AppCatalog.load(ctx) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Text(
                stringResource(R.string.add_app),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                listOf(R.string.tab_installed, R.string.tab_system, R.string.tab_discover).forEachIndexed { i, r ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(r)) })
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            if (tab == 2) {
                DiscoverTab(query)
            } else {
                if (tab == 1) {
                    Text(
                        stringResource(R.string.system_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val list = apps.filter { it.isSystem == (tab == 1) && it.label.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(list, key = { it.pkg }) { app ->
                            AppRow(app, vm.countOf(app.pkg)) {
                                val inst = vm.addApp(app)
                                val space = vm.spaceNames[inst.number]
                                    ?: ctx.getString(R.string.space_default, inst.number)
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.added_toast, inst.displayName, space),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, count: Int, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.pkg, Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (count > 0) {
                Text(
                    stringResource(R.string.instances_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, null) }
    }
}

@Composable
private fun DiscoverTab(query: String) {
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.discover_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val q = Uri.encode(query)
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q&c=apps")))
                } catch (e: ActivityNotFoundException) {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$q&c=apps"))
                    )
                }
            },
            shape = RoundedCornerShape(14.dp)
        ) { Text(stringResource(R.string.discover_open_store)) }
    }
}
