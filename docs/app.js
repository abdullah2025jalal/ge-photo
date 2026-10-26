/* =====================================================================
   PhotoMagic — web app (PWA)
   ---------------------------------------------------------------------
   Shares the EXACT login contract of the Android apps + the admin panel:

     /accounts/{username}          (username = trimmed, lower-cased)
       salt:            hex string (UTF-8 bytes are the PBKDF2 salt)
       hash:            "v2$<iterations>$<saltHex>$<digestHex>"  (PBKDF2)
                        ...or legacy bare sha256(salt + password) hex
       permanent:       true | false
       expiry_at:       epoch ms (temporary accounts only)
       suspended:       true | false
       suspended_until: epoch ms (0/absent = permanent suspension)

   Reads are performed over the Realtime Database REST API, so no web app
   registration / SDK is required. Security rules stay the single source
   of truth for access.
   ===================================================================== */

'use strict';

/* ------------------------------------------------------------------ */
/* Configuration                                                       */
/* ------------------------------------------------------------------ */

const DB_URL = 'https://photomagic-2a69e-default-rtdb.europe-west1.firebasedatabase.app';

const SESSION_KEY = 'photomagic.web.session.v1';
const LOCALE_KEY = 'photomagic.web.locale_v1';
const THEME_KEY = 'photomagic.web.theme_v1';
const INSTALL_DISMISS_KEY = 'photomagic.web.install_dismissed_v1';
const PREF_LOCK_PREFIX = 'photomagic.web.lock_v1.';

/** Same four channels, same order, same destinations as the Android app. */
const CONTACTS = {
  whatsapp: 'https://wa.me/qr/ZBCM2HUUDBTWB1',
  telegram: 'https://t.me/Abdullah2001jalal',
  email: 'mailto:abdullah2001jalal@gmail.com',
  channel: 'https://t.me/AbodyCode',
};

const MAX_ATTEMPTS = 5;
const MONITOR_MS = 30000;

/* ------------------------------------------------------------------ */
/* Translations (identical wording to the Android app)                 */
/* ------------------------------------------------------------------ */

