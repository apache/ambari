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

# React Service Theme and Configuration Layout Gap Analysis

## Comparison Scope

This document compares the React implementation under `ambari-web/latest`
with the executable Classic behavior and metadata contract recorded in
`ember-baseline/14-service-theme-layout.md`. The reviewed snapshot is based on
commit `ccac716f9a1a7942887da697b90e7818f9bb1195` plus the reviewed Module 14
changes on the unified frontend-refactor branch. Re-run this audit against the
eventual PR commit before changing any status to complete.

Service Theme is the stack/service-author JSON extension point for composing
the operational configuration form. It is not a user drag-and-drop editor and
is not the Metrics Widget framework. Metrics dashboards, charts, Heatmaps, and
their layout APIs remain excluded.

Matching a route, component, or parser function is not parity. A contract is
complete only when metadata identity, layout, Widget value round trips,
conditions, permissions, canonical saves, failure recovery, and executable
tests all agree with Module 14.

## Audit Status

The 40 executable contracts are mutually exclusive in this checkpoint. No
contract is `COVERED`.

| Status | Count | Meaning |
| --- | ---: | --- |
| `PARTIAL` | 39 | Frontend or server tests execute, but the complete fixture, interaction, failure, recovery, or integration matrix is not proved |
| `MISSING` | 1 | No real-cluster acceptance coverage exists |
| Total | 40 | All `SVC-THEME-TEST-*` IDs |

The most important blockers are:

| Affected contract | Current React behavior | Required correction |
| --- | --- | --- |
| `SVC-THEME-MODEL-004`, `PLACEMENT-002`, `TEST-009/010` | Layout-qualified tab IDs and ambiguous-target rejection are executable; missing targets remain Advanced/condition eligible like real HIVE | Extend parent-qualified identity through all active/error/filter state and test direct/nested UI-only synthesis plus duplicate declarations |
| `SVC-THEME-PLACEMENT-006`, `COND-005`, `TEST-014` | Static and dynamic negative attributes now share semantic inversion and preserve base restrictions | Exercise every source mode, repeated canonical updates, recommendations, lazy/filter state, raw controls, and spinner changes |
| `SVC-THEME-COND-009`, `COND-010`, `TEST-016` | The closed evaluator now returns structured invalid/missing-reference diagnostics and skips unsafe actions, preserving prior safe attributes | Surface diagnostics at the consumer boundary and expand the complete shipped-expression/hostile-input matrix |
| `SVC-THEME-ATTR-008` | Configuration-value debug logging was removed; Test Connection diagnostics redact configured and key-labelled secrets; comparison excludes passwords | Extend the executable secrets regression to every action, log boundary, and server response shape |

## Feature ID Status

This canonical table gives every non-test Module 14 ID an explicit matrix status.
The detailed checkpoints and executable test rows below provide the supporting
evidence and remaining boundary. All rows remain partial until their linked
acceptance contracts are complete.

