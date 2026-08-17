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
-->

# React Feature Parity Matrix Specification

This document defines the acceptance format for comparing `ambari-web/latest` with `ambari-web/classic`. At this stage, it establishes rules and templates only; it does not pre-judge React coverage status. Legacy facts remain in the module documents, while React status is derived data that can be updated repeatedly.

## Comparison Unit

Each matrix row corresponds to one stable feature ID, not an Ember route, React component, or REST URL. Viewing, modification, failure retry, and recovery on the same page may be separate features; one endpoint may serve multiple features.

The following dimensions must be checked for every feature:

| Dimension | Minimum requirement for `COVERED` |
| --- | --- |
| Entry point | The route, menu, button, modal, or automatic trigger exists, with equivalent deep links and return paths |
| Visibility conditions | Equivalent permission, feature flag, stack/service/component/status/maintenance/upgrade/wizard conditions |
| Normal behavior | Equivalent user input, validation, confirmation, request order, success result, and model refresh |
| API | Equivalent method, URL, predicate, fields, payload, header, operation level, context, and response branches |
| Asynchronous behavior | Equivalent request ID, polling/STOMP, task detail, logs, terminal states, and progress display |
| Failure behavior | Equivalent disabling, error messages, retry, skip, rollback, cancel, partial failure, and duplicate-submission protection |
| Failure recovery | Equivalent refresh, crash, other-window, wizard-ownership, and server-state recovery |
| Tests | At least one automated test; `STATIC_ONLY/CONDITIONAL` also requires evidence from a real cluster scenario |

If any required dimension is not equivalent, the feature must not be marked `COVERED`.

## Status Enumeration

| Status | Definition |
| --- | --- |
| `COVERED` | UI, API, conditions, failure behavior, failure recovery, and tests are equivalent to the legacy baseline |
| `PARTIAL` | The primary path exists, but one or more required dimensions are not implemented or lack evidence |
| `MISSING` | React has no reachable entry point or behavior, or only an empty page shell |
| `BEHAVIOR_DIFF` | React intentionally or unintentionally changes legacy behavior; the difference, impact, and maintainer decision must be recorded |
| `NOT_APPLICABLE` | The maintainers explicitly decide not to migrate it; a reason/issue is required and this status must not hide missing work |
| `NEEDS_RUNTIME_VALIDATION` | Static code appears to provide coverage, but confirmation is required in a real stack, KDC, HA, external DB/LDAP/Log Search, or similar environment |
| `BLOCKED` | Blocked by a specific backend, dependency, or infrastructure issue; an issue is required and this status cannot replace `MISSING` |

`PLACEHOLDER` and `OUT_OF_SCOPE` are legacy evidence levels/scope markers, not React implementation statuses. Legacy `PLACEHOLDER` entries generally do not enter the required matrix; Metrics `OUT_OF_SCOPE` entries never count as gaps.

## Matrix Fields

| Field | Required | Content |
| --- | --- | --- |
| `feature_id` | Yes | Stable ID from the module document |
| `legacy_doc` | Yes | Legacy module document and anchor/heading |
| `legacy_evidence` | Yes | `CONFIRMED/STATIC_ONLY/CONDITIONAL/PLACEHOLDER/OUT_OF_SCOPE` |
| `react_status` | Yes | Status from the enumeration above |
| `react_route` | Conditional | Reachable route/entry point; record the trigger location for automatic behavior |
| `react_ui` | Conditional | Component/hook/store file |
| `react_api` | Conditional | API client/query/mutation file and request name/endpoint |
| `condition_evidence` | Yes | Code/test evidence for equivalent permissions, flags, and states |
| `happy_path_test` | Yes | Unit/component/e2e/real-cluster test reference |
| `failure_recovery_test` | Yes | Test reference for error/retry/cancel/refresh/ownership and similar flows; explain when not applicable |
| `differences` | Conditional | Precise description of missing dimensions or intentional behavior changes |
| `runtime_scenario` | Conditional | Reproduction conditions such as stack, service, KDC, HA topology, and permission roles |
| `issue` | Conditional | Fix/decision issue or PR |
| `reviewed_commit` | Yes | Full Git commit used for the React comparison |
| `reviewed_by` | Yes | Reviewer and date |