const I18N = {
  ar: {
    dir: 'rtl',
    appTitle: 'PhotoMagic',
    tagline: 'استوديو طباعة شبكة الصور A4',
    taglineShort: 'استوديو طباعة شبكة الصور A4 و المستندات',
    loginTitle: 'تسجيل الدخول',
    username: 'اسم المستخدم',
    password: 'كلمة المرور',
    loginButton: 'تسجيل الدخول',
    logout: 'تسجيل الخروج',
    createAccount: 'إنشاء حساب',
    loginFailed: 'اسم المستخدم أو كلمة المرور غير صحيحة',
    accountExpired: 'انتهت صلاحية الحساب التجريبي',
    accountSuspended: 'الحساب موقوف. يرجى التواصل مع الإدارة.',
    loginOffline: 'لا يمكن تسجيل الدخول بدون اتصال بالإنترنت',
    loginChecking: 'جارٍ التحقق…',
    serverError: 'تعذّر الاتصال بالخادم، حاول مرة أخرى',
    secureRequired: 'يتطلب تسجيل الدخول اتصالاً آمناً (HTTPS)',
    lockedOut: 'محاولات كثيرة خاطئة. حاول بعد {s} ثانية',
    sessionEnded: 'انتهت الجلسة، يرجى تسجيل الدخول من جديد',
    createAccountTitle: 'إنشاء حساب',
    createAccountMessage:
      'للحصول على حساب و استخدام تجريبي مجاني للتطبيق لمدة 12 ساعة لغرض الحصول على اسم مستخدم و كلمة مرور لتسجيل الدخول و بدأ استخدام التطبيق يرجى التواصل مع المُهندس عبدالله جلال عن طريق احدى هذه الوسائل:',
    contactMe: 'تواصل معي',
    contactWhatsapp: 'واتساب',
    contactTelegram: 'تيليجرام',
    contactEmail: 'ايميل',
    contactChannel: 'قناتي على تيليجرام',
    installAppTitle: 'تثبيت التطبيق',
    installAppBody: 'أضف PhotoMagic إلى شاشتك الرئيسية — بأيقونته الخاصة وبملء الشاشة وأسرع في الفتح كل مرة.',
    installAppNow: 'تثبيت',
    installAppLater: 'لاحقاً',
    ok: 'حسناً',
    installHelpIos: 'على آيفون: اضغط زر المشاركة في المتصفح ثم "إضافة إلى الشاشة الرئيسية".',
    installHelpBrowser: 'من قائمة المتصفح (⋮) اختر "تثبيت التطبيق" أو "إضافة إلى الشاشة الرئيسية".',
    /* studio */
    tabItems: 'العناصر',
    tabPapers: 'معاينة الورق',
    addPhoto: 'إضافة صورة',
    back: 'رجوع',
    emptyTitle: 'لا توجد عناصر مضافة بعد',
    emptyBody: 'أضف صوراً شخصية (مثل 3×4 سم) أو مستندات وهوية بوجهين لطباعتها على ورقة A4.',
    addItem: 'إضافة عنصر',
    chooseType: 'اختر نوع العنصر المراد إضافته',
    photoType: 'صورة شخصية / صور عادية',
    photoBody: 'تتكرر في صفوف شبكة A4 (مثل 3×4 سم، 6 صور بالسطر كحد أقصى)',
    docType: 'مستند / هوية (وجه أمامي وخلفي)',
    docBody: 'يوضع بصف مستقل أسفل الصور مع خيار الوجهان بنفس الصفحة أو بظهر الورقة',
    cancel: 'إلغاء',
    add: 'إضافة',
    docTitle: 'إضافة مستند أو هوية (وجه أمامي وخلفي)',
    frontFace: 'الوجه الأمامي',
    backFace: 'الوجه الخلفي',
    placement: 'موضع الوجهين في الطباعة',
    samePage: 'الوجهان بنفس الصفحة',
    backSide: 'الوجه الخلفي في ظهر الورقة (صفحة ثانية)',
    widthCm: 'العرض (سم)',
    heightCm: 'الارتفاع (سم)',
    copies: 'عدد النسخ المطلوبة',
    copiesLabel: 'النسخ:',
    delete: 'حذف',
    editItemTitle: 'تعديل العنصر',
    saveEdit: 'حفظ التعديلات',
    invalidDimensions: 'تحقق من القيم: الأبعاد ١ سم أو أكثر وعدد النسخ ١ أو أكثر',
    photoChip: 'صورة عادية',
    docChip: 'مستند / هوية',
    photoName: 'صورة',
    docName: 'مستند',
    sheet: 'ورقة',
    of: 'من',
    frontSheet: 'الوجه الأمامي',
    backSheet: 'الوجه الخلفي - Duplex',
    saveThisSheet: 'حفظ هذه الورقة',
    pagesInfo: 'تظهر الأوراق بالتمرير الرأسي تماماً كملف Word',
    margins: 'هوامش 0.6 سم | مسافة 1 ملم | ورق مقوى 300 DPI',
    exportShare: '📲 مشاركة ملف Word (.docx)',
    exportDownload: '📥 تنزيل Word (.docx)',
    shareDocxText: 'ملف Word (.docx) لشبكة الصور على A4 جاهز للطباعة — من PhotoMagic',
    saveImages: '💾 حفظ صور (300 DPI)',
    addPhotoFirst: 'يرجى اختيار صورة الوجه الأمامي للمستند',
    addItemsFirst: 'أضف عناصر أولاً',
    preparing: 'جاري التحضير…',
    shared: 'تمت مشاركة ملف Word بنجاح!',
    downloaded: 'تم تنزيل الملف بنجاح',
    downloadFallback: 'تم تنزيل ملف Word لعدم توفر المشاركة المباشرة في متصفحك',
    errorPrefix: 'حدث خطأ: ',
  },
  en: {
    dir: 'ltr',
    appTitle: 'PhotoMagic',
    tagline: 'A4 photo grid printing studio',
    taglineShort: 'A4 photo grid & document printing studio',
    loginTitle: 'Sign in',
    username: 'Username',
    password: 'Password',
    loginButton: 'SIGN IN',
    logout: 'Sign out',
    createAccount: 'Create account',
    loginFailed: 'Incorrect username or password',
    accountExpired: 'Trial account has expired',
    accountSuspended: 'Account suspended. Contact the administrator.',
    loginOffline: 'Sign-in requires an internet connection',
    loginChecking: 'Verifying…',
    serverError: 'Could not reach the server, please try again',
    secureRequired: 'Sign-in requires a secure (HTTPS) connection',
    lockedOut: 'Too many failed attempts. Try again in {s}s',
    sessionEnded: 'Your session ended, please sign in again',
    createAccountTitle: 'Create account',
    createAccountMessage:
      'To get an account with a free 12-hour trial - and to receive a username and password to sign in and start using the app - please contact engineer Abdullah Jalal through one of these channels:',
    contactMe: 'Contact me',
    contactWhatsapp: 'WhatsApp',
    contactTelegram: 'Telegram',
    contactEmail: 'Email',
    contactChannel: 'My Telegram channel',
    installAppTitle: 'Install the app',
    installAppBody: 'Add PhotoMagic to your home screen — its own icon, full screen, and faster to open every time.',
    installAppNow: 'Install',
    installAppLater: 'Later',
    ok: 'OK',
    installHelpIos: 'On iPhone: tap the browser Share button, then "Add to Home Screen".',
    installHelpBrowser: 'Open the browser menu (⋮) and choose "Install app" or "Add to Home screen".',
    tabItems: 'Items',
    tabPapers: 'Paper preview',
    addPhoto: 'Add photo',
    back: 'Back',
    emptyTitle: 'No items added yet',
    emptyBody: 'Add portrait photos (e.g. 3×4 cm) or two-sided documents to print them on an A4 sheet.',
    addItem: 'Add item',
    chooseType: 'Choose the item type',
    photoType: 'Portrait / regular photos',
    photoBody: 'Repeats across the A4 grid (e.g. 3×4 cm, max 6 per row)',
    docType: 'Document / ID (front and back)',
    docBody: 'Placed on its own row below the photos, fronts+backs on the same page or on the sheet back',
    cancel: 'Cancel',
    add: 'Add',
    docTitle: 'Add a document or ID (front and back)',
    frontFace: 'Front face',
    backFace: 'Back face',
    placement: 'Faces placement when printing',
    samePage: 'Both faces on the same page',
    backSide: 'Back face on the sheet back (second page)',
    widthCm: 'Width (cm)',
    heightCm: 'Height (cm)',
    copies: 'Number of copies',
    copiesLabel: 'Copies:',
    delete: 'Delete',
    editItemTitle: 'Edit item',
    saveEdit: 'Save changes',
    invalidDimensions: 'Check the values: dimensions 1 cm or more and copies 1 or more',
    photoChip: 'Photo',
    docChip: 'Document / ID',
    photoName: 'Photo',
    docName: 'Document',
    sheet: 'Sheet',
    of: 'of',
    frontSheet: 'Front face',
    backSheet: 'Back face - Duplex',
    saveThisSheet: 'Save this sheet',
    pagesInfo: 'Sheets scroll vertically exactly like a Word document',
    margins: 'Margins 0.6 cm | 1 mm spacing | 300 DPI photo paper',
    exportShare: '📲 Share Word file (.docx)',
    exportDownload: '📥 Download Word (.docx)',
    shareDocxText: 'An A4 photo-grid Word (.docx) file ready for printing — by PhotoMagic',
    saveImages: '💾 Save images (300 DPI)',
    addPhotoFirst: 'Please pick the document front face image',
    addItemsFirst: 'Add items first',
    preparing: 'Preparing…',
    shared: 'Word file shared successfully!',
    downloaded: 'File downloaded successfully',
    downloadFallback: 'Word file downloaded (direct sharing is unavailable in this browser)',
    errorPrefix: 'Error: ',
  },
};

let locale = 'ar';
const t = (key) => (I18N[locale] && I18N[locale][key]) || (I18N.ar[key] || key);

/* ------------------------------------------------------------------ */
/* Small helpers                                                       */
/* ------------------------------------------------------------------ */

const $ = (id) => document.getElementById(id);
const show = (el) => el && el.classList.remove('hidden');
const hide = (el) => el && el.classList.add('hidden');

let toastTimer = null;
function toast(message, icon = '✅') {
  let box = $('toast');
  if (!box) {
    box = document.createElement('div');
    box.id = 'toast';
    document.body.appendChild(box);
  }
  box.textContent = `${icon}  ${message}`;
  show(box);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => hide(box), 4500);
}

