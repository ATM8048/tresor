package ch.bbw.pr.tresorbackend.controller;

import ch.bbw.pr.tresorbackend.model.Secret;
import ch.bbw.pr.tresorbackend.model.NewSecret;
import ch.bbw.pr.tresorbackend.model.EncryptCredentials;
import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.service.SecretService;
import ch.bbw.pr.tresorbackend.service.UserService;
import ch.bbw.pr.tresorbackend.util.EncryptUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SecretController
 * @author Peter Rutschmann
 */
@RestController
@AllArgsConstructor
@RequestMapping("api/secrets")
public class SecretController {

   private SecretService secretService;
   private UserService userService;

   // create secret REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping
   public ResponseEntity<String> createSecret2(@Valid @RequestBody NewSecret newSecret, BindingResult bindingResult) {
      // Eingabewerte validieren
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         System.out.println("SecretController.createSecret: Fehler bei der Validierung " + errors);

         // Fehler als JSON zurückgeben
         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         System.out.println("SecretController.createSecret, Validierung schlägt fehl: " + json);
         return ResponseEntity.badRequest().body(json);
      }

      System.out.println("SecretController.createSecret: Validierung erfolgreich");

      // User anhand der E-Mail finden
      User user = userService.findByEmail(newSecret.getEmail());

      // Verschlüsselung des Contents
      String encryptedContent = new EncryptUtil(newSecret.getEncryptPassword()).encrypt(newSecret.getContent().toString());

      // Die Secret-Daten in das gewünschte Format bringen
      JsonObject encryptedDataJson = new JsonObject();
      encryptedDataJson.addProperty("encryptedData", encryptedContent);

      // Verschlüsseltes Secret speichern
      Secret secret = new Secret(
              null,
              user.getId(),
              encryptedDataJson.toString()  // Das verschlüsselte JSON speichern
      );

      try {
         // Secret speichern
         Secret saved = secretService.createSecret(secret);
         System.out.println("SecretController.createSecret, Secret in der DB gespeichert: " + saved);
      } catch (Exception e) {
         e.printStackTrace();
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Speichern des Secrets: " + e.getMessage());
      }

