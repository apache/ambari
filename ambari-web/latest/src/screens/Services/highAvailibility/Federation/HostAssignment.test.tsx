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

import { render, waitFor } from "@testing-library/react";
import { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { validateComponentAssignments } from "./workflowUtils";

const mocks = vi.hoisted(() => ({
  getHosts: vi.fn(),
  recommendations: vi.fn(),
}));

vi.mock("../../../../api/hostsApi", () => ({
  HostsApi: { getHostComponentsDetails: mocks.getHosts },
}));
vi.mock("../../../../api/assignMastersApi", () => ({
  default: { postRecommendations: mocks.recommendations },
}));

import HostAssignment from "./HostAssignment";

describe("restored host assignment validation", () => {
  it("marks an assigned host missing from the fresh cluster host set", async () => {
    mocks.getHosts.mockResolvedValue({
      items: [
        {
          Hosts: { host_name: "host1", maintenance_state: "OFF" },
          host_components: [],
        },
      ],
    });
    const onChange = vi.fn();
    render(
      <AppContext.Provider
        value={
          {
            clusterName: "c1",
            cluster: { stack: "BIGTOP", versionNum: "3.2.0" },
          } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
        }
      >
        <HostAssignment
          componentName="NAMENODE"
          componentLabel="NameNode"
          installedHosts={[]}
          initialAssignments={[
            { component: "NAMENODE", hostName: "removed-host", isInstalled: false },
            { component: "NAMENODE", hostName: "host1", isInstalled: false },
          ]}
          additionalCount={2}
          services={["HDFS"]}
          onChange={onChange}
        />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(onChange).toHaveBeenCalled());
    const assignments = onChange.mock.calls.at(-1)?.[0];
    expect(assignments).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          hostName: "removed-host",
          isAvailable: false,
        }),
      ]),
    );
    expect(
      validateComponentAssignments(assignments, "NAMENODE", 2),
    ).toContain("removed-host is no longer available");
    expect(mocks.recommendations).not.toHaveBeenCalled();
  });
});
