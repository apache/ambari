<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
--->

# Mpack V1 Delivery Baseline

The [2026-09-22 correction](corrective-implementation.md) supersedes the earlier
global-maintenance and import interaction implementation. Its local verification
is separate from the live acceptance ledger. The
[2026-09-26 conclusions](conclusion-2026-09-26.md) record passing Rocky operational
paths and acceptance fixes, including official Kyuubi binaries. The complete
crash/race, advanced Hadoop and database-upgrade matrices are still unfinished;
the original requirement statuses below are not wholesale promoted by partial
deployment evidence.

For a new acceptance session start with [README.md](README.md), execute the
[acceptance runbook](acceptance-runbook.md), and record actual live results in
[acceptance-results.md](acceptance-results.md). The verification record below
is historical focused evidence, not a substitute for those live cases.

The [design](design.md) and [Chinese translation](design.zh-CN.md) define the
accepted behavior. This document maps that behavior to implementation and
verification. Clarifications must update the design and this ledger before the
affected behavior is implemented. Implementation difficulty is not permission
to remove a requirement. Unfinished or unsupported behavior remains visible.

The historical implementation started from
`116be946fb5f911236db70861631eb4ed95792cc` in `work/mpack-v1-http`. The current
implementation is `/Users/jialiang/PRJS/ambari-mpack-v1` on `AMBARI-26663`.
Compilation and live acceptance have since run; use the dated conclusions and
result ledger for current evidence rather than inferring results from source.

Statuses are `NOT_IMPLEMENTED`, `PARTIAL`, `IMPLEMENTED_UNVERIFIED`, `VERIFIED`,
and `DEFERRED_BY_SCOPE`.
`VERIFIED` requires the exact successful commands and associated results in
the verification record. A rejected unsupported operation is evidence only for
its documented rejection contract, not for the missing positive capability.
Row status describes the complete requirement. Focused passing tests are
recorded below without promoting unexecuted integration/deployment acceptance
to VERIFIED.

