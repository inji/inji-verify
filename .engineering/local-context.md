# inji-verify: local engineering context

Context for contributors and AI agents working in this repository: layout, verified build and test commands, and the
default change scope. Produced by module onboarding with the AIDLC Engineering Agent and reviewed by the module owner.
Every row was **verified** on `upstream/develop` @ `110c0529` (2026-10-29) from the file named, unless marked Inferred.
`AGENTS.md` and the repository's own docs win where they are more specific.

| Item | Value | Evidence |
|---|---|---|
| Repository | https://github.com/inji/inji-verify (base branch `develop`) | |
| Maven parent | `pom.xml` | file |
| Java | 21 (`pom.xml`); 1.8 in `ui-test/pom.xml` (separate UI-test build) | `pom.xml:109`, `ui-test/pom.xml:31` |
| Unit-test libraries | spring-boot-starter-test | poms |
| JaCoCo | yes | `pom.xml:319` |
| CI workflows | browserstack-web.yml, chart-lint-publish.yml, codeql.yml, publish-inji-verify-sdk-npm.yml, push-trigger.yml | `.github/workflows/` |
| Reusable CI | mosip/kattu workflows (browserstack-web, chart-lint-publish, docker-build, maven-build, maven-publish-to-nexus, maven-sonar-analysis, npm-publish-to-npm-registry, npm-sonar-analysis) @ v0.0.1; GitHub CodeQL; actions/checkout, setup-java, cache, upload-artifact; 8398a7/action-slack | workflow `uses:` |
| DB scripts | `db_scripts/inji_verify/`: init scripts (`db.sql`, `ddl.sql` + `ddl/*.sql`, `grants.sql`, `role_dbuser.sql`, `drop_db.sql`, `drop_role.sql`) with `deploy.sh` / `deploy.properties`; top-level `init_db.sh`, `copy_cm_func.sh`, `init_values.yaml`, `postgres-config.yaml`, `README.md`; no versioned upgrade/rollback pairs yet | `db_scripts/` listing |
| Deploy | Helm: `helm/inji-verify-service`, `helm/inji-verify-ui`, `helm/inji-verify-uitestrig`, `helm/injiverify-apitestrig`; 4 Dockerfiles | files |
| Repository guidance | `AGENTS.md` | file |
| API tests | yes, `api-test/` (see `.sdet/module-context.md`) | `api-test/` |

## Services and tests

| Service | Sources | Component-test styles |
|---|---|---|
| `verify-core` | 104 main / 53 test | MockMvc 0, @WebMvcTest 0, @SpringBootTest 0 |
| `verify-service` | 13 main / 11 test | MockMvc 6, @WebMvcTest 3, @SpringBootTest 1 |

## Node / front-end projects

| Project | Test runner | Test command | lcov coverage | Lockfile / Node |
|---|---|---|---|---|
| `inji-verify-sdk` (@injistack/react-inji-verify-sdk) | Jest, 5 test files | `npm run test` | no: `coverageReporters: ["html"]` (KI-IV-003) | package-lock.json; Node not pinned |
| `verify-ui` (inji-verify) | Jest, 46 test files | `npm run test:ci` | yes | package-lock.json; Node not pinned (AGENTS.md: Node 18, KI-IV-004) |

## UI test projects

- `ui-test/`: Selenium/Cucumber-style Maven project. Not a service: excluded from unit-test and coverage gates.

## Verified commands (2026-10-29, upstream/develop 110c0529)

- **Backend tests + coverage** (Verified: exit 0, about 5 min; verify-core 642 tests, verify-service 84 tests, 0 failures; JaCoCo
  line coverage verify-core 87.7 %, verify-service 97.5 %), from the repository root:
  `mvn -B -Dmaven.gitcommitid.skip=true -pl verify-service -am test jacoco:report`
  -> `verify-core/target/site/jacoco/jacoco.xml`, `verify-service/target/site/jacoco/jacoco.xml`
- **verify-ui** (Verified: 46 suites / 209 tests passed, 0 snapshots written, about 1 min; line coverage 88.5 % over 76 files):
  `npm ci --prefix verify-ui --ignore-scripts`, then `cd verify-ui && npm run test:ci` -> `verify-ui/coverage/lcov.info`.
  Use `test:ci`, never `npm test`: it runs `--updateSnapshot` (KI-IV-005). On a slow network add
  `--fetch-timeout=600000 --fetch-retries=5` to `npm ci` (a first attempt timed out on the registry).
- **inji-verify-sdk** (Inferred, not run): its `npm test` installs React 18/19 peers itself; for lcov use
  `npm ci --ignore-scripts && npx jest --coverage --coverageReporters=lcov` (KI-IV-003).

## Commands (Inferred from the layout; mark Verified once run)

- Build: `mvn -B -f pom.xml -Dmaven.gitcommitid.skip=true -pl <service> -am verify`
- Unit tests: `mvn -B -f pom.xml -Dmaven.gitcommitid.skip=true -pl <service> -am test` -> `<service>/target/surefire-reports/TEST-*.xml`
- Coverage: the same with `jacoco:report` -> `<service>/target/site/jacoco/jacoco.xml`
- Component tests: follow `verify-service/src/test/java/io/inji/verify/controller/DidWebControllerTest.java`

## Default change scope (a task's plan narrows it)

- `verify-core/src/main/java/**`, `verify-core/src/test/java/**`
- `verify-service/src/main/java/**`, `verify-service/src/test/java/**`
- API tests: `api-test/src/main/resources/**`, `api-test/testNgXmlFiles/**`
- DB: `db_scripts/**` (database-migration changes only)
- Sensitive, needs explicit approval: `.github/**`, `helm/**`, `deploy/**`, `**/application*.properties`, Dockerfiles

## Open (module owner to confirm)

- Which environment runs a branch build for system tests.
- Coverage threshold, if the team's standard differs from 80 %.
