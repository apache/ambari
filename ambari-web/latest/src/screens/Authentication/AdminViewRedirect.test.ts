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
  adminViewUrl,
  applicationRootFromDocumentPath,
  classicExperienceUrl,
  latestServerVersion,
} from "../../Utils/adminViewRedirect";

describe("Admin View version selection", () => {
  it("uses numeric version order and removes build suffixes", () => {
    expect(latestServerVersion({
      components: [
        { RootServiceComponents: { component_version: "2.9.0.0-1" } },
        { RootServiceComponents: { component_version: "2.10.0.0-custom" } },
        { RootServiceComponents: {} },
      ],
    })).toBe("2.10.0.0");
  });

  it("returns null when Ambari Server reports no usable version", () => {
    expect(latestServerVersion({ components: [] })).toBeNull();
  });

  it("preserves root and proxy-prefixed application paths", () => {
    expect(adminViewUrl("2.10.0.0", null, "/"))
      .toBe("/views/ADMIN_VIEW/2.10.0.0/INSTANCE/#/");
    expect(adminViewUrl("2.10.0.0", "stackVersions", "/ambari/index.html"))
      .toBe("/ambari/views/ADMIN_VIEW/2.10.0.0/INSTANCE/#/stackVersions");
    expect(adminViewUrl("2.10.0.0", null, "/latest/"))
      .toBe("/views/ADMIN_VIEW/2.10.0.0/INSTANCE/#/");
    expect(adminViewUrl("2.10.0.0", null, "/gateway/ambari/latest/index.html"))
      .toBe("/gateway/ambari/views/ADMIN_VIEW/2.10.0.0/INSTANCE/#/");
  });

  it("derives Classic and Admin View URLs from the deployment root", () => {
    expect(applicationRootFromDocumentPath("/latest/")).toBe("/");
    expect(applicationRootFromDocumentPath("/gateway/ambari/latest/"))
      .toBe("/gateway/ambari/");
    expect(classicExperienceUrl("/gateway/ambari/latest/index.html"))
      .toBe("/gateway/ambari/#/");
  });
});
