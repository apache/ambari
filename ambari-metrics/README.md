<!--
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

# Ambari Metrics RPM

This module packages the default metrics storage provider distributed with
Ambari. It does not compile VictoriaMetrics. The build downloads the official
standalone, cluster, and vmutils release archives, verifies pinned SHA-256
digests, and includes only the six binaries used by the Ambari service.

The module is excluded from normal development builds. Build the RPM explicitly:

```bash
mvn -Pmetrics-rpm -pl ambari-metrics \
  -Dbuild.os_arch=x86_64 package
```

Supported RPM architectures are `x86_64` and `aarch64`. The release URL and
cache directory can be overridden for release mirrors or offline builds:

```bash
-Dvictoriametrics.release.baseUrl=https://mirror.example/releases/v1.150.0
-Dvictoriametrics.cache.directory=/path/to/cache
```

When updating VictoriaMetrics, update the version, standalone and cluster
commits, archive digests, required binary digests, and Ambari third-party
license records together.
