<!---
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
--->

# Mpack V1 Acceptance Runbook

Read [the entry point](README.md), [delivery baseline](delivery-baseline.md)
and [HTTP contract](http-api.md) first. This is an execution plan, not evidence
that the cases passed. Record new results in [the result ledger](acceptance-results.md).
The source implementation is frozen; acceptance defects may require focused
fixes, but not additional frameworks or a weaker definition of success.

The [2026-09-24 Rocky/Kyuubi plan](rocky-kyuubi-implementation-plan.md)
supersedes the original Ubuntu prerequisite. This run uses Rocky 8 aarch64
and excludes Ubuntu deployment and testing. Exercise the complete current
store, including Kyuubi, as well as the GENERIC infrastructure selection.
Ubuntu-specific examples below are historical, not this run's commands.

## 1. Preconditions And Evidence

AC-00 must pass before live cases. Establish an isolated environment; do not
use an existing business cluster for destructive/failure tests.

| Requirement | What to establish |
| --- | --- |
| Source identity | Worktree/branch/HEAD, dirty and untracked file inventory, content digests or preserved source artifact, reference repository identity |
| Server | Actual running Server built from this implementation, JDK 17, selected supported database; this is not the Vite frontend |
| Packaging | Fresh runtime artifacts including the published manifest schema, Python dependencies and wrappers; compile-only commands in the baseline do not produce a deployment |
| Agent | Agent built from this implementation, registered and connected; structured Agent environment advertises `resourceProtocols` containing `MPACK_RESOURCES_V1` |
| Hosts | Rocky 8 aarch64 with systemd; two independent PostgreSQL instances for isolated recovery; compatible Spark/Hadoop/ZooKeeper inputs for Kyuubi |
| Accounts | Separate authenticated Ambari administrator and restricted account; use interactive password prompts or approved secret injection |
| Store | Server service account can write `mpacks.managed.path`; sufficient disk space; no manual modification of distribution roots |
| Observability | Authorized API responses, task records, allowlisted execution payload fields, snapshot/resource hashes; database reads only if a required persistent field is not exposed by API |
| Failure fixtures | Isolated candidate/update/hook fixtures and controlled fault injection; never mutate the submitted release bytes or authoritative lifecycle rows to force a desired outcome |

The current Rocky references use RPM database bindings, systemd D-Bus, OS
platform Python and psycopg2. They do not promise every OS/architecture.
Verify the packaged Agent Python ABI against its native dependencies; do not
silently substitute system Python for the core wrapper. Reference observers
intentionally run under `/usr/libexec/platform-python` on Rocky 8 for the OS
client libraries. This does not replace the Agent's Python 3.9 runtime.

Server properties and defaults:

| Property | Default |
| --- | --- |
| `mpacks.managed.path` | `/var/lib/ambari-server/mpack-store` |
| `mpacks.upload.max.bytes` | `268435456` |
| `mpacks.expanded.max.bytes` | `1073741824` |
| `mpacks.archive.max.entries` | `100000` |

Place raw evidence and fixture archives outside Git. Do not record credentials,
Authorization headers, cookies, private keys, or full secret-bearing configs.
Use JSON parsers/typed clients and exact documented fields. Validate schema,
required types and identities before consuming a response. Logs are diagnostic
evidence only. A missing observation, foreign identity or inconsistent result
is not success; mark FAIL, or BLOCKED if infrastructure prevents execution.

For each mutation retain plan ID/digest, idempotency key, operation ID,
generation, previous/candidate/effective snapshot IDs, exact release/archive
identities, affected clusters, and observed result. For host commands retain
request/task/host/component IDs and only these execution resource fields:
`mpack_definition_snapshot`, `mpack_resource_contract`, `mpack_execution_id`,
`mpack_resource_references`, `resource_archive_digests`, service package and
hook folders. Persisted command JSON or a structured STOMP/test capture may
supply them; do not substitute human-readable command detail strings.

## 2. Prepare Reproducible Inputs

Run authoring from `/Users/jialiang/PRJS/ambari-mpack-v1`, using Python >=3.10:

```sh
python3.10 -m venv /tmp/mpack-acceptance-venv
/tmp/mpack-acceptance-venv/bin/python -m pip install -e dev-support/mpack
export PATH="/tmp/mpack-acceptance-venv/bin:$PATH"
ambari-mpack --json build --all --repository /Users/jialiang/PRJS/ambari-mpacks --output /tmp/mpack-acceptance-build --bundle infrastructure
```