      // Erfolgreiche Antwort zurückgeben
      JsonObject responseObj = new JsonObject();
      responseObj.addProperty("answer", "Secret saved");
      String jsonResponse = new Gson().toJson(responseObj);
      System.out.println("SecretController.createSecret " + jsonResponse);
      return ResponseEntity.accepted().body(jsonResponse);
   }

   // Build Get Secrets by userId REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byuserid")
   public ResponseEntity<List<Secret>> getSecretsByUserId(@RequestBody EncryptCredentials credentials) {
      System.out.println("SecretController.getSecretsByUserId " + credentials);

      List<Secret> secrets = secretService.getSecretsByUserId(credentials.getUserId());
      if (secrets.isEmpty()) {
         System.out.println("SecretController.getSecretsByUserId secret isEmpty");
         return ResponseEntity.notFound().build();
      }
      //Decrypt content
      EncryptUtil decryptor = new EncryptUtil(credentials.getEncryptPassword());
      for (Secret secret : secrets) {
         try {
            // content ist ein JSON-String wie: {"encryptedData": "..."}
            JsonObject contentJson = JsonParser.parseString(secret.getContent()).getAsJsonObject();
            String encryptedValue = contentJson.get("encryptedData").getAsString();

            // entschlüsseln
            String decryptedContent = decryptor.decrypt(encryptedValue);
            secret.setContent(decryptedContent);
         } catch (Exception e) {
            System.out.println("SecretController.getSecretsByEmail decryption error: " + e + " | Secret ID: " + secret.getId());
            secret.setContent("not decryptable. Wrong password?");
         }
      }

      System.out.println("SecretController.getSecretsByUserId " + secrets);
      return ResponseEntity.ok(secrets);
   }

   // Build Get Secrets by email REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byemail")
   public ResponseEntity<List<Secret>> getSecretsByEmail(@RequestBody EncryptCredentials credentials) {
      System.out.println("SecretController.getSecretsByEmail " + credentials);

      User user = userService.findByEmail(credentials.getEmail());

      List<Secret> secrets = secretService.getSecretsByUserId(user.getId());
      if (secrets.isEmpty()) {
         System.out.println("SecretController.getSecretsByEmail secret isEmpty");
         return ResponseEntity.notFound().build();
      }
      //Decrypt content
      EncryptUtil decryptor = new EncryptUtil(credentials.getEncryptPassword());
      for (Secret secret : secrets) {
         try {
            // content ist ein JSON-String wie: {"encryptedData": "..."}
            JsonObject contentJson = JsonParser.parseString(secret.getContent()).getAsJsonObject();
            String encryptedValue = contentJson.get("encryptedData").getAsString();

            // entschlüsseln
            String decryptedContent = decryptor.decrypt(encryptedValue);
            secret.setContent(decryptedContent);
         } catch (Exception e) {
            System.out.println("SecretController.getSecretsByEmail decryption error: " + e + " | Secret ID: " + secret.getId());
            secret.setContent("not decryptable. Wrong password?");
         }
      }

      System.out.println("SecretController.getSecretsByEmail " + secrets);
      return ResponseEntity.ok(secrets);
   }

   // Build Get All Secrets REST API
   // http://localhost:8080/api/secrets
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping
   public ResponseEntity<List<Secret>> getAllSecrets() {
      List<Secret> secrets = secretService.getAllSecrets();
      return new ResponseEntity<>(secrets, HttpStatus.OK);
   }

   // Build Update Secrete REST API
   // http://localhost:8080/api/secrets/1
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PutMapping("{id}")
   public ResponseEntity<String> updateSecret(
         @PathVariable("id") Long secretId,
         @Valid @RequestBody NewSecret newSecret,
         BindingResult bindingResult) {
      //input validation
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
               .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
               .collect(Collectors.toList());
         System.out.println("SecretController.createSecret " + errors);

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         System.out.println("SecretController.updateSecret, validation fails: " + json);
         return ResponseEntity.badRequest().body(json);
      }

      //get Secret with id
      Secret dbSecrete = secretService.getSecretById(secretId);
      if(dbSecrete == null){
         System.out.println("SecretController.updateSecret, secret not found in db");
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Secret not found in db");
         String json = new Gson().toJson(obj);
         System.out.println("SecretController.updateSecret failed:" + json);
         return ResponseEntity.badRequest().body(json);
      }
      User user = userService.findByEmail(newSecret.getEmail());

      //check if Secret in db has not same userid
      if(dbSecrete.getUserId() != user.getId()){
         System.out.println("SecretController.updateSecret, not same user id");
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Secret has not same user id");
         String json = new Gson().toJson(obj);
         System.out.println("SecretController.updateSecret failed:" + json);
         return ResponseEntity.badRequest().body(json);
      }
      //check if Secret can be decrypted with password
      try {
         new EncryptUtil(newSecret.getEncryptPassword()).decrypt(dbSecrete.getContent());
      } catch (EncryptionOperationNotPossibleException e) {
         System.out.println("SecretController.updateSecret, invalid password");
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Password not correct.");
         String json = new Gson().toJson(obj);
         System.out.println("SecretController.updateSecret failed:" + json);
         return ResponseEntity.badRequest().body(json);
      }
      //modify Secret in db.
      Secret secret = new Secret(
            secretId,
            user.getId(),
            new EncryptUtil(newSecret.getEncryptPassword()).encrypt(newSecret.getContent().toString())
      );
      Secret updatedSecret = secretService.updateSecret(secret);
      //save secret in db
      secretService.createSecret(secret);
      System.out.println("SecretController.updateSecret, secret updated in db");
      JsonObject obj = new JsonObject();
      obj.addProperty("answer", "Secret updated");
      String json = new Gson().toJson(obj);
      System.out.println("SecretController.updateSecret " + json);
      return ResponseEntity.accepted().body(json);
   }

   // Build Delete Secret REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @DeleteMapping("{id}")
   public ResponseEntity<String> deleteSecret(@PathVariable("id") Long secretId) {
      //todo: Some kind of brute force delete, perhaps test first userid and encryptpassword
      secretService.deleteSecret(secretId);
      System.out.println("SecretController.deleteSecret succesfully: " + secretId);
      return new ResponseEntity<>("Secret successfully deleted!", HttpStatus.OK);
   }
}
