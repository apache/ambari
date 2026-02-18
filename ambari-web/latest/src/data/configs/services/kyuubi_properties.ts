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
export const kyuubi_properties = [
  // Kyuubi Server Configuration
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.bind.host",
    serviceName: "KYUUBI",
  },
  {
    category: "General", 
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.bind.port",
    serviceName: "KYUUBI",
  },
  {
    category: "General",
    filename: "kyuubi-defaults.xml", 
    name: "kyuubi.authentication",
    serviceName: "KYUUBI",
  },
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.protocols",
    serviceName: "KYUUBI",
  },
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.type",
    serviceName: "KYUUBI",
  },
  
  // Session Management
  {
    category: "Session",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.session.timeout",
    serviceName: "KYUUBI",
  },
  {
    category: "Session",
    filename: "kyuubi-defaults.xml", 
    name: "kyuubi.session.check.interval",
    serviceName: "KYUUBI",
  },
  {
    category: "Session",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.operation.idle.timeout", 
    serviceName: "KYUUBI",
  },
  
  // Engine Configuration
  {
    category: "Engine",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.spark.sql.adaptive.enabled",
    serviceName: "KYUUBI",
  },
  {
    category: "Engine", 
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.spark.sql.adaptive.coalescePartitions.enabled",
    serviceName: "KYUUBI",
  },
  {
    category: "Engine",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.share.level",
    serviceName: "KYUUBI",
  },
  
  // Security
  {
    category: "Security",
    filename: "kyuubi-defaults.xml", 
    name: "kyuubi.kinit.principal",
    serviceName: "KYUUBI",
  },
  {
    category: "Security",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.kinit.keytab",
    serviceName: "KYUUBI",
  },
  {
    category: "Security", 
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.connection.url.use.hostname",
    serviceName: "KYUUBI",
  },
  
  // Performance
  {
    category: "Performance",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.pool.size",
    serviceName: "KYUUBI", 
  },
  {
    category: "Performance",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.initialize.timeout",
    serviceName: "KYUUBI",
  },
  {
    category: "Performance",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.backend.server.exec.pool.size", 
    serviceName: "KYUUBI",
  },
  
  // Environment Configuration
  {
    category: "Environment",
    filename: "kyuubi-env.xml",
    name: "kyuubi_user",
    serviceName: "KYUUBI",
  },
  {
    category: "Environment",
    filename: "kyuubi-env.xml", 
    name: "kyuubi_group",
    serviceName: "KYUUBI",
  },
  {
    category: "Environment",
    filename: "kyuubi-env.xml",
    name: "kyuubi_log_dir",
    serviceName: "KYUUBI",
  },
  {
    category: "Environment",
    filename: "kyuubi-env.xml",
    name: "kyuubi_pid_dir", 
    serviceName: "KYUUBI",
  },
  
  // Additional common Kyuubi properties
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.ha.enabled",
    serviceName: "KYUUBI",
  },
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.ha.namespace",
    serviceName: "KYUUBI",
  },
  {
    category: "General",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.ha.zookeeper.quorum",
    serviceName: "KYUUBI",
  },
  {
    category: "Engine",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.idle.timeout",
    serviceName: "KYUUBI",
  },
  {
    category: "Engine",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.engine.request.timeout",
    serviceName: "KYUUBI",
  },
  {
    category: "Performance",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.min.worker.threads",
    serviceName: "KYUUBI",
  },
  {
    category: "Performance",
    filename: "kyuubi-defaults.xml",
    name: "kyuubi.frontend.max.worker.threads",
    serviceName: "KYUUBI",
  },
];
