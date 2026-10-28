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
import { chooseServiceVersion, groupServiceCatalog } from "./serviceCatalog";
import type { MpackServiceEntry } from "../../api/mpackApi";

const entry: MpackServiceEntry = { id: "old", release_id: "example/1.2", archive_digest: "a",
  service_name: "EXAMPLE", display_name: "Example", description: "", service_version: "3.1",
  stack_name: "BASE", stack_version: "1.0", required_services: [], enabled: true, client_only: false };

describe("management pack service grouping", () => {
  it("preserves stack boundaries and prefers an enabled definition over a newer unused release", () => {
    const groups = groupServiceCatalog([entry,
      { ...entry, id: "new", release_id: "example/1.10", enabled: false },
      { ...entry, id: "other-stack", stack_name: "OTHER" }]);
    expect(groups).toHaveLength(2);
    expect(groups[0].entries.map(item => item.id)).toEqual(["old", "new"]);
  });
  it("sorts numeric release versions without lexical or integer precision loss", () => {
    const groups = groupServiceCatalog([
      { ...entry, enabled: false },
      { ...entry, id: "ten", release_id: "example/1.10", enabled: false },
      { ...entry, id: "huge", release_id: "example/999999999999999999999.0", enabled: false }]);
    expect(groups[0].entries.map(item => item.id)).toEqual(["huge", "ten", "old"]);
  });
  it("replaces only the chosen service version and rejects foreign choices", () => {
    const group = groupServiceCatalog([entry, { ...entry, id: "new", release_id: "example/1.10" }])[0];
    expect(chooseServiceVersion(["old", "other-service"], group, "new")).toEqual(["other-service", "new"]);
    expect(chooseServiceVersion(["old", "new", "other-service"], group, null)).toEqual(["other-service"]);
    expect(() => chooseServiceVersion([], group, "foreign")).toThrow("Unknown service version");
  });
});
