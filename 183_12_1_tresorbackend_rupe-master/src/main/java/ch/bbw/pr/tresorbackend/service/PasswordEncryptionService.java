package ch.bbw.pr.tresorbackend.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PasswordEncryptionService
 * @author Peter Rutschmann
 */
@Service
public class PasswordEncryptionService {
   //todo ergänzen!
   private static final int ITERATIONS = 65536;
   private static final int KEY_LENGTH = 128;
   private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

   public PasswordEncryptionService() {
      //todo anpassen!
   }

   /**
    * Erzeugt einen Hash für das Passwort und kombiniert diesen mit einem zufällig generierten Salt.
    * Das Format ist "salt$hash", wobei beide Teile Base64-kodiert sind.
    */
   public String hashPassword(String password) {
      SecureRandom random = new SecureRandom();
      byte[] salt = new byte[16];
      random.nextBytes(salt);

      try {
         PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
         SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
         byte[] hash = factory.generateSecret(spec).getEncoded();

         return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
         throw new RuntimeException("Error hashing password", e);
      }
   }

   /**
    * Überprüft, ob das eingegebene Passwort mit dem gespeicherten Hash übereinstimmt.
    * Dazu wird der gespeicherte String anhand des Trennzeichens "$" in Salt und Hash aufgeteilt,
    * und das eingegebene Passwort mit dem extrahierten Salt erneut gehasht.
    *
    * @param password     Das eingegebene Passwort.
    * @param storedHash   Der in der Datenbank gespeicherte Hash im Format "salt$hash".
    * @return true, wenn das Passwort korrekt ist, ansonsten false.
    */
   public boolean verifyPassword(String password, String storedHash) {
      String[] parts = storedHash.split("\\$");
      if (parts.length != 2) {
         throw new IllegalArgumentException("Stored hash is in an invalid format");
      }
      byte[] salt = Base64.getDecoder().decode(parts[0]);
      String hashOfInput;
      try {
         PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
         SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
         byte[] hash = factory.generateSecret(spec).getEncoded();
         hashOfInput = Base64.getEncoder().encodeToString(hash);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
         throw new RuntimeException("Error verifying password", e);
      }
      return hashOfInput.equals(parts[1]);
   }
}
