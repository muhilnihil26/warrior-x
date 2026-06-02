package com.example.data.local

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun deriveKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = passphrase.toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(bytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(data: String, keySpec: SecretKeySpec): String {
        if (data.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(keySpec.encoded.copyOfRange(0, 16))
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedData: String, keySpec: SecretKeySpec): String {
        if (encryptedData.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(keySpec.encoded.copyOfRange(0, 16))
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "🔓 [Decryption Error - Invalid Key or Corrupted Payload]"
        }
    }
}