Use CSV/JSON for the editable matrix and Markdown only as an automatically rendered result. The legacy feature index must be generated from the documents; do not copy the ID list manually.

## Row Template

```csv
feature_id,legacy_doc,legacy_evidence,react_status,react_route,react_ui,react_api,condition_evidence,happy_path_test,failure_recovery_test,differences,runtime_scenario,issue,reviewed_commit,reviewed_by
INST-MODE-008,07-cluster-installation.md,CONFIRMED,MISSING,,,,,,,,,,,
KRB-MODE-003,08-kerberos.md,CONDITIONAL,NEEDS_RUNTIME_VALIDATION,/main/admin/kerberos,...,...,...,...,...,,Existing IPA with Kerberos enabled,...,...,...
```

## Comparison Workflow

1. Rerun the extractor and baseline checks at a fixed Ember baseline commit to freeze feature IDs and the API catalog.
2. At a fixed React commit, scan routes, UI actions, API clients, permissions, and tests to establish static candidate mappings first.
3. Check every dimension for each feature; do not mark a feature covered merely because a route or endpoint exists.
4. Run real-cluster tests for installation, Kerberos, HA/Federation, upgrade, external DB/LDAP/Log Search, and similar scenarios.
5. Have a second reviewer recheck all `COVERED` and `NOT_APPLICABLE` entries; perform at least one additional fault-injection/refresh recovery test for complex long workflows.
6. Each React PR updates only the status and evidence for affected rows and does not change legacy facts; submit a separate baseline correction with source evidence when a legacy baseline error is found.

## Complex Workflow Scenario Matrix

The following scenarios cannot be represented by a single happy path:

| Scenario group | Variables to cover at minimum |
| --- | --- |
| Cluster Installation | Public/Local repository; VDF file/URL; SSH/manual Agent; warning acceptance; bootstrap/registration failure; install/start/check retry; refresh recovery |
| Add Host/Add Service | Kerberos off/automatic/manual; with/without master/slave/config step; config group; descriptor/CSV; partial deployment failure |
| Kerberos | MIT/AD/IPA/Manual; KDC connection failure; lost heartbeat; descriptor create/update; Step 7 forced retry; Disable skip; persistent/temporary credential; all/missing/restart keytabs |
| NameNode/JournalNode HA | Secure/non-secure; dynamic tasks from dependent services; checkpoint wait; JN add/remove/delete-only; automatic/manual rollback; refresh/other window |
| RM/Ranger HA | Service/topology prerequisite failure; host conflict; Stack Advisor changes; conditional HAWQ/HDFS configuration; progress failure/retry |
| Federation/HAWQ | NameNode/Router federation; format/bootstrap failure; Kerberos; add/remove/activate standby on legacy HAWQ stacks |
| Upgrade | Rolling/Express/Host Ordered; precheck required/warning/bypass; pause/resume/retry/skip/abort/downgrade/finalize; non-owner |
| Permissions | At minimum read-only, service operator, cluster admin, Ambari admin, and View-only; repeat validation under upgrade/wizard exclusion |

## Review Gates

A module can be declared migrated only when all of the following are satisfied:

1. Every in-scope feature ID in the module has a matrix row with a non-empty status.
2. Every `MISSING/PARTIAL/BLOCKED/BEHAVIOR_DIFF` entry has an issue or an explicit maintainer decision.
3. Every `COVERED` entry has normal-path and failure/recovery evidence; simple read-only items without a failure path must state why it is `not applicable`.
4. Every `CONDITIONAL/STATIC_ONLY/NEEDS_RUNTIME_VALIDATION` entry has a runtime scenario and test result.
5. Reverse API scanning finds no unassociated mutations, and reverse route/action scanning finds no unassociated reachable user behavior.
6. Metrics exclusions are not incorrectly counted toward the completion rate.
