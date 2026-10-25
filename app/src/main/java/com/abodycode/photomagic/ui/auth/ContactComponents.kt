package com.abodycode.photomagic.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abodycode.photomagic.R

/// Opens a contact URL in the best matching external app (WhatsApp,
/// Telegram, browser or mail client); silently ignores failures — the
/// user can retry. (p2p dish `ContactLauncher.open`)
internal fun openContactUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        // No matching app installed — nothing sensible to do.
    }
}

/// One contact channel row — identical to the p2p dish `_ChannelButton` and
/// the sheet `_ContactTile`: brand-colored outlined pill with the brand
/// icon on the leading side, the label, and a trailing chevron. The sheet
/// variant adds the translucent glass fill behind the row.
@Composable
internal fun ChannelButton(
    label: String,
    color: Color,
    url: String,
    palette: AuthPalette,
    glassBackground: Boolean = false,
) {
    val context = LocalContext.current

    @DrawableRes
    val brandRes: Int = when (url) {
        ContactUrls.WHATSAPP -> R.drawable.ic_whatsapp
        ContactUrls.TELEGRAM, ContactUrls.CHANNEL -> R.drawable.ic_telegram
        // Universal e-mail icon (the standard envelope everyone knows)
        // instead of the Gmail logo — works for any mail client.
        ContactUrls.EMAIL -> R.drawable.ic_email
        else -> 0
    }

    val base: Modifier = Modifier
        .fillMaxWidth()
        .clickable { openContactUrl(context, url) }

    val styled: Modifier = if (glassBackground) {
        base
            .background(color = palette.glass, shape = RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        base
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(modifier = styled, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(brandRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
    }
}

/// The "تواصل معي" bottom sheet — same four options, same order, same
/// colors and same glass-tile look as the p2p dish contact sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSheet(
    dark: Boolean,
    locale: String,
    onDismiss: () -> Unit,
) {
    val palette = AuthPalette(dark)
    val t = authStringsFor(locale)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = t.contactMe,
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.W800,
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    glassBackground = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