Choose the actual isolated deployment as `AMBARI_SERVER_URL` and its admin
username as `AMBARI_USERNAME`. Use the CLI's interactive password prompt; do
not put a password in a command, repository file or shell trace. The CLI accepts
a Server base URL and normalizes `/api/v1`. Global options such as `--json`
precede the subcommand. HTTPS with validated trust is preferred; do not disable
verification to make a test pass.

Record the JSON build result and recompute archive hashes. Previous bundle
digest was `a917dbdaafdf483d7423e03ee2b6192ae0926bc4895a7e82384e9c5b3b7b3f3e`;
a changed source/toolchain requires a newly recorded identity, not a forced
match. Build into a second directory and compare bytes/digests for AC-01.
Do not rely on old `/tmp` artifacts. The command above is local authoring,
not Server installation.

## 3. Core Acceptance, In Order

Complete AC-00 through AC-09 first. A basic path is accepted only when all ten
cases pass against a live Server/Agent. Execute independent negative/recovery
fixtures in clean environments so an unresolved scoped reservation does not hide
later failures.

For the 2026-09-22 corrective implementation, first import the entire mpackstore
bundle through `IMPORT`, verify that no package hooks or host commands execute,
then select services from `/mpack_services`. Use `/mpack_service_plans` to enable
the exact selection and exercise its new-cluster and existing-cluster handoff.
Include a multi-service package: selecting one service must not deploy its siblings.
The user requested coordination before deployment acceptance; obtain the environment
and procedure from the user before starting live cases.

| Case | Action and required observations | Baseline |
| --- | --- | --- |
| AC-00 | Establish the preconditions in section 1; record actual Server/Agent artifacts, database and platform identities. Confirm this is the implementation, not trunk/old installed binaries. | MP-16/18/25 |
| AC-01 | Build all current reference packs and bundle twice. Compare compressed-byte digests and exact bundle member index. Validate the published schema against the tooling contract. | MP-01/02/17/24 |
| AC-02 | With no clusters, read capabilities, schema, releases, bindings and operations through HTTP. Verify schema 1, STACK_VERSION scope, advertised artifact/action sets and Agent protocol. Repeat with restricted/anonymous accounts: authorization rejects without catalog/resource changes. | MP-09/16/23 |
| AC-03 | Upload and dry-run the infrastructure bundle. Compare release/binding inventory, effective snapshot, runtime metadata and control before/after. Preview must not initialize durable control or activate definitions. Immutable uploaded/candidate bytes and a retained plan are allowed. | MP-06/07/08/14 |
| AC-04 | Accept the bundle before creating a cluster. Capture the exact plan, operation and member identities; poll to SUCCEEDED. Check all three installed releases, effective snapshot, bindings, GENERIC/1.0 and both service definitions. HTTP 201/202 or a green progress bar alone is insufficient. | MP-03/04/05/06/08/14/16 |
| AC-05 | Create a fresh GENERIC cluster with NGINX only using the actual installer. Verify default version definition/repositories, host assignment, configuration and completed install request/tasks. No HDFS, ZooKeeper, stack-select or Hadoop/Java stack hooks may be introduced. | MP-18/19/23 |
| AC-06 | Configure, start, service-check, VERSION_CHECK, RELOAD and stop NGINX through existing Ambari actions. Verify exact task lineage, pinned resources and structured package/systemd/endpoint observations described in section 5. | MP-11/12/18/19 |
| AC-07 | Update Nginx definitions using a separately built new exact release, without changing extension identity initially. Preview must report the deployed cluster and require maintenance. Without maintenance reject; with explicit maintenance complete/wait as declared. Verify changed snapshot, refreshed advisors/metadata and unchanged observed host software version. | MP-05/08/10/11/18 |
| AC-08 | Attempt unbind/uninstall while the deployed service or its components remain, including when stopped: reject with exact blockers. In an explicitly disposable cluster remove its deployment through normal APIs, then unbind and uninstall the exact pack release. Check binding/definition retirement and protected host data/old resource retention. Test foundation reverse-dependency rejection before removing its consumers. | MP-04/09/13 |
| AC-09 | Repeat equivalent basic management through /mpacks as an admin, before cluster creation. Cover upload, member/target selection, impact confirmation, completion and deployment handoff. Reload while active and after lost acceptance response; verify the same operation/key, no duplicate submission. Restricted users must not access management by direct route/API. Use real backend calls, not API mocks. | MP-06/14/16/23 |

