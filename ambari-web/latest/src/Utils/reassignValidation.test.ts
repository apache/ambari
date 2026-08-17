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
  getReassignValidationErrors,
  isMissingHostComponentError,
} from "./reassignValidation";

const stack = (reassignAllowed = true) => ({
  items: [
    {
      StackServices: { service_name: "HDFS" },
      components: [
        {
          StackServiceComponents: {
            component_name: "NAMENODE",
            reassign_allowed: reassignAllowed,
          },
        },
      ],
    },
  ],
});

const model = (hosts: string[]) => ({
  masterComponents: [
    {
      componentName: "NAMENODE",
      hostComponents: hosts.map((host_name) => ({
        HostRoles: { host_name },
      })),
    },
  ],
});

describe("reassign entry validation", () => {
  it("allows an installed reassignable component with a free target host", () => {
    expect(
      getReassignValidationErrors({
        componentName: "NAMENODE",
        serviceName: "HDFS",
        allHostNames: ["host1", "host2"],
        serviceComponentInfo: stack(),
        serviceModel: model(["host1"]),
      })
    ).toEqual([]);
  });

  it("rejects one-host clusters and components occupying every host", () => {
    expect(
      getReassignValidationErrors({
        componentName: "NAMENODE",
        serviceName: "HDFS",
        allHostNames: ["host1"],
        serviceComponentInfo: stack(),
        serviceModel: model(["host1"]),
      })
    ).toEqual([
      "You must have at least 2 hosts to run the Move Wizard.",
      "Every cluster host already has NAMENODE.",
    ]);
  });

  it("rejects a direct route for a component the stack cannot reassign", () => {
    expect(
      getReassignValidationErrors({
        componentName: "NAMENODE",
        serviceName: "HDFS",
        allHostNames: ["host1", "host2"],
        serviceComponentInfo: stack(false),
        serviceModel: model(["host1"]),
      })
    ).toContain("NAMENODE cannot be reassigned for HDFS.");
  });
});

describe("reassign rollback recovery", () => {
  it("treats an already deleted target component as idempotent", () => {
    expect(isMissingHostComponentError({ response: { status: 404 } })).toBe(
      true
    );
    expect(
      isMissingHostComponentError({
        response: { data: { message: "NoSuchResourceException" } },
      })
    ).toBe(true);
  });

  it("preserves unrelated rollback failures", () => {
    expect(isMissingHostComponentError({ response: { status: 500 } })).toBe(
      false
    );
  });
});
