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
import { ConfigPropertiesType } from "../../CommonConfigs/types";
import { getCategoryClientErrors } from "./categoryValidation";

const property = (
  propertyName: string,
  errorMessage = "",
  overrides: Record<string, unknown> = {},
) => ({
  propertyName,
  propertyDisplayname: propertyName,
  propertyValue: "value",
  propertyAttributes: { type: "string" },
  previousValue: "value",
  value: "value",
  final: "false",
  type: "service-site",
  serviceName: "SVC",
  isEditable: true,
  isVisible: true,
  errorMessage,
  ...overrides,
});

const configs = (): ConfigPropertiesType => ({
  SVC: {
    "service-site": {
      errors: 0,
      properties: {
        visibleDatabase: property("visibleDatabase", "Required"),
        hiddenDatabase: property("hiddenDatabase", "Required"),
        unrelated: property("unrelated", "Required"),
      },
    },
  },
  MISC: {
    "Users and Groups": {
      errors: 0,
      properties: {
        account: property("account", "Invalid account", {
          serviceName: "SVC",
          type: "service-env",
        }),
      },
    },
  },
});

const databaseTheme = {
  items: [
    {
      StackServices: { service_name: "SVC" },
      themes: [
        {
          ThemeInfo: {
            service_name: "SVC",
            file_name: "database.json",
            theme_data: {
              Theme: {
                name: "database",
                configuration: {
                  layouts: [
                    {
                      name: "database",
                      tabs: [
                        {
                          name: "database",
                          layout: {
                            sections: [
                              {
                                name: "database",
                                subsections: [
                                  { name: "visible" },
                                  { name: "conditional" },
                                ],
                              },
                            ],
                          },
                        },
                      ],
                    },
                  ],
                  placement: {
                    configs: [
                      {
                        config: "service-site/visibleDatabase",
                        "subsection-name": "visible",
                      },
                      {
                        config: "service-site/hiddenDatabase",
                        "subsection-name": "conditional",
                        "depends-on": [
                          {
                            resource: "service",
                            if: "HDFS",
                            then: { property_value_attributes: { visible: true } },
                            else: { property_value_attributes: { visible: false } },
                          },
                        ],
                      },
                    ],
                  },
                  widgets: [],
                },
              },
            },
          },
        },
      ],
    },
  ],
};

describe("Step 7 category validation", () => {
  it("checks only active properties placed in the database Theme", () => {
    const withoutDependency = getCategoryClientErrors({
      configProperties: configs(),
      selectedTab: "databases",
      serviceNames: ["SVC"],
      themes: databaseTheme,
    });
    expect(withoutDependency.map((item) => item.propertyName)).toEqual([
      "visibleDatabase",
    ]);

    const withInstalledDependency = getCategoryClientErrors({
      configProperties: configs(),
      selectedTab: "databases",
      serviceNames: ["HDFS", "SVC"],
      themes: databaseTheme,
    });
    expect(withInstalledDependency.map((item) => item.propertyName)).toEqual([
      "visibleDatabase",
      "hiddenDatabase",
    ]);
  });

  it("isolates account validation and keeps non-gated categories enabled", () => {
    expect(
      getCategoryClientErrors({
        configProperties: configs(),
        selectedTab: "accounts",
        serviceNames: ["SVC"],
        themes: databaseTheme,
      }).map((item) => item.propertyName),
    ).toEqual(["account"]);
    expect(
      getCategoryClientErrors({
        configProperties: configs(),
        selectedTab: "credentials",
        serviceNames: ["SVC"],
        themes: databaseTheme,
      }),
    ).toEqual([]);
  });
});
