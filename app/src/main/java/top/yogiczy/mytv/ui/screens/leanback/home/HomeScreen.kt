package top.yogiczy.mytv.ui.screens.leanback.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key

private val HomeBg = Color(0xFF101010)
private val CardBg = Color(0xFF232323)
private val CardBgFocused = Color(0xFF3A3A26)
private val Accent = Color(0xFFFFD600)
private val TitleColor = Color.White
private val DescColor = Color(0xFFBBBBBB)

/**
 * 长辈首页：四大卡片，零学习成本。
 * 大字 + 高对比 + 焦点黄描边 + 每张卡写明用途；返回键直接回直播。
 */
@Composable
fun LeanbackHomeScreen(
    modifier: Modifier = Modifier,
    showSettings: Boolean,
    onLive: () -> Unit,
    onFavorites: () -> Unit,
    onGuide: () -> Unit,
    onSettings: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBg.copy(alpha = 0.92f))
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && e.key == Key.Back) {
                    onLive(); true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "按 OK 打开 · 按返回键直接看电视",
            color = DescColor,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            HomeCard(
                focusRequester = focusRequester,
                icon = "📺", title = "看直播", desc = "打开电视频道",
                onClick = onLive,
            )
            HomeCard(
                icon = "❤️", title = "我的收藏", desc = "常看的频道都在这里",
                onClick = onFavorites,
            )
            HomeCard(
                icon = "🎞️", title = "节目单·回看", desc = "错过的节目重新看",
                onClick = onGuide,
            )
            if (showSettings) {
                HomeCard(
                    icon = "⚙️", title = "电视设置", desc = "家人调校用",
                    onClick = onSettings,
                )
            }
        }
    }
}

@Composable
private fun HomeCard(
    focusRequester: FocusRequester? = null,
    icon: String,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, label = "cardScale")

    Box(
        modifier = Modifier
            .width(250.dp)
            .height(210.dp)
            .scale(scale)
            .background(if (isFocused) CardBgFocused else CardBg, RoundedCornerShape(20.dp))
            .border(
                width = if (isFocused) 4.dp else 0.dp,
                color = if (isFocused) Accent else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .focusRequester(focusRequester ?: FocusRequester.Default)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { e ->
                when {
                    e.type == KeyEventType.KeyUp && e.key == Key.Back -> { onClick(); true }
                    e.type == KeyEventType.KeyUp &&
                        (e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter) -> {
                        onClick(); true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 52.sp)
            Text(
                text = title,
                color = if (isFocused) Accent else TitleColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = desc,
                color = DescColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
