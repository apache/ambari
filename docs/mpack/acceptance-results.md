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

Live acceptance started on 2026-09-24 using Rocky 8 aarch64 and resumed on
2026-09-26. See [the local conclusions](conclusion-2026-09-26.md) for corrections,
actual runtime identities and the remaining limits. The earlier paused state is
retained in [the stage checkpoint](checkpoint-2026-09-24.md).
Ubuntu is excluded. Subcases below distinguish actual passes from unfinished
matrix coverage. Prior local evidence remains in [delivery-baseline.md](delivery-baseline.md).

Allowed statuses: NOT_RUN, PASS, FAIL, BLOCKED, SKIPPED_OUT_OF_SCOPE. A missing
environment is BLOCKED; an observed contract violation is FAIL. Out-of-scope
exclusions require a stated scope reference. Do not mark an in-scope case
SKIPPED_OUT_OF_SCOPE merely because it is difficult or lacks infrastructure.

## Run Identity

| Field | Value |
| --- | --- |
| Run UUID/date/operator | Started 2026-09-24, resumed 2026-09-26; original deploy run `776dcb2d-2101-40bb-bce4-b843475a6e8d` reused |
| Core worktree/branch/HEAD and dirty-source identity | `AMBARI-26663`; original RPM input `20c33adf1b4d25647bddf5f4e23144d52cdd0345`; correction code through `737829e865`; exact replacements in conclusions |
| Reference repository source identity | Local commit `2e73780`; generic-base 1.0.0.3, nginx 1.0.0.4, postgresql 1.0.0.5, Kyuubi 1.0.0.7; final bundle identity in conclusions |
| Server/Agent deployment artifact identities | `rocky8-arm64-mpack-v1-20260924` manifest; original FileCache fix plus final acceptance-v3 JAR and React assets recorded in conclusions |
| Server/host OS, architecture, JDK and Python ABI | Rocky 8 aarch64; JDK 17; Agent Python 3.9; OS observers use platform Python 3.6 |
| Database backend/version and initialization/upgrade source | Fresh PostgreSQL deployment; upgrade matrix not run |
| Isolated cluster/host IDs | `mpack_nginx_20260924`, `mpack_kyuubi`, `mpack_fresh_20260926`; worker1 through worker5.bigtop.apache.org |
| Evidence directory or access-controlled artifact location | `/Users/jialiang/.local/share/ambari-mpack-acceptance/20260924` and `20260926` |
| Actual API base URL and frontend used, without credentials | `http://127.0.0.1:18080/api/v1`; `/latest/` |

## Case Ledger

Split a case into separately identified subcases when its branches have
different results. Link sanitized evidence; record exact request/task,
operation/plan/generation/snapshot and release/archive identities as applicable.
Do not paste secret-bearing configs, headers or checkpoints with credentials.

