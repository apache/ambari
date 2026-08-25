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
import {
  buildExpressServiceRestartRequest,
  buildServiceRestartSchedule,
  getServiceRestartGroups,
  hasActiveServiceComponentRestart,
  type RestartableServiceComponent,
  selectServiceRestartComponents,
} from "./serviceRestartUtils";

function host(
  hostName: string,
  componentName: string,
  haState = "",
) {
  return {
    HostRoles: {
      host_name: hostName,
      component_name: componentName,
      display_name: componentName,
      maintenance_state: "OFF",
      ha_state: haState,
    },
  };
}

function restartComponent(
  category: "MASTER" | "SLAVE",
  componentName: string,
  hostName: string,
  maintenanceState = "OFF",
): RestartableServiceComponent {
  return {
    serviceName: "HDFS",
    componentName,
    displayName: componentName,
    hostName,
    category,
    maintenanceState,
    haState: "",
  };
}

describe("service restart planning", () => {
  it("selects all, master-only, and slave-only host components", () => {
    const groups = getServiceRestartGroups("YARN", {
      masterComponents: [{
        componentName: "RESOURCEMANAGER",
        hostComponents: [host("rm1", "RESOURCEMANAGER")],
      }],
      slaveComponents: [{
        componentName: "NODEMANAGER",
        hostComponents: [
          host("nm1", "NODEMANAGER"),
          host("nm2", "NODEMANAGER"),
        ],
      }],
      clientComponents: [{
        componentName: "YARN_CLIENT",
        hostComponents: [host("client1", "YARN_CLIENT")],
      }],
    });

    expect(selectServiceRestartComponents(groups, "MASTERS").map(
      (component) => component.hostName,
    )).toEqual(["rm1"]);
    expect(selectServiceRestartComponents(groups, "SLAVES").map(
      (component) => component.hostName,
    )).toEqual(["nm1", "nm2"]);
    expect(selectServiceRestartComponents(groups, "ALL").map(
      (component) => component.hostName,
    )).toEqual(["rm1", "nm1", "nm2"]);
  });

  it("preserves the intended HDFS HA master restart order", () => {
    const groups = getServiceRestartGroups("HDFS", {
      isNameNodeHaEnabled: true,
      masterComponents: [
        {
          componentName: "NAMENODE",
          hostComponents: [
            host("nn-active", "NAMENODE", "ACTIVE"),
            host("nn-standby", "NAMENODE", "STANDBY"),
          ],
        },
        {
          componentName: "JOURNALNODE",
          hostComponents: [
            host("jn1", "JOURNALNODE"),
            host("jn2", "JOURNALNODE"),
          ],
        },
      ],
      slaveComponents: [
        {
          componentName: "ZKFC",
          hostComponents: [
            host("nn-active", "ZKFC"),
            host("nn-standby", "ZKFC"),
          ],
        },
        {
          componentName: "DATANODE",
          hostComponents: [host("dn1", "DATANODE")],
        },
      ],
    });

    expect(groups.masters.map(
      (component) => `${component.componentName}@${component.hostName}`,
    )).toEqual([
      "JOURNALNODE@jn1",
      "JOURNALNODE@jn2",
      "NAMENODE@nn-standby",
      "ZKFC@nn-standby",
      "NAMENODE@nn-active",
      "ZKFC@nn-active",
    ]);
    expect(selectServiceRestartComponents(groups, "ALL").filter(
      (component) => component.componentName === "ZKFC",
    )).toHaveLength(2);

    const schedule = buildServiceRestartSchedule({
      clusterName: "c1",
      serviceName: "HDFS",
      components: selectServiceRestartComponents(groups, "ALL"),
      batchSize: 10,
      intervalTimeSeconds: 120,
      tolerateSize: 0,
    })[0].RequestSchedule;
    const requests = schedule.batch[0].requests ?? [];

    expect(requests.map((request) => request.order_id)).toEqual([
      1, 2, 3, 4, 5, 6, 7,
    ]);
    expect(requests.map((request) =>
      request.RequestBodyInfo["Requests/resource_filters"][0],
    )).toEqual([
      { service_name: "HDFS", component_name: "JOURNALNODE", hosts: "jn1" },
      { service_name: "HDFS", component_name: "JOURNALNODE", hosts: "jn2" },
      { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn-standby" },
      { service_name: "HDFS", component_name: "ZKFC", hosts: "nn-standby" },
      { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn-active" },
      { service_name: "HDFS", component_name: "ZKFC", hosts: "nn-active" },
      { service_name: "HDFS", component_name: "DATANODE", hosts: "dn1" },
    ]);
  });

  it("builds ordered master requests and component-specific slave batches", () => {
    const payload = buildServiceRestartSchedule({
      clusterName: "c1",
      serviceName: "HDFS",
      components: [
        restartComponent("MASTER", "JOURNALNODE", "jn1"),
        restartComponent("MASTER", "NAMENODE", "nn1"),
        restartComponent("SLAVE", "DATANODE", "dn1"),
        restartComponent("SLAVE", "DATANODE", "dn2"),
        restartComponent("SLAVE", "DATANODE", "dn3"),
        restartComponent("SLAVE", "NFS_GATEWAY", "nfs1"),
      ],
      batchSize: 2,
      intervalTimeSeconds: 30,
      tolerateSize: 1,
    });
    const schedule = payload[0].RequestSchedule;
    const requests = schedule.batch[0].requests ?? [];

    expect(requests.map((request) => request.order_id)).toEqual([1, 2, 3, 4, 5]);
    expect(requests.map((request) =>
      request.RequestBodyInfo["Requests/resource_filters"][0],
    )).toEqual([
      { service_name: "HDFS", component_name: "JOURNALNODE", hosts: "jn1" },
      { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn1" },
      { service_name: "HDFS", component_name: "DATANODE", hosts: "dn1,dn2" },
      { service_name: "HDFS", component_name: "DATANODE", hosts: "dn3" },
      { service_name: "HDFS", component_name: "NFS_GATEWAY", hosts: "nfs1" },
    ]);
    expect(requests[2].RequestBodyInfo.RequestInfo.context).toBe(
      "_PARSE_.ROLLING-RESTART.DATANODE.1.2",
    );
    expect(schedule.batch[1].batch_settings).toEqual({
      batch_separation_in_seconds: 30,
      task_failure_tolerance: 1,
    });
  });

  it("builds an express request only for the selected component group", () => {
    const payload = buildExpressServiceRestartRequest({
      clusterName: "c1",
      serviceName: "HDFS",
      scope: "SLAVES",
      components: [
        restartComponent("SLAVE", "DATANODE", "dn1"),
        restartComponent("SLAVE", "DATANODE", "dn2", "ON"),
        restartComponent("SLAVE", "NFS_GATEWAY", "nfs1"),
        restartComponent("SLAVE", "NFS_GATEWAY", "nfs2", "IMPLIED_FROM_HOST"),
      ],
    });

    expect(payload.RequestInfo).toEqual({
      command: "RESTART",
      context: "_PARSE_.RESTART.HDFS.SLAVES",
      operation_level: {
        level: "SERVICE",
        cluster_name: "c1",
        service_name: "HDFS",
      },
    });
    expect(payload["Requests/resource_filters"]).toEqual([
      { service_name: "HDFS", component_name: "DATANODE", hosts: "dn1" },
      { service_name: "HDFS", component_name: "NFS_GATEWAY", hosts: "nfs1" },
    ]);

    expect(buildExpressServiceRestartRequest({
      clusterName: "c1",
      serviceName: "HDFS",
      scope: "MASTERS",
      components: [
        restartComponent("MASTER", "NAMENODE", "nn1", "ON"),
        restartComponent(
          "MASTER",
          "NAMENODE",
          "nn2",
          "IMPLIED_FROM_SERVICE",
        ),
      ],
    })["Requests/resource_filters"]).toEqual([]);
  });

  it("detects active rolling requests for components in the service", () => {
    expect(hasActiveServiceComponentRestart([
      {
        Requests: {
          id: 1,
          request_context: "_PARSE_.ROLLING-RESTART.DATANODE.1.2",
          request_status: "IN_PROGRESS",
        },
      },
    ], ["NAMENODE", "DATANODE"], "HDFS")).toBe(true);
    expect(hasActiveServiceComponentRestart([
      {
        Requests: {
          id: 2,
          request_context: "_PARSE_.ROLLING-RESTART.NODEMANAGER.1.1",
          request_status: "IN_PROGRESS",
        },
      },
      {
        Requests: {
          id: 3,
          request_context: "_PARSE_.ROLLING-RESTART.DATANODE.1.1",
          request_status: "COMPLETED",
        },
      },
    ], ["DATANODE"], "HDFS")).toBe(false);
    expect(hasActiveServiceComponentRestart([
      {
        Requests: {
          id: 4,
          request_context: "_PARSE_.ROLLING-RESTART.DATANODE.1.1",
          request_status: "IN_PROGRESS",
          resource_filters: [{ service_name: "OTHER" }],
        },
      },
    ], ["DATANODE"], "HDFS")).toBe(false);
  });

  it("keeps resumable rolling request states active", () => {
    ["HOLDING", "HOLDING_FAILED", "HOLDING_TIMEDOUT", "PAUSED"].forEach(
      (status) => {
        expect(hasActiveServiceComponentRestart([
          {
            Requests: {
              id: 5,
              request_context: "_PARSE_.ROLLING-RESTART.DATANODE.1.1",
              request_status: status,
            },
          },
        ], ["DATANODE"], "HDFS")).toBe(true);
      },
    );
  });
});