Use a pristine isolated catalog for the convenient bundle route:

```sh
ambari-mpack --json install /tmp/mpack-acceptance-build/infrastructure.bundle.tar.gz --stack GENERIC/1.0 --dry-run
ambari-mpack --json install /tmp/mpack-acceptance-build/infrastructure.bundle.tar.gz --stack GENERIC/1.0 --no-wait
```

`--dry-run` uploads and creates a plan; it does not apply it. A later ordinary
CLI install creates a new preview, so use direct HTTP acceptance of the retained
plan when testing stale-plan/idempotency behavior. Extract IDs from validated
JSON results, not progress text. Query the exact operation with:

```sh
ambari-mpack --json operations show "$OPERATION_ID"
ambari-mpack --json operations members "$OPERATION_ID"
ambari-mpack --json list
```

For a separately reset catalog, also exercise foundation first, then a stored
extension and explicit bind. These commands are an alternative scenario, not
instructions to reinstall the same releases after the bundle case:

```sh
ambari-mpack --json install /tmp/mpack-acceptance-build/generic-base-1.0.0.0.tar.gz
ambari-mpack --json install /tmp/mpack-acceptance-build/nginx-1.0.0.0.tar.gz --store-only
ambari-mpack --json bind nginx/1.0.0.0 --stack GENERIC/1.0
```

For AC-07, build an isolated copy of the Nginx source as release `1.0.0.1`,
retaining extension `NGINX/1.0` and its exact foundation dependency. Change a
harmless script/resource to distinguish generations; preserve license headers.
Never edit the original release in place. Use the old exact identity:

```sh
ambari-mpack --json update nginx/1.0.0.0 --file /tmp/mpack-acceptance-update/nginx-1.0.0.1.tar.gz --stack GENERIC/1.0 --maintenance --dry-run
ambari-mpack --json update nginx/1.0.0.0 --file /tmp/mpack-acceptance-update/nginx-1.0.0.1.tar.gz --stack GENERIC/1.0 --maintenance --no-wait
```

Maintenance is explicit permission to change definitions, not automatic host
software upgrade or an invented service stop/start workflow. After an update
use the new exact release identity for subsequent operations. Old release bytes
remain retained; inventory registration and active snapshot membership differ.
For AC-08 use `unbind RELEASE --stack GENERIC/1.0` before `uninstall RELEASE`.

## 4. Failure And Compatibility Acceptance

| Case | Procedure and pass conditions | Baseline |
| --- | --- | --- |
| AC-10 | Upload bad digests, traversal/outside links, duplicate members, unsupported entries and over-limit archives; send duplicate/unknown JSON fields and bad schema versions. Match documented HTTP/error codes; no active metadata/control change. Exercise all four artifact formats with valid isolated fixtures, not only parsing their type names. | MP-01/02/03/07 |
| AC-11 | Missing, cyclic, ambiguous and changed exact dependencies; duplicate provider contributions; collision with built-in definitions. Reject exact conflicts. Keep ordinary inheritance valid. Do not treat rejected built-in replacement as a supported replacement workflow. | MP-03/04/07 |
| AC-12 | Accept one retained plan/key, deliberately lose its response, then replay the same plan/key. Require one operation and generation. Reuse that key for a different plan: IDEMPOTENCY_CONFLICT. Expired/stale catalog or changed distribution snapshot: STALE_PLAN with no new acceptance. | MP-06/07/16/17 |
| AC-13 | Disconnect/terminate CLI after acceptance; Server must continue. Resume from the same local submission UUID. Restart the Server while accepted work is unfinished; match operation/plan/generation/snapshot and finish or expose explicit unresolved state. No duplicate effects or guessed success. | MP-06/08/15/17 |
| AC-14 | Isolated hooks with valid receipts, known FAILED/NOT_APPLIED results, missing/foreign/malformed receipts and interrupted UNKNOWN effects. Verify retained attempt history, safe explicit retry and no replay of APPLIED effects. Unknown effects stay RECOVERY_REQUIRED until a matching authoritative receipt is reconciled; cancel must reject applied/uncertain external effects. | MP-06/14/15 |
| AC-15 | Fault injection at registration, runtime publication, effective DB commit, announcement and later hooks; interrupt/restart at each boundary. Compare durable control/operation/link records, actually loaded snapshot and resource hashes. Desired candidate is not effective until verified commit. Already effective publication remains visible after a later hook failure. Management/recovery stays reachable; no mixed generation or blind compensation. | MP-06/07/08/10/15 |
| AC-16 | Shared-definition consumers appear in impact while another service in the same stack and a separate cluster remain usable. Affected queued/holding/running tasks block publication with exact identities; task control remains available. Race task submission/retry and configuration/topology mutation with reservation/publication. Verify unsupported category/cardinality/version-advertisement/component-set changes reject without suggesting an unimplemented restart migration. | MP-05/08/10/11 |
| AC-17 | Persist a task with snapshot A, publish an unrelated definition change B, then verify scheduling/retry keeps the original A resources; fresh tasks use B. A change to the task's own definition still waits for it. Compare Server archive digests, Agent A/B paths, background/status-check and selected/disabled hooks. Missing/foreign pinned references or unsupported Agent protocol must reject, never substitute B. | MP-11/12/18 |
| AC-18 | Two independent PostgreSQL instances: backup, protected transfer, isolated restore and failure cases in section 6. Match task/backup/source/target identities and typed data observations; no source overwrite or silent cleanup. | MP-20 |
| AC-19 | Export/build HDFS from the fixed commit into isolated definition roots. Compare complete resources, then actual loading, Agent delivery and supported install/configuration/security/custom-command/HA/JournalNode/Federation/RBF/upgrade paths against equivalent built-in service. Never overlay conflicting built-in providers to avoid a failure. CLIENT components retain existing non-daemon restrictions. | MP-01/03/07/11/21/22 |
| AC-20 | Fresh initialization and upgrade from a database without mpack_record, for each supported backend. Inspect actual table/index/JPA/CAS behavior and interrupted acceptance/publication. Separately restore a same-stopped-state database plus complete managed store in an isolated environment; verify snapshot/operation lineage and unresolved external effects. | MP-06/08/25; offline procedure only for MP-26 |