function hex(buffer) {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

async function sha256Hex(text) {
  const data = new TextEncoder().encode(text);
  return hex(await crypto.subtle.digest('SHA-256', data));
}

/** PBKDF2-HMAC-SHA256 — salt is used EXACTLY as the stored string bytes. */
async function pbkdf2Hex(password, salt, iterations) {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey('raw', enc.encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: enc.encode(salt), iterations, hash: 'SHA-256' },
    key,
    256,
  );
  return hex(bits);
}

function equalHex(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

const openUrl = (url) => window.open(url, '_blank', 'noopener');

/* ------------------------------------------------------------------ */
/* Theme + language                                                    */
/* ------------------------------------------------------------------ */

function applyTheme(mode) {
  document.documentElement.setAttribute('data-theme', mode);
  localStorage.setItem(THEME_KEY, mode);
  const icon = mode === 'light' ? '🌙' : '☀️';
  ['themeBtnLogin', 'themeBtnHome'].forEach((id) => {
    const btn = $(id);
    if (btn) btn.textContent = icon;
  });
}

function initTheme() {
  applyTheme(localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light');
}

const themeToggle = () =>
  applyTheme(document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light');

function applyLocale(next) {
  locale = next === 'en' ? 'en' : 'ar';
  localStorage.setItem(LOCALE_KEY, locale);
  const html = document.documentElement;
  html.setAttribute('lang', locale);
  html.setAttribute('dir', I18N[locale].dir);

  document.querySelectorAll('[data-i18n]').forEach((el) => {
    el.textContent = t(el.getAttribute('data-i18n'));
  });
  document.querySelectorAll('[data-i18n-ph]').forEach((el) => {
    el.setAttribute('placeholder', t(el.getAttribute('data-i18n-ph')));
  });
  ['langEn', 'langEnH'].forEach((id) => $(id) && $(id).classList.toggle('on', locale === 'en'));
  ['langAr', 'langArH'].forEach((id) => $(id) && $(id).classList.toggle('on', locale === 'ar'));

  // Localize the arrow + label of the back button beside the home badge
  // (RTL: points right, LTR: points left).
  const backBtn = $('btnBackStep');
  if (backBtn) {
    const label = t('back');
    backBtn.setAttribute('aria-label', label);
    backBtn.setAttribute('title', label);
    backBtn.textContent = locale === 'ar' ? '→' : '←';
  }

  if (items.length) {
    renderItems();
    renderPapers();
    // Refresh the pre-built Word file for the current layout too.
    warmDocx();
  }
}

function initLocale() {
  applyLocale(localStorage.getItem(LOCALE_KEY) || 'ar');
}

/* ------------------------------------------------------------------ */
/* Install prompt (the rectangle with the SAME Android app icon)       */
/* ------------------------------------------------------------------ */

let deferredPrompt = null;

const isStandalone = () =>
  window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;

function isIos() {
  return (
    /iphone|ipad|ipod/i.test(navigator.userAgent) ||
    // iPadOS 13+ masquerades as a desktop Mac UA; it is still installable
    // through Safari's Share → "Add to Home Screen".
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  );
}

function installEligible() {
  // Only when opened in a BROWSER. Once the app is installed (standalone /
  // iOS home-screen) the rectangle disappears for good.
  if (isStandalone()) return false;
  if (localStorage.getItem(INSTALL_DISMISS_KEY) === '1') return false;
  return Boolean(deferredPrompt) || isIos();
}

function refreshInstallBanners() {
  const showBanner = installEligible();
  ['installLogin', 'installCreate', 'installHome'].forEach((id) =>
    showBanner ? show($(id)) : hide($(id)),
  );
}

async function triggerInstall() {
  if (deferredPrompt) {
    deferredPrompt.prompt();
    try {
      await deferredPrompt.userChoice;
    } catch (e) {
      /* ignored */
    }
    deferredPrompt = null;
    refreshInstallBanners();
    return;
  }
  $('installHelpBody').textContent = isIos() ? t('installHelpIos') : t('installHelpBrowser');
  show($('installHelp'));
}

window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  deferredPrompt = event;
  refreshInstallBanners();
});

window.addEventListener('appinstalled', () => {
  deferredPrompt = null;
  localStorage.setItem(INSTALL_DISMISS_KEY, '1');
  refreshInstallBanners();
  toast(locale === 'ar' ? 'تم تثبيت التطبيق' : 'App installed', '📲');
});

// Keep the banners honest if the display mode changes while the tab is still
// open (the user installed and came back — the rectangle must disappear).
try {
  window.matchMedia('(display-mode: standalone)').addEventListener('change', refreshInstallBanners);
} catch (e) {
  /* older Safari without the change event — nothing to do */
}

/* ------------------------------------------------------------------ */
/* Views                                                              */
/* ------------------------------------------------------------------ */

function showLogin() {
  hide($('viewHome'));
  hide($('viewCreate'));
  show($('viewLogin'));
  refreshInstallBanners();
}

function showCreate() {
  hide($('viewLogin'));
  hide($('viewHome'));
  show($('viewCreate'));
  refreshInstallBanners();
}

function showHome() {
  hide($('viewLogin'));
  hide($('viewCreate'));
  show($('viewHome'));
  refreshInstallBanners();
}

/* ------------------------------------------------------------------ */
/* Contact sheet                                                      */
/* ------------------------------------------------------------------ */

document.querySelectorAll('[data-open-contact]').forEach((btn) =>
  btn.addEventListener('click', () => show($('contactSheet'))),
);

document.querySelectorAll('.channel[data-url]').forEach((btn) =>
  btn.addEventListener('click', () => {
    hide($('contactSheet'));
    openUrl(btn.getAttribute('data-url'));
  }),
);

[$('contactSheet')].forEach((backdrop) =>
  backdrop.addEventListener('click', (event) => {
    if (event.target === backdrop) hide(backdrop);
  }),
);

/* ------------------------------------------------------------------ */
/* Authentication                                                     */
/* ------------------------------------------------------------------ */

const passwordInput = $('password');
const usernameInput = $('username');
const errorBox = $('loginError');

let busy = false;
let failedAttempts = 0;

function setError(message) {
  if (!message) {
    hide(errorBox);
    errorBox.textContent = '';
    return;
  }
  errorBox.textContent = message;
  show(errorBox);
}

function lockUntil() {
  const raw = localStorage.getItem(PREF_LOCK_PREFIX + 'lock');
  const until = raw ? Number(raw) : 0;
  return Number.isFinite(until) ? until : 0;
}

function registerFailure() {
  failedAttempts += 1;
  if (failedAttempts >= MAX_ATTEMPTS) {
    const seconds = 30 << (failedAttempts - MAX_ATTEMPTS);
    localStorage.setItem(PREF_LOCK_PREFIX + 'lock', String(Date.now() + seconds * 1000));
  }
}

function setBusy(state) {
  busy = state;
  const label = $('btnLogin');
  if (state) {
    label.innerHTML = `<span class="spinner"></span>`;
    label.disabled = true;
  } else {
    label.innerHTML = `<span>${t('loginButton')}</span>`;
    label.disabled = false;
  }
}

/** Reads one account node through the RTDB REST API. */
async function fetchAccount(username) {
  const response = await fetch(`${DB_URL}/accounts/${encodeURIComponent(username)}.json`, { cache: 'no-store' });
  if (!response.ok) {
    const error = new Error(`HTTP ${response.status}`);
    error.transient = true;
    throw error;
  }
  return response.json();
}

/**
 * Verifies credentials exactly like the Android app:
 * suspension → expiry → password hash (v2 PBKDF2 or legacy sha256).
 */
async function verifyAccount(username, password) {
  const key = username.trim().toLowerCase();
  if (!key || !password) return { status: 'invalid' };

  let account;
  try {
    account = await fetchAccount(key);
  } catch (e) {
    return { status: 'offline' };
  }
  if (!account || typeof account !== 'object') return { status: 'invalid' };
  const salt = account.salt;
  const stored = account.hash;
  if (typeof salt !== 'string' || typeof stored !== 'string') return { status: 'invalid' };

  const now = Date.now();
  const permanent = account.permanent === true;
  const expiry = typeof account.expiry_at === 'number' ? account.expiry_at : 0;
  const suspended = account.suspended === true;
  const suspendedUntil = typeof account.suspended_until === 'number' ? account.suspended_until : 0;

  if (suspended && (suspendedUntil <= 0 || now < suspendedUntil)) return { status: 'suspended' };
  if (!permanent && expiry > 0 && now >= expiry) return { status: 'expired' };

  let ok = false;
  let upgraded = null;
  if (stored.startsWith('v2$')) {
    const parts = stored.split('$');
    const iterations = parts.length === 4 ? parseInt(parts[1], 10) : NaN;
    if (Number.isFinite(iterations) && iterations > 0 && iterations <= 2000000 && parts[3].length === 64) {
      ok = equalHex(await pbkdf2Hex(password, parts[2], iterations), parts[3]);
    }
  } else if (stored.length === 64) {
    ok = equalHex(await sha256Hex(`${salt}${password}`), stored);
    if (ok) upgraded = await pbkdf2Hash(salt, password, 100000);
  }

  if (!ok) return { status: 'invalid' };

  // One-time v1 → v2 hardening, same as the Android app (best effort:
  // silently skipped when the database rules are read-only).
  if (upgraded) {
    fetch(`${DB_URL}/accounts/${encodeURIComponent(key)}.json`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hash: upgraded }),
    }).catch(() => {});
  }

  return { status: 'success', account };
}

