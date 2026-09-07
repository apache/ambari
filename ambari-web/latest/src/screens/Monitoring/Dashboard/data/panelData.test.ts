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
import type { DashboardPanel } from "../../types";
import {
  calculatePanelValue,
  latestPanelValue,
  panelNumericBounds,
  panelThresholds,
  panelValueColor,
  panelValueText,
  type DashboardPanelResult,
} from "./panelData";

const panel = (options: DashboardPanel["options"] = {}): DashboardPanel => ({
  id: "cpu",
  name: "CPU",
  type: "stat",
  layout: { h: 4, w: 6, x: 0, y: 0, i: "cpu", isResizable: true },
  targets: [{ refId: "A", expr: "up" }],
  options,
});

const result: DashboardPanelResult = {
  metric: { __name__: "cpu_usage", host: "worker-1" },
  values: [[1, "2"], [2, "4.5"]],
  displayName: "worker-1",
  seriesKey: "A:worker-1",
  targetRefId: "A",
  targetName: "worker-1",
};

describe("dashboard panel data", () => {
  it("returns the most recent numeric sample", () => {
    expect(latestPanelValue(result)).toBe(4.5);
    expect(latestPanelValue({ ...result, values: [[1, "NaN"]] })).toBeNull();
  });

  it("calculates summary values for aggregate visualizations", () => {
    expect(calculatePanelValue(result, "firstNotNull")).toBe(2);
    expect(calculatePanelValue(result, "min")).toBe(2);
    expect(calculatePanelValue(result, "max")).toBe(4.5);
    expect(calculatePanelValue(result, "avg")).toBe(3.25);
    expect(calculatePanelValue(result, "sum")).toBe(6.5);
    expect(calculatePanelValue(result, "count")).toBe(2);
  });

  it("sorts threshold steps and resolves their colors", () => {
    const config = panel({ thresholds: { steps: [
      { value: 80, color: "red" },
      { value: null, color: "green" },
      { value: 50, color: "orange" },
    ] } });
    expect(panelThresholds(config).map((step) => step.value)).toEqual([null, 50, 80]);
    expect(panelValueColor(config, 65)).toBe("#bd6418");
    expect(panelValueColor(config, 90)).toBe("#b33a3a");
  });

  it("applies inclusive range value mappings before thresholds", () => {
    const config = panel({
      thresholds: { steps: [{ value: null, color: "red" }] },
      valueMappings: [
        { type: "range", match: { from: 0, to: 0 }, result: { color: "#369903", text: "Healthy" } },
        { type: "range", match: { from: 1, to: null }, result: { color: "#f0310f" } },
      ],
    });
    expect(panelValueColor(config, 0)).toBe("#369903");
    expect(panelValueText(config, 0)).toBe("Healthy");
    expect(panelValueColor(config, 1)).toBe("#f0310f");
  });

  it("exposes numeric bounds from standard options", () => {
    expect(panelNumericBounds(panel({ standardOptions: { min: 0, max: 100 } }))).toEqual({ min: 0, max: 100 });
    expect(panelNumericBounds(panel())).toEqual({});
  });
});
