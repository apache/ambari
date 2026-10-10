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

# AMBARI-26663: Rocky Acceptance Conclusions, 2026-09-26

The executed Rocky operational paths pass after the corrections below. The
complete AC-00 through AC-20 acceptance matrix is **not complete**. Database
upgrade coverage, publication crash injection and advanced Hadoop parity must
not be inferred from these results. See [the case ledger](acceptance-results.md)
for the remaining cases.

The original deployment and Ambari RPMs were reused. Server JAR and React
assets were replaced incrementally. Published mpack archives and snapshots were
not edited: reference fixes were delivered as new releases through the HTTP
lifecycle. No PR or remote push was performed in this session; mpackstore stays
local. Ubuntu remains excluded by the user's scope decision.

## Local Source And Artifacts

| Item | Identity |
| --- | --- |
| Core Server corrections | `163eca9b9c`, `6be858c878` on `AMBARI-26663` |
| React corrections | `86868ba03c`, `737829e865` on `AMBARI-26663` |
| Reference repository | `/Users/jialiang/PRJS/ambari-mpacks`, local commit `2e73780` |
| Deploy repository | `2bd0138`; no source changes this session |
| Platform | Rocky 8 aarch64; existing four-node deployment plus one isolated acceptance node |
| Effective definition snapshot | `5da22f096a9cdae70279bff196ee6ff4d5062ad136c77605aeedfea489915fc8` |
| Final reference versions | generic-base `1.0.0.3`, nginx `1.0.0.4`, postgresql `1.0.0.5`, kyuubi `1.0.0.7` |
| Final bundle SHA-256 | `d383845ad99818a449e502e20992465cb520edd722c67906f789edc65ef175bb` |
| Incremental Server JAR SHA-256 | `68e42ed771534a7a91ba43f4e1eece681f0210254daa0a472e0f7c02920aace8` |
| React entry asset | `index-D0Bn1YhX.js`, SHA-256 `30b5a273b82e8b8cfa314145c5d92fbcd085485e682ce97e8cf3d19c475b9361` |

Evidence and artifacts are retained under
`/Users/jialiang/.local/share/ambari-mpack-acceptance/20260926`.
The importable bundle is `store-k/mpackstore.bundle.tar.gz`; `store-l` is the
independent, byte-identical rebuild. The Server artifact is
`ambari-server-acceptance-v3.jar`. The prior deployment identities and RPM
build receipts remain in [the earlier checkpoint](checkpoint-2026-09-24.md).

Server and frontend changes use separate topic commits. Reference changes form
one atomic release commit: shared observer digest locks, consumer manifests and
immutable release indexes must agree across all affected packages.

## Corrected Defects

| Area | Defect and resulting behavior |
| --- | --- |
| Durable plans | Set iteration order changed across JVMs and invalidated stored plan digests. Scope configuration types now preserve their serialized order, including Jackson deserialization. Existing records validate without rewriting their digests or lifecycle rows. |
| Authorization | Restricted users reached an unmapped checked exception and received HTTP 500. Authenticated denial now returns HTTP 403 with `FORBIDDEN`; authorization still precedes resource access. |
| Archive handling | Library file/directory heuristics accepted special entry types. Extraction now checks explicit tar type flags, rejects directory payloads and rejects ambiguous file/directory names. |
| Systemd observations | A stopped unit could be unloaded before `GetUnit`, causing a false failure. The observer loads the exact unit and verifies its identity, load state, activity and matching job result. |
| Kyuubi SQL | `DESCRIBE SESSION` was forwarded to Spark as a table description. The helper now uses the upstream `KYUUBI DESCRIBE SESSION` command. |
| Kyuubi failures | JDBC failures lost their diagnostic identity. Bounded stage, exception-class, SQL-state and vendor-code observations now retain task/execution/snapshot lineage without exception-message contents. |
| Kyuubi stop | The foreground launcher returns 143 on SIGTERM. The systemd unit accepts that exit code while the observer still requires a successful stop job and an inactive unit. |
| Command receipts | The post-stop status callback overwrote the successful Kyuubi stop receipt with an expected failed health observation. Status probes no longer replace the primary command's structured result. |
| PostgreSQL recovery | Backup descriptors did not strictly validate schema types and original execution identity. Restore now validates the backup/task/snapshot/payload/source/table contract before creating a target database. |
| Browser recovery | The real Users API has no `user_id`, causing shared `undefined` submission keys. Recovery is now isolated by authenticated username; unowned legacy checkpoints are not silently attributed to a user. |
| Deployment handoff | A completed operation's deployment link waited behind catalog refresh. The verified handoff now loads first and checks the exact plan and effective snapshot. |
| UI presentation | Descriptions hid package release identities, and a fixed footer overlapped mobile rows. Releases remain visible alongside descriptions, and the global directory footer participates in normal layout. |
| Initial login | The expected unauthenticated session probe emitted a stale global error toast. It now uses the existing quiet API client while retaining caller error handling and authentication redirects; actual browser login passed without the toast. |