| Feature ID | Status | React evidence and remaining boundary |
| --- | --- | --- |
| `SVC-THEME-SCOPE-001` | `PARTIAL` | The React boundary and Metrics exclusion are documented; end-to-end Theme acceptance remains incomplete. |
| `SVC-THEME-SCOPE-002` | `PARTIAL` | The React boundary and Metrics exclusion are documented; end-to-end Theme acceptance remains incomplete. |
| `SVC-THEME-SCOPE-003` | `PARTIAL` | The React boundary and Metrics exclusion are documented; end-to-end Theme acceptance remains incomplete. |
| `SVC-THEME-SCOPE-004` | `PARTIAL` | The React boundary and Metrics exclusion are documented; end-to-end Theme acceptance remains incomplete. |
| `SVC-THEME-API-001` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-002` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-003` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-004` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-005` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-006` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-API-007` | `PARTIAL` | Theme loading and provider adapters exist; the complete server projection, predicate, error, and live-response matrix remains incomplete. |
| `SVC-THEME-MAP-001` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-002` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-003` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-004` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-005` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-006` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-007` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MAP-008` | `PARTIAL` | Representative stack Theme parsing and inheritance exist; exhaustive merge, removal, invalid-child, and custom-stack behavior remains incomplete. |
| `SVC-THEME-MODEL-001` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-002` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-003` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-004` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-005` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-006` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-007` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-MODEL-008` | `PARTIAL` | The shared normalizer implements the graph and identity path; complete malformed, duplicate, cache, and service-isolation coverage remains incomplete. |
| `SVC-THEME-LAYOUT-001` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-002` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-003` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-004` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-005` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-006` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-007` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-008` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-LAYOUT-009` | `PARTIAL` | React renders Theme layout structure; the complete grid, focus, visibility, nested-tab, and responsive contract remains incomplete. |
| `SVC-THEME-PLACEMENT-001` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-PLACEMENT-002` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-PLACEMENT-003` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-PLACEMENT-004` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-PLACEMENT-005` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-PLACEMENT-006` | `PARTIAL` | Full-path placement and Advanced fallback exist; duplicate, UI-only synthesis, and layered-attribute coverage remains incomplete. |
| `SVC-THEME-WIDGET-001` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-002` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-003` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-004` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-005` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-006` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-007` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-008` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-009` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-010` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-011` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-012` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-013` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-WIDGET-014` | `PARTIAL` | The Widget dispatcher and core controls exist; the complete type, conversion, raw-mode, and exact round-trip matrix remains incomplete. |
| `SVC-THEME-ATTR-001` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-002` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-003` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-004` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-005` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-006` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-007` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-ATTR-008` | `PARTIAL` | React applies Theme attributes and secret handling; complete source, interaction, logging, and save-boundary coverage remains incomplete. |
| `SVC-THEME-PERM-001` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-PERM-002` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-PERM-003` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-PERM-004` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-PERM-005` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-PERM-006` | `PARTIAL` | Permission and read-only paths exist; the full role, mode, override, final, and consumer matrix remains incomplete. |
| `SVC-THEME-COND-001` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-002` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-003` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-004` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-005` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-006` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-007` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-008` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-009` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-COND-010` | `PARTIAL` | The closed condition evaluator and ordered mutations exist; complete shipped-expression, dependency-source, and consumer-diagnostic coverage remains incomplete. |
| `SVC-THEME-CONSUME-001` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-002` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-003` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-004` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-005` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-006` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-007` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-008` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-009` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `SVC-THEME-CONSUME-010` | `PARTIAL` | A focused React consumer path exists; its complete load, switch, failure, retry, persistence, and save contract remains incomplete. |
| `INST-7-THEME-001` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-002` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-003` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-004` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-005` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-006` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-007` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-008` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-009` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-010` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-011` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-012` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-013` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `INST-7-THEME-014` | `PARTIAL` | Installer/Add Service Step 7 has Theme-aware behavior; complete category, recommendation, validation, re-entry, and payload coverage remains incomplete. |
| `SVC-THEME-FALLBACK-001` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-002` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-003` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-004` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-005` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-006` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-007` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |
| `SVC-THEME-FALLBACK-008` | `PARTIAL` | Scoped fallback and retry behavior exists; complete response classification, cancellation, cache, and stale-write coverage remains incomplete. |

## Contract Area Checkpoint

These are area-level checkpoints, not substitutes for the individual baseline
IDs. No row is a completion claim.

| Baseline area | Current React evidence | Remaining acceptance gaps |
| --- | --- | --- |
| `SVC-THEME-API-*` | Service, Host, Installer, and comparator call sites contain Theme loading paths and response adapters; provider tests cover default/named-file filtering, nested projection, collection/batch predicates, errors, and primary-key identity | REST child isolation, order independence, single/batch equivalence across real stacks, and custom Theme integration remain incomplete |
| `SVC-THEME-MAP-*` | React consumes the server projection and does not attempt server inheritance; server tests cover nested additions/order, type-specific removals/replacements, deleted descriptors, parse failures, and parent isolation | Valid-sibling/custom-directory loading, invalid-child fallback, REST ordering, and real custom Theme integration remain incomplete |
| `SVC-THEME-MODEL-*` | A shared normalizer parses all returned layouts, keeps full paths, layout-qualifies duplicate tab names, creates Advanced fallback, and isolates malformed siblings | Complete graph identity, diagnostics, cache/reset, and service/file isolation are incomplete |
| `SVC-THEME-LAYOUT-*` | CSS grid coordinates, spans, declaration order, one splitter, nested tabs, first-visible top-tab selection, hidden-active handoff, lazy Pane rendering, filter propagation, and visible-only errors have executable evidence | Focus order, empty cells, overlap, top splitters, keyboard behavior, and responsive viewport behavior remain incomplete |
| `SVC-THEME-PLACEMENT-*` | Full config paths, direct/nested placement, orphan Advanced-only retention, and ambiguous layout-attachment rejection are executable | Duplicate declarations, nested UI-only synthesis, and the full layered-attribute matrix are incomplete |
| `SVC-THEME-WIDGET-*` | All dispatcher cases exist in source; entry/directory controls, checkbox mappings, slider unit/percent conversion, config-group bounds, raw-unit labels, zero values, precision, and unsupported Widget state have tests | The complete 14-type table, secrets, slider recommendation marker and slide-stop boundary, time composition, final/recommendation/override behavior, and exact save round trips are not proved |
| `SVC-THEME-ATTR-*`, `PERM-*` | Installed-service inputs honor `SERVICE.MODIFY_CONFIGS`, `read_only`, `editable_only_at_install`, parent final state, and config-group ownership; group override values/final flags save independently; Installer/Add Service retains its wizard edit boundary; UI-only properties are excluded | The complete role/compare/Host/recommendation/debounce matrix and password-log regression coverage remain incomplete |
| `SVC-THEME-COND-*` | Config/service conditions, closed parsing, structured unsafe/missing diagnostics, ordered attributes, and some container visibility execute without `eval` | Consumer diagnostics, repeated parent names, all shipped expressions, every canonical dependency source, and complete visibility layers are incomplete |
| `SVC-THEME-CONSUME-*` | Service Configs, Host Configs, comparison, Installer credentials, and Step 7 have focused partial evidence | No consumer satisfies its complete load/switch/retry/save matrix; Add Service and canonical payload integration remain especially incomplete |
| `SVC-THEME-FALLBACK-*` | Service and Host retries are scoped; Service ordinary/Theme responses reject prior-service results; Step 7 recompiles a successful retry while preserving edits; comparator effects reject stale writes | Empty/malformed distinctions, stack-version changes, request deduplication, cache invalidation, and active-item handoff remain incomplete |
| `INST-7-THEME-*` | Credentials bind exact paths; Step 7 has Theme fallback/edit-preserving Retry, skips disabled categories, and includes Add Service persistence-before-navigation evidence | Five-category validation, recommendations, service switching, Review payload, re-entry/refresh, and full Add Service behavior remain incomplete |

## Executable Test Matrix

Every row below is required before this module can be declared migrated. A
partial row must not be promoted because one assertion in the scenario passes.

| ID | Status | Current executable evidence | Missing or conflicting coverage |
| --- | --- | --- | --- |
| `SVC-THEME-TEST-001` | `PARTIAL` | `themeEngine.test.ts` reads the real HIVE default Theme and asserts its ordered layout, graph, placements, Widgets, and Advanced tab | Orphan Advanced-only placement, UI-only DB action, and Metrics exclusion are not asserted together |
| `SVC-THEME-TEST-002` | `PARTIAL` | The real HIVE directories Theme is parsed despite its `configuration-layout` mismatch | Database and credentials isolation, exact required-properties, and all canonical links are incomplete |
| `SVC-THEME-TEST-003` | `PARTIAL` | A real Ranger Theme contributes nested-tab and placement evidence | Full Ranger grid, every nested tab and condition, parent-qualified identities, and independent collections are not proved |
| `SVC-THEME-TEST-004` | `PARTIAL` | Ranger DB payload helpers cover normal and root actions | Real Ranger/Ranger KMS Theme compilation, distinct action identity, exact metadata, and UI-only save exclusion are incomplete |
| `SVC-THEME-TEST-005` | `PARTIAL` | Real YARN and selected real Theme fixtures are parsed | Complete YARN layout plus MAPREDUCE2 `themes-mapred` discovery, declaration order, service conditions, sliders, toggles, and directories are not proved |
| `SVC-THEME-TEST-006` | `PARTIAL` | A malformed sibling is isolated | Single/batch semantic equivalence, omitted/empty/non-array collections, mixed service results, and service-scoped diagnostics are incomplete |
| `SVC-THEME-TEST-007` | `PARTIAL` | Multiple ordered layouts, layout-qualified duplicate tab names, a mismatch, real HIVE directories, and no-layout diagnostics execute | Add an explicit missing-field case and prove all matching/mismatched values remain non-selecting source metadata |
| `SVC-THEME-TEST-008` | `PARTIAL` | Exact config paths and a slash-containing property have unit evidence | Repeat the same basename across services and every placement, condition, Widget, save, and comparison lookup |
| `SVC-THEME-TEST-009` | `PARTIAL` | Synthetic nested-tab fixtures exercise some repeated local structure | Complete service/Theme/parent-qualified identity across active state, errors, placements, and Widgets is not proved |
| `SVC-THEME-TEST-010` | `PARTIAL` | Direct/nested targets execute; ambiguous targets are diagnosed and not layout-attached; real missing targets remain canonical/Advanced eligible | Duplicate declarations, explicit nested UI-only synthesis, and ordinary missing-property rejection are not covered |
| `SVC-THEME-TEST-011` | `PARTIAL` | Malformed and unknown Widget cases retain a controlled fallback | Wrong field types, invalid geometry, malformed required-properties, missing identity/type, diagnostics, and valid-sibling retention need a table-driven matrix |
| `SVC-THEME-TEST-012` | `PARTIAL` | Boolean atoms, conjunction precedence, and representative malformed expressions execute | Disjunction/mixed precedence, whitespace, every shipped expression, missing/no-token atoms, rejected operators, and structured diagnostics are incomplete |
| `SVC-THEME-TEST-013` | `PARTIAL` | Installed and selected-service condition cases execute | Case mismatch, absent services, changing sets, and recomputation in existing/Installer/Add Service contexts are incomplete |
| `SVC-THEME-TEST-014` | `PARTIAL` | Ordered condition mutation and semantic `read_only`/`ui_only_property` inversion have unit coverage | All canonical source modes and repeated updates remain untested |
| `SVC-THEME-TEST-015` | `PARTIAL` | Base and container visibility conjunction has tests | Parent-qualified duplicates, filter, permission/read-only layers, and independent removal of every visibility layer are incomplete |
| `SVC-THEME-TEST-016` | `PARTIAL` | Unsupported syntax, missing paths, code-like/prototype-shaped input, structured diagnostics, and preservation of prior safe attributes execute without dynamic evaluation | Add consumer-visible diagnostics and explicit spies for every forbidden dynamic execution API |
| `SVC-THEME-TEST-017` | `PARTIAL` | Section CSS grid/span and non-monotonic Section/SubSection declaration order are asserted | Empty cells, overlap, validation stability, and semantic focus order are absent |
| `SVC-THEME-TEST-018` | `PARTIAL` | One SubSection span, border, and left splitter are asserted | Inner dimensions, top splitter, title gaps, simultaneous spans, responsive viewport, and focus-order stability are absent |
| `SVC-THEME-TEST-019` | `PARTIAL` | Component tests cover first-visible selection, disabled-click rejection, hidden-active handoff, Advanced reachability, lazy Pane rendering, and the all-empty state | Keyboard behavior, focus transfer, real multi-layout Themes, and responsive rendering remain incomplete |
| `SVC-THEME-TEST-020` | `PARTIAL` | Synthetic nested tabs can be selected and conditionally hidden; hiding the active tab hands off to the first visible sibling | Real Ranger three-tab fixture, keyboard behavior, all-hidden state, independent errors, and parent identity are absent |
| `SVC-THEME-TEST-021` | `PARTIAL` | Tests cover name, display name, description, saved/current/override value and group search, AND-combined property filters, upward visibility, and visible-only error counts | Real nested/duplicate layouts, permission changes, and keyboard/focus behavior remain incomplete |
| `SVC-THEME-TEST-022` | `PARTIAL` | Entry/list/radio/directory/directories/label and controlled unknown cases have tests | No table dispatches all 14 supported types plus missing/unknown and verifies the required component/error boundary |
| `SVC-THEME-TEST-023` | `PARTIAL` | Widget tests cover Ember checkbox pairs (`true/false`, `Yes/No`, `YES/NO`, and `yes/no`), inverted semantics, editable entries, radio selection, and exact/range/unbounded/ALL list cardinality | Toggle interaction round trips, combo unknown/editability, radio dependencies, and the complete list/raw matrix are incomplete |
| `SVC-THEME-TEST-024` | `PARTIAL` | Text and directory-family rendering has partial DOM evidence | Password confirmation/secrecy, text-area, exact delimiters, validation, label non-saveability, and all round trips are incomplete |
| `SVC-THEME-TEST-025` | `PARTIAL` | Tests cover B-KB-MB-GB-TB and integer/float percent conversion, zero bounds/steps, precision, config-group min/max/increment overrides, and canonical raw-unit labels | Recommendation markers, recommendation/undo integration, true slide-stop boundaries, raw round trips, and exact saves remain incomplete |
| `SVC-THEME-TEST-026` | `PARTIAL` | Configured spinner units render and the implementation retains a configured milliseconds field | Composition, cap/overflow, modulo, min/max, raw, exact-save, and live-condition tests remain absent |
| `SVC-THEME-TEST-027` | `PARTIAL` | Theme and Advanced controls cover installed-service permission, read-only/install-only attributes, parent final state, independent group override values/final flags, default/non-default group editing, and Installer/Add Service edit mode; Host disables its UI-only connection action | Recommendation/undo/debounce/raw-mode and full Host/compare interaction coverage remain incomplete |
| `SVC-THEME-TEST-028` | `PARTIAL` | Payload and component tests cover HIVE and Ranger normal/root, full-path collision handling, Theme JDK fields, and installed versus uninstalled custom-action API paths | Ranger KMS, multiple hosts, and an explicit password-log exclusion spy are incomplete |
| `SVC-THEME-TEST-029` | `PARTIAL` | DB action tests cover request creation, polling failures, missing IDs, terminal recovery, missing structured success, and redacted diagnostic rendering | Stale and unmounted response handling plus the complete server-output shape matrix are incomplete |
| `SVC-THEME-TEST-030` | `PARTIAL` | Service Configs covers selected-service requests, fallback, Retry, edit retention, service switching, and rejection of late Theme, configuration, and value responses | Empty/404/500/malformed distinctions, stack-version changes, and request deduplication are incomplete |
| `SVC-THEME-TEST-031` | `PARTIAL` | Host Configs covers traditional-only component filtering without pruning Theme paths, Theme-failure Advanced fallback, and scoped `Retry Theme` recovery | The test mocks the actual renderer; complete read-only Theme graph, selected group, and all non-editable controls are not proved |
| `SVC-THEME-TEST-032` | `PARTIAL` | Comparator tests use real HIVE data, default selection, full paths, declaration order, fallback, basic DOM, and added/removed password exclusion | Added/removed/unchanged non-secret fields, nested flattening, filters, `Undefined`, UI-only controls, and stale-response interaction coverage remain incomplete |
| `SVC-THEME-TEST-033` | `PARTIAL` | Credentials exact paths, disabled-category skipping, and edit-preserving new-install Theme fallback/Retry have tests | Five-category availability, service switch, conditions, full validation, Review payload, re-entry, and refresh persistence are incomplete |
| `SVC-THEME-TEST-034` | `PARTIAL` | Add Service preserves current values before a category jump | Ordinary template, installed/new merge, AddService recommendations, groups, Kerberos, dependent changes, exact tags, and fallback/retry are incomplete |
| `SVC-THEME-TEST-035` | `PARTIAL` | `buildConfigsJSON` excludes Theme UI-only properties and confirmation values; service-scoped collection prevents same-filename properties from other services entering a payload; canonical same-named properties retain full-path identity | A real consumer save/reload test must still prove exactly one occurrence for every edited config across default and non-default groups |
| `SVC-THEME-TEST-036` | `PARTIAL` | Real Theme and Advanced renderers cover view-only versus modify, read-only/install-only, parent-final, config-group override, and Installer/Add Service editing; Host disables its UI-only action | Compare and full Host real-renderer matrices plus all action/undo/recommendation states remain incomplete |
| `SVC-THEME-TEST-037` | `PARTIAL` | `ThemeModuleTest` covers missing, syntax, binding, deleted-descriptor, inheritance, and parent-isolation behavior and passed in this checkpoint | Log-only valid-sibling, custom-directory, semantically malformed, and invalid-child fallback integration remain incomplete |
| `SVC-THEME-TEST-038` | `PARTIAL` | `ThemeMergeTest` covers nested additions/order, presentation-only subsection overrides, conditions/tabs, exact placement/Widget replacement, type-specific removals, and null-parent safety; `ThemeModuleTest` covers deletion and parent isolation; both passed | Exhaustive scalar/list permutations and real inherited custom-stack integration remain incomplete |
| `SVC-THEME-TEST-039` | `PARTIAL` | `ThemeArtifactResourceProviderTest` has 14 passing tests for nested projection, logical/file identity, collection and batch predicates, default filtering, whole-stack behavior, missing stack/service/file errors, primary-key IDs, and unsupported mutations | REST child-resource isolation, ordering through the full request framework, and real/custom-stack integration remain incomplete |
| `SVC-THEME-TEST-040` | `MISSING` | No automated real-cluster evidence was found | Install/Add Service, all named service fixtures, custom service, groups, Kerberos, injected failures, retry, save/reload, and comparison remain required |

## Test Execution Checkpoint

The aggregate frontend command completed successfully during this checkpoint:

```bash
npm test -- --run
```

Result: 185 test files and 976 tests passed. The run includes Theme engine,
renderer, Service Configs, Host Configs, Config Versions, Installer/Add Service,
permissions, config-group override/final, and save-payload coverage. Vitest
emitted existing `TimeoutNaNWarning` messages but returned zero failures.

The following earlier focused command remains useful for a faster Theme-only
audit:

```bash
npx vitest run \
  src/screens/CommonConfigs/themeEngine.test.ts \
  src/screens/CommonConfigs/ConfigUtils.theme.test.ts \
  src/screens/CommonConfigs/ThemeWidgetControls.test.tsx \
  src/screens/CommonConfigs/Config.theme.test.tsx \
  src/screens/CommonConfigs/testConnectionUtils.test.ts \
  src/screens/CommonConfigs/TestConnection.test.tsx \
  src/screens/ConfigVersions/configsComparatorTheme.test.ts \
  src/screens/ConfigVersions/ConfigsComparator.theme.test.tsx \
  src/screens/Hosts/HostConfigs.test.tsx \
  src/screens/ServiceConfigs/ServiceConfigs.theme.test.tsx \
  src/screens/ClusterWizard/Step7/index.test.tsx \
  src/screens/ClusterWizard/Step7/CredentialsTab.test.tsx
```

Result: 12 test files and 120 tests passed. This run covers the shared Theme
engine and controls plus Service Configs, Host Configs, Config Versions
comparison, Installer/Add Service Step 7, and Test Connection. Vitest emitted
one existing `TimeoutNaNWarning` but returned zero failures.

The focused server Theme command also completed successfully in a JDK 17
Maven container:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace \
  maven:3.9.11-eclipse-temurin-17 \
  mvn \
  -pl ambari-server \
  -Dtest=ThemeMergeTest,ThemeModuleTest,ThemeArtifactResourceProviderTest \
  -Dcheckstyle.skip \
  -Drat.skip=true \
  -DskipPythonTests test
```

Result: 21 tests passed with no failures, errors, or skips: 2 merge tests, 5
module tests, and 14 provider tests.

Before the module can be promoted, repeat the aggregate suite at the eventual
PR commit, extend the remaining server/provider integration boundaries, and
record `TEST-040` against a real cluster. A passing aggregate still does not
close a row unless all assertions listed in Module 14 are executable.
