/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { describe, expect, it } from "vitest";
import { genericServiceModels } from "./genericServiceModels";
import { serviceNameModelMapping } from "../constants";
const services = ["CUSTOM_QUEUE", "CUSTOM_TOOL"].map(service_name => ({ ServiceInfo: {
  service_name, state: "INSTALLED", maintenance_state: "OFF",
} }));
const definitions = { items: services.map(({ ServiceInfo }, index) => ({
  StackServices: { service_name: ServiceInfo.service_name, display_name: ServiceInfo.service_name },
  components: [{ StackServiceComponents: { component_name: "COMPONENT_" + index, component_category: index ? "CLIENT" : "MASTER" } }],
})) };
describe("generic service models", () => {
  it("keeps arbitrary services distinct and derives client-only behavior from metadata", () => {
    const models = genericServiceModels(services, definitions, { items: [] }, "test");
    const queue = models[serviceNameModelMapping.CUSTOM_QUEUE], tool = models[serviceNameModelMapping.CUSTOM_TOOL];
    expect(queue.serviceName).toBe("CUSTOM_QUEUE"); expect(tool.serviceName).toBe("CUSTOM_TOOL");
    expect(queue.isClientOnlyService).toBe(false); expect(tool.isClientOnlyService).toBe(true);
    expect(tool.workStatus).toBe("INSTALLED");
    expect(tool.masterComponents).toEqual([]);
  });
  it("does not invent service state from diagnostic text or component counts", () => {
    const invalid = [{ ServiceInfo: { service_name: "CUSTOM_QUEUE", maintenance_state: "OFF", diagnostic: "STARTED" } }];
    expect(genericServiceModels(invalid, definitions, { items: [] }, "test")).toEqual({});
  });
  it("preserves specialized built-in models", () => {
    expect(genericServiceModels([{ ServiceInfo: { service_name: "HDFS", state: "STARTED", maintenance_state: "OFF" } }],
      definitions, { items: [] }, "test")).toEqual({});
    expect(serviceNameModelMapping.HDFS).toBe("hdfs");
  });
  it("applies newer service observations without deriving state from running components", () => {
    const models = genericServiceModels(services, definitions, { items: [] }, "test",
      new Map([["CUSTOM_QUEUE", { state: "STARTED", maintenance_state: "ON" }]]));
    expect(models[serviceNameModelMapping.CUSTOM_QUEUE].workStatus).toBe("STARTED");
    expect(models[serviceNameModelMapping.CUSTOM_QUEUE].isInPassiveForService).toBe(true);
  });
});
