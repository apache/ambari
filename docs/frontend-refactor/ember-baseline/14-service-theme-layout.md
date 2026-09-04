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

# Service Theme and Configuration Layout Module

## Scope and Acceptance Meaning

Service Theme is the stack-metadata extension point that lets a stack,
common-service, extension, or custom service author compose the operational
configuration UI from JSON. It controls named layouts, tabs, sections, grid
positions, subsections, nested subsection tabs, property placement, Widget
selection, value attributes, and conditional attribute changes. It is therefore a
runtime metadata compiler contract, not a fixed list of built-in Ambari
services or React components.

This is not a user-editable drag-and-drop layout. The author changes the Theme
files shipped with a stack or service; Ambari Server parses, binds, inherits,
and serves the merged artifact, but does not semantically validate the layout;
the frontend renders it. A React implementation
must consequently work for custom metadata that was not known when the React
bundle was built.

Metrics Dashboard Widgets, Metrics Widget layouts, Heatmaps, and metric/time
series display are `OUT_OF_SCOPE`. The word "Widget" in this module means an
operational configuration editor under `common/configs/widgets`, not a Metrics
Widget.

| ID | Baseline requirement | Acceptance boundary |
| --- | --- | --- |
| SVC-THEME-SCOPE-001 | Compiles Service Theme metadata into a service configuration form without a service-name whitelist | A custom stack service receives the same behavior as a built-in service when it supplies the same metadata |
| SVC-THEME-SCOPE-002 | Keeps the canonical saved configuration collection separate from Theme presentation metadata | Theme placement and Widget state must not invent, rename, or silently drop an Agent configuration property |
| SVC-THEME-SCOPE-003 | Treats the server-merged Theme as the input contract while still validating it defensively in the browser | Client validation isolates a bad Theme and preserves ordinary configurations; it does not attempt to reproduce server inheritance |
| SVC-THEME-SCOPE-004 | Excludes Metrics Dashboard/Heatmap definitions and user Widget-layout APIs | Configuration Widgets remain in scope even when their class or JSON field is named `widget` |

`LEGACY_PARITY` below means preserving user-visible Classic behavior.
`METADATA_CONTRACT` means honoring the extensible server model even where the
Classic mapper only worked for the current metadata inventory. `BEHAVIOR_DIFF`
means an intentional recovery or correctness improvement over a documented
Classic defect. A React parity claim must identify which of these applies; a
matching route or page title is not evidence.

## REST API Contract

| ID | Request or resource | Response and behavior | Failure boundary | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-THEME-API-001 | `GET {stackVersionUrl}/services/{serviceName}/themes?ThemeInfo/default=true&fields=*` | Returns a top-level `items` array of Theme resources for one service | Stack/version/service must exist; a collection with no matching Theme normally becomes 404 | `app/utils/ajax/ajax.js#configs.theme`; `themes_mapping.js#loadConfigTheme` |
| SVC-THEME-API-002 | `GET {stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&themes/ThemeInfo/default=true&fields=themes/*` | Returns service resources with `StackServices` and a nested Theme collection; Installer and Add Service flatten the child resources | A child `NoSuchResourceException` is caught per service, leaving that service's nested collection empty without failing valid siblings or the outer batch | `ajax.js#configs.theme.services`; `QueryImpl#getSubResources` |
| SVC-THEME-API-003 | Theme resource fields | Exposes `ThemeInfo/stack_name`, `stack_version`, `service_name`, `file_name`, `default`, and parsed `theme_data` | `file_name` is the logical route/resource key; `theme_data.Theme` is the parsed JSON object, not a JSON string. The provider's `getPKPropertyIds()` incorrectly returns `null`, which requires a regression test | `ThemeArtifactResourceProvider#propertyIds`, `getPKPropertyIds` |
| SVC-THEME-API-004 | Collection ordering | Treats resource order as non-semantic and selects by exact metadata | The provider prepends defaults internally, but normal REST processing re-sorts unsorted resources by key/file name. Clients must not depend on default-first or discovery order; all currently declared non-Metrics artifacts are default anyway | `ThemeArtifactResourceProvider#getThemes`; `ClusterControllerImpl#getResources` |
| SVC-THEME-API-005 | Empty and missing resources | A zero-resource collection throws `NoSuchResourceException`; an unknown stack/service throws `NoSuchParentResourceException` | A named unknown file can currently append `null` and fail during projection, producing a server error rather than a clean 404. React must recover from either response | `ThemeArtifactResourceProvider#getResources`, `getThemes` |
| SVC-THEME-API-006 | Mutation | Create, update, and delete throw `UnsupportedOperationException` | There is no frontend Theme save endpoint; configuration values continue through the ordinary config-version APIs | `ThemeArtifactResourceProvider#createResources`, `updateResources`, `deleteResources` |
| SVC-THEME-API-007 | Field and Theme selection | The client requests fields explicitly, retains exact service/file/Theme names for diagnostics, and compiles only artifacts returned by the default filter | Route name or service name is not a substitute for `Theme.name`; unrelated artifacts must not be merged client-side | AJAX definitions; `themes_mapper.js` call sites |

The single-service response shape is:

```json
{
  "items": [
    {
      "ThemeInfo": {
        "stack_name": "BIGTOP",
        "stack_version": "3.2.0",
        "service_name": "HIVE",
        "file_name": "theme.json",
        "default": true,
        "theme_data": { "Theme": { "name": "default" } }
      }
    }
  ]
}
```

The batch response instead nests the same Theme resources below each service.
Server query expansion isolates a missing child collection per service. A
normalizer must therefore accept an empty or omitted `themes` collection as
"no Theme for this service", reject a non-array value with a service-scoped
diagnostic, and continue normalizing valid siblings.

Theme resources are read-only presentation metadata. The same canonical config
objects used by the enhanced form continue through the ordinary configuration
APIs; rendering a Theme must not create an alternative persistence path:

| Purpose | Classic request/endpoint | Theme-specific boundary |
| --- | --- | --- |
| Load stack properties for services | `GET {stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&fields=configurations/*` (`config.advanced.multiple.services`) | Must complete before batch Theme placement is linked |
| Load current or selected config versions | `GET /clusters/{cluster}/configurations/service_config_versions?...&fields=*` (`configs.config_versions.*`, `service.serviceConfigVersions.*`) | Exact type/property/group values populate the canonical collection after Theme presentation is known |
| Load config groups | `GET /clusters/{cluster}/config_groups?...` (`service.load_config_groups`, `config.tags_and_groups`) | Theme Widgets reuse ordinary default/non-default group and override semantics |
| Get recommendations and validation | `POST {stackVersionUrl}/recommendations`; `POST {stackVersionUrl}/validations` | Requests use canonical values and the correct ClusterCreate/AddService/config-dependencies context, not layout-local state |
| Save default-group values | `PUT /clusters/{cluster}` (`common.across.services.configurations`) | Payload contains desired configs/properties/attributes once; synthetic UI-only properties are excluded |
| Save non-default groups | `POST /clusters/{cluster}/config_groups`; `PUT /clusters/{cluster}/config_groups/{id}` | Override values remain attached to the selected group; Theme metadata itself is never written |

New-cluster Review creates cluster configurations/config groups through the
Step 8 dependency queue documented in the installation module. The DB-test
Widget's action/task endpoints are specified separately below because they are
the only action APIs introduced by a current configuration Widget.

## Server Loading, Validation, Inheritance, and Deletion

A service declares Theme descriptors with `fileName`, `default`, and `deleted`;
`themes-dir` defaults to `themes` but may be overridden, as MAPREDUCE2 does with
`themes-mapred`. `ThemeModule` reads each non-deleted file with Jackson into the
classes under `org.apache.ambari.server.state.theme`. Only missing files and
JSON I/O/binding failures invalidate a module. Names, targets, references,
coordinates, spans, Widget types, and required-property keys are not
semantically validated, and most model classes ignore unknown fields.
`ServiceModule` logs and filters a parse-invalid Theme without invalidating the
complete service; the error is not exposed through `ServiceInfo` or the REST
resource. Inheritance matches exact descriptor `fileName`, not inner
`Theme.name`, and is performed on the server. The browser must not merge parent
and child files a second time.

