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

export const sqoop_properties = [
  // Sqoop Server Configuration
  {
    category: "General",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.client.autoconnect.url",
    serviceName: "SQOOP",
  },
  {
    category: "General",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.client.autoconnect.username",
    serviceName: "SQOOP",
  },
  {
    category: "General",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.client.autoconnect.password",
    serviceName: "SQOOP",
  },
  {
    category: "General",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.server.location",
    serviceName: "SQOOP",
  },
  {
    category: "General",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.server.port",
    serviceName: "SQOOP",
  },
  {
    category: "Performance",
    filename: "sqoop-site.xml",
    name: "sqoop.connection.factories",
    serviceName: "SQOOP",
  },
  {
    category: "Performance",
    filename: "sqoop-site.xml",
    name: "sqoop.tool.plugins",
    serviceName: "SQOOP",
  },
  {
    category: "Security",
    filename: "sqoop-site.xml",
    name: "sqoop.metastore.client.enable.autoconnect",
    serviceName: "SQOOP",
  },
  // Environment Configuration
  {
    category: "Environment",
    filename: "sqoop-env.xml",
    name: "sqoop_user",
    serviceName: "SQOOP",
  },
  {
    category: "Environment",
    filename: "sqoop-env.xml",
    name: "sqoop_group",
    serviceName: "SQOOP",
  },
  {
    category: "Environment",
    filename: "sqoop-env.xml",
    name: "sqoop_log_dir",
    serviceName: "SQOOP",
  },
  {
    category: "Environment",
    filename: "sqoop-env.xml",
    name: "sqoop_pid_dir",
    serviceName: "SQOOP",
  },
];
