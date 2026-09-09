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

import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import "../../i18n";
import ManagedDependencySettings from "./ManagedDependencySettings";

describe("managed dependency settings", () => {
  it("keeps primary identity visible and technical client values collapsed", () => {
    render(<ManagedDependencySettings
      onReturn={vi.fn()}
      selections={{
        HDFS: {
          mode: "managed",
          provider: { cluster_id: 9, cluster_name: "provider-a", service_name: "HDFS" },
          preview: {
            binding_id: "11111111-1111-4111-8111-111111111111",
            client_config: { "core-site": { "fs.defaultFS": "hdfs://provider-a" } },
            compatible: true,
            consumer: {
              lifecycle: "INIT",
              planned_hbase_user: "hbase_a",
              scope: "SERVICE",
              service_name: "HBASE",
            },
            dependency_type: "HDFS",
            errors: [],
            namespace: {
              root_uri: "hdfs://provider-a/apps/hbase/a",
              wal_uri: "hdfs://provider-a/apps/hbase/a-wal",
            },
            preview_schema_version: 1,
            provider: { cluster_id: 9, cluster_name: "provider-a", service_name: "HDFS" },
          },
        },
      }}
      view="review"
    />);

    expect(screen.getByText("provider-a / HDFS")).toBeTruthy();
    expect(screen.getByText("hdfs://provider-a/apps/hbase/a")).toBeTruthy();
    expect(screen.getByText("hbase_a")).toBeTruthy();
    const advanced = screen.getByText("Advanced client settings").closest("details");
    expect(advanced).toBeTruthy();
    expect(advanced?.hasAttribute("open")).toBe(false);
    expect(advanced?.textContent).toContain("core-site/fs.defaultFS");
  });
});