| ID | Server behavior | Exact merge or validation rule | Primary evidence |
| --- | --- | --- | --- |
| SVC-THEME-MAP-001 | Parses and binds declared Theme JSON during stack loading | Missing files and Jackson I/O/binding errors mark that Theme module invalid. Bindable but semantically incomplete metadata can pass and must still be checked by the client | `stack/ThemeModule` constructor |
| SVC-THEME-MAP-002 | Isolates a parse-invalid Theme from its service | Invalid modules are logged and omitted from `ServiceInfo.themes`; valid siblings and the service remain. Module errors are log-only and are not an API diagnostic | `stack/ServiceModule#populateThemeModules`; `StackManagerExtensionTest` |
| SVC-THEME-MAP-003 | Inherits Theme files by exact file name | An absent child body inherits the parent body; a populated child Theme merges with the matching parent; a child `ThemeInfo.deleted=true` removes that file from `themesMap` | `ThemeModule#resolve`; `ServiceModule#mergeThemes` |
| SVC-THEME-MAP-004 | Applies type-specific inheritance/removal sentinels | Layout, Widget, tab, section, subsection, and placement removal predicates remain different. Merge methods tolerate missing parents for complete child additions; a presentation-only subsection override is retained rather than mistaken for deletion | `state/theme/*#mergeWithParent`, `isRemoved`; `ThemeMergeTest` |
| SVC-THEME-MAP-005 | Merges collections by metadata identity | Layouts/tabs/sections/subsections use `name`; Widgets and placements use exact `config`. All merge maps use insertion-preserving maps, so inherited declaration order is retained and complete child additions append in child declaration order | `ThemeConfiguration`, `Layout`, `TabLayout`, `Section`, `Placement`; `ThemeMergeTest` |
| SVC-THEME-MAP-006 | Applies field-specific scalar inheritance | Theme, tab, tab-layout, section, and existing-subsection missing fields inherit. A retained placement inherits only `subsection-name`; nested-tab target, attributes, and conditions replace rather than field-merge | `Theme`, `Tab`, `TabLayout`, `Section`, `Subsection`, `ConfigPlacement` |
| SVC-THEME-MAP-007 | Preserves resolved server-side Theme identity and default metadata | The API reads the resolved `ServiceInfo.themesMap`, keyed by file name. Existing extension tests inspect the pre-resolution `getThemes()` list and therefore do not prove API-map deletion/filtering | `ServiceInfo#getThemesMap`; `ThemeArtifactResourceProvider#getThemes`; `StackManagerExtensionTest` |
| SVC-THEME-MAP-008 | Preserves declaration order through inherited collection merges | Nested layout, tab, section, subsection, placement, and Widget merges use `LinkedHashMap`. React must still use the API array order and select resources by exact metadata rather than depending on REST collection or file discovery order | `Layout#mergeTabs`, `TabLayout#mergedSections`, `Section#mergeSubsections`; `ThemeMergeTest` |

The implemented deep-merge matrix is exact and intentionally recorded with its
type-specific removal rules. Focused Java tests cover its core additions,
removals, replacements, ordering, descriptor deletion, and parent isolation;
custom inherited Themes still need real-stack integration acceptance:

| Record | Identity and merge behavior | Removal or known hazard |
| --- | --- | --- |
| Theme file | Descriptor `fileName`; a child body merges with the same parent file | `ThemeInfo.deleted=true` skips the child file and removes that key from the resolved map. A parse-invalid child is filtered before merge, so its valid parent can survive |
| Theme/configuration | Non-null child `name`, `description`, and `configuration` win | `ThemeModule.resolve` deep-copies inherited parent Theme data before assigning or merging it into a child, preventing resolved child mutation from aliasing the parent. Semantic validation is still intentionally limited |
| Placement | `configuration-layout` inherits when absent; config entries merge by exact full `config` in insertion order | Missing child `subsection-name` means removal even if nested-tab target, attributes, or conditions are present. A retained entry inherits only `subsection-name` |
| Widget | Exact full `config`; a non-null child replaces the complete Widget entry | `widget=null` deletes. Widget type, units, display name, and required-properties do not field-merge |
| Layout and tab | Layout/tab `name`; existing entries deep-merge and complete new entries append | `tabs=null` deletes a layout; a tab with both display name and layout absent deletes |
| Section | Section `name`; existing scalars/subsections inherit and complete new sections append in declaration order | A section with every field other than identity absent removes |
| SubSection | SubSection `name`; existing missing fields inherit; `depends-on` and `subsection-tabs` replace as whole lists | Removal checks coordinates/spans, presentation fields, conditions, and nested tabs. An entry containing only `display-name`, `border`, or `left-vertical-splitter` is a retained override; a truly empty identity-only entry removes |

`ThemeModuleTest` covers representative inheritance, missing/syntax/binding
failures, deleted descriptors, and parent-object isolation. `ThemeMergeTest`
covers nested additions and declaration order at every collection level,
presentation-only subsection overrides, conditions and nested tabs, and
type-specific layout/tab/section/subsection/placement/Widget removals and
replacements. Valid-sibling/custom-directory stack integration and the full
semantically malformed matrix remain separate acceptance obligations.

## Theme Data Model and Client Compilation

The API wraps the raw file as `ThemeInfo.theme_data.Theme`. The inner object has
`name`, optional `description`, and `configuration`. Configuration contains
`layouts`, `placement`, and `widgets`. The server stores and inherits
`placement.configuration-layout`, but Classic never reads it when rendering;
`placement.configs` links exact properties to a target. Existing metadata
therefore cannot treat `configuration-layout` as a client-side selector.
The server's `ThemeResponse.theme_data` Java declaration does not model this
extra `Theme` wrapper correctly, so generated schema alone is not sufficient
wire-contract evidence.

| Server object | JSON fields accepted/projected by the current model |
| --- | --- |
| Theme | `name`, `description`, `configuration` |
| Configuration | `layouts`, `placement`, `widgets` |
| Placement entry | `config`, `subsection-name`, `subsection-tab-name`, `property_value_attributes`, `depends-on` |
| Layout/tab | layout `name`/`tabs`; tab `name`/`display-name`/`layout`; layout dimensions `tab-rows`, `tab-columns`, and `sections` |
| Section | `name`, `display-name`, row/column index and span, `section-rows`, `section-columns`, and `subsections` |
| SubSection | identity/display, row/column index and span, `border`, `left-vertical-splitter`, `depends-on`, and `subsection-tabs` |
| SubSectionTab | `name`, `display-name`, and `depends-on` |
| Condition | optional `resource`, `configs`, `if`, and `then`/`else.property_value_attributes`; omitted resource means config |
| Widget entry | exact `config` plus `widget.type`, `units[].unit-name`, string-map `required-properties`, and `display-name` |

Dimensions, coordinates, spans, units, bounds, and increments are represented
as strings in this model and are not range-checked by the server. React must
reject or isolate invalid types and impossible geometry without losing the
ordinary configuration collection.

Classic processes the response in this order: map every returned layout; link
placements; attach Widget metadata; load mapped records; and generate an
Advanced tab for requested services. It relies on stack configuration metadata
having been loaded first. Installer/Add Service explicitly load stack configs
before the batch Theme request. Installed Service, Host Configs, and version
comparison then reject categorized tabs (`Theme.name !== default`); those
database, credentials, and directories layouts remain available only to their
wizard categories.

