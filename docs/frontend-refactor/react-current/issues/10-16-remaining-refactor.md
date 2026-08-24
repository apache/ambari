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

## Suggested Summary

Advance React parity for the remaining HA, Views, authorization, Service
Theme, and audit modules

## Problem

The React frontend does not yet have an accepted parity result for the final
five functional modules in the Classic Ember baseline. The remaining work is
tightly coupled: ResourceManager and Ranger Admin HA, Federation and HAWQ,
Views, global authorization, and Service Theme configuration layouts share
route guards, upgrade and wizard exclusions, configuration identity, persisted
workflow ownership, long-running request recovery, and application-shell
behavior. Reviewing or landing these areas independently can leave
contradictory permissions, recovery semantics, or matrix evidence.

This issue covers Modules 10 through 16 as one coherent non-Metrics delivery.
It includes the 581 stable feature IDs in Modules 10 through 14, generation and
validation of the canonical 1,154-ID React parity matrix, and the final
five-pass reverse audit. A matching route, component name, or REST endpoint is
not sufficient evidence of parity.

## Scope

* Complete ResourceManager HA and Ranger Admin HA entry guards, topology and
  host validation, Stack Advisor integration, KDC boundaries, generated
  configuration, ordered stop/install/save/start/test operations, persisted
  request checkpoints, Retry behavior, refresh recovery, and workflow-owner
  cleanup, while explicitly recording the pre-request-ID unknown-outcome
  boundary that requires server reconciliation or idempotency support.
* Complete NameNode Federation, Router-based Federation, and historical HAWQ
  Standby Add/Remove/Activate workflows, including stack metadata and topology
  gates, secure-cluster branches, exact configuration transformations, custom
  commands, ordered operations, failure handling, and durable recovery.
* Complete ordinary and View-only Views shell behavior, two-phase View
  discovery, server-authorized instance filtering, regular and short routes,
  encoded paths and query strings, iframe loading and recovery, browser
  navigation, and Admin View version/proxy-root redirection.
* Align global permission expressions, upgrade exceptions, wizard-owner
  mutation exclusions, read-versus-mutate checks, Admin routes, User Settings,
  Experimental settings, restart-required operations, support flags, stack
  metadata, service/component topology, Windows gates, and transition-state
  locks with the executable Classic rules.
* Implement the Service Theme extension contract across Service Configs, Host
  Configs, configuration comparison, and installation/Add Service Step 7,
  including layout-qualified identity, ordered geometry, Widgets, conditions,
  canonical saves, read-only modes, secret-safe diagnostics, fallback, Retry,
  stale-response rejection, and custom stack/service Theme fixtures.
* Resolve every remaining static gap or record an explicit, reviewable
  compatibility decision. Known gaps must not be hidden by a general parity
  claim.
* Generate JSON, CSV, and Markdown representations of the canonical React
  parity matrix from all 14 module comparison documents. Preserve all 1,154
  stable IDs, source evidence, issue references, review metadata, and the
  reproducible reviewed-source digest.
* Perform five independent reverse-audit passes over routes and entry points;
  mutations, payloads, and request ordering; permissions, flags, topology,
  upgrades, and wizard ownership; asynchronous failure, Retry, persistence,
  refresh, and cleanup; and tests, exclusions, and evidence consistency.
* Add focused tests for normal, denied, failed, retried, refreshed, and
  cross-owner paths, and retain explicit real-cluster scenarios for behavior
  that static tests cannot prove.

## Classic UI Baseline

The acceptance baseline is the executable Classic Ember source together with
these reviewed documents:

* `docs/frontend-refactor/ember-baseline/10-rm-ranger-ha.md`: 107 feature IDs.
* `docs/frontend-refactor/ember-baseline/11-federation-hawq.md`: 194 feature IDs.
* `docs/frontend-refactor/ember-baseline/12-views.md`: 92 feature IDs.
* `docs/frontend-refactor/ember-baseline/13-permissions-flags.md`: 36 feature IDs.
* `docs/frontend-refactor/ember-baseline/14-service-theme-layout.md`: 152 feature IDs.
* `docs/frontend-refactor/ember-baseline/15-react-gap-matrix.md`: matrix schema,
  status rules, evidence requirements, and review gates.
