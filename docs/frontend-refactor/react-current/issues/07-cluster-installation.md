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

## Problem

The React cluster installation module exposes New Cluster, Add Host, and Add
Service wizards, but it does not yet provide a reliable equivalent to the
Classic Ember workflows. Direct routes are not consistently protected,
wizard state can be lost or overwritten during navigation and reload, version
definition requests use a hard-coded stack, and installation can advance after
failed resource creation. Deploy polling also loses request identity, exposes
invalid transitions, and does not restore the Classic completion checkpoints.

Several conditional branches are incomplete, including Linux, manual Agent,
and Windows host registration, JDK checks, managed repositories, services with
no assignable components or configurations, Kerberos descriptor and credential
flows, configuration groups, and installation with component start disabled.
The result is a wizard that can render every numbered page without preserving
the state machine, API ordering, and failure-recovery guarantees of Classic.

This issue covers the complete non-Metrics Cluster Installation module defined
by the audited Classic baseline. Metrics services, metric widgets, charts, and
Metrics data APIs are outside this scope.

## Scope

* Enforce New Cluster, Add Host, and Add Service route permissions, feature
  flags, active-operation conflicts, and wizard ownership at direct-entry and
  mutation boundaries.
* Serialize wizard hydration and persistence so every forward or backward
  transition stores the resulting data and destination step before navigation,
  and restore Classic server checkpoints without replaying mutations.
* Use the selected stack for stack, version, operating-system, repository, and
  Advisor requests; retain XML or URL version-definition sources through the
  non-dry-run Review submission.
* Match public, local XML, local URL, Satellite, managed-repository, URL-skip,
  and Ambari Server JDK compatibility branches.
* Match Linux SSH, manual Agent, and HDPWIN PowerShell registration, runtime
  Agent-user support, host normalization, installed/suspicious host handling,
  bootstrap, registration, host checks, the independent JDK check, selective
  retry/removal, timeout, polling cleanup, and reload recovery.
* Preserve service dependencies, filesystem conflicts, master and slave/client
  cardinality, Advisor recommendations, matching validation warnings, manual
  assignments, configuration recommendations, external tests, overrides, and
  dirty-state navigation.
* Skip Add Service assignment or configuration steps when component metadata
  makes them inapplicable, and complete Kerberos descriptor, KDC type, CSV, and
  Manual KDC responsibility behavior.
* Make Review cleanup recoverable, retain validated repository input, create
  clusters, services, components, hosts, host components, configurations,
  configuration groups, and Kerberos artifacts in dependency order, and abort
  before installation on the first failed prerequisite.
* Add Review print, Blueprint archive, and applicable Kerberos CSV exports.
* Persist install, start, and service-check request identities and phases; poll
  one request at a time; restore on reload; load task logs lazily; expose Retry
  only for the applicable failed phase; block route exit; and enable Summary
  only at a defined terminal state.
* Persist the correct `*_INSTALLED_4` checkpoint, update provisioning state only
  for a new cluster, clear only the completed wizard, and refresh the correct
  destination for Add Host or Add Service.
* Add focused tests for request contracts, permission and branch boundaries,
  state persistence, serial deployment, polling, retry, cancellation, reload,
  and completion.

## Classic UI Baseline

The acceptance baseline is
`docs/frontend-refactor/ember-baseline/07-cluster-installation.md`, feature IDs
`INST-MODE-001` through `INST-10-002`. The detailed React comparison, complete
state machines, API sequence, five-pass audit, and runtime matrix are recorded
in
`docs/frontend-refactor/react-current/07-cluster-installation-gap.md`.

The audit found no new conflict between the written Module 07 baseline and the
executable Classic source. The authoritative network review combines the AJAX
registry and callers, direct HTTP calls, browser entry points, route and action
inventories, permissions, feature flags, and REST polling behavior.

## Acceptance Criteria

* Direct wizard routes and mutations enforce the documented permission,
  feature, ownership, and active-operation gates.
* Hydration failures never overwrite persisted data, transitions persist the
  resulting snapshot and destination step, and every documented Classic
  checkpoint restores the correct workflow and request phase.
* Version, repository, VDF, bootstrap, registration, host-check, JDK, Advisor,
  validation, resource, install, start, check, task-log, and completion requests
  match the documented URL, method, query, payload, and order.
* New Cluster and Add Host cover Linux, manual, and HDPWIN onboarding with the
  applicable support flags, timers, retries, removals, warnings, and reload
  behavior.
* Service selection and assignment preserve dependencies and cardinality;
  Add Service skips inapplicable steps and completes all Kerberos branches.
* Review creates every applicable resource serially, aborts on the first
  failure, never starts installation after a failed prerequisite, and provides
  recoverable errors without duplicating completed destructive work.
* Deploy owns one current request and poll loop, restores the exact phase after
  reload, prevents duplicate requests, gates Retry and Summary by terminal
  state, blocks unsupported route exits, and cleans timers on unmount.
* Summary reports the final host, component, service, and check outcomes,
  persists the correct completion state, and applies new-cluster-only
  provisioning behavior.
* Focused Vitest suites, the full frontend test suite, the production build,
  the Ember baseline validator, and whitespace checks pass.
* The runtime acceptance matrix in the React gap document passes against a real
  Ambari Server for representative stacks, roles, scale, failures, Kerberos,
  HA prerequisites, and all three installation modes.

## Compatibility Decisions

The React implementation must not reproduce Classic failure modes that allow
work to continue after a rejected prerequisite. Configuration-group writes and
all dependency-ordered resource mutations are awaited, cleanup errors become
visible and retryable, unsafe task output is not rendered as HTML, and stale
Advisor or polling responses cannot overwrite newer state. These are deliberate
reliability corrections, not missing parity.

## Out of Scope

* Ambari Metrics, Ganglia, Metrics services, metric widgets, charts, and metric
  data APIs.
* Standalone HA, Kerberos administration, Upgrade, and Reassign Master wizards;
  Module 07 implements only their installation entry conditions and shared
  prerequisites.
* Module 05, Module 06, and other parallel module gap documents or workflows.

## Verification Boundary

Static code and unit tests are not sufficient to mark the 79 Feature IDs
covered. All items remain subject to the real-cluster runtime matrix, including
public and local repositories, Linux/manual/Windows hosts, failures at every
Review stage, install/start/check interruption, reload and retry, permissions,
multi-window ownership, large host sets, Kerberos, and conditional HA topology.
