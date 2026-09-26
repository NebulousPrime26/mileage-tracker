package com.nebulousprime26.mileage_tracker.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption for trip backups. The key is generated fresh
 * for each export and never stored — the user is shown it once and
 * must write it down to be able to import later.
 */
object TripCrypto {

    private const val KEY_SIZE_BYTES = 32          // 256 bits
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val FORMAT_VERSION = 1.toByte()

    private val MAGIC = byteArrayOf(
        'M'.code.toByte(), 'L'.code.toByte(),
        'G'.code.toByte(), 'B'.code.toByte(),
    )

    fun generateKey(): ByteArray =
        ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }

    /**
     * Formats a key as uppercase hex in 4-character groups, e.g.
     * "A3F2 B19C 4D7E 8A1F …". Easier to transcribe by hand than a
     * single unbroken string.
     */
    fun formatKey(key: ByteArray): String =
        key.joinToString("") { "%02X".format(it) }
            .chunked(4)
            .joinToString(" ")

    /**
     * Parses a key from user input. Ignores whitespace and case.
     * Returns null if the input isn't a valid 64-character hex string.
     */
    fun parseKey(input: String): ByteArray? {
        val cleaned = input.filterNot { it.isWhitespace() }.uppercase()
        if (cleaned.length != KEY_SIZE_BYTES * 2) return null
        if (!cleaned.all { it in '0'..'9' || it in 'A'..'F' }) return null
        return ByteArray(KEY_SIZE_BYTES) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * Encrypts [plaintext] and returns a self-describing blob:
     *   MAGIC (4 bytes) | VERSION (1 byte) | IV (12 bytes) | CIPHERTEXT + GCM TAG
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE_BYTES) { "Key must be $KEY_SIZE_BYTES bytes" }
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        val ciphertext = cipher.doFinal(plaintext)

        return MAGIC + byteArrayOf(FORMAT_VERSION) + iv + ciphertext
    }

    /**
     * Decrypts a blob produced by [encrypt]. Returns null on any failure —
     * wrong key, corrupted file, or an unrecognized format.
     */
    fun decrypt(blob: ByteArray, key: ByteArray): ByteArray? {
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
            cipher.doFinal(ciphertext)
        } catch (_: Throwable) {
            null
        }
    }
}