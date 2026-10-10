<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Rocky And Kyuubi Implementation Plan

Approved on 2026-09-24. This plan extends the existing mpack V1 delivery with
a real Apache Kyuubi package and live acceptance using deploy's existing
Builder and preloaded runtime images. The user explicitly excluded Ubuntu
deployment and testing. This platform decision supersedes the Ubuntu-only
environment prerequisites in the earlier handoff; it does not remove any
in-scope mpack behavior from acceptance.

## Scope And Sources Of Truth

- Core implementation: `/Users/jialiang/PRJS/ambari-mpack-v1`.
- Independent package repository: `/Users/jialiang/PRJS/ambari-mpacks`.
- Build and deployment tooling: `/Users/jialiang/PRJS/deploy`.
- Upstream Kyuubi checkout: `/Users/jialiang/PRJS/kyuubi`.
- Acceptance: [runbook](acceptance-runbook.md),
  [HTTP contract](http-api.md), [delivery baseline](delivery-baseline.md),
  [corrective implementation](corrective-implementation.md), and
  [result ledger](acceptance-results.md).
- Deployment procedures: `deploy/docs/README.md`, scenarios 01, 03, 04,
  07, 08 and 11. Scenario 02 is only needed if the existing runtime contract
  is genuinely incompatible. Scenario 10's legacy mpack builder is not the
  authoring entry point for the new schema-1 store.

Rocky version, architecture, stack, image digests and software versions must
be recorded from verified artifacts before selecting the runtime combination.
Existing Ubuntu code may be preserved, but Ubuntu is not a test target and
must not acquire a VERIFIED status from this work.

## Implementation Sequence

| Stage | Work and completion condition | Initial status |
| --- | --- | --- |
| P0 | Preserve the dirty/untracked source identity; inventory existing Builder, RPMs, manifests and preloaded images; select an exact compatible Rocky environment. | COMPLETE; Spark artifact compatibility remains an integration input |
| P1 | Clone official Apache Kyuubi, pin tag and full commit, inspect its actual startup/configuration/JDBC/session/engine contracts and record compatibility with existing Spark/Scala/JDK. | COMPLETE; runtime compatibility remains unverified |
| P2 | Add `mpacks/kyuubi` with schema-1 manifest, Server/Client definitions, configuration, lifecycle commands, structured observations, focused tests and package documentation; include it in the store release. | IMPLEMENTED; live acceptance pending |
| P3 | Adapt Nginx/PostgreSQL references and GENERIC repository metadata to the selected Rocky platform using typed package/systemd/database observations; retain existing platform paths. | IMPLEMENTED; live acceptance pending |
| P4 | Build complete initial Ambari RPMs in the existing deploy Builder, including frontend, Agent, schema and Python runtime dependencies. Reuse compatible stable RPMs; build Kyuubi alone only if no compatible artifact exists. | Ambari COMPLETE; Kyuubi IN_PROGRESS |
| P5 | Deploy isolated GENERIC and BIGTOP acceptance environments from verified preloaded images and the new Ambari RPM repository; verify actual Server/Agent identities and protocol. | Server/Agent deployed; service environments IN_PROGRESS |
| P6 | Execute AC-00 through AC-20 and Kyuubi-specific live cases, fix demonstrated defects, rerun affected checks and reconcile documentation with structured evidence. | IN_PROGRESS |

Each stage records actual results, artifact identities and unresolved blockers.
The implementation worktrees already contain uncommitted work. Do not reset,
clean, stage or overwrite unrelated changes. Because deploy archives exact
commits, freeze the selected full source in an isolated build snapshot that
includes the existing implementation and new untracked files. An old HEAD
alone is not a valid source identity for these RPMs.

## Kyuubi Package

Use an upstream release compatible with the verified existing ecosystem.
The local Bigtop recipe's Kyuubi 1.9.4 is an initial candidate, not a claim of
runtime compatibility. Record the selected upstream commit and build inputs.
Keep management-definition versions separate from installed software versions.

The package must support installation, configuration, start, stop, status,
restart, version observation and a real service check. Server and Client
components must follow Ambari's daemon and non-daemon operation constraints.
Declare required service dependencies and compatible stack contexts explicitly.
Configuration covers the selected engine, Spark path, listener and discovery
settings, service account, owned directories and logging. Additional modes are
advertised only when implemented and exercised.

The service check opens a real JDBC session, executes deterministic SQL through
the selected Spark engine, verifies typed result values, and closes operation
and session resources. Record exact session/operation identities and the Ambari
task/execution/snapshot association. A listening port, process exit, log text or
successful submission alone is insufficient. Unknown, missing or foreign
results fail validation and remain available for diagnosis.

Kyuubi regression cases cover standalone import, whole-store import, exact
service selection, adding to a compatible existing cluster, configuration and
lifecycle actions, definition updates with software version unchanged,
dependency/uninstall blockers, scoped impact, failed actions and recovery.

## Build And Fast Iteration

The first build must produce and inspect complete installable Ambari RPMs.
Stable Hadoop, Spark, ZooKeeper and other existing artifacts are reused after
verifying their metadata, architecture, digest and dependency closure. Do not
rebuild stable components merely because source or manifest identities changed.
Keep the existing download, Maven and build caches.

After initial installation use the smallest necessary replacement:

