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
# Apache Ambari
[![Build Status](https://builds.apache.org/buildStatus/icon?job=Ambari-trunk-Commit)](https://builds.apache.org/view/A/view/Ambari/job/Ambari-trunk-Commit/)
![license](http://img.shields.io/badge/license-Apache%20v2-blue.svg)

Apache Ambari is a tool for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari consists of a set of RESTful APIs and a browser-based management interface. Its monitoring stack uses Prometheus-compatible APIs, Ambari Agent exporters, and a bundled VictoriaMetrics storage provider.

The optional [Ambari Metrics RPM](ambari-metrics/README.md) packages the pinned
VictoriaMetrics release used by the default deployment. It replaces the legacy
Ambari Metrics System and Ganglia integrations; it is not the former external
`apache/ambari-metrics` project.

## Sub-projects

- Ambari Log Search ([GitHub](https://github.com/apache/ambari-logsearch), [GitBox](https://gitbox.apache.org/repos/asf?p=ambari-logsearch.git)) 
- Ambari Infra ([GitHub](https://github.com/apache/ambari-infra), [GitBox](https://gitbox.apache.org/repos/asf?p=ambari-infra.git))

## Getting Started

https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide

## Built With

https://cwiki.apache.org/confluence/display/AMBARI/Technology+Stack

## Contributing

https://cwiki.apache.org/confluence/display/AMBARI/How+to+Contribute

## License

http://ambari.apache.org/license.html
