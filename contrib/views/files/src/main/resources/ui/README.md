<!---
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

# Files View UI

This directory contains the React and TypeScript frontend for Ambari's HDFS
Files View. The Java View services remain in the parent Maven module and expose
the HDFS operations used by this application.

## Prerequisites

- Node.js 20 or newer
- npm 10 or newer

## Install

```bash
npm ci
```

## Test

```bash
npm test
```

## Build

```bash
npm run build
```

Vite writes deployable resources to `dist`. Maven runs `npm ci` and the
production build before packaging that directory into the Files View JAR.

The production application derives its View name, version, and instance from
the Ambari URL. A standalone development server therefore needs an Ambari API
proxy or mocked API responses.

The UI preserves the existing Files View REST contract under
`resources/files`: service health, home/trash discovery, directory listing,
upload, preview, file operations, archive downloads, and concatenated file
downloads.
