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
import { buildClusterConfigurationPayload } from "../screens/ClusterWizard/clusterConfigPayload";
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

const property = (name: string, type: string, value: string) => ({
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
  isEditable: true,
});

const stepConfigs = (): ConfigPropertiesType => ({
  HDFS: {
    "core-site": {
      errors: 0,
      properties: { "fs.defaultFS": property("fs.defaultFS", "core-site", "hdfs://nn:8020") },
    },
    "Custom core-site": { errors: 0, properties: {}, displayName: "Custom core-site" },
  },
  HIVE: {
    "hive-site": {
      errors: 0,
      properties: { "hive.metastore.uris": property("hive.metastore.uris", "hive-site", "thrift://hms:9083") },
    },
  },
});

// Shape of the Stack Advisor response, as returned for ODP 3.3 HDFS + HIVE.
const advisorResponse = {
  resources: [{
    recommendations: {
      blueprint: {
        configurations: {
          "core-site": {
            properties: {
              "fs.defaultFS": "hdfs://nn:8020",
              "hadoop.proxyuser.hdfs.hosts": "*",
              "hadoop.proxyuser.hdfs.groups": "*",
              "hadoop.proxyuser.hive.hosts": "hs2.example",
              "hadoop.proxyuser.hive.groups": "*",
            },
          },
        },
      },
    },
  }],
};

const processWith = (controllerName: string, installedServices: string[], configs: ConfigPropertiesType) => {
  const setConfigProperties = vi.fn();
  const { result } = renderHook(
    () => useEnhancedConfigs(setConfigProperties, undefined, installedServices, undefined, controllerName),
    { wrapper },
  );
  act(() => {
    result.current.processRecommendations(advisorResponse, configs);
  });
  expect(setConfigProperties).toHaveBeenCalledTimes(1);
  return {
    configs: setConfigProperties.mock.calls[0][0] as ConfigPropertiesType,
    result,
  };
};

const coreSiteOf = (payload: ReturnType<typeof buildClusterConfigurationPayload>) =>
  payload
    .flatMap(({ Clusters }) => Clusters.desired_config)
    .find((desired) => desired.type === "core-site")?.properties;

describe("useEnhancedConfigs addByRecommendations", () => {
  it("deploys recommended properties the stack does not define on cluster creation", () => {
    const input = stepConfigs();
    const { configs, result } = processWith("clusterCreation", [], input);

    const custom = configs.HDFS["Custom core-site"].properties;
    expect(Object.keys(custom).sort()).toEqual([
      "hadoop.proxyuser.hdfs.groups",
      "hadoop.proxyuser.hdfs.hosts",
      "hadoop.proxyuser.hive.groups",
      "hadoop.proxyuser.hive.hosts",
    ]);
    // The existing stack property is updated in place, not duplicated.
    expect(custom["fs.defaultFS"]).toBeUndefined();
    // The caller's configs are not mutated; only the copy is published.
    expect(input.HDFS["Custom core-site"].properties).toEqual({});
    expect(result.current.recommendedChanges).toBeDefined();

    const coreSite = coreSiteOf(buildClusterConfigurationPayload({
      configProperties: configs,
      includeInstalledChanges: false,
      installedServices: [],
    }));
    expect(coreSite).toMatchObject({
      "fs.defaultFS": "hdfs://nn:8020",
      "hadoop.proxyuser.hdfs.hosts": "*",
      "hadoop.proxyuser.hdfs.groups": "*",
      "hadoop.proxyuser.hive.hosts": "hs2.example",
      "hadoop.proxyuser.hive.groups": "*",
    });
  });

  it("sends new proxyuser properties for an installed service when adding a service", () => {
    const { configs } = processWith("addService", ["HDFS"], stepConfigs());

    const coreSite = coreSiteOf(buildClusterConfigurationPayload({
      configProperties: configs,
      includeInstalledChanges: true,
      installedServices: ["HDFS"],
    }));
    // Only the changes reach installed HDFS: the new properties, not the unchanged fs.defaultFS.
    expect(coreSite).toEqual({
      "hadoop.proxyuser.hdfs.hosts": "*",
      "hadoop.proxyuser.hdfs.groups": "*",
      "hadoop.proxyuser.hive.hosts": "hs2.example",
      "hadoop.proxyuser.hive.groups": "*",
    });
  });

  it("keeps a recommendation that was rejected in the dependent-configs dialog out of the payload", () => {
    const { configs } = processWith("addService", ["HDFS"], stepConfigs());
    // handleDependentConfigsModalCallback restores initialValue || originalValue, both null for a new property.
    configs.HDFS["Custom core-site"].properties["hadoop.proxyuser.hive.hosts"].value = null;

    const coreSite = coreSiteOf(buildClusterConfigurationPayload({
      configProperties: configs,
      includeInstalledChanges: true,
      installedServices: ["HDFS"],
    }));
    expect(coreSite).not.toHaveProperty("hadoop.proxyuser.hive.hosts");
    expect(coreSite).toHaveProperty("hadoop.proxyuser.hive.groups", "*");
  });

  it("updates a previously added recommended property instead of adding it twice", () => {
    const first = processWith("clusterCreation", [], stepConfigs()).configs;
    first.HDFS["Custom core-site"].properties["hadoop.proxyuser.hive.hosts"].value = "edited.example";

    const second = processWith("clusterCreation", [], first).configs;

    const matches = Object.values(second.HDFS).flatMap((section) =>
      Object.values(section.properties).filter(
        (p) => p.propertyName === "hadoop.proxyuser.hive.hosts",
      ),
    );
    expect(matches).toHaveLength(1);
    expect(matches[0].recommendedValue).toBe("hs2.example");
  });

  it("does not add recommended properties for a service that is not being configured", () => {
    const input = stepConfigs();
    delete input.HDFS;

    const { configs } = processWith("clusterCreation", [], input);

    expect(configs.HDFS).toBeUndefined();
    expect(Object.keys(configs)).toEqual(["HIVE"]);
  });
});