| ID | Compilation behavior | Required React identity or boundary | Classic evidence and limitation |
| --- | --- | --- | --- |
| SVC-THEME-MODEL-001 | Normalizes each Theme independently | Theme identity is stack/version/service/file plus exact `Theme.name` | `themes_mapper.js#map`, `mapThemeLayouts` |
| SVC-THEME-MODEL-002 | Maps every declared layout | Preserve the returned layout order and compile every valid tab. Retain `placement.configuration-layout` only as source metadata; do not use it to discard layouts because Classic ignores it and shipped HIVE directories metadata names `default` while declaring only `directories` | `themes_mapper.js#mapThemeLayouts`; server `Placement.configurationLayout`; HIVE `directories.json` |
| SVC-THEME-MODEL-003 | Preserves exact config identity | Split at the first slash into config type and the complete remaining property name; include service in the runtime lookup boundary | `themes_mapper.js#getConfigId`; `App.config.configId` |
| SVC-THEME-MODEL-004 | Creates parent-qualified layout identities | Tab, Section, SubSection, and SubSectionTab identities include service, Theme, and every relevant parent; equal local names in two Themes cannot collide | Classic tab IDs omit Theme and subsection-tab IDs are only the local name, so collision is a known defect |
| SVC-THEME-MODEL-005 | Keeps placement-scoped Widget metadata | Two Themes may place the same config with different presentation without one overwriting the other | Classic stores `widget` on the global stack config property, so later mapping can overwrite earlier metadata |
| SVC-THEME-MODEL-006 | Generates a service Advanced tab after mapping | One Advanced tab exists per requested service and exposes ordinary/traditional categories | `themes_mapper.js#generateAdvancedTabs` |
| SVC-THEME-MODEL-007 | Determines enhanced-layout support from named non-Advanced tabs | Advanced alone is not enhanced support; a missing Theme falls back to ordinary categories | `service_config_view.js#supportsConfigLayout` |
| SVC-THEME-MODEL-008 | Keys Theme caches by stack/version and service set | A stack change, wizard restart, or changed service set cannot reuse stale models; an explicit reset clears Tabs, Sections, SubSections, SubSectionTabs, and conditions | `themes_mapper.js#resetModels`; wizard `clearEnhancedConfigs`; Classic single-service cache checks only existing service tabs |

The semantic graph React must expose, even when represented as plain
TypeScript rather than Ember Data, is:

| Record | Stable fields and relationships |
| --- | --- |
| Theme | Source stack/version/service/file/default metadata, exact Theme name, description, ordered layout names, diagnostics |
| Tab | Parent Theme, name/display name, rows/columns, ordered Sections, active/visible/error/render state |
| Section | Parent Tab, name/display name, row/column index and span, inner rows/columns, ordered SubSections |
| SubSection | Parent Section, name/display name, border/splitter metadata, coordinates/spans, conditions, direct placements, nested tabs |
| SubSectionTab | Parent SubSection, name/display name, conditions, ordered placements, active/visible/error state |
| Placement | Exact config path, exactly one target, static attributes, ordered conditions, original declaration order |
| Widget | Placement/config path, exact type, display name, units, required properties, and type-specific metadata |
| Condition | Parent target, declaration index, resource, referenced config paths, expression, `then`, and `else` mutations |

## Layout, Grid, Nested Tabs, and State

Theme coordinates are zero-based in the shipped metadata. A tab declares
`tab-rows`/`tab-columns`; each Section declares its position/span and its own
`section-rows`/`section-columns`; each SubSection declares its position/span
inside that Section. The layout must remain stable while values, validation,
filters, and conditions change.

| ID | Function and behavior | Detailed acceptance boundary | Primary evidence |
| --- | --- | --- | --- |
| SVC-THEME-LAYOUT-001 | Renders Theme tabs and Sections in metadata order | The generated Advanced tab is separate from named Theme tabs | `themes_mapper.js#tabConfig`; service config templates |
| SVC-THEME-LAYOUT-002 | Places Sections using Classic table-flow semantics | Group Sections by `rowIndex`, retain declaration order inside each row, and place each cell in the next unoccupied column while honoring row/column spans; do not manufacture gaps from `columnIndex` | `enhanced_configs.js#processTab`; `service_config_layout_tab.hbs` |
| SVC-THEME-LAYOUT-003 | Places SubSections using the same collision-free table flow | A spanning cell reserves its occupied rows and columns; later cells advance to the next free column without overlap, metadata-driven gaps, or declaration reordering | `enhanced_configs.js#processTab`; `service_config_layout_tab.hbs` |
| SVC-THEME-LAYOUT-004 | Applies SubSection presentation metadata | Render `border` and `left-vertical-splitter`; derive first/middle/last row/column and top splitters consistently | `models/configs/theme/sub_section.js`; layout template |
| SVC-THEME-LAYOUT-005 | Renders direct properties in placement order | A property renders only when it has a supported Widget and is effectively visible; hiding it must not collapse unrelated grid coordinates | mapper linking; `service_config_layout_tab.hbs` |
| SVC-THEME-LAYOUT-006 | Renders `subsection-tabs` as nested selectable tabs | Each tab has an independent ordered property list and error count; only a visible tab can be selected | `themes_mapper.js#loadSubSectionTabs`; `SubSectionTab`; layout view/template |
| SVC-THEME-LAYOUT-007 | Maintains a valid active nested tab | First visible is active; when conditions/filtering hide the active tab, hand off to the next visible tab or show an explicit empty state | Classic initializes the first declared tab and blocks clicks on hidden tabs but does not reliably hand off; React correction is `BEHAVIOR_DIFF` |
| SVC-THEME-LAYOUT-008 | Propagates search/filter visibility upward | Property visibility determines nested-tab, SubSection, Section, and top-level Tab visibility; Advanced remains reachable and no-content state is explicit | Theme models; `service_config_view.js#filterEnhancedConfigs`, `pickActiveTab` |
| SVC-THEME-LAYOUT-009 | Aggregates errors only from effectively visible properties and overrides | Counts propagate through visible nested tabs/SubSections/Sections to the named tab; hidden errors do not block through a stale badge | Theme models `errorsCount`; service config templates |

Classic groups cells by `rowIndex`, uses HTML `rowspan`/`colspan`, and appends
cells in declaration order; `processTab` does not place empty `columnIndex`
slots or sort by column. Browser table layout instead advances each cell past
columns occupied by earlier spans. React must reproduce that automatic
collision-free placement for compatibility with shipped and custom Themes.
The test suite must lock this down with non-monotonic `columnIndex` metadata,
multiple cells on one row, and overlapping row/column spans.

## Placement and Property-Attribute Semantics

| ID | Function and behavior | Detailed boundary | Primary evidence |
| --- | --- | --- | --- |
| SVC-THEME-PLACEMENT-001 | Links by full `config-type/property-name` identity | The first slash separates type; the remaining text is the property name. Same basenames in two config types never alias | `themes_mapper.js#getConfigId`; condition mapper |
| SVC-THEME-PLACEMENT-002 | Resolves enhanced-layout targets without losing canonical attributes | A unique target inside the same service/Theme graph receives the property. An ambiguous target is diagnosed and not attached. A missing target remains eligible for Advanced display and condition/static-attribute processing because shipped HIVE metadata intentionally contains such placements | `ConfigPlacement`; `themes_mapper.js#mapThemeConfigs`; HIVE `interactive-query-row1-col1` placements |
| SVC-THEME-PLACEMENT-003 | Preserves declaration order without duplicates | Repeated exact config/target placement renders once; the same config may legitimately appear in a different named Theme | mapper property linking |
| SVC-THEME-PLACEMENT-004 | Requires a Widget for enhanced rendering | A placed ordinary property without a Widget remains available in Advanced/traditional layout rather than becoming an implicit text field | `EnhancedConfigsMixin#setConfigsToContainer`; layout template |
| SVC-THEME-PLACEMENT-005 | Creates a synthetic config only for explicit `ui_only_property=true` | It is non-Agent, non-overridable, not a user property, and hosts an action/label Widget; an ordinary missing stack property is never synthesized | `themes_mapper.js#getConfigByAttributes`; config saver `isRequiredByAgent` filter |
| SVC-THEME-PLACEMENT-006 | Keeps static and conditional attributes layered | Base stack attributes, explicit Theme attributes, ordered conditions, recommendation/group attributes, and read-only context produce one effective state without losing the base value | `ValueAttributesInfo`; stack config mapper; enhanced config condition methods |

