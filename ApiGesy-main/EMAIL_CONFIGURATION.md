# Configuration de la Messagerie Email

## Vue d'ensemble
Ce module permet la gestion complète des emails avec support SMTP/IMAP pour l'envoi et la réception de messages.

## Configuration du serveur mail

### Paramètres de connexion
Les paramètres de connexion sont configurés dans `application-dev.properties` :

```properties
# Serveur mail
spring.mail.host=mail.sfb-petroleum.com
spring.mail.username=votre_email@sfb-petroleum.com
spring.mail.password=VOTRE_MOT_DE_PASSE

# SMTP (Envoi) - Port 465
spring.mail.port=465
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true

# IMAP (Réception) - Port 993
spring.mail.imap.host=mail.sfb-petroleum.com
spring.mail.imap.port=993
```

**IMPORTANT:** Remplacez `YOUR_EMAIL_PASSWORD` par le mot de passe réel de votre compte email.

### Ports utilisés
- **SMTP (Envoi):** Port 465 (SSL/TLS)
- **POP3:** Port 995 (disponible mais non utilisé)
- **IMAP (Réception):** Port 993 (SSL/TLS)

## Structure de la base de données

### Table `emails`
```sql
CREATE TABLE emails (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_name VARCHAR(255),
  from_email VARCHAR(255) NOT NULL,
  to_email VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  preview TEXT,
  content TEXT,
  date_sent DATETIME NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  is_starred BOOLEAN DEFAULT FALSE,
  folder VARCHAR(50) NOT NULL,
  message_id VARCHAR(255),
  in_reply_to VARCHAR(255),
  cc VARCHAR(255),
  bcc VARCHAR(255)
);

CREATE TABLE email_attachments (
  email_id BIGINT,
  attachment VARCHAR(255),
  FOREIGN KEY (email_id) REFERENCES emails(id)
);
```

## API Endpoints

### Emails
- `GET /api/emails` - Récupérer tous les emails
- `GET /api/emails/{id}` - Récupérer un email par ID
- `POST /api/emails` - Créer un email (brouillon)
- `PUT /api/emails/{id}` - Mettre à jour un email
- `DELETE /api/emails/{id}` - Supprimer un email (déplace vers corbeille)

### Dossiers
- `GET /api/emails/folder/{folder}` - Emails d'un dossier (inbox, sent, draft, trash)
- `GET /api/emails/folder/{folder}/unread` - Emails non lus
- `GET /api/emails/folder/{folder}/starred` - Emails favoris
- `GET /api/emails/folder/{folder}/count` - Nombre d'emails non lus
- `PUT /api/emails/{id}/move/{folder}` - Déplacer vers un dossier

### Actions
- `PUT /api/emails/{id}/read` - Marquer comme lu
- `PUT /api/emails/{id}/unread` - Marquer comme non lu
- `PUT /api/emails/{id}/star` - Ajouter/Retirer des favoris
- `POST /api/emails/send` - Envoyer un email
- `POST /api/emails/sync` - Synchroniser avec le serveur IMAP

### Recherche
- `GET /api/emails/folder/{folder}/search?query={query}` - Rechercher dans un dossier

### Statistiques
- `GET /api/emails/counts` - Nombre d'emails par dossier

## Utilisation Frontend

### Service Email
```typescript
import { EmailService } from '../services/email.service';

constructor(private emailService: EmailService) {}

// Récupérer les emails
this.emailService.getEmailsByFolder('inbox').subscribe(emails => {
  this.emails = emails;
});

// Envoyer un email
this.emailService.sendEmail({
  toEmail: 'destinataire@example.com',
  subject: 'Objet',
  content: 'Contenu du message'
}).subscribe(sentEmail => {
  console.log('Email envoyé', sentEmail);
});

// Synchroniser les emails
this.emailService.syncEmails().subscribe(newEmails => {
  console.log('Nouveaux emails:', newEmails);
});
```

## Fonctionnalités

### ✅ Envoi d'emails
- Support SMTP avec SSL/TLS
- Copie (CC) et copie cachée (BCC)
- Sauvegarde automatique dans "Envoyés"

### ✅ Réception d'emails
- Synchronisation IMAP
- Récupération des 50 derniers messages
- Détection des doublons via Message-ID

### ✅ Gestion des emails
- 4 dossiers: Boîte de réception, Envoyés, Brouillons, Corbeille
- Marquer comme lu/non lu
- Favoris (étoiles)
- Recherche dans les emails
- Prévisualisation automatique

### ✅ Interface utilisateur
- Vue en 3 colonnes (sidebar, liste, détail)
- Responsive mobile
- Recherche en temps réel
- Compteurs de messages non lus

## Sécurité

### Recommandations
1. **Ne jamais committer le mot de passe** dans le fichier de configuration
2. Utiliser des variables d'environnement en production
3. Activer l'authentification à deux facteurs sur le compte email
4. Utiliser des mots de passe d'application si disponible

### Variables d'environnement (Production)
```bash
export MAIL_HOST=mail.sfb-petroleum.com
export MAIL_USERNAME=votre_email@sfb-petroleum.com
export MAIL_PASSWORD=your_secure_password
export MAIL_PORT=465
export IMAP_PORT=993
```

## Dépendances Maven

Ajoutez ces dépendances dans `pom.xml` si elles ne sont pas présentes :

```xml
<!-- Spring Boot Mail -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- JavaMail API -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>2.0.1</version>
</dependency>
```

## Dépendances NPM (Frontend)

Aucune dépendance supplémentaire requise. Le module utilise :
- `@angular/common/http` (déjà inclus)
- `rxjs` (déjà inclus)

## Tests

### Tester l'envoi d'email
```bash
curl -X POST http://localhost:8080/api/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "toEmail": "test@example.com",
    "subject": "Test",
    "content": "Ceci est un test"
  }'
```

### Tester la synchronisation
```bash
curl -X POST http://localhost:8080/api/emails/sync
```

## Troubleshooting

### Erreur d'authentification
- Vérifiez le nom d'utilisateur et le mot de passe
- Assurez-vous que le compte email permet les connexions IMAP/SMTP
- Vérifiez les paramètres de sécurité du compte email

### Erreur de connexion
- Vérifiez que les ports 465 (SMTP) et 993 (IMAP) sont ouverts
- Vérifiez le nom d'hôte du serveur mail
- Testez la connexion avec `telnet mail.sfb-petroleum.com 465`

### Emails non récupérés
- Vérifiez les logs du serveur
- Assurez-vous que le dossier INBOX existe
- Vérifiez les quotas du compte email

## Support
Pour toute question ou problème, consultez la documentation Spring Mail :
- https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.email

