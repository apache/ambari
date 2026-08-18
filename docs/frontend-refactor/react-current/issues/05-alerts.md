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

The React Alerts module provides a substantial list, details, Alert Group, and Alert Notification UI, but several high-risk behaviors do not match the classic Ambari contract. Alert Definition creation and 24-hour instance history are missing. List and action permissions are incorrect, definition/config editing lacks validation and route-leave protection, and common load or save failures are not recoverable.

Alert Group requests omit notification target IDs and use unsupported pseudo membership resources. Notification builders send incorrect API types and property names for built-in SNMP, Custom SNMP, and Alert Script targets, and authenticated Email edits can overwrite an unchanged sensitive password. Instance response text can also be interpreted as markup when opened in a new window.

This issue covers the complete non-Metrics Alerts module defined by the corrected Classic baseline. Metric Alert Definition parameters, metric expressions, metric data, and Metrics APIs are excluded.

## Scope

* Load definitions directly by ID and expose recoverable list, details, instance, group, and notification failures.
* Use `CLUSTER.TOGGLE_ALERTS` for list toggles and `SERVICE.TOGGLE_ALERTS` for definition detail edits, toggles, and the direct create route.
* Keep Manage Groups and Manage Settings visible without an independent permission gate, gate Notifications with `CLUSTER.MANAGE_ALERT_NOTIFICATIONS`, and gate the Create menu with `supports.createAlerts`.
* Add the three-step `/main/alerts/add/:stepNumber` wizard for Port, Web, Script, and Aggregate definitions, with validation, read-only review, exact request construction, recoverable submission, and back/forward navigation.
* Keep Metric and RAW creation out of the wizard. Document the working Script and Aggregate paths as intentional repairs beyond broken Classic behavior.
* Query `/alert_history` for the previous 24 hours and map record counts by host instead of using the current instance update timestamp.
* Validate labels, interval/timeout values, parameters, thresholds, repeat tolerance, and `DEBUG`; preserve description and source/config values in update payloads.
* Coordinate dirty label/config edits with Save, Discard, and Cancel behavior and retain edit state after failed saves.
* Open and copy Alert Instance response text literally without HTML interpretation.
* Build complete Alert Group create/update replacement payloads containing name, definition IDs, and notification target IDs.
* Keep group definition and target changes local until Save; run all deletes first, then updates and creates, wait for every result, aggregate failures, and keep the original editor open after partial failure.
* Map Email, SNMP, Custom SNMP, and Alert Script UI models to their exact server types and built-in properties.
* Preserve an unchanged sensitive Email password on edit, support explicit replacement, and reject custom property names that conflict with built-in keys.
* Await global repeat-tolerance configuration saves, preserve `DEBUG`, and retain the dialog after failure.
* Remove Alerts debugging output from touched code and add focused tests for request shapes, state transitions, validation, permissions, concurrency, and failure recovery.

## Classic UI Baseline

The acceptance baseline is `docs/frontend-refactor/ember-baseline/05-alerts.md`, feature IDs `ALERT-LIST-001` through `ALERT-SET-003`. The detailed React comparison, reverse API contract, permission matrix, five-pass audit, compatibility decisions, and live runtime matrix are recorded in `docs/frontend-refactor/react-current/05-alerts-gap.md`.

The baseline was corrected during the audit:

* Classic contains an Alert Definition delete controller method and AJAX definition, but no inspected UI call site. It is unreachable dead code, not a user-facing workflow.
* Port and Web creation have functional-looking paths. RAW has no renderer; Script dereferences missing wizard data; Aggregate omits common fields and dereferences a missing definition name.
* Classic Alert Groups contain a name, definitions, and notification targets but no description field.

The authoritative network comparison includes global AJAX definitions and call sites, direct HTTP and browser entry points, routes, permissions, feature flags, and realtime destinations. Group membership is represented by replacement arrays on Alert Group create/update, not by child membership resources.

## Acceptance Criteria

* Alert list data remains coherent across polling and navigation, displays load failure and Retry, and applies filters and sorting without losing state.
* List toggles require `CLUSTER.TOGGLE_ALERTS`; details and direct creation require `SERVICE.TOGGLE_ALERTS`; Create and Notifications obey their independent feature and permission gates.
* Groups and Settings remain reachable by users without toggle or notification permissions.
* Details load the exact definition by ID, including definitions without non-default group membership.
* Label and non-Metric configuration edits validate input, preserve description/source fields, await saves, keep failed edits retryable, and protect dirty navigation with Save, Discard, and Cancel.
* Alert Instance polling stops on exit, survives a failed request, and displays exact 24-hour history counts per host.
* Response text containing markup or script-like content is copied and opened as literal text.
* Port, Web, Script, and Aggregate creation validates each step, shows the exact payload at review, submits once, and retains the wizard after failure; Metric and RAW types are not offered.
* Alert Group create/update payloads contain complete numeric `definitions` and `targets` arrays and never clear associations during rename or copy.
* Group Save waits for concurrent deletes before concurrent updates/creates, waits for every started request, reports aggregate errors, and preserves pending state after any failure.
* Email maps to `EMAIL`, built-in SNMP to `AMBARI_SNMP`, Custom SNMP to `SNMP`, and Alert Script to `ALERT_SCRIPT` with exact Classic property names.
* An authenticated Email edit without a replacement password retains the existing sensitive property; custom properties cannot conflict with corrected built-in properties.
* Notification create, edit, enable/disable, and delete failures retain selection and expose Retry.
* Per-definition and global repeat tolerance accepts 1 through 99 or `DEBUG`, preserves the sentinel, and does not close before a successful save.
* Focused tests cover exact URLs, methods, queries, payloads, creation, history mapping, validation, dirty decisions, permission policy, safe response handling, group sequencing, notification conversion, sensitive edits, and rejected operations.
* The applicable runtime matrix in `docs/frontend-refactor/react-current/05-alerts-gap.md` passes against a real Ambari Server.

## Compatibility Decisions

The React implementation must not reproduce known broken or unsafe Classic behavior:

* It does not add Alert Definition deletion UI solely because an unreachable Classic controller method exists.
* It repairs Script and Aggregate creation rather than reproducing their broken Classic wizard data paths; this is identified as `IMPROVED_BEYOND_CLASSIC`.
* It does not add an Alert Group description field that is absent from the inspected Classic UI and request contract.
* It waits for mutation results and preserves retryable state instead of reproducing Classic's optimistic close or incomplete rollback behavior.
* It opens server-provided response data through text nodes rather than `document.write()`.
* It preserves unchanged sensitive values instead of submitting empty credentials.

These are intentional compatibility corrections, not missing parity.

## Out of Scope

* Metric Alert Definition parameters, metric expressions, metric data, Metrics APIs, Metrics routes, charts, and widgets.
* RAW creation, because the Classic type is an unimplemented placeholder.
* New Alert Definition deletion UI, because the Classic method is unreachable dead code.
* Alert Group descriptions, because the inspected Classic contract has no such field.
* Host, Service, upgrade, installation, Kerberos, and HA mutation implementations owned by other modules; this issue validates only their documented Alerts presentation boundary.

## Verification Boundary

Static code and focused tests are not sufficient to complete runtime acceptance. The live matrix must still cover polling and realtime convergence, permission-role combinations, direct URL entry, dirty browser navigation, real Server validation, group partial failures, notification round trips, masked sensitive properties, maintenance interactions, and every supported non-Metric creation type.
