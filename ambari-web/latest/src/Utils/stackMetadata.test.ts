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
  filterInstallableStackServices,
  isInstallableStackService,
} from "./stackMetadata";

describe("stack service installability", () => {
  it("keeps ordinary services when the server omits is_installable", () => {
    expect(isInstallableStackService({
      StackServices: { service_name: "HDFS" },
    })).toBe(true);
  });

  it("rejects explicit server exclusions and Classic's Kerberos exclusion", () => {
    const resources = [
      { StackServices: { service_name: "HDFS" } },
      { StackServices: { service_name: "CUSTOM", is_installable: false } },
      { StackServices: { service_name: "KERBEROS", is_installable: true } },
    ];

    expect(filterInstallableStackServices(resources)).toEqual([resources[0]]);
  });

  it("rejects malformed stack-service resources", () => {
    expect(isInstallableStackService({})).toBe(false);
    expect(isInstallableStackService(null)).toBe(false);
  });
});