async function pbkdf2Hash(salt, password, iterations) {
  return `v2$${iterations}$${salt}$${await pbkdf2Hex(password, salt, iterations)}`;
}

/* ---------------------------- session ----------------------------- */

let session = null;
let monitorTimer = null;

function saveSession(username) {
  session = { username, at: Date.now() };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
  session = null;
  localStorage.removeItem(SESSION_KEY);
  stopMonitor();
}

function readSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed.username === 'string' ? parsed : null;
  } catch (e) {
    return null;
  }
}

function startMonitor() {
  stopMonitor();
  monitorTimer = setInterval(async () => {
    if (!session) return;
    try {
      const account = await fetchAccount(session.username);
      const now = Date.now();
      const permanent = account && account.permanent === true;
      const expiry = account && typeof account.expiry_at === 'number' ? account.expiry_at : 0;
      const suspended = account && account.suspended === true;
      const suspendedUntil = account && typeof account.suspended_until === 'number' ? account.suspended_until : 0;
      const dead =
        !account ||
        (suspended && (suspendedUntil <= 0 || now < suspendedUntil)) ||
        (!permanent && expiry > 0 && now >= expiry);
      if (dead) signOut(t('sessionEnded'));
    } catch (e) {
      /* offline: keep the session, the next tick re-checks */
    }
  }, MONITOR_MS);
}

function stopMonitor() {
  if (monitorTimer) clearInterval(monitorTimer);
  monitorTimer = null;
}

function signOut(message) {
  clearSession();
  showLogin();
  setError(message || '');
  toast(message || t('logout'), '⏏');
}

/* ----------------------------- login ------------------------------ */

async function attemptLogin() {
  if (busy) return;
  const username = usernameInput.value.trim();
  const password = passwordInput.value;

  const lockedFor = lockUntil() - Date.now();
  if (lockedFor > 0) {
    setError(t('lockedOut').replace('{s}', String(Math.ceil(lockedFor / 1000))));
    return;
  }
  if (!username || !password) {
    setError(t('loginFailed'));
    return;
  }
  if (!window.isSecureContext || !crypto.subtle) {
    setError(t('secureRequired'));
    return;
  }
  if (navigator.onLine === false) {
    setError(t('loginOffline'));
    return;
  }

  setError('');
  setBusy(true);
  const result = await verifyAccount(username, password);
  setBusy(false);

  switch (result.status) {
    case 'success':
      failedAttempts = 0;
      localStorage.removeItem(PREF_LOCK_PREFIX + 'lock');
      passwordInput.value = '';
      // The account node's KEY is the identity (nodes do NOT store a
      // separate "username" field). Saving the normalized typed name —
      // exactly like the Android client. Reading account.username here was
      // a bug: it is undefined, JSON.stringify silently dropped it, and the
      // 30s monitor then fetched "/accounts/undefined.json" → the session
      // was killed right after a successful login.
      saveSession(username.trim().toLowerCase());
      showHome();
      startMonitor();
      break;
    case 'expired':
      registerFailure();
      setError(t('accountExpired'));
      break;
    case 'suspended':
      registerFailure();
      setError(t('accountSuspended'));
      break;
    case 'offline':
      setError(t('loginOffline'));
      break;
    default:
      registerFailure();
      setError(t('loginFailed'));
  }
}

$('btnLogin').addEventListener('click', attemptLogin);
passwordInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') attemptLogin();
});
usernameInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') passwordInput.focus();
});

