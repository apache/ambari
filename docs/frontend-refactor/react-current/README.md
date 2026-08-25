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

# React Refactor Current-State Evidence

This directory records the reviewed React implementation for each legacy module. The module comparison documents are the source for initial matrix status and evidence; matching page, route, component, or endpoint names alone does not establish parity.

## Parity Matrix Workflow

Generate the initial matrix after all 14 module comparison documents exist:

```bash
node docs/frontend-refactor/react-current/tools/generate-react-parity-matrix.mjs \
  --source gaps \
  --reviewed-commit <full-base-commit> \
  --reviewed-by "<reviewer>, <YYYY-MM-DD>" \
  --issue <fallback-issue-or-decision-reference>
```

The generator resolves explicitly superseded initial tables in favor of final or post-implementation tables. It fails on missing in-scope IDs, unknown IDs, unsupported status vocabulary, an invalid fallback JIRA key, or equally authoritative duplicate rows. Static source statuses, including `COVERED`, `MATCH`, `PASS`, and `STATIC_COMPLETE`, normalize to `NEEDS_RUNTIME_VALIDATION`; only an explicit later acceptance review may set `COVERED` in the editable matrix. Such a row must set `source_status=RUNTIME_VALIDATED` and record the runtime scenario plus independent normal and failure/recovery evidence.

A one-commit pull request cannot truthfully embed its own final commit SHA. Therefore `reviewed_commit` is the full base commit used to start the audit, not the commit containing the matrix. The matrix also records `reviewed_diff_sha256`, calculated from a canonical list of paths changed from that base plus each path's final file mode and contents under `ambari-web/latest` and `docs/frontend-refactor/react-current`. Deleted paths are recorded explicitly, and untracked additions use the same representation before and after they are committed. The three matrix output files are excluded to avoid self-reference. Together the base SHA and diff digest form the reproducible reviewed source anchor; the validator recomputes the digest and fails after any audited source or module evidence changes.

The JSON matrix is the preferred editable source. After editing it, synchronize CSV and Markdown with:

```bash
node docs/frontend-refactor/react-current/tools/generate-react-parity-matrix.mjs --source json
```

CSV can instead be edited and imported with `--source csv`. Both commands rewrite all three representations so they remain identical. Markdown is rendered output and must not be edited directly.

Validate the canonical 1,154-ID set, statuses, issue references, review fields, Metrics exclusions, and JSON/CSV/Markdown synchronization with:

```bash
node docs/frontend-refactor/react-current/tools/validate-react-parity-matrix.mjs
```

Metrics `OUT_OF_SCOPE` and legacy `PLACEHOLDER` rows remain in the matrix for ID completeness but do not enter the completion denominator. `MISSING`, `PARTIAL`, `BLOCKED`, and `BEHAVIOR_DIFF` rows require an issue or explicit decision reference. `NOT_APPLICABLE` also requires a recorded reason and decision reference.