| ID | Required behavior | Design section | Status | Implementation and evidence |
| --- | --- | --- | --- | --- |
| MP-01 | Versioned manifest, strict validation, all four V1 artifacts | 5 | IMPLEMENTED_UNVERIFIED | MpackManifest/MpackJson; shared published JSON schema; Java and Python contract tests |
| MP-02 | Bounded upload, safe extraction, immutable digest identity | 5, 8.7 | IMPLEMENTED_UNVERIFIED | MpackArchiveStore; configured compressed/expanded/member limits; archive tests |
| MP-03 | One normalized contribution and ownership model, including built-ins | 5, 7.1 | IMPLEMENTED_UNVERIFIED | MpackResources/MpackSnapshots; built-in providers and resource digests |
| MP-04 | Exact dependency resolution and reverse-reference checks | 7.2 | IMPLEMENTED_UNVERIFIED | MpackDependencies; retained and active provider checks; dependency tests |
| MP-05 | Shared stack/version binding context and impact reporting | 7.3 | IMPLEMENTED_UNVERIFIED | MpackPlanner/StackResolutionContext; cluster/service provenance; active upgrade conflict |
| MP-06 | Durable acceptance, idempotency, server-owned progress and recovery | 8.1, 8.2 | IMPLEMENTED_UNVERIFIED | MpackCatalog/MpackLifecycleService/Worker; transaction and acceptance/worker tests |
| MP-07 | Side-effect-free candidate resolution, no implicit autolink | 8.4 | IMPLEMENTED_UNVERIFIED | Isolated StackManager/StackContext; preview has no control-row initialization; candidate tests |
| MP-08 | Immutable definition snapshots and five-step publication protocol | 7.3, 8.6 | IMPLEMENTED_UNVERIFIED | MpackSnapshots/Activation/Runtime; startup effective-snapshot load; crash/deployment acceptance outstanding |
| MP-09 | All supported definition mutation paths use the lifecycle boundary | 4.1 | IMPLEMENTED_UNVERIFIED | Legacy registration/link/unlink reject; local CLI rejects; upgrade replay removed intentionally |
| MP-10 | Controlled in-use update and model-transition checks | 8.3 | IMPLEMENTED_UNVERIFIED | Definition reservations, scoped task drain/control and publication recovery; unsupported model changes reject; live concurrency acceptance outstanding |
| MP-11 | Task, retry, status-check and shared-hook resource pinning | 8.4 | IMPLEMENTED_UNVERIFIED | ExecutionCommand/Wrapper/ActionDBAccessor/Scheduler; Agent ConfigurationBuilder/FileCache tests |
| MP-12 | Server and Agent immutable resource addressing and retention | 8.4, 8.7 | IMPLEMENTED_UNVERIFIED | ResourceManager and FileCache; append-only retention, no fallback, no automatic deletion |
| MP-13 | Unbind and uninstall with exact blockers and no host-data removal | 8.3 | IMPLEMENTED_UNVERIFIED | Planner/Usage; dependencies, links, cluster stack and service blockers; retire bytes without deletion |
| MP-14 | Bundle dependency validation, independent results and safe partial retry | 8.5 | IMPLEMENTED_UNVERIFIED | MpackBundles; derived member outcomes; retry retains completed hooks and attempt history |
| MP-15 | Hook identity, structured observations and unresolved-side-effect recovery | 4.2, 8.4, 8.6 | IMPLEMENTED_UNVERIFIED | MpackHookRunner/LifecycleState; attempt-specific receipts; uncertain effects never automatically replay |
| MP-16 | HTTP resources, permissions, error codes and public API contract | 10.1 | IMPLEMENTED_UNVERIFIED | MpackLifecycleApiService/ExceptionMapper; administrator-only; HTTP contract in http-api.md |
| MP-17 | HTTP-only CLI with resumable operations and local authoring tools | 10.2, 11 | IMPLEMENTED_UNVERIFIED | dev-support/mpack package; durable submissions without credentials; client/build tests |
| MP-18 | GENERIC runtime, repository mapping and independent software versions | 9 | IMPLEMENTED_UNVERIFIED | hooksFolder/repositoryVersionMode inheritance; independent repository validation; default VDF accepts empty upgrade manifest |
| MP-19 | Fresh GENERIC Nginx deployment without Hadoop dependencies | 12, 13 | VERIFIED | Fresh Rocky 8 worker5 deployment; install 59/209 and start 60/210; final health 102/284; see 2026-09-26 conclusions |
| MP-20 | PostgreSQL isolated backup/restore and failure recovery | 9.1, 13 | PARTIAL | Distinct-instance recovery, typed data comparison and eight unsafe/corrupt-input cases passed; interrupted recovery remains unexecuted |
| MP-21 | Non-daemon component operation constraints | 9.2 | PARTIAL | Existing CLIENT categories reused; no new example or new operation model; dedicated acceptance outstanding |
| MP-22 | Reproducible HDFS reference and full supported capability parity | 6, 13 | PARTIAL | Fixed-Git-SHA export/build passed; 640 resources match source, 3 stack metainfo files differ only by hooksFolder; advanced live workflow acceptance outstanding |
| MP-23 | React management before cluster creation, recovery and deployment handoff | 10.3 | PARTIAL | Authenticated import, lost-response recovery, exact service handoff, permissions and mobile layout passed; empty-Server browser context remains unexecuted |
| MP-24 | Independent third-party repository, bundles and deterministic tooling | 11 | IMPLEMENTED_UNVERIFIED | Separate ambari-mpacks repository; release/profile/tool lock; deterministic Python builder; remote publication not requested |
| MP-25 | Supported database initialization and upgrade paths | 12 | IMPLEMENTED_UNVERIFIED | mpack_record JPA entity/DAO; seven create DDLs and UpgradeCatalog310; actual database upgrade acceptance outstanding |
| MP-26 | Aggregate quotas, automatic cleanup and automated coordinated backup/restore | 1.4, 8.6, 8.7 | DEFERRED_BY_SCOPE | User-directed scope freeze; keep all recovery bytes, enforce per-archive bounds, monitor disk; offline database+store backup only |
| MP-27 | Whole-mpackstore import and generic service selection | 1.4, 10.3, 11 | IMPLEMENTED_UNVERIFIED | IMPORT/ENABLE, Java-derived catalog, exact service/destination handoff, generic models and declared commands; local evidence in corrective-implementation.md; live acceptance pending |

