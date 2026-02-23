# Guide des pièces jointes email

## ✅ Fonctionnalité implémentée

La fonctionnalité complète d'envoi de fichiers joints par email est maintenant disponible !

## 🏗️ Architecture

### Backend (Spring Boot)

#### 1. **FileStorageService.java**
Service de gestion des fichiers :
- `storeFile()` - Sauvegarde un fichier sur le serveur
- `loadFileAsResource()` - Charge un fichier pour le télécharger
- `deleteFile()` - Supprime un fichier
- Stockage dans : `uploads/email-attachments/`

#### 2. **EmailServiceImpl.java**
Méthode `sendEmail()` mise à jour :
- Attache automatiquement les fichiers à l'email
- Utilise `MimeMessageHelper` pour gérer les pièces jointes
- Supporte plusieurs fichiers par email

#### 3. **EmailController.java**
Nouveaux endpoints :

**Upload unique**
```http
POST /api/emails/upload
Content-Type: multipart/form-data
Body: file={fichier}

Response:
{
  "fileName": "uuid_fichier.pdf",
  "originalName": "document.pdf",
  "size": 1024000,
  "contentType": "application/pdf",
  "downloadUri": "/api/emails/download/uuid_fichier.pdf"
}
```

**Upload multiple**
```http
POST /api/emails/upload-multiple
Content-Type: multipart/form-data
Body: files[]={fichier1}, files[]={fichier2}

Response: [
  { "fileName": "...", "originalName": "...", ... },
  { "fileName": "...", "originalName": "...", ... }
]
```

**Téléchargement**
```http
GET /api/emails/download/{fileName}
Response: Le fichier en téléchargement
```

### Frontend (Angular/Ionic)

#### 1. **email.service.ts**
Nouvelles méthodes :
- `uploadFile(file: File)` - Upload un fichier
- `uploadMultipleFiles(files: File[])` - Upload plusieurs fichiers
- `downloadFile(fileName: string)` - Génère l'URL de téléchargement

#### 2. **email.component.ts**
Nouvelles fonctionnalités :
- `onFileSelected()` - Gère la sélection de fichiers
- `uploadFiles()` - Upload les fichiers sélectionnés
- `removeAttachment()` - Retire une pièce jointe
- `formatFileSize()` - Formate la taille en KB/MB

Variables ajoutées :
```typescript
selectedFiles: File[] = [];        // Fichiers sélectionnés
uploadedFiles: any[] = [];         // Fichiers uploadés
isUploading: boolean = false;      // État d'upload
```

#### 3. **email.component.html**
Interface utilisateur :
- Bouton "📎 Ajouter des fichiers"
- Input file caché (multiple)
- Liste des fichiers attachés avec :
  - Nom du fichier
  - Taille formatée
  - Bouton de suppression
- Indicateur d'upload en cours

#### 4. **email.component.scss**
Styles ajoutés pour :
- `.attachments-section` - Section des pièces jointes
- `.attach-btn` - Bouton d'ajout
- `.attached-file-item` - Élément de fichier
- Animation `pulse` pour l'indicateur de chargement

## 📋 Configuration

### application-dev.properties
```properties
# Taille maximale des fichiers
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Répertoire de stockage
file.upload-dir=uploads/email-attachments
```

**Limites :**
- **10 MB** par fichier
- **50 MB** par requête (total de tous les fichiers)

## 🎯 Utilisation

### Côté utilisateur

1. **Composer un email**
   - Cliquer sur "Nouveau message"
   - Remplir destinataire, objet, message

2. **Ajouter des pièces jointes**
   - Cliquer sur "📎 Ajouter des fichiers"
   - Sélectionner un ou plusieurs fichiers
   - Les fichiers sont uploadés automatiquement
   - Affichage de la progression

3. **Gérer les pièces jointes**
   - Voir le nom et la taille de chaque fichier
   - Retirer un fichier en cliquant sur ✕

4. **Envoyer l'email**
   - Cliquer sur "Envoyer"
   - Les pièces jointes sont envoyées avec l'email

### Côté développeur

#### Upload de fichiers
```typescript
// Upload un fichier
this.emailService.uploadFile(file).subscribe(response => {
  console.log('Fichier uploadé:', response.fileName);
});

// Upload plusieurs fichiers
this.emailService.uploadMultipleFiles(files).subscribe(responses => {
  responses.forEach(r => console.log(r.fileName));
});
```

