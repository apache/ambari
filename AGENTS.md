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
--->

# Apache Ambari Agent Guide

## Scope

These instructions apply to the entire repository. A more specific
`AGENTS.md` in a subdirectory overrides this file for that subtree.

## Repository Overview

Apache Ambari is a Maven multi-module project targeting JDK 17. Major areas
include:

- `ambari-server`: Java server code and Python server utilities.
- `ambari-agent`: Python agent code and supporting Java utilities.
- `ambari-common`, `ambari-server-spi`, and `ambari-utility`: shared code.
- `ambari-web`: the legacy Ember frontend.
- `ambari-web/latest`: the React and TypeScript frontend built with Vite.
- `ambari-admin`, `ambari-views`, and `contrib`: additional UI and extension
  modules.
- `dev-support`: development, build, and contribution tools.

## Working Rules

- Inspect `git status` before making changes. The worktree may contain
  unrelated user changes. Do not modify, stage, delete, or revert them.
- Keep each change limited to the requested module and behavior. Avoid
  unrelated refactoring, formatting, dependency updates, or generated-file
  churn.
- Follow the style and architecture of the surrounding code. Prefer existing
  helpers, APIs, test frameworks, and dependency versions.
- Add or update focused tests for behavior changes. Include failure and
  recovery paths when the affected code has stateful or asynchronous behavior.
- Use English for source code, comments, scripts, CLI output, JIRA content,
  commit messages, and pull request content. Preserve existing localization
  resources and translated user-facing strings.
- Add the standard ASF license header to new source and documentation files.
- Never place passwords, tokens, private keys, cookies, or other credentials in
  repository files, command output, commits, issues, or pull requests.
- Do not claim a test passed unless the command was actually run successfully.
  Report skipped tests and existing failures explicitly.

## Frontend Refactor Baseline

`docs/frontend-refactor` is temporary working material for the migration from
the legacy Ember frontend in `ambari-web/classic` to the React frontend in
`ambari-web/latest`. It is a refactor baseline, not permanent product
documentation, and it will be deleted after the React refactor is complete and
accepted.

When implementing or reviewing React frontend work:

- Read the relevant files under `docs/frontend-refactor/ember-baseline` before
  changing a feature. They document the legacy Ember behavior, entry points,
  permissions, feature flags, API calls, asynchronous flows, error handling,
  recovery behavior, and tests.
- Before implementing or committing every React frontend fix, inspect both the
  corresponding legacy Ember source and the relevant Ember baseline documents.
  If the React behavior intentionally differs, record the reason and add focused
  regression coverage for that decision.
- Compare the React implementation with both the baseline and the actual Ember
  source. If the documentation conflicts with source code or runtime behavior,
  verify the behavior and update the baseline rather than guessing.
- Do not treat matching routes, page names, or component names as feature
  parity. Compare user-visible behavior, permissions, API payloads, success and
  failure paths, polling or realtime behavior, and refresh or retry recovery.
- Use `docs/frontend-refactor/react-current` to record the current React
  implementation and remaining gaps. Keep gap status and supporting evidence
  aligned with the reviewed code.
- Do not manually edit generated evidence under
  `docs/frontend-refactor/ember-baseline/generated`. Regenerate it with the
  baseline extraction tool when the legacy source changes.
- Do not delete `docs/frontend-refactor` during incremental refactor work. It
  should be removed only when the overall React migration is declared complete.

## JIRA And Pull Requests

Ambari tracks work in ASF JIRA. Create an issue with a meaningful type,
summary, and description before submitting code. Reuse an existing issue when
one already covers the work.

Use the JIRA key consistently so ASF integrations associate the pull request:

```text
Branch:   AMBARI-26474
Commit:   AMBARI-26474: Fix ambari server py test
PR title: AMBARI-26474: Fix ambari server py test
Base:     trunk
```

Use this pull request body structure:

```markdown
## What changes were proposed in this pull request?

Describe the problem, the implementation, and any important behavior changes.

## How was this patch tested?

List the exact commands that were run and their results. Include before and
after evidence for UI or behavior changes when useful.

Please review [Ambari Contributing Guide](https://cwiki.apache.org/confluence/display/AMBARI/How+to+Contribute) before opening a pull request.
```

Submit branches from the contributor's personal fork to `apache/ambari`. The
pull request author must not approve their own pull request. Another Ambari
committer or PMC member reviews, approves, and merges it.

### Reviewable Commit Structure

Keep one pull request for one coherent JIRA deliverable, but organize large
changes into reviewable topic commits.

- A change must use multiple commits when it modifies more than 1,000
  non-generated lines, touches more than 15 source or test files, spans
  multiple functional modules or workflows, or combines shared infrastructure
  with multiple consumers.
- Split commits by coherent behavior or dependency boundary, such as shared
  infrastructure, one workflow, one UI integration, server support, and
  generated evidence. Do not split changes mechanically only to satisfy a
  commit count.
- Keep focused tests in the same commit as the behavior they verify. Put
  generated files, parity matrices, and broad mechanical evidence updates in a
  separate final commit when practical.
- Each intermediate commit should compile independently and should pass its
  applicable focused tests. When an API or type contract change would break an
  existing consumer, commit the contract and the required consumer updates
  together.
- Use the JIRA key in every commit subject. Before pushing, inspect the commit
  sequence from the target base and verify that every changed path belongs to
  exactly one intended topic.
- A large commit that cannot be split without breaking atomicity must have a
  clear reason in the pull request description.

The helper at `dev-support/ambari-ai/ambari_ai.py` may create JIRA issues,
commit explicitly selected files, push a branch, and create a pull request.
Always pass an explicit `--files` list so unrelated worktree changes are not
included.
