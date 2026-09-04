<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Docker Build Environment

The repository root `start-build-env.sh` creates a Rocky Linux 8 build image
with JDK 17, Python 3.9, RPM tooling, and a checksum-verified Maven
distribution. The source tree and the current user's Maven cache are mounted
into a non-root container user.

Start an interactive shell:

```shell
./start-build-env.sh bash
```

Run a build command directly:

```shell
./start-build-env.sh mvn -B -DskipTests package
```

`BUILD_OS` and `MAVEN_VERSION` may be set explicitly. Only build-image
definitions present under `dev-support/docker` are accepted; the supported
default is `BUILD_OS=rocky8`.
