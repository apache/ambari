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

import { describe, expect, it } from "vitest";
import { buildBlueprintExport } from "./blueprintExport";

describe("cluster Blueprint export", () => {
  it("groups equal host topologies and retains configs, clients, and ALL components", () => {
    const result = buildBlueprintExport({
      clusterName: "cluster1",
      stackName: "HDP",
      stackVersion: "3.0",
      hosts: ["host1", { name: "host2" }],
      selectedServiceNames: ["HDFS"],
      masterAssignments: [{
        host_name: "host1",
        masterServices: [{ component: "NAMENODE", hostName: "host1" }],
      }],
      slaveAssignments: [
        { hostname: "host1", checkboxes: [{ checked: true, label: "DATANODE" }] },
        { hostname: "host2", checkboxes: [{ checked: true, label: "CLIENT" }] },
      ],
      serviceComponents: [{ components: [
        { StackServiceComponents: { cardinality: "1", component_name: "NAMENODE", service_name: "HDFS" } },
        { StackServiceComponents: { cardinality: "1+", component_name: "DATANODE", service_name: "HDFS" } },
        { StackServiceComponents: { cardinality: "0+", component_name: "HDFS_CLIENT", is_client: true, service_name: "HDFS" } },
        { StackServiceComponents: { cardinality: "ALL", component_name: "REQUIRED", service_name: "HDFS" } },
      ] }],
      configProperties: {
        HDFS: {
          "core-site": {
            properties: {
              auth: {
                final: "true",
                propertyAttributes: { type: "string" },
                propertyName: "hadoop.security.authentication",
                type: "core-site",
                value: "kerberos",
              },
            },
          },
        },
      },
    });

    expect(result.blueprint.Blueprints).toEqual({
      blueprint_name: "cluster1",
      stack_name: "HDP",
      stack_version: "3.0",
    });
    expect(result.blueprint.configurations).toEqual([{
      "core-site": {
        properties: { "hadoop.security.authentication": "kerberos" },
        properties_attributes: {
          isFinal: { "hadoop.security.authentication": "true" },
        },
      },
    }]);
    expect(result.blueprint.host_groups).toEqual([
      {
        cardinality: "1",
        components: [{ name: "DATANODE" }, { name: "NAMENODE" }, { name: "REQUIRED" }],
        name: "host_group_0",
      },
      {
        cardinality: "1",
        components: [{ name: "HDFS_CLIENT" }, { name: "REQUIRED" }],
        name: "host_group_1",
      },
    ]);
    expect(result.clusterTemplate.host_groups[1]).toEqual({
      name: "host_group_1",
      hosts: [{ fqdn: "host2" }],
    });
  });
});