| Change | Iteration path |
| --- | --- |
| Server Java | Incremental compilation and replacement of the affected artifact; restart the Server when required. |
| Agent/Python | Replace changed files and restart only processes that retain the old module. |
| React | Build and replace production static assets; rerun affected real-browser flows. |
| Kyuubi binary | Replace the changed runtime artifact and restart affected instances. |
| Mpack definitions/scripts | Build a new lightweight release and use the actual HTTP update/activation lifecycle. |

Retain replacement manifests with source and destination digests and recoverable
originals outside Git. Do not edit published archives, effective snapshots or
Agent snapshot caches in place. Their immutability is part of the feature under
test. Record the baseline RPM plus applied replacements as the effective runtime
identity. RPM dependency/scriptlet or database migration changes require their
specific packaging/migration validation; file replacement cannot prove those
contracts. Do not run another full RPM build by default.

## Live Acceptance

Use dedicated disposable resources and distinct IDs for destructive/recovery
cases. Preserve the runbook order, beginning with a pre-cluster whole-store
IMPORT and exact service selection. A GENERIC Nginx-only deployment must not
install Hadoop services merely because the underlying image contains their
packages. The default seven-service quickstart does not satisfy that case.

Cover every existing store package plus Kyuubi, and the separate HDFS reference
acceptance. AC-00 through AC-09 establish the basic path; AC-10 through AC-20
cover malformed inputs, dependency conflicts, idempotency, durable recovery,
hooks, publication failures, concurrency, immutable Agent resources, PostgreSQL
backup/restore, HDFS parity, database initialization/upgrades and offline recovery.
Add real browser coverage for management, deployment handoff, refresh recovery
and authorization. Ubuntu is excluded; unavailable in-scope infrastructure is
BLOCKED, never silently excluded or counted as PASS.

Retain versioned JSON observations and exact plan, operation, generation,
snapshot, request/task, release/archive and host identities. Validate required
types, outcomes and lineage before recording success. Logs are diagnostic only.
Do not save credentials, secret-bearing configurations or unrestricted payloads
in repository documentation or evidence.

## Documentation And Completion

Update package usage/support documentation with implementation changes. Update
the owning deploy scenario if build/deployment behavior changes. Update the
HTTP contract and design only for actual contract decisions, with their reason
and focused regression evidence. Keep the acceptance ledger and delivery matrix
aligned; retain historical failures and their successful reruns.

Completion requires the four store packages to build reproducibly, the selected
Rocky environment to run the intended core and Agent, Kyuubi SQL execution to
pass, and all in-scope acceptance cases to have explicit evidence or unresolved
blockers. A blocked case prevents a claim of full acceptance. Automatic GC,
aggregate quotas, an online coordinated backup framework and arbitrary frontend
plugins remain outside the previously approved delivery boundary.

## Execution Record

- 2026-09-24: Plan recorded; initial source inspection confirms three existing
  reference packages and no live acceptance results. Docker Desktop context is
  configured locally; daemon availability and existing artifacts still need
  verification. No RPM or live acceptance result is claimed.
- 2026-09-24: Docker started. Selected Rocky 8 aarch64 runtime digest
  `88b3f988311639f4d5bfb66ee09a4b3ea014b3ff9f63b978338d599c317857d0`.
  Deploy Builder prepare upgraded the existing Maven 3.8.8 toolchain to the
  current profile; verified plan `builder-aarch64-4a6dac07e98a` was activated.
- Kyuubi cloned locally at v1.9.4, commit
  `5f48f2ef5b2532383a1e21984eec94ac451caf85`. Added package definitions and
  Rocky adaptations. Eleven focused tests pass both locally and with the
  preloaded image's platform Python 3.6; the JDBC helper compiles with JDK 17
  against the actual Kyuubi JDBC artifact.
- Initial Ambari build `rocky8-arm64-mpack-v1-20260924` completed through
  deploy. Isolated source snapshot: `20c33adf1b4d25647bddf5f4e23144d52cdd0345`;
  source archive SHA-256: `723bcb0e0ca66f735d98513bd1ecfbe4bbe7ff53694c8d8f81584f5e6bc8fbee`.
  Server, Agent and Metrics RPMs are version `3.1.0.0`, release
  `1790252549.git20c33adf1b4d`. Original worktree Git state was not committed.
- Deploy created four isolated nodes with `services: [ambari]`, deployment
  `mpack-rocky-20260924`, plan
  `3e997c9fe3c782dccd440d500a535f1120754226594cbe394e85466109c5aa88`.
  API base: `http://127.0.0.1:18080/api/v1`. Four hosts are registered.
- First live capability call exposed Gson serialization of Jackson internals.
  Added a dedicated JSON-node writer; two real Jersey HTTP tests passed.
  Incremental Server JAR replacement SHA-256:
  `06b239a001487a162d03379a025c7c93a806cbb9b4faec9b84cfa518582cba54`.
  Live capabilities now validate. No second Ambari RPM build was run.
- Kyuubi's first RPM attempt failed because Maven proxy flags reached Vite.
  Deploy now disables pnpm's Maven argument inheritance while retaining npm
  network configuration, and preserves failed intermediate output. Its focused
  suite passed 43 tests. Retry run: `rocky8-arm64-kyuubi-mpack-20260924-v2`.
- Four member archives and the store bundle were built twice and compared
  byte-for-byte. Bundle SHA-256:
  `47c3ab3a4225f70e42749625f4928a369da0e6ad309f85d4e0fcd25bac0d5a1b`.
  Raw artifacts are under
  `/Users/jialiang/.local/share/ambari-mpack-acceptance/20260924`.
