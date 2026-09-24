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

# Mpack V1 Acceptance Results

Live acceptance started on 2026-09-24 using Rocky 8 aarch64. The user requested
a remote checkpoint and a pause; see [the stage checkpoint](checkpoint-2026-09-24.md).
Ubuntu is excluded. Subcases below distinguish actual passes from unfinished
matrix coverage. Prior local evidence remains in [delivery-baseline.md](delivery-baseline.md).

Allowed statuses: NOT_RUN, PASS, FAIL, BLOCKED, SKIPPED_OUT_OF_SCOPE. A missing
environment is BLOCKED; an observed contract violation is FAIL. Out-of-scope
exclusions require a stated scope reference. Do not mark an in-scope case
SKIPPED_OUT_OF_SCOPE merely because it is difficult or lacks infrastructure.

## Run Identity

| Field | Value |
| --- | --- |
| Run UUID/date/operator | 2026-09-24; deploy run `776dcb2d-2101-40bb-bce4-b843475a6e8d` |
| Core worktree/branch/HEAD and dirty-source identity | `AMBARI-26663`; build input `20c33adf1b4d25647bddf5f4e23144d52cdd0345`; incremental replacements in checkpoint |
| Reference repository source identity | generic-base/nginx/postgresql 1.0.0.3; Kyuubi 1.0.0.2; `store-f` bundle SHA-256 recorded in checkpoint |
| Server/Agent deployment artifact identities | `rocky8-arm64-mpack-v1-20260924` manifest plus recorded JAR, FileCache and React replacements |
| Server/host OS, architecture, JDK and Python ABI | Rocky 8 aarch64; JDK 17; Agent Python 3.9; OS observers use platform Python 3.6 |
| Database backend/version and initialization/upgrade source | Fresh PostgreSQL deployment; upgrade matrix not run |
| Isolated cluster/host IDs | `mpack_nginx_20260924`, `mpack_kyuubi`; worker1 through worker4.bigtop.apache.org |
| Evidence directory or access-controlled artifact location | `/Users/jialiang/.local/share/ambari-mpack-acceptance/20260924` |
| Actual API base URL and frontend used, without credentials | `http://127.0.0.1:18080/api/v1`; `/latest/` |

## Case Ledger

Split a case into separately identified subcases when its branches have
different results. Link sanitized evidence; record exact request/task,
operation/plan/generation/snapshot and release/archive identities as applicable.
Do not paste secret-bearing configs, headers or checkpoints with credentials.

