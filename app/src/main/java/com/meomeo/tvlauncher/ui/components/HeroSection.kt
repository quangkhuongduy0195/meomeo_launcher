package com.meomeo.tvlauncher.ui.components

import com.meomeo.tvlauncher.ui.theme.glassIdleColor
import com.meomeo.tvlauncher.ui.theme.QuicksandFamily
import com.meomeo.tvlauncher.ui.theme.glassFocusedColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.StarYellow
import com.meomeo.tvlauncher.ui.theme.TextSecondary

/** What the hero area describes, driven by whichever dock item currently has focus. */
sealed interface HeroContent {
    data class App(val app: AppInfo) : HeroContent
    data class AllApps(val totalApps: Int) : HeroContent
    data object EmptySlot : HeroContent
}

/** Badge + app name shown in the middle of Home. Hidden for now; set to true to bring it back. */
const val SHOW_HERO = false

/** Remote-key hint row under the hero title. Temporarily hidden; flip to true to bring it back. */
private const val SHOW_HERO_HINTS = false

private val HeroTitleStyle = TextStyle(
    fontFamily = QuicksandFamily,
    fontSize = 42.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White,
    letterSpacing = 0.5.sp,
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        offset = Offset(0f, 3f),
        blurRadius = 10f
    )
)

@Composable
fun HeroSection(
    content: HeroContent,
    modifier: Modifier = Modifier
) {
    if (!SHOW_HERO) return
    AnimatedContent(
        targetState = content,
        // Re-key on identity so toggling a favourite updates in place instead of re-animating.
        contentKey = {
            when (it) {
                is HeroContent.App -> it.app.packageName
                is HeroContent.AllApps -> "all_apps"
                HeroContent.EmptySlot -> "empty_slot"
            }
        },
        transitionSpec = {
            (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 8 })
                .togetherWith(fadeOut(tween(120)))
        },
        modifier = modifier,
        label = "heroContent"
    ) { hero ->
        Column {
            when (hero) {
                is HeroContent.App -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroBadge(
                            text = if (hero.app.isTvApp) "ỨNG DỤNG TV" else "ỨNG DỤNG DI ĐỘNG",
                            color = Color.White
                        )
                        if (hero.app.isFavorite) {
                            HeroBadge(text = "★ YÊU THÍCH", color = StarYellow)
                        }
                    }
                    HeroTitle(hero.app.label)
                    HeroHints(
                        "OK" to "Mở",
                        "Giữ OK" to "Tùy chọn"
                    )
                }

                is HeroContent.AllApps -> {
                    HeroBadge(text = "KHO ỨNG DỤNG • HỆ THỐNG", color = Color.White)
                    HeroTitle("Tất cả ứng dụng")
                    HeroHints("OK" to "Mở toàn bộ ${hero.totalApps} ứng dụng")
                }

                HeroContent.EmptySlot -> {
                    HeroBadge(text = "Ô TRỐNG", color = Color.White)
                    HeroTitle("Thêm ứng dụng yêu thích")
                    HeroHints("OK" to "Chọn ứng dụng cho ô này")
                }
            }
        }
    }
}

@Composable
private fun HeroTitle(text: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = text,
        style = HeroTitleStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun HeroBadge(text: String, color: Color) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (color == Color.White) glassFocusedColor() else color.copy(alpha = 0.28f))
            .border(1.dp, color.copy(alpha = if (color == Color.White) 0.35f else 0.7f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (color == StarYellow) StarYellow else Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

/** Remote-control hint chips: each pair is (key, action). */
@Composable
private fun HeroHints(vararg hints: Pair<String, String>) {
    if (!SHOW_HERO_HINTS) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        hints.forEach { (key, action) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(glassIdleColor())
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = key,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = action,
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
