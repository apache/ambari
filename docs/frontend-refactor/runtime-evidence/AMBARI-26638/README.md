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

# AMBARI-26638 Runtime Evidence

These screenshots were captured from a three-node Rocky Linux 8 ARM64
container cluster managed by the React frontend. VictoriaMetrics Server,
VMAGENT, and VMAUTH were installed through an Ambari Blueprint and reached the
`STARTED` state.

- `victoriametrics-service-summary.png` shows the generic component summary and
  live service health state.
- `prometheus-scrape-targets.png` shows healthy Linux and component telemetry
  targets discovered through Ambari HTTP service discovery.
- `monitoring-dashboard-catalog.png` shows the provisioned Linux, HDFS, YARN,
  HBase, and Hive dashboards.
- `linux-fleet-dashboard.png` shows native Linux telemetry from all three hosts.
- `hdfs-service-metrics.png` shows HDFS service dashboard queries.
- `hbase-service-metrics.png` shows HBase RegionServer metrics and JVM telemetry.
