package com.abodycode.photomagic.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abodycode.photomagic.R

/// Informational "Create account" screen — the p2p dish layout with the
/// PhotoMagic name/branding: explains that accounts are provisioned
/// personally by engineer Abdullah Jalal (SAME message text) and shows the
/// same four contact buttons as the "Contact me" sheet.
@Composable
fun CreateAccountScreen(
    dark: Boolean,
    locale: String,
    onBack: () -> Unit,
) {
    val palette = AuthPalette(dark)
    val t = authStringsFor(locale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Back arrow pinned to the visual LEFT — fixed like p2p dish.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBackIosNew,
                        contentDescription = null,
                        tint = palette.textPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Official brand logo; fills the spare vertical space on large
        // screens and shrinks (Flexible) so the contacts stay reachable.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.abody_code_logo),
                contentDescription = "ABODY CODE",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = t.createAccountTitle,
            textAlign = TextAlign.Center,
            color = palette.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        // The SAME informational message as p2p dish.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.glass, RoundedCornerShape(14.dp))
                .border(1.dp, palette.frame, RoundedCornerShape(14.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = t.createAccountMessage,
                textAlign = TextAlign.Center,
                color = palette.textPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.W600,
            )
        }

        // Push the contact buttons to the bottom like p2p dish.
        Spacer(modifier = Modifier.weight(2f))

        val channels: List<Pair<String, Color>> = listOf(
            t.contactWhatsapp to Color(0xFF25D366),   // official WhatsApp green
            t.contactTelegram to Color(0xFF229ED9),   // official Telegram cyan
            t.contactEmail to Color(0xFFEA4335),      // Gmail red accent
            t.contactChannel to Color(0xFF229ED9),    // Telegram cyan (channel)
        )
        val urls: List<String> = ContactUrls.all
        channels.forEachIndexed { index, (label, color) ->
            ChannelButton(
                label = label,
                color = color,
                url = urls[index],
                palette = palette,
                glassBackground = false,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
