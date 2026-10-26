package com.abodycode.photomagic.ui.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/// Actions row shown on the screen after a successful sign-in:
///
/// * Fixed visual row: language toggle + theme toggle on the LEFT,
///   "تواصل معي" on the RIGHT — independent of locale direction.
/// * Below the actions: a small quiet sign-out row (aligned to the end).
///
/// The ABODY CODE logo itself is NOT here: the studio screen directly below
/// renders it BIG and CENTERED with the "add photo" button under it.
@Composable
fun AuthenticatedHeader(
    dark: Boolean,
    locale: String,
    onLocaleChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenContact: () -> Unit,
    onSignOut: () -> Unit,
) {
    val palette = AuthPalette(dark)
    val t = authStringsFor(locale)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        // Fixed-visual action row (always left/right as drawn in p2p dish).
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LanguageToggle(
                        locale = locale,
                        onLocaleChange = onLocaleChange,
                        palette = palette,
                    )
                    ThemeToggle(dark = dark, onToggleTheme = onToggleTheme, palette = palette)
                }
                OutlinedButton(
                    onClick = onOpenContact,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.4.dp, palette.cyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.cyan),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = t.contactMe, fontSize = 13.sp, fontWeight = FontWeight.W700)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign-out lives in a small quiet row under the header actions.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onSignOut,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = palette.textDim,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = t.logout, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