`ValueAttributesInfo` defines `type`, `minimum`, `maximum`, `unit`,
`increment_step`, `visible`, `overridable`, `read_only`,
`empty_value_valid`, `editable_only_at_install`, `ui_only_property`,
`show_property_name`, `entries`, `entries_editable`,
`selection_cardinality`, `hidden`, `copy`, `delete`, `property-file-name`,
`property-file-type`, user/group entries, and keystore metadata. Some
unannotated bean properties serialize as `entriesEditable`,
`userGroupEntries`, and `keyStore`; current Theme JSON actually uses
`keyStore`. Classic consumes these mixed forms inconsistently. Current
non-Metrics Theme placements use only static `ui_only_property` and legacy
`keyStore`, while current Theme conditions mutate only `visible`; custom
metadata remains extensible.

Classic consumes static placement attributes only partially and its generic
condition-to-model map does not invert negative attributes such as
`read_only`, `empty_value_valid`, and `ui_only_property` correctly. React must
apply their semantic meaning rather than reproduce those boolean inversions.
That is a `METADATA_CONTRACT` correction and requires explicit tests.

The BIGTOP HIVE default Theme contains placements for
`interactive-query-row1-col1`, but that SubSection is absent from its returned
layout. Classic does not render those properties in an enhanced SubSection;
it can still apply their conditions/attributes and expose them through
Advanced. React must preserve that split instead of deleting the canonical
placement or attaching it to an unrelated same-name node.

## Configuration Widget Dispatch

Classic recognizes exactly the following Widget types. Unknown types assert in
development. React may render a controlled unsupported-Widget state, but must
not silently replace an unknown type with a text input because that can submit
an invalid value. Types absent from the current BIGTOP Theme inventory remain
part of the extension contract because the dispatcher supports them and custom
stacks may emit them.

| ID | Widget type | Required user-visible and round-trip behavior |
| --- | --- | --- |
| SVC-THEME-WIDGET-001 | `text-field` | Single-line input; effective value/type/unit, validation, editability, recommendation/default/saved values, final state, undo, and overrides round-trip |
| SVC-THEME-WIDGET-002 | `password` | Masked input with confirmation/retyped validation where the property view requires it; secrets never become ordinary display text or diagnostics |
| SVC-THEME-WIDGET-003 | `checkbox` | Round-trips only the recognized pairs `true/false`, `Yes/No`, `YES/NO`, and `yes/no`, including `boolean-inverted`; it never uses JavaScript truthiness of `"false"` and has no raw fallback. Arbitrary pairs require an explicit `METADATA_CONTRACT` decision |
| SVC-THEME-WIDGET-004 | `toggle` | Requires exactly two entries; the first is ON and the second OFF, both round-trip exact server strings, a switch triggers dependency recommendations, and incompatible values enter raw mode |
| SVC-THEME-WIDGET-005 | `combo` | Maps exact value/label entries and refreshes recommendation boundaries. An unknown value warns but does not auto-enter raw mode; manual raw mode remains available. React must honor `entries_editable` even though Classic checks camel-case `entriesEditable` in this path |
| SVC-THEME-WIDGET-006 | `radio-buttons` | Renders one exclusive group and preserves non-boolean values. Classic misspells the description mapping, does not render descriptions, and does not request dependent recommendations; rendering descriptions and recomputing dependencies are correctness improvements |
| SVC-THEME-WIDGET-007 | `list` | Preserves selection order as exact comma-separated values, enforces cardinality, disables choices at maximum, errors below minimum, and sends incompatible values to raw mode. React must correct Classic's `0+` to minimum zero, define `ALL`, ranges, whitespace, and empty selection deterministically |
| SVC-THEME-WIDGET-008 | `directory` | Edits one filesystem path through the ordinary text-field behavior and directory validation |
| SVC-THEME-WIDGET-009 | `directories` | Edits a directory collection/multiline value while preserving the property's exact delimiter and stored representation |
| SVC-THEME-WIDGET-010 | `slider` | Converts B/KB/MB/GB/TB and integer/float percent units, applies group-specific bounds and step defaults, renders current/default/recommended ticks, validates raw entry, requests recommendations on slide-stop, and saves exact config-unit precision. Zero values and a recommended marker at tick zero require regression coverage for known Classic truthiness defects |
| SVC-THEME-WIDGET-011 | `time-interval-spinner` | Decomposes/composes exact base-unit integers across days/hours/minutes/seconds/milliseconds, caps parts, applies increment/min/max/modulo validation, and disables an unusable final unit. React must still re-evaluate Theme conditions when this Widget is a dependency source; Classic shadows that observer |
| SVC-THEME-WIDGET-012 | `text-area` | Multiline editor with the same validation, recommendation, final, undo, dependency, and override behavior as other saved properties |
| SVC-THEME-WIDGET-013 | `label` | Displays a non-editable label/value state and never fabricates a saveable text input or required-value error |
| SVC-THEME-WIDGET-014 | `test-db-connection` | Uses an explicit UI-only placement and Theme `required-properties` to validate/build the action; action creation, task discovery/polling, terminal result, failure detail, and Retry follow the contract below |

Raw text fallback is shared only by Widgets whose Classic view sets
`supportSwitchToTextBox=true`; it is not an automatic fallback for every
Widget. An incompatible value may switch to raw mode, and Enter switches back
only when the Widget accepts the value. While the raw input is focused, edits
trigger a 500 ms debounced Stack Advisor dependency request. React tests must
exercise both directions and must not erase an incompatible saved value.

The DB connection Widget resolves semantic keys such as
`db.connection.source.host`, `db.type`, `db.type.label`,
`db.connection.user`, `db.connection.password`, and `jdbc.driver.url` to exact
config paths supplied by `required-properties`. It adds Ambari Server JDK/Java
properties and target hosts, then uses:

| Phase | Installed service | Service not installed yet |
| --- | --- | --- |
| Create action | `POST /api/v1/clusters/{cluster}/requests` | `POST /api/v1/requests` |
| Discover tasks | `GET /api/v1/requests/{requestId}/tasks/` | Same unscoped request/task resource |
| Poll task | `GET /api/v1/requests/{requestId}/tasks/{taskId}` | Same unscoped request/task resource |

The exact payload includes resolved DB name/URL/user/password, Java/JDK
properties, Ambari Server host, `check_execute_list=db_connection_check`, and
all target hosts. `COMPLETED` is successful only when the structured DB check
reports exit code zero. Failed/aborted/timed-out tasks and create/discovery/poll
errors end Connecting, show sanitized stderr/stdout/structured detail, and
allow Retry without losing form values. Classic lacks dedicated error handling
for task discovery/poll GET failures; the terminating Retry behavior is
`BEHAVIOR_DIFF`.

## Shared Property Controls and Permissions

