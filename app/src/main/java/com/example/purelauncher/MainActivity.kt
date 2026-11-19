package com.example.purelauncher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PureWallpaperLauncherScreen()
        }
    }
}

@Composable
fun PureWallpaperLauncherScreen() {
    val context = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    val installedApps = remember {
        loadInstalledApps(context)
    }

    val autumnGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2B1B12),
            Color(0xFF6E3B1E),
            Color(0xFFB86B2B),
            Color(0xFF3A2314)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(autumnGradient)
            .pointerInput(drawerOpen) {
                detectDragGestures(
                    onDragStart = { dragAccumulatedY = 0f },
                    onDragEnd = {
                        if (!drawerOpen && dragAccumulatedY < -80f) {
                            drawerOpen = true
                        } else if (drawerOpen && dragAccumulatedY > 120f) {
                            drawerOpen = false
                        }
                        dragAccumulatedY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedY += dragAmount.y
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 240)
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 200)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9111111))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { drawerOpen = false }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val launchIntent = context.packageManager
                                        .getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                        drawerOpen = false
                                    }
                                }
                                .padding(6.dp)
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun loadInstalledApps(context: android.content.Context): List<AppEntry> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
    return resolveInfos
        .mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val label = info.loadLabel(pm).toString()
            val icon = drawableToBitmap(info.loadIcon(pm)).asImageBitmap()
            AppEntry(label = label, packageName = pkg, icon = icon)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
