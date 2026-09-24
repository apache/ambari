<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Corrective Implementation: 2026-09-22

This records the user-approved correction after design and source review.
Implementation lives in `ambari-mpack-v1`; the independent `ambari-mpacks`
repository is mpackstore. No online marketplace is required. Source changes
remain uncommitted, so HEAD alone does not identify the implementation.

The user requested coordination before deployment acceptance. No live
Server/Agent deployment has started during this correction.

## Behavior

- Whole-store IMPORT registers all releases and a persistent service catalog
  without hooks or definition activation. Initial import preserves the unmanaged
  distribution runtime and its task protocol. Removing an inactive import does
  not execute uninstall hooks for definitions it does not own.
- Java resolves each package dependency closure into selectable services. A
  multi-service package exposes individual services; foundation resources remain
  dependencies. Mixed platforms are not bound to an arbitrary common stack.
  Exact service/destination selections survive the new-cluster or Add Service
  handoff and browser refresh.
- Scoped definition reservations replace the long-lived global mutation gate.
  Domain service/component/configuration/topology guards cover internal callers
  too. Task admission, retries and publication synchronize through transaction
  completion. Blockers identify exact cluster/request/task IDs and statuses.
  Unrelated work continues; prepared tasks retain compatible original resources.
- Advisor imports do not write bytecode into immutable snapshots. Publication
  restores a verified previous view or keeps affected readers explicitly gated.
  Unknown DB commit outcomes do not become assumed success. Confirmed terminal
  records can reconcile an orphaned runtime reservation without a Server restart.
- Recovery persists valid FAILED/NOT_APPLIED receipts. Cancellation records
  CANCELLING and the requesting administrator before changing the retained view;
  failed notification or restart resumes cancellation, not the original install.
  Hooks follow dependency order, and completed effects are not replayed on retry.
- UI import and service selection are the default flow. Rejected submissions can
  be replanned; uncertain submissions retain their idempotency identity with an
  accessible recovery action. Generic models use declared component categories
  and observed service state. Declared custom commands reuse the existing
  host-command authorization, confirmation and request tracking.

See [HTTP contract](http-api.md) for IMPORT/ENABLE, service catalog/planning,
deployment handoff, scoped plan fields and CANCELLING. Incomplete plans from older
draft schema-1 implementations must be recreated. Online hooks require
`scope: "DEFINITIONS"`; omitted/SERVER scope can be imported but cannot execute
online. This is an author-declared effect contract, not a sandbox. Unsupported
deployed component-model changes reject until a tested migration exists.

## Local Verification

Final commands completed successfully on 2026-09-22:

| Check | Result |
| --- | --- |
| Java compilation and focused suites | 21 suites, 136 tests; no failures, errors or skips; totals checked against Surefire XML |
| React component/contract suites | 87 tests passed; checked against the Vitest JSON report |
| React production build | Passed; existing Sass deprecations and bundle-size warnings remain |
| Python authoring/CLI contracts | 16 tests passed |
| Agent FileCache/ConfigurationBuilder | 40 tests passed |
| Deterministic mpackstore build | All four archives byte-identical across two builds |
| Worktree whitespace checks | `git diff --check` passed in both repositories |

The implementation branch is `work/mpack-v1-http`, still based on
`116be946fb5f911236db70861631eb4ed95792cc`. No commit or staging was performed.
Earlier verification failures included an obsolete global-gate assertion, missing
Router context in wizard tests and mismatched translated labels; these were fixed
and the affected suites rerun successfully. Missing temporary Python dependencies
were installed using the existing locks, including the separately locked docopt
source requirement. These local outcomes do not change live acceptance to PASS.