$('eyeBtn').addEventListener('click', () => {
  const hidden = passwordInput.type === 'password';
  passwordInput.type = hidden ? 'text' : 'password';
  $('eyeBtn').textContent = hidden ? '🙈' : '👁';
});

$('btnCreate').addEventListener('click', showCreate);
$('btnBackToLogin').addEventListener('click', showLogin);
$('btnLogout').addEventListener('click', () => signOut(''));

/* ------------------------------------------------------------------ */
/* Studio: items, A4 plan, Word export                                 */
/* ------------------------------------------------------------------ */

const items = [];
let docFrontBitmap = null;
let docBackBitmap = null;

function loadImage(file) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.src = URL.createObjectURL(file);
  });
}

/* ----------------------------- sheets ------------------------------ */

document.querySelectorAll('[data-close-sheets]').forEach((btn) =>
  btn.addEventListener('click', () => {
    hide($('chooseSheet'));
    hide($('docSheet'));
    hide($('editSheet'));
    hide($('installHelp'));
  }),
);

$('btnOpenAdd').addEventListener('click', () => show($('chooseSheet')));

// "Add another item" under the items list opens the same chooser. This
// button existed but never had a listener, so tapping it did nothing and
// users could not add more than one item.
$('btnAddMore').addEventListener('click', () => show($('chooseSheet')));

// Back button beside the home badge: returns ONE step in the app — closes
// any open sheet first, otherwise returns from the paper-preview tab to
// the items tab. No-op when there is nothing to go back to.
$('btnBackStep').addEventListener('click', () => {
  const openSheet = [$('chooseSheet'), $('docSheet'), $('editSheet'), $('contactSheet')].find(
    (el) => !el.classList.contains('hidden')
  );
  if (openSheet) { hide(openSheet); return; }
  if (!$('panePapers').classList.contains('hidden')) {
    $('tabItems').click();
  }
});
$('pickDoc').addEventListener('click', () => {
  hide($('chooseSheet'));
  show($('docSheet'));
});

document.querySelectorAll('[data-preset]').forEach((btn) =>
  btn.addEventListener('click', () => {
    const [w, h] = btn.getAttribute('data-preset').split(',');
    $('docW').value = w;
    $('docH').value = h;
  }),
);

document.querySelectorAll('#installLogin [data-install-now], #installHome [data-install-now]').forEach((btn) =>
  btn.addEventListener('click', triggerInstall),
);
document.querySelectorAll('[data-install-later]').forEach((btn) =>
  btn.addEventListener('click', () => {
    localStorage.setItem(INSTALL_DISMISS_KEY, '1');
    refreshInstallBanners();
  }),
);

