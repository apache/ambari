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

import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import HDFSFederationTopology from "./HDFSFederationTopology";

const hostComponent = (
  componentName: string,
  hostName: string,
  state: string,
  haStatus?: string,
) => ({
  HostRoles: { component_name: componentName, host_name: hostName },
  state,
  passiveState: "OFF",
  haStatus,
});

describe("HDFSFederationTopology", () => {
  it("groups NameNode and ZKFC control-plane status by namespace", () => {
    render(
      <MemoryRouter>
        <HDFSFederationTopology
          namespaces={[
            { name: "ns1", hosts: ["nn1", "nn2"] },
            { name: "ns2", title: "Analytics", hosts: ["nn3", "nn4"] },
          ]}
          masterComponents={[{
            componentName: "NAMENODE",
            hostComponents: [
              hostComponent("NAMENODE", "nn1", "STARTED", "active"),
              hostComponent("NAMENODE", "nn2", "STARTED", "standby"),
              hostComponent("NAMENODE", "nn3", "INSTALLED"),
            ],
          }]}
          slaveComponents={[{
            componentName: "ZKFC",
            hostComponents: [hostComponent("ZKFC", "nn1", "STARTED")],
          }]}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText("Namespace: ns1")).toBeTruthy();
    expect(screen.getByText("Namespace: Analytics")).toBeTruthy();
    expect(screen.getByText("active NameNode")).toBeTruthy();
    expect(screen.getByText("standby NameNode")).toBeTruthy();
    expect(screen.getAllByText("ZKFailoverController")).toHaveLength(4);
    expect(screen.getByRole("link", { name: "nn1" }).getAttribute("href"))
      .toBe("/main/hosts/nn1/summary");
  });
});
