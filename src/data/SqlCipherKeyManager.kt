package com.nebulousprime26.mileage_tracker.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom

class SqlCipherKeyManager(private val sharedPreferences: SharedPreferences) {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val masterKeyAlias = "sqlcipher_master_key"

    init {
        generateMasterKeyIfNeeded()
    }

    fun getOrCreateDatabaseKey(): ByteArray {
        val encryptedKey = sharedPreferences.getString("encrypted_db_key", null)
        if (encryptedKey != null) {
            return decryptKey(encryptedKey)
        } else {
            val newKey = generateRandomKey()
            val encryptedNewKey = encryptKey(newKey)
            sharedPreferences.edit().putString("encrypted_db_key", encryptedNewKey).apply()
            return newKey
        }
    }

    private fun generateMasterKeyIfNeeded() {
        if (!keyStore.containsAlias(masterKeyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenSpec = KeyGenParameterSpec.Builder(
                masterKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    private fun generateRandomKey(): ByteArray {
        val key = ByteArray(32) // 256-bit key
        SecureRandom().nextBytes(key)
        return key
    }

    private fun encryptKey(key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encryptedKey = cipher.doFinal(key)
        // Combine IV and encrypted key for storage
        val combined = iv + encryptedKey
        return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }

    private fun decryptKey(encryptedKeyString: String): ByteArray {
        val combined = android.util.Base64.decode(encryptedKeyString, android.util.Base64.DEFAULT)
        val iv = combined.copyOfRange(0, 12) // GCM IV length is 12 bytes
        val encryptedKey = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), gcmSpec)
        return cipher.doFinal(encryptedKey)
    }

    private fun getMasterKey(): SecretKey {
        return (keyStore.getEntry(masterKeyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }
}