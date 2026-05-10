# Code Review: Learning Blocks for Folio.io

> **How to use this document:** Each block is self-contained. Read the *Why it matters* section, study the *What your code does* excerpt, then implement the *Fix* yourself before peeking at the solution. The goal is deliberate practice, not copy-pasting.

---

## Table of Contents

1. [🔐 Block 1 — JWT Stored in `localStorage` (Critical Security Flaw)](#block-1)
2. X [🛡️ Block 2 — Missing Ownership Check on Goal Deletion](#block-2)
3. X [🐛 Block 3 — Frontend/Backend Contract Mismatch (Silent Runtime Bug)](#block-3)
4. [🧩 Block 4 — Data Fetching Logic Lives Inside Page Components](#block-4)
5. X [💥 Block 5 — No React Error Boundary (Entire App Can Crash)](#block-5)
6. [🔑 Block 6 — In-Memory Token Blacklist Is Lost on Restart](#block-6)
7. X [🔒 Block 7 — Secrets in `application.properties`](#block-7)
8. [✅ Block 8 — Your Test Suite Is Empty (One Test That Does Nothing)](#block-8)
9. [⚡ Block 9 — Stale Closures and Missing `useEffect` Dependencies](#block-9)
10. [🏷️ Block 10 — `any` Is a Lie You're Telling TypeScript](#block-10)
11. [🚦 Block 11 — No Rate Limiting on Auth Endpoints](#block-11)
12. [🗂️ Block 12 — Category Ownership Is Only Half-Validated](#block-12)

---

<a name="block-1"></a>
## 🔐 Block 1 — JWT Stored in `localStorage` (Critical Security Flaw)

### Why it matters
`localStorage` is accessible by **any JavaScript running on your page**. If a third-party script, a browser extension, or a single XSS vulnerability exists, your user's JWT is stolen instantly. This is the single most common frontend security mistake.

### What your code does

```typescript
// frontend/src/utils/tokenStorage.ts
const TOKEN_KEY = 'pi_jwt_token';
export const getToken  = () => localStorage.getItem(TOKEN_KEY);
export const setToken  = (token: string) => localStorage.setItem(TOKEN_KEY, token);
export const removeToken = () => localStorage.removeItem(TOKEN_KEY);
```

### The Professional Alternative
Store the JWT in an **`httpOnly` cookie** set by the server. JavaScript cannot read `httpOnly` cookies at all — not your code, not an attacker's code.

### 🏋️ Exercise

**Step 1 — Backend:** Modify `AuthController` so that `login` and `register` responses set an `httpOnly`, `Secure`, `SameSite=Strict` cookie instead of returning the token in the JSON body.

```java
// Hint: In AuthController.java
@PostMapping("/login")
public ResponseEntity<UserResponse> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletResponse response // <-- add this
) {
    AuthResponse auth = authService.login(request);

    Cookie cookie = new Cookie("auth_token", auth.getToken());
    cookie.setHttpOnly(true);   // JS cannot read this
    cookie.setSecure(true);     // HTTPS only
    cookie.setPath("/");
    cookie.setMaxAge(86400);    // 1 day, matches your JWT expiry
    response.addCookie(cookie);

    // Return only user info, NOT the token
    return ResponseEntity.ok(auth.getUser());
}
```

**Step 2 — Backend:** Modify `JwtFilter` to read the token from the cookie instead of the `Authorization` header.

```java
// Hint: in JwtFilter.doFilterInternal
// Replace:
String authHeader = request.getHeader("Authorization");
// With:
String token = Arrays.stream(Optional.ofNullable(request.getCookies())
    .orElse(new Cookie[0]))
    .filter(c -> "auth_token".equals(c.getName()))
    .map(Cookie::getValue)
    .findFirst()
    .orElse(null);
```

**Step 3 — Frontend:** Delete `tokenStorage.ts` entirely. Your `apiClient.ts` no longer needs to attach a header — the browser sends the cookie automatically. Add `withCredentials: true` to your axios instance.

```typescript
// frontend/src/api/apiClient.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // Sends cookies automatically
});
// Delete the request interceptor that attaches the Bearer token
```

**Step 4 — Backend CORS:** When using cookies cross-origin, `allowedOrigins("*")` doesn't work. You must specify the exact origin.

```java
// CorsConfig.java — this is already correct in your code ✅
.allowedOrigins("http://localhost:5173")
.allowCredentials(true)
```

### Takeaway
> **Rule:** Never store auth tokens in `localStorage` or `sessionStorage`. Use `httpOnly` cookies for session tokens in web apps.

---

<a name="block-2"></a>
## 🛡️ Block 2 — Missing Ownership Check on Goal Deletion

### Why it matters
Any authenticated user can delete **any other user's goals** by guessing an ID. This is called an **Insecure Direct Object Reference (IDOR)** — OWASP Top 10 vulnerability.

### What your code does

```java
// GoalController.java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
    goalService.deleteGoal(id);  // ← No userId check!
    return ResponseEntity.noContent().build();
}

// GoalService.java
public void deleteGoal(Long id) {
    goalRepository.deleteById(id);  // ← Deletes whatever ID is given
}
```

### 🏋️ Exercise

Fix both the controller and the service. The `userId` is already available on the request object from `JwtFilter`.

```java
// Step 1: Fix GoalController — pass userId to service
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteGoal(
    HttpServletRequest httpRequest, // <-- add this
    @PathVariable Long id
) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    goalService.deleteGoal(userId, id); // <-- pass userId
    return ResponseEntity.noContent().build();
}

// Step 2: Fix GoalService — verify ownership
public void deleteGoal(Long userId, Long goalId) {
    // Use findByIdAndUserId — already exists on your repo!
    Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    goalRepository.delete(goal);
}
```

### Audit All Your Endpoints
Now go through every controller and verify this pattern is consistent. You'll find the same issue isn't present in `TransactionController`, `BudgetController`, or `CategoryController` because those correctly pass `userId`. **Good job on those — but this one was missed.**

### Takeaway
> **Rule:** Every data-mutating endpoint must verify the authenticated user owns the resource. Never trust the client-supplied ID alone.

---

<a name="block-3"></a>
## 🐛 Block 3 — Frontend/Backend Contract Mismatch (Silent Runtime Bug)

### Why it matters
Your backend returns one shape, your frontend expects another. There's no error — the data is just silently `undefined`. These bugs are invisible until a user reports broken UI.

### What your code does

**Backend returns** (`GoalService.java`):
```java
Map<Integer, Boolean> milestones = new HashMap<>();
milestones.put(25, progressPercentage >= 25);
milestones.put(50, progressPercentage >= 50);
milestones.put(75, progressPercentage >= 75);
milestones.put(100, progressPercentage >= 100);
// JSON: { "25": true, "50": false, "75": false, "100": false }
```

**Frontend type expects** (`types/index.ts`):
```typescript
milestones: {
    twentyFive: boolean;  // ❌ This key doesn't exist in the response
    fifty: boolean;       // ❌
    seventyFive: boolean; // ❌
    hundred: boolean;     // ❌
}
```

**Frontend reads** (`GoalCard.tsx`):
```typescript
const milestones = [
    { value: 25, reached: progress.milestones.twentyFive }, // undefined → false
    { value: 50, reached: progress.milestones.fifty },      // always broken
    ...
];
```

All milestone dots will **always show as un-reached**, regardless of actual progress.

### 🏋️ Exercise

You have two options. Pick one and implement it:

**Option A — Fix the Frontend type** (easier, recommended):

```typescript
// types/index.ts
export interface GoalProgress {
  // ...
  milestones: {
    '25': boolean;   // matches JSON key "25"
    '50': boolean;
    '75': boolean;
    '100': boolean;
  };
}

// GoalCard.tsx
const milestones = [
    { value: 25,  reached: progress.milestones['25'] },
    { value: 50,  reached: progress.milestones['50'] },
    { value: 75,  reached: progress.milestones['75'] },
    { value: 100, reached: progress.milestones['100'] },
];
```

**Option B — Fix the Backend** to send named keys:

```java
// GoalProgressResponse.java — replace Map with a dedicated DTO
@Data
@Builder
public class MilestonesDto {
    private boolean twentyFive;
    private boolean fifty;
    private boolean seventyFive;
    private boolean hundred;
}

// Then in GoalProgressResponse.java:
private MilestonesDto milestones;
```

### Prevention: Generate Types from OpenAPI
The professional solution is to use **Springdoc OpenAPI** to auto-generate a spec, then use **openapi-typescript** to generate your TypeScript types. This makes mismatches a compile error.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

```bash
# Frontend: generate types from your running backend
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.generated.ts
```

### Takeaway
> **Rule:** Define your API contract first (OpenAPI spec), then generate types on both sides. Never manually maintain two copies of the same type definition.

---

<a name="block-4"></a>
## 🧩 Block 4 — Data Fetching Logic Lives Inside Page Components

### Why it matters
Every page component (Dashboard, Transactions, Goals, Budgets) contains fetch functions, loading state, error state, and retry logic. This is duplicated across files, impossible to unit test, and inflates each component to 150+ lines.

### What your code does

```tsx
// Dashboard.tsx (and every other page — same pattern repeated)
export const Dashboard: React.FC = () => {
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const kpiData = await dashboardApi.getKpis(month);
        setKpis(kpiData);
      } catch (err: any) {
        setError(err.response?.data?.error || '...');
      } finally {
        setIsLoading(false);
      }
    };
    fetchDashboardData();
  }, [month]);
  // ...
```

### 🏋️ Exercise

Create a **generic `useQuery` hook** that encapsulates this pattern:

```typescript
// frontend/src/hooks/useQuery.ts
import { useState, useEffect, useCallback } from 'react';

interface QueryState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useQuery<T>(
  fetcher: () => Promise<T>,
  deps: React.DependencyList = []
): QueryState<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await fetcher();
      setData(result);
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Une erreur est survenue');
    } finally {
      setIsLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => { execute(); }, [execute]);

  return { data, isLoading, error, refetch: execute };
}
```

Now your Dashboard becomes:

```tsx
// Dashboard.tsx — after refactor
export const Dashboard: React.FC = () => {
  const [month, setMonth] = useState(currentMonth());

  const { data: kpis, isLoading, error } = useQuery(
    () => dashboardApi.getKpis(month),
    [month]  // refetches when month changes
  );

  const { data: spending } = useQuery(
    () => dashboardApi.getSpending(),
    []
  );
  // Component is now focused on rendering, not fetching
};
```

### Bonus: Domain-Specific Hooks
Go one level further and create domain hooks:

```typescript
// hooks/useBudgets.ts
export const useBudgets = (month: string) =>
  useQuery(() => listBudgetsByMonth(month), [month]);

// hooks/useCategories.ts
export const useCategories = () =>
  useQuery(() => listCategories(), []);
```

### Takeaway
> **Rule:** Custom hooks are the React equivalent of a service layer. Extract anything that isn't rendering logic into a hook. It makes components readable and logic testable.

---

<a name="block-5"></a>
## 💥 Block 5 — No React Error Boundary (Entire App Can Crash)

### Why it matters
If any component throws an unhandled error during render (a null reference, a bad API response shape, etc.), React unmounts the **entire component tree** and shows a blank white screen. Users see nothing and have no way to recover.

### What your code does
There is no `ErrorBoundary` anywhere in `App.tsx` or `main.tsx`.

### 🏋️ Exercise

Create an Error Boundary component (must be a class component — this is one place where they're still necessary):

```tsx
// frontend/src/components/ui/ErrorBoundary.tsx
import React, { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // In production: send to Sentry, Datadog, etc.
    console.error('ErrorBoundary caught:', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div className="flex flex-col items-center justify-center h-screen bg-bg-base text-center p-8">
          <h2 className="text-2xl font-bold text-text-primary mb-2">
            Une erreur inattendue s'est produite
          </h2>
          <p className="text-text-secondary mb-6">
            {this.state.error?.message}
          </p>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            className="px-4 py-2 bg-primary text-bg-base rounded-lg font-medium"
          >
            Réessayer
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
```

Wrap your router in `App.tsx`:

```tsx
// App.tsx
import { ErrorBoundary } from './components/ui/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          {/* ... */}
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}
```

### Takeaway
> **Rule:** Every React app needs at least one Error Boundary at the root. Production apps add them at the route level too, so a crash in one page doesn't affect the whole app.

---

<a name="block-6"></a>
## 🔑 Block 6 — In-Memory Token Blacklist Is Lost on Restart

### Why it matters
When a user logs out, you blacklist their token. But `TokenBlacklist` is a plain Java `HashMap` in memory. When the server restarts (deployment, crash), the blacklist is wiped. Old tokens become valid again. Users who logged out are no longer logged out.

### What your code does

```java
// TokenBlacklist.java
@Component
public class TokenBlacklist {
    // Lives entirely in RAM — evaporates on every restart
    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();
    // No cleanup — grows forever as long as server is up
}
```

### The Professional Solutions

**Option A — Redis (production standard):**

```java
// Add spring-boot-starter-data-redis to pom.xml, then:
@Component
@RequiredArgsConstructor
public class TokenBlacklist {
    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, Duration ttl) {
        // Key expires automatically when JWT expires — no cleanup needed
        redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("blacklist:" + token)
        );
    }
}
```

**Option B — Short-lived tokens + Refresh tokens (modern approach):**
Instead of blacklisting, issue access tokens that expire in 15 minutes and refresh tokens that expire in 7 days. "Logout" just deletes the refresh token from the database. The short access token lifetime limits damage.

**Option C — Database (simple, no extra infrastructure):**

```java
// Create a revoked_tokens table, query it in JwtFilter
// Not as fast as Redis but correct and survives restarts
```

### 🏋️ Exercise
For now, add at minimum a **cleanup scheduled task** to prevent unbounded memory growth:

```java
// TokenBlacklist.java
@Scheduled(fixedDelay = 3_600_000) // Every hour
public void evictExpiredTokens() {
    Instant cutoff = Instant.now().minusSeconds(86400); // older than 1 day
    blacklistedTokens.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
}
```

Add `@EnableScheduling` to `PortfolioApplication.java`.

### Takeaway
> **Rule:** Never store auth state in application memory. It doesn't survive restarts, doesn't scale to multiple instances, and leaks memory. Use Redis or a database.

---

<a name="block-7"></a>
## 🔒 Block 7 — Secrets in `application.properties`

### Why it matters
`application.properties` is committed to Git. Your JWT secret and database password are now in your repository history forever. Anyone with access to your repo can impersonate any user.

### What your code does

```properties
# application.properties — committed to Git ❌
jwt.secret=${JWT_SECRET:your-super-secret-key-min-256-bits-long-asdfghjklqwertyuiopzxcvbnm}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:secret}
```

The fallback values (after `:`) are the problem — they make the app "work" without real secrets, so devs skip setting environment variables.

### 🏋️ Exercise

**Step 1 — Remove all fallback values:**

```properties
# application.properties — force explicit env vars
jwt.secret=${JWT_SECRET}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

The app will fail to start without these — which is the correct behavior. Fail loudly, not silently with a weak default.

**Step 2 — Create a `.env.local` file for local development (not committed):**

```bash
# .env.local — add to .gitignore
JWT_SECRET=local-dev-only-not-production-value-minimum-256-bits
SPRING_DATASOURCE_PASSWORD=secret
```

**Step 3 — Add validation at startup:**

```java
// In JwtUtil.java constructor
public JwtUtil(@Value("${jwt.secret}") String secret, ...) {
    if (secret.length() < 32) {
        throw new IllegalStateException(
            "JWT_SECRET must be at least 32 characters. Got: " + secret.length()
        );
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
}
```

**Step 4 — Scan your Git history:**
```bash
git log --all --full-history -- application.properties
# If secrets were ever committed, you need to rotate them immediately
# and use git-filter-repo to scrub history
```

### Takeaway
> **Rule:** Secrets go in environment variables, a vault (HashiCorp Vault, AWS Secrets Manager), or a `.env` file that is gitignored. They never go in code or config files committed to version control.

---

<a name="block-8"></a>
## ✅ Block 8 — Your Test Suite Is Empty

### Why it matters
You have one test file. It contains one test. The test is auto-generated. It verifies that the Spring context loads. This catches nothing. If you push a breaking change, CI won't catch it.

### What your code does

```java
// PortfolioApplicationTests.java
@SpringBootTest
class PortfolioApplicationTests {
    @Test
    void contextLoads() {
        // Does nothing. Tests nothing. False sense of security.
    }
}
```

### 🏋️ Exercise: Write 3 Meaningful Tests

**Test 1 — Service unit test (fast, no DB):**

```java
// TransactionServiceTest.java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private TransactionService transactionService;

    @Test
    void getById_whenTransactionNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(99L, 1L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
            () -> transactionService.getById(1L, 99L));
    }

    @Test
    void delete_softDeletesTransaction() {
        // Arrange
        Transaction tx = Transaction.builder().id(1L).userId(1L)
            .isDeleted(false).title("Test").amount(100L)
            .type(TransactionType.DEPENSE).txDate(LocalDate.now()).build();

        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(1L, 1L))
            .thenReturn(Optional.of(tx));

        // Act
        transactionService.delete(1L, 1L);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsDeleted());
    }
}
```

**Test 2 — Controller slice test (tests HTTP layer):**

```java
// AuthControllerTest.java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private JwtFilter jwtFilter; // exclude from filter chain

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "email": "not-an-email", "username": "test", "password": "pass123" }
            """))
            .andExpect(status().isBadRequest());
    }
}
```

**Test 3 — Frontend: hook test with React Testing Library:**

```typescript
// useQuery.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { useQuery } from '../hooks/useQuery';

test('returns data on successful fetch', async () => {
  const fetcher = jest.fn().mockResolvedValue({ name: 'test' });

  const { result } = renderHook(() => useQuery(fetcher, []));

  expect(result.current.isLoading).toBe(true);

  await waitFor(() => expect(result.current.isLoading).toBe(false));

  expect(result.current.data).toEqual({ name: 'test' });
  expect(result.current.error).toBeNull();
});
```

### Takeaway
> **Rule:** Aim for the **Testing Pyramid**: many unit tests (fast, isolated), fewer integration tests (real DB), few E2E tests. Start with unit tests for services — they catch the most bugs for the least effort.

---

<a name="block-9"></a>
## ⚡ Block 9 — Stale Closures and Missing `useEffect` Dependencies

### Why it matters
React's `useEffect` dependency array tells React *when* to re-run your effect. Omitting dependencies means your effect reads **stale values** from the first render — a subtle bug that's notoriously hard to debug.

### What your code does

```tsx
// Transactions.tsx
const fetchTransactions = async () => {
    // This function captures `page`, `filters` from the outer scope
    const data = await listTransactions({ page, size: 10, ...filters });
    // ...
};

useEffect(() => {
    fetchTransactions(); // ← `fetchTransactions` is not in the dep array
}, [page, filters]);     // ← But the function itself changes on every render
```

The ESLint rule `react-hooks/exhaustive-deps` would flag this. Your `eslint.config.js` includes the plugin but only if the linter is run — check if you're actually running it.

### 🏋️ Exercise

Stabilize the function with `useCallback`, and make dependencies explicit:

```tsx
// Transactions.tsx — correct pattern
const fetchTransactions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
        const data = await listTransactions({
            page,
            size: 10,
            ...filters,
            type: filters.type || undefined,
            categoryId: filters.categoryId || undefined,
        });
        setTransactions(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
    } catch (err: any) {
        setError(err.response?.data?.error ?? '...');
    } finally {
        setIsLoading(false);
    }
}, [page, filters]); // ← explicit dependencies

useEffect(() => {
    fetchTransactions();
}, [fetchTransactions]); // ← stable reference, only re-runs when page/filters change
```

### Enable the ESLint Rule Strictly

```js
// eslint.config.js — add this to your rules
rules: {
  'react-hooks/exhaustive-deps': 'error', // error, not warn
}
```

### Takeaway
> **Rule:** Enable `react-hooks/exhaustive-deps` as an error in your ESLint config and never suppress it without understanding exactly why. Stale closures cause bugs that appear intermittently and in production only.

---

<a name="block-10"></a>
## 🏷️ Block 10 — `any` Is a Lie You're Telling TypeScript

### Why it matters
Using `any` disables TypeScript's type checking for that value and every value derived from it. You lose autocomplete, refactoring safety, and compile-time error detection — the main reasons to use TypeScript at all.

### What your code does

```tsx
// GoalApi.ts
const transformGoal = (goal: any): Goal => ({  // ← any disables checking
    ...goal,
    targetAmount: Number(fromCentimes(goal.targetAmount)),
});

// GoalCard.tsx
const renderStatusChip = () => {
    switch (goal.status) {  // could check but `any` propagation risks
```

```java
// DashboardService.java
for (int i = 0; i < projections.size(); i++) {
    CategorySpendingProjection proj = projections.get(i);
    if (i < 8) {
        com.gc2026.portfolio.dto.response.CategoryResponse catResp =
            com.gc2026.portfolio.dto.response.CategoryResponse.builder() // ← fully qualified = missing import
```

### 🏋️ Exercise

**Replace `any` with a proper intermediate type:**

```typescript
// goalApi.ts
// Define the raw API response shape
interface GoalApiResponse {
  id: number;
  userId: number;
  title: string;
  targetAmount: number; // centimes from API
  currentAmount: number;
  targetDate: string;
  status: string;
  createdAt: string;
}

// Now the transformer is fully typed
const transformGoal = (goal: GoalApiResponse): Goal => ({
  ...goal,
  targetAmount: Number(fromCentimes(goal.targetAmount)),
  currentAmount: Number(fromCentimes(goal.currentAmount)),
  status: goal.status as GoalStatus, // explicit cast with known values
});
```

**Enable strict TypeScript:**

```json
// tsconfig.app.json — add these
{
  "compilerOptions": {
    "strict": true,         // enables all strict checks
    "noImplicitAny": true,  // error on implicit any
    "strictNullChecks": true
  }
}
```

Fix every error the compiler finds — don't suppress them with `// @ts-ignore`.

### Takeaway
> **Rule:** `any` is an escape hatch, not a solution. Create proper interface types for all API responses. Enable `strict: true` in `tsconfig` and treat TypeScript errors as seriously as runtime errors.

---

<a name="block-11"></a>
## 🚦 Block 11 — No Rate Limiting on Auth Endpoints

### Why it matters
Without rate limiting, an attacker can try millions of password combinations against your `/api/v1/auth/login` endpoint. Brute-force login is trivial without protection.

### What your code does

```java
// SecurityConfig.java
.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
// No rate limiting whatsoever
```

### 🏋️ Exercise

**Option A — Spring's Bucket4j (per-IP rate limiting):**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

```java
// RateLimitFilter.java
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                10,                        // 10 attempts
                Refill.intervally(10, Duration.ofMinutes(1)) // per minute
            ))
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
        }
    }
}
```

### Takeaway
> **Rule:** Auth endpoints must have rate limiting. This is table stakes for any production application. At scale, use a CDN/WAF (Cloudflare, AWS WAF) for this instead.

---

<a name="block-12"></a>
## 🗂️ Block 12 — Category Ownership Is Only Half-Validated

### Why it matters
Your budget creation endpoint checks category ownership — but the logic has a subtle flaw that allows users to create budgets for **system categories they shouldn't be able to modify**, and potentially use categories from other users.

### What your code does

```java
// BudgetService.java
Category category = categoryRepository.findById(request.getCategoryId())
    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

// This check has a logic error:
if (category.getUserId() != null && !category.getUserId().equals(userId)) {
    throw new ValidationException("You cannot create a budget for this category");
}
```

The problem: `findById` finds *any* category in the database, including other users' categories. If user B knows the ID of user A's custom category, the check `category.getUserId() != null` is `true`, and `!category.getUserId().equals(userId)` is `true`, so it throws — but only if the category is non-system. A user could potentially discover valid category IDs belonging to others.

Also, system categories have `userId` pointing to the category's creator — your seeder sets `userId` to the user's ID, not `null`.

### 🏋️ Exercise

Fix the query to only find categories that belong to this user or are system categories:

```java
// CategoryRepository.java — add this method
@Query("SELECT c FROM Category c WHERE c.id = :id AND (c.userId = :userId OR c.isSystem = true)")
Optional<Category> findByIdAndUserIdOrSystem(@Param("id") Long id, @Param("userId") Long userId);

// BudgetService.java — use the safer query
Category category = categoryRepository
    .findByIdAndUserIdOrSystem(request.getCategoryId(), userId)
    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

// Remove the manual ownership check — the query handles it
```

Apply the same pattern in `TransactionService.create` — currently it uses `categoryRepository.findById()` without any ownership check:

```java
// TransactionService.java — currently:
Category category = categoryRepository.findById(dto.getCategoryId())
    .orElseThrow(...); // ← anyone can use anyone's category ID

// Fix:
Category category = categoryRepository
    .findByIdAndUserIdOrSystem(dto.getCategoryId(), userId)
    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
```

### Takeaway
> **Rule:** Authorization is not just "is the user logged in?" — it's "does this user have permission to access this specific resource?" Check at the database query level, not just in application logic.

---

## Summary Checklist

Use this as your personal improvement backlog:

| # | Block | Priority | Effort |
|---|-------|----------|--------|
| 1 | JWT in `localStorage` → `httpOnly` cookie | 🔴 Critical | High |
| 2 | Goal deletion missing userId check | 🔴 Critical | Low |
| 3 | GoalProgress milestones mismatch | 🟠 High | Low |
| 4 | Custom `useQuery` hook | 🟡 Medium | Medium |
| 5 | Add Error Boundary | 🟡 Medium | Low |
| 6 | Persist token blacklist (Redis/DB) | 🟠 High | Medium |
| 7 | Remove secrets from properties file | 🔴 Critical | Low |
| 8 | Write real tests | 🟠 High | High |
| 9 | Fix `useEffect` stale closures | 🟡 Medium | Medium |
| 10 | Eliminate `any` types | 🟡 Medium | Medium |
| 11 | Rate limiting on auth endpoints | 🟠 High | Medium |
| 12 | Fix category ownership query | 🟠 High | Low |

---

## What You Built Well ✅

Before closing, these are patterns in your codebase that reflect solid engineering judgment — keep them:

- **Soft deletes** on transactions (`isDeleted` flag) — data is recoverable
- **`TransactionSpecification`** with the Specification pattern — dynamic queries without N query methods
- **`PaginatedResponse<T>` generic wrapper** — consistent pagination contract
- **Flyway migrations** — schema changes are versioned and reproducible
- **`GlobalExceptionHandler`** — centralized error responses, no stack traces exposed
- **Amounts stored in centimes** as `BIGINT` — avoiding floating-point precision errors in money is a critical correct choice
- **Seeding default categories on registration** — thoughtful UX that reduces new-user friction
- **`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`** — you understood the Page serialization gotcha

The foundation is genuinely strong. These blocks are the next layer — the difference between "it works" and "it's production-ready."
