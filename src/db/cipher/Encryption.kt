package com.nebulousprime26.mileage_tracker.db.cipher

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Result of [Encryption.encrypt]. Holds the one-time key that the user
 * must save in order to restore the backup later.
 *
 * Note: because [key] and [blob] are `ByteArray`s, this data class
 * compares them by reference, not by content. Call [wipeKey] once the
 * key has been shown to the user.
 */
data class EncryptedBackup(
    val key: ByteArray,
    val blob: ByteArray,
    val filename: String,
) {
    /** Best-effort wipe of the key material after it has been displayed. */
    fun wipeKey() = key.fill(0)
}

object Encryption {

    private const val KEY_SIZE_BYTES = 32          // 256 bits
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val FORMAT_VERSION: Byte = 1

    private val MAGIC = "MLGB".encodeToByteArray()
    private val secureRandom = SecureRandom()
    private val filenameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss-SSS", Locale.ROOT)

    private val HEADER = MAGIC + FORMAT_VERSION
    
    /**
     * Encrypts [plaintext] with a fresh 256-bit key and returns a
     * self-describing blob plus the key and suggested filename.
     */
    fun encrypt(plaintext: ByteArray): EncryptedBackup {
        val key = ByteArray(KEY_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_SIZE_BYTES).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        cipher.updateAAD(HEADER)
        val ciphertext = cipher.doFinal(plaintext)

        val blob = HEADER + iv + ciphertext
        val stamp = filenameFormatter.format(LocalDateTime.now())

        return EncryptedBackup(key, blob, "mileage-backup-$stamp.mlgbak")
    }

    /**
     * Decrypts a blob produced by [encrypt]. Returns null on any failure —
     * wrong key, corrupted file, or an unrecognized format.
     */
    fun decryptBlob(blob: ByteArray, key: ByteArray): ByteArray? {
        if (key.size != KEY_SIZE_BYTES) return null

        val headerSize = MAGIC.size + 1 + IV_SIZE_BYTES
        if (blob.size < headerSize + GCM_TAG_BITS / 8) return null
        if (!blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
        if (blob[MAGIC.size] != FORMAT_VERSION) return null

        val ivStart = MAGIC.size + 1
        val iv = blob.copyOfRange(ivStart, ivStart + IV_SIZE_BYTES)
        val ciphertext = blob.copyOfRange(ivStart + IV_SIZE_BYTES, blob.size)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            cipher.updateAAD(HEADER)
            cipher.doFinal(ciphertext)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    /**
     * Formats a key as uppercase hex in 4-character groups, e.g.
     * "A3F2 B19C 4D7E 8A1F …". Easier to transcribe by hand than a
     * single unbroken string.
     */
    fun formatKey(key: ByteArray): String =
        key.joinToString("") { "%02X".format(Locale.ROOT, it) }
            .chunked(4)
            .joinToString(" ")

    /**
     * Parses a key from user input. Ignores whitespace and case.
     * Returns null if the input isn't a valid 64-character hex string.
     */
    fun parseKey(input: String): ByteArray? {
        val cleaned = input.filterNot { it.isWhitespace() }.uppercase(Locale.ROOT)

        if (cleaned.length != KEY_SIZE_BYTES * 2) return null
        if (cleaned.any { it !in '0'..'9' && it !in 'A'..'F' }) return null

        return ByteArray(KEY_SIZE_BYTES) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}