package com.abodycode.photomagic.ui.auth

/// AR/EN catalog interface — the auth-surface strings are the EXACT same
/// wording used by p2p dish (only the app name / taglines differ).
interface AuthStrings {
    // ---------------------------------------------------------------- app
    val appTitle: String
    val tagline: String
    // ---------------------------------------------------------------- auth
    val username: String
    val password: String
    val loginButton: String
    val logout: String
    val createAccount: String
    val loginFailed: String
    val accountExpired: String
    val accountSuspended: String
    val loginOffline: String
    val loginChecking: String
    val createAccountTitle: String
    val createAccountMessage: String
    // --------------------------------------------------------------- misc
    val error: String
    val sessionEnded: String
    // ------------------------------------------------------------- contact
    val contactMe: String
    val contactWhatsapp: String
    val contactTelegram: String
    val contactEmail: String
    val contactChannel: String
}

/// English catalog (LTR).
object AuthStringsEn : AuthStrings {
    override val appTitle: String = "PhotoMagic"
    override val tagline: String = "A4 photo grid printing studio"
    override val username: String = "Username"
    override val password: String = "Password"
    override val loginButton: String = "SIGN IN"
    override val logout: String = "Sign out"
    override val createAccount: String = "Create account"
    override val loginFailed: String = "Incorrect username or password"
    override val accountExpired: String = "Trial account has expired"
    override val accountSuspended: String = "Account suspended. Contact the administrator."
    override val loginOffline: String = "Sign-in requires an internet connection"
    override val loginChecking: String = "Verifying…"
    override val createAccountTitle: String = "Create account"
    override val createAccountMessage: String =
        "To get an account with a free 12-hour trial - and to receive a username and password to sign in and start using the app - please contact engineer Abdullah Jalal through one of these channels:"
    override val error: String = "Error"
    override val sessionEnded: String = "Your session ended, please sign in again"
    override val contactMe: String = "Contact me"
    override val contactWhatsapp: String = "WhatsApp"
    override val contactTelegram: String = "Telegram"
    override val contactEmail: String = "Email"
    override val contactChannel: String = "My Telegram channel"
}

/// Arabic catalog (RTL) — identical wording to p2p dish except the app name.
object AuthStringsAr : AuthStrings {
    override val appTitle: String = "PhotoMagic"
    override val tagline: String = "استوديو طباعة شبكة الصور A4"
    override val username: String = "اسم المستخدم"
    override val password: String = "كلمة المرور"
    override val loginButton: String = "تسجيل الدخول"
    override val logout: String = "تسجيل الخروج"
    override val createAccount: String = "إنشاء حساب"
    override val loginFailed: String = "اسم المستخدم أو كلمة المرور غير صحيحة"
    override val accountExpired: String = "انتهت صلاحية الحساب التجريبي"
    override val accountSuspended: String = "الحساب موقوف. يرجى التواصل مع الإدارة."
    override val loginOffline: String = "لا يمكن تسجيل الدخول بدون اتصال بالإنترنت"
    override val loginChecking: String = "جارٍ التحقق…"
    override val createAccountTitle: String = "إنشاء حساب"
    override val createAccountMessage: String =
        "للحصول على حساب و استخدام تجريبي مجاني للتطبيق لمدة 12 ساعة لغرض الحصول على اسم مستخدم و كلمة مرور لتسجيل الدخول و بدأ استخدام التطبيق يرجى التواصل مع المُهندس عبدالله جلال عن طريق احدى هذه الوسائل:"
    override val error: String = "خطأ"
    override val sessionEnded: String = "انتهت الجلسة، يرجى تسجيل الدخول من جديد"
    override val contactMe: String = "تواصل معي"
    override val contactWhatsapp: String = "واتساب"
    override val contactTelegram: String = "تيليجرام"
    override val contactEmail: String = "ايميل"
    override val contactChannel: String = "قناتي على تيليجرام"
}

fun authStringsFor(locale: String): AuthStrings = when (locale) {
    "en" -> AuthStringsEn
    else -> AuthStringsAr
}

/// The four contact URLs, in the exact requested order. Shared by the
/// "Create account" screen and the "Contact me" bottom sheet.
object ContactUrls {
    const val WHATSAPP: String = "https://wa.me/qr/ZBCM2HUUDBTWB1"
    const val TELEGRAM: String = "https://t.me/Abdullah2001jalal"
    const val EMAIL: String = "mailto:abdullah2001jalal@gmail.com"
    const val CHANNEL: String = "https://t.me/AbodyCode"

    val all: List<String> = listOf(WHATSAPP, TELEGRAM, EMAIL, CHANNEL)
}
