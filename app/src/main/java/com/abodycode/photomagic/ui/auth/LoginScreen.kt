package com.abodycode.photomagic.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abodycode.photomagic.R

/// Mandatory first screen: username + password sign-in (p2p dish layout):
///
/// * Fixed visual row: language toggle (EN/ع) far LEFT, theme toggle CENTER,
///   "تواصل معي" far RIGHT — independent of the active locale direction.
/// * Official ABODY CODE brand logo below, then the sign-in form.
/// * "Create account" → informational screen with the four contacts.
@Composable
fun LoginScreen(
    dark: Boolean,
    locale: String,
    busy: Boolean,
    error: AuthError,
    onLocaleChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenCreate: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    val palette = AuthPalette(dark)
    val t = authStringsFor(locale)
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var obscure by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- fixed-visual top bar (language LEFT · theme CENTER · contact RIGHT)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageToggle(
                    locale = locale,
                    onLocaleChange = onLocaleChange,
                    palette = palette,
                )
                ThemeToggle(dark = dark, onToggleTheme = onToggleTheme, palette = palette)
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

        Spacer(modifier = Modifier.height(24.dp))

        // Official brand logo, ~full screen width (p2p dish BrandLogo).
        Image(
            painter = painterResource(R.drawable.abody_code_logo),
            contentDescription = "ABODY CODE",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = t.appTitle,
            textAlign = TextAlign.Center,
            color = palette.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.W700,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = t.tagline,
            textAlign = TextAlign.Center,
            color = palette.textDim,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(36.dp))

        // Username field (filled, radius 10, hint dim — p2p dish style).
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            singleLine = true,
            enabled = !busy,
            placeholder = { Text(text = t.username, color = palette.textDim, fontSize = 16.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = palette.textDim,
                )
            },
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            colors = authFieldColors(palette),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Password field.
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            singleLine = true,
            enabled = !busy,
            placeholder = { Text(text = t.password, color = palette.textDim, fontSize = 16.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = palette.textDim,
                )
            },
            trailingIcon = {
                IconButton(onClick = { obscure = !obscure }) {
                    Icon(
                        imageVector = if (obscure) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = palette.textDim,
                    )
                }
            },
            visualTransformation = if (obscure) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit(username, password) },
            ),
            colors = authFieldColors(palette),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (error != AuthError.NONE) {
            ErrorBanner(error = error, locale = locale, palette = palette)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SIGN IN — accent-filled, height 56, radius 10 (p2p dish style).
        Button(
            onClick = { onSubmit(username, password) },
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.cyan,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text(
                    text = t.loginButton,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 1.2.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenCreate,
            enabled = !busy,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.4.dp, palette.cyan),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.cyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = t.createAccount,
                fontSize = 16.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 1.2.sp,
            )
        }

        // Bottom breathing room on tall screens.
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/// Tiny reusable error banner container (p2p dish `_errorBanner`).
@Composable
internal fun AuthErrorBannerBox(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ErrorBanner(error: AuthError, locale: String, palette: AuthPalette) {
    val t = authStringsFor(locale)
    val (text, color) = when (error) {
        AuthError.INVALID -> t.loginFailed to palette.alarm
        AuthError.EXPIRED -> t.accountExpired to palette.alarm
        AuthError.SUSPENDED -> t.accountSuspended to palette.alarm
        AuthError.OFFLINE -> t.loginOffline to palette.amber
        AuthError.SERVER -> t.error to palette.alarm
        AuthError.NONE -> "" to palette.textDim
    }
    AuthErrorBannerBox(color = color, text = text)
}

/// EN/ع switch (p2p dish `_languageToggle`): selected = accent bg/white text,
/// unselected = transparent/dim, hairline outer frame.
@Composable
internal fun LanguageToggle(
    locale: String,
    onLocaleChange: (String) -> Unit,
    palette: AuthPalette,
) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = palette.cyan,
        activeContentColor = Color.White,
        activeBorderColor = palette.cyan,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = palette.textDim,
        inactiveBorderColor = palette.frame,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .selectableGroup()
            .heightIn(min = 40.dp),
    ) {
        SegmentedButton(
            selected = locale == "en",
            onClick = { onLocaleChange("en") },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = colors,
        ) {
            Text(text = "EN", fontSize = 14.sp, fontWeight = FontWeight.W600)
        }
        SegmentedButton(
            selected = locale == "ar",
            onClick = { onLocaleChange("ar") },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = colors,
        ) {
            Text(text = "ع", fontSize = 14.sp, fontWeight = FontWeight.W600)
        }
    }
}

/// Sun in light mode, moon in dark mode (p2p dish `_themeToggle`).
@Composable
internal fun ThemeToggle(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    palette: AuthPalette,
) {
    IconButton(onClick = onToggleTheme, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = if (dark) "الوضع الفاتح" else "الوضع الداكن",
            tint = palette.cyan,
            modifier = Modifier.size(22.dp),
        )
    }
}

/// OutlinedTextField colors replicating the p2p dish InputDecorationTheme:
/// filled with `surface`, hairline `frame` border, dim hint.
@Composable
internal fun authFieldColors(palette: AuthPalette) = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = palette.surface,
    unfocusedContainerColor = palette.surface,
    disabledContainerColor = palette.surface,
    focusedBorderColor = palette.cyan,
    unfocusedBorderColor = palette.frame,
    disabledBorderColor = palette.frame,
    focusedTextColor = palette.textPrimary,
    unfocusedTextColor = palette.textPrimary,
    disabledTextColor = palette.textDim,
    cursorColor = palette.cyan,
    focusedPlaceholderColor = palette.textDim,
    unfocusedPlaceholderColor = palette.textDim,
    focusedLeadingIconColor = palette.textDim,
    unfocusedLeadingIconColor = palette.textDim,
    focusedTrailingIconColor = palette.textDim,
    unfocusedTrailingIconColor = palette.textDim,
)