$('photoInput').addEventListener('change', async (event) => {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    const bitmap = await loadImage(file);
    items.push({
      id: `p_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      type: 'PHOTO',
      name: `${t('photoName')} ${items.filter((i) => i.type === 'PHOTO').length + 1}`,
      bitmap,
      widthCm: 3.0,
      heightCm: 4.0,
      copies: 6,
    });
  }
  event.target.value = '';
  hide($('chooseSheet'));
  renderItems();
});

$('docFrontInput').addEventListener('change', async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  docFrontBitmap = await loadImage(file);
  $('docFrontPreview').src = docFrontBitmap.src;
  show($('docFrontPreview'));
});

$('docBackInput').addEventListener('change', async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  docBackBitmap = await loadImage(file);
  $('docBackPreview').src = docBackBitmap.src;
  show($('docBackPreview'));
});

$('btnAddDoc').addEventListener('click', () => {
  if (!docFrontBitmap) {
    toast(t('addPhotoFirst'), '⚠️');
    return;
  }
  const w = parseFloat($('docW').value) || 8.5;
  const h = parseFloat($('docH').value) || 5.4;
  const copies = Math.max(1, parseInt($('docCopies').value, 10) || 1);
  const duplex = document.querySelector('input[name="duplex"]:checked').value;

  items.push({
    id: `d_${Date.now()}`,
    type: 'DOCUMENT',
    name: `${t('docName')} ${w}×${h} cm`,
    bitmap: docFrontBitmap,
    backBitmap: docBackBitmap,
    duplexMode: duplex,
    widthCm: w,
    heightCm: h,
    copies,
  });

  docFrontBitmap = null;
  docBackBitmap = null;
  hide($('docFrontPreview'));
  hide($('docBackPreview'));
  hide($('docSheet'));
  renderItems();
});

/* ---------------------------- rendering ---------------------------- */

function renderItems() {
  $('countItems').textContent = String(items.length);
  const list = $('itemsList');
  const empty = $('itemsEmpty');
  // The compact "+" add button only shows when real items exist (the hero
  // empty-box disappears then, and there must stay ONE reachable add).
  const addMore = $('btnAddMore');
  if (addMore) addMore.classList.toggle('hidden', items.length === 0);

  // The pre-built Word file is tied to this exact layout: any change to
  // the items invalidates it (it is rebuilt on tap / at warm-up).
  docxCache = null;

  if (items.length === 0) {
    hide(list);
    show(empty);
    return;
  }
  hide(empty);
  show(list);
  list.innerHTML = '';

  items.forEach((item, index) => {
    const isDoc = item.type === 'DOCUMENT';
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `
      <div class="row" style="justify-content:space-between">
        <div class="group" style="gap:8px">
          <span class="chip ${isDoc ? 'doc' : 'photo'}">${isDoc ? t('docChip') : t('photoChip')}</span>
          <b style="font-size:14px">${item.name}</b>
        </div>
        <div class="cardbtns">
          <button class="iconbtn" data-edit="${index}" type="button" aria-label="${t('editItemTitle')}" title="${t('editItemTitle')}"><svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg></button>
          <button class="iconbtn alarm" data-delete="${index}" type="button" aria-label="${t('delete')}" title="${t('delete')}"><svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg></button>
        </div>
      </div>
      <div class="row" style="margin-top:12px">
        <div class="thumb"><img src="${item.bitmap.src}" alt=""></div>
        <div style="flex:1;min-width:0">
          <b style="font-size:13px">${item.widthCm} × ${item.heightCm}</b>
          <div class="metaline">${
            isDoc
              ? item.duplexMode === 'SAME_PAGE'
                ? t('samePage')
                : t('backSide')
              : item.widthCm <= 3.05
                ? t('photoBody')
                : ''
          }</div>
        </div>
        <div class="stepper">
          <span class="metaline">${t('copiesLabel')}</span>
          <button data-step="${index}:-1" type="button">－</button>
          <b>${item.copies}</b>
          <button data-step="${index}:1" type="button">＋</button>
        </div>
      </div>`;
    list.appendChild(card);
  });

  list.querySelectorAll('[data-delete]').forEach((btn) =>
    btn.addEventListener('click', () => {
      items.splice(Number(btn.getAttribute('data-delete')), 1);
      renderItems();
      renderPapers();
    }),
  );
  list.querySelectorAll('[data-edit]').forEach((btn) =>
    btn.addEventListener('click', () => openEditSheet(Number(btn.getAttribute('data-edit')))),
  );
  list.querySelectorAll('[data-step]').forEach((btn) =>
    btn.addEventListener('click', () => {
      const [index, delta] = btn.getAttribute('data-step').split(':');
      const target = items[Number(index)];
      if (!target) return;
      target.copies = Math.max(1, target.copies + Number(delta));
      renderItems();
    }),
  );
}

/* ----------------------------- edit sheet ---------------------------- */

// Opens the ✏️ (pencil) sheet for one item so its dimensions, copies and —
// for documents — the duplex mode can be changed after it was added.
let editIndex = -1;

function openEditSheet(index) {
  const item = items[index];
  if (!item) return;
  editIndex = index;
  $('editW').value = item.widthCm;
  $('editH').value = item.heightCm;
  $('editCopies').value = item.copies;
  const isDoc = item.type === 'DOCUMENT';
  const wrap = $('editDuplexWrap');
  if (isDoc) {
    show(wrap);
    const want = item.duplexMode === 'BACK_SIDE' ? 'BACK_SIDE' : 'SAME_PAGE';
    const radio = document.querySelector(`input[name="editDuplex"][value="${want}"]`);
    if (radio) radio.checked = true;
  } else {
    hide(wrap);
  }
  show($('editSheet'));
}

$('btnSaveEdits').addEventListener('click', () => {
  const item = items[editIndex];
  if (!item) { hide($('editSheet')); return; }
  const w = parseFloat($('editW').value);
  const h = parseFloat($('editH').value);
  const copies = parseInt($('editCopies').value, 10);
  if (!(w >= 1) || !(h >= 1) || !(copies >= 1)) {
    toast(t('errorPrefix') + t('invalidDimensions'), '⚠️');
    return;
  }
  item.widthCm = w;
  item.heightCm = h;
  item.copies = copies;
  if (item.type === 'DOCUMENT') {
    const checked = document.querySelector('input[name="editDuplex"]:checked');
    if (checked) item.duplexMode = checked.value;
    // The document card shows its dimensions inside the generated name.
    item.name = `${t('docName')} ${w}×${h} cm`;
  }
  hide($('editSheet'));
  renderItems();
  renderPapers();
});

/* ------------------------- A4 layout engine ------------------------ */

function computePlan() {
  const pageWidth = 21.0;
  const pageHeight = 29.7;
  const margin = 0.6;
  const spacing = 0.1;
  const maxX = pageWidth - margin;
  const maxY = pageHeight - margin;
  const placed = [];
  let page = 0;
  let x = margin;
  let y = margin;
  let rowHeight = 0;
  let inRow = 0;
  let order = 0;

  const photos = items.filter((i) => i.type === 'PHOTO');
  const docs = items.filter((i) => i.type === 'DOCUMENT');

  for (const item of photos) {
    const w = Math.max(1.0, item.widthCm);
    const h = Math.max(1.0, item.heightCm);
    for (let c = 0; c < item.copies; c++) {
      if (x + w > maxX + 0.01 || (w <= 3.05 && inRow >= 6)) {
        y += rowHeight + spacing;
        x = margin;
        rowHeight = 0;
        inRow = 0;
      }
      if (y + h > maxY + 0.01) {
        page += 1;
        x = margin;
        y = margin;
        rowHeight = 0;
        inRow = 0;
      }
      placed.push({ item, back: false, page, x, y, w, h, order: order++ });
      inRow += 1;
      rowHeight = Math.max(rowHeight, h);
      x += w + spacing;
    }
  }

  if (inRow > 0) {
    y += rowHeight + spacing;
    x = margin;
    rowHeight = 0;
    inRow = 0;
  }

  for (const item of docs) {
    const w = Math.max(1.0, item.widthCm);
    const h = Math.max(1.0, item.heightCm);
    for (let c = 0; c < item.copies; c++) {
      if (y + h > maxY + 0.01) {
        page = placed.reduce((max, p) => Math.max(max, p.page), page) + 1;
        x = margin;
        y = margin;
      }
      if (item.duplexMode === 'BACK_SIDE') {
        placed.push({ item, back: false, page, x: margin, y, w, h, order: order++ });
        placed.push({ item, back: true, page: page + 1, x: pageWidth - margin - w, y, w, h, order: order++ });
        y += h + spacing;
      } else if (item.backBitmap) {
        placed.push({ item, back: true, page, x: margin, y, w, h, order: order++ });
        placed.push({ item, back: false, page, x: pageWidth - margin - w, y, w, h, order: order++ });
        y += h + spacing;
      } else {
        placed.push({ item, back: false, page, x: margin, y, w, h, order: order++ });
        y += h + spacing;
      }
    }
  }

  const totalPages = placed.length === 0 ? 1 : Math.max(...placed.map((p) => p.page)) + 1;
  return { placed, totalPages, pageWidth, pageHeight };
}

function drawCropped(ctx, img, dx, dy, dw, dh) {
  const target = dw / dh;
  const source = img.width / img.height;
  let sx = 0;
  let sy = 0;
  let sw = img.width;
  let sh = img.height;
  if (source > target) {
    sw = Math.round(img.height * target);
    sx = Math.round((img.width - sw) / 2);
  } else {
    sh = Math.round(img.width / target);
    sy = Math.round((img.height - sh) / 2);
  }
  ctx.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);
}

function paintPage(canvas, pageIndex, plan, dpi) {
  const width = Math.round((plan.pageWidth / 2.54) * dpi);
  const height = Math.round((plan.pageHeight / 2.54) * dpi);
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, width, height);

  plan.placed
    .filter((p) => p.page === pageIndex)
    .forEach((p) => {
      const img = p.back ? p.item.backBitmap : p.item.bitmap;
      if (!img) return;
      drawCropped(
        ctx,
        img,
        Math.round((p.x / 2.54) * dpi),
        Math.round((p.y / 2.54) * dpi),
        Math.round((p.w / 2.54) * dpi),
        Math.round((p.h / 2.54) * dpi),
      );
    });
}

function renderPapers() {
  const plan = computePlan();
  $('countPapers').textContent = String(plan.totalPages);
  const list = $('papersList');
  list.innerHTML = '';
  if (items.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'card empty';
    empty.innerHTML = `<div class="ico">📄</div><h2>${t('emptyTitle')}</h2>`;
    list.appendChild(empty);
    return;
  }

  for (let p = 0; p < plan.totalPages; p++) {
    const sheetItems = plan.placed.filter((i) => i.page === p);
    const backOnly = sheetItems.some((i) => i.back) && !sheetItems.some((i) => !i.back);
    const box = document.createElement('div');
    box.className = 'card';
    box.innerHTML = `
      <div class="paper-head">
        <b>${t('sheet')} ${p + 1} ${t('of')} ${plan.totalPages} (${backOnly ? t('backSheet') : t('frontSheet')})</b>
        <button data-save-page="${p}" type="button">💾 ${t('saveThisSheet')}</button>
      </div>
      <canvas class="page-canvas" data-canvas="${p}"></canvas>`;
    list.appendChild(box);
    paintPage(box.querySelector(`[data-canvas="${p}"]`), p, plan, 200);
  }

  list.querySelectorAll('[data-save-page]').forEach((btn) =>
    btn.addEventListener('click', () => savePageImage(Number(btn.getAttribute('data-save-page')))),
  );
}

function savePageImage(pageIndex) {
  const plan = computePlan();
  const canvas = document.createElement('canvas');
  paintPage(canvas, pageIndex, plan, 300);
  const link = document.createElement('a');
  link.download = `PhotoMagic_A4_Page${pageIndex + 1}.png`;
  link.href = canvas.toDataURL('image/png');
  link.click();
}

/* --------------------------- Word export --------------------------- */

async function buildDocx() {
  const plan = computePlan();
  const zip = new JSZip();
  const faces = [];
  const faceIds = new Map();

  plan.placed.forEach((p) => {
    const key = `${p.item.id}_${p.back ? 'b' : 'f'}`;
    const img = p.back ? p.item.backBitmap : p.item.bitmap;
    if (img && !faceIds.has(key)) {
      const relId = `rId${faces.length + 1}`;
      faceIds.set(key, relId);
      faces.push({ key, img, relId, w: p.w, h: p.h });
    }
  });

  faces.forEach((face, index) => {
    const canvas = document.createElement('canvas');
    canvas.width = 600;
    canvas.height = Math.round(600 * (face.h / face.w));
    drawCropped(canvas.getContext('2d'), face.img, 0, 0, canvas.width, canvas.height);
    zip.file(`word/media/image_${index + 1}.png`, canvas.toDataURL('image/png').split(',')[1], { base64: true });
  });

  zip.file(
    '[Content_Types].xml',
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>`,
  );
  zip.file(
    '_rels/.rels',
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>`,
  );

  let rels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">`;
  faces.forEach((face, index) => {
    rels += `<Relationship Id="${face.relId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image_${index + 1}.png"/>`;
  });
  rels += '</Relationships>';
  zip.file('word/_rels/document.xml.rels', rels);

  let doc = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
 xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
 xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
 xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>`;

  for (let p = 0; p < plan.totalPages; p++) {
    if (p > 0) doc += `<w:p><w:r><w:br w:type="page"/></w:r></w:p>`;
    doc += `<w:p><w:pPr><w:spacing w:before="0" w:after="0"/></w:pPr>`;
    plan.placed
      .filter((i) => i.page === p)
      .forEach((i) => {
        const relId = faceIds.get(`${i.item.id}_${i.back ? 'b' : 'f'}`) || 'rId1';
        const x = Math.round(i.x * 360000);
        const y = Math.round(i.y * 360000);
        const w = Math.round(i.w * 360000);
        const h = Math.round(i.h * 360000);
        doc += `<w:r><w:drawing><wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="${251658240 + i.order}" behindDoc="0" locked="0" layoutInCell="1" allowOverlap="1"><wp:simplePos x="0" y="0"/><wp:positionH relativeFrom="page"><wp:posOffset>${x}</wp:posOffset></wp:positionH><wp:positionV relativeFrom="page"><wp:posOffset>${y}</wp:posOffset></wp:positionV><wp:extent cx="${w}" cy="${h}"/><wp:wrapNone/><wp:docPr id="${i.order + 1}" name="Photo_${i.order + 1}"/><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="${i.order + 1}" name="Photo_${i.order + 1}"/><pic:cNvPicPr><a:picLocks noChangeAspect="1"/></pic:cNvPicPr></pic:nvPicPr><pic:blipFill><a:blip r:embed="${relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${w}" cy="${h}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:anchor></w:drawing></w:r>`;
      });
    doc += '</w:p>';
  }

  doc += `<w:sectPr><w:pgSz w:w="11906" w:h="16838" w:orient="portrait"/><w:pgMar w:top="340" w:right="340" w:bottom="340" w:left="340" w:header="0" w:footer="0"/></w:sectPr></w:body></w:document>`;
  zip.file('word/document.xml', doc);

  const mime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  const blob = await zip.generateAsync({ type: 'blob', mimeType: mime, compression: 'DEFLATE' });
  const name = `PhotoMagic_A4_${Date.now()}.docx`;
  return { blob, name, file: new File([blob], name, { type: mime, lastModified: Date.now() }) };
}

