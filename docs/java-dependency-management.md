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
# Java dependency management

Ambari's Java build baseline is JDK 17 and Maven 3.9.x. The root Maven
Enforcer configuration rejects other Java feature releases, Maven versions
older than 3.9, and direct declarations of dependencies removed during the
Java 17 modernization.

## Version policy

- Manage shared framework versions in `ambari-project/pom.xml`. Use BOMs for
  Jackson, Jersey, JUnit, SLF4J, Spring Framework, and Spring Security.
- Manage build plugin versions in the root `pom.xml`. Child modules should not
  repeat a version for a managed plugin.
- Use stable releases compatible with JDK 17. Do not combine dependency
  maintenance with an unrelated framework major-version migration.
- Keep the server on one SLF4J provider. Logback is the server provider;
  Log4j 1 callers use `log4j-over-slf4j`.
- Align the Servlet API with the deployed Jetty generation. Jetty 11 uses the
  Jakarta Servlet 5 API.
- New code uses Jakarta JAXB and Jakarta Mail, Commons Lang 3, and the
  maintained `com.github.mwiede:jsch` fork.

Dependabot proposes grouped monthly patch and minor updates. Every proposed
update still requires compatibility review and the focused subsystem tests.
Major updates are intentionally ignored and must be proposed explicitly.

## Verification

Run Java verification in the Ambari build container with JDK 17 and Maven
3.9.x. Record the exact commands and results in the pull request. The baseline
commands are:

```shell
mvn -version
mvn -DskipTests -DskipPythonTests -DskipAdminWebTests=true -DskipUiBuild=true validate
mvn -am test -pl ambari-server,ambari-funtest -DskipPythonTests -DskipFunctionalTests=false -Drat.skip -DskipAdminWebTests=true -DskipUiBuild=true
mvn -B -Psbom -DskipTests -DskipPythonTests -DskipAdminWebTests=true -DskipUiBuild=true -Drat.skip org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
```

Review `ambari-server`'s runtime dependency tree after changing logging,
Servlet, persistence, security, database, or serialization dependencies.

## Deferred migrations

AMBARI-26642 also tracks four migrations that require dedicated source and
contract changes rather than version-only updates:

- Swagger 1 annotations and the custom reader to an OpenAPI 3 Jakarta scanner,
  including canonical specification comparison and a maintained UI.
- PowerMock tests to injectable collaborators or scoped Mockito static and
  construction mocks, with Jupiter/Vintage retained during the transition.
- Apache HttpClient 4 callers to HttpClient 5 in module-sized batches.
- H2 1.4 test infrastructure to H2 2.x, including SQL dialect, connection
  lifecycle, schema cleanup, and repeated persistence-context initialization.

Until those migrations land, their compatibility dependencies remain managed
and must not be upgraded mechanically.
