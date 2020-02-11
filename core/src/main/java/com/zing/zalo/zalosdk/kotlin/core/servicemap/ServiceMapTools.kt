package com.zing.zalo.zalosdk.kotlin.core.servicemap

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.GZIPInputStream
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

internal object ServiceMapTools
{
    private const val USER_NAME = "ZALO"
    private const val AES_KEY = "zalo@123"
     
     @Throws(IOException::class, NoSuchAlgorithmException::class, InvalidKeyException::class, NoSuchPaddingException::class, IllegalBlockSizeException::class, BadPaddingException::class)
     fun decryptString(encryptString: String): String
     {
          val decryptPass = decryptPass(encryptString, USER_NAME, AES_KEY)
          val decompress = decompress(decryptPass)
          return String(decompress)
     }
     
     @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, NoSuchPaddingException::class, IllegalBlockSizeException::class, BadPaddingException::class, IllegalArgumentException::class)
     private fun decryptPass(encryptString: String, username: String, aesKey: String): ByteArray
     {
          val key = aesKey + username
          val digest = MessageDigest.getInstance("MD5")
          digest.update(key.toByteArray())
          val keyBytes = digest.digest()
          val keySpec = SecretKeySpec(keyBytes, "AES")

         val rawByte = hexToByte(encryptString)
          val cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding")
          cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec)
          
          return cipher.doFinal(rawByte)
     }
     
     @Throws(IllegalArgumentException::class)
     private fun hexToByte(hex: String?): ByteArray
     {
          if (hex == null || hex.length % 2 != 0) throw IllegalArgumentException("Invalid hex string :" + hex!!)
          
          val bb = ByteArrayOutputStream()
          
          var i = 0
          while (i < hex.length)
          {
               bb.write(Integer.parseInt(hex.substring(i, i + 2), 16).toByte().toInt())
               i += 2
          }
          return bb.toByteArray()
     }
     
     @Throws(IOException::class)
     private fun decompress(contentBytes: ByteArray): ByteArray
     {
          val out = zipInputToByteArrayOutputStream(GZIPInputStream(ByteArrayInputStream(contentBytes)))
          return out.toByteArray()
     }
     
     @Throws(IOException::class)
     private fun zipInputToByteArrayOutputStream(gzipInput: GZIPInputStream): ByteArrayOutputStream
     {
          val buffer = ByteArray(1024)
          val out = ByteArrayOutputStream()
          
          var len: Int = gzipInput.read(buffer)
          while (len > 0)
          {
               out.write(buffer, 0, len)
               len = gzipInput.read(buffer)
          }
          
          gzipInput.close()
          out.close()
          return out
     }
}