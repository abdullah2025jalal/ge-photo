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

/// AR/EN catalog interface for the STUDIO surface (second screen + tabs +
/// dialogs + toasts). Arabic wording identical to the original release.
interface StudioStrings {
    // ----------------------------------------------------------------- hero
    val addPhotoBtn: String
    val back: String
    // ----------------------------------------------------------------- tabs
    fun tabItems(count: Int): String
    fun tabPreview(pages: Int): String
    // ----------------------------------------------------------- empty state
    val emptyTitle: String
    val emptyBody: String
    val emptyAdd: String
    // ---------------------------------------------------------------- items
    val photoBadge: String
    val docBadge: String
    val edit: String
    val del: String
    val front: String
    val backFace: String
    val none: String
    fun dims(text: String): String
    val samePageDetail: String
    val backSideDetail: String
    val standardRows: String
    val customDims: String
    val copiesLabel: String
    val decrease: String
    val increase: String
    fun total(photos: Int, docs: Int): String
    val specsShort: String
    val viewPaper: String
    fun photoName(n: Int): String
    // ---------------------------------------------------- add-type dialog
    val chooseTitle: String
    val photoOptionTitle: String
    val photoOptionBody: String
    val docOptionTitle: String
    val docOptionBody: String
    val cancel: String
    // ------------------------------------------------- add-document dialog
    val addDocTitle: String
    val frontPick: String
    val requiredTag: String
    val backPickHint: String
    val optionalTag: String
    val frontDone: String
    val backDone: String
    val placementTitle: String
    val samePageTitle: String
    val samePageBody: String
    val backSideTitle: String
    val backSideBody: String
    val widthCm: String
    val heightCm: String
    val presetId: String
    val presetPassport: String
    val presetHalfA5: String
    val copiesPrompt: String
    val addDocAction: String
    fun docName(widthCm: Float, heightCm: Float): String
    val needFront: String
    // ------------------------------------------------- edit-dimensions dialog
    fun editTitle(name: String): String
    val samePageShort: String
    val backSideShort: String
    val saveEdit: String
    // ----------------------------------------------------------- preview tab
    fun sheetFront(page: Int, total: Int): String
    fun sheetBack(page: Int, total: Int): String
    fun sheetPlain(page: Int, total: Int): String
    val saveThisSheet: String
    val exportSummaryTitle: String
    fun exportSummaryBadge(pages: Int): String
    val exportDocxBtn: String
    val buildingDocx: String
    val exportAllBtn: String
    val exportNote: String
    // -------------------------------------------------------- toasts/intents
    val shareChooser: String
    val shareSubject: String
    val shareText: String
    fun wordError(message: String): String
    fun processing(page: Int): String
    fun savedOk(page: Int): String
    fun saveFailed(message: String): String
}

