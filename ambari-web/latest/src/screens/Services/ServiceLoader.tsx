/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useParams } from "react-router-dom";
import ServiceDashboard from "./ServiceDashboard";

const serviceByComponent: Record<string, string> = {
  namenode: "HDFS",
  snamenode: "HDFS",
  secondary_namenode: "HDFS",
  journalnode: "HDFS",
  resourcemanager: "YARN",
  historyserver: "YARN",
  app_timeline_server: "YARN",
  yarn_registry_dns: "YARN",
  rangeradmin: "RANGER",
  metrics_collector: "AMBARI_METRICS",
  ssm_server: "SSM",
  hive_server: "HIVE",
  hive_metastore: "HIVE",
  hawq: "HAWQ",
};

function serviceNameForComponent(componentName?: string) {
  return componentName
    ? serviceByComponent[componentName.toLowerCase()]
    : undefined;
}

function ServiceLoader() {
  const { componentName } = useParams();
  const serviceName = serviceNameForComponent(componentName);

  return serviceName ? (
    <ServiceDashboard serviceName={serviceName} />
  ) : (
    <div>Service Dashboard Not Available</div>
  );
}

export default ServiceLoader;