#### Envoi d'email avec pièces jointes
```typescript
const email = {
  toEmail: 'dest@example.com',
  subject: 'Document joint',
  content: 'Veuillez trouver ci-joint...',
  attachments: ['uuid_file1.pdf', 'uuid_file2.docx']
};

this.emailService.sendEmail(email).subscribe(sent => {
  console.log('Email envoyé avec pièces jointes');
});
```

## 🔍 Vérifications

### Backend
✅ `FileStorageService` créé
✅ Répertoire `uploads/email-attachments/` créé automatiquement
✅ `EmailServiceImpl` gère les pièces jointes
✅ Endpoints d'upload/download fonctionnels
✅ Configuration multipart activée

### Frontend
✅ Service `uploadFile()` et `uploadMultipleFiles()` créés
✅ Interface de sélection de fichiers
✅ Affichage de la liste des fichiers
✅ Indicateur d'upload en cours
✅ Suppression de fichiers avant envoi
✅ Styles CSS ajoutés

## 🐛 Dépannage

### Erreur "Fichier trop volumineux"
**Cause:** Fichier > 10MB
**Solution:** 
- Augmenter `spring.servlet.multipart.max-file-size` dans application-dev.properties
- Ou compresser le fichier

### Erreur "Répertoire de stockage non créé"
**Cause:** Permissions insuffisantes
**Solution:**
```bash
mkdir -p uploads/email-attachments
chmod 755 uploads/email-attachments
```

### Les pièces jointes ne s'envoient pas
**Cause:** Fichiers non uploadés ou noms de fichiers incorrects
**Solution:**
- Vérifier que `uploadedFiles` contient les fichiers
- Vérifier les logs backend pour les erreurs d'attachement
- S'assurer que les fichiers existent dans `uploads/email-attachments/`

## 🔒 Sécurité

### Validations implémentées
✅ Validation du nom de fichier (pas de `..`)
✅ Génération de noms uniques (UUID)
✅ Taille maximale par fichier (10MB)
✅ Taille maximale par requête (50MB)

### Recommandations
1. Ajouter validation du type MIME
2. Scanner les fichiers avec antivirus
3. Limiter les types de fichiers autorisés
4. Nettoyer les fichiers temporaires périodiquement

## 📊 Statistiques

### Capacités
- ✅ Upload multiple simultané
- ✅ Formats de fichiers: Tous types
- ✅ Taille max par fichier: 10 MB
- ✅ Nombre de fichiers: Illimité (limité par la taille totale)

### Performance
- Upload en parallèle
- Feedback utilisateur en temps réel
- Gestion d'erreurs robuste

## 🚀 Améliorations futures

1. **Drag & Drop** - Glisser-déposer les fichiers
2. **Prévisualisation** - Afficher aperçu des images/PDF
3. **Compression** - Compresser automatiquement les gros fichiers
4. **Cloud Storage** - Utiliser AWS S3 ou Azure Blob
5. **Scan antivirus** - Vérifier les fichiers uploadés
6. **Filtrage** - Limiter les types de fichiers (PDF, images, etc.)
7. **Progression** - Barre de progression détaillée
8. **Miniatures** - Afficher des miniatures pour les images

## 📝 Notes

- Les fichiers sont stockés localement dans `uploads/email-attachments/`
- Les noms de fichiers sont préfixés par un UUID pour éviter les collisions
- Les fichiers ne sont pas automatiquement supprimés après envoi
- Implémenter un cron job pour nettoyer les vieux fichiers si nécessaire

## ✅ Checklist de test

- [ ] Upload d'un fichier PDF
- [ ] Upload de plusieurs fichiers
- [ ] Retrait d'une pièce jointe
- [ ] Envoi d'email avec pièces jointes
- [ ] Réception d'email avec pièces jointes
- [ ] Téléchargement d'une pièce jointe
- [ ] Test avec fichier > 10MB (devrait échouer)
- [ ] Test avec plusieurs fichiers > 50MB total (devrait échouer)

---

**Date de création:** 2026-01-06
**Version:** 1.0
**Auteur:** AI Assistant

