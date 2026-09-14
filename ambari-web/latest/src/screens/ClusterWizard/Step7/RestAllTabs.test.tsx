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

import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { useState, type ContextType } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";
import RestAllTabs from "./RestAllTabs";
import type { ConfigPropertiesType } from "../../CommonConfigs/types";
import {
  createManagedDependencyAdvisorRunner,
} from "../managedDependencyAdvisor";

const mocks = vi.hoisted(() => ({
  getRecommendations: vi.fn(),
  setConfigProperties: vi.fn(),
  useHostComponents: vi.fn(() => ({
    hostComponents: [],
    serviceComponents: [],
  })),
}));

vi.mock("../../../api/configsApi", () => ({
  default: {
    getRecommendations: mocks.getRecommendations,
    loadConfigsFromStack: vi.fn(),
  },
}));

vi.mock("../hooks/useHostComponents", () => ({
  default: mocks.useHostComponents,
}));

vi.mock("../../../hooks/useAuthorizationPolicy", () => ({
  default: () => ({
    isAuthorized: () => true,
  }),
}));

vi.mock("../../../components/Tooltip", () => ({
  default: ({ children }: { children: unknown }) => children,
}));

vi.mock("../../CommonConfigs/ChooseConfigGroup", () => ({
  default: () => null,
}));

vi.mock("../../ConfigGroups/ManageConfigGroups", () => ({
  default: () => null,
}));

vi.mock("../../CommonConfigs/AdvancedConfigs", () => ({
  default: () => null,
}));

vi.mock("../../CommonConfigs/TestConnection", () => ({
  default: () => null,
}));

const configProperties = {
  HBASE: {
    "hbase-site": {
      errors: 0,
      properties: {
        "hbase.rootdir": {
          propertyName: "hbase.rootdir",
          propertyDisplayname: "hbase.rootdir",
          propertyValue: "hdfs://host-a:8020/hbase",
          propertyAttributes: { type: "string" },
          previousValue: "hdfs://host-a:8020/hbase",
          value: "hdfs://host-a:8020/hbase",
          final: "false",
          fileName: "hbase-site.xml",
          type: "hbase-site",
          serviceName: "HBASE",
          isEditable: true,
          isVisible: true,
          propertyDependedBy: [{ type: "hbase-site", property: "hbase.zookeeper.quorum" }],
        },
      },
    },
  },
};