* `docs/frontend-refactor/ember-baseline/16-five-pass-audit.md`: independent
  reverse-audit method and frozen baseline conclusions.

The corresponding React comparison documents under
`docs/frontend-refactor/react-current` must remain synchronized with the code.
When a baseline statement conflicts with Classic source or verified runtime
behavior, the baseline must be corrected with source evidence instead of
changing React to match an unsupported statement.

## Acceptance Criteria

* All 581 Module 10 through 14 feature IDs appear exactly once in their final
  React comparison tables with a supported status and concrete evidence.
* ResourceManager and Ranger Admin HA enforce menu and direct-route conditions,
  generate the documented configuration, serialize every mutation, retain
  recoverable request state, stop after a failed prerequisite, and avoid
  replay after local or server-confirmed failures. The issue must not claim
  exactly-once recovery when a mutation response is lost before its request ID
  is persisted.
* NameNode Federation executes its complete ordered workflow; Router Federation
  creates or updates its configuration and manages Router components safely;
  HAWQ Add, Remove, and Activate retain their distinct stack, topology,
  permission, and custom-command contracts.
* Every HA workflow persists the owner, step, inputs, operation state, returned
  request IDs, and terminal results before advancing. Refresh resumes completed
  work or active work with a saved request ID, another user cannot take
  ownership, and cleanup failures remain visible and retryable. A recovered
  `QUEUED` mutation without an ID remains `PARTIAL` until a server protocol can
  reconcile whether the earlier request was accepted.
* Views discovery preserves the two-request contract and distinguishes empty,
  hidden, system, unauthorized, malformed, and failed responses. Regular and
  short routes preserve encoded application paths and query strings without
  treating client visibility as authorization.
* View-only initialization avoids cluster-only operations while preserving the
  documented shell and session lifecycle. Admin View navigation uses the
  server-reported version contract and configured proxy root and provides a
  deterministic failure path.
* Permission expressions preserve comma-separated OR semantics and the
  whole-expression upgrade exceptions. Visibility checks remain distinct from
  owner-aware mutation checks, and direct routes repeat the applicable action
  gates.
* Support flags and runtime gates have an executable consumer, an explicit
  `MISSING` or `PARTIAL` result, or a documented `NOT_APPLICABLE` compatibility
  decision. No flag is considered migrated only because it is loaded.
* Service Theme preserves full config paths and parent-qualified layout
  identity, declaration order, every supported Widget round trip, layered
  conditions and attributes, canonical save exclusion for UI-only properties,
  password secrecy, read-only consumers, fallback, Retry, and stale-response
  isolation across all four consumers.
* Reverse API review accounts for every reachable mutation, including HTTP
  method, URL, predicates, fields, headers, body shape, operation level,
  ordering, response branches, polling or realtime behavior, and error
  recovery.
* The generated matrix contains exactly the canonical 1,154 stable IDs and its
  JSON, CSV, and Markdown representations are synchronized. The 581 IDs above
  are only this issue's functional-module subset. Metrics exclusions and legacy
  placeholders remain in the full generated outputs without entering the
  completion denominator.
* Source statuses `COVERED`, `MATCH`, `PASS`, and `STATIC_COMPLETE` that are
  supported only by static evidence normalize to `NEEDS_RUNTIME_VALIDATION`.
  Explicit gap and decision statuses remain unchanged. Final `COVERED` is used
  only after an acceptance review records independent normal-path and
  failure/recovery runtime evidence.
* Every `MISSING`, `PARTIAL`, `BLOCKED`, `BEHAVIOR_DIFF`, and
  `NOT_APPLICABLE` row has this issue, a follow-up issue, or an explicit
  maintainer decision with a reason.
* The baseline validator, parity-matrix tests and validator, complete Feature
  ID reverse checks, focused Vitest suites, TypeScript build, production build,
  applicable lint checks, and `git diff --check` pass, or unrelated pre-existing
  failures are reported precisely.
* A second reviewer independently checks all eventual `COVERED` and
  `NOT_APPLICABLE` rows and confirms that the five audit passes found no
  unassociated non-Metrics route, action, mutation, permission, flag, recovery
  path, or test boundary.

## Required Runtime Validation

Static source review, mocks, and unit/component tests cannot close the
following acceptance boundary:

