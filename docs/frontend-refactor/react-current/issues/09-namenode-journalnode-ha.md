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

The React NameNode HA and JournalNode management wizards expose the expected routes and step pages, but they do not yet preserve the complete Classic Ambari workflow. Several gaps can allow an irreversible phase to start with invalid state, incorrectly accept incomplete JournalNode formatting, lose progress during refresh, or continue after a prerequisite request fails.

The detailed Feature ID audit is recorded in `docs/frontend-refactor/react-current/09-namenode-journalnode-ha-gap.md`.

## Proposed Change

Complete Module 09 parity as one change:

- enforce route, persisted-data, topology, HA-state, stack, and service preconditions;
- preserve the nine-step NameNode HA and seven-step/five-step JournalNode state machines, including safe Back, Cancel, and recovery boundaries;
- wait for the complete selected JournalNode and Federation namespace response sets;
- make component installation, configuration, deletion, and progress polling fail closed with focused Retry behavior;
- implement add-only, delete-only, mixed, and no-op JournalNode handling, including delete-only step skipping;
- reload final JournalNode topology before starting components;
- complete dependency-service configuration branches used by the NNHA finalization sequence;
- serialize workflow persistence with navigation and retain recoverable task/request checkpoints; and
- add focused React tests for validation, aggregation, API ordering, failures, retry, and refresh recovery.

Metrics product functionality is outside this issue. Operational checkpoint and JournalNode formatted fields below `metrics/dfs/...` remain in scope. Disable NameNode HA and automatic rollback are not claimed because the Classic implementations are placeholder-only or unreachable.

## Acceptance Criteria

1. NameNode HA cannot start or resume without the required authorization, persisted-data capability, supported topology, and safe component state.
2. Every automatic operation is strictly ordered, durably checkpointed, and stopped on a failed prerequisite or terminal Ambari request.
3. NNHA formatted-status validation waits for every selected JournalNode, including four- and five-node topologies.
4. JournalNode management supports add-only, delete-only, mixed, and no-op validation, with five visible steps for delete-only.
5. Federation configuration and checkpoint validation require an exact response for every expected nameservice/host pair.
6. Refresh resumes the active request or failed operation without replaying completed mutations.
7. Focused tests, the complete React test suite, TypeScript, production build, baseline validation, and diff checks pass, with unrelated pre-existing lint failures documented separately.
