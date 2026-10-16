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
import { declaredServiceCommands } from "./declaredServiceCommands";
const definition = { component_name: "EXAMPLE_SERVER", component_category: "MASTER", cardinality: "1", custom_commands: ["BACKUP"] };
const inventory = { items: [{ ServiceComponentInfo: { service_name: "EXAMPLE" }, host_components: [{ HostRoles: {
  component_name: "EXAMPLE_SERVER", host_name: "host1", state: "STARTED", maintenance_state: "OFF",
  custom_commands: ["BACKUP", "FOREIGN"],
} }] }] };
describe("declared service operations", () => {
  it("uses exact component and host declarations without software-name branches", () => {
    const result = declaredServiceCommands("cluster1", "EXAMPLE", [definition], inventory);
    expect(result.map(item => item.command.command)).toEqual(["BACKUP"]);
    expect(result[0].component.clusterName).toBe("cluster1");
    expect(result[0].component.hostName).toBe("host1");
  });
  it("does not advertise commands with missing or foreign metadata", () => {
    expect(declaredServiceCommands("cluster1", "OTHER", [definition], inventory)).toEqual([]);
    expect(declaredServiceCommands("cluster1", "EXAMPLE", [], inventory)).toEqual([]);
  });
});
