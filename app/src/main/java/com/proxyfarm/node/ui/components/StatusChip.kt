package com.proxyfarm.node.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxyfarm.node.data.model.ProxyState
import com.proxyfarm.node.ui.theme.ProxyBlue
import com.proxyfarm.node.ui.theme.ProxyGreen
import com.proxyfarm.node.ui.theme.ProxyRed

@Composable
fun StatusChip(proxyState: ProxyState, modifier: Modifier = Modifier) {
    val (dotColor, labelText) = when (proxyState) {
        is ProxyState.Idle     -> Pair(ProxyGreen, "[Online] Listening for Cloud Hooks")
        is ProxyState.Active   -> Pair(ProxyBlue,  "[Active] Tunnelling Scraping Job")
        is ProxyState.Starting -> Pair(ProxyBlue,  "[Starting] Initialising Engine…")
        is ProxyState.Stopping -> Pair(ProxyRed,   "[Stopping] Shutting Down…")
        is ProxyState.Error    -> Pair(ProxyRed,   "[Error] ${proxyState.message}")
    }
    val shouldAnimate = proxyState is ProxyState.Active || proxyState is ProxyState.Idle
    val animatedBg by animateColorAsState(dotColor.copy(alpha = 0.12f), tween(400), label = "chipBg")
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = animatedBg) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(color = dotColor, animate = shouldAnimate)
            Spacer(Modifier.width(8.dp))
            Text(labelText, color = dotColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

@Composable
private fun PulsingDot(color: Color, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by transition.animateFloat(if (animate) 0.3f else 1f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "dotAlpha")
    Box(modifier = modifier.size(10.dp).clip(CircleShape).alpha(if (animate) alpha else 1f).background(color))
}
