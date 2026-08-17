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

The React Kerberos UI exposes the primary Enable, Disable, identity, credential,
and keytab screens, but the implementation does not yet provide a reliable
equivalent to the Classic Ember workflow. KDC mode forcing, resource creation,
Step 3 client installation, Stack Advisor, descriptor fallback, long-running
request recovery, formal exit cleanup, Disable failure paths, credential error
propagation, and Regenerate convergence have incomplete or incorrect behavior.

This issue covers the complete non-Metrics Kerberos module defined by the 81
feature IDs in `docs/frontend-refactor/ember-baseline/08-kerberos.md`. The React
comparison and runtime matrix are recorded in
`docs/frontend-refactor/react-current/08-kerberos-gap.md`.

## Scope

* Preserve permission and feature gates, security-status loading, installed-service warnings, and optional pre-Kerberize checks.
* Implement MIT, Active Directory, IPA, and Manual mode visibility, forced values, prerequisites, and navigation.
* Make Step 2 resource/config/session creation ordered, failure-propagating, retryable, and duplicate-safe.
* Install KERBEROS_CLIENT through the Classic INIT versus initialized branches and run service/heartbeat checks with Retry and Ignore Errors.
* Load, recommend, edit, create/update, and recover Kerberos descriptor values without losing the current form.
* Preserve mode-specific confirmation and Manual CSV responsibility with visible download failure.
* Stop services, perform ATS compatibility cleanup, force-retry Kerberize, and start/test services with persisted request recovery.
* Complete or exit the wizard through atomic recovery-state cleanup and formal best-effort unkerberize/service deletion.
* Implement Disable sequencing, unkerberize Skip, idempotent service removal, service-check policy, and completion gating.
* Align identity management, automatic/Manual Regenerate, persistent KDC credential management, and invalid-session replay.
* Add focused tests for request shapes, mode branches, failure ordering, polling/recovery, credential CRUD, and compatibility fixes.
* Record cross-module Add Service, Add Host/component, Service/Host, Reassign, HA, and Federation acceptance boundaries.

## Acceptance Criteria

* All 81 baseline IDs have a mutually exclusive status and evidence in the React comparison.
* The Kerberos routes require `CLUSTER.TOGGLE_KERBEROS` and `enableToggleKerberos`; CSV retains its independent upgrade authorization.
* Step 1 uses the Classic prerequisite sets and shows the ONEFS item only when ONEFS is installed.
* Manual and IPA forced configuration values are included in the submitted desired configs.
* Step 2 stops after any failed prerequisite, retains input, unlocks Retry, and never reports false success.
* Step 3 reads component state and submits exactly one of service installation or named host-component installation.
* Stack Advisor is called only for a first Step 4 load, recommendations are applied before display, and saved form recovery avoids duplicate recommendation.
* Descriptor create/update fallback is limited to the expected 409/404 response and remains one awaited promise.
* Steps 3, 6, 7, and 8 recover request IDs and poll existing work instead of repeating mutations.
* Exit from Steps 1-7 attempts unkerberize and KERBEROS deletion before clearing state; Step 8 never rolls back enabled security.
* Disable exposes the identity-management Skip only after unkerberize failure and cannot hang on obsolete-service deletion.
* Credential CRUD failures remain errors, retain input, and never replay the protected operation.
* Regenerate tracks its returned request and starts optional component restart only after successful completion.
* Every timer and request poll is invalidated on terminal state, retry, dependency change, or unmount.
* Focused frontend tests, TypeScript, build, lint, baseline validation, and helper tests pass or have pre-existing failures documented exactly.
* The real-cluster runtime matrix passes before operational parity is claimed.

## Partial Cross-Module Boundaries

`KRB-X-001` through `KRB-X-005` require integration with the Add Service, Add
Host/Hosts, Services, Reassign, HA, and Federation modules. This issue validates
the shared KDC-session and Regenerate contracts but does not duplicate those
modules' wizard or component code.

Disable refresh/server-restart recovery and Manual principal/keytab fulfillment
cannot be closed by static frontend tests. They remain explicit runtime gates.

## Out of Scope

* Metrics pages, charts, widgets, metric polling, Metrics APIs, and any Metrics-specific Kerberos presentation.
* Reproducing the broken Classic Step 3 Rollback or unreachable Skip controls.
* Adding a user action for the Classic Step 7 test-only unkerberize method.
* Reproducing false-success credential behavior or the undefined descriptor fallback callback.
* Rewriting Add Service, Add Host, Reassign, HA, Federation, or global Background Operations in this issue.

## Compatibility Decisions

React treats prerequisite and credential failures as failures instead of
continuing like Classic. Descriptor fallback is atomic. CSV and ATS failures are
recoverable. Regenerate follows its own request rather than unrelated global
operation counts. Disable does not clear Add Service state. These are deliberate
correctness fixes and are not missing parity.

The unusual API-root pre-Kerberize GET and direct Step 7 force-retry semantics
are retained because they are executable Classic contracts.

## Verification Boundary

Unit and component tests cannot prove MIT, Active Directory, IPA, or Manual KDC
operation. They also cannot prove server-side principal/keytab effects, service
restart convergence, websocket/background presentation, or refresh after a
server restart. Do not mark this issue operationally complete until the runtime
matrix in `08-kerberos-gap.md` has been executed against real environments.

