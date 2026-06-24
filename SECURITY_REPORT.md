# Säkerhetsrapport — Library API

Den här rapporten är en genomgång av säkerheten i Library API, kopplat till
Individuell Labb 2k5. Jag gick igenom koden mot OWASP Top 10 och hittade tre
sårbarheter som kändes verkliga och relevanta för just denna applikation —
inte bara teoretiska checklistepunkter. Nedan beskriver jag vad jag hittade,
hur jag fixade det, och varför jag tyckte att just dessa tre var värda att
prioritera.

## Sammanfattning

| # | OWASP-kategori | Fanns i | Löst med |
|---|---|---|---|
| 1 | A07:2021 — Identification and Authentication Failures | `SecurityConfig.java` | JWT-autentisering |
| 2 | A01:2021 — Broken Access Control | `SecurityConfig.java` | Deny-by-default + rollbaserad behörighet |
| 3 | A06:2021 — Vulnerable and Outdated Components | `pom.xml` | OWASP Dependency-Check, kopplat till CI |

---

## 1. A07 — Identification and Authentication Failures

### Identifiering
Den ursprungliga `SecurityConfig` använde HTTP Basic Auth med ett enda
inloggat konto, hårdkodat direkt i koden:

```java
UserDetails user = User.builder()
    .username("admin")
    .password("{noop}password")   // "{noop}" = inget hashat lösenord, rent klartext
    .roles("ADMIN")
    .build();
```

Det här är egentligen två problem i ett:
- Lösenordet lagrades och jämfördes **i klartext** — `{noop}` betyder
  uttryckligen "ingen hashning alls". Vem som helst som kommer åt
  källkoden (eller en minnesdump av servern) får lösenordet gratis.
- Lösenordet låg **hårdkodat i källkoden**. Om man ville byta det måste man
  ändra koden och deploya om — det går inte att rotera ett läckt lösenord
  i efterhand utan en ny release.
- Basic Auth skickar dessutom lösenordet i klartext i *varje enda request*,
  vilket gör att risken för att det fångas upp (t.ex. i en proxy-logg)
  ökar för varje anrop som görs.

### Åtgärd
- Bytte ut Basic Auth mot **stateless JWT-autentisering**:
  `POST /api/v1/auth/login` loggar in via Springs `AuthenticationManager`
  och returnerar en signerad (HS256) token. Alla andra endpoints kräver nu
  `Authorization: Bearer <token>` (se `JwtAuthenticationFilter` och
  `JwtService`).
- Lösenord **hashas nu med BCrypt** (`PasswordEncoder`-bean i
  `SecurityConfig`) istället för att sparas i klartext.
- Användarnamn, lösenord och signeringsnyckeln för JWT-tokens läses från
  **miljövariabler** (`JWT_SECRET`, `ADMIN_PASSWORD`, `USER_PASSWORD`, ...)
  och ligger inte hårdkodade — i Docker och CI sätts de riktiga värdena via
  `-e`-flaggan respektive GitHub Secrets, aldrig i koden.
- Misslyckade inloggningar och saknad/ogiltig token ger nu ett konsekvent
  `401`-svar (via `RestAuthenticationEntryPoint` och en ny
  `AuthenticationException`-hanterare i `GlobalExceptionHandler`) istället
  för en stacktrace eller Spring Securitys standardsida.

### Analys & prioritering
Jag valde att börja här eftersom autentisering är grunden allt annat säkerhetsarbete
står på — om man kan komma in genom dörren utan att visa legitimation
spelar det ingen roll hur bra lås man satt på skåpen därinne. Ett hårdkodat,
ohashat lösenord är precis den typen av sårbarhet som ligger kvar synlig i
`git log` för alltid, även efter man "fixat" den i en senare commit. Om den
här applikationen hade hanterat riktiga lån — eller om den, som i vårt
labbexempel, varit kopplad till en betald tjänst — hade ett läckt lösenord
gett en angripare obegränsad åtkomst som admin, utan något sätt att
återkalla den åtkomsten förutom att skriva om och deploya ny kod. Med korta,
miljöstyrda JWT-tokens kan man rotera en läckt nyckel utan kodändring, och en
kapad token slutar fungera efter en timme istället för att vara giltig
permanent.

---

## 2. A01 — Broken Access Control

