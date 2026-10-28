package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import kotlinx.coroutines.delay

/** A grid of icons; the first one takes focus when the page opens. Shared by every picker. */
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    columns: Int = 7,
    cell: @Composable (app: AppInfo, modifier: Modifier) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = rememberLazyGridState(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
        modifier = modifier
    ) {
        itemsIndexed(items = apps, key = { _, app -> app.packageName }) { index, app ->
            cell(
                app,
                Modifier
                    .aspectRatio(0.95f)
                    .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
            )
        }
    }
}

/** The search field at the top of Launchpad. Decorative, but it also names the page. */
@Composable
fun LaunchpadHeader(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = LabelSecondary, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 14.sp, color = Label)
        if (trailing != null) {
            Box(Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.3f)))
            Text(trailing, fontSize = 13.sp, color = LabelSecondary)
        }
    }
}

/** Launchpad: every installed app on the blurred wallpaper. OK opens, MENU / long press shows options. */
@Composable
fun AllAppsDialog(
    allApps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchpadDialog(onDismiss = onDismiss) {
        LaunchpadHeader("Launchpad", trailing = "${allApps.size} ứng dụng")
        AppGrid(
            apps = allApps,
            firstItemFocusRequester = firstItemFocusRequester,
            modifier = Modifier.weight(1f)
        ) { app, cellModifier ->
            LaunchpadIcon(
                app = app,
                onClick = { onLaunchApp(app) },
                onLongClick = { onShowAppMenu(app) },
                modifier = cellModifier
            )
        }
    }
}
