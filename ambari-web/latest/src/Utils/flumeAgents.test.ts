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
  canStartFlumeAgent,
  canStopFlumeAgent,
  extractFlumeAgents,
} from "./flumeAgents";

describe("Flume agents", () => {
  it("extracts and sorts handlers by host and name", () => {
    expect(
      extractFlumeAgents([
        {
          ServiceComponentInfo: {
            service_name: "FLUME",
            component_name: "FLUME_HANDLER",
          },
          host_components: [
            {
              HostRoles: { host_name: "host-b" },
              processes: [
                { HostComponentProcess: { name: "agent-2", status: "RUNNING" } },
              ],
            },
            {
              HostRoles: { host_name: "host-a" },
              processes: [
                {
                  HostComponentProcess: {
                    host_name: "public-agent-host",
                    name: "agent-1",
                    status: "NOT_RUNNING",
                  },
                },
                { HostComponentProcess: { name: "agent-unknown", status: "BROKEN" } },
              ],
            },
          ],
        },
      ])
    ).toEqual([
      {
        id: "agent-unknown-host-a",
        hostName: "host-a",
        name: "agent-unknown",
        status: "UNKNOWN",
      },
      {
        id: "agent-2-host-b",
        hostName: "host-b",
        name: "agent-2",
        status: "RUNNING",
      },
      {
        id: "agent-1-public-agent-host",
        hostName: "public-agent-host",
        name: "agent-1",
        status: "NOT_RUNNING",
      },
    ]);
  });

  it("allows only classic state-valid actions", () => {
    expect(canStartFlumeAgent("NOT_RUNNING")).toBe(true);
    expect(canStartFlumeAgent("RUNNING")).toBe(false);
    expect(canStopFlumeAgent("RUNNING")).toBe(true);
    expect(canStopFlumeAgent("UNKNOWN")).toBe(false);
  });
});
