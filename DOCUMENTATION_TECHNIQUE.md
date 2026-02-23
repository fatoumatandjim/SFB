# Documentation technique — GesY

Document technique du projet GesY, basé sur les configurations du backend (`apiGesy/pom.xml`) et du frontend (`GesY/package.json`).

---

## 1. Vue d’ensemble

| Composant | Dépôt / Module | Rôle |
|-----------|----------------|------|
| **Backend API** | `apiGesy` | API REST Spring Boot (données, sécurité, métier) |
| **Frontend** | `GesY` | Application Angular/Ionic (interface utilisateur) |

---

## 2. Backend — apiGesy

### 2.1 Identité du projet

| Élément | Valeur |
|--------|--------|
| **GroupId** | `com.backend` |
| **ArtifactId** | `gesy` |
| **Version** | `0.0.1-SNAPSHOT` |
| **Nom** | gesy |
| **Parent** | Spring Boot `4.0.0` |

### 2.2 Environnement d’exécution

| Paramètre | Valeur |
|-----------|--------|
| **Java** | 17 |

### 2.3 Dépendances principales

#### Framework & données

| Dépendance | Usage |
|------------|--------|
| `spring-boot-starter-data-jdbc` | Accès JDBC (Spring Data) |
| `spring-boot-starter-data-jpa` | Persistance JPA / Hibernate |
| `spring-boot-starter-jdbc` | JDBC |
| `spring-boot-starter-session-jdbc` | Sessions stockées en base |
| `spring-boot-starter-webmvc` | API REST / MVC |
| `spring-boot-starter-security` | Authentification / autorisation |

#### Sécurité & authentification

| Dépendance | Version | Usage |
|------------|---------|--------|
| `jjwt-api` | 0.12.6 | JWT (API) |
| `jjwt-impl` | 0.12.6 | Implémentation JWT (runtime) |
| `jjwt-jackson` | 0.12.6 | Sérialisation JSON JWT (runtime) |

#### Messagerie

| Dépendance | Usage |
|------------|--------|
| `spring-boot-starter-mail` | Envoi / réception d’emails |
| `jakarta.mail` | 2.0.1 — API JavaMail |

#### Bases de données (runtime)

| Dépendance | Usage |
|------------|--------|
| `mysql-connector-j` | Pilote MySQL |
| `postgresql` | Pilote PostgreSQL |

#### Outillage & utilitaires

| Dépendance | Version | Usage |
|------------|---------|--------|
| `lombok` | (héritée) | Réduction du code boilerplate (optionnel) |
| `kernel` (iText) | 8.0.2 | Génération PDF (noyau) |
| `layout` (iText) | 8.0.2 | Mise en page PDF |
| `io` (iText) | 8.0.2 | I/O iText |

### 2.4 Dépendances de test

- `spring-boot-starter-data-jdbc-test`
- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-jdbc-test`
- `spring-boot-starter-mail-test`
- `spring-boot-starter-webmvc-test`

### 2.5 Build

- **Outil** : Maven
- **Plugin** : `spring-boot-maven-plugin` (packaging exécutable, exclusion de Lombok pour le build)

---

## 3. Frontend — GesY

### 3.1 Identité du projet

| Élément | Valeur |
|--------|--------|
| **Nom** | GesY |
| **Version** | 0.0.1 |
| **Type** | Projet Ionic / Angular (application privée) |

### 3.2 Environnement d’exécution

| Outil | Version cible |
|------|----------------|
| **Node.js / npm** | Compatible avec Angular 20 et scripts définis dans `package.json` |

### 3.3 Dépendances principales

#### Framework & UI

| Dépendance | Version | Usage |
|------------|---------|--------|
| `@angular/core` | ^20.0.0 | Framework Angular |
| `@angular/common` | ^20.0.0 | Utilitaires communs |
| `@angular/forms` | ^20.0.0 | Formulaires réactifs |
| `@angular/router` | ^20.0.0 | Routage |
| `@angular/animations` | ^20.0.0 | Animations |
| `@angular/platform-browser` | ^20.0.0 | Exécution navigateur |
| `@angular/platform-browser-dynamic` | ^20.0.0 | Bootstrap dynamique |
| `@ionic/angular` | ^8.0.0 | Composants Ionic |
| `ionicons` | ^7.0.0 | Icônes Ionic |

#### Mobile (Capacitor)

| Dépendance | Version |
|------------|---------|
| `@capacitor/core` | 8.0.0 |
| `@capacitor/app` | 8.0.0 |
| `@capacitor/haptics` | 8.0.0 |
| `@capacitor/keyboard` | 8.0.0 |
| `@capacitor/status-bar` | 8.0.0 |

#### Données & utilitaires

| Dépendance | Version | Usage |
|------------|---------|--------|
| `rxjs` | ~7.8.0 | Programmation réactive |
| `zone.js` | ~0.15.0 | Zone.js (Angular) |
| `tslib` | ^2.3.0 | Helpers TypeScript |
| `jspdf` | ^3.0.4 | Génération PDF côté client |
| `jspdf-autotable` | ^5.0.2 | Tableaux dans les PDF |
| `xlsx` | ^0.18.5 | Import / export Excel |

### 3.4 Dépendances de développement

| Dépendance | Version | Usage |
|------------|---------|--------|
| `@angular/cli` | ^20.0.0 | CLI Angular |
| `@angular-devkit/build-angular` | ^20.0.0 | Build Angular |
| `@angular-eslint/*` | (divers) | Linting Angular |
| `@capacitor/cli` | 8.0.0 | CLI Capacitor |
| `@ionic/angular-toolkit` | ^12.0.0 | Outils Ionic |
| `typescript` | ~5.9.0 | Compilateur TypeScript |
| `eslint` | ^9.16.0 | Linting |
| `jasmine-core` | ~5.1.0 | Tests unitaires |
| `karma` | ~6.4.0 | Exécution des tests (navigateur) |

### 3.5 Scripts npm

| Script | Commande | Description |
|--------|----------|-------------|
| `start` | `ng serve` | Serveur de développement |
| `build` | `ng build` | Build de production |
| `watch` | `ng build --watch --configuration development` | Build en mode watch (dev) |
| `test` | `ng test` | Lancement des tests (Karma/Jasmine) |
| `lint` | `ng lint` | Vérification ESLint |
| `ng` | `ng` | Invocation du CLI Angular |

---

## 4. Synthèse de la stack technique

| Couche | Technologie |
|--------|-------------|
| **Backend** | Java 17, Spring Boot 4, Spring Data JPA/JDBC, Spring Security, JWT, MySQL/PostgreSQL, iText PDF, JavaMail |
| **Frontend** | Angular 20, Ionic 8, Capacitor 8, RxJS 7, jsPDF, xlsx |
| **Tests backend** | Spring Boot Test (Web, Data, Mail, etc.) |
| **Tests frontend** | Jasmine 5, Karma 6, TypeScript 5.9 |
| **Qualité frontend** | ESLint 9, Angular ESLint |

---

## 5. Prérequis & lancement

### Backend (apiGesy)

- **Java** : 17  
- **Maven** : version compatible avec Spring Boot 4  
- **Base de données** : MySQL et/ou PostgreSQL (selon la configuration)

```bash
cd apiGesy
mvn spring-boot:run
```

### Frontend (GesY)

- **Node.js** : version LTS supportée par Angular 20  
- **npm** : installé avec Node.js

```bash
cd GesY
npm install
ionic serve
```

L’API backend doit être configurée (URL, port) dans l’application Angular (variables d’environnement ou fichiers `environment.*.ts`) pour que le frontend puisse l’appeler.

---