Java, from the implementation worktree:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  /tmp/apache-maven-3.9.11/bin/mvn -B -ntp \
  -l /tmp/ambari-mpack-final-java.log -Pstatic-web -pl ambari-server test \
  -Dtest=MpackManifestTest,MpackArchiveStoreTest,MpackDependenciesTest,MpackSnapshotsTest,MpackLifecycleStateTest,MpackRecordDAOTest,StackCandidateContextTest,MpackLifecycleServiceTest,MpackPlannerTest,MpackRuntimeTest,MpackActivationTest,MpackTaskUsageTest,MpackServiceCatalogTest,MpackExecutionResourcesTest,MpackMutationInterceptorTest,StackAdvisorHelperTest,StackAdvisorRunnerTest,ExecutionCommandWrapperTest,VersionDefinitionTest,StackManagerTest,RepositoryVersionResourceProviderTest \
  -DskipTests=false -DskipPythonTests=true -DskipSurefireTests=false \
  -Dexec.skip=true -Dcheckstyle.skip=true -Drat.skip=true -Dswagger.skip=true \
  -DskipUiBuild=true -Dsurefire.argLine=
```

The reactor `-pl ambari-server -am test-compile` also passed earlier with the same
exclusions. Python/distribution packaging, RAT, checkstyle and Swagger generation
were not exercised. Tests include real advisor subprocess import and real Java
resolution of multi-service definitions; injected recovery failures are not live
deployment/crash acceptance.

React, from `ambari-web/latest`:

```sh
npm run build
npm test -- src/Utils/genericServiceModels.test.ts src/Utils/declaredServiceCommands.test.ts src/screens/Services/Actions.test.tsx src/screens/Mpacks/Mpacks.test.tsx src/api/mpackApi.test.ts src/screens/ClusterWizard/Step4.test.tsx src/components/AssignMasters.test.tsx src/components/AssignMastersAddable.test.tsx src/screens/Directories/GlobalDirectoryLayout.test.tsx src/api/versionsApi.test.ts --reporter=json --outputFile=/tmp/ambari-mpack-final-ui-tests.json
```

These are jsdom component/contract tests and a production asset build, not
authenticated-browser or mobile-layout acceptance.

Python tooling, from the implementation worktree:

```sh
/tmp/ambari-mpack-v1-py310/bin/python -m pip install -e dev-support/mpack
/tmp/ambari-mpack-v1-py310/bin/python -m unittest discover -s dev-support/mpack/tests -v
```

The temporary environment initially lacked the editable tool and Agent runtime
dependencies. Installed the tool and existing hash-locked Agent, Agent sdist and
Server requirements without changing dependency manifests. Agent regression,
from `ambari-agent/src/test/python`:

```sh
TMPDIR=/private/tmp \
PYTHONPATH=/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/test/python/ambari_agent:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/main/python/ambari_agent:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/main/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-common/src/test/python:/Users/jialiang/PRJS/ambari-mpack-v1/ambari-agent/src/test/python \
  /tmp/ambari-mpack-v1-py310/bin/python -m unittest TestFileCache TestConfigurationBuilder -v
```

## Store Artifact

Built twice from the independent repository:

```sh
/tmp/ambari-mpack-v1-py310/bin/python -m ambari_mpack --json build --all --repository /Users/jialiang/PRJS/ambari-mpacks --output /tmp/ambari-mpackstore-20260922-a --bundle mpackstore
/tmp/ambari-mpack-v1-py310/bin/python -m ambari_mpack --json build --all --repository /Users/jialiang/PRJS/ambari-mpacks --output /tmp/ambari-mpackstore-20260922-b --bundle mpackstore
```

Both builds succeeded; the three member archives and bundle are byte-identical.
Bundle SHA-256:
`a917dbdaafdf483d7423e03ee2b6192ae0926bc4895a7e82384e9c5b3b7b3f3e`.
The current release contains generic-base, nginx and postgresql. HDFS reference
export and advanced live workflows remain separately tracked acceptance work.

## Deployment Handoff

Build Server/Agent packages from the current dirty worktree, including untracked
implementation files. Store archives and frontend assets are prepared; the Java
checks above do not build a complete deployable Ambari distribution. Obtain the
user's deployment environment and procedure first, then execute the
[runbook](acceptance-runbook.md) and record structured results and exact lineage
in [acceptance-results.md](acceptance-results.md). All live AC-00 through AC-20
cases remain NOT_RUN. Automatic GC, aggregate quotas, online coordinated backup,
arbitrary frontend plugins and a new workflow engine remain outside this delivery.