| ID | Function and behavior | Required boundary | Primary evidence |
| --- | --- | --- | --- |
| SVC-THEME-ATTR-001 | Computes effective editability | Combine page permission/mode, current-version/compare state, config-group ownership, final/read-only state, `read_only`, and `editable_only_at_install` | configs controller `canEdit`; stack config mapper; Widget base view |
| SVC-THEME-ATTR-002 | Displays effective metadata | Show display/property name according to `show_property_name`, description, value, unit, required state, error, warning, and undefined state without changing identity | Widget base/templates; stack property model |
| SVC-THEME-ATTR-003 | Preserves saved/default/recommended/current values | Undo restores saved value/final state; recommended action applies recommended value/final state; conversion Widgets compare and save in config units | `config_widget_view.js`; Widget views |
| SVC-THEME-ATTR-004 | Supports config-group overrides | Create/remove/edit an allowed override, independently validate it, and keep its recommendation/final state associated with the selected group | `ConfigOverridable`; Widget control templates |
| SVC-THEME-ATTR-005 | Excludes UI-only state from saves | Synthetic actions may read canonical required properties but `isRequiredByAgent=false` keeps them out of config payloads, comparisons, and change hashes | Theme mapper; configs saver/comparator/hash filters |
| SVC-THEME-ATTR-006 | Re-evaluates dependent behavior after edits and recommendations | Config edits, undo/recommended actions, service-set changes, and refreshed recommendations recompute conditions and visible/error state | Widget base observers; `updateAttributesFromTheme` |
| SVC-THEME-ATTR-007 | Preserves filter and selection state safely | Search/filter changes may switch the active tab but must not mutate config values or lose unsaved edits | `service_config_view.js#filterEnhancedConfigs`, `pickActiveTab` |
| SVC-THEME-ATTR-008 | Makes unsupported/malformed metadata diagnosable | Display a service/Theme/config-path-scoped unsupported state; never log secret values or interpolate metadata into executable code | React acceptance requirement |

| ID | Entry or operation | Gate |
| --- | --- | --- |
| SVC-THEME-PERM-001 | View installed service configuration and its Theme layout | Existing config-view route/cluster permission; Theme adds no separate permission |
| SVC-THEME-PERM-002 | Edit/save installed service configuration, Widget controls, and overrides | `SERVICE.MODIFY_CONFIGS`; selected historical/compare views remain read-only |
| SVC-THEME-PERM-003 | Compare versions in the Theme hierarchy | `SERVICE.COMPARE_CONFIGS`; compare renderer is always non-editable |
| SVC-THEME-PERM-004 | Host Config Theme view | Read-only regardless of service edit permission; categories are filtered by components on that host |
| SVC-THEME-PERM-005 | New-cluster Installer | `AMBARI.ADD_DELETE_CLUSTERS`; selected-service metadata drives Theme categories |
| SVC-THEME-PERM-006 | Add Service | `SERVICE.ADD_DELETE_SERVICES` plus the existing support flag/upgrade/wizard gates; Step 7 uses existing-cluster save and recommendation context |

There is no independent "enable Service Theme" feature flag. Classic's
`supportsConfigLayout` is a graph/controller predicate: at least one
non-categorized, non-Advanced Theme tab must exist for Service Configs, Host
Configs, or wizard Step 7. Permission changes controls, not the semantic
layout.

## Condition Evaluation and Effective Visibility

Conditions can be attached to a config placement, SubSection, or
SubSectionTab. The mapper assigns a target-local declaration index and defaults
an omitted `resource` to config behavior. The 28 descriptor-declared
non-Metrics artifacts in main server resources currently have 45 config
conditions and 9 service conditions; every current action changes `visible`,
although the action model can carry other value attributes.

| ID | Function and behavior | Detailed acceptance boundary | Primary evidence |
| --- | --- | --- | --- |
| SVC-THEME-COND-001 | Evaluates every `depends-on` entry in declaration order | Do not stop after the first condition; later writes to the same effective attribute win deterministically | mapper condition IDs; `updateAttributesFromConditions#forEach` |
| SVC-THEME-COND-002 | Supports exact config references | `${config-type/property-name}` lookup uses full type/name identity within the applicable service config set. React observes the canonical source even when it appears only in Advanced, has no Widget, is filtered, or belongs to an unrendered tab | `configTheme.calculateConfigCondition`; mapper condition config list |
| SVC-THEME-COND-003 | Supports the shipped expression grammar | Bare references compare with string `true`; `===` compares exact unquoted metadata tokens; `&&` binds before `\|\|`; parentheses and other operators are unsupported | `utils/configs/theme/theme.js#calculateConfigCondition`; current Theme JSON |
| SVC-THEME-COND-004 | Supports `resource=service` | Existing pages test installed services; Installer/Add Service also test `allSelectedServiceNames` (installed or selected) against the exact service name in `if` | `EnhancedConfigsMixin#updateAttributesFromConditions` |
| SVC-THEME-COND-005 | Applies `then` or `else` attributes | Config targets can receive effective value attributes; SubSection/SubSectionTab targets apply visibility to the container and its properties. Semantic negatives such as `read_only`, `empty_value_valid`, `editable_only_at_install`, and `ui_only_property` must be mapped with the correct inversion | `changeConfigAttribute`, `changeSubsectionAttribute`; `StackConfigValAttributesMap` |
| SVC-THEME-COND-006 | Re-evaluates after dependency changes | Initial mapping, Widget/raw edits, recommendation responses, service selection, Advanced-only sources, lazy/unrendered sources, and time-spinner sources converge on the same effective result | Widget observers; wizard `updateConfigAttributesFromThemes`; Classic gaps are `BEHAVIOR_DIFF` |
| SVC-THEME-COND-007 | Separates base/static and condition-hidden state | A condition becoming true/false may only remove its own layer; it cannot reveal a property hidden by base metadata, parent container, permission, or another active condition | Required React layered-state contract; Classic uses shared `hiddenBySection` and can overwrite another layer |
| SVC-THEME-COND-008 | Propagates visibility and errors upward | Hidden properties stop rendering and stop contributing visible errors; container visibility and active-tab state update in the same render transaction | Theme models and templates |
| SVC-THEME-COND-009 | Rejects malformed or unsupported expressions without execution | Return a deterministic diagnostic and preserve the last safe/static state; never execute Theme text as JavaScript | Classic uses `window.eval`; parser replacement is `BEHAVIOR_DIFF` |
| SVC-THEME-COND-010 | Handles missing references and duplicate basenames safely | Missing exact paths and atoms with no `${...}` reference produce a diagnostic/safe result; a property with the same basename in another type or service is never substituted | Classic can leave a token false, throw on a missing token list, or lose path text after a second slash |

Classic interpolates string values and calls `window.eval`. That mechanism is
not parity. React must implement a closed parser for only the grammar above,
with no `eval`, `Function`, DOM script injection, or implicit JavaScript value
coercion. Parser tests must include hostile/malformed Theme text and assert that
no code path executes it.

Classic applies initial conditions centrally, but interactive observers are
installed only from a rendered placed config that has a Widget. A dependency
present only in Advanced, without a Widget, behind a filter, or in a lazily
unrendered top tab can therefore remain stale until a later full recomputation;
the time-spinner view also shadows the base Theme-condition observer. Dynamic
container lookup is under-qualified (`name + theme`, and initial lookup can be
name-only), so equal local names in two services can mutate the wrong target.
React must observe canonical configuration state independently of presentation
and resolve every target through its complete parent-qualified identity.

## Consumer Flows

