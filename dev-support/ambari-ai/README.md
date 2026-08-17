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

# Ambari AI contribution tool

`ambari_ai.py` gives an AI agent a small command-line interface for creating
an ASF JIRA and submitting, approving, or merging an Ambari pull request. It
uses only the Python 3 standard library.

## Configuration

```shell
export ASF_JIRA_USER="your-asf-jira-user"
export ASF_JIRA_TOKEN="your-asf-jira-password-or-token"
export GITHUB_TOKEN="token-used-to-create-and-merge-pull-requests"
export GITHUB_FORK_REPO="JiaLiangC/ambari"
```

Set `GITHUB_REVIEW_TOKEN` when pull requests must be approved by a different
GitHub account. GitHub does not allow a pull request author to approve their
own pull request. Git pushes use an existing `fork` remote when present and
otherwise use `git@github.com:<GITHUB_FORK_REPO>.git`.

## Create an issue

Issue descriptions accept Markdown. The tool converts headings, lists, links,
and code to Jira wiki markup before submitting the description.

```shell
python3 dev-support/ambari-ai/ambari_ai.py issue create \
  --type Bug \
  --summary "Fix ambari server py test" \
  --description "Describe the failure, expected behavior, and scope."
```

Update an existing issue description with the same conversion:

```shell
python3 dev-support/ambari-ai/ambari_ai.py issue update \
  --issue AMBARI-26474 \
  --description-file path/to/issue.md
```

## Create a pull request

Run this after changing and testing the code. Only paths passed through
`--files` are staged and committed.

```shell
python3 dev-support/ambari-ai/ambari_ai.py pr create \
  --issue AMBARI-26474 \
  --changes "Fix TestAmbariServer initialization." \
  --test-result "The affected Python test passed." \
  --test-command 'mvn clean -am test -pl ambari-server -Dpython.test.mask="TestAmbariServer.py"' \
  --files ambari-server/src/test/python/TestAmbariServer.py
```

The defaults match PR `#3995`:

```text
Branch:   AMBARI-26474
Commit:   AMBARI-26474: Fix ambari server py test
PR title: AMBARI-26474: Fix ambari server py test
Base:     trunk
```

The summary is read from JIRA unless `--summary` is supplied. The branch is
created from `origin/trunk` by default. Use `--base` and `--base-ref` for a
different target. The generated pull request body starts with a link to the
corresponding ASF JIRA issue.

## Approve and merge

```shell
python3 dev-support/ambari-ai/ambari_ai.py pr approve --number 3995
python3 dev-support/ambari-ai/ambari_ai.py pr merge --number 3995
```

Merge waits until all reported commit statuses and check runs pass. It refuses
failed CI and sends the checked head SHA with the merge request. Use
`--skip-ci` only when the repository has no CI for that pull request.

All successful commands print JSON to stdout. Errors print JSON to stderr and
return a nonzero exit status, so an AI agent can consume the commands directly.

Run the tests with:

```shell
cd dev-support/ambari-ai
python3 -m unittest -v
```