const themeData = {
  items: [
    {
      StackServices: { service_name: "HBASE" },
      themes: [
        {
          ThemeInfo: {
            service_name: "HBASE",
            theme_data: {
              Theme: {
                name: "default",
                configuration: {
                  layouts: [
                    {
                      name: "default",
                      tabs: [
                        {
                          name: "general",
                          layout: {
                            sections: [
                              {
                                name: "section",
                                subsections: [{ name: "subsection" }],
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
                        config: "hbase-site/hbase.rootdir",
                        "subsection-name": "subsection",
                      },
                    ],
                  },
                  widgets: [
                    {
                      config: "hbase-site/hbase.rootdir",
                      widget: { type: "text-field" },
                    },
                  ],
                },
              },
            },
          },
        },
      ],
    },
  ],
};

describe("RestAllTabs managed advisor chain", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    mocks.getRecommendations.mockReset();
    mocks.setConfigProperties.mockReset();
    mocks.getRecommendations.mockResolvedValue({
      resources: [{ recommendations: { blueprint: { configurations: {} } } }],
    });
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("checkpoints the edited Config snapshot and drops a response after scope change", async () => {
    let resolveRecommendation: (value: unknown) => void = () => undefined;
    mocks.getRecommendations.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveRecommendation = resolve;
      }),
    );
    const scopeKeyRef = { current: "scope-1" };
    const checkpointCalls = vi.fn();
    const withStateCheckpoint = async <T,>(request: (revision: number) => Promise<T>) => {
      checkpointCalls(request);
      return request(37);
    };
    const managedDependencies = {
      HDFS: {
        mode: "managed" as const,
        provider: { cluster_id: 31, service_name: "HDFS" },
        preview: {
          binding_id: "11111111-1111-4111-8111-111111111111",
          compatible: true,
          consumer_descriptor_fingerprint: "consumer-fingerprint",
          dependency_type: "HDFS" as const,
          expected_provider_fingerprint: "provider-fingerprint",
          provider: { cluster_id: 31, service_name: "HDFS" },
          provider_fingerprint: "provider-fingerprint",
          snapshot_fingerprint: "snapshot-fingerprint",
          preview_schema_version: 2,
        },
      },
    };
    const checkpointConfigProperties = vi.fn(async (snapshot: ConfigPropertiesType) => snapshot);
    const onConfigEdit = vi.fn();
    const recommendationStates: Array<{ pending: boolean; error: string | null }> = [];
    let retryRecommendation: () => void = () => undefined;

    function RestAllTabsHarness() {
      const [scopeKey, setScopeKey] = useState("scope-1");
      scopeKeyRef.current = scopeKey;
      const runner = createManagedDependencyAdvisorRunner({
        draftId: "draft-1",
        managedDependencies: managedDependencies as never,
        scopeKey,
        scopeKeyRef,
        withStateCheckpoint,
      });
      return (
        <RestAllTabs
          themes={themeData}
          configs={{ items: [] }}
          configProperties={configProperties}
          setConfigProperties={mocks.setConfigProperties}
          services={["HBASE"]}
          tabName="default"
          recommendationsDataToSend={{ blueprint: { configurations: {} } }}
          stack="BIGTOP"
          stackVersion="3.3.0"
          hosts={["host-a"]}
          wizardName="clusterCreation"
          conditionServices={["HBASE"]}
          runWithAdvisorRequest={runner}
          advisorScopeKey={scopeKey}
          checkpointConfigProperties={checkpointConfigProperties}
          onConfigEdit={() => {
            onConfigEdit();
            setScopeKey((current) => current === "scope-1" ? "scope-2" : "scope-3");
          }}
          onRecommendationStateChange={({ pending, error, retry }) => {
            recommendationStates.push({ pending, error });
            retryRecommendation = retry;
          }}
        />
      );
    }

    render(
      <AppContext.Provider
        value={
          {
            cluster: { stack: "BIGTOP", versionNum: "3.3.0", cluster_id: 27 },
            clusterName: "cluster-a",
            allHostNames: ["host-a"],
            services: [],
          } as unknown as ContextType<typeof AppContext>
        }
      >
        <RestAllTabsHarness />
      </AppContext.Provider>,
    );

    await act(async () => {
      vi.advanceTimersByTime(300);
      await Promise.resolve();
    });
    mocks.setConfigProperties.mockClear();
    const input = screen.getByRole("textbox", { name: "hbase.rootdir" });
    fireEvent.change(input, { target: { value: "hdfs://host-b:8020/hbase" } });
    expect(onConfigEdit).toHaveBeenCalledTimes(1);
    const setCallsAfterEdit = mocks.setConfigProperties.mock.calls.length;

    await act(async () => {
      vi.advanceTimersByTime(800);
      await Promise.resolve();
    });

    expect(checkpointConfigProperties).toHaveBeenCalledWith(
      expect.objectContaining({ HBASE: expect.anything() }),
    );
    expect(
      checkpointConfigProperties.mock.calls[0][0].HBASE["hbase-site"].properties[
        "hbase.rootdir"
      ].value,
    ).toBe("hdfs://host-b:8020/hbase");
    expect(checkpointCalls).toHaveBeenCalledWith(expect.any(Function));
    expect(mocks.getRecommendations).toHaveBeenCalledWith(
      "BIGTOP",
      "3.3.0",
      expect.objectContaining({
        recommend: "configuration-dependencies",
        recommendations: expect.objectContaining({
          blueprint: expect.objectContaining({
            configurations: expect.objectContaining({
              "hbase-site": expect.objectContaining({
                properties: expect.objectContaining({
                  "hbase.rootdir": "hdfs://host-b:8020/hbase",
                }),
              }),
            }),
          }),
        }),
        managed_dependency_plan: expect.objectContaining({
          consumer: {
            scope: "DRAFT",
            draft_id: "draft-1",
            expected_revision: 37,
          },
        }),
      }),
    );
    expect(recommendationStates.some((state) => state.pending)).toBe(true);

    resolveRecommendation({
      resources: [{ recommendations: { blueprint: { configurations: {} } } }],
    });
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });

    // The edited request is current after the scope checkpoint and its empty
    // recommendation response is applied once. A stale request would add no
    // further call beyond this expected current response.
    expect(mocks.setConfigProperties).toHaveBeenCalledTimes(setCallsAfterEdit + 1);

    mocks.getRecommendations.mockRejectedValueOnce(new Error("advisor unavailable"));
    fireEvent.change(input, { target: { value: "hdfs://host-c:8020/hbase" } });
    await act(async () => {
      vi.advanceTimersByTime(800);
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(recommendationStates).toContainEqual({
      pending: false,
      error: "advisor unavailable",
    });

    await act(async () => {
      retryRecommendation();
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(mocks.getRecommendations).toHaveBeenCalledTimes(3);
  });
});
