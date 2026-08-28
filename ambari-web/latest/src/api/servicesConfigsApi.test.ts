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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
  supressErrorAmbariApi: { request: mocks.request },
}));

import ConfigsApi from "./configsApi";
import { ActionsApi } from "./actionsApi";
import { QuicklinksApi } from "./quicklinksApi";
import { ServiceApi } from "./serviceApi";
import { ServiceConfigApi } from "./serviceConfigApi";

describe("services and configs API contracts", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.request.mockResolvedValue({ data: {} });
    vi.spyOn(Date, "now").mockReturnValue(1234);
  });

  it("creates a component with the classic services collection POST", async () => {
    await ServiceApi.createComponent("c1", "HIVE", "HIVE_SERVER");
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/services?ServiceInfo/service_name=HIVE",
      method: "POST",
      data: {
        components: [
          { ServiceComponentInfo: { component_name: "HIVE_SERVER" } },
        ],
      },
    });
  });

  it("targets a Flume handler by agent and host", async () => {
    await ServiceApi.updateFlumeAgent(
      "c1",
      "host1",
      "agent1",
      "STARTED",
      "Start Flume Agent agent1"
    );
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/hosts/host1/host_components/FLUME_HANDLER",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Start Flume Agent agent1",
          flume_handler: "agent1",
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: "c1",
            service_name: "FLUME",
            host_name: "host1",
          },
        },
        Body: { HostRoles: { state: "STARTED" } },
      },
    });
  });

  it("updates service maintenance with the classic service PUT", async () => {
    await ActionsApi.turnOnOffMaintenance("c1", "HDFS", {
      requestInfo: "Turn On Maintenance Mode for HDFS",
      passive_state: "ON",
    });
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/services/HDFS",
      method: "PUT",
      data: {
        RequestInfo: { context: "Turn On Maintenance Mode for HDFS" },
        Body: { ServiceInfo: { maintenance_state: "ON" } },
      },
    });
  });

  it("uses exact non-metrics component and desired-config URLs", async () => {
    await ServiceApi.getAllServiceComponents("c1", "a,b");
    await ConfigsApi.getDesiredConfigsInfo("c1");
    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/clusters/c1/components?fields=a,b&_=1234",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/clusters/c1?fields=Clusters/desired_configs&_=1234",
      method: "GET",
    });
  });

  it("preserves Ambari's encoded grouped version predicate", async () => {
    await ConfigsApi.getMultipleVersionConfigValues("c1", "HDFS", "1", "2");
    expect(mocks.request).toHaveBeenCalledWith({
      url: "clusters/c1/configurations/service_config_versions?(service_name=HDFS%26service_config_version.in(1,2))",
      method: "GET",
    });
  });

  it("loads descriptors and public-host mappings without malformed suffixes", async () => {
    await QuicklinksApi.getQuicklinks("3.2.0", "BIGTOP", "HDFS");
    await QuicklinksApi.getPublicHostNames("c1", ["host/one", "host2"]);
    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/stacks/BIGTOP/versions/3.2.0/services/HDFS/quicklinks?QuickLinkInfo/default=true&fields=*",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/clusters/c1/hosts?Hosts/host_name.in(host%2Fone,host2)&fields=Hosts/public_host_name&minimal_response=true",
      method: "GET",
    });
  });

  it("reloads current versions for the installed service list", async () => {
    await ServiceConfigApi.getCurrentServiceConfigs("c1", ["HDFS", "YARN"]);
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/configurations/service_config_versions?service_name.in(HDFS,YARN)&is_current=true&fields=*&_=1234",
      method: "GET",
    });
  });
});