| ID | Consumer | Required sequence and user-visible behavior | Backend interactions and boundaries |
| --- | --- | --- | --- |
| SVC-THEME-CONSUME-001 | Installed Service Configs | Load the selected service's default Theme before current/config-version data, compile effective attributes after current values/overrides, render enhanced layout when supported, and retain Advanced/traditional categories | Single-service Theme GET; config versions/current configs; recommendations; config-version save |
| SVC-THEME-CONSUME-002 | Installed Service Config cache | Reuse a Theme only for the same stack/version/service and invalidate it when stack context changes | Classic reuses any existing Tab for the service, which is too weak for a React cache key |
| SVC-THEME-CONSUME-003 | Host Configs | Reuse the complete service Theme hierarchy for the selected host, remain read-only, and select the host's config group. Host-component filtering applies only to traditional `configCategories`; it does not prune Theme Sections/SubSections | Same single-service Theme/config-version APIs; `MainHostServiceConfigsController` extends Service Configs |
| SVC-THEME-CONSUME-004 | Config version comparison | Reuse named Theme tab/Section/SubSection placement but flatten nested-tab configs into read-only comparison rows. Exact identity preserves added/removed/modified state; UI-only entries are excluded, missing sides show `Undefined`, and password changes are not highlighted as diffs | Same Theme already loaded for service; two selected config-version datasets; no Theme mutation |
| SVC-THEME-CONSUME-005 | New Installer Step 7 | Load cluster/selected-service stack configs, then batch Themes, then create canonical step configs, recommendations, static/effective attributes, validation, and category views | Batch Theme GET; stack config metadata; Stack Advisor recommendations/validation; Review/save payload |
| SVC-THEME-CONSUME-006 | Installer Credentials | Build rows from categorized `credentials` Theme metadata; bind username/password controls to canonical step configs; confirmation remains UI-only; invalid credentials gate category progression | No separate Theme save; exact config values flow to Review/save |
| SVC-THEME-CONSUME-007 | Installer Databases | Render every selected service's `database` Theme and DB-test UI-only actions; only active visible properties participate in category validation | Batch Theme plus DB custom-action/task APIs |
| SVC-THEME-CONSUME-008 | Installer Directories and Accounts | Render per-service `directories` layouts; Accounts comes from MISC `displayType=user` properties and is not a named Theme | Directory edits and account edits share the canonical config collection |
| SVC-THEME-CONSUME-009 | Installer All Configurations | Render selected services' `default` Themes plus Advanced/traditional properties, service switching, filters, recommendations, conditions, validation, and final state | Same canonical step config collection and Stack Advisor flow |
| SVC-THEME-CONSUME-010 | Add Service Customize Services | Batch-load installed plus selected-service Theme context, use the ordinary service/default-Theme UI rather than the new Installer category strip, preserve existing configs/groups, and save only intentional new/dependent changes | Add Service Stack Advisor `AddService` user context, installed config tags/groups, Kerberos descriptor when enabled, and config save/deploy APIs |

### New Installer Category State

| ID | Category behavior | Exact Classic behavior and React requirement |
| --- | --- | --- |
| INST-7-THEME-001 | Stack configs precede batch Theme mapping | Theme placement must link against the populated config collection; a service-stack-config load failure must settle with an actionable error |
| INST-7-THEME-002 | Theme request settles before config creation | Theme success maps enhanced records; Theme failure continues with ordinary categories rather than blocking Step 7 |
| INST-7-THEME-003 | Effective state is applied after values/recommendations | Current/default values, recommendations, group state, Theme attributes, and conditions must be reflected before user interaction |
| INST-7-THEME-004 | Specialized edits use canonical configs | Credentials, Databases, Directories, Accounts, and All Configurations share one collection used by Review/save; confirmation and action-only values are excluded |
| INST-7-THEME-005 | Category navigation preserves valid state | Back/Next skips unavailable categories, validates the category that requires it, remembers per-category service selection, and never loses edits |
| INST-7-THEME-006 | Credentials availability | Classic skips it when no selected service has a `credentials` Theme; React must recompute after service selection/Theme retry |
| INST-7-THEME-007 | Credentials validation | Required username plus password/retyped-password errors gate Next; every field writes its exact stack property |
| INST-7-THEME-008 | Databases availability | Classic skips it when no selected service has a `database` Theme; each visible service tab has independent validation and DB test state |
| INST-7-THEME-009 | Directories availability | Layouts come from `directories` Themes and retain per-service selection; Classic does not skip the outer category when none exist, so an explicit skip/empty state is a React `BEHAVIOR_DIFF` |
| INST-7-THEME-010 | Accounts source | MISC user/group properties and installer checkboxes render independently of Theme availability |
| INST-7-THEME-011 | All Configurations | Exact `default` Themes and Advanced/traditional categories remain the final comprehensive editor and validation surface |
| INST-7-THEME-012 | Re-entry | Stored Step 7 configs open All Configurations and preserve values; first entry starts at the first enabled category and enforces progression gates |
| INST-7-THEME-013 | Service conditions | `allSelectedServiceNames` includes installed or selected services, so service-resource conditions respond to both new and existing dependencies |
| INST-7-THEME-014 | Recommendation context | New cluster sends `ClusterCreate`; Add Service sends `AddService` with newly selected service names and must not use cluster-creation context |

Add Service is deliberately different from the new Installer: because
`WizardStep7View` selects the category-strip template only for
`installerController`, Add Service renders `templates/wizard/step7` and the
ordinary `ServicesConfigView`. Named credentials/database/directories artifacts
still contribute metadata and conditions, but they do not create the five-tab
outer category strip in Add Service.

## Fallback, Failure, Retry, and Known Classic Defects

| ID | Scenario | Required React result | Classic result/evidence |
| --- | --- | --- | --- |
| SVC-THEME-FALLBACK-001 | Named non-Advanced Theme tab exists | Render the enhanced layout and retain Advanced | `supportsConfigLayout=true` |
| SVC-THEME-FALLBACK-002 | 200 response has no usable Theme tab | Render ordinary/Advanced configs and an explicit no-Theme state; do not infer support from Advanced | Mapper/support predicate |
| SVC-THEME-FALLBACK-003 | Single-service Theme request returns 404/500/network error | Settle loading, preserve ordinary configs and current edits, show scoped error plus Retry | Classic `loadConfigTheme` resolves its private Deferred only on success and can spin forever; `BEHAVIOR_DIFF` |
| SVC-THEME-FALLBACK-004 | Batch Theme request fails | Continue Installer/Add Service with ordinary configs, show scoped fallback/Retry, and keep valid service/category selection | Classic caller continues through `.always()`; error callback is empty |
| SVC-THEME-FALLBACK-005 | One batch service/Theme is malformed | Isolate it, compile valid siblings, preserve that service's ordinary configs, and record service/file diagnostics | Server filters only missing/unbindable files; semantically malformed but bindable metadata reaches the client |
| SVC-THEME-FALLBACK-006 | Retry succeeds after fallback edits | Recompile presentation around the same canonical edited values, preserve validation and selection where valid, and avoid duplicate requests/models | Classic has no complete retry merge workflow; `BEHAVIOR_DIFF` |
| SVC-THEME-FALLBACK-007 | Condition or filter hides the active item | Select another visible top/nested tab or show explicit empty state in the same service; never show stale hidden content | Classic handles top tabs better than nested tabs |
| SVC-THEME-FALLBACK-008 | Stack/version/service set changes or component unmounts | Abort/ignore stale responses, clear the correct Theme cache, and never apply an old response to the new context | Wizard clear/reset paths and React async recovery requirement |

Additional Classic limitations that are evidence, not desired parity:

1. `themesMapper.map` itself does not reset all stores; lifecycle callers must
   do so, and single-service records can persist across route changes.
2. Tab and SubSectionTab IDs are not fully parent-qualified, so equal local
   names can collide across Themes/services.
3. Widget metadata is attached to a global stack config property, so the last
   mapped Theme can overwrite presentation for another placement.
4. `configuration-layout` is retained by the server but ignored by the Classic
   mapper. Some current `directories` files name their sole layout
   `directories` while the field is `default`; React must map the declared
   layouts and must not discard one based on this field.
5. Classic uses `window.eval` for conditions and a shared hidden flag for
   multiple visibility layers.
6. UI-only synthesis in Classic requires a direct SubSection target; a custom
   UI-only placement in a SubSectionTab is lost.
7. Batch response flattening assumes every service has a valid `themes` array;
   malformed siblings are not isolated.
8. The DB test Widget can remain Connecting after task-list or polling errors.
9. Interactive conditions are presentation-observer-driven, so Advanced-only,
   no-Widget, filtered, lazy, and time-spinner dependency sources can go stale.
10. Condition target lookup omits service and parent identity in multiple paths,
    so repeated local names can mutate another service's container.
11. Dynamic assignment does not invert four negative value attributes; current
    shipped conditions happen to mutate only visibility.
12. The provider can fail while projecting an explicitly requested unknown
    Theme file instead of returning a clean missing-resource response.

