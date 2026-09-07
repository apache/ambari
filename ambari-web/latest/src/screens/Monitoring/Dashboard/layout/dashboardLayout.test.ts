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
  DASHBOARD_GRID_COLUMNS,
  DASHBOARD_GRID_GAP,
  DASHBOARD_GRID_ROW_HEIGHT,
  dashboardGridStyle,
  dashboardPanelHeight,
  collapseDashboardSections,
  normalizeDashboardLayout,
  sortPositionedPanels,
} from "./dashboardLayout";

const panel = (id: string, layout: DashboardPanel["layout"], type: DashboardPanel["type"] = "timeseries"): DashboardPanel => ({
  id,
  name: id,
  type,
  layout,
  targets: type === "row" ? [] : [{ refId: "A", expr: "up" }],
});

describe("dashboard layout", () => {
  it("uses the persisted geometry without flow-based re-layout", () => {
    const positioned = normalizeDashboardLayout([
      panel("right", { h: 3, w: 8, x: 8, y: 4, i: "right", isResizable: true }),
      panel("left", { h: 2, w: 8, x: 0, y: 1, i: "left", isResizable: true }),
    ]);

    expect(positioned[0].layout).toEqual({ h: 3, w: 8, x: 8, y: 4, i: "right", isResizable: true });
    expect(sortPositionedPanels(positioned).map(({ panel: item }) => item.id)).toEqual(["left", "right"]);
  });

  it("makes row panels span the dashboard and stay fixed", () => {
    const [row] = normalizeDashboardLayout([
      panel("section", { h: 1, w: 24, x: 0, y: 0, i: "section", isResizable: false }, "row"),
    ]);

    expect(row.layout.w).toBe(DASHBOARD_GRID_COLUMNS);
    expect(row.layout.isResizable).toBe(false);
    expect(dashboardPanelHeight(row.layout)).toBe(DASHBOARD_GRID_ROW_HEIGHT);
  });

  it("maps geometry to stable CSS grid tracks", () => {
    const [positioned] = normalizeDashboardLayout([
      panel("cpu", { h: 4, w: 6, x: 3, y: 2, i: "cpu", isResizable: true }),
    ]);
    expect(dashboardGridStyle(positioned.layout)).toMatchObject({
      gridColumn: "4 / span 6",
      gridRow: "3 / span 4",
    });
    expect(dashboardPanelHeight(positioned.layout)).toBe(4 * DASHBOARD_GRID_ROW_HEIGHT + 3 * DASHBOARD_GRID_GAP);
  });

  it("hides collapsed row contents and closes the following layout gap", () => {
    const positioned = normalizeDashboardLayout([
      panel("first-row", { h: 1, w: 24, x: 0, y: 0, i: "first-row", isResizable: false }, "row"),
      panel("hidden", { h: 4, w: 12, x: 0, y: 1, i: "hidden", isResizable: true }),
      panel("second-row", { h: 1, w: 24, x: 0, y: 5, i: "second-row", isResizable: false }, "row"),
      panel("visible", { h: 3, w: 12, x: 0, y: 6, i: "visible", isResizable: true }),
    ]);

    const collapsed = collapseDashboardSections(positioned, new Set(["first-row"]));

    expect(collapsed.map(({ panel: item }) => item.id)).toEqual(["first-row", "second-row", "visible"]);
    expect(collapsed.map(({ layout: item }) => item.y)).toEqual([0, 1, 2]);
    expect(collapsed[0].panel.collapsed).toBe(true);
  });
});
