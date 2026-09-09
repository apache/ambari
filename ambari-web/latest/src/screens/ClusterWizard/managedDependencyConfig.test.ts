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
  buildManagedDependencyClientConfig,
  mergeManagedDependencyConfigProperties,
} from "./managedDependencyConfig";

const selections = {
  HDFS: {
    mode: "managed" as const,
    preview: {
      binding_id: "11111111-1111-4111-8111-111111111111",
      client_config: {
        "core-site": { "fs.defaultFS": "hdfs://provider:8020" },
        "hbase-site": { "hbase.zookeeper.quorum": "zk-provider" },
      },
      compatible: true,
      consumer: {
        lifecycle: "INIT",
        planned_hbase_user: "hbase_a",
        scope: "DRAFT",
        service_name: "HBASE" as const,
      },
      dependency_type: "HDFS" as const,
      errors: [],
      namespace: {
        root_uri: "hdfs://provider:8020/apps/hbase/a",
        wal_uri: "hdfs://provider:8020/apps/hbase/a-wal",
      },
      preview_schema_version: 1,
      provider: {
        cluster_id: 9,
        cluster_name: "provider-a",
        service_name: "HDFS",
      },
    },
  },
};

describe("managed dependency configuration", () => {
  it("projects only approved client, namespace, and service identity values", () => {
    expect(buildManagedDependencyClientConfig(selections)).toEqual({
      "hbase-env": { hbase_user: "hbase_a" },
      "hbase-site": {
        "hbase.rootdir": "hdfs://provider:8020/apps/hbase/a",
        "hbase.wal.dir": "hdfs://provider:8020/apps/hbase/a-wal",
        "hbase.zookeeper.quorum": "zk-provider",
      },
    });
  });

  it("renders matching wizard properties read-only with their reviewed values", () => {
    const original: any = {
      HBASE: {
        General: {
          properties: {
            root: {
              fileName: "hbase-site.xml",
              isEditable: true,
              propertyName: "hbase.rootdir",
              value: "hdfs://local/apps/hbase",
            },
          },
        },
      },
    };
    const merged = mergeManagedDependencyConfigProperties(
      original,
      buildManagedDependencyClientConfig(selections),
    ) as any;

    expect(merged.HBASE.General.properties.root).toMatchObject({
      isEditable: false,
      isManagedDependency: true,
      recommendedValue: "hdfs://provider:8020/apps/hbase/a",
      value: "hdfs://provider:8020/apps/hbase/a",
    });
    expect(original.HBASE.General.properties.root.value).toBe(
      "hdfs://local/apps/hbase",
    );

    const restored = mergeManagedDependencyConfigProperties(merged, {}) as any;
    expect(restored).toEqual(original);
  });

  it("retains the original ordinary value while changing managed providers", () => {
    const original: any = {
      HBASE: {
        General: {
          properties: {
            root: {
              fileName: "hbase-site.xml",
              isEditable: true,
              propertyName: "hbase.rootdir",
              recommendedValue: "hdfs://local/recommended",
              value: "hdfs://local/edited",
            },
          },
        },
      },
    };
    const first = mergeManagedDependencyConfigProperties(original, {
      "hbase-site": { "hbase.rootdir": "hdfs://provider-a/hbase" },
    });
    const second = mergeManagedDependencyConfigProperties(first, {
      "hbase-site": { "hbase.rootdir": "hdfs://provider-b/hbase" },
    });

    expect((second as any).HBASE.General.properties.root.value)
      .toBe("hdfs://provider-b/hbase");
    expect(mergeManagedDependencyConfigProperties(second, {})).toEqual(original);
  });
});