/// English catalog for the studio (LTR).
object StudioStringsEn : StudioStrings {
    override val addPhotoBtn: String = "Add photo"
    override val back: String = "Back"
    override fun tabItems(count: Int): String = "Items ($count)"
    override fun tabPreview(pages: Int): String = "File & print preview ($pages sheets)"
    override val emptyTitle: String = "No items added yet"
    override val emptyBody: String =
        "You can add regular personal photos or documents (ID, license, card with front and back faces) to print them at maximum resolution on an A4 sheet without any overlap."
    override val emptyAdd: String = "Add photo or document"
    override val photoBadge: String = "Photo"
    override val docBadge: String = "Document / ID"
    override val edit: String = "Edit"
    override val del: String = "Delete"
    override val front: String = "Front"
    override val backFace: String = "Back"
    override val none: String = "None"
    override fun dims(text: String): String = "Size: $text"
    override val samePageDetail: String = "Both faces on the same page (right & left, 0.6 cm apart)"
    override val backSideDetail: String = "Back face on the sheet back (second page)"
    override val standardRows: String = "Up to 6 photos per row (3 cm wide)"
    override val customDims: String = "Custom dimensions"
    override val copiesLabel: String = "Copies:"
    override val decrease: String = "Decrease"
    override val increase: String = "Increase"
    override fun total(photos: Int, docs: Int): String = "Total: $photos photos | $docs documents"
    override val specsShort: String = "0.6 cm margins | 1 mm gap | no overlap"
    override val viewPaper: String = "View paper file"
    override fun photoName(n: Int): String = "Photo $n"
    override val chooseTitle: String = "Choose the item type to add"
    override val photoOptionTitle: String = "Personal photo / regular photos"
    override val photoOptionBody: String = "Repeat across A4 grid rows (e.g. 3×4 cm, 6 per row)"
    override val docOptionTitle: String = "Document / ID (front and back faces)"
    override val docOptionBody: String = "Placed on its own row below the photos with 1 mm spacing"
    override val cancel: String = "Cancel"
    override val addDocTitle: String = "Add document or ID (front and back faces)"
    override val frontPick: String = "Front face photo"
    override val requiredTag: String = "(required)"
    override val backPickHint: String = "Back face photo"
    override val optionalTag: String = "(optional)"
    override val frontDone: String = "Front face ✔"
    override val backDone: String = "Back face ✔"
    override val placementTitle: String = "Placement of the two faces in printing:"
    override val samePageTitle: String = "Both faces on the same page"
    override val samePageBody: String = "Front on the right, back on the left (0.6 cm from sheet edges)"
    override val backSideTitle: String = "Back face on the sheet back (second page)"
    override val backSideBody: String = "For printing both faces (Duplex) identically back-to-back with precision"
    override val widthCm: String = "Width (cm)"
    override val heightCm: String = "Height (cm)"
    override val presetId: String = "ID card (8.5×5.4)"
    override val presetPassport: String = "Passport (12.5×8.8)"
    override val presetHalfA5: String = "Half A5 sheet (14.8×21)"
    override val copiesPrompt: String = "Number of copies to print"
    override val addDocAction: String = "Add document"
    override fun docName(widthCm: Float, heightCm: Float): String = "Document ${widthCm}×${heightCm} cm"
    override val needFront: String = "Please pick the front-face photo of the document"
    override fun editTitle(name: String): String = "Edit dimensions and copies: $name"
    override val samePageShort: String = "Both faces on the same page (right and left)"
    override val backSideShort: String = "Back face on the sheet back (second page)"
    override val saveEdit: String = "Save changes"
    override fun sheetFront(page: Int, total: Int): String = "Sheet $page of $total (front face)"
    override fun sheetBack(page: Int, total: Int): String = "Sheet $page of $total (back face - Duplex)"
    override fun sheetPlain(page: Int, total: Int): String = "Sheet $page of $total"
    override val saveThisSheet: String = "Save this sheet"
    override val exportSummaryTitle: String = "Printing sheet details (Photo Paper):"
    override fun exportSummaryBadge(pages: Int): String = "$pages sheets | 0.6 cm margins | 1 mm gap"
    override val exportDocxBtn: String = "Export to Word file (.docx) - in-front-of-text wrapping for all pages"
    override val buildingDocx: String = "Building Word file..."
    override val exportAllBtn: String = "Save all sheets at 300 DPI (card stock)"
    override val exportNote: String =
        "• All pages appear with vertical scrolling exactly like a Word file; documents are placed in their own rows below the photos with 1 mm spacing."
    override val shareChooser: String = "Share or open the Word file"
    override val shareSubject: String = "A4 photo & document grid on card stock"
    override val shareText: String =
        "A Word (.docx) file for A4 sheets with 0.6 cm margins, 1 mm spacing and free in-front-of-text wrapping was created."
    override fun wordError(message: String): String = "Word export error: $message"
    override fun processing(page: Int): String = "Processing sheet $page at 300 DPI..."
    override fun savedOk(page: Int): String = "Sheet $page saved successfully to the Pictures folder!"
    override fun saveFailed(message: String): String = "Save failed: $message"
}