| Case | Description | Status | Evidence / blocker / defect |
| --- | --- | --- | --- |
| AC-00 | Source and isolated deployment preconditions | PASS | Source snapshot, completed RPM manifest, exact installed RPM identities and four Agent protocol observations |
| AC-01 | Reproducible tooling, packs, bundle and schema | PASS | Final `store-k` and `store-l` are byte-identical; published schema matches tooling; `closeout.json` |
| AC-02-A | Pre-cluster administrator capabilities/schema/catalog | PASS | Initial HTTP serialization defect fixed; real Jersey tests and live capability response passed |
| AC-02-B | Restricted and anonymous authorization | PASS | Fourteen live API checks and restricted browser route; unmapped HTTP 500 fixed to 403/FORBIDDEN |
| AC-03 | Side-effect-free upload/preview and virtual baseline | PASS | `evidence-v2/preview-before.json` and `preview-after.json` match |
| AC-04-A | Whole-store import and service discovery | PASS | Four releases registered without hooks or activation; `evidence-v2` and `final-store` |
| AC-04-B | Exact ENABLE selection and deployment handoff | PASS | Nginx-only handoff; Kyuubi and existing-cluster PostgreSQL selection also completed |
| AC-04-C | Alternative legacy INSTALL-compatible route | PASS | Operation `ebad8a59-b021-4b8a-8050-ddd703387b45` after normal Nginx unbind/uninstall |
| AC-05-A | First fresh GENERIC installation | FAIL | Original request 1 failed on download routing and repository configuration; evidence retained |
| AC-05-B | Corrected Nginx installation recovery | PASS | Request 9/task 54; NGINX-only cluster, actual RPM observation |
| AC-05-C | Fresh installation with final corrected Nginx release | PASS | New worker5: topology 56, actual install 59/209 and start 60/210; final Nginx archive unchanged afterward |
| AC-06 | Nginx operations and structured host observations | PASS | Version 50/203, reload 51/204, health 52/205, stop 53/206, restart 54/207; final health 100/282 and 102/284 |
| AC-07 | In-use definition update and impact/maintenance | PASS | Explicit maintenance denial, successful new releases, scoped affected definitions and unchanged software version |
| AC-08 | Stopped-service protection, unbind/uninstall and reinstallation | PASS | Exact live blockers, normal component/service removal, definition retirement, retained inventory and successful restoration; foundation removal rejected |
| AC-09-A | Real React import, selection, refresh and lost-response recovery | PASS | Real backend; recovered original operation/generation; exact NGINX handoff to installer; restricted route and mobile layout checked |
| AC-09-B | Complete browser management on an empty Server | NOT_RUN | Global route/unit coverage and pre-cluster API coverage do not replace this browser context |
| AC-10-A | Positive loading of all four artifact types | PASS | GENERIC/NGINX plus live common-service/addon inheritance fixture `cc091bbd-3829-41f2-bcb6-3296e59938df` |
| AC-10-B | Executed malformed and unsafe input cases | PASS | Digest, duplicate/unknown fields, boolean schema, traversal, outside link, FIFO, directory payload and ambiguous names; unchanged catalog |
| AC-10-C | Complete live archive size/member-limit matrix | NOT_RUN | Focused archive tests pass; complete live limit matrix remains outstanding |
| AC-11-A | Ordinary common-service inheritance | PASS | Live addon inherited the declared CLIENT component and was retired normally |
| AC-11-B | Complete dependency/provider/conflict matrix | NOT_RUN | Focused Java coverage only for the remaining branches |
| AC-12-A | Idempotent acceptance and lost-response replay | PASS | Same operation/plan/generation after actual browser transport loss and repeated lifecycle submissions |
| AC-12-B | Complete stale/expiry/distribution-change matrix | NOT_RUN | Not promoted from replay and focused-test evidence |
| AC-13-A | Completed history across Server restarts | PASS | Set-order digest defect fixed; original plans and all 30 completed operations query successfully after restart |
| AC-13-B | Active-operation Server crash recovery | NOT_RUN | Completed-history recovery is not crash injection |
| AC-14-A | Known no-effect hook failures and explicit retry | PASS | `1aed779e-6d4b-427b-8aed-b4e8ad744096`; retained attempts, same generation and eventual APPLIED result |
| AC-14-B | Unknown effects, cancellation and full bundle recovery matrix | NOT_RUN | Remaining live recovery paths unexecuted |
| AC-15 | Publication boundary failures and actual-state recovery | NOT_RUN | Pending |
| AC-16-A | Scoped impact and unrelated ordinary progress | PASS | Nginx config change rejected with exact scope/operation; same-cluster PostgreSQL config persisted; other-cluster Kyuubi task 75/257 completed during RUNNING hook |
| AC-16-B | Task-control races and complete model-transition matrix | NOT_RUN | Focused tests do not establish the remaining live races |
| AC-17 | Old/new task, Agent, background and hook resource pinning | NOT_RUN | Pending |
| AC-18-A | PostgreSQL install/start/SQL health | PASS | Requests 25/28/36, tasks 99/112/120; database instance identity and version observed |
| AC-18-B | PostgreSQL isolated backup/restore | PASS | Final backup 76/258 and restore 77/259; distinct instance IDs and independently verified 100-row data set |
| AC-18-C | PostgreSQL unsafe and corrupt recovery inputs | PASS | Eight live failure cases; no target database created for invalid schema, execution identity or payload digest |
| AC-18-D | Interrupted backup/restore and explicit recovery | NOT_RUN | Successful recovery and input rejection do not establish interruption handling |
| AC-19-A | HDFS fixed-commit export/build | PASS | Source `f00356217f`; archive `623c6a17a7d2700b79eb46a1663f0fa002c21f3884eb2402c10e93aaff1a3b3e` |
| AC-19-B | Advanced HDFS parity and live CLIENT operation restrictions | NOT_RUN | Advanced isolated deployment/HA/Federation/RBF/security/upgrade matrix remains unfinished |
| AC-20 | Supported database initialization/upgrades and offline recovery | NOT_RUN | Pending |
| EX-01 | Automatic GC/aggregate quota/online backup subsystems | SKIPPED_OUT_OF_SCOPE | Design section 1.4; MP-26 remains deferred |
| KY-01 | Official Kyuubi binary, install/start/stop | PASS | Apache SHA-512 verified; RPM 1.9.4-2.official.el8; 660 payload entries match; install 88/270, start 89/271, final stop 95/277 and restart 96/278 |
| KY-02 | Real JDBC/Spark SQL | PASS | Official binary final request 104/task 286; sum 45, count 10, matching session/operation/engine/execution identity; local[2] only |
| KY-03 | Kyuubi Components summary | PASS | Authenticated browser renders declared KYUUBI_SERVER component |

Earlier failures are retained as historical evidence. Kyuubi SQL, normal SIGTERM
handling and post-stop observation replacement were corrected through new pack
releases. The final five health requests completed with exact task/execution and
snapshot identity. This does not promote the remaining crash, race, recovery,
HDFS or database matrices to PASS. TIMELINE_READER's embedded HBase start failure
is retained in the BIGTOP deployment evidence and was not repaired by this work.

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
| Executed 2026-09-26 Rocky operational paths | PASS | Final health, official Kyuubi, scoped writes, browser recovery and isolated database recovery passed |
| Basic live acceptance, AC-00 through AC-09 | FAIL | Empty-Server browser context remains unexecuted; historical failed attempts remain visible |
| Full in-scope acceptance, AC-00 through AC-20 | FAIL | Crash/race, interrupted recovery, advanced parity and database upgrade matrices remain unfinished |
| Topic commits | PASS | Local AMBARI-26663 correction commits in core and store; deploy unchanged this session; no PR or push |
| Baseline reconciled with this run | PASS | This ledger and the dated conclusions retain the executed results and remaining limits |

Do not call source closeout, focused-test success or frontend HTML HTTP 200
full delivery acceptance. Explain remaining FAIL/BLOCKED/NOT_RUN cases and
explicit exclusions in the final acceptance report.
