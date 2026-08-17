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

# Background Operations and Non-Metrics Dashboard Features

## Background Operations

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| BG-001 | The top-level Background Operations view displays a request list, status, progress, context, and start/end times | Polling runs only on the main page and stops on logout | `background_operations.get_most_recent` and related requests | `app/controllers/global/background_operations_controller.js` |
| BG-002 | Expands a request to display stages/tasks by host, role, command, and status | Large responses use field filtering and minimal response | `background_operations.get_by_request` | `app/controllers/global/background_operations_controller.js`, `app/templates/common/host_progress_popup.hbs` |
| BG-003 | Opens an individual task to view stdout, stderr, and the corresponding output/error log paths, and allows the loaded text to be copied or opened in a new window | Continues polling while the task is incomplete; the generic UI does not display raw `structured_out` or exit code, and only HDFS Rebalance converts structured fields into dedicated progress data | `background_operations.get_by_task` | `app/utils/host_progress_popup.js#createTask`, `app/utils/host_progress_popup.js#_handleRebalanceHDFS`, `app/templates/common/host_progress_popup.hbs` |
| BG-004 | Opens the corresponding request progress from service, host, installation, and upgrade actions instead of creating a separate progress model | The request ID comes from the 202 response; synchronous responses do not enter progress | `common.request.polling`, `background_operations.get_by_request` | `app/utils/host_progress_popup.js`, `app/controllers/global/background_operations_controller.js` |
| BG-005 | Displays failed hosts and logs when a request/task fails and allows the related business flow to trigger a retry | Whether retry is available is determined by the specific business controller | Business-specific retry requests | Each wizard/service/upgrade controller |
| BG-006 | Supports request schedules: batch start/stop/restart operations can run immediately or be scheduled | Creates, queries pending, and deletes schedules; mutually exclusive with a running wizard/upgrade | `common.batch.request_schedules`, `request_schedule.get.pending`, `common.delete.request_schedule` | `app/utils/batch_scheduled_requests.js` |
| BG-007 | Prevents overlapping requests of the same type and stops polling when the page exits or the controller is disabled | Network failures and aborts must not create duplicate modals; each poller has its own interval | Multiple GET/status requests | `app/utils/polling.js`, `app/utils/updater.js`, `app/controllers/global/update_controller.js` |
| BG-008 | Native WebSocket/STOMP pushes host-component, alert summary, topology, config, service, host, alert definition/group, upgrade, background request, and dynamic task detail updates, which mapper/controller code applies to Ember Data; an initial native connection failure falls back to the SockJS eventsource/xhr polling transport series | When both transports fail, there is no unified REST polling fallback: some states later converge through existing updaters or page REST snapshots, while others recover only after a manual refresh or re-entry. After an established connection disconnects, it reconnects after 6 seconds and restores subscriptions from a shallow snapshot, but has no event replay; an initial SockJS failure does not continue transport retry. See the realtime contract for all 11 destinations and failure boundaries, `CONDITIONAL` | `/api/stomp/v1/websocket`, SockJS `/api/stomp/v1`, non-REST resource APIs | `app/utils/stomp_client.js`, `app/mappers/socket`, `app/controllers/global/update_controller.js`, `generated/realtime-channels.md` |
| BG-009 | Displays Abort for an abortable request in a running or unknown state; after confirmation, updates the request status to `ABORTED` and adds an abort reason | Requires `SERVICE.START_STOP`; Abort is disabled before submission, restored according to the current state on failure with global error handling, and followed by a confirmation result on success | `background_operations.abort_request` | `app/utils/host_progress_popup.js#isAbortableByStatus`, `app/utils/host_progress_popup.js#abortRequest`, `app/utils/ajax/ajax.js` |

## Non-Metrics Dashboard

The Metrics tab, Heatmap, Horizon Chart, and metric Widgets are all `OUT_OF_SCOPE`. The following non-Metrics capabilities are retained:

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| DASH-001 | The default Dashboard route redirects to the Metrics page | The page itself is excluded by scope; only the default navigation fact is recorded here | Metrics request excluded | `app/routes/main.js` |
| DASH-002 | Config History lists service config versions, including service, version, author, creation time, note, group, current, and cluster-compatible status | Supports pagination, sorting, and keyword/service/version filtering; data is loaded through direct HttpClient | `DIRECT:main/dashboard/config_history_controller.js#getUrl` | `app/controllers/main/dashboard/config_history_controller.js`, `app/templates/main/dashboard/config_history.hbs` |
| DASH-003 | Navigates from a configuration history record to the corresponding Service Configs and preselects that config version | The target service must still exist; the unsaved-changes check still runs when leaving Service Configs | No additional request; reuses config-version loading | `app/routes/main.js` |
| DASH-004 | Config History displays the hosts/config group associations and version notes for a version | The data contains `hosts`, `group_id`, and `group_name` | Same as DASH-002 | `app/models/configs/service_config_version.js`, config history view/controller |
| DASH-005 | Dashboard service/host/alert health summaries and navigation come from Dashboard Widgets | Widget layout and content are explicitly excluded Metrics/Widget capabilities and are marked `OUT_OF_SCOPE`; the non-Metrics Dashboard shell contains only tabs/outlets and Config History, so it must not be used to require an independent health summary in React | Excluded | `app/templates/main/dashboard.hbs`, `app/routes/main.js`, dashboard widget templates |

## Global Data Refresh

`global/update_controller.js` maintains independent refresh channels for:

- Hosts and host component state.
- Services, service component state, and stale config.
- Alert definitions, instances, summary, groups, and notifications.
- Upgrade state, background requests, and cluster topology.
- Metrics refresh channels, which are explicitly excluded.

These REST updaters partially overlap with the realtime channels rather than providing a unified fallback; see [generated/realtime-channels.md](generated/realtime-channels.md) for the snapshot/reconciliation semantics of each destination. [generated/api-by-module/background-common.md](generated/api-by-module/background-common.md) is only a broad-regular-expression candidate inventory and does not represent the complete interface set for this module; authoritative network verification must jointly inspect [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entry points](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).