function downloadBlob(blob, name) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  setTimeout(() => {
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, 8000);
}

/* --------------- native Word share (the OS share UI) ---------------- *
 * Goal per platform:
 *   • iOS / iPadOS Safari     → the system share SHEET with the real file.
 *   • Android (Chrome/WV)     → the system share SHEET with the real file.
 *   • Windows 10+ Chrome/Edge → the OS share panel with the real file.
 *   • Anything else (Firefox, older iOS,…) → the real .docx is saved
 *     through the browser download, landing on the device ready to be
 *     shared with any app.
 * The .docx is PRE-BUILT (warm cache) as soon as the layout is final, so a
 * tap hands the file to the OS within the same user gesture — required on
 * iPhone/iPad, where awaiting a long build can expire the activation and
 * make navigator.share fail. Web Share needs a secure context, which the
 * published site (https) always has.
 * -------------------------------------------------------------------- */

let docxCache = null;   // { key, blob, name, file } for the CURRENT layout
let warmingDocx = false;

/** Stable fingerprint of the current page layout (what the file contains). */
function planKey(plan) {
  return `v1_${plan.totalPages}_${plan.placed
    .map((i) => `${i.item.id}_${i.back ? 'b' : 'f'}_${i.page}_${Math.round(i.x * 1000)}_${Math.round(i.y * 1000)}`)
    .join('-')}`;
}

