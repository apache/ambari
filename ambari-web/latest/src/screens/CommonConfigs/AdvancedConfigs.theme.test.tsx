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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  hasAuthorization: vi.fn(() => true),
  setConfigProperties: vi.fn(),
  setTabErrors: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    havePermissions: mocks.hasAuthorization,
    hasAuthorization: mocks.hasAuthorization,
  }),
}));
vi.mock("../../hooks/useEnhancedConfigs", () => ({
  default: () => ({ onValueUpdate: vi.fn(), processingConfig: false }),
}));
vi.mock("../../components/Tooltip", () => ({
  default: ({ children }: { children: unknown }) => children,
}));
vi.mock("../../components/Modal", () => ({ default: () => null }));
vi.mock("../../components/OverlayBackdrop", () => ({ default: () => null }));

import AdvancedConfigs from "./AdvancedConfigs";

const configs = () => ({
  SVC: {
    "Advanced site": {
      errors: 0,
      properties: {
        primary: {
          propertyName: "primary",
          propertyDisplayname: "Primary property",
          propertyValue: "default value",
          propertyAttributes: { type: "string", overridable: true },
          previousValue: "default value",
          value: "default value",
          final: "false",
          savedFinal: "false",
          fileName: "site.xml",
          type: "site",
          isEditable: true,
          isOverridable: true,
          isVisible: true,
          supportsFinal: true,
          overrideValues: [
            {
              groupName: "Blue",
              value: "group value",
              previousValue: "group value",
              final: "false",
              savedFinal: "false",
            },
          ],
        },
      },
    },
  },
});

const zookeeperConfigs = () => {
  const section = (displayName: string) => ({
    displayName,
    errors: 0,
    properties: {
      value: {
        propertyName: "value",
        propertyDisplayname: "Value",
        propertyValue: "value",
        propertyAttributes: { type: "string" },
        previousValue: "value",
        value: "value",
        fileName: "site.xml",
        type: "site",
        isEditable: true,
        isVisible: true,
      },
    },
  });
  return {
    SVC: {
      ZOOKEEPER_SERVER: section("ZooKeeper Server"),
      "zookeeper-log4j": section("Advanced zookeeper-log4j"),
      "zoo.cfg": section("Advanced zoo.cfg"),
      "zookeeper-env": section("Advanced zookeeper-env"),
      "Custom zookeeper-log4j": section("Custom zookeeper-log4j"),
    },
  };
};

const renderAdvanced = (props: Record<string, unknown> = {}) =>
  render(
    <AdvancedConfigs
      configPropertiesData={configs()}
      setConfigProperties={mocks.setConfigProperties}
      chosenService="SVC"
      displayUndoRedo
      setTabErrors={mocks.setTabErrors}
      configGroup="Blue"
      {...props}
    />,
  );

describe("Advanced Theme fallback permissions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
  });

  afterEach(cleanup);

  it("keeps an installed-service override read-only without modify permission", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderAdvanced();
    fireEvent.click(screen.getByRole("button", { name: /Advanced site/ }));

    expect(
      (await screen.findByDisplayValue("group value") as HTMLInputElement)
        .disabled,
    ).toBe(true);
    expect(document.querySelector('[data-icon="lock"]')).toBeNull();
    expect(document.querySelector('[data-icon="circle-minus"]')).toBeNull();
  });

  it("edits Installer overrides and preserves their independent final state", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderAdvanced({ installer: true });
    fireEvent.click(screen.getByRole("button", { name: /Advanced site/ }));

    expect(
      (await screen.findByDisplayValue("group value") as HTMLInputElement)
        .disabled,
    ).toBe(false);
    const lock = document.querySelector('[data-icon="lock"]');
    expect(lock).toBeTruthy();
    fireEvent.click(lock!);

    await waitFor(() => {
      const latest = mocks.setConfigProperties.mock.lastCall?.[0];
      expect(
        latest.SVC["Advanced site"].properties.primary.overrideValues[0].final,
      ).toBe("true");
    });
  });

  it("opens ordinary categories and orders advanced sections deterministically", () => {
    render(
      <AdvancedConfigs
        configPropertiesData={zookeeperConfigs()}
        setConfigProperties={mocks.setConfigProperties}
        chosenService="SVC"
        displayUndoRedo
        setTabErrors={mocks.setTabErrors}
      />,
    );

    const categoryButtons = screen.getAllByRole("button").filter((button) =>
      /ZooKeeper Server|Advanced |Custom /.test(button.textContent || ""),
    );
    expect(categoryButtons.map((button) => button.textContent?.trim())).toEqual([
      "ZooKeeper Server",
      "Advanced zoo.cfg",
      "Advanced zookeeper-env",
      "Advanced zookeeper-log4j",
      "Custom zookeeper-log4j",
    ]);
    expect(categoryButtons[0].getAttribute("aria-expanded")).toBe("true");
    expect(categoryButtons[1].getAttribute("aria-expanded")).toBe("false");
  });

  it("commits edited values to the canonical parent state immediately", async () => {
    renderAdvanced({ configGroup: "Default" });
    fireEvent.click(screen.getByRole("button", { name: /Advanced site/ }));
    const input = await screen.findByDisplayValue("default value");

    fireEvent.change(input, { target: { value: "changed value" } });

    const latest = mocks.setConfigProperties.mock.lastCall?.[0];
    expect(latest.SVC["Advanced site"].properties.primary.value)
      .toBe("changed value");
  });
});
