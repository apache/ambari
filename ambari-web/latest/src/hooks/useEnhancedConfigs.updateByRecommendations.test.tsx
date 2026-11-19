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

import { act, renderHook } from "@testing-library/react";
import type { ContextType, ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";
import useEnhancedConfigs from "./useEnhancedConfigs";

vi.mock("../screens/ClusterWizard/hooks/useHostComponents", () => ({
  default: () => ({ hostComponents: [] }),
}));

const contextValue = {
  cluster: { stack: "", versionNum: "", cluster_id: 1 },
  services: [],
  allHostNames: [],
  serviceComponentInfo: undefined,
} as unknown as ContextType<typeof AppContext>;

const wrapper = ({ children }: { children: ReactNode }) => (
  <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>
);

const property = (serviceName: string, name: string, type: string, value: string) => ({
  propertyName: name,
  propertyDisplayname: name,
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  initialValue: value,
  final: "false",
  fileName: `${type}.xml`,
  type,
  serviceName,
  isEditable: true,
});

const DB_HOST = "db.example";
const RECOMMENDED_URL = `jdbc:mysql://${DB_HOST}:3306/ranger`;

const stepConfigs = (): ConfigPropertiesType => ({
  RANGER: {
    "ranger-admin-site": {
      errors: 0,
      properties: {
        "ranger.jpa.jdbc.url": property(
          "RANGER", "ranger.jpa.jdbc.url", "ranger-admin-site", "jdbc:mysql://localhost",
        ),
      },
    },
  },
  HDFS: {
    "hdfs-site": {
      errors: 0,
      properties: {
        "dfs.replication": property("HDFS", "dfs.replication", "hdfs-site", "3"),
      },
    },
  },
});

// Stack Advisor response after admin-properties/db_host changed (ODP 3.3 Ranger advisor).
const advisorResponse = () => ({
  resources: [{
    recommendations: {
      blueprint: {
        configurations: {
          "ranger-admin-site": { properties: { "ranger.jpa.jdbc.url": RECOMMENDED_URL } },
          "hdfs-site": { properties: { "dfs.replication": "2" } },
        },
      },
    },
  }],
});

const processWith = (
  controllerName: string,
  installedServices: string[],
  configs: ConfigPropertiesType,
  clusterInstalledServices?: string[],
) => {
  const setConfigProperties = vi.fn();
  const { result } = renderHook(
    () => useEnhancedConfigs(
      setConfigProperties, undefined, installedServices, undefined, controllerName,
      undefined, undefined, undefined, undefined, undefined, undefined, undefined,
      clusterInstalledServices,
    ),
    { wrapper },
  );
  act(() => {
    result.current.processRecommendations(advisorResponse(), configs);
  });
  expect(setConfigProperties).toHaveBeenCalledTimes(1);
  return setConfigProperties.mock.calls[0][0] as ConfigPropertiesType;
};

const jdbcUrl = (configs: ConfigPropertiesType) =>
  configs.RANGER["ranger-admin-site"].properties["ranger.jpa.jdbc.url"];

describe("useEnhancedConfigs updateConfigsByRecommendations", () => {
  it("applies recommendations to properties of a service being added", () => {
    const configs = processWith("addService", ["HDFS"], stepConfigs());

    expect(jdbcUrl(configs).value).toBe(RECOMMENDED_URL);
    expect(jdbcUrl(configs).recommendedValue).toBe(RECOMMENDED_URL);
  });

  it("keeps a value the user typed for a service being added", () => {
    const input = stepConfigs();
    const url = jdbcUrl(input);
    url.value = "jdbc:mysql://manual.example:3307/ranger";
    url.didUserOverrideValue = true;

    const configs = processWith("addService", ["HDFS"], input);

    expect(jdbcUrl(configs).value).toBe("jdbc:mysql://manual.example:3307/ranger");
    expect(jdbcUrl(configs).recommendedValue).toBe(RECOMMENDED_URL);
  });

  it("leaves installed-service values as reviewable recommendations when adding a service", () => {
    const configs = processWith("addService", ["HDFS"], stepConfigs());

    const replication = configs.HDFS["hdfs-site"].properties["dfs.replication"];
    expect(replication.value).toBe("3");
  });

  it("applies recommendations in the Step 7 child, which scopes every service as installed", () => {
    // RestAllTabs passes all in-scope services as installedServices (they also feed the
    // advisor request) and the cluster's real installed list separately.
    const configs = processWith("addService", ["HDFS", "RANGER"], stepConfigs(), ["HDFS"]);

    expect(jdbcUrl(configs).value).toBe(RECOMMENDED_URL);
    expect(configs.HDFS["hdfs-site"].properties["dfs.replication"].value).toBe("3");
  });

  it("treats the new service as installed without the real installed list", () => {
    const configs = processWith("addService", ["HDFS", "RANGER"], stepConfigs());

    expect(jdbcUrl(configs).value).toBe("jdbc:mysql://localhost");
  });

  it("keeps applying recommendations during cluster creation", () => {
    const configs = processWith("clusterCreation", [], stepConfigs());

    expect(jdbcUrl(configs).value).toBe(RECOMMENDED_URL);
    expect(configs.HDFS["hdfs-site"].properties["dfs.replication"].value).toBe("2");
  });
});
