/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { cleanup, render, screen } from "@testing-library/react";
import { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import { HostsListStateProvider } from "../../store/HostsListStateContext";
import { ServiceContext } from "../../store/ServiceContext";
import ServiceComponents from "./ServiceComponents";

const victoriaMetricsComponents = [
  {
    ServiceComponentInfo: {
      category: "MASTER",
      component_name: "VICTORIAMETRICS_SERVER",
      display_name: "VictoriaMetrics Server",
      service_name: "VICTORIAMETRICS",
      started_count: 1,
      total_count: 1,
    },
    host_components: [
      {
        HostRoles: {
          host_name: "worker1.bigtop.apache.org",
          maintenance_state: "OFF",
          state: "STARTED",
        },
      },
    ],
  },
  {
    ServiceComponentInfo: {
      category: "SLAVE",
      component_name: "VMAGENT",
      display_name: "VictoriaMetrics Agent",
      service_name: "VICTORIAMETRICS",
      started_count: 1,
      total_count: 1,
    },
    host_components: [
      {
        HostRoles: {
          host_name: "worker1.bigtop.apache.org",
          maintenance_state: "OFF",
          state: "STARTED",
        },
      },
    ],
  },
  {
    ServiceComponentInfo: {
      category: "MASTER",
      component_name: "VMAUTH",
      display_name: "VictoriaMetrics Auth",
      service_name: "VICTORIAMETRICS",
      started_count: 1,
      total_count: 1,
    },
    host_components: [
      {
        HostRoles: {
          host_name: "worker1.bigtop.apache.org",
          maintenance_state: "OFF",
          state: "STARTED",
        },
      },
    ],
  },
];

function componentView(data: any[], serviceName = "VICTORIAMETRICS") {
  return (
    <MemoryRouter>
      <HostsListStateProvider>
        <ServiceContext.Provider
          value={
            {
              masterSlaveClientsData: data,
            } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <ServiceComponents serviceName={serviceName} alerts={[]} />
        </ServiceContext.Provider>
      </HostsListStateProvider>
    </MemoryRouter>
  );
}

function renderComponents(data: any[], serviceName = "VICTORIAMETRICS") {
  return render(componentView(data, serviceName));
}

function renderHDFSSummary(hdfsModel: any) {
  return render(
    <MemoryRouter>
      <HostsListStateProvider>
        <ServiceContext.Provider
          value={
            {
              allServiceModels: { hdfs: hdfsModel },
            } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <ServiceComponents serviceName="HDFS" alerts={[]} />
        </ServiceContext.Provider>
      </HostsListStateProvider>
    </MemoryRouter>
  );
}

function nameNodeHostComponent(hostName: string, haStatus: string) {
  return {
    HostRoles: { host_name: hostName, component_name: "NAMENODE" },
    state: "STARTED",
    haStatus,
  };
}

function zkfcHostComponent(hostName: string) {
  return {
    HostRoles: { host_name: hostName, component_name: "ZKFC" },
    state: "STARTED",
  };
}

describe("generic service summary", () => {
  afterEach(cleanup);

  it("renders VictoriaMetrics master and slave components", () => {
    renderComponents(victoriaMetricsComponents);

    expect(screen.getByText("VictoriaMetrics Server")).toBeTruthy();
    expect(screen.getByText("VictoriaMetrics Auth")).toBeTruthy();
    expect(screen.getByText("VictoriaMetrics Agent")).toBeTruthy();
    expect(screen.getAllByText("Started")).toHaveLength(2);
    expect(screen.getByText("1/1 Live")).toBeTruthy();
  });

  it("shows an explicit empty state instead of a blank summary", () => {
    renderComponents([]);

    expect(screen.getByText("No components to display")).toBeTruthy();
  });

  it("keeps imported masters visible while host details load and recovers when they arrive", () => {
    const component = {
      ServiceComponentInfo: { category: "MASTER", component_name: "ELASTICSEARCH_NODE",
        display_name: "Elasticsearch Node", service_name: "ELASTICSEARCH", total_count: 1 },
      host_components: [],
    };
    const view = renderComponents([component], "ELASTICSEARCH");
    expect(screen.getByText("Elasticsearch Node")).toBeTruthy();
    expect(screen.getByText("Host details unavailable")).toBeTruthy();
    expect(screen.queryByText("Started")).toBeNull();
    view.rerender(componentView([{ ...component, host_components: [{ HostRoles: {
      host_name: "search.test", state: "STARTED", maintenance_state: "OFF",
    } }] }], "ELASTICSEARCH"));
    expect(screen.getByText("Elasticsearch Node")).toBeTruthy();
    expect(screen.getByText("Started")).toBeTruthy();
    expect(screen.queryByText("Host details unavailable")).toBeNull();
  });

  it("renders an imported Kyuubi server using its declared component identity", () => {
    renderComponents([{
      ServiceComponentInfo: {
        category: "MASTER", component_name: "KYUUBI_SERVER", display_name: "Kyuubi Server",
        service_name: "KYUUBI", started_count: 1, total_count: 1,
      },
      host_components: [{ HostRoles: {
        host_name: "worker4.bigtop.apache.org", maintenance_state: "OFF", state: "STARTED",
      } }],
    }], "KYUUBI");

    expect(screen.getByText("Kyuubi Server")).toBeTruthy();
    expect(screen.getByText("Started")).toBeTruthy();
  });
});

describe("HDFS NameNode/ZKFC summary", () => {
  afterEach(cleanup);

  it("labels each NameNode by HA state and pairs it with its host's ZKFC", () => {
    renderHDFSSummary({
      isNameNodeHaEnabled: true,
      federationNamespaces: [{ name: "default", title: "default", hosts: [], components: [], clusterId: "default" }],
      masterComponents: [
        {
          componentName: "NAMENODE",
          hostComponents: [
            nameNodeHostComponent("nn1.example.com", "active"),
            nameNodeHostComponent("nn2.example.com", "observer"),
          ],
        },
      ],
      slaveComponents: [
        {
          componentName: "ZKFC",
          displayName: "ZKFailoverController",
          hostComponents: [
            zkfcHostComponent("nn1.example.com"),
            zkfcHostComponent("nn2.example.com"),
          ],
        },
      ],
    });

    expect(screen.getByText("Active NameNode")).toBeTruthy();
    expect(screen.getByText("Observer NameNode")).toBeTruthy();
    expect(screen.getAllByText("ZKFailoverController")).toHaveLength(2);
  });

  it("stays out of the federated layout when only one namespace has an installed NameNode", () => {
    renderHDFSSummary({
      isNameNodeHaEnabled: true,
      // A declared-but-unpopulated second namespace must not flip isFederated.
      federationNamespaces: [
        { name: "ns1", title: "ns1", hosts: ["nn1.example.com"], components: ["NAMENODE", "ZKFC"], clusterId: "default" },
      ],
      masterComponents: [
        {
          componentName: "NAMENODE",
          hostComponents: [nameNodeHostComponent("nn1.example.com", "active")],
        },
      ],
      slaveComponents: [
        { componentName: "ZKFC", displayName: "ZKFailoverController", hostComponents: [] },
      ],
    });

    expect(screen.getByText("Active NameNode")).toBeTruthy();
    expect(screen.queryByText(/Namespace:/)).toBeNull();
  });
});
