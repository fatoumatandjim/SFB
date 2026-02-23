# Résolution des problèmes d'authentification email

## Erreur actuelle
```
org.springframework.mail.MailAuthenticationException: Authentication failed
```

## Solutions testées

### ✅ Solution 1: Configuration SSL pour port 465 (Appliquée)

La configuration a été mise à jour pour utiliser `smtps` avec SSL direct :

```properties
spring.mail.protocol=smtps
spring.mail.port=465
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory
```

**Action**: Redémarrez le backend et testez à nouveau.

### 🔄 Solution 2: Utiliser le port 587 avec STARTTLS (Alternative)

Si le port 465 ne fonctionne toujours pas, essayez cette configuration :

```properties
# Configuration Mail SMTP/IMAP - PORT 587
spring.mail.host=mail.sfb-petroleum.com
spring.mail.port=587
spring.mail.username=votre_email@sfb-petroleum.com
spring.mail.password=VOTRE_MOT_DE_PASSE
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=mail.sfb-petroleum.com
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
```

### 🔐 Solution 3: Vérifier les credentials

1. **Testez la connexion manuellement** :
```bash
# Test SMTP avec OpenSSL
openssl s_client -connect mail.sfb-petroleum.com:465 -crlf

# Ou avec telnet pour port 587
telnet mail.sfb-petroleum.com 587
```

2. **Vérifiez que le compte email autorise** :
   - ✅ Les connexions IMAP/SMTP
   - ✅ L'authentification par mot de passe (pas seulement OAuth)
   - ✅ Les applications tierces

3. **Caractères spéciaux dans le mot de passe** :
   - Si problème avec caractères spéciaux, utilisez des guillemets : `"VotreMotDePasse@"`

### 🧪 Solution 4: Tester avec un email de test

Créez un compte Gmail de test et utilisez cette configuration pour valider que le code fonctionne :

```properties
# Configuration Gmail pour test
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=votre.email.test@gmail.com
spring.mail.password=mot_de_passe_application
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Note**: Pour Gmail, vous devez créer un "Mot de passe d'application" dans les paramètres de sécurité.

### 🔍 Solution 5: Debug détaillé

Ajoutez ces propriétés pour voir les détails de la connexion :

```properties
# Debug SMTP
spring.mail.properties.mail.debug=true
spring.mail.properties.mail.smtp.debug=true
logging.level.org.springframework.mail=DEBUG
```

Regardez les logs pour voir :
- Si la connexion au serveur réussit
- Quel mécanisme d'authentification est utilisé
- Les erreurs détaillées

### 📧 Solution 6: Vérifier avec le serveur mail

Contactez l'administrateur du serveur `mail.sfb-petroleum.com` pour confirmer :

1. **Port SMTP correct** : 465 (SSL) ou 587 (STARTTLS) ?
2. **Méthode d'authentification** : LOGIN, PLAIN, CRAM-MD5 ?
3. **Restrictions IP** : Votre IP est-elle autorisée ?
4. **Quota/Limite** : Le compte a-t-il des restrictions ?

### 🆘 Solution temporaire: Désactiver l'envoi d'email

Si vous voulez tester le reste de l'application sans email :

1. Commentez l'appel à `mailSender.send()` dans `EmailServiceImpl.java` ligne 191
2. Ou créez un profil "dev" qui simule l'envoi sans vraiment envoyer

```java
// Dans EmailServiceImpl.java
@Profile("!prod")
public EmailDTO sendEmail(EmailDTO emailDTO) {
    log.info("MODE DEV: Email non envoyé (simulation)");
    // Ne pas appeler mailSender.send()
    // Juste sauvegarder en base
    return save(emailDTO);
}
```

## Checklist de débogage

- [ ] Redémarrer le backend après changement de config
- [ ] Vérifier les logs détaillés avec `mail.debug=true`
- [ ] Tester la connexion manuelle au serveur SMTP
- [ ] Vérifier que le compte email n'est pas verrouillé
- [ ] Essayer avec un autre compte email (Gmail test)
- [ ] Contacter l'administrateur du serveur mail
- [ ] Vérifier si un firewall bloque le port 465/587

## Contact support

Si le problème persiste, fournissez ces informations :
- Les logs complets avec `mail.debug=true`
- Le résultat de `openssl s_client -connect mail.sfb-petroleum.com:465`
- La configuration utilisée
- La version de Spring Boot (actuellement 4.0.0)

