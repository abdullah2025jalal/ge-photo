package com.abodycode.photomagic.ui.auth

import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/// Outcome of a sign-in attempt — mirrors the p2p dish AuthResultStatus.
enum class AuthOutcome { SUCCESS, INVALID, EXPIRED, SUSPENDED, OFFLINE, SERVER_ERROR }

enum class AuthError { NONE, INVALID, EXPIRED, SUSPENDED, OFFLINE, SERVER }

/// One loginable user account under `/accounts/{username}`, exactly as the
/// admin panel (and p2p dish) writes it:
///
///   salt              hex string (RANDOM per account)
///   hash              v2 PBKDF2 wire format, or legacy bare sha256 hex
///   permanent         true|false
///   expiry_at         epoch ms (temporary accounts only)
///   suspended         true|false
///   suspended_until   epoch ms (0/absent = permanent suspension)
data class UserAccount(
    val username: String,
    val salt: String,
    val passwordHash: String,
    val permanent: Boolean,
    val expiryAtMs: Long,
    val suspended: Boolean,
    val suspendedUntilMs: Long,
) {
    /// True while the suspension is active (permanent or timed).
    fun isSuspendedAt(nowMs: Long): Boolean {
        if (!suspended) return false
        if (suspendedUntilMs <= 0) return true
        return nowMs < suspendedUntilMs
    }

    /// True when the trial lifetime has elapsed (temporary only).
    fun isExpiredAt(nowMs: Long): Boolean =
        !permanent && expiryAtMs > 0 && nowMs >= expiryAtMs
}

/// Realtime Database access over the plain REST API — no Firebase SDK
/// dependency needed. The instance lives in europe-west1 (the default
/// *.firebaseio.com host does NOT exist for this project), which is why the
/// URL is fully spelled out here.
internal object AuthApi {

    const val DB_URL: String =
        "https://photomagic-2a69e-default-rtdb.europe-west1.firebasedatabase.app"

    private const val NODE: String = "accounts"

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /// Fetches one account node; null when it does not exist or the payload
    /// is malformed. Throws [IOException] on network failure.
    private fun fetchAccountSync(key: String): UserAccount? {
        val url = "$DB_URL/$NODE/${URLEncoder.encode(key, "UTF-8")}.json"
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            if (body.isBlank() || body == "null") return null
            val json = JSONObject(body)
            val salt = json.opt("salt") as? String
            val hash = json.opt("hash") as? String
            if (salt == null || hash == null) return null
            return UserAccount(
                username = key,
                salt = salt,
                passwordHash = hash,
                permanent = json.opt("permanent") == true,
                expiryAtMs = (json.opt("expiry_at") as? Number)?.toLong() ?: 0L,
                suspended = json.opt("suspended") == true,
                suspendedUntilMs = (json.opt("suspended_until") as? Number)?.toLong() ?: 0L,
            )
        }
    }

    /// Parses one `/accounts/{key}` REST payload into [UserAccount]; null
    /// when missing or malformed.
    fun parseAccount(key: String, body: String): UserAccount? {
        if (body.isBlank() || body == "null") return null
        return try {
            val json = JSONObject(body)
            val salt = json.opt("salt") as? String
            val hash = json.opt("hash") as? String
            if (salt == null || hash == null) return null
            UserAccount(
                username = key,
                salt = salt,
                passwordHash = hash,
                permanent = json.opt("permanent") == true,
                expiryAtMs = (json.opt("expiry_at") as? Number)?.toLong() ?: 0L,
                suspended = json.opt("suspended") == true,
                suspendedUntilMs = (json.opt("suspended_until") as? Number)?.toLong() ?: 0L,
            )
        } catch (_: Exception) {
            null
        }
    }

    /// Live account re-check used by the 30 s monitor:
    ///   true  → account still exists, not suspended, not expired
    ///   false → account gone / suspended / expired (sign out now)
    ///   null  → network unreachable (keep the session, retry next tick)
    suspend fun checkAlive(key: String): Boolean? = withContext(Dispatchers.IO) {
        try {
            val account = fetchAccountSync(key)
            val now = System.currentTimeMillis()
            account != null && !account.isSuspendedAt(now) && !account.isExpiredAt(now)
        } catch (_: IOException) {
            null
        }
    }

    /// One-time v1 → v2 upgrade write (best effort, silently skipped when
    /// the database rules are read-only) — mirrors the p2p dish behavior.
    suspend fun upgradeHash(key: String, upgraded: String): Unit = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("hash", upgraded).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$DB_URL/$NODE/${URLEncoder.encode(key, "UTF-8")}.json")
                .patch(body)
                .build()
            http.newCall(request).execute().use { /* ignored */ }
        } catch (_: Exception) {
            // Best effort only.
        }
    }

    /// Verifies credentials against `/accounts/{username}` — the exact
    /// p2p dish order: suspension → expiry → password hash (v2 or legacy).
    ///
    /// [ONLINE]: network failures map to OFFLINE (matching the real-internet
    /// gate in p2p dish), other unexpected failures to SERVER_ERROR.
    suspend fun verifyCredentials(
        username: String,
        password: String,
    ): Pair<AuthOutcome, UserAccount?> = withContext(Dispatchers.IO) {
        val key = username.trim().lowercase()
        if (key.isEmpty() || password.isEmpty()) return@withContext AuthOutcome.INVALID to null

        val account: UserAccount? = try {
            fetchAccountSync(key)
        } catch (_: IOException) {
            return@withContext AuthOutcome.OFFLINE to null
        } catch (_: Exception) {
            return@withContext AuthOutcome.SERVER_ERROR to null
        }
        if (account == null) return@withContext AuthOutcome.INVALID to null

        val now = System.currentTimeMillis()
        if (account.isSuspendedAt(now)) return@withContext AuthOutcome.SUSPENDED to account
        if (account.isExpiredAt(now)) return@withContext AuthOutcome.EXPIRED to account

        // v2 PBKDF2 verification (accepts + transparently upgrades the
        // legacy sha256 format after a successful legacy login).
        val (ok, upgraded) = PasswordHasher.verify(account.salt, password, account.passwordHash)
        if (!ok) return@withContext AuthOutcome.INVALID to null

        if (upgraded != null) {
            // One-time v1 → v2 hardening: persist the slow hash so this
            // account is no longer trivially crackable from a DB dump.
            try {
                upgradeHash(key, upgraded)
            } catch (_: Exception) {
                // Best effort only.
            }
        }
        AuthOutcome.SUCCESS to account
    }
}