## Live Results

These observations came from the actual Server, Agents, databases, systemd and
browser. HTTP acceptance, process exit or diagnostic log text alone was not
used as proof of completion.

| Scenario | Evidence |
| --- | --- |
| Fresh GENERIC/Nginx installation | New `worker5.bigtop.apache.org`; topology request 56, actual install 59/task 209 and start 60/task 210. Only NGINX was deployed initially. |
| Nginx lifecycle | Version 50/203, reload 51/204, HTTP check 52/205, stop 53/206 and restart 54/207. Endpoint identity is `ambari-nginx-v1` on port 8088. |
| Scoped ordinary writes | Operation `1aed779e-6d4b-427b-8aed-b4e8ad744096` held a Nginx definition reservation. A changed Nginx config received 409/`MAINTENANCE_REQUIRED`; a PostgreSQL config in the same cluster persisted, and Kyuubi version request 75/task 257 completed in another cluster. |
| Hook retry | Two verified FAILED/NOT_APPLIED attempts were retained. Explicit retry applied attempt 3 with the same operation ID and generation. The temporary definition was then replaced by the official reference release and retired. |
| Removal protection and recovery | Stopped deployed Nginx still blocked removal. After normal component/service removal, unbind and uninstall succeeded; foundation removal remained blocked. Legacy INSTALL operation `ebad8a59-b021-4b8a-8050-ddd703387b45` restored the pack. Both Nginx deployments were reinstalled, configured and started. |
| Browser import and lost response | Actual POST reached the Server, its response was deliberately lost, then browser reload and reconcile recovered the same operation/generation. Example: `0a09daeb-f5b7-4283-8441-349de86f0919`. |
| Service selection | Browser selected NGINX alone; handoff `0330579a-afe8-48df-a3e8-060213ee7f96` opened the cluster installer with that exact selection. This does not claim a complete browser-driven cluster installation. |
| Browser permissions and mobile | Restricted cluster viewer was redirected away from `/mpacks`. At 390 px width there was no page overflow and the footer boundary equaled the scrolling-content boundary, with no overlay. Kyuubi's declared server component was visible. |
| HTTP permission and input rejection | Fourteen anonymous/restricted checks passed. Digest, duplicate/unknown fields, boolean schema, traversal, escaping link, FIFO, directory payload and ambiguous entry tests returned exact documented errors without changing catalog state. |
| Artifact loading | Existing stack/extension references plus common-service/addon fixture `cc091bbd-3829-41f2-bcb6-3296e59938df` exercised all four artifact types. The live resolver inherited a CLIENT component from a common service; the unused fixture was retired. |
| PostgreSQL recovery | Final backup 76/258 and restore 77/259 matched table digests. Source instance `7689102948144019413` and target `7689681910423379144` differ. Both retained 100 rows, sum 5050 and 67 non-null values. |
| PostgreSQL failure cases | Source/system/existing targets, missing backup, same-instance restore, invalid schema, foreign execution and mismatched payload digest failed. Corrupt descriptors created no target database. Interrupted recovery remains untested. |
| Final health | Nginx 100/282 and 102/284; PostgreSQL 101/283 and 103/285; official Kyuubi SQL 104/286. All five completed with matching action, task, execution and final snapshot. |

