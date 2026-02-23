# Debug - Pièces jointes email non envoyées

## 🔍 Diagnostic étape par étape

### Étape 1: Vérifier le répertoire de stockage

```bash
# Accéder au répertoire du projet
cd /Users/owner/Documents/MesProjets/gesY/apiGesy

# Vérifier si le répertoire existe
ls -la uploads/email-attachments/

# Si le répertoire n'existe pas, le créer
mkdir -p uploads/email-attachments
chmod 755 uploads/email-attachments
```

**Résultat attendu:** Le répertoire doit exister et avoir les permissions 755

---

### Étape 2: Tester l'API de vérification des fichiers

```bash
# Test 1: Vérifier l'état du répertoire et lister les fichiers
curl http://localhost:8080/api/emails/test-files

# Résultat attendu:
# {
#   "uploadDirectory": "/chemin/vers/uploads/email-attachments",
#   "directoryExists": true,
#   "directoryReadable": true,
#   "directoryWritable": true,
#   "files": ["fichier1.pdf (1234 bytes)", "fichier2.jpg (5678 bytes)"],
#   "fileCount": 2
# }
```

---

### Étape 3: Uploader un fichier de test

```bash
# Créer un fichier de test
echo "Ceci est un test de pièce jointe" > test.txt

# Uploader le fichier
curl -X POST http://localhost:8080/api/emails/upload \
  -F "file=@test.txt"

# Résultat attendu:
# {
#   "fileName": "uuid-random_test.txt",
#   "originalName": "test.txt",
#   "size": 34,
#   "contentType": "text/plain",
#   "downloadUri": "/api/emails/download/uuid-random_test.txt"
# }
```

**Notez le `fileName` retourné pour les tests suivants**

---

### Étape 4: Vérifier que le fichier a été uploadé

```bash
# Re-vérifier les fichiers
curl http://localhost:8080/api/emails/test-files

# Le fichier doit apparaître dans la liste
```

---

### Étape 5: Test d'envoi avec pièce jointe

```bash
# Remplacer:
# - votre.email@example.com par votre email
# - uuid-random_test.txt par le fileName obtenu à l'étape 3

curl -X POST "http://localhost:8080/api/emails/test-send-attachment?to=votre.email@example.com&fileName=uuid-random_test.txt"

# Résultat attendu:
# {
#   "success": true,
#   "fileExists": true,
#   "fileSize": 34,
#   "filePath": "/chemin/complet/vers/fichier",
#   "fileReadable": true,
#   "emailId": 123,
#   "message": "Email envoyé avec succès"
# }
```

---

### Étape 6: Vérifier les logs backend

Regardez les logs du backend Spring Boot. Vous devriez voir :

```
=== ENVOI EMAIL ===
Destinataire: votre.email@example.com
Sujet: Test pièce jointe - GesY
Pièces jointes: [uuid-random_test.txt]
Nombre de pièces jointes: 1
  - uuid-random_test.txt

Nombre de pièces jointes à attacher: 1
Pièce jointe attachée avec succès: test.txt (taille: 34 bytes)
Email envoyé avec succès à: votre.email@example.com (avec 1 pièce(s) jointe(s))
```

---

## 🐛 Problèmes courants et solutions

### Problème 1: Le répertoire n'existe pas

**Symptôme:**
```json
{
  "uploadDirectory": "/path/uploads/email-attachments",
  "directoryExists": false
}
```

**Solution:**
```bash
cd /Users/owner/Documents/MesProjets/gesY/apiGesy
mkdir -p uploads/email-attachments
chmod 755 uploads/email-attachments

# Redémarrer le backend
```

---

### Problème 2: Le fichier est uploadé mais pas dans le bon répertoire

**Solution:**
Vérifiez `application-dev.properties`:
```properties
file.upload-dir=uploads/email-attachments
```

Le chemin est relatif au répertoire de l'application.

---

### Problème 3: Le fichier n'est pas lisible

**Symptôme:**
```json
{
  "fileReadable": false
}
```

**Solution:**
```bash
# Donner les permissions de lecture
chmod 644 uploads/email-attachments/*
```

---

