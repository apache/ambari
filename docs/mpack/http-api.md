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

# Mpack V1 HTTP Contract

For ordered live cases and evidence requirements use the
[acceptance runbook](acceptance-runbook.md). Record results in the
[acceptance ledger](acceptance-results.md), not by treating this contract as
proof that deployed endpoints passed.

All paths below are relative to `/api/v1`. All endpoints require an
authenticated Ambari administrator and `AMBARI.MANAGE_STACK_VERSIONS`.
They are global and do not require a cluster. Responses are JSON with integer
`schema_version: 1`; collection responses contain `items`. Authentication
failures retain the existing Ambari authentication contract. Authenticated users
without the required administrator authorization receive HTTP 403 with a
schema-1 `FORBIDDEN` error envelope; denial does not initialize catalog state.

## Resources

| Method | Path | Result |
| --- | --- | --- |
| GET | `/mpack_capabilities` | 200: manifest versions, artifact types, operations, shared binding scope, target stacks, required Agent protocol |
| GET | `/mpack_capabilities/manifest_schema` | 200: `manifest_schema`, the same JSON schema used by authoring tools |
| POST | `/mpack_uploads` | 201: archive digest and inspected pack metadata, or bundle with exact `members` |
| POST | `/mpack_plans` | 201: validated immutable plan |
| GET | `/mpack_services` | 200: imported service catalog, unavailable package reasons and exact cluster destinations |
| POST | `/mpack_service_plans` | 201: plan for exact selected catalog services and a compatible destination |
| GET | `/mpack_plans/{id}` | 200: retained plan |
| POST | `/mpack_operations` | 202: durable accepted operation; Location identifies its query endpoint |
| GET | `/mpack_operations` | 200: operation collection |
| GET | `/mpack_operations/{id}` | 200: authoritative operation and hook observations |
| GET | `/mpack_operations/{id}/members` | 200: derived member results with exact `parent_id` and archive identities |
| GET | `/mpack_operations/{id}/deployment` | 200: completed service selection, verified definition inputs and destination for wizard handoff |
| POST | `/mpack_operations/{id}/recover` | 202: receipt reconciliation accepted; unknown hook effects are not rerun |
| POST | `/mpack_operations/{id}/retry` | 202: explicit retry of eligible idempotent FAILED/NOT_APPLIED hooks |
| POST | `/mpack_operations/{id}/cancel` | 200: cancelled operation, only when no applied or uncertain effects prevent cancellation |
| GET | `/mpacks` | 200: release collection, including retained retired releases with `installed: false` |
| GET | `/mpacks/{name}/versions/{version}` | 200: exact retained release |
| GET | `/mpacks/{name}/versions/{version}/usages` | 200: exact dependency, binding, operation, cluster and service references |
| GET | `/mpack_bindings` | 200: effective bindings, control revision and snapshot; unmanaged distribution links before first acceptance |

Uploads use `Content-Type: application/octet-stream` and tar.gz bytes.
`X-Content-SHA256` is optional; if supplied it must match the lowercase SHA-256
of the compressed bytes. Default limits are 256 MiB compressed, 1 GiB expanded,
and 100000 entries. Bundle members are validated as independent archives.
Upload/preview does not activate definitions or initialize the durable control
record. Preparing immutable candidate resources is permitted during preview.

## Plans And Acceptance

Plan requests use `Content-Type: application/json` and are limited to 1 MiB.
All mutation fields are required; unknown fields and duplicate JSON keys fail:

```json
{
  "schema_version": 1,
  "action": "INSTALL",
  "archive_digests": ["0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"],
  "release_ids": [],
  "bindings": [],
  "activate": true,
  "maintenance": false
}
```

Actions are `IMPORT`, `ENABLE`, `INSTALL`, `UPDATE`, `BIND`, `UNBIND`, and `UNINSTALL`.
`IMPORT` requires archives, no release IDs/bindings, and `activate: false`.
It registers the complete bundle without executing hooks or enabling its definitions.
`ENABLE` requires imported release IDs and activation; bindings are optional for
packages which provide stack/addon definitions directly. Install hooks execute for
newly enabled releases in dependency order. The UI uses service-selection plans
to derive these package and binding inputs rather than asking users to construct them.
An initial IMPORT does not switch the distribution runtime or require its existing
tasks to adopt the managed resource protocol. Only definition activation publishes
the managed runtime view. Inactive-package removal is also catalog-only.
`INSTALL` requires archives and no old release IDs. `UPDATE` requires archives,
replaced release IDs, and activation. Binding actions require installed release
IDs and exact targets, no new archives, and activation. `UNINSTALL` requires
exact release IDs without archives or bindings. A binding has `stack_name`,
`stack_version`, `extension_name`, and `extension_version`. A release ID is
`<manifest-name>/<exact-manifest-version>`, not a repository or host version.

A plan includes `id`, `digest`, `catalog_revision`, `expires_at`, the complete
`mutation`, `previous_snapshot`, `candidate_snapshot`, exact proposed releases,
active release IDs, affected clusters, and maintenance/restart requirements.
It also includes `affected_definitions` (exact stack/version/service and configuration
types), `initializing_releases`, `retiring_releases`, and nullable `deployment`. These are draft schema-1
contract additions; previously stored incomplete draft plans must be recreated.
Plans expire after one hour. Changed catalog revisions, distribution resources,
or impact preconditions require a fresh preview.

Acceptance requires `Idempotency-Key`, 1 through 200 printable non-space ASCII
characters, and exactly this JSON body:

