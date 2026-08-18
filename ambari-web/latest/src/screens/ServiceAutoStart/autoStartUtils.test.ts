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
  AutoStartComponent,
  changedRecoveryComponents,
  filterAutoStartComponents,
} from "./autoStartUtils";

function component(
  name: string,
  recoveryEnabled: string,
  category = "MASTER",
  totalCount = 1,
): AutoStartComponent {
  return {
    ServiceComponentInfo: {
      category,
      component_name: name,
      recovery_enabled: recoveryEnabled,
      service_name: "HDFS",
      total_count: totalCount,
    },
  };
}

describe("filterAutoStartComponents", () => {
  it("keeps only installed non-client components", () => {
    expect(filterAutoStartComponents([
      component("NAMENODE", "false"),
      component("HDFS_CLIENT", "false", "CLIENT"),
      component("DATANODE", "false", "SLAVE", 0),
    ])).toEqual([component("NAMENODE", "false")]);
  });
});

describe("changedRecoveryComponents", () => {
  it("separates enabled and disabled component changes", () => {
    const cached = [
      component("NAMENODE", "false"),
      component("DATANODE", "true", "SLAVE"),
      component("JOURNALNODE", "true"),
    ];
    const current = [
      component("NAMENODE", "true"),
      component("DATANODE", "false", "SLAVE"),
      component("JOURNALNODE", "true"),
    ];

    expect(changedRecoveryComponents(current, cached)).toEqual({
      enabled: ["NAMENODE"],
      disabled: ["DATANODE"],
    });
  });
});
