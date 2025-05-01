package ch.bbw.pr.tresorbackend.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptUtil
 * Used to encrypt content.
 * Not implemented yet.
 * @author Peter Rutschmann
 */
public class EncryptUtil {

   private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
   private static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA256";
   private static final int ITERATIONS = 65536;
   private static final int KEY_LENGTH = 128;
   private static final int SALT_LENGTH = 16;
   private static final int IV_LENGTH = 16;

   private final String password;

   public EncryptUtil(String password) {
      this.password = password;
   }

   /**
    * Encrypts a given plaintext string using AES encryption with CBC mode and PKCS5 padding.
    * The encryption process includes:
    * - Generating a random salt and IV
    * - Deriving a secret key from the provided password and salt using PBKDF2
    * - Encrypting the data
    * - Combining salt + IV + encrypted data and encoding the result with Base64
    *
    * @param data the plaintext string to encrypt
    * @return a Base64-encoded string containing salt + IV + encrypted data
    * @throws RuntimeException if any encryption step fails
    */
   public String encrypt(String data) {
      try {
         // Generate salt
         byte[] salt = new byte[SALT_LENGTH];
         SecureRandom random = new SecureRandom();
         random.nextBytes(salt);

         // Derive key
         SecretKey secretKey = deriveKey(password, salt);

         // Generate IV
         byte[] iv = new byte[IV_LENGTH];
         random.nextBytes(iv);
         IvParameterSpec ivSpec = new IvParameterSpec(iv);

         // Encrypt
         Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
         cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
         byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

         // Combine: salt + iv + ciphertext
         ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + encrypted.length);
         buffer.put(salt);
         buffer.put(iv);
         buffer.put(encrypted);

         return Base64.getEncoder().encodeToString(buffer.array());

      } catch (Exception e) {
         throw new RuntimeException("Encryption failed", e);
      }
   }

   /**
    * Decrypts a Base64-encoded string that was encrypted using the `encrypt` method.
    * The method:
    * - Decodes the Base64 string
    * - Extracts the salt, IV, and ciphertext
    * - Derives the original encryption key using the salt and password
    * - Decrypts the ciphertext and returns the original plaintext
    *
    * @param base64Data the Base64-encoded string (salt + IV + ciphertext)
    * @return the original decrypted plaintext string
    * @throws RuntimeException if decryption fails (e.g., wrong password or data corrupted)
    */
   public String decrypt(String base64Data) {
      try {
         byte[] allBytes = Base64.getDecoder().decode(base64Data);
         ByteBuffer buffer = ByteBuffer.wrap(allBytes);

         // Extract salt
         byte[] salt = new byte[SALT_LENGTH];
         buffer.get(salt);

         // Extract IV
         byte[] iv = new byte[IV_LENGTH];
         buffer.get(iv);
         IvParameterSpec ivSpec = new IvParameterSpec(iv);

         // Extract ciphertext
         byte[] ciphertext = new byte[buffer.remaining()];
         buffer.get(ciphertext);

         // Derive key
         SecretKey secretKey = deriveKey(password, salt);

         // Decrypt
         Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
         cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
         byte[] original = cipher.doFinal(ciphertext);

         return new String(original, StandardCharsets.UTF_8);

      } catch (Exception e) {
         throw new RuntimeException("Decryption failed", e);
      }
   }

   private SecretKey deriveKey(String password, byte[] salt) throws Exception {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION);
      byte[] keyBytes = factory.generateSecret(spec).getEncoded();
      return new SecretKeySpec(keyBytes, "AES");
   }
}
