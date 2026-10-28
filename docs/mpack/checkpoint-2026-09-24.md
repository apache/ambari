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

# AMBARI-26663 Stage Checkpoint: 2026-09-24

The user requested topic commits and remote pushes without a pull request,
followed by a pause until the next session. This is a work-in-progress checkpoint,
not full mpack acceptance. Ubuntu deployment and testing remain excluded.
Continue from these artifacts; do not repeat the full RPM build.

## Workspaces And Runtime

| Item | Identity |
| --- | --- |
| Core | `/Users/jialiang/PRJS/ambari-mpack-v1`, `AMBARI-26663` |
| Store | `/Users/jialiang/PRJS/ambari-mpacks` |
| Deploy | `/Users/jialiang/PRJS/deploy` |
| Upstream Kyuubi | `/Users/jialiang/PRJS/kyuubi`, v1.9.4, `5f48f2ef5b2532383a1e21984eec94ac451caf85` |
| Platform | Rocky 8 aarch64; four Docker nodes, 4 GiB each |
| Image | `local/ambari-rocky8@sha256:88b3f988311639f4d5bfb66ee09a4b3ea014b3ff9f63b978338d599c317857d0` |
| Deployment | `mpack-rocky-20260924` |
| Ambari | `http://127.0.0.1:18080`; React under `/latest/` |
| Clusters | `mpack_nginx_20260924` on worker2; `mpack_kyuubi` on worker1, worker3 and worker4 |
| Private artifacts | `/Users/jialiang/.local/share/ambari-mpack-acceptance/20260924` |

Container prefix: `ambari-mpack-rocky-20260924-c83d830a-`; suffixes are
`controller-1`, `worker2-1`, `worker3-1`, `worker4-1`. Hostnames are
`workerN.bigtop.apache.org`. Credentials remain in the existing private
environment and are not copied into this checkpoint.

The first Ambari build froze the dirty implementation into isolated build input
`20c33adf1b4d25647bddf5f4e23144d52cdd0345`, based on core
`116be946fb5f911236db70861631eb4ed95792cc`. This temporary build-input commit
does not replace the deliverable's topic history.

## Builds And Incremental Replacements

Completed deploy runs:

- `rocky8-arm64-mpack-v1-20260924`: Server, Agent and Metrics RPMs,
  version `3.1.0.0-1790252549.git20c33adf1b4d`.
- `rocky8-arm64-kyuubi-mpack-20260924-v2`: Kyuubi 1.9.4 RPMs.
  The first attempt failed because pnpm passed Maven proxy flags to Vite.
- `rocky8-arm64-spark-repack-20260924`: reused the completed Spark 3.5.8
  payload from `bigtop360-rocky9-arm64-v4`, verified RPM/file digests, and
  repackaged dependency names and paths for Rocky 8 / BIGTOP 3.3.0.
  Spark source was not compiled.
- `composed-spark/manifest.json` combines the verified component manifests.
  Only Spark base/core/datanucleus and Kyuubi are used for the new SQL path;
  SparkR and other extra modes are not validated.

The existing Builder was updated through `builder prepare` because Maven 3.8.8
did not meet the current 3.9+ contract. No runtime image was rebuilt.

The live Server has an incremental JAR replacement:
`ambari-server-resource-fix.jar`, SHA-256
`1db0997b8496932f0a3e85eb7d174cc886f0f04223e2e5ca775d88f41ef7ec96`.
It adds correct Jackson-node HTTP serialization and the actual
`/resources/mpacks/*` download route. Original JAR recovery copies are under
`/var/lib/ambari-server/development-backups`.

The Agent FileCache fix was copied to all four nodes and Agents restarted.
Source SHA-256:
`602e818708653524edf097b4bbd50e02d4cad76c94f87f91ce308b438a1818f9`.
Verified resource roots now become traversable by service accounts, including
repair of old 0700 roots without replacing resource contents.

React assets were rebuilt and copied to `/usr/lib/ambari-server/web/latest`.
The new bundle is `index-Cw2PFpJC.js`, SHA-256
`f12efe8ed1d9d83550e3d41c423dcd8d45c16f0e6c7317c7bbfc171c15bcac22`.
Kyuubi Summary now renders declared components through the generic view; the old
hardcoded `KYUUBI` component omitted the actual `KYUUBI_SERVER`.

## Observed Live Results

Evidence is retained outside Git in `evidence`, `evidence-v2`,
`upgrade-v3`, `final-store`, `kyuubi-live` and `postgresql-live`.

