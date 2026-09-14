/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file to You under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import { describe, expect, it } from "vitest";
import { projectClusterEvent } from "./clusterEvents";

const cluster = { cluster_id: 7, cluster_name: "alpha" };

describe("projectClusterEvent", () => {
  it("filters mixed collection payloads before reducers can observe them", () => {
    const projected = projectClusterEvent({
      hostComponents: [{ clusterId: 7, hostName: "a" }, { clusterId: 8, hostName: "b" }],
    }, "/events/hostcomponents", cluster);
    expect(projected.hostComponents).toEqual([{ clusterId: 7, hostName: "a" }]);
  });

  it("selects the exact alert summary key and never the first key", () => {
    const projected = projectClusterEvent({ summaries: { 8: { wrong: {} }, 7: { right: {} } } },
      "/events/alerts", cluster);
    expect(projected.summaries).toEqual({ 7: { right: {} } });
  });

  it("rejects foreign and identity-less request updates", () => {
    expect(projectClusterEvent({ clusterName: "beta" }, "/events/requests", cluster)).toBeNull();
    expect(projectClusterEvent({ requestId: 1 }, "/events/requests", cluster)).toBeNull();
  });
});
