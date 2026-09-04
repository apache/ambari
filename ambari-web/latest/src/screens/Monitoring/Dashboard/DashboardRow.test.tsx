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

import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import i18n from "../../../i18n";
import type { DashboardPanel } from "../types";
import DashboardRow from "./DashboardRow";

const row = (values: Partial<DashboardPanel> = {}): DashboardPanel => ({
  id: "system",
  name: "System resource usage",
  titleKey: "monitoring.dashboard.sections.test.systemResources",
  type: "row",
  targets: [],
  layout: { h: 1, w: 24, x: 0, y: 0, i: "system", isResizable: false },
  ...values,
});

describe("DashboardRow", () => {
  afterEach(async () => {
    cleanup();
    i18n.removeResourceBundle("test-locale", "translation");
    await i18n.changeLanguage("en");
  });

  it("uses the active application language and falls back to the stored name", async () => {
    i18n.addResourceBundle("test-locale", "translation", {
      "monitoring.dashboard.sections.test.systemResources": "Translated system resources",
    });
    await i18n.changeLanguage("test-locale");

    const { rerender } = render(<DashboardRow panel={row()} />);
    expect(screen.getByText("Translated system resources")).toBeTruthy();

    rerender(<DashboardRow panel={row({ titleKey: "monitoring.dashboard.sections.missing" })} />);
    expect(screen.getByText("System resource usage")).toBeTruthy();
  });
});