| Area | Minimum real-environment scenarios |
| --- | --- |
| ResourceManager HA | Secure and non-secure clusters; valid and invalid RM/ZooKeeper/host topology; Stack Advisor changes; optional HAWQ/HDFS configuration; KDC cancellation/failure; failed requests; refresh and cross-user recovery |
| Ranger Admin HA | Eligible and ineligible topology; `http`, `https`, and `ftp` load-balancer values; Kerberos providers; configuration save and service-check failure; interrupted requests; external load-balancer verification |
| NameNode Federation | Secure and non-secure clusters; optional Ranger, Accumulo, and Infra Solr branches; all ordered operations; format/bootstrap failure; Retry; reload and non-owner access |
| Router Federation | Stack metadata with and without DFSRouter support; existing and new `hdfs-rbf-site`; zero and multiple topology inputs; install/start/maintenance failure; Retry and reload |
| HAWQ Standby | A compatible historical stack; Add, Remove, and Activate; each permission combination and custom-command gate; request failure, retry, refresh, and owner recovery |
| Views | Cluster user, View-only user, Ambari administrator, and unauthorized user; hidden/system/multiple-version instances; short URLs and proxy roots; cold and warm valid, invalid, deleted, and malformed-percent-encoded deep links; path/query preservation; iframe load, resize, host scrolling, activity, popup, download, and fullscreen behavior; interval cleanup; partial initialization; session expiry; logout failure; keep-alive start, duplicate prevention, and stop; Admin View redirect failure |
| KDC and credentials | Manual, MIT, Active Directory, and IPA modes; temporary and persisted credentials; cancellation, expiry, lookup and save failure, Retry, and resulting principal/keytab ownership across each applicable HA or Federation workflow |
| Global gates | Read-only, service operator, cluster administrator, Ambari administrator, and View-only roles while idle, during supported and blocking upgrades, and with the same or another wizard owner |
| Service Theme | Real shipped and custom service Themes; duplicate local names; malformed metadata; every Widget and condition; Service, Host, comparison, Install, and Add Service consumers; read-only roles; secret-bearing failures; Retry, service switching, refresh, save/reload, and stale responses |
| Fault injection | Network loss and server errors before and after every irreversible mutation, failed persistence, stale polling responses, browser refresh, tab close, and a second session attempting the active workflow |

Runtime outcomes must be recorded per matrix row or linked scenario. A single
happy-path demonstration does not establish operational parity for a long HA
workflow.

## Out of Scope

* Ambari Metrics pages, charts, widgets, Heatmaps, Horizon Charts, Metrics data
  APIs, and Metrics-specific realtime destinations.
* Business functionality implemented inside separately deployed View
  applications. This issue covers only the Ambari shell's discovery, routing,
  hosting, authorization boundary, and browser navigation.
* Reimplementation of the AngularJS Admin Console CRUD pages. Only discovery
  and navigation into the existing Admin View are included.
* Backend authorization or service-command implementation changes unless a
  verified frontend contract defect requires a separately reviewed fix.
* Classic placeholder, unreachable, or verified broken behavior that has an
  explicit compatibility decision, including unsupported HA disable or
  rollback paths.
* Metrics-related permissions and support flags. Operational CPU, memory, disk,
  topology, formatted-state, and request-progress fields remain in scope when
  they are direct inputs to a non-Metrics workflow.

## Compatibility Decisions

React may deliberately improve verified unsafe or broken Classic behavior by
failing closed on incomplete data, waiting for prerequisite requests, guarding
direct routes, avoiding replay after confirmed failures, escaping untrusted
output, and retaining recoverable workflow state. Each difference must identify the
Classic behavior, React behavior, user impact, and maintainer decision in the
module document and parity matrix. Reliability improvements are not evidence
that unrelated Classic branches can be omitted.

## Verification Boundary

The code portion of this issue can be considered ready after the focused tests,
TypeScript and production builds, validators, matrix generation, reverse
checks, and five-pass audit succeed. Operational parity remains
`NEEDS_RUNTIME_VALIDATION` until the required real-cluster matrix has been run
and independently reviewed. The pull request must report static test results
and pending runtime scenarios separately and must not claim that unexecuted
cluster, KDC, historical-stack, proxy, iframe, role, upgrade, or fault-injection
scenarios passed.
