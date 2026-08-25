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
import { configValidator } from "../../../../Utils/validators";
import {
  buildRangerAdminConfigBodies,
  buildRangerAdminConfigQuery,
  buildRangerAdminPreview,
  getRangerAdminHosts,
  validateRangerAdminAssignments,
} from "./rangerAdminHaUtils";

describe("Ranger Admin HA utilities", () => {
  it("uses the full Classic URL contract without trimming or normalization", () => {
    expect(configValidator.isValidURL("http://lb.example.com:6080/ranger?a=1#ui")).toBe(true);
    expect(configValidator.isValidURL("ftp://user:pass@lb.example.com/ranger")).toBe(true);
    expect(configValidator.isValidURL("http://localhost:6080")).toBe(false);
    expect(configValidator.isValidURL("http://ranger-lb:6080")).toBe(false);
    expect(configValidator.isValidURL(" https://lb.example.com")).toBe(false);
  });

  it("requires one current and at least one additional unique assignment", () => {
    const valid = [
      { component: "RANGER_ADMIN", hostName: "ra1", isInstalled: true },
      { component: "RANGER_ADMIN", hostName: "ra2", isInstalled: false },
      { component: "RANGER_ADMIN", selectedHost: "ra3", isInstalled: false },
    ];
    expect(validateRangerAdminAssignments(valid)).toEqual([]);
    expect(getRangerAdminHosts(valid)).toEqual({
      currentHosts: ["ra1"],
      additionalHosts: ["ra2", "ra3"],
    });
    expect(validateRangerAdminAssignments(valid.slice(0, 1))).toContain(
      "At least one additional Ranger Admin is required.",
    );
    expect(
      validateRangerAdminAssignments([
        valid[0],
        { component: "RANGER_ADMIN", hostName: "ra1", isInstalled: false },
      ]),
    ).toContain("Ranger Admin instances must be assigned to different hosts.");
  });

  it("previews only installed services and retains absent stack properties", () => {
    const preview = buildRangerAdminPreview(
      [
        {
          StackServices: { service_name: "RANGER", display_name: "Ranger" },
          configurations: [
            {
              StackConfigurations: {
                type: "admin-properties.xml",
                property_name: "policymgr_external_url",
                property_display_name: "External URL",
              },
            },
          ],
        },
      ],
      ["RANGER", "HDFS"],
      "https://lb.example.com",
    );

    expect(preview.categories.map((category) => category.name)).toEqual([
      "RANGER",
      "HDFS",
    ]);
    expect(preview.properties).toHaveLength(2);
    expect(preview.properties[0].displayName).toBe("External URL");
    expect(preview.properties[1]).toMatchObject({
      name: "ranger.plugin.hdfs.policy.rest.url",
      fileName: "ranger-hdfs-security.xml",
      value: "https://lb.example.com",
    });
  });

  it("loads only current candidate sites in stable wizard order", () => {
    expect(
      buildRangerAdminConfigQuery({
        "ranger-yarn-security": { tag: "version2" },
        "admin-properties": { tag: "version1" },
        "unrelated-site": { tag: "version3" },
      }),
    ).toBe(
      "(type=admin-properties&tag=version1)|(type=ranger-yarn-security&tag=version2)",
    );
  });

  it("preserves complete configs and creates an absent target property", () => {
    const bodies = buildRangerAdminConfigBodies(
      [
        {
          type: "admin-properties",
          properties: { existing: "value" },
          properties_attributes: { final: { existing: "true" } },
        },
      ],
      "https://lb.example.com",
      "HA note",
    );

    expect(bodies).toEqual([
      {
        Clusters: {
          desired_config: [
            {
              type: "admin-properties",
              properties: {
                existing: "value",
                policymgr_external_url: "https://lb.example.com",
              },
              properties_attributes: { final: { existing: "true" } },
              service_config_version_note: "HA note",
            },
          ],
        },
      },
    ]);
  });
});
