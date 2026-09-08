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

# Ambari Classic Web UI

The classic Ember application is built from this directory. The parent
`ambari-web/pom.xml` invokes Brunch here and packages `public/`; package files
in the parent directory are not part of the classic build.

Yarn 1.22.22 is the canonical package manager. Do not generate or commit an npm
`package-lock.json`. Install exactly the committed graph before running local
commands:

```bash
yarn install --ignore-engines --ignore-optional --frozen-lockfile --non-interactive
yarn build
yarn test
yarn test:server
```

Maven installs Node 22.22.2 for the legacy application. Installation skips the
optional legacy `fsevents` dependency; Brunch uses its portable file watcher.

The `resolutions` in `package.json` intentionally replace vulnerable
transitive build dependencies without changing the legacy application or
Brunch plugin APIs. Keep those resolutions and `yarn.lock` synchronized by
running Yarn 1.22.22.