Client `operations resume` takes a **submission UUID**, not an operation UUID.
The checkpoint is under `${XDG_STATE_HOME:-$HOME/.local/state}/ambari-mpack/submissions`.
Record its credential-free plan/key/operation association before simulating a
lost response. Server address and authenticated user must remain the same.
`recover`, `retry`, `cancel`, `show` and `members` take an operation UUID:

```sh
ambari-mpack --json operations resume "$SUBMISSION_ID"
ambari-mpack --json operations recover "$OPERATION_ID"
ambari-mpack --json operations retry "$OPERATION_ID"
ambari-mpack --json operations cancel "$OPERATION_ID"
```

Execute recovery commands only for the case's eligible state, not as an
unconditional sequence. CLI waiting ends at WAITING_RESTART or
RECOVERY_REQUIRED as well as terminal phases; process exit 0 is not proof of
SUCCEEDED. Query and validate the authoritative operation again.

Hook receipts use schema 1, `operation_id`, `plan_digest`, `archive_digest`,
`phase`, `attempt`, `state`, `effect_state` and `observations`. Runner-provided
inputs are `AMBARI_MPACK_OPERATION_ID`, `AMBARI_MPACK_PLAN_DIGEST`,
`AMBARI_MPACK_ARCHIVE_DIGEST`, `AMBARI_MPACK_HOOK_PHASE`,
`AMBARI_MPACK_HOOK_ATTEMPT` and `AMBARI_MPACK_RECEIPT_PATH`. Controlled fixtures
must write atomic receipts backed by actual owned-resource observations;
stdout/exit-only fixtures cannot establish a successful business effect.
Retried hooks require manifest `idempotent: true`, FAILED and NOT_APPLIED;
retain successful bundle members and earlier attempts. Do not hand-edit
lifecycle rows or fabricate an APPLIED receipt to release a barrier.

## 5. Nginx And Host Resource Observations

NGINX_SERVER is a MASTER with cardinality 1 and no advertised distribution
version. `nginx-env/listen_port` defaults to 8080; choose a free port if the
Server/another service shares the host. Configuration owns
`/etc/nginx/conf.d/ambari-managed.conf`. The unit is `nginx.service`.

For host task results, parse `Tasks/structured_out` according to its actual
JSON encoding and validate `software_observation`. Its integer schema 1,
`definition_snapshot`, `execution_id`, `task_id` and `action` must match the
captured execution, with `outcome: APPLIED` and required `observations`.
Missing or foreign structured output fails the action acceptance even if
the task/process reports completion. Background executions may have no task
ID; still require the exact captured execution UUID and snapshot.