/** Builds (or reuses) the .docx and caches it for this exact layout. */
async function getDocxPayload() {
  let key = planKey(computePlan());
  let payload = await buildDocx();
  // If the items changed while building, rebuild once so the file matches
  // the layout that is actually on screen.
  if (planKey(computePlan()) !== key) {
    key = planKey(computePlan());
    payload = await buildDocx();
  }
  docxCache = { key, ...payload };
  return docxCache;
}

/** Fire-and-forget warm-up so the share button is instant afterwards. */
function warmDocx() {
  if (items.length === 0 || warmingDocx) return;
  if (docxCache && docxCache.key === planKey(computePlan())) return;
  warmingDocx = true;
  getDocxPayload()
    .catch(() => {})
    .finally(() => {
      warmingDocx = false;
    });
}

/** Hands the real .docx file to the native OS share UI.
 *  Returns 'shared' | 'cancelled' | 'unsupported' | 'failed'. */
async function tryNativeFileShare(file) {
  if (!navigator.share || !navigator.canShare) return 'unsupported';
  let canFiles = false;
  try {
    canFiles = navigator.canShare({ files: [file] });
  } catch (e) {
    canFiles = false;
  }
  if (!canFiles) return 'unsupported';
  try {
    await navigator.share({
      files: [file],
      title: 'PhotoMagic',
      text: t('shareDocxText'),
    });
    return 'shared';
  } catch (error) {
    // The user closed the system sheet themselves — that is NOT an error.
    if (error && error.name === 'AbortError') return 'cancelled';
    return 'failed';
  }
}

async function shareDocx() {
  if (items.length === 0) {
    toast(t('addItemsFirst'), '⚠️');
    return;
  }
  const button = $('btnShareDocx');
  const original = button.textContent;
  const keyNow = planKey(computePlan());
  const warm = docxCache && docxCache.key === keyNow;

  // Busy state only while the file still needs building — when it is
  // pre-built, the OS sheet opens within the very same tap.
  if (!warm) {
    button.disabled = true;
    button.textContent = t('preparing');
  }

  try {
    const { blob, name, file } = warm ? docxCache : await getDocxPayload();
    const outcome = await tryNativeFileShare(file);

    if (outcome === 'shared') {
      toast(t('shared'), '📲');
      return;
    }
    if (outcome === 'cancelled') {
      return;
    }
    // 'unsupported' (Firefox / older iOS / no share API) or 'failed' (the
    // activation ran out during an awaited build): deliver the REAL file
    // through the browser download — it lands on the device where every
    // OS lets the user attach it anywhere without any further setup.
    downloadBlob(blob, name);
    toast(t('downloadFallback'), '📥');
  } catch (error) {
    toast(t('errorPrefix') + error.message, '⚠️');
  } finally {
    button.disabled = false;
    button.textContent = original;
  }
}

async function downloadDocx() {
  if (items.length === 0) {
    toast(t('addItemsFirst'), '⚠️');
    return;
  }
  try {
    const { blob, name } = await buildDocx();
    downloadBlob(blob, name);
    toast(t('downloaded'), '📥');
  } catch (error) {
    toast(t('errorPrefix') + error.message, '⚠️');
  }
}

$('btnShareDocx').addEventListener('click', shareDocx);
$('btnDownloadDocx').addEventListener('click', downloadDocx);
$('btnSaveImages').addEventListener('click', () => {
  if (items.length === 0) {
    toast(t('addItemsFirst'), '⚠️');
    return;
  }
  const plan = computePlan();
  for (let p = 0; p < plan.totalPages; p++) savePageImage(p);
});

$('tabItems').addEventListener('click', () => {
  $('tabItems').classList.add('on');
  $('tabPapers').classList.remove('on');
  show($('paneItems'));
  hide($('panePapers'));
});
$('tabPapers').addEventListener('click', () => {
  $('tabPapers').classList.add('on');
  $('tabItems').classList.remove('on');
  hide($('paneItems'));
  show($('panePapers'));
  renderPapers();
  // Pre-build the .docx in the background so the 📲 share button hands the
  // real file to the OS sheet within the same tap (iOS-safe).
  warmDocx();
});

/* ------------------------------------------------------------------ */
/* Localised sheet headings that live outside [data-i18n]              */
/* ------------------------------------------------------------------ */
function applySheetHeadings() {
  const margins = $('pagesInfo');
  if (margins) margins.textContent = t('margins');
}

/* ------------------------------------------------------------------ */
/* Boot                                                               */
/* ------------------------------------------------------------------ */

['themeBtnLogin', 'themeBtnHome'].forEach((id) => $(id).addEventListener('click', themeToggle));
['langEn', 'langEnH'].forEach((id) => $(id).addEventListener('click', () => applyLocale('en')));
['langAr', 'langArH'].forEach((id) => $(id).addEventListener('click', () => applyLocale('ar')));

async function boot() {
  initTheme();
  initLocale();
  applySheetHeadings();

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }

  const saved = readSession();
  if (saved) {
    try {
      const account = await fetchAccount(saved.username);
      const now = Date.now();
      const permanent = account && account.permanent === true;
      const expiry = account && typeof account.expiry_at === 'number' ? account.expiry_at : 0;
      const suspended = account && account.suspended === true;
      const suspendedUntil = account && typeof account.suspended_until === 'number' ? account.suspended_until : 0;
      const alive =
        account &&
        !(suspended && (suspendedUntil <= 0 || now < suspendedUntil)) &&
        !(!permanent && expiry > 0 && now >= expiry);
      if (alive) {
        session = { username: saved.username };
        showHome();
        startMonitor();
        return;
      }
      clearSession();
    } catch (e) {
      // Offline (or transient error) right at boot: keep the saved session
      // optimistically — the same behavior as the Android client — instead
      // of kicking the user to the login screen. The 30s monitor keeps the
      // session while offline and ends it only when the account is really
      // gone / suspended / expired.
      session = { username: saved.username };
      showHome();
      startMonitor();
      return;
    }
  }

  showLogin();
  refreshInstallBanners();
}

boot();
