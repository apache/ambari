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
import { ComponentProps, ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";

const mocks = vi.hoisted(() => ({
  saveConfigs: vi.fn(),
  updateConfigGroupProperties: vi.fn(),
  showModal: vi.fn(),
}));
vi.mock("../api/configsApi", () => ({
  default: {
    saveConfigs: mocks.saveConfigs,
    updateConfigGroupProperties: mocks.updateConfigGroupProperties,
  },
}));
vi.mock("../store/ModalManager", () => ({
  default: { show: mocks.showModal, hide: vi.fn() },
}));

import { useConfigSaver } from "./useConfigSaver";
import { AppContext } from "../store/context";

const property = (name: string, value: string, previousValue: string) => ({
  propertyName: name,
  propertyDisplayname: name,
  propertyValue: value,
  propertyAttributes: { type: "text" },
  fileName: "hdfs-site.xml",
  value,
  previousValue,
  final: "false",
  savedFinal: "false",
  isEditable: true,
  foundInPropertyValues: true,
  isRequiredByAgent: true,
  propertyType: ["TEXT"],
});

const configProperties: ConfigPropertiesType = {
  HDFS: {
    "Advanced hdfs-site": {
      errors: 0,
      properties: {
        changed: property("changed", "new", "old"),
        unchanged: property("unchanged", "keep", "keep"),
        deleted: {
          ...property("deleted", "old", "old"),
          value: null,
          isVisible: false,
        },
      },
    },
  },
};

const wrapper = ({ children }: { children: ReactNode }) => (
  <AppContext.Provider
    value={
      {
        clusterName: "c1",
        services: [{ ServiceInfo: { service_name: "HDFS" } }],
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
    }
  >
    {children}
  </AppContext.Provider>
);

describe("configuration save lifecycle", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("keeps saving state until the full replacement payload settles", async () => {
    let resolveSave!: (value: unknown) => void;
    mocks.saveConfigs.mockReturnValue(
      new Promise((resolve) => {
        resolveSave = resolve;
      })
    );
    const setSubmitDisabled = vi.fn();
    const { result } = renderHook(
      () =>
        useConfigSaver(
          false,
          setSubmitDisabled,
          "Default",
          configProperties,
          "HDFS",
          [],
          "Module 04 save"
        ),
      { wrapper }
    );

    let saveResult!: Promise<boolean>;
    act(() => {
      saveResult = result.current.saveStepConfigs();
    });
    expect(result.current.saveInProgress).toBe(true);
    expect(setSubmitDisabled).toHaveBeenCalledWith(true);

    await act(async () => {
      resolveSave({});
      expect(await saveResult).toBe(true);
    });

    expect(result.current.saveInProgress).toBe(false);
    expect(setSubmitDisabled).toHaveBeenLastCalledWith(false);
    expect(mocks.saveConfigs).toHaveBeenCalledWith("c1", [
      {
        Clusters: {
          desired_config: [
            expect.objectContaining({
              type: "hdfs-site",
              properties: { changed: "new", unchanged: "keep" },
              properties_attributes: expect.objectContaining({
                text: { changed: "true", unchanged: "true" },
              }),
            }),
          ],
        },
      },
    ]);
  });

  it("returns false and restores the submit state after a rejected save", async () => {
    mocks.saveConfigs.mockRejectedValue(new Error("save failed"));
    const setSubmitDisabled = vi.fn();
    const { result } = renderHook(
      () =>
        useConfigSaver(
          false,
          setSubmitDisabled,
          "Default",
          configProperties,
          "HDFS",
          [],
          "Module 04 save"
        ),
      { wrapper }
    );

    await act(async () => {
      expect(await result.current.saveStepConfigs()).toBe(false);
    });
    expect(result.current.saveInProgress).toBe(false);
    expect(setSubmitDisabled).toHaveBeenLastCalledWith(false);
    expect(mocks.showModal).toHaveBeenCalled();
  });
});
