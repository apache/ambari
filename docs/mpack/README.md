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

# Mpack V1 Acceptance Entry Point

Start here in a new session. Acceptance must not depend on earlier chat history.

Read the [2026-09-24 stage checkpoint](checkpoint-2026-09-24.md) first. It records
the deployed Rocky environment, source/artifact identities, completed checks,
known failures and the user-requested pause after remote pushes without a PR.

The active work is the [Rocky and Kyuubi implementation plan](rocky-kyuubi-implementation-plan.md),
approved on 2026-09-24. It adds Kyuubi and live acceptance using existing deploy
images. The user explicitly excludes Ubuntu deployment and testing; the Rocky
platform decision supersedes the earlier environment prerequisites below.

Read the [2026-09-22 corrective implementation](corrective-implementation.md)
before the earlier handoff below. It records whole-store import/service selection,
scoped maintenance, recovery corrections and earlier local checks. Live
acceptance has now started; its current state is in the checkpoint and result ledger.

## Workspace And Current State

| Item | Value |
| --- | --- |
| Implementation worktree | `/Users/jialiang/PRJS/ambari-mpack-v1` |
| Implementation branch | `AMBARI-26663` |
| Starting commit | `116be946fb5f911236db70861631eb4ed95792cc` |
| Independent reference repository | `/Users/jialiang/PRJS/ambari-mpacks` |
| Original workspace, not acceptance source | `/Users/jialiang/PRJS/ambari` |
| Commit state | AMBARI-26663 topic commits; see checkpoint-2026-09-24.md and Git history |
| Completed checks | Latest corrective checks: Java compilation and 136 tests, React build and 87 tests, Python tooling 16 and Agent 40 tests, deterministic whole-store build; see corrective-implementation.md |
| Not completed | Kyuubi SQL, authenticated browser acceptance, recovery/crash tests, HDFS advanced workflows, database upgrades and offline backup/restore |

The branch HEAD alone does not identify the uncommitted implementation. Record
the working-tree changes, including untracked implementation files, before
building acceptance artifacts. Do not reset, clean, merge, or overwrite the
original workspace. Temporary tools, generated archives, logs and the Vite
server recorded in the baseline may no longer exist; recreate or verify them.

## Reading Order

1. [Delivery baseline](delivery-baseline.md): accepted scope, implementation
   status, exact previous verification commands, exclusions and pending commits.
2. [Acceptance runbook](acceptance-runbook.md): prerequisites, ordered cases,
   commands, authoritative observations and pass/fail conditions.
3. [HTTP contract](http-api.md): actual endpoint, payload and error contracts.
4. [Design](design.md) or [Chinese design](design.zh-CN.md): normative behavior,
   especially sections 1.4, 7, 8 and 9.
5. [Acceptance results](acceptance-results.md): update this ledger with actual
   execution evidence, failures, blockers and the resulting delivery decision.

## New Session Task

```text
Use /Users/jialiang/PRJS/ambari-mpack-v1 as the implementation worktree.
Read AGENTS.md and docs/mpack/README.md, delivery-baseline.md,
acceptance-runbook.md and http-api.md before acting. Inspect git status in both
the implementation worktree and /Users/jialiang/PRJS/ambari-mpacks.

Perform mpack V1 acceptance in the runbook's order, starting with the live
HTTP lifecycle and a fresh GENERIC/Nginx deployment. Establish an isolated
Server/database/Rocky 8 Agent environment from this implementation, reusing the
recorded deployment and incremental replacements. Ubuntu testing is excluded.
If infrastructure or credentials are missing, report the exact blocker;
do not replace live acceptance with mocks or source inspection.

Validate structured API/task/service observations and exact operation,
plan, generation, snapshot and task identities. Never infer success from
logs, process exit codes, HTTP 202, or frontend resource HTTP 200 alone.
Keep the delivery scope frozen. Fix demonstrated acceptance defects only,
with focused regressions; do not add GC, quota/backup/workflow frameworks.
Record results in acceptance-results.md and reconcile delivery-baseline.md.
Keep unfinished or blocked cases visible. Commits remain subject to the
repository's JIRA-key and reviewable-topic requirements.
```
