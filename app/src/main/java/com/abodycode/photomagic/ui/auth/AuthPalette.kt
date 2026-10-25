package com.abodycode.photomagic.ui.auth

import androidx.compose.ui.graphics.Color

/// Auth-surface palette — byte-for-byte the p2p dish AppPalette values.
/// The only change: the PAGE BACKGROUND comes from PhotoMagic's own theme
/// (studio slate) so the brand background differs from p2p dish.
internal data class AuthPalette(val dark: Boolean) {

    val background: Color = if (dark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val surface: Color = if (dark) Color(0xFF1A1B26) else Color(0xFFFFFFFF)
    val frame: Color = if (dark) Color(0xFF2A2C3D) else Color(0xFFC3CAD8)
    val textPrimary: Color = if (dark) Color(0xFFE8F0FF) else Color(0xFF101423)
    val textDim: Color = if (dark) Color(0xFF8A93AC) else Color(0xFF5A6478)

    /// Translucent glass panel fill (p2p dish values).
    val glass: Color = if (dark) Color(0x141E2233) else Color(0x14646F87)

    /// Accent — sign-in button, contact outlines, toggle highlights.
    val cyan: Color = if (dark) Color(0xFF00F2FE) else Color(0xFF0087A3)

    /// Error banner (invalid / expired / suspended).
    val alarm: Color = if (dark) Color(0xFFFF2A55) else Color(0xFFD21243)

    /// Offline / warning banner.
    val amber: Color = if (dark) Color(0xFFFFC24B) else Color(0xFFB26B00)
}