| Case | Description | Status | Evidence / blocker / defect |
| --- | --- | --- | --- |
| AC-00 | Source and isolated deployment preconditions | PASS | Source snapshot, completed RPM manifest, exact installed RPM identities and four Agent protocol observations |
| AC-01 | Reproducible tooling, packs, bundle and schema | NOT_RUN | Pending |
| AC-02-A | Pre-cluster administrator capabilities/schema/catalog | PASS | Initial HTTP serialization defect fixed; real Jersey tests and live capability response passed |
| AC-02-B | Restricted and anonymous authorization | NOT_RUN | Pending |
| AC-03 | Side-effect-free upload/preview and virtual baseline | PASS | `evidence-v2/preview-before.json` and `preview-after.json` match |
| AC-04-A | Whole-store import and service discovery | PASS | Four releases registered without hooks or activation; `evidence-v2` and `final-store` |
| AC-04-B | Exact ENABLE selection and deployment handoff | PASS | Nginx-only handoff; Kyuubi and existing-cluster PostgreSQL selection also completed |
| AC-04-C | Alternative legacy INSTALL-compatible route | NOT_RUN | Pending |
| AC-05-A | First fresh GENERIC installation | FAIL | Original request 1 failed on download routing and repository configuration; evidence retained |
| AC-05-B | Corrected Nginx installation recovery | PASS | Request 9/task 54; NGINX-only cluster, actual RPM observation |
| AC-05-C | Fresh installation with final corrected reference release | NOT_RUN | Recovery is not a substitute for this clean rerun |
| AC-06 | Nginx operations and structured host observations | NOT_RUN | Pending |
| AC-07 | In-use definition update and impact/maintenance | NOT_RUN | Pending |
| AC-08 | Exact blockers, unbind/uninstall and data/resource retention | NOT_RUN | Pending |
| AC-09 | Real React workflow, refresh and lost-response recovery | NOT_RUN | Pending |
| AC-10 | All artifact formats and malformed/unsafe input rejection | NOT_RUN | Pending |
| AC-11 | Exact dependency/provider/conflict and inheritance behavior | NOT_RUN | Pending |
| AC-12 | Idempotency, stale/expired plans and distribution changes | NOT_RUN | Pending |
| AC-13 | Client loss, durable Server progress and restart recovery | NOT_RUN | Pending |
| AC-14 | Hooks, unknown effects, safe retry/cancel and bundle lineage | NOT_RUN | Pending |
| AC-15 | Publication boundary failures and actual-state recovery | NOT_RUN | Pending |
| AC-16 | Scoped impact, unrelated progress, task control and model transitions | NOT_RUN | Pending |
| AC-17 | Old/new task, Agent, background and hook resource pinning | NOT_RUN | Pending |
| AC-18-A | PostgreSQL install/start/SQL health | PASS | Requests 25/28/36, tasks 99/112/120; database instance identity and version observed |
| AC-18-B | PostgreSQL isolated backup/restore and failure recovery | NOT_RUN | Pending |
| AC-19 | HDFS capability parity and existing CLIENT restrictions | NOT_RUN | Pending |
| AC-20 | Supported database initialization/upgrades and offline recovery | NOT_RUN | Pending |
| EX-01 | Automatic GC/aggregate quota/online backup subsystems | SKIPPED_OUT_OF_SCOPE | Design section 1.4; MP-26 remains deferred |
| KY-01 | Kyuubi build, install and start | PASS | Completed RPM run; installation 27/106 and start 32/116 |
| KY-02 | Real JDBC/Spark SQL | FAIL | Request 35/task 119 remains failed after cache access repair; no successful SQL result |
| KY-03 | Kyuubi Components summary | FAIL | Reported blank view fixed in source and deployed assets; focused test passes, browser confirmation pending |

Additional executed subsets: Nginx start, HTTP service check and RELOAD passed;
maintenance-authorized definition update, idempotent replay and inactive release
retirement passed. These do not promote the complete AC-06/07/08/12/16/17 matrices
to PASS. The first cache mode defect prevented PostgreSQL and Kyuubi helpers from
running as their service accounts; FileCache repair resolved PostgreSQL health,
while Kyuubi SQL still requires investigation. TIMELINE_READER's embedded HBase
start failure is also retained in the BIGTOP deployment evidence.

## Detailed Evidence

For each executed case record:

- Case/subcase ID, input source/artifact digests, preconditions and exact command
  or API method/path/payload, excluding credentials.
- Authoritative before/after observations, expected result and actual result.
  Include schema/type checks, exact operation and task lineage, and resource
  hashes. Logs/screenshots supplement these observations, not replace them.
- Defect or blocker, affected baseline IDs and reproduction. If fixed, record
  changed source identity, focused regression command/result and live rerun.
- Cleanup performed on owned test resources and unresolved state retained.
  Never conceal a failed target or uncertain hook by deleting its evidence.

## Delivery Decision

| Decision | Status | Reason |
| --- | --- | --- |
| Basic live acceptance, AC-00 through AC-09 | FAIL | Partial passes; clean rerun, permissions and browser flows outstanding |
| Full in-scope acceptance, AC-00 through AC-20 | FAIL | Kyuubi SQL fails; recovery, parity and database matrix remain unfinished |
| Topic commits | PASS | AMBARI-26663 source topics committed across core, store and deploy; documentation is the final checkpoint topic |
| Baseline reconciled with this run | PASS | This ledger and the dated checkpoint record the current limits |

Do not call source closeout, focused-test success or frontend HTML HTTP 200
full delivery acceptance. Explain remaining FAIL/BLOCKED/NOT_RUN cases and
explicit exclusions in the final acceptance report.