### Problème 4: La pièce jointe n'apparaît pas dans l'email reçu

**Vérifications:**

1. **Le fichier existe-t-il ?**
```bash
ls -la uploads/email-attachments/
```

2. **Le fichier est-il envoyé avec le bon Content-Type ?**
Vérifiez les logs pour :
```
Pièce jointe attachée avec succès: test.txt (taille: 34 bytes)
```

3. **Le serveur SMTP accepte-t-il les pièces jointes ?**
Vérifiez dans `application-dev.properties` que le debug est activé :
```properties
spring.mail.properties.mail.debug=true
```

Puis regardez les logs pour voir la communication SMTP complète.

---

### Problème 5: L'upload échoue

**Symptôme:** Erreur 500 lors de l'upload

**Solutions possibles:**

1. **Taille de fichier trop grande**
```properties
# Dans application-dev.properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

2. **Permissions insuffisantes**
```bash
chmod 755 uploads
chmod 755 uploads/email-attachments
```

3. **Espace disque insuffisant**
```bash
df -h .
```

---

## 📋 Checklist de diagnostic

Cochez chaque étape au fur et à mesure :

- [ ] Le répertoire `uploads/email-attachments` existe
- [ ] Le répertoire a les bonnes permissions (755)
- [ ] L'endpoint `/test-files` retourne `directoryExists: true`
- [ ] L'upload d'un fichier test fonctionne
- [ ] Le fichier apparaît dans la liste des fichiers
- [ ] Le fichier est lisible (`fileReadable: true`)
- [ ] L'endpoint `/test-send-attachment` retourne `success: true`
- [ ] Les logs montrent "Pièce jointe attachée avec succès"
- [ ] Les logs montrent "Email envoyé avec succès"
- [ ] L'email est bien reçu
- [ ] La pièce jointe apparaît dans l'email reçu

---

## 🔬 Test depuis l'interface web

1. **Ouvrir la console du navigateur** (F12)
2. **Aller dans l'onglet Network**
3. **Composer un email et ajouter un fichier**
4. **Observer les requêtes:**

**Requête 1: Upload**
```
POST /api/emails/upload-multiple
Status: 200
Response: [{fileName: "uuid_fichier.pdf", ...}]
```

**Requête 2: Envoi**
```
POST /api/emails/send
Body: {
  toEmail: "...",
  subject: "...",
  content: "...",
  attachments: ["uuid_fichier.pdf"]
}
Status: 200
```

5. **Vérifier que `attachments` contient bien le nom du fichier uploadé**

---

## 🆘 Si rien ne fonctionne

### Créer un test minimal

Créez un fichier `TestAttachment.java` dans le package `email` :

```java
package com.backend.gesy.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import java.io.File;

@Component
public class TestAttachment implements CommandLineRunner {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Override
    public void run(String... args) throws Exception {
        // Créer un fichier de test
        File testFile = new File("test-attachment.txt");
        if (!testFile.exists()) {
            java.nio.file.Files.writeString(testFile.toPath(), "Test de pièce jointe");
        }
        
        // Envoyer un email de test
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("${MAIL_USERNAME}"); // Variable d'environnement
        helper.setTo("destinataire@example.com"); // Remplacer par votre email
        helper.setSubject("Test pièce jointe au démarrage");
        helper.setText("Si vous recevez ce fichier, les pièces jointes fonctionnent !");
        
        FileSystemResource file = new FileSystemResource(testFile);
        helper.addAttachment("test.txt", file);
        
        mailSender.send(message);
        System.out.println("✅ Email de test envoyé avec pièce jointe !");
    }
}
```

Redémarrez le backend. Un email de test sera envoyé automatiquement au démarrage.

**Si cet email de test arrive avec la pièce jointe**, le problème est dans le code du service.
**Si cet email de test arrive sans la pièce jointe**, le problème est la configuration SMTP.

---

## 📞 Support

Si le problème persiste après tous ces tests, fournissez :
1. Les logs complets du backend
2. Le résultat de `/test-files`
3. Le résultat de `/test-send-attachment`
4. Les headers de l'email reçu
5. La configuration SMTP utilisée

