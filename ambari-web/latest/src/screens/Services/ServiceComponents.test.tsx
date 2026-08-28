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

function renderComponents(data: any[]) {
  return render(
    <MemoryRouter>
      <ServiceContext.Provider
        value={
          {
            masterSlaveClientsData: data,
          } as unknown as ComponentProps<
            typeof ServiceContext.Provider
          >["value"]
        }
      >
        <ServiceComponents serviceName="VICTORIAMETRICS" alerts={[]} />
      </ServiceContext.Provider>
    </MemoryRouter>
  );
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
});