## Stage Checkpoint: 2026-09-24

See [the stage checkpoint](checkpoint-2026-09-24.md) and
[acceptance results](acceptance-results.md) for actual Rocky deployment,
incremental fixes and topic commits. Ubuntu is excluded by the user. Nginx and
PostgreSQL basic runtime checks passed after fixes; Kyuubi starts but SQL
acceptance is still failing. Earlier matrix entries remain unverified wherever
their complete acceptance scope has not run. The user requested remote pushes
without a pull request and a pause afterward.

## Closeout Boundary

The scope freeze in design section 1.4 stops additional feature development.
Closeout consists of blocking-defect fixes, focused regressions, final Java and
React builds and Python tests, topic commits, and the final documentation audit.
Keep live GENERIC/Nginx, PostgreSQL recovery, HDFS advanced workflows, and
multi-database upgrades visibly unverified when the required environment is
not available. Passing unit tests alone must not promote these rows to VERIFIED.

## Clarification Record

Record the requirement ID, reason, chosen behavior, and affected acceptance
cases here when implementation exposes an underspecified contract. Keep the
normative behavior in its owning design section instead of duplicating it here.

- MP-01/MP-02: Section 5.1 specifies initial JSON, numeric version, dependency,
  hook, archive layout, and size contracts. Strict input validation and shared
  Java/Python fixtures must cover malformed and ambiguous inputs.
- MP-07: StackContext also registers service checks and performs remote
  repository discovery during resolution. Section 8.4 now includes these in
  candidate isolation, not just database writes and autolink.
- MP-18: Section 9 specifies stack-level `hooksFolder` selection because the
  existing global default executes Hadoop/Java setup. Cover inherited, disabled,
  explicit, and pinned hook locations; do not special-case the GENERIC name.
- MP-18: RepositoryVersionResourceProvider enforces the stack-version prefix.
  Section 9 adds the explicit `repositoryVersionMode` contract, retaining that
  check for distribution stacks while allowing independent service selections.
- MP-06/MP-25: An internal `mpack_record` table stores versioned lifecycle
  records using the existing JPA transaction infrastructure. Related operation,
  idempotency, and inventory writes use one revision-checked transaction. This
  private table is not accessible through `/persist` and does not introduce a
  separate state machine for upload or plan records. Existing package API
  convergence remains part of MP-09/MP-16, not an accomplished result of adding
  persistence.
- MP-14/MP-15: Sections 8.4 and 8.5 define hook attempt identity, known versus
  unknown effects, safe explicit retry/cancellation, and bundle member evidence
  when definitions publish together but a later hook fails. Test missing and
  foreign receipts, retained successful members, and retry without replaying
  completed effects.
- MP-06/MP-07: First preview uses a virtual built-in control revision of -1;
  only accepted operations initialize durable control. Acceptance recaptures
  and verifies the distribution snapshot against the preview.
- MP-15: Resource processing is implemented in Java rather than a separate
  Python worker. Hook subprocesses still require structured, lineage-checked
  receipts. There is no additional resource-worker RPC.
- MP-18: Default version definitions reuse AmbariMetaInfo and repoinfo.
  Empty upgrade manifests are valid for foundations with no advertised
  distribution services. Stack services remain visible without opting into
  stack upgrades or claiming an observed host software version.
- MP-09: Historical local install/update/uninstall and upgrade replay are not
  compatibility requirements; replay removal has an explicit regression.
- MP-16/MP-26: The user-directed freeze replaces new OpenAPI/code-generation,
  cleanup/quota/backup subsystems with a checked HTTP contract and documented
  operational limits. These are explicit scope changes, not verification.

## Verification Record

Final verification ran on 2026-09-17 after implementation was frozen. Java uses
JDK 17 and Maven 3.9.11; Python tooling/tests use a separate Python 3.10 venv;
React dependencies were installed with `npm ci` in the implementation worktree.
The original source workspace still has only its pre-existing untracked
`docs/mpack/`. No source was reverted or committed there.

### Successful Commands

