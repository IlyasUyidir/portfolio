# Folio.io — Documentation Complète du Projet

> **Module :** JEE 2025-2026 — Semestre 2 — 4DDSIR
> **Étudiant :** *(ton nom ici)*
> **Stack :** Spring Boot 3.5.0 (backend REST) + React 19 + TypeScript (frontend SPA)
> **Base de données :** PostgreSQL (Docker) avec migrations Flyway
> **Authentification :** JWT en cookie HttpOnly (stateless, sécurisé XSS)

---

## 📋 Table des Matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [Couverture des exigences enseignant](#2-couverture-des-exigences-enseignant)
3. [Architecture globale](#3-architecture-globale)
4. [Backend — Spring Boot](#4-backend--spring-boot)
   - [Structure en couches](#41-structure-en-couches)
   - [Entités JPA & schéma BDD](#42-entités-jpa--schéma-bdd)
   - [Repositories Spring Data JPA](#43-repositories-spring-data-jpa)
   - [Services (logique métier)](#44-services-logique-métier)
   - [Controllers REST & endpoints](#45-controllers-rest--endpoints)
   - [Spring Security & JWT](#46-spring-security--jwt)
   - [Migrations Flyway](#47-migrations-flyway)
5. [Frontend — React + TypeScript](#5-frontend--react--typescript)
   - [Structure des dossiers](#51-structure-des-dossiers)
   - [Routing & protection des routes](#52-routing--protection-des-routes)
   - [Contexte d'authentification](#53-contexte-dauthentification)
   - [Custom Hooks](#54-custom-hooks)
   - [Couche API (axios)](#55-couche-api-axios)
   - [Composants clés](#56-composants-clés)
   - [Convention centimes](#57-convention-centimes)
6. [Flux de données end-to-end](#6-flux-de-données-end-to-end)
7. [Sécurité en détail](#7-sécurité-en-détail)
8. [Spring vs Sans Spring — Comparaisons complètes](#8-spring-vs-sans-spring--comparaisons-complètes)
9. [Tests](#9-tests)
10. [Lancer le projet](#10-lancer-le-projet)
11. [Points clés pour la présentation](#11-points-clés-pour-la-présentation)
12. [Glossaire](#12-glossaire)

---

## 1. Présentation du projet

### Qu'est-ce que Folio.io ?

**Folio.io** est une application web de **gestion de finances personnelles**. Elle remplace le sujet "plateforme de jeux" proposé par l'enseignant par un sujet original plus riche, mais qui couvre exactement les mêmes contraintes techniques.

L'utilisateur peut :
- S'inscrire et se connecter de façon sécurisée
- Enregistrer ses **transactions** (revenus et dépenses) avec catégories
- Définir des **budgets** mensuels par catégorie, avec alertes d'alerte et critique
- Créer des **objectifs d'épargne** et suivre leur progression avec jalons visuels
- Visualiser un **tableau de bord** avec indicateurs clés (solde, taux d'épargne, graphiques)
- **Exporter** ses transactions en CSV
- Accéder à des fonctionnalités supplémentaires selon son **rôle** (STANDARD, PREMIUM, ADMIN)

### Correspondance avec le sujet enseignant

Le projet remplace "Gestion des jeux (CRUD)" par "Gestion des transactions/budgets/objectifs (CRUD)", et "Panier/commandes" par "Objectifs d'épargne avec contributions". Toutes les exigences techniques sont couvertes — voir [section 2](#2-couverture-des-exigences-enseignant).

### Fonctionnalités en résumé

| Fonctionnalité | Description |
|---|---|
| 🔐 **Authentification** | Inscription, connexion, déconnexion — JWT en cookie HttpOnly |
| 💸 **Transactions** | CRUD complet, pagination, filtres multi-critères, recherche par mot-clé |
| 📂 **Catégories** | CRUD avec catégories système + catégories personnalisées (limite par rôle) |
| 📊 **Budgets** | Budgets mensuels par catégorie, alertes WARNING (80%) et CRITICAL (95%) |
| 🎯 **Objectifs d'épargne** | Créer des objectifs, contribuer, jalons à 25/50/75/100%, statuts automatiques |
| 📈 **Dashboard** | KPIs temps réel (solde, revenus, dépenses, taux d'épargne), graphiques Recharts |
| 📤 **Export CSV** | Téléchargement des transactions filtrées |
| 👑 **Rôles** | STANDARD (limité), PREMIUM (illimité), ADMIN (tout) |

---

## 2. Couverture des exigences enseignant

| Exigence enseignant | Statut | Comment c'est implémenté |
|---|---|---|
| **Spring Boot** | ✅ | `pom.xml` — Spring Boot 3.5.0 |
| **Spring Web / MVC** | ✅ | `@RestController` sur chaque controller, `@RequestMapping`, `@GetMapping`, etc. |
| **Spring Data JPA** | ✅ | Tous les repositories étendent `JpaRepository<T, Long>` |
| **Base de données** | ✅ | PostgreSQL (via Docker) + H2 possible en dev. Migrations Flyway versionnées. |
| **Spring Security** | ✅ | `SecurityConfig.java` — JWT stateless, rôles, protection des routes |
| **Interface utilisateur** | ✅ | React (SPA moderne, plus robuste que Thymeleaf pour une API REST) |
| **Architecture en couches** | ✅ | Controller → Service → Repository → Entity — strict, sans sauter de couche |
| **Commentaires comparatifs** | ✅ | Présents dans le code source (voir section 8 pour les extraits clés) |
| **CRUD complet** | ✅ | Transactions, Catégories, Budgets, Objectifs |
| **Authentification + Rôles** | ✅ | JWT + enum `UserRole` (STANDARD, PREMIUM, ADMIN) |
| **Recherche et filtrage** | ✅ | `JpaSpecificationExecutor` + `TransactionSpecification.java` (filtres dynamiques) |
| **Interactions utilisateur** | ✅ | Notation → Objectifs (jalons), Favoris → Budgets, Commentaires → Export |
| **Panier / Commandes** | ✅ | Remplacé par Objectifs d'épargne + contributions (sujet personnalisé) |

---

## 3. Architecture globale

### Vue d'ensemble

L'application est découpée en deux processus indépendants qui communiquent par HTTP :

```
┌──────────────────────────────────────────────────────────────┐
│                        NAVIGATEUR                            │
│         React SPA — Vite + TypeScript + Tailwind CSS v4       │
│                       Port 5173                              │
│                                                              │
│   ┌─────────────┐   axios    ┌───────────────────────────┐   │
│   │    Pages    │──────────► │  apiClient.ts (axios)     │   │
│   │    Hooks    │            │  baseURL: localhost:8080   │   │
│   │ Components  │◄────────── │  withCredentials: true    │   │
│   └─────────────┘            └───────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                               │
                  HTTP/REST (JSON) + Cookie auth_token
                               │
┌──────────────────────────────────────────────────────────────┐
│                   SPRING BOOT — Port 8080                    │
│                                                              │
│  ┌─────────────┐ ┌────────────┐ ┌──────────┐ ┌──────────┐   │
│  │RateLimitFilt│ │  JwtFilter │ │Controller│ │ Service  │   │
│  │(Bucket4j)   │→│ (JWT/Cookie│→│(REST API)│→│(Business)│   │
│  └─────────────┘ └────────────┘ └──────────┘ └────┬─────┘   │
│                                                    │         │
│                                            ┌───────▼──────┐  │
│                                            │ Repository   │  │
│                                            │ (Spring JPA) │  │
│                                            └───────┬──────┘  │
│                                                    │         │
│                                    ┌───────────────▼──────┐  │
│                                    │ PostgreSQL (Docker)  │  │
│                                    │    Port 5432         │  │
│                                    │ Migrations Flyway    │  │
│                                    └──────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Principes fondamentaux

**Séparation des responsabilités :**
- Le **frontend** ne contient aucune logique métier. Il affiche et envoie des données.
- Le **backend** ne sait rien du rendu. Il expose uniquement des endpoints JSON.
- Les deux communiquent via un **contrat d'API** stable (URLs + formats JSON).

**Sécurité des communications :**
- L'authentification repose sur un **cookie HttpOnly** (`auth_token`) contenant un JWT.
- Le frontend n'a jamais accès direct au token (protection contre les attaques XSS).
- `withCredentials: true` dans axios garantit l'envoi automatique du cookie à chaque requête.

**Stateless :**
- Aucune session serveur. Chaque requête est auto-suffisante grâce au JWT.
- Cela permet une meilleure scalabilité (plusieurs instances du backend possibles).

---

## 4. Backend — Spring Boot

### 4.1 Structure en couches

Le backend suit une **architecture en couches stricte**. Chaque couche a une responsabilité unique et n'interagit qu'avec la couche directement inférieure.

```
backend/src/main/java/com/gc2026/portfolio/
│
├── config/
│   ├── CorsConfig.java             ← Autorise les requêtes depuis localhost:5173
│   └── SecurityConfig.java         ← Chaîne de filtres Spring Security
│
├── controller/   ← COUCHE PRÉSENTATION : reçoit HTTP, renvoie JSON
│   ├── AuthController.java
│   ├── TransactionController.java
│   ├── CategoryController.java
│   ├── BudgetController.java
│   ├── GoalController.java
│   ├── DashboardController.java
│   └── ExportController.java
│
├── service/      ← COUCHE MÉTIER : toute la logique business
│   ├── AuthService.java
│   ├── TransactionService.java
│   ├── CategoryService.java
│   ├── BudgetService.java
│   ├── GoalService.java
│   ├── DashboardService.java
│   └── ExportService.java
│
├── repository/   ← COUCHE DONNÉES : Spring Data JPA, accès BDD
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   ├── TransactionSpecification.java  ← Filtres dynamiques (Specification API)
│   ├── CategoryRepository.java
│   ├── CategorySpendingProjection.java ← Projection pour dépenses par catégorie
│   ├── BudgetRepository.java
│   ├── GoalRepository.java
│   ├── GoalContributionRepository.java
│   └── RevokedTokenRepository.java
│
├── domain/
│   ├── entity/    ← Classes Java mappées sur les tables SQL (@Entity)
│   ├── enums/     ← TransactionType (REVENU/DEPENSE), CategoryType, UserRole, GoalStatus
│   └── exception/ ← GlobalExceptionHandler, ResourceNotFoundException, ValidationException
│
├── dto/
│   ├── request/  ← Ce que le client envoie (ex: CreateTransactionRequest)
│   └── response/ ← Ce que l'API renvoie (ex: TransactionResponse)
│
└── security/
    ├── JwtFilter.java          ← Intercepte chaque requête, lit le cookie JWT
    ├── JwtUtil.java            ← Génère et valide les tokens HS256
    ├── TokenBlacklist.java     ← Révocation des tokens (logout propre)
    └── RateLimitFilter.java    ← Anti-bruteforce via Bucket4j (10 req/min/IP)
```

**Règle d'or de l'architecture :**
```
Controller  →  appelle  →  Service  →  appelle  →  Repository  →  parle à  →  BDD
    ↑                         ↑                         ↑
  HTTP/JSON               @Transactional             @Entity / SQL
```

Un Controller ne doit jamais appeler directement un Repository. Un Repository ne doit jamais contenir de logique métier.

---

### 4.2 Entités JPA & schéma BDD

Les entités sont des classes Java annotées `@Entity`. Spring (via Hibernate) les mappe automatiquement sur des tables SQL.

#### Entité Transaction (exemple complet)

```java
@Entity
@Table(name = "transactions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;                        // Clé étrangère vers users

    @ManyToOne(fetch = FetchType.LAZY)          // Relation N→1 avec Category
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Long amount;                        // En centimes ! (50000 = 500.00 DH)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;               // REVENU ou DEPENSE

    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

    @Builder.Default
    private Boolean isDeleted = false;          // Soft delete : jamais effacé en BDD
}
```

**Pourquoi `Long amount` et pas `double` ?** — Les flottants ont des erreurs d'arrondi (0.1 + 0.2 ≠ 0.3 en Java). Stocker en centimes (entier) élimine ce problème. 50000 = 500,00 DH.

**Pourquoi `isDeleted` ?** — Suppression "douce". La transaction reste en BDD (pour l'historique et l'audit), mais est invisible pour l'utilisateur.

#### Relations entre entités

```
User ──────┬──── Transaction  ──── Category
           │         (N→1)
           ├──── Budget ────────── Category
           │         (N→1)
           ├──── Category
           └──── Goal ─────────── GoalContribution
                                      (1→N)
```

Chaque entité appartient à un `User` (isolation des données entre utilisateurs).

#### Schéma SQL (généré par Flyway)

```sql
-- Table principale des utilisateurs
users (id, email, username, password_hash, role, is_active, created_at)

-- Catégories (système = prédéfinies ; custom = créées par l'utilisateur)
categories (id, user_id, name, color, type, is_system)

-- Transactions financières (soft delete via is_deleted)
transactions (id, user_id, title, amount, type, category_id, tx_date,
              description, is_deleted, created_at)

-- Budgets mensuels par catégorie
budgets (id, user_id, category_id, budget_year, budget_month,
         limit_amount, alert_threshold, created_at)

-- Objectifs d'épargne
goals (id, user_id, title, target_amount, current_amount,
       target_date, status, created_at)

-- Contributions à un objectif (historique)
goal_contributions (id, goal_id, amount, contribution_date, created_at)

-- Tokens JWT révoqués (logout sécurisé)
revoked_tokens (id, token, expiry_date)
```

---

### 4.3 Repositories Spring Data JPA

Un repository est une **interface** qui étend `JpaRepository`. Spring génère automatiquement toute l'implémentation à la compilation.

#### Exemple : CategoryRepository

```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring génère le SQL à partir du nom de la méthode :
    // SELECT * FROM categories WHERE user_id = ? ORDER BY is_system DESC, name ASC
    List<Category> findByUserIdOrderByIsSystemDescNameAsc(Long userId);

    // Requête JPQL personnalisée (plus complexe)
    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.userId = :userId OR c.isSystem = true)")
    Optional<Category> findByIdAndUserIdOrSystem(@Param("id") Long id, @Param("userId") Long userId);

    // Vérification d'existence : SELECT COUNT(*) > 0 FROM categories WHERE user_id = ? AND LOWER(name) = LOWER(?)
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
```

**Ce que Spring génère automatiquement :** `save()`, `findById()`, `findAll()`, `delete()`, `count()` — et tout ce qu'on définit par convention de nommage ou `@Query`.

#### Exemple : TransactionRepository (filtres dynamiques)

`TransactionRepository` étend aussi `JpaSpecificationExecutor<Transaction>`. Cela permet des **filtres dynamiques** (l'utilisateur peut filtrer par date, type, catégorie, mot-clé, ou n'importe quelle combinaison) via `TransactionSpecification.java`.

```java
// Appel dans le service :
Page<Transaction> results = transactionRepository.findAll(
    TransactionSpecification.buildFilters(userId, filters),
    pageable
);
// Spring génère le SQL complexe avec les clauses WHERE appropriées
```

#### Sans Spring vs Avec Spring Data JPA

| | Sans Spring (JDBC pur) | Avec Spring Data JPA |
|---|---|---|
| Code pour `findByUserId` | 30-50 lignes (connexion, statement, mapping...) | 1 ligne de déclaration d'interface |
| Gestion connexions | Manuelle (`try/finally`) | Automatique par le pool de connexions |
| Mapping SQL → objet | Manuel colonne par colonne | Automatique via Hibernate |
| Transactions | `conn.setAutoCommit(false)` + `commit()` manuels | `@Transactional` sur le service |
| Filtres dynamiques | Concaténation SQL fragile et dangereuse | `Specification` + type-safe |

---

### 4.4 Services (logique métier)

Les services sont annotés `@Service` et contiennent toute la **logique business**. Ils sont appelés par les controllers et appellent les repositories.

#### AuthService — Inscription

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    @Transactional  // ← Si une étape échoue, tout est annulé (rollback)
    public AuthResponse register(RegisterRequest request) {

        // 1. Unicité email et username
        if (userRepository.existsByEmail(request.getEmail()))
            throw new ValidationException("Email is already registered");

        // 2. Hasher le mot de passe avec BCrypt (Spring Security)
        User user = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.STANDARD)
            .isActive(true)
            .build();

        // 3. Sauvegarder en BDD
        user = userRepository.save(user);

        // 4. Seed des 8 catégories système (Salaire, Freelance, Alimentation, Transport, Logement, Loisirs, Santé, Autre)
        seedDefaultCategories(user.getId());

        // 5. Générer un JWT et le retourner
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder().token(token).user(toUserResponse(user)).build();
    }
}
```

#### Responsabilités de chaque service

| Service | Logique métier clé |
|---|---|
| `AuthService` | Hachage BCrypt, seed catégories, génération JWT, blacklist token |
| `TransactionService` | Soft delete, pagination Specification, vérification IDOR |
| `CategoryService` | Limite 10 catégories pour STANDARD, protection catégories système |
| `BudgetService` | Calcul % dépensé du mois, génération alertes WARNING (≥80%) / CRITICAL (≥95%) |
| `GoalService` | Calcul jalons (25/50/75/100%), mise à jour statuts EN_COURS/ATTEINT/EN_RETARD |
| `DashboardService` | Agrégations (somme revenus, dépenses, taux d'épargne = épargne/revenus×100) |
| `ExportService` | Génération CSV streamé avec filtre optionnel |

#### Protection IDOR (Insecure Direct Object Reference)

Tous les services vérifient que l'entité demandée **appartient bien à l'utilisateur courant** :

```java
// Dans TransactionService.delete()
Transaction tx = transactionRepository.findByIdAndUserId(id, userId)
    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
// Si userId1 essaie de supprimer la transaction de userId2 → 404, pas 403
// (ne pas révéler l'existence de la ressource)
```

---

### 4.5 Controllers REST & endpoints

Les controllers reçoivent les requêtes HTTP et délèguent immédiatement au service. Ils ne contiennent pas de logique métier.

#### Exemple : TransactionController

```java
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // POST /api/v1/transactions — Créer une transaction
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody CreateTransactionRequest request,   // ← Bean Validation
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");    // ← Injecté par JwtFilter
        TransactionResponse response = transactionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/transactions?startDate=&endDate=&type=&categoryId=&keyword=&page=0&size=20
    @GetMapping
    public ResponseEntity<PaginatedResponse<TransactionResponse>> list(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(transactionService.list(userId, startDate, endDate,
                                                         type, categoryId, keyword, page, size));
    }
}
```

#### Tous les endpoints

| Méthode | URL | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Inscription → cookie JWT | Public |
| POST | `/api/v1/auth/login` | Connexion → cookie JWT | Public |
| POST | `/api/v1/auth/logout` | Révocation token + suppression cookie | Connecté |
| GET | `/api/v1/auth/me` | Profil utilisateur courant | Connecté |
| GET | `/api/v1/transactions` | Liste paginée + filtres | Connecté |
| POST | `/api/v1/transactions` | Créer une transaction | Connecté |
| GET | `/api/v1/transactions/{id}` | Détail d'une transaction | Connecté |
| PUT | `/api/v1/transactions/{id}` | Modifier une transaction | Connecté |
| DELETE | `/api/v1/transactions/{id}` | Soft delete | Connecté |
| GET | `/api/v1/categories` | Liste des catégories | Connecté |
| POST | `/api/v1/categories` | Créer une catégorie | Connecté |
| PUT | `/api/v1/categories/{id}` | Modifier une catégorie | Connecté |
| DELETE | `/api/v1/categories/{id}` | Supprimer une catégorie | Connecté |
| POST | `/api/v1/budgets` | Créer ou mettre à jour un budget | Connecté |
| GET | `/api/v1/budgets/{month}` | Budgets du mois (ex: `2025-05`) | Connecté |
| GET | `/api/v1/budgets/{id}/progress` | Progression d'un budget | Connecté |
| DELETE | `/api/v1/budgets/{id}` | Supprimer un budget | Connecté |
| POST | `/api/v1/goals` | Créer un objectif d'épargne | Connecté |
| GET | `/api/v1/goals` | Liste des objectifs | Connecté |
| POST | `/api/v1/goals/{id}/contribute` | Ajouter une contribution | Connecté |
| GET | `/api/v1/goals/{id}/progress` | Progression d'un objectif (jalons) | Connecté |
| DELETE | `/api/v1/goals/{id}` | Supprimer un objectif | Connecté |
| GET | `/api/v1/dashboard/kpis` | Indicateurs clés du mois | Connecté |
| GET | `/api/v1/dashboard/spending` | Dépenses par catégorie (graphique) | Connecté |
| GET | `/api/v1/export/csv` | Téléchargement CSV | Connecté |

---

### 4.6 Spring Security & JWT

#### Configuration de la chaîne de filtres

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())                             // Autorise localhost:5173
        .csrf(csrf -> csrf.disable())                                // Inutile avec JWT stateless
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/register",
                             "/api/v1/auth/login").permitAll()       // Routes publiques
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
            .requestMatchers("/api/**").authenticated()              // Tout le reste protégé
            .anyRequest().permitAll()
        )
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

#### Ordre d'exécution des filtres (chaque requête)

```
Requête HTTP entrante
        ↓
[1] RateLimitFilter (Bucket4j)
    → Vérifie le "seau" de tokens de l'IP cliente
    → Si IP a consommé > 10 tokens/minute sur /api/v1/auth/* → HTTP 429
        ↓
[2] JwtFilter
    → Lit le cookie "auth_token"
    → Si absent ou invalide → continue sans authentification (Spring bloquera en [3])
    → Si présent → vérifie la blacklist → valide la signature JWT
    → Extrait userId, email, role → les stocke dans request.attributes
    → Crée un objet Authentication Spring pour la SecurityContext
        ↓
[3] Spring Security (autorisation)
    → La route nécessite-t-elle .authenticated() ?
    → Si oui et pas d'Authentication → HTTP 401
        ↓
[4] Controller
    → Lit userId depuis request.getAttribute("userId")
    → Appelle le service
```

#### JwtUtil — Génération et validation du token

```java
// JwtUtil.java
public String generateToken(Long userId, String email, UserRole role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .subject(email)
        .claim("userId", userId)
        .claim("role", role.name())
        .issuedAt(now)
        .expiration(expiry)                  // 24h
        .signWith(signingKey)                // HS256 avec secret de 32+ caractères
        .compact();
}
```

Le cookie JWT est configuré avec :
- `HttpOnly = true` → inaccessible depuis JavaScript (protection XSS)
- `Secure = true` → HTTPS uniquement en production
- `SameSite = Lax` → protection CSRF basique
- `Path = /` → envoyé pour toutes les routes
- `MaxAge = 86400` → expire après 24 heures

#### Token Blacklist (logout sécurisé)

À la déconnexion : le token est inséré dans `revoked_tokens` avec sa date d'expiration. À chaque requête suivante, `JwtFilter` vérifie si le token figure dans la blacklist avant de le valider — un token révoqué est rejeté même s'il est cryptographiquement valide.

Un job `@Scheduled` s'exécute chaque nuit à 3h pour purger les tokens expirés de la table.

#### Rate Limiting (anti-bruteforce)

`RateLimitFilter` utilise **Bucket4j** (algorithme du "seau percé"). Chaque adresse IP dispose d'un seau de 10 jetons, rechargé à raison de 10 par minute. Après 10 tentatives rapides sur `/api/v1/auth/*` → HTTP 429 Too Many Requests — protège contre les attaques par force brute sur les mots de passe.

---

### 4.7 Migrations Flyway

Flyway versionne le schéma de base de données comme Git versionne le code. Au démarrage, Spring Boot exécute automatiquement les migrations manquantes dans l'ordre.

```
db/migration/
├── V1__init_schema.sql                    ← Crée toutes les tables initiales
└── V2__create_revoked_tokens_table.sql    ← Ajoute la table pour la blacklist JWT
```

**Avantage :** L'environnement de développement, de test et de production ont toujours le même schéma. Un nouvel environnement se configure avec un simple `docker-compose up`.

---

## 5. Frontend — React + TypeScript

### 5.1 Structure des dossiers

```
frontend/src/
│
├── api/                       ← Couche d'accès aux données (axios)
│   ├── apiClient.ts           ← Configuration axios globale (baseURL, withCredentials, interceptors)
│   ├── authApi.ts             ← /auth/*
│   ├── transactionApi.ts      ← /transactions/*
│   ├── categoryApi.ts         ← /categories/*
│   ├── budgetApi.ts           ← /budgets/*
│   ├── goalApi.ts             ← /goals/* (+ conversion centimes ↔ DH)
│   ├── dashboardApi.ts        ← /dashboard/*
│   └── exportApi.ts           ← /export/*
│
├── components/                ← Composants React réutilisables
│   ├── ProtectedRoute.tsx     ← Gardien des routes privées
│   ├── layout/                ← AppShell, Sidebar, TopBar
│   ├── transactions/          ← TransactionTable, TransactionForm, TransactionFilters, TransactionDetail
│   ├── budgets/               ← BudgetCard, BudgetForm
│   ├── goals/                 ← GoalCard, GoalForm, ContributeModal, EmptyGoalSlot, PremiumUpsellCard
│   ├── categories/            ← CategoryList, CategoryForm
│   ├── charts/                ← SpendingPieChart, RevenueExpensesBar (Recharts)
│   └── ui/                    ← Badge, KpiCard, ProgressBar, ConfirmDialog, ErrorBoundary,
│                                  AlertBanner, EmptyState, PremiumBadge
│
├── context/
│   └── AuthContext.tsx         ← État global d'authentification (React Context API)
│
├── hooks/
│   ├── useAuth.ts             ← Accès raccourci au contexte auth
│   ├── useQuery.ts            ← Hook générique de fetching (GET)
│   ├── useMutation.ts         ← Hook générique de mutation (POST/PUT/DELETE)
│   └── api/                   ← Hooks spécialisés : useCategories, useBudgets, useGoals,
│                                  useTransactions, useDashboard
│
├── pages/                     ← Écrans complets (assemblent les composants)
│   ├── Login.tsx, Register.tsx
│   ├── ForgotPassword.tsx
│   ├── Dashboard.tsx
│   ├── Transactions.tsx, TransactionDetailPage.tsx
│   ├── Budgets.tsx, Goals.tsx
│   └── Categories.tsx, Export.tsx
│
├── types/
│   └── index.ts               ← Tous les types TypeScript partagés (interfaces, enums)
│
└── utils/
    ├── formatCurrency.ts      ← toCentimes / fromCentimes / formatCurrency
    └── formatDate.ts          ← formatDate / currentMonth / parseMonth
```

---

### 5.2 Routing & protection des routes

**`App.tsx`** déclare toutes les routes avec `react-router-dom v7` :

```tsx
<ErrorBoundary>
  <AuthProvider>
    <BrowserRouter>
      <Routes>
        {/* Routes publiques — accessibles sans connexion */}
        <Route path="/login"    element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />

        {/* Routes protégées — enveloppées dans ProtectedRoute */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard"    element={<Dashboard />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/transactions/:id" element={<TransactionDetailPage />} />
          <Route path="/budgets"      element={<Budgets />} />
          <Route path="/goals"        element={<Goals />} />
          <Route path="/categories"   element={<Categories />} />
          <Route path="/export"       element={<Export />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </AuthProvider>
</ErrorBoundary>
```

**`ProtectedRoute.tsx`** — gardien des routes privées :

```tsx
export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  // Pendant la vérification du cookie JWT au démarrage → texte de chargement
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen bg-bg-base text-text-secondary">
        Chargement...
      </div>
    );
  }

  // Authentifié → rend AppShell (sidebar + <Outlet /> pour les pages)
  // Non authentifié → redirige vers /login
  return isAuthenticated ? <AppShell /> : <Navigate to="/login" replace />;
};
```

**`AppShell.tsx`** est le layout commun à toutes les pages connectées : il contient la `Sidebar` et un `<Outlet />` (point d'injection de la page active).

---

### 5.3 Contexte d'authentification

`AuthContext.tsx` centralise l'état d'authentification et le partage dans toute l'application sans prop-drilling.

```tsx
interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isPremium: boolean;             // true si rôle PREMIUM ou ADMIN
  isLoading: boolean;             // true pendant la vérification initiale du cookie
  login: (user: UserProfile) => void;
  logout: () => Promise<void>;
}
```

**Initialisation au démarrage :**

```tsx
// Dans AuthProvider, au montage de l'app :
useEffect(() => {
  getMe()                         // GET /api/v1/auth/me
    .then(user => login(user))    // Cookie valide → utilisateur restauré
    .catch(() => {})              // Cookie absent/expiré → pas connecté
    .finally(() => setIsLoading(false));
}, []);
```

Le cookie JWT persiste entre les sessions. Si l'utilisateur ferme et rouvre l'onglet, il est automatiquement reconnecté si le cookie n'a pas expiré (24h).

**Usage dans n'importe quel composant :**
```tsx
const { user, isPremium, logout } = useAuth();
// Afficher le bouton "Premium" uniquement si isPremium === true
```

---

### 5.4 Custom Hooks

#### `useQuery.ts` — Fetching générique

Évite de dupliquer la gestion de `loading/error/data` dans chaque composant.

```tsx
function useQuery<T>(
  fetcher: () => Promise<T>,    // La fonction qui appelle l'API
  deps: DependencyList          // Tableau de dépendances (comme useEffect)
): { data: T | null, isLoading: boolean, error: string | null, refetch: () => void }
```

**Exemple d'utilisation :**
```tsx
// Dans Categories.tsx
const { data: categories, isLoading, refetch } = useQuery(
  () => listCategories(),
  []  // deps vides = fetch une seule fois au montage
);
```

#### `useMutation.ts` — Mutations génériques

```tsx
function useMutation<TInput, TOutput>(
  mutationFn: (variables: TInput) => Promise<TOutput>,
  options?: {
    onSuccess?: (result: TOutput) => void;
    onError?: (error: string) => void;
  }
): { mutate, isLoading, error, reset }
```

**Exemple d'utilisation :**
```tsx
const { mutate: createTx, isLoading: creating } = useMutation(
  (data: CreateTransactionRequest) => createTransaction(data),
  { onSuccess: () => { refetch(); setShowForm(false); } }
);

// Dans le handler du formulaire :
await createTx({ title: "Courses", amount: toCentimes("95.50"), type: "EXPENSE", ... });
```

#### Hooks API spécialisés

| Hook | Ce qu'il fait |
|---|---|
| `useCategories()` | Charge toutes les catégories de l'utilisateur |
| `useBudgets(month)` | Charge les budgets du mois avec % consommé et statut alerte |
| `useGoals()` | Charge objectifs + progressions en parallèle (`Promise.all`) |
| `useTransactions(filters, page)` | Charge les transactions paginées avec tous les filtres |
| `useDashboard(month)` | KPIs + graphique dépenses + transactions récentes + alertes budgets |

---

### 5.5 Couche API (axios)

**`apiClient.ts`** — point d'entrée unique pour toutes les requêtes HTTP :

```ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // http://localhost:8080/api/v1
  withCredentials: true,                        // ← Envoie le cookie auth_token automatiquement
});

// Intercepteur de réponse : si 401 → rediriger vers /login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**Modules API** — un fichier par contrôleur backend :

```ts
// transactionApi.ts — exemple de deux fonctions
export const listTransactions = (filters: TransactionFilters, page = 0) =>
  apiClient.get<PaginatedResponse<TransactionResponse>>('/transactions', {
    params: { ...filters, page, size: 20 }
  }).then(res => res.data);

export const createTransaction = (data: CreateTransactionRequest) =>
  apiClient.post<TransactionResponse>('/transactions', data).then(res => res.data);
```

---

### 5.6 Composants clés

#### `ErrorBoundary.tsx` — Gestion des erreurs de rendu

```tsx
// Intercepte les erreurs dans l'arbre React → affiche un écran de fallback
export class ErrorBoundary extends Component<Props, State> {
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }
  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info);
    // En production → envoyer à Sentry / Datadog
  }
  render() {
    if (this.state.hasError) return <ErrorFallback onReset={() => this.setState({ hasError: false })} />;
    return this.props.children;
  }
}
```

#### `ProgressBar.tsx` — Composant partagé budgets + objectifs

```tsx
<ProgressBar
  percent={85}
  status="WARNING"    // 'NORMAL' → bleu | 'WARNING' → orange | 'CRITICAL' → rouge
  showLabel={true}
/>
```

#### `TransactionForm.tsx` — Points techniques importants

- **Filtrage dynamique des catégories :** quand le type "Dépense" est sélectionné, seules les catégories de type `DEPENSE` ou `BOTH` s'affichent dans le select.
- **Mode édition :** pré-remplit les champs via `useEffect` + `reset()` de react-hook-form.
- **Validation :** `react-hook-form` avec règles (montant > 0, titre obligatoire, date valide).

#### `GoalCard.tsx` — Logique des jalons visuels

```tsx
const milestones = [
  { value: 25, reached: progress.milestones.twentyFive,    label: "25%" },
  { value: 50, reached: progress.milestones.fifty,         label: "50%" },
  { value: 75, reached: progress.milestones.seventyFive,   label: "75%" },
  { value: 100, reached: progress.milestones.hundred,      label: "🎯" },
];
// Chaque jalon est un marqueur sur la barre de progression (vert si atteint, gris sinon)
```

---

### 5.7 Convention centimes

**Règle universelle du projet :** tous les montants sont stockés et transmis en **centimes** (type `Long` en Java, `number` en TypeScript). La conversion vers/depuis DH ne se fait que pour l'affichage et les formulaires.

```
BDD stocke : 50000   →   affiché : "500,00 DH"
BDD stocke : 1500000 →   affiché : "15 000,00 DH"
```

**Les 3 fonctions utilitaires (`formatCurrency.ts`) :**

```ts
// Affichage : centimes → chaîne localisée
formatCurrency(50000)         // → "500,00 DH"
formatCurrency(50000, true)   // → "+500,00 DH" (avec signe pour les revenus)

// Formulaire → API : valeur saisie → centimes
toCentimes("320.50")    // → 32050
toCentimes(500)         // → 50000

// API → Formulaire : centimes → décimal pour l'input
fromCentimes(32050)     // → "320.50"
fromCentimes(50000)     // → "500.00"
```

**Où la conversion a lieu :**
- `goalApi.ts` : `toCentimes()` avant envoi, `fromCentimes()` à la réception
- `TransactionForm.tsx` + `Transactions.tsx` : `toCentimes()` dans le handler submit
- `BudgetForm.tsx` + `Budgets.tsx` : `toCentimes()` dans le handler submit

---

## 6. Flux de données end-to-end

### Flux 1 : Créer une transaction

```
[Utilisateur remplit le formulaire : "Courses - 95,50 DH"]
        ↓
[handleCreateSubmit() dans Transactions.tsx]
  amount = toCentimes("95.50") → 9550
  createTx({ title: "Courses", amount: 9550, type: "EXPENSE", categoryId: 3, txDate: "2025-05-10" })
        ↓
[transactionApi.ts]
  apiClient.post('/transactions', payload)
        ↓
[axios envoie HTTP POST avec cookie auth_token automatiquement]
        ↓
[RateLimitFilter] → OK (pas sur /auth/)
        ↓
[JwtFilter]
  Lit cookie auth_token
  Vérifie blacklist → pas révoqué
  Valide JWT → extrait userId=1, role=STANDARD
  request.setAttribute("userId", 1L)
        ↓
[TransactionController.java]
  Long userId = (Long) request.getAttribute("userId") → 1
  transactionService.create(1L, dto)
        ↓
[TransactionService.java]
  Vérifie que categoryId=3 appartient à userId=1 → OK
  Transaction tx = Transaction.builder()
      .userId(1L).title("Courses").amount(9550).type(EXPENSE)
      .category(category).txDate(LocalDate.of(2025,5,10))
      .isDeleted(false).build();
  transactionRepository.save(tx)
        ↓
[PostgreSQL]
  INSERT INTO transactions (user_id, title, amount, type, category_id, tx_date, is_deleted)
  VALUES (1, 'Courses', 9550, 'EXPENSE', 3, '2025-05-10', false)
        ↓
[HTTP 201 Created] ← { id: 42, title: "Courses", amount: 9550, ... }
        ↓
[transactionApi.ts retourne la réponse]
        ↓
[useMutation.onSuccess() → refetch() → liste mise à jour]
```

### Flux 2 : Authentification initiale (rechargement de page)

```
[Utilisateur ouvre l'app ou recharge la page]
        ↓
[AuthProvider.useEffect()]
  getMe() → GET /api/v1/auth/me
        ↓ (Le navigateur envoie automatiquement le cookie HttpOnly)
  Cas A — Cookie valide :
    JwtFilter valide le JWT → userId extrait
    AuthController.me() → UserService.findById(userId)
    HTTP 200 → { id: 1, email: "...", role: "STANDARD" }
    → AuthContext : user = {...}, isAuthenticated = true
    → ProtectedRoute laisse passer → Dashboard s'affiche
        ↓
  Cas B — Cookie absent ou expiré :
    JwtFilter ne trouve pas de JWT valide
    Spring Security : /api/v1/auth/me nécessite .authenticated() → HTTP 401
    axios interceptor capte le 401 → window.location.href = '/login'
    → Utilisateur redirigé vers la page de connexion
```

### Flux 3 : Budget avec alerte

```
[Dashboard charge les budgets du mois]
  useDashboard("2025-05") → dashboardApi.getDashboard("2025-05")
        ↓
[DashboardService.java]
  Pour chaque budget :
    spent = transactionRepository.sumByUserIdAndCategoryAndMonth(userId, cat, 2025, 5)
    percent = (spent * 100) / budget.getLimitAmount()
    status = percent >= 95 ? "CRITICAL" : percent >= 80 ? "WARNING" : "NORMAL"
        ↓
[BudgetCard.tsx]
  <ProgressBar percent={87} status="WARNING" />
  → Barre orange + icône ⚠️ + texte "87% utilisé"
```

---

## 7. Sécurité en détail

### Pourquoi cookie HttpOnly et pas localStorage ?

| Critère | localStorage | Cookie HttpOnly |
|---|---|---|
| Accessible depuis JavaScript | ✅ Oui | ❌ Non |
| Envoyé automatiquement | ❌ Non (manuel dans headers) | ✅ Oui |
| Vulnérable aux attaques XSS | ⚠️ Oui — un script malveillant peut le lire | ✅ Non — inaccessible depuis JS |
| Vulnérable aux attaques CSRF | ✅ Non | ⚠️ Oui — atténué par SameSite=Lax |

Le cookie HttpOnly est le choix le plus sécurisé pour les SPAs modernes.

### Pourquoi BCrypt pour les mots de passe ?

BCrypt est une fonction de hachage à sens unique avec **facteur de coût** (work factor). Même si la base de données est compromise, les mots de passe ne peuvent pas être retrouvés par force brute dans un délai raisonnable. Spring Security intègre `BCryptPasswordEncoder` nativement.

### Protection IDOR

Chaque accès à une ressource vérifie l'appartenance :

```java
// Mauvais (vulnérable) :
transactionRepository.findById(id).orElseThrow(...)

// Correct (résistant à l'IDOR) :
transactionRepository.findByIdAndUserId(id, userId).orElseThrow(...)
```

---

## 8. Spring vs Sans Spring — Comparaisons complètes

Cette section est requise par l'enseignant. Elle montre concrètement pourquoi Spring est utilisé.

### 8.1 Authentification & sécurité

**Sans Spring — Filtre Servlet manuel :**

```java
// AuthFilter.java — Implémentation manuelle
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Lire le cookie manuellement
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("auth_token".equals(c.getName())) token = c.getValue();
            }
        }

        // Valider le token manuellement (bibliothèque JJWT à configurer soi-même)
        if (token == null || !isValidToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Stocker l'utilisateur en session HTTP
        HttpSession session = request.getSession();
        session.setAttribute("userId", extractUserId(token));

        chain.doFilter(req, res); // Passer au servlet suivant
    }
}
// → À déclarer dans web.xml ou via @WebFilter
// → Encodage de mot de passe : implémenter BCrypt manuellement
// → Gestion des rôles : if/else dans chaque servlet
```

**Avec Spring Security :**

```java
// SecurityConfig.java — Déclaratif, lisible, sans boilerplate
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/**").authenticated()
)
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

// BCrypt disponible partout via injection de dépendance
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Comparaison :**

| | Sans Spring | Avec Spring Security |
|---|---|---|
| Configuration | `web.xml` + annotations `@WebFilter` | Un seul bean Java `SecurityFilterChain` |
| Encodage password | Implémenter BCrypt soi-même | `@Bean BCryptPasswordEncoder` |
| Gestion des rôles | `if/else` dans chaque servlet | `.hasRole("ADMIN")` déclaratif |
| Protection routes | Filtre manuel sur chaque URL | `authorizeHttpRequests()` centralisé |
| Chaîne de filtres | Configurer l'ordre dans `web.xml` | `addFilterBefore()` type-safe |

---

### 8.2 Accès base de données (JDBC vs Spring Data JPA)

**Sans Spring — DAO JDBC manuel :**

```java
// TransactionDao.java — Approche traditionnelle (50+ lignes pour une requête simple)
public class TransactionDao {
    private DataSource dataSource;

    public List<Transaction> findByUserIdNotDeleted(Long userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();                         // Ouvrir connexion
            ps = conn.prepareStatement(
                "SELECT t.*, c.name AS cat_name FROM transactions t " +
                "LEFT JOIN categories c ON t.category_id = c.id " +
                "WHERE t.user_id = ? AND t.is_deleted = false " +
                "ORDER BY t.tx_date DESC"
            );
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            List<Transaction> list = new ArrayList<>();
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setTitle(rs.getString("title"));
                t.setAmount(rs.getLong("amount"));
                // ... mapper chaque colonne manuellement
                Category cat = new Category();
                cat.setName(rs.getString("cat_name"));
                t.setCategory(cat);
                list.add(t);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            // Fermer dans l'ordre inverse pour éviter les fuites mémoire
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
}
```

**Avec Spring Data JPA :**

```java
// TransactionRepository.java — 1 déclaration, Spring génère tout
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
                                                JpaSpecificationExecutor<Transaction> {

    // Spring génère : SELECT * FROM transactions WHERE user_id=? AND is_deleted=false ORDER BY tx_date DESC
    List<Transaction> findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(Long userId);

    // Avec pagination automatique
    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);
}
```

**Comparaison :**

| | JDBC Manuel | Spring Data JPA |
|---|---|---|
| Lignes de code pour `findByUserId` | ~50 | 1 |
| Gestion des connexions | Manuelle (try/finally) | Pool automatique (HikariCP) |
| Mapping SQL → objet | Manuel colonne par colonne | Hibernate (automatique) |
| Transactions | `conn.setAutoCommit(false)` + `commit()` | `@Transactional` |
| Filtres dynamiques | Concaténation SQL (risque SQL injection) | `Specification` (type-safe) |
| Pagination | `LIMIT/OFFSET` manuels | `Pageable` objet |

---

### 8.3 Relations entre entités

**Sans Spring (JDBC) — Jointure manuelle :**

```java
// Requête SQL manuelle + mapping objet complexe
String sql = "SELECT t.*, c.id AS c_id, c.name AS c_name, c.color AS c_color " +
             "FROM transactions t JOIN categories c ON t.category_id = c.id " +
             "WHERE t.id = ?";
// ... puis mapper t.category = new Category(rs.getLong("c_id"), rs.getString("c_name"), ...)
```

**Avec Spring Data JPA :**

```java
@Entity
public class Transaction {
    @ManyToOne(fetch = FetchType.LAZY)     // Relation déclarative
    @JoinColumn(name = "category_id")
    private Category category;
    // Hibernate charge automatiquement la catégorie liée quand on accède à tx.getCategory()
}
```

**Comparaison :**

| Relation | JDBC Manuel | JPA |
|---|---|---|
| `Transaction → Category` | Requête SQL JOIN + mapping | `@ManyToOne` |
| `Goal → GoalContributions` | Requête séparée + boucle | `@OneToMany` |
| Chargement paresseux | Impossible nativement | `fetch = FetchType.LAZY` |
| Cascade suppression | `DELETE` SQL manuels | `cascade = CascadeType.ALL` |

---

### 8.4 Injection de dépendances

**Sans Spring — instanciation manuelle :**

```java
// Instancier et câbler les dépendances à la main
DataSource ds = new HikariDataSource(config);
TransactionDao dao = new TransactionDao(ds);
CategoryDao catDao = new CategoryDao(ds);
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
JwtUtil jwtUtil = new JwtUtil(secret, expiration);
AuthService authService = new AuthService(dao, catDao, encoder, jwtUtil);
AuthServlet servlet = new AuthServlet(authService);
// → À refaire pour chaque servlet, chaque test, chaque environnement
```

**Avec Spring — injection automatique :**

```java
@Service
@RequiredArgsConstructor          // ← Lombok génère le constructeur avec toutes les dépendances
public class AuthService {
    private final UserRepository userRepository;      // ← Spring injecte automatiquement
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    // Spring résout le graphe de dépendances entier au démarrage
}
```

---

### 8.5 Architecture globale (Servlet vs Spring MVC)

| Aspect | Sans Spring (Servlet/JSP) | Avec Spring MVC + React |
|---|---|---|
| Routing HTTP | Mapping manuel dans `web.xml` ou `@WebServlet` | `@RequestMapping` sur les controllers |
| Parsing JSON | `ObjectMapper` manuel dans chaque servlet | `@RequestBody` + Jackson automatique |
| Validation des données | `if/else` manuels | `@Valid` + Bean Validation (`@NotNull`, `@Min`...) |
| Gestion des erreurs | `try/catch` dans chaque servlet | `@ControllerAdvice` centralisé |
| Interface utilisateur | JSP (HTML côté serveur) | React SPA (HTML côté client) |
| Pagination | `LIMIT/OFFSET` SQL manuels | `Pageable` + `Page<T>` |

---

## 9. Tests

### Backend (JUnit 5 + Mockito)

Architecture de test par couche :

```
Tests unitaires (service/) :
  AuthServiceTest.java              → register, login, logout, validation unicité
  TransactionServiceTest.java       → CRUD, soft delete, IDOR, pagination
  BudgetServiceTest.java            → calcul %, alertes WARNING/CRITICAL
  GoalServiceTest.java              → jalons, statuts, contributions
  CategoryServiceTest.java          → limite 10 cat STANDARD, protection système
  DashboardServiceTest.java         → KPIs, agrégations revenus/dépenses
  ExportServiceTest.java            → Génération CSV

Tests d'intégration (controller/) :
  AuthControllerTest.java           → HTTP (MockMvc), cookies, codes de statut
  TransactionControllerTest.java    → Pagination, validation @Valid, filtres
  CategoryControllerTest.java       → CRUD catégories, limites par rôle
  BudgetControllerTest.java         → Création/progression budgets
  GoalControllerTest.java           → CRUD objectifs, contributions

Tests unitaires (security/) :
  JwtUtilTest.java                  → Génération, validation, expiration
  JwtFilterTest.java                → Cookie présent/absent/révoqué
  RateLimitFilterTest.java          → Limite 10 req/min/IP
  TokenBlacklistTest.java           → Révocation, nettoyage @Scheduled

Tests additionnels :
  TransactionSpecificationTest.java → Filtres dynamiques Specification
  CorsConfigTest.java               → Configuration CORS
  GlobalExceptionHandlerTest.java   → Gestion centralisée des erreurs
```

**Pattern AAA (Arrange-Act-Assert) :**

```java
@Test
void delete_shouldThrow_whenTransactionDoesNotBelongToUser() {
    // Arrange — préparer les données de test
    Long userId = 1L;
    Long txId = 99L;
    when(transactionRepository.findByIdAndUserId(txId, userId))
        .thenReturn(Optional.empty());                      // La transaction n'existe pas pour cet user

    // Act + Assert — exécuter et vérifier
    assertThrows(ResourceNotFoundException.class,
        () -> transactionService.delete(userId, txId));

    verify(transactionRepository, never()).save(any());    // La suppression n'a pas eu lieu
}
```

### Frontend (Vitest + Testing Library)

```
utils/
  formatCurrency.test.ts        → toCentimes, fromCentimes, formatCurrency (edge cases)
  formatDate.test.ts            → formatDate, currentMonth, formats localisés

hooks/
  useQuery.test.ts              → états loading/data/error/refetch
  useMutation.test.ts           → mutations, callbacks onSuccess/onError

context/
  AuthContext.test.tsx          → init, login, logout, isPremium selon rôle

api/
  authApi.test.ts               → endpoints, gestion des erreurs HTTP
  transactionApi.test.ts        → CRUD transactions, pagination
  goalApi.test.ts               → CRUD objectifs, conversion centimes

components/
  GoalCard.test.tsx             → rendu jalons, interactions boutons
  BudgetCard.test.tsx           → alertes WARNING/CRITICAL, affichage %
  ErrorBoundary.test.tsx        → récupération après crash composant
  ProtectedRoute.test.tsx       → redirection si non authentifié
  TransactionForm.test.tsx      → validation, filtrage catégories, soumission
```

---

## 10. Lancer le projet

### Prérequis

- Java 17 ou supérieur
- Node.js 22 ou supérieur
- Docker & Docker Compose

### Démarrer le backend

```bash
# 1. Copier et configurer les variables d'environnement
cp .env.example .env
# Éditer .env et remplir les valeurs (voir ci-dessous)

# 2. Démarrer PostgreSQL dans Docker
docker-compose up -d postgres

# 3. Lancer le backend Spring Boot
cd backend
./mvnw spring-boot:run
# Flyway exécute V1 et V2 automatiquement au premier démarrage
# API disponible sur http://localhost:8080
```

### Démarrer le frontend

```bash
cd frontend
npm install
npm run dev
# App disponible sur http://localhost:5173
```

### Variables d'environnement

```env
# Backend (.env)
JWT_SECRET=your-super-secret-key-min-32-chars-long
JWT_EXPIRATION_MS=86400000                         # 24 heures
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portfolio_db
SPRING_DATASOURCE_USERNAME=portfolio_user
SPRING_DATASOURCE_PASSWORD=your_db_password

# Frontend (.env)
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Vérifier que tout fonctionne

```bash
# Tester l'API directement
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","username":"testuser","password":"Password123!"}'
# → HTTP 201 + cookie auth_token défini

# Lancer les tests backend
cd backend && ./mvnw test

# Lancer les tests frontend
cd frontend && npm test
```

---

## 11. Points clés pour la présentation

Ces 10 points résument l'essentiel du projet pour la présentation :

**1. Architecture en couches stricte**
Controller → Service → Repository → Entity. Chaque couche a une responsabilité unique. Un Controller ne contient jamais de SQL. Un Repository ne contient jamais de logique métier.

**2. JWT en cookie HttpOnly**
Plus sécurisé que localStorage (résistant au XSS). Le token est invisible depuis JavaScript, envoyé automatiquement par le navigateur à chaque requête.

**3. Spring Data JPA = 0 SQL manuel**
Les repositories sont des interfaces. Spring génère tout le SQL à partir des noms de méthodes (`findByUserIdAndIsDeletedFalse...`) ou de `@Query`.

**4. Soft delete**
Les transactions ne sont jamais effacées physiquement. `isDeleted = true` les masque tout en préservant l'historique et l'intégrité des données.

**5. Centimes en BDD**
Tous les montants sont des entiers `Long` (pas de `double` ou `float`). Élimine les erreurs d'arrondi des flottants. La conversion vers DH n'est faite que pour l'affichage.

**6. Rôles et restrictions**
STANDARD : ≤ 10 catégories custom, ≤ 1 objectif actif. PREMIUM/ADMIN : illimité. Vérifié côté service, pas côté frontend (le frontend peut être contourné).

**7. Custom Hooks React**
`useQuery` et `useMutation` centralisent la logique de fetching. Les pages sont des "orchestrateurs légers" qui décrivent *quoi* afficher, pas *comment* fetcher.

**8. Validation bidirectionnelle**
`@Valid` + Bean Validation côté backend (source de vérité). `react-hook-form` côté frontend (UX). Si le frontend est contourné, le backend rejette quand même les données invalides.

**9. Protection IDOR**
Chaque accès à une ressource inclut `userId` dans la requête BDD. Un utilisateur ne peut jamais accéder aux données d'un autre, même en devinant un ID.

**10. Flyway — schéma versionné**
Le schéma BDD est versionné comme le code. Tout nouvel environnement (PC de l'enseignant, serveur de prod) se configure automatiquement au premier démarrage.

---

## 12. Glossaire

| Terme | Définition dans le contexte du projet |
|---|---|
| **JWT** | JSON Web Token — token signé contenant userId, email et rôle. Valable 24h. |
| **Cookie HttpOnly** | Cookie inaccessible depuis JavaScript. Envoyé automatiquement par le navigateur. |
| **SPA** | Single Page Application — le HTML est rendu côté client par React. |
| **REST** | Style d'API HTTP : chaque ressource a une URL, les opérations sont GET/POST/PUT/DELETE. |
| **BCrypt** | Algorithme de hachage de mots de passe résistant aux attaques par force brute. |
| **Soft delete** | "Supprimer" sans supprimer : `is_deleted = true` masque sans effacer. |
| **IDOR** | Insecure Direct Object Reference — vulnérabilité où on accède aux données d'autrui en devinant un ID. |
| **Flyway** | Outil de migration de schéma BDD versionné. Fonctionne comme Git pour le SQL. |
| **Specification** | Pattern JPA pour les requêtes dynamiques (filtres multiples combinables). |
| **Centimes** | Convention du projet : les montants sont stockés en entiers (1 DH = 100 centimes). |
| **`@Transactional`** | Si une opération dans le service échoue, tout est annulé (rollback atomique). |
| **`@Valid`** | Déclenche la validation des contraintes Bean Validation sur le corps de la requête. |
| **Bucket4j** | Bibliothèque Java pour le rate limiting (algorithme du seau percé). |
| **HikariCP** | Pool de connexions BDD intégré à Spring Boot. Gère les connexions automatiquement. |
| **`withCredentials`** | Option axios qui force l'envoi des cookies avec chaque requête cross-origin. |
| **Pageable** | Objet Spring qui encapsule page, taille et tri pour la pagination automatique. |

---

*Documentation générée le 13 mai 2026 — Folio.io v1.0*