```json
{"schema_version": 1, "plan_id": "11111111-1111-4111-8111-111111111111"}
```

The key is scoped to the authenticated user and exact plan identity/digest.
Replaying the same key and plan returns the original operation, even after
completion. A changed request with the same key returns `IDEMPOTENCY_CONFLICT`.
Persist the plan ID/digest and key before submission; after a lost response,
resubmit them rather than constructing another operation. No client credentials
belong in submission checkpoints. A 202 response proves acceptance, not success.

## Whole-Store Import And Service Selection

Upload the mpackstore bundle, then submit an `IMPORT` plan containing every member
archive digest. A successful import makes its services discoverable; it does not
deploy software, select all services, or bind every package to one stack.

`GET /mpack_services` returns `items`, `unavailable`, and `destinations`. Each item
has an exact `id`, package release/digest, service name/display/version, compatible
stack/version, required services, dependency release IDs, bindings, `enabled` and
`client_only`. The catalog is derived through the existing Java resolver. A package
may yield multiple services or no runnable service. Different providers/contexts
retain different identities. Unavailable packages remain visible with a code and reason.

Service-plan input has exactly `schema_version: 1`, `service_ids` (catalog IDs),
`cluster_id` (a numeric existing cluster ID, or null for a new environment), and
`maintenance` (boolean). Selected services must share a compatible environment.
The resulting `deployment` records `stack_name`, `stack_version`, `cluster_id`,
`service_names`, and the exact `service_ids`. Verify that it matches the selection.

After `SUCCEEDED`, the deployment endpoint returns `operation_id`, `plan_id`,
`effective_snapshot`, `deployment` and nullable `cluster_name`. It rechecks the
selected definitions against the current effective resources. Unrelated snapshot
changes do not invalidate the handoff; changed selected definitions do. The UI
uses this record for new-cluster or Add Service navigation and refresh recovery.

## Progress, Recovery And Limits

Operation phases are `ACCEPTED`, `PREPARING`, `WAITING_MAINTENANCE`,
`WAITING_RESTART`, `PUBLISHING`, `SUCCEEDED`, `FAILED`, `CANCELLING`, `CANCELLED`, and
`RECOVERY_REQUIRED`. Match `id`, `plan_id`, `plan_digest`, and `generation` when
consuming results. Only `SUCCEEDED` establishes completion; its effective
snapshot and required hook observations must also validate. `RECOVERY_REQUIRED`
is unresolved, not successful. `WAITING_RESTART` requires a real Server restart.

Hooks carry schema, operation, plan, archive, phase and attempt identities,
`state`, `effect_state`, and structured `observations`. Completed effects are
not replayed; retries retain earlier attempts. Recovery/cancel/retry endpoints
are server-validated commands, not promises that every operation is eligible.
Reconciliation persists valid FAILED/NOT_APPLIED observations too, making an
interrupted attempt eligible for its supported retry/cancel policy. Online hook
execution requires manifest `scope: "DEFINITIONS"`; omitted scope means `SERVER`
and is rejected for online execution, while import remains permitted. This is an
author-declared effect boundary, not a subprocess security sandbox.

Cancellation records `CANCELLING` and its requesting administrator before changing
the retained runtime view. A failed notification or restart resumes cancellation,
not the original install/update. Ownership is cleared only after the retained view
and required notification have completed. Removing an inactive imported release
does not run uninstall hooks for definitions it does not currently own.

Package publication has one coordinator. Durable reservations block only affected
definition consumers and relevant configuration/topology mutations. Existing
blocking tasks are identified by cluster/request/task IDs and authoritative status;
unrelated tasks continue, and original-task control/recovery remains available.
Candidate construction, verification and hook subprocesses run outside the
exclusive publication lock. The final view switch/commit is protected, and failed
publication restores the previous view or leaves affected readers explicitly gated.
Affected active stack upgrades and unsupported in-use component-model changes
are rejected. A restart is not a substitute for a component migration.
Shared stack/version definitions apply to every cluster using that context.
An mpack update does not upgrade host software; uninstall does not delete host
software, data, or retained recovery bytes. Store/cache cleanup, aggregate disk
quota, live coordinated backup, and generated clients are not delivered.

## Errors

Lifecycle errors contain `schema_version: 1` and `error` with exact `code`,
diagnostic `message`, and structured `details`. Branch on codes, not messages.

| HTTP status | Codes |
| --- | --- |
| 404 | `NOT_FOUND` |
| 413 | `UPLOAD_LIMIT` |
| 409 | `STALE_PLAN`, `IDEMPOTENCY_CONFLICT`, `OPERATION_CONFLICT`, `RELEASE_CONFLICT`, `RESOURCE_CONFLICT`, `RESOURCE_IN_USE`, `MAINTENANCE_REQUIRED`, `RECOVERY_REQUIRED` |
| 503 | `STORAGE_FAILURE` |
| 400 | `INVALID_MANIFEST`, `UNSUPPORTED_SCHEMA`, `INVALID_ARCHIVE`, `DIGEST_MISMATCH`, `DEPENDENCY_MISSING`, `DEPENDENCY_AMBIGUOUS`, `DEPENDENCY_CYCLE`, `VERSION_INCOMPATIBLE`, `INVALID_TARGET`, `INVALID_RECEIPT`, `UNSUPPORTED_OPERATION` |

Authoritative definitions are MpackLifecycleApiService, MpackLifecycleState,
MpackExceptionMapper, and the published manifest schema. The delivery baseline
records which compilation, contract, and deployment checks were actually run.