## Executable React and Server Test Contract

Static source inspection is not sufficient for parity. Tests must execute in
the existing frontend test runner (Vitest/jsdom with React Testing Library and
request mocks where applicable) or the relevant server JUnit suite. A snapshot
alone, a route-exists assertion, or a mocked normalizer that never consumes
real Theme JSON is not coverage.

| ID | Executable scenario | Required fixture and assertions |
| --- | --- | --- |
| SVC-THEME-TEST-001 | Compile real HIVE default Theme | Read the repository JSON; assert ordered layouts, tab/Section/SubSection dimensions, exact placements, orphan Advanced-only placements, Widget types/units, UI-only DB action, and no Metrics artifacts |
| SVC-THEME-TEST-002 | Compile real HIVE database/credentials/directories Themes | Wrap as API resources; assert Theme isolation, specialized names, exact required-properties, and canonical config links |
| SVC-THEME-TEST-003 | Compile real Ranger default Theme | Assert nontrivial grids, all nested `subsection-tabs`, conditions, full paths, Widget metadata, and independent nested-tab collections |
| SVC-THEME-TEST-004 | Compile real Ranger/Ranger KMS database Themes | Assert both normal/root DB actions remain distinct, UI-only configs never enter saves, and required-property maps are exact |
| SVC-THEME-TEST-005 | Compile real YARN and MAPREDUCE2 Themes | Assert multirow/multicolumn placements, spans, service conditions, sliders/toggles/directories, declaration order, and discovery through MAPREDUCE2's non-default `themes-mapred` directory |
| SVC-THEME-TEST-006 | Normalize single and batch response shapes | Assert identical semantic Theme records, empty/omitted nested collections, non-array rejection, mixed valid/invalid/no-Theme services, per-service isolation, and service-scoped diagnostics |
| SVC-THEME-TEST-007 | Preserve all layouts independently of `configuration-layout` | Test matching, missing, and mismatched field values, multiple ordered layouts, duplicate tab names with layout-qualified identity, the real HIVE directories mismatch, and no layouts |
| SVC-THEME-TEST-008 | Protect full config identity | Use identical basenames in two config types/services and a property name containing another slash; assert no placement/condition/Widget alias |
| SVC-THEME-TEST-009 | Protect graph identity | Reuse tab/section/subsection/subsection-tab names across two services and four Theme names; assert no record, active state, error, or Widget collision |
| SVC-THEME-TEST-010 | Validate placement targets | Cover direct SubSection, SubSectionTab, duplicate placement, missing target, ambiguous target, ordinary missing property, and explicit UI-only synthesis in both target types |
| SVC-THEME-TEST-011 | Isolate malformed metadata | Cover null/wrong-type fields, invalid coordinates/spans, missing config/Widget type, unknown Widget, malformed required-properties, and one valid sibling |
| SVC-THEME-TEST-012 | Parse config conditions | Cover bare true/false strings, strict `===` tokens, `&&`, `\|\|`, mixed precedence, whitespace, missing references, an atom with no token, rejected parentheses/operators, and every shipped expression shape |
| SVC-THEME-TEST-013 | Evaluate service conditions | Cover installed, selected-not-installed, absent, case mismatch, changing service sets, and explicit recomputation in existing/Installer/Add Service contexts |
| SVC-THEME-TEST-014 | Apply ordered mutations from every source mode | Two or more conditions write the same attribute; sources cover rendered Widget, Advanced-only, no-Widget placement, filtered/lazy tab, raw mode, recommendation update, and time spinner. Assert declaration-order result after every canonical-value update |
| SVC-THEME-TEST-015 | Preserve visibility and identity layers | Combine base hidden, config condition, parent SubSection, nested-tab, filter, and permission/read-only context; removing one layer must not reveal another. Repeat equal subsection/Theme names across two services and prove mutations remain parent-qualified |
| SVC-THEME-TEST-016 | Reject unsafe conditions | Feed missing references, incomplete atoms, unsupported operators, code-like strings, and prototype-shaped tokens; assert diagnostic, safe state, and no dynamic execution API call |
| SVC-THEME-TEST-017 | Render exact Section grid | Use non-monotonic declaration order, empty cells, row/column spans, and changing validation; assert stable CSS-grid coordinates and no overlap |
| SVC-THEME-TEST-018 | Render exact SubSection grid | Assert inner dimensions, border, left/top splitters, title gaps, spans, and responsive narrow viewport without reordering semantic focus order |
| SVC-THEME-TEST-019 | Operate top-level tabs | Assert initial first-visible selection, disabled/filtered click rejection, active handoff, Advanced reachability, render-on-demand, and no-content state |
| SVC-THEME-TEST-020 | Operate nested tabs | Use real Ranger's three LDAP tabs plus a synthetic mixed-visibility fixture; assert first-visible selection, keyboard/pointer selection, independent content, hidden-active handoff, all-hidden state, and parent-qualified placement/error isolation |
| SVC-THEME-TEST-021 | Propagate filter/errors | Search values/names/descriptions/overrides, combine filter columns, and assert effective property/container visibility and visible-only error totals |
| SVC-THEME-TEST-022 | Dispatch every Widget type | Table-drive all 14 types plus unknown/missing types; assert directory versus directories/text-area component choice, label/test action required-error exemption, controlled unsupported state, and no implicit text fallback |
| SVC-THEME-TEST-023 | Round-trip entry Widgets | Cover every recognized/inverted checkbox pair and arbitrary-pair decision; exact two-value toggle; combo unknown value plus snake/camel editability; radio description/dependency correction; list `0+`, `ALL`, bounded ranges, whitespace, empty selection, order, maximum disabling, and invalid raw values |
| SVC-THEME-TEST-024 | Round-trip text/directory Widgets | Cover text, password confirmation, text area, directory, directories delimiter/multiline state, label non-saveability, required/empty validation, and secrets not rendered/logged |
| SVC-THEME-TEST-025 | Convert slider units | Cover B/KB/MB/GB/TB and integer/float percent units, group bounds, default/explicit increments, zero value, marker at minimum/tick zero, precision, raw mode, recommendation/undo, slide-stop request, and exact saved config-unit value |
| SVC-THEME-TEST-026 | Convert time spinner units | Cover days/hours/minutes/seconds/milliseconds composition, caps/overflow, disabled final unit, modulo increment, min/max, raw mode, exact base-unit save, and use as a live Theme-condition source |
| SVC-THEME-TEST-027 | Exercise shared controls and raw fallback | Cover read-only/install-only/final/permission states, semantically inverted attributes, recommendation/undo final flags, create/remove/invalid overrides, supported/unsupported raw transitions with debounce, and comparison/host read-only mode |
| SVC-THEME-TEST-028 | Build DB action payloads | Cover HIVE, Ranger normal/root, and Ranger KMS exact required-property maps; installed/uninstalled endpoints; one/many target hosts; Java/JDK fields; no password logging |
| SVC-THEME-TEST-029 | Complete DB task lifecycle | Cover create failure, missing request/task ID, task-list failure/empty list, polling failure, every terminal status, `COMPLETED` exit code zero/nonzero, details, Retry, and stale/unmounted response |
| SVC-THEME-TEST-030 | Load installed Service Config Theme | Mock success, empty, 404, 500, malformed, retry, service switch, stack switch, and late response; assert fallback/selection/unsaved values and request deduplication |
| SVC-THEME-TEST-031 | Load Host Config Theme | Assert the complete service Theme graph remains unchanged/read-only, the selected host group is used, only traditional categories are host-component-filtered, Theme failure falls back, and no editable control appears |
| SVC-THEME-TEST-032 | Compare config versions | Cover added/removed/modified/unchanged exact paths, direct and flattened nested-tab placements, hidden/filter behavior, mock `Undefined` side, UI-only exclusion, password row with no secret diff highlight, and no edit controls |
| SVC-THEME-TEST-033 | Run new Installer category flow | Cover all five categories, credentials/database skipping, empty Directories handling, per-category Next/Back, service switching, service conditions, validation, Review payload, back/re-entry, and refresh persistence |
| SVC-THEME-TEST-034 | Run Add Service flow | Assert ordinary template (no five-category strip), installed/new config merge, AddService recommendation context, groups/overrides, Kerberized cluster, dependent changes, exact save tags, and Theme fallback/retry |
| SVC-THEME-TEST-035 | Preserve canonical save payload | Edit one value in every specialized/default/Advanced surface; assert exact config type/name/value/final/group fields once each and complete exclusion of UI-only/confirmation state |
| SVC-THEME-TEST-036 | Enforce permissions and modes | Cover view-only, modify, compare, host, Installer, and Add Service gates; layout remains readable while actions/inputs follow authorization |
| SVC-THEME-TEST-037 | Test server parsing/filtering | JUnit covers missing/syntax/binding errors, log-only diagnostics, valid sibling retention, semantically malformed-but-bindable input, invalid child with parent fallback, and extension/common/custom-directory loading |
| SVC-THEME-TEST-038 | Test server inheritance/removal | JUnit covers every exact identity, scalar/list replacement rule, complete/incomplete additions, every removal sentinel, deleted file, parent aliasing/mutation, nested ordering, conditions/tabs, and all documented null-parent hazards |
| SVC-THEME-TEST-039 | Test Theme resource provider | Provider/integration tests cover exact `theme_data.Theme` projection, logical/file key behavior, single/batch/default predicate, order non-contract, per-service empty child, zero collection, missing parent, named missing-file regression, `getPKPropertyIds`, and unsupported mutations |
| SVC-THEME-TEST-040 | Real-cluster acceptance | Install and Add HDFS/YARN/MAPREDUCE2/HIVE/Ranger/Ranger KMS plus one custom/extension service with default/database/credentials/directories Themes, config groups, Kerberos, failure injection, retry, save, reload, and version comparison |

