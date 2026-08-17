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
  buildBulkHostQueryParams,
  isBulkComponentDeleteVisible,
} from "./bulkOperations";

describe("Hosts bulk-operation targeting", () => {
  it("targets the explicit host set for Selected Hosts", () => {
    expect(buildBulkHostQueryParams("selected", ["host1", "host2"], []))
      .toEqual([{
        key: "Hosts/host_name",
        value: ["host1", "host2"],
        type: "MULTIPLE",
      }]);
  });

  it("uses the active predicates for Filtered Hosts", () => {
    const filters = [{
      field: { label: "Host Name", value: "hostName" },
      value: { label: "worker", value: "worker" },
    }] as any;

    const result = buildBulkHostQueryParams("filtered", [], filters);
    expect(result).not.toEqual([]);
    expect(JSON.stringify(result)).toContain("worker");
  });

  it("sends no predicate for All Hosts", () => {
    expect(buildBulkHostQueryParams("all", ["host1"], []))
      .toEqual([]);
  });

  it("exposes component deletion for Selected and Filtered, but not All", () => {
    expect(isBulkComponentDeleteVisible("selected")).toBe(true);
    expect(isBulkComponentDeleteVisible("filtered")).toBe(true);
    expect(isBulkComponentDeleteVisible("all")).toBe(false);
  });
});
