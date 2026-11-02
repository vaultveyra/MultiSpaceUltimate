package com.multispace.ultimate

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun AppIcon(pkg: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(pkg) { IconCache.get(ctx, pkg) }
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
            Icon(Icons.Rounded.Android, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * چیدمان مثل صفحه‌ی اصلی گوشی: چند ستون و چند ردیف.
 * نگه‌داشتن روی آیکن = شروع جابه‌جایی با درگ؛ نگه‌داشتن روی نام = ویرایش.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaceGrid(
    items: List<Instance>,
    columns: Int,
    onReorder: (List<String>) -> Unit,
    onOpen: (Instance) -> Unit,
    onEdit: (Instance) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellW = constraints.maxWidth / columns
        val cellH = with(density) { 118.dp.roundToPx() }

        val order: SnapshotStateList<String> =
            remember(items.map { it.id }) { mutableStateListOf<String>().apply { addAll(items.map { it.id }) } }
        val byId = items.associateBy { it.id }

        var draggingId by remember { mutableStateOf<String?>(null) }
        var origin by remember { mutableStateOf(IntOffset.Zero) }
        var delta by remember { mutableStateOf(Offset.Zero) }
        var lastDragEnd by remember { mutableLongStateOf(0L) }

        val rows = (order.size + columns - 1) / columns
        Box(Modifier.fillMaxWidth().height(with(density) { (rows * cellH).toDp() })) {
            order.toList().forEach { id ->
                val inst = byId[id] ?: return@forEach
                key(id) {
                    val idx = order.indexOf(id)
                    val base = IntOffset((idx % columns) * cellW, (idx / columns) * cellH)
                    val isDrag = draggingId == id
                    val animated by animateIntOffsetAsState(base, label = "pos")
                    val pos = if (isDrag)
                        IntOffset(origin.x + delta.x.roundToInt(), origin.y + delta.y.roundToInt())
                    else animated

                    val endDrag = {
                        draggingId = null
                        delta = Offset.Zero
                        lastDragEnd = System.currentTimeMillis()
                        onReorder(order.toList())
                    }

                    val dragModifier = Modifier.pointerInput(id, cellW, cellH, columns) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                val i = order.indexOf(id)
                                origin = IntOffset((i % columns) * cellW, (i / columns) * cellH)
                                delta = Offset.Zero
                                draggingId = id
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                delta += amount
                                val cx = origin.x + delta.x + cellW / 2f
                                val cy = origin.y + delta.y + cellH / 2f
                                val col = (cx / cellW).toInt().coerceIn(0, columns - 1)
                                val row = (cy / cellH).toInt().coerceAtLeast(0)
                                val target = (row * columns + col).coerceIn(0, order.lastIndex)
                                val cur = order.indexOf(id)
                                if (target != cur && cur >= 0) {
                                    order.removeAt(cur)
                                    order.add(target, id)
                                }
                            },
                            onDragEnd = { endDrag() },
                            onDragCancel = { endDrag() }
                        )
                    }

                    Box(
                        Modifier
                            .offset { pos }
                            .size(with(density) { cellW.toDp() }, with(density) { cellH.toDp() })
                            .zIndex(if (isDrag) 1f else 0f)
                            .graphicsLayer {
                                val s = if (isDrag) 1.12f else 1f
                                scaleX = s; scaleY = s
                                alpha = if (isDrag) 0.94f else 1f
                            }
                    ) {
                        GridCell(
                            inst = inst,
                            dragging = isDrag,
                            dragModifier = dragModifier,
                            onOpen = {
                                if (System.currentTimeMillis() - lastDragEnd > 350) onOpen(inst)
                            },
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEdit(inst)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(
    inst: Instance,
    dragging: Boolean,
    dragModifier: Modifier,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp)) {
            AppIcon(
                inst.pkg,
                Modifier
                    .fillMaxSize()
                    .shadow(if (dragging) 18.dp else 5.dp, shape)
                    .clip(shape)
                    .then(dragModifier)
                    .clickable { onOpen() }
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${inst.number}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            inst.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onOpen, onLongClick = onEdit)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