- All four Agents advertised `MPACK_RESOURCES_V1`.
- Whole-store upload, preview without catalog mutation, IMPORT, service
  discovery and exact ENABLE handoff passed. Imports did not execute hooks
  or change the effective definition snapshot. The first import creates
  durable catalog control, so the bindings response's `managed` flag alone
  is not an activation observation.
- Idempotent acceptance replay retained the operation ID and generation.
  Retiring inactive releases retained their inventory records with
  `installed: false` and ran no uninstall hooks.
- Definition update operation `4e0f53c1-dfd7-40d5-918b-1eb546d241f1`
  completed with explicit maintenance during installation recovery.
- Current store: generic-base/nginx/postgresql `1.0.0.3`, Kyuubi `1.0.0.2`.
  The `store-f` bundle digest is
  `587d79b35cfb44b4713cd3f7d8a9e0c2e386fa86cfc251df95d880491c97b51d`.
- Nginx: installation request 9/task 54, start 11/55, real HTTP service check
  13/56 and RELOAD 24/98 completed. Observations report Nginx 1.14.1, the
  matching systemd job and HTTP identity `ambari-nginx-v1` on port 8088.
- PostgreSQL: installation 25/99, start 28/112 and health check 36/120
  completed. The last check reports PostgreSQL 10.23, server version 100023,
  instance `7689102948144019413` and matching task/execution/snapshot IDs.
- Kyuubi: installation 27/106 and start 32/116 completed with RPM 1.9.4
  and the matching `ambari-kyuubi.service` job.
- Kyuubi SQL check 34 failed before the cache permission repair; retry
  35/task 119 still failed afterward. It has no validated SQL result and must
  not be counted as successful. The next session must inspect the structured
  helper failure and complete actual JDBC/Spark execution.
- Initial BIGTOP request 14 also failed to start TIMELINE_READER's embedded
  HBase. Explicit install/start recovery requests restored other affected
  components, but this original failure and the Timeline Reader gap remain.
- The UI change passes component tests and is deployed. Authenticated browser
  verification of the reported Components issue is still outstanding.

The runtime RPM repository needed an index refresh to expose Kyuubi/Spark under
the repository ID pinned in existing install commands. Separate local-repository
availability alone did not satisfy that selection. Metadata files were generated
separately and `repomd.xml` replaced after publishing their referenced files.
Rocky BaseOS/AppStream and the official key path were added for missing OS
dependencies. The GENERIC definition includes both repositories and required
Agent defaults; do not work around these by disabling package signature checks.

## Verification At Checkpoint

- Initial full Ambari RPM build through deploy: passed.
- Three real Jersey/Jetty HTTP tests: passed.
- Agent FileCache: 33 tests passed with the documented working directory and
  `TMPDIR=/private/tmp`; an earlier invocation with different path settings failed.
- Python mpack authoring/HTTP contracts: 16 tests passed.
- Store observation/Kyuubi tests: 11 passed locally and under Rocky 8 platform Python.
- React focused suites: 47 tests passed; production build passed.
- Deploy build-tool suite: 43 tests passed.
- Kyuubi JDBC helper compiled against the actual JDBC artifact.
- Earlier store archives were byte-identical across repeated builds. Final
  release content and every live result must retain their own recorded digest.

The earlier Java 136-test and React 87-test results remain historical evidence
in `corrective-implementation.md`, not newly rerun checks. Full crash injection,
permissions/browser recovery, PostgreSQL backup/restore, HDFS parity and the
database upgrade matrix are unfinished.

## Commit Boundaries And Resume

Core code topics end at `80dc84543d`; this document follows in the final
evidence commit. The store checkpoint is `793eebf` (four topic commits), and
deploy is `2bd0138` on `AMBARI-26663`. Core and deploy target their personal
GitHub repositories on `AMBARI-26663`, without a PR. At push preparation,
the store had no remote and `JiaLiangC/ambari-mpacks` was not accessible;
its local commits must be preserved until a remote is supplied. Check actual
remote refs before treating all three repositories as backed up remotely.

The checkpoint is split into persistence, authoring/schema, Server lifecycle,
Agent integration, generic service UI, management/deployment UI and evidence.
Server lifecycle wiring spans a mutually dependent set of stack, task, runtime
and HTTP classes and is kept together so the intermediate revision compiles.
Store commits add the foundation, Nginx, PostgreSQL and Kyuubi in dependency
order, with matching release indexes. Deploy changes form a separate build fix.

After the requested pushes, stop work and preserve the running environment.
On resumption inspect Git and deployment status, read this checkpoint and the
result ledger, finish Kyuubi SQL and browser validation, then continue the
remaining acceptance cases. Keep using incremental replacement and lightweight
new mpack releases; never overwrite published archives or snapshot contents.