Run from the implementation worktree unless a different directory is stated.
These are focused build/test results, not a complete Ambari distribution build.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  /tmp/apache-maven-3.9.11/bin/mvn -B -ntp \
  -l /tmp/ambari-mpack-v1-java-clean-compile.log -Pstatic-web \
  -pl ambari-server -am clean test-compile \
  -DskipTests=true -DskipPythonTests=true -DskipSurefireTests=true \
  -Dexec.skip=true -Dcheckstyle.skip=true -Drat.skip=true \
  -Dswagger.skip=true -DskipUiBuild=true
```

Result: exit 0; Server and dependent Java modules compiled, including all 900
Server test source files. Legacy Admin UI, Python bundling/tests, RAT,
checkstyle, and Swagger generation are explicitly excluded from this command.
React compilation and Python tests were executed separately below.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  /tmp/apache-maven-3.9.11/bin/mvn -B -ntp \
  -l /tmp/ambari-mpack-v1-java-verified-tests.log -Pstatic-web \
  -pl ambari-server surefire:test \
  -Dtest=MpackManifestTest,MpackArchiveStoreTest,MpackDependenciesTest,MpackSnapshotsTest,MpackLifecycleStateTest,MpackRecordDAOTest,StackCandidateContextTest,MpackLifecycleServiceTest,MpackPlannerTest,VersionDefinitionTest,StackAdvisorHelperTest,StackManagerTest,RepositoryVersionResourceProviderTest,ExecutionCommandWrapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false \
  -DskipSurefireTests=false -Dsurefire.argLine= -DskipUiBuild=true
```

Result: exit 0; 14 suites, 117 tests, 0 failures, 0 errors, 0 skipped.
Counts were also checked against the Surefire XML reports, not inferred from
diagnostic messages. Internal dependency jars were installed from this
worktree before the standalone goal to avoid stale SNAPSHOT cache artifacts.

In `ambari-web/latest`:

```sh
npm ci --no-audit --no-fund
npm run build
npm test -- src/api/mpackApi.test.ts src/components/AssignMasters.test.tsx src/components/AssignMastersAddable.test.tsx src/screens/Directories/GlobalDirectoryLayout.test.tsx src/api/versionsApi.test.ts
```

Result: each command exited 0; TypeScript/Vite production build passed;
5 suites and 43 tests passed. Existing Sass deprecations, bundle-size warnings,
React key warnings, and Node localStorage warnings remain diagnostic warnings.
The implementation worktree's Vite development server is available at
`http://127.0.0.1:5177`; `/mpacks` and its source module returned HTTP 200.
Port 5176 was already occupied and its existing process was left untouched.
This is a frontend resource smoke check only. Functional use requires the
Ambari backend; no authenticated browser/API workflow was exercised.

```sh
/tmp/ambari-mpack-v1-py310/bin/python -m pip install -e dev-support/mpack
/tmp/ambari-mpack-v1-py310/bin/python -m unittest discover -s dev-support/mpack/tests -v
```

Result: tooling installed; 13 contract/builder/client tests passed.
Agent/Server runtime dependencies were separately installed from their
hash-locked requirements in that venv, without changing dependency manifests.

In `ambari-agent/src/test/python`:

```sh
TMPDIR=/private/tmp \
PYTHONPATH=/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/test/python/ambari_agent:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python/ambari_agent:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/test/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/test/python \
  /tmp/ambari-mpack-v1-py310/bin/python -m unittest TestFileCache TestConfigurationBuilder -v
```

Result: 40 tests passed. The working directory supplies the legacy test fixtures;
TMPDIR avoids macOS `/var` versus `/private/var` alias differences in existing
path/concurrency assertions.

In `ambari-server/src/test/python`:

```sh
TMPDIR=/private/tmp \
PYTHONPATH=/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/test/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python/ambari_agent:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-server/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-server/src/main/python/ambari_server:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-server/src/main/python/ambari-server-state:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-server/src/main/resources/custom_actions:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-server/src/main/resources/scripts \
  /tmp/ambari-mpack-v1-py310/bin/python -m unittest TestAmbariServer.TestAmbariServer.test_upgrade_linux -v
```

Result: 1 test passed, including the intentional no-replay assertion.

