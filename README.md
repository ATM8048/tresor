# 📚 Dokumentation

## 📑 Inhaltsverzeichnis

- [1. Passwort Hashing](#1-passwort-hashing)
  - [1.1 Backend](#11-backend)
    - [1.1.1 PasswordEncryptionService](#111-passwordencryptionservice)
      - [Konstanten](#konstanten)
      - [Passwort-Hashing](#passwort-hashing)
    - [1.1.2 UserController – Benutzer erstellen](#112-usercontroller--benutzer-erstellen)
  - [1.2 Frontend](#12-frontend)
- [2. Login](#2-login)
  - [2.1 Backend](#21-backend)
    - [2.1.1 PasswordEncryptionService – Passwort überprüfen](#211-passwordencryptionservice--passwort-überprüfen)
    - [2.1.2 UserController – Login-Handling](#212-usercontroller--login-handling)
    - [2.1.3 LoginUser – DTO](#213-loginuser--dto)
  - [2.2 Frontend](#22-frontend)
    - [2.2.1 FetchUser](#221-fetchuser)
    - [2.2.2 Loginuser – handleSubmit](#222-loginuser--handlesubmit)
- [3. Secret Encryption](#3-secret-encryption)

---

## 1. Passwort Hashing

### 1.1 Backend

#### 1.1.1 PasswordEncryptionService

##### Konstanten

Diese Konstanten definieren Parameter für die Passwort-Verschlüsselung:

```java
private static final int ITERATIONS = 65536;
private static final int KEY_LENGTH = 128;
private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
```

##### Passwort-Hashing

Erzeugt aus einem Klartext-Passwort einen sicheren Hash im Format `salt$hash`.

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

#### 1.1.2 UserController – Benutzer erstellen

Erstellt ein neues `User`-Objekt und hashed das Passwort mit `hashPassword()`.

```java
User user = new User(
   null,
   registerUser.getFirstName(),
   registerUser.getLastName(),
   registerUser.getEmail(),
   passwordService.hashPassword(registerUser.getPassword())
);
```

---

### 1.2 Frontend

Alle Eingabefelder, die Passwörter erwarten, sind als `type="password"` definiert.

---

## 2. Login

### 2.1 Backend

#### 2.1.1 PasswordEncryptionService – Passwort überprüfen

Vergleicht ein eingegebenes Passwort mit einem gespeicherten Hash.

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

---

#### 2.1.2 UserController – Login-Handling

Diese Methode verarbeitet den Login eines Nutzers und gibt entsprechende JSON-Antworten zurück.

```java
@CrossOrigin(origins = "${CROSS_ORIGIN}")
@PostMapping("/login")
public ResponseEntity<String> login(@RequestBody LoginUser loginUser, BindingResult bindingResult) {
   if (bindingResult.hasErrors()) {
      // Fehler sammeln
   }

   User user = userService.findByEmail(loginUser.getEmail());
   if (user == null) {
      // Fehler: Benutzer nicht gefunden
   }

   if (!passwordService.verifyPassword(loginUser.getPassword(), user.getPassword())) {
      // Fehler: Passwort falsch
   }

   // Erfolg: Rückgabe von Login-JSON
}
```

---

#### 2.1.3 LoginUser – DTO

```java
@Value
public class LoginUser {
    @NotEmpty(message="E-Mail is required.")
    private String email;

    @NotEmpty(message="Password is required.")
    private String password;
}
```

---

### 2.2 Frontend

#### 2.2.1 FetchUser

Sendet die Login-Daten an das Backend und verarbeitet die Antwort.

```javascript
export const postLogin = async (content) => {
   const protocol = process.env.REACT_APP_API_PROTOCOL;
   const host = process.env.REACT_APP_API_HOST;
   const port = process.env.REACT_APP_API_PORT;
   const path = process.env.REACT_APP_API_PATH;
   const portPart = port ? \`:\${port}\` : '';
   const API_URL = \`\${protocol}://\${host}\${portPart}\${path}\`;

   try {
      // Daten an Backend senden
   } catch (error) {
      // Fehlerbehandlung
   }
};
```

---

#### 2.2.2 Loginuser – handleSubmit

Ruft `postLogin` mit den eingegebenen Login-Daten auf.

```javascript
const handleSubmit = async (e) => {
   e.preventDefault()
   try {
      await postLogin(loginValues);
      navigate('/');
   } catch (error) {
      setLoginValues({ email: '', password: '' });
      console.error('Login fehlgeschlagen:', error.message);
   }
};
```

---

## 3. Secret Encryption

*Hier kann später der Teil zu Secret Encryption ergänzt werden...*
