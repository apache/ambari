<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Ui

To start the UI in development mode, developer has to proxy the xhr calls
to correct ambari endpoint.

**ember serve --proxy http://c6401.ambari.apache.org:8080/api/v1/views/HIVE/versions/{version}/instances/{instance_name}**

Example:
```
$ cd src/main/resources/ui
$ ember serve --proxy http://c6401.ambari.apache.org:8080/api/v1/views/HIVE/versions/1.5.0/instances/AUTO_HIVE_INSTANCE
```