Frontend coverage must include DOM and interaction assertions, not only the pure
normalizer. At minimum, the real HIVE, Ranger, Ranger KMS, and YARN files remain
test fixtures read from their repository paths so changes to shipped Theme
metadata cannot silently break React. Synthetic fixtures cover extensibility
edges that current files do not exercise, especially duplicate local names,
all dispatcher Widget types, multiple layouts, UI-only nested-tab placement,
static non-visibility attributes, and malformed expressions.

## Current Non-Metrics Metadata Inventory

Only descriptor-declared artifacts are reachable through the Theme API. Raw
file counts are included separately to prevent a loose filesystem glob from
being mistaken for the runtime inventory:

| Repository scope | Descriptor-declared/API artifacts | Raw Theme JSON | Declared inner Theme names |
| --- | ---: | ---: | --- |
| Main server (`BIGTOP` stacks plus `common-services`) | 28 | 31 | 9 `default`, 13 `directories`, 3 `database`, 3 `credentials` |
| Main server plus `contrib` descriptors | 30 | 33 | 11 `default`, 13 `directories`, 3 `database`, 3 `credentials` |

The three raw but undeclared main-server files are both BIGTOP SOLR Theme files
and Ranger KMS `theme_version_1.json`; they are not API fixtures. Conversely,
MAPREDUCE2 declares two reachable files under the custom
`<themes-dir>themes-mapred</themes-dir>`, which a literal `/themes/` glob misses.
All 30 declared entries across main plus contrib currently set
`<default>true</default>`.

| Widget type | Declared main | Declared main + contrib | Raw main files |
| --- | ---: | ---: | ---: |
| `text-field` | 138 | 144 | 152 |
| `toggle` | 41 | 47 | 43 |
| `slider` | 39 | 39 | 41 |
| `password` | 11 | 12 | 15 |
| `combo` | 12 | 12 | 14 |
| `directories` | 10 | 10 | 10 |
| `test-db-connection` | 8 | 8 | 10 |
| `directory` | 2 | 2 | 4 |
| `checkbox` | 3 | 4 | 3 |
| `time-interval-spinner` | 3 | 3 | 3 |

`radio-buttons`, `list`, `text-area`, and `label` are supported by the Classic
dispatcher but absent from these current declared fixtures. The declared main
inventory has 45 config conditions, 9 service conditions, and one Ranger
SubSection containing three nested tabs.

Inventory counts are evidence about today's fixtures, not a whitelist. Custom
services and later stack versions may add supported types, layouts, conditions,
and repeated names. Metrics service Theme files are deliberately excluded from
every count.

## Primary Source Evidence

| Area | Source evidence |
| --- | --- |
| REST URL definitions | `ambari-web/classic/app/utils/ajax/ajax.js` (`configs.theme`, `configs.theme.services`) |
| Client loading/failure behavior | `ambari-web/classic/app/mixins/main/service/themes_mapping.js` |
| Client graph mapping | `ambari-web/classic/app/mappers/configs/themes_mapper.js` |
| Conditions, Widget dispatch, grid preparation | `ambari-web/classic/app/mixins/common/configs/enhanced_configs.js` |
| Condition formula implementation | `ambari-web/classic/app/utils/configs/theme/theme.js` |
| Theme records/visibility/errors | `ambari-web/classic/app/models/configs/theme/{tab,section,sub_section,sub_section_tab,theme_condition}.js` |
| Enhanced layout rendering | `ambari-web/classic/app/views/common/configs/service_config_layout_tab_view.js`; `app/templates/common/configs/service_config_layout_tab.hbs` |
| Comparison rendering | `service_config_layout_tab_compare_view.js`; `service_config_layout_tab_compare.hbs` |
| Widget behavior | `ambari-web/classic/app/views/common/configs/widgets/*`; corresponding templates and Widget mixins |
| Service Config consumer | `ambari-web/classic/app/controllers/main/service/info/configs.js`; `service_config_view.js`; `service_config.hbs` |
| Host Config consumer | `ambari-web/classic/app/controllers/main/host/configs_service.js`; host configs service view/template |
| Installer/Add Service loading | `ambari-web/classic/app/controllers/wizard.js#loadConfigThemes`; `controllers/installer.js`; `controllers/main/service/add_controller.js` |
| Installer categories | `controllers/wizard/step7_controller.js`; `views/wizard/step7_view.js`; `views/wizard/step7/*`; `templates/wizard/step7_with_category_tabs.hbs` |
| Server provider | `ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ThemeArtifactResourceProvider.java` |
| Server query ordering/nested isolation | `ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterControllerImpl.java`; `ambari-server/src/main/java/org/apache/ambari/server/api/query/QueryImpl.java` |
| Server load/inheritance | `ambari-server/src/main/java/org/apache/ambari/server/stack/ThemeModule.java`; `ServiceModule.java` |
| Server model | `ambari-server/src/main/java/org/apache/ambari/server/state/theme/*`; `state/ThemeInfo.java`; `state/ValueAttributesInfo.java` |
| Existing server tests | `ambari-server/src/test/java/org/apache/ambari/server/stack/ThemeModuleTest.java`; `StackManagerExtensionTest.java` |
| Descriptor/custom-directory evidence | `ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/YARN/metainfo.xml` (`themes-mapred`); service `metainfo.xml` Theme descriptors |
| Real non-Metrics fixtures | `ambari-server/src/main/resources/common-services/*/themes*`; `ambari-server/src/main/resources/stacks/*/*/services/*/themes*`; declared `contrib` Theme directories, excluding Metrics artifacts |

The existing `ThemeModuleTest` verifies representative parent layout
inheritance and placement/Widget count merging. `StackManagerExtensionTest`
verifies valid/invalid entries in the pre-resolution `getThemes()` list, not
the resolved `getThemesMap()` consumed by the API. No focused
`ThemeArtifactResourceProviderTest` was found. Provider semantics and the full
inheritance/removal matrix therefore remain explicit test obligations rather
than assumed coverage.
