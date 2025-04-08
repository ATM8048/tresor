# Inhaltsverzeichnis Dokumentation
- [PasswordHash](#passwordhash)
- [Backend](#backend)
- [Frontend](#frontend)
- [SecretEncryption](#secretencryption)
- [Backend](#backend-1)
- [Frontend](#frontend-1)


# PasswordHash
## Backend
### PasswordEncryptionService
#### Konstanten
Diese Konstanten definieren Parameter für die Passwort-Verschlüsselung: Anzahl der Iterationen, Schlüssellänge und verwendeter Algorithmus.
```java
private static final int ITERATIONS = 65536;
private static final int KEY_LENGTH = 128;
private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
```
---
#### Passwort-Hashing
Diese Methode erzeugt aus einem Klartext-Passwort einen sicheren Hash im Format `salt$hash`. Der Salt ist zufällig und wird Base64-kodiert gespeichert.

```java
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
```
---
### UserController
#### createUser
wird die neue Object User erstellt und der der Password wid übergeben mit Aufruf von hashPassword Function.
```java
//transform registerUser to user
      User user = new User(
            null,
            registerUser.getFirstName(),
            registerUser.getLastName(),
            registerUser.getEmail(),
            passwordService.hashPassword(registerUser.getPassword())
            );
```
---
## Frontend
In Frontend werden alle Inputs, die ein Password erwarten, werden zu type Password geändert.
# Login
## Backend
### PasswordEncryptionService
#### verifyPassword
Diese Methode prüft, ob ein Passwort mit einem gespeicherten Hash übereinstimmt. Dazu wird der Hash rekonstruiert und verglichen.

```java
public boolean verifyPassword(String password, String storedHash) {
   String[] parts = storedHash.split("\$");
   if (parts.length != 2) {
      throw new IllegalArgumentException("Stored hash is in an invalid format");
   }

   byte[] salt = Base64.getDecoder().decode(parts[0]);

   try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
      byte[] hash = factory.generateSecret(spec).getEncoded();
      String hashOfInput = Base64.getEncoder().encodeToString(hash);
      return hashOfInput.equals(parts[1]);
   } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Error verifying password", e);
   }
}
```
### UserController
Diese Methode behandelt einen Login-POST-Request, überprüft die Eingabedaten und das Passwort, und gibt je nach Ergebnis eine JSON-Antwort mit dem Login-Status zurück.
```java
 @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/login")
   public ResponseEntity<String> login(@RequestBody LoginUser loginUser, BindingResult bindingResult) {
      // Optional: Input-Validierung
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);
         return ResponseEntity.badRequest().body(json);
      }

      // Suche den Nutzer per E-Mail
      User user = userService.findByEmail(loginUser.getEmail());
      if (user == null) {
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "Kein Benutzer mit dieser E-Mail gefunden");
         String json = new Gson().toJson(obj);
         return ResponseEntity.badRequest().body(json);
      }

      // Überprüfe das Passwort
      if (!passwordService.verifyPassword(loginUser.getPassword(), user.getPassword())) {
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "Falsche Password");
         String json = new Gson().toJson(obj);
         return ResponseEntity.badRequest().body(json);
      }

     // zurückgabe mit json.....
   }
```
### LoginUser
```java
@Value
public class LoginUser {

    @NotEmpty(message="E-Mail is required.")
    private String email;

    @NotEmpty(message="Password is required.")
    private String password;
}
```
## Frontend
### FetchUser
Es wird eine Funktion erstellt, die eine Kommunikation mit dem Backend erstellt und email und password schickt, wenn Backend ein positives Antwort antwortet, dann wird der person eingeloggt und wenn nicht wird die error message anzeigt.
```javascript
export const postLogin = async (content) => {
    const protocol = process.env.REACT_APP_API_PROTOCOL; // z.B. "http"
    const host = process.env.REACT_APP_API_HOST; // z.B. "localhost"
    const port = process.env.REACT_APP_API_PORT; // z.B. "8080"
    const path = process.env.REACT_APP_API_PATH; // z.B. "/api"
    const portPart = port ? `:${port}` : ''; // Port ist optional
    const API_URL = `${protocol}://${host}${portPart}${path}`;

    try {
        // hier wird die Daten an Backend geschickt
    } catch (error) {
        // hier weden die Error von Backend angezeigt.
    }
};
```
### Loginuser
handleSubmit soll die postLogin aufrufen mit den email und password
```javascript
 const handleSubmit = async (e) => {
        e.preventDefault()
        try {
            console.log(loginValues + "test")
            await postLogin(loginValues);
            navigate('/');
        } catch (error) {
            setLoginValues({ email: '', password: '' });
            console.error('Failed to fetch to server:', error.message);
        }
    };
```
# SecretEncryption