/// Arabic catalog for the studio (RTL) — original wording.
object StudioStringsAr : StudioStrings {
    override val addPhotoBtn: String = "إضافة صورة"
    override val back: String = "رجوع"
    override fun tabItems(count: Int): String = "العناصر ($count)"
    override fun tabPreview(pages: Int): String = "معاينة الملف والطباعة ($pages ورقة)"
    override val emptyTitle: String = "لا توجد عناصر مضافة بعد"
    override val emptyBody: String =
        "يمكنك إضافة صور شخصية عادية أو مستندات (هوية، رخصة، بطاقة مع وجه أمامي وخلفي) لطباعتها بأعلى دقة على ورقة A4 دون أي تداخل."
    override val emptyAdd: String = "إضافة صورة أو مستند"
    override val photoBadge: String = "صورة عادية"
    override val docBadge: String = "مستند / هوية"
    override val edit: String = "تعديل"
    override val del: String = "حذف"
    override val front: String = "أمامي"
    override val backFace: String = "خلفي"
    override val none: String = "بدون"
    override fun dims(text: String): String = "القياس: $text"
    override val samePageDetail: String = "الوجهان بنفس الصفحة (يمين ويسار ببعد 0.6 سم)"
    override val backSideDetail: String = "الوجه الخلفي في ظهر الورقة (صفحة ثانية)"
    override val standardRows: String = "6 صور بالسطر كحد أقصى (عرض 3 سم)"
    override val customDims: String = "أبعاد خاصة"
    override val copiesLabel: String = "عدد النسخ:"
    override val decrease: String = "تقليل"
    override val increase: String = "زيادة"
    override fun total(photos: Int, docs: Int): String = "المجموع: $photos صورة | $docs مستند"
    override val specsShort: String = "هوامش 0.6 سم | مسافة 1 ملم | بدون تداخل"
    override val viewPaper: String = "عرض ملف الورق"
    override fun photoName(n: Int): String = "صورة $n"
    override val chooseTitle: String = "اختر نوع العنصر المراد إضافته"
    override val photoOptionTitle: String = "صورة شخصية / صور عادية"
    override val photoOptionBody: String = "تتكرر في صفوف شبكة A4 (مثل 3×4 سم، 6 بالسطر)"
    override val docOptionTitle: String = "مستند / هوية (وجه أمامي وخلفي)"
    override val docOptionBody: String = "يوضع بصف مستقل أسفل الصور وبمسافة 1 ملم"
    override val cancel: String = "إلغاء"
    override val addDocTitle: String = "إضافة مستند أو هوية (وجه أمامي وخلفي)"
    override val frontPick: String = "صورة الوجه الأمامي"
    override val requiredTag: String = "(مطلوب)"
    override val backPickHint: String = "صورة الوجه الخلفي"
    override val optionalTag: String = "(اختياري)"
    override val frontDone: String = "الوجه الأمامي ✔"
    override val backDone: String = "الوجه الخلفي ✔"
    override val placementTitle: String = "موضع الوجهين في الطباعة:"
    override val samePageTitle: String = "الوجهان بنفس الصفحة"
    override val samePageBody: String = "الأمامي على اليمين والخلفي على اليسار (0.6 سم من حواف الورقة)"
    override val backSideTitle: String = "الوجه الخلفي في ظهر الورقة (صفحة ثانية)"
    override val backSideBody: String = "لطباعة الوجهين (Duplex) متطابقين ظهر لظهر بدقة"
    override val widthCm: String = "العرض (سم)"
    override val heightCm: String = "الارتفاع (سم)"
    override val presetId: String = "بطاقة هوية (8.5×5.4)"
    override val presetPassport: String = "جواز سفر (12.5×8.8)"
    override val presetHalfA5: String = "نصف ورقة A5 (14.8×21)"
    override val copiesPrompt: String = "عدد النسخ المطلوب طباعتها"
    override val addDocAction: String = "إضافة المستند"
    override fun docName(widthCm: Float, heightCm: Float): String = "مستند ${widthCm}×${heightCm} سم"
    override val needFront: String = "يرجى تحديد صورة الوجه الأمامي للمستند"
    override fun editTitle(name: String): String = "تعديل أبعاد وعدد نسخ: $name"
    override val samePageShort: String = "الوجهان بنفس الصفحة (يمين ويسار)"
    override val backSideShort: String = "الوجه الخلفي بظهر الورقة (صفحة ثانية)"
    override val saveEdit: String = "حفظ التعديل"
    override fun sheetFront(page: Int, total: Int): String = "ورقة $page من $total (الوجه الأمامي)"
    override fun sheetBack(page: Int, total: Int): String = "ورقة $page من $total (الوجه الخلفي - Duplex)"
    override fun sheetPlain(page: Int, total: Int): String = "ورقة $page من $total"
    override val saveThisSheet: String = "حفظ هذه الورقة"
    override val exportSummaryTitle: String = "تفاصيل ورقة الطباعة (Photo Paper):"
    override fun exportSummaryBadge(pages: Int): String = "$pages ورقة | هوامش 0.6 سم | مسافة 1 ملم"
    override val exportDocxBtn: String = "تصدير إلى ملف Word (.docx) - التفاف أمام النص لكافة الصفحات"
    override val buildingDocx: String = "جاري بناء ملف Word..."
    override val exportAllBtn: String = "حفظ كافة أوراق الملف بدقة 300 DPI (ورق مقوى)"
    override val exportNote: String =
        "• تظهر جميع الصفحات بالتمرير الرأسي تماماً كملف Word، ويتم وضع المستندات بصفوف مستقلة أسفل الصور بمسافة 1 ملم."
    override val shareChooser: String = "مشاركة أو فتح ملف Word"
    override val shareSubject: String = "شبكة صور ومستندات A4 لورق الصور المقوى"
    override val shareText: String =
        "تم إنشاء ملف Word (.docx) لصفحات A4 بهامش 0.6 سم ومسافة 1 ملم والتفاف حر أمام النص."
    override fun wordError(message: String): String = "خطأ في تصدير Word: $message"
    override fun processing(page: Int): String = "جاري معالجة ورقة $page بدقة 300 DPI..."
    override fun savedOk(page: Int): String = "تم حفظ الورقة $page بنجاح في مجلد الصور!"
    override fun saveFailed(message: String): String = "فشل الحفظ: $message"
}

fun studioStringsFor(locale: String): StudioStrings = when (locale) {
    "en" -> StudioStringsEn
    else -> StudioStringsAr
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