### Identifiering
Den ursprungliga behörighetslogiken såg ut så här:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()   // <-- öppet som standard
)
```

Två problem:
- `anyRequest().permitAll()` betyder att **allt som inte är explicit
  matchat ovanför är öppet för hela internet som standard**. Lägger man till
  en ny endpoint senare och glömmer skydda den, är den automatiskt publik —
  precis tvärtom mot hur det borde fungera (stäng allt, öppna bara det som
  behövs).
- Alla regler för `/api/**` krävde bara `.authenticated()` — det fanns ingen
  skillnad mellan roller. Det enda `admin`-kontot kunde både läsa *och*
  skriva, men det kunde också vilken annan inloggad användare som helst, nu
  eller i framtiden. Det gick inte att skapa ett konto med enbart
  läsrättigheter, t.ex. till en samarbetspartner eller en praktikant.

### Åtgärd
- Bytte standardregeln till **`anyRequest().authenticated()`** — stängt som
  standard. Endast inloggningsendpointen och API-dokumentationen är
  uttryckligen publika.
- Delade upp behörigheten **per HTTP-metod**: `GET` på `/api/v1/**` och
  `/api/v2/**` kräver bara en inloggad användare, oavsett roll. `POST`,
  `PUT`, `PATCH` och `DELETE` kräver `hasRole("ADMIN")`. Kontot `user` kan
  alltså bläddra i katalogen men kan inte skapa, ändra eller ta bort böcker,
  författare eller lån.
- Obehöriga skrivförsök ger nu ett strukturerat `403`-svar via
  `RestAccessDeniedHandler`. Det här är testat i `AuthIntegrationTest`: en
  token med rollen `USER` får `403` på `POST /api/v1/authors`, medan en
  token med rollen `ADMIN` får `201`.

### Analys & prioritering
Broken Access Control toppar OWASP:s egen statistik över de vanligaste
sårbarheterna, och "öppet som standard" är precis den typen av miss som gör
att en ny endpoint glöms bort och hamnar öppen utan att någon märker det.
Kombinerat med att alla inloggade konton hade samma rättigheter innebar det
att *vilken* giltig inloggning som helst — oavsett hur den togs fram — var
likvärdig med fullständig adminåtkomst. I en verklig drift hade det betytt
att ett läckt konto med tänkt läsbehörighet (t.ex. en integrationsnyckel till
en samarbetspartner) hade räckt för att radera hela bokkatalogen. Jag
prioriterade den här tillsammans med autentiseringsfixen eftersom de
kompletterar varandra: JWT avgör *vem* du är, men utan rollbaserad
behörighet säger det ingenting om *vad* du faktiskt får göra när du är
inloggad.

---

## 3. A06 — Vulnerable and Outdated Components

### Identifiering
Projektet drar in en hel del tredjepartsbibliotek (Spring Security, Spring
Data Redis, Spring Cloud Vault, Resilience4j, Bucket4j, jjwt,
springdoc-openapi, med flera) men det fanns **ingen automatisk process** för
att upptäcka om något av dem — eller deras egna underliggande beroenden —
har en känd säkerhetsbrist (CVE). Utan det skulle en kritisk sårbarhet i
till exempel ett JSON-bibliotek kunna ligga oupptäckt på obestämd tid; man
skulle bara märka det om man manuellt läste säkerhetsbulletiner, vilket i
praktiken aldrig händer kontinuerligt.

### Åtgärd
- Lade till **OWASP Dependency-Check-pluginet** för Maven (`pom.xml`),
  konfigurerat med `failBuildOnCVSS=7` — hittas ett beroende med en hög
  eller kritisk sårbarhet stoppas bygget.
- Pluginet körs **medvetet inte automatiskt** vid `mvn test` eller
  `mvn package`, så den vanliga utvecklingsloopen förblir snabb. Istället
  körs det som ett eget steg, `dependency-check`, i `ci-cd-pipeline.yml` —
  det körs på varje push och pull request, och **blockerar**
  `docker-build-push`-jobbet om det misslyckas. En osäker build kan därmed
  aldrig nå Docker Hub.
- Rapporten (HTML/JSON) sparas som en artefakt i GitHub Actions, så man kan
  gå in och se exakt vilket beroende och vilken CVE som orsakade felet.

### Analys & prioritering
Jag valde tröskeln CVSS 7 (Hög) istället för en strängare gräns på bara
Kritiska sårbarheter (CVSS 9). Anledningen är att en hög-allvarlig brist i
ett bibliotek som faktiskt körs i varje request — som Spring Security eller
Jackson — är en reell risk, inte bara en teoretisk. Kostnaden för att
ibland behöva utvärdera ett falskt larm är betydligt lägre än kostnaden för
att av misstag skicka ut ett känt sårbart bibliotek i produktion. Den här
sårbarheten hamnade på tredje plats eftersom det, till skillnad från de
andra två, inte är ett fel i kod jag själv skrivit — det är en risk i kod
jag *är beroende av*, och med sex nya tredjepartsbibliotek tillagda bara i
den här labben blir det helt orealistiskt att hålla koll på det manuellt.
Genom att automatisera kontrollen i CI får man reda på en nyupptäckt CVE
redan vid nästa push, istället för att upptäcka den via en incident.

---

## Bonus: extra skyddslager (inte en av de tre obligatoriska fixarna)

`RateLimitingFilter` (byggd med Bucket4j) begränsar hur många requests en
enskild IP-adress får göra per minut, och svarar med `429 Too Many Requests`
om gränsen överskrids. Det här är inte en av de tre sårbarheterna ovan, men
den kompletterar autentiseringsfixen rent praktiskt: utan den hade
`/api/v1/auth/login` varit helt öppen för obegränsade brute-force-försök
mot lösenord, även med stark hashning på plats.