APT observations contain actual package name/version/architecture, not the
GENERIC 1.0, definition 1.0 or mpack 1.0.0.0 version. Systemd start/stop/reload
observations require the matching JobRemoved job identity/unit/result and actual
ActiveState. Health additionally requires HTTP status 200 and exact endpoint
identity `ambari-nginx-v1` at `/ambari-health` on the configured port. An HTTP
200 from the frontend or a different Nginx location is not this health result.

Inspect command/resource lineage before and after update. Immutable URLs and
cache paths contain `mpacks/<snapshot-sha>/...`; resource/reference/digest maps
must belong to that same snapshot. A versioned archive's digest must match the
compressed archive bytes. If the required capture is not available, AC-17 is
BLOCKED, not passed because a versioned-looking path appeared in a log.

## 6. PostgreSQL Recovery Reference

POSTGRESQL_SERVER is a MASTER with cardinality 1, OS-managed PostgreSQL 14 and
no advertised distribution version. Configuration is `postgresql-env`:

| Property | Meaning/default |
| --- | --- |
| `database` | Exact source database; default `postgres` |
| `unit` | `postgresql@14-main.service` |
| `backup_directory` | `/var/lib/postgresql/ambari-backups`, contained under `/var/lib/postgresql` |
| `backup_id` | Explicit backup UUID for RESTORE; initially empty |
| `restore_database` | Explicit new target database; initially empty |

Custom commands are BACKUP, RESTORE and VERSION_CHECK. The reference reads
these inputs from versioned service configuration, not arbitrary commandParams.
Record config tags/revisions when changing recovery inputs. The service uses
the local `/var/run/postgresql` socket as OS user postgres; two database names
in the same instance do not provide the required isolation. Two disposable
hosts/clusters may supply the independent instances; same-named independent
service instances inside one cluster are not a delivered identity model.

1. On source instance A, create an explicitly owned test database and seed known
   data through a typed PostgreSQL driver. Record system_identifier, database,
   server_version_num, table row counts/digests and service config revision.
2. Execute BACKUP through Ambari. Validate software_observation lineage and
   `observations.database.backup`: backup_id equals the execution UUID; source
   instance/database/table observations match the exported snapshot. Descriptor
   and custom-format `.dump` payload must match payload_digest. Store both.
3. Transfer the protected descriptor/payload to B's contained backup directory,
   preserving exact identities/digest and appropriate postgres permissions.
   Set B's `database` to the descriptor's original source name for selection,
   `backup_id` to its UUID, and a fresh non-system `restore_database`. Ensure
   B's actual system_identifier differs from A's and its service unit is active.
4. Execute RESTORE through Ambari. Require verification RESTORED_TABLE_DIGESTS,
   matching backup/payload/source, B's target identity, same server major and
   exact per-table counts/digests. Independently observe the restored target
   with a typed driver; confirm A's source data was not changed by restoration.
5. Test source/system/existing targets, same-instance restore, mismatched
   backup/source/digest, missing descriptor/payload, interrupted backup/restore
   and explicit retry. Failed targets/payloads must remain visible and must not
   be blindly overwritten. A new BACKUP UUID is a new operation, not completion
   of an interrupted prior backup. Any target removal/retry must be explicitly
   authorized and confined to the fixture's owned data.

The configured source database may not exist on B. Do not interpret a health
failure for that source name as proof about the restored target; observe each
exact database separately. Existing source and target observations are distinct.
No automatic failover, topology orchestration or general backup framework is
under acceptance here.

## 7. Completion And Limits

Record separate decisions for basic acceptance (AC-00 through AC-09) and full
in-scope acceptance (AC-00 through AC-20). Any FAIL/NOT_RUN/BLOCKED case prevents
claiming that decision passed. Partial HDFS/non-daemon coverage stays PARTIAL;
successful rejection is not positive capability parity. For AC-20 retain a
per-database result rather than passing all backends from one successful test.

MP-26 automatic cleanup, aggregate quota and automated/online coordinated
backup remain deferred by scope, not secretly passed. Retain Server/Agent old
resources; monitor disk. Legacy filesystem-only backup does not prove database
and managed-store consistency. Offline recovery must use the same stopped state
and cannot establish rollback of host software, business data or unknown hooks.

Update acceptance-results.md with exact evidence and delivery-baseline.md only
after execution. Rerun focused regressions for any demonstrated defect fix and
the affected final builds; keep runtime and unit-test evidence separate. Do not
create extra feature frameworks. Commit work remains uncompleted until actual
JIRA-keyed topic commits exist; do not stage original-workspace or generated
files as part of acceptance.