`closeout.json` records the five final checks, byte-identical final archives and
30 completed lifecycle operations, with no unresolved lifecycle reservation.
Expected negative-test task failures and earlier failures remain in history.

## Official Kyuubi Binary

The earlier source-built RPM is historical evidence. This session downloaded
the official Apache 1.9.4 binary, verified the Apache-published SHA-512, and
packaged it using the existing Rocky builder without compiling Kyuubi or Spark.
The source URL and digest are pinned in the store's `mpacks/kyuubi/source.json`.

- Archive: `upstream/apache-kyuubi-1.9.4-bin.tgz`.
- SHA-512: `57f1cf51287ffc813db0170814569f1f2f18dc4596ffae68ba75ed48f4b13c5d65631cee12d6f30f953d8fc95366f47465d74ba3ad9075a1f005259daa7cd147`.
- RPM: `official-kyuubi-rpm/RPMS/noarch/kyuubi_3_3_0-1.9.4-2.official.el8.noarch.rpm`.
- RPM SHA-256: `0329b5b20a94242beba4f8d9f770f146dfcfe15b5543dbd544949159bcbba037`.
- Fresh mpack installation on worker3: request 88/task 270; start 89/271;
  initial official SQL 90/272. All 660 installed upstream payload files/links
  matched the official archive.
- Final unit/result fixes: start 94/276, stop 95/277, restart 96/278 and SQL
  104/286. SQL returned sum 45, count 10, a matching execution UUID and real
  session/operation/engine identifiers after resource closure.

Spark remains the verified reused 3.5.8 payload. These SQL checks use `local[2]`;
they do not establish YARN integration, security integration or HA failover.

## Regression Verification

| Check | Actual result |
| --- | --- |
| Server focused Maven suites | 142 tests passed; subsequent archive-boundary correction passed all 12 `MpackArchiveStoreTest` tests |
| React focused suites | 91 tests passed; later footer and session-probe changes passed 11 and 15 affected tests respectively; final production build and browser checks passed |
| Agent FileCache/ConfigurationBuilder | 41 tests passed |
| Python authoring/HTTP contracts | 16 tests passed |
| Store tests | 20 passed locally and under Rocky 8 platform Python 3.6 |
| Official binary packaging | Existing builder, no network during packaging; verified RPM header/digest and installed payload |
| Source hygiene | Explicit file staging and `git diff --check` |

Commands used the existing acceptance venv, JDK 17 and Maven 3.9.11. The Maven
invocation followed `corrective-implementation.md` with the mpack HTTP/resource
suites added. Its exclusions remained explicit: Python-in-Maven, Checkstyle,
RAT and Swagger checks were not run. Agent tests ran separately with the
documented `PYTHONPATH` and `TMPDIR=/private/tmp`. Missing locked Agent dependencies
were installed before the successful rerun. An initial frontend assertion used
an unavailable matcher; it was corrected and rerun. Existing Vite/Sass/chunk
warnings did not fail the production build.

## Remaining Acceptance And Runtime Notes

Full browser installation from an empty Server, the complete dependency and
stale/expiry matrix, every publication crash boundary, unknown hook effect
reconciliation, queued/retried/background task resource races, interrupted
PostgreSQL recovery, advanced HDFS parity and the seven-database initialization/
upgrade plus coordinated offline-restore matrix remain unfinished. They are not
converted to PASS by the focused tests above.

The earlier BIGTOP request 14 TIMELINE_READER/embedded-HBase failure also remains
outside the corrected mpack paths.

The extra container `ambari-mpack-acceptance-worker5-20260926` hosts
`mpack_fresh_20260926` and the isolated restore database. It is not part of the
original deploy lifecycle state and must be included explicitly in later
environment cleanup. No cluster, backup, old archive or failure evidence was
deleted to hide a failure. The original four containers were resumed; their
Docker IPs changed, so generated hosts mappings were refreshed from Docker
inspection rather than changing persistent deployment/lifecycle rows.

The initial official-binary install needed local repository metadata recovery;
its failure is retained. A first isolated PostgreSQL install was aborted across
an Agent reconnect and succeeded on explicit retry. Re-adding Nginx after
removal required restoring its desired configuration, as a normal Add Service
workflow does; the corrected fixture and final health evidence record that step.
