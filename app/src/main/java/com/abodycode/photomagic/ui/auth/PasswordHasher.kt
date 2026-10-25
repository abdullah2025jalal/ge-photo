package com.abodycode.photomagic.ui.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/// Password hashing for `/accounts/{username}`, byte-for-byte identical to
/// the p2p dish contract (and the PhotoMagic web build):
///
///   * v2: `v2$<iterations>$<saltHex>$<pbkdf2Hex>` — PBKDF2-HMAC-SHA256
///     (RFC 2898), salt applied as the RAW UTF-8 BYTES of the stored salt
///     string (not hex-decoded — this MUST match the Dart/web verifier).
///   * v1 (legacy): bare 64-char hex `sha256(salt + password)` — verified
///     transparently, then upgraded to v2 on the next successful login.
internal object PasswordHasher {

    const val ITERATIONS: Int = 100_000
    private const val KEY_LEN: Int = 32
    private const val PREFIX: String = "v2"
    private const val MAX_ITERATIONS: Int = 2_000_000

    /// PBKDF2-HMAC-SHA256 core (RFC 2898 §5.2) over javax.crypto.Mac.
    fun pbkdf2(password: String, salt: String, iterations: Int = ITERATIONS, keyLen: Int = KEY_LEN): ByteArray {
        val pw = password.toByteArray(StandardCharsets.UTF_8)
        val sl = salt.toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(pw, "HmacSHA256"))

        val blocks = (keyLen + 31) / 32
        val out = ByteArray(keyLen)

        for (block in 1..blocks) {
            // U1 = PRF(P, S || INT_32_BE(block))
            var u = mac.doFinal(sl + intToBigEndian(block))
            val t = u.copyOf()
            for (i in 1 until iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            val offset = (block - 1) * 32
            val take = minOf(32, keyLen - offset)
            System.arraycopy(t, 0, out, offset, take)
        }
        return out
    }

    private fun intToBigEndian(v: Int): ByteArray = byteArrayOf(
        ((v ushr 24) and 0xff).toByte(),
        ((v ushr 16) and 0xff).toByte(),
        ((v ushr 8) and 0xff).toByte(),
        (v and 0xff).toByte(),
    )

    private fun hex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v))
        }
        return sb.toString()
    }

    /// v2 hash for storage next to [salt].
    fun hash(salt: String, password: String): String =
        v2Hash(salt, password, ITERATIONS)

    /// v2 hash with an explicit iteration count (used by verify()).
    fun v2Hash(salt: String, password: String, iterations: Int): String =
        StringBuilder(PREFIX)
            .append('$').append(iterations)
            .append('$').append(salt)
            .append('$').append(hex(pbkdf2(password, salt, iterations)))
            .toString()

    /// LEGACY v1: `sha256(salt+password)` — kept only to verify accounts
    /// created before the v2 upgrade; never used for new hashes.
    fun legacyHash(salt: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt$password".toByteArray(StandardCharsets.UTF_8))
        return hex(digest)
    }

    fun needsUpgrade(stored: String): Boolean = !stored.startsWith("$PREFIX$")

    /// Constant-time-ish verification of [password] against [stored],
    /// accepting BOTH formats. Returns the hash to persist afterwards
    /// (non-null only for a legacy account that just logged in).
    fun verify(salt: String, password: String, stored: String): Pair<Boolean, String?> {
        if (stored.startsWith("$PREFIX$")) {
            // v2: `v2$iter$salt$digest` — the embedded salt wins.
            val parts = stored.split("$")
            if (parts.size != 4) return false to null
            val iter = parts[1].toIntOrNull() ?: return false to null
            val storedDigest = parts[3]
            if (iter < 1 || iter > MAX_ITERATIONS || storedDigest.length != 64) return false to null
            val candidate = hex(pbkdf2(password, parts[2], iter))
            return fixedTimeEquals(candidate, storedDigest) to null
        }

        if (stored.length != 64) return false to null
        val ok = fixedTimeEquals(legacyHash(salt, password), stored)
        val upgraded = if (ok) v2Hash(salt, password, ITERATIONS) else null
        return ok to upgraded
    }

    /// Length-independent constant-time comparison.
    fun fixedTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