### Reference Artifacts

```sh
/tmp/ambari-mpack-v1-py310/bin/ambari-mpack --json build --all --repository /Users/jialiang/PRJS/ambari-mpacks --output /tmp/ambari-mpack-v1-reference-build --bundle infrastructure
/tmp/ambari-mpack-v1-py310/bin/ambari-mpack --json build --all --repository /Users/jialiang/PRJS/ambari-mpacks --output /tmp/ambari-mpack-v1-reference-build-repeat --bundle infrastructure
/tmp/ambari-mpack-v1-py310/bin/ambari-mpack --json export-hdfs-reference --repository /Users/jialiang/PRJS/ambari --commit 116be946fb5f911236db70861631eb4ed95792cc --output /tmp/ambari-mpack-v1-hdfs-reference
/tmp/ambari-mpack-v1-py310/bin/ambari-mpack --json build /tmp/ambari-mpack-v1-hdfs-reference --output /tmp/ambari-mpack-v1-hdfs-build
git diff --check
```

Result: all commands exited 0. The three ordinary-software archives and their
bundle have identical SHA-256 values across the two builds. The bundle digest
is `a917dbdaafdf483d7423e03ee2b6192ae0926bc4895a7e82384e9c5b3b7b3f3e`.
HDFS reference archive digest is
`11be67822beb0af1c4f6e7fa292e8f03cd3d393312e74226e719801129bf0cf7`.
The source tar/provenance and exported files were checked structurally:
640 mapped resource entries retain bytes or exact link targets; the three
BIGTOP stack metainfo files preserve the XML tree except the declared
`hooksFolder`. This is resource preservation evidence only, not proof of live
HDFS HA, Federation, security, or upgrade parity.

### Resolved Failures

- Initial frontend tests used incomplete shared node_modules and an unavailable
  Chai matcher. Independent `npm ci` and the native getAttribute assertion
  resolved them; the final focused run is green.
- System Python 3.9/old pip could not install the Python >=3.10 tooling. The
  separate Python 3.10 venv installed it successfully.
- Initial Java compilation found a missing checked AmbariException declaration
  in commitEffective. The method and its transaction rollback contract were
  corrected. A failed javac run left missing secondary classes; clean
  compilation regenerated them and resolved JAXB initialization errors.
- The new assisted factory method required the corresponding StackManagerMock
  constructor; isolated candidates bypass its shared parser cache. The admin
  acceptance fixture now carries the real permission name. Final tests pass.
- Earlier combined Maven test attempts triggered the unrelated legacy Admin
  UI/Bower tests. Java test compilation and the explicit Surefire goal isolate
  the affected Java suites; that legacy UI is not declared verified.

### Outstanding Acceptance And Commits

No actual Ambari HTTP deployment, browser session, GENERIC host installation,
PostgreSQL backup/restore, HDFS advanced workflow, multi-database upgrade,
online crash/restart publication, or offline backup/restore was exercised.
These remain acceptance gaps, not additional framework-development scope.

No commits or staging have occurred. The requested topic commits require a
JIRA key under AGENTS.md; none was supplied and no JIRA credentials are
available in this environment. Planned topics are shared contracts/storage,
atomic Server/Agent lifecycle integration, CLI tooling, React integration, and
the final documentation/evidence update. The independent reference repository
also requires its own source commit; none has been created. Never stage
generated egg-info, caches, build
outputs, or original-workspace files.
Final documentation audit checked 51 relative links under docs/mpack, with no
missing targets. All 54 new tracked-candidate files were checked for source/
documentation license headers; generated package metadata is ignored.
These counts describe the source-closeout audit before the separate acceptance
handoff documents were added. Live case status is maintained in the acceptance
result ledger and must not be inferred from the historical counts.

### Acceptance Handoff Audit

The document-only handoff added README.md, acceptance-runbook.md and
acceptance-results.md on 2026-09-17. Checked 74 relative document links with
no missing targets, paired code fences and ASF headers in the three new
documents, and matching AC-00 through AC-20 IDs in the runbook/result ledger.
All 21 live case results remain NOT_RUN. No implementation source, build,
test, deployment or commit was changed/executed as part of this handoff.
