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

import type { CSSProperties } from "react";
import type { DashboardPanel } from "../types";

export const DASHBOARD_GRID_COLUMNS = 24;
export const DASHBOARD_GRID_ROW_HEIGHT = 56;
export const DASHBOARD_GRID_GAP = 12;

export interface DashboardGridLayout {
  i: string;
  x: number;
  y: number;
  w: number;
  h: number;
  isResizable: boolean;
}

export interface PositionedDashboardPanel {
  panel: DashboardPanel;
  layout: DashboardGridLayout;
}

export function normalizeDashboardLayout(panels: DashboardPanel[]): PositionedDashboardPanel[] {
  return panels.map((panel) => ({
    panel,
    layout: {
      ...panel.layout,
      w: panel.type === "row" ? DASHBOARD_GRID_COLUMNS : panel.layout.w,
      isResizable: panel.type === "row" ? false : panel.layout.isResizable,
    },
  }));
}

export function sortPositionedPanels(items: PositionedDashboardPanel[]) {
  return [...items].sort((left, right) => (
    left.layout.y - right.layout.y || left.layout.x - right.layout.x
  ));
}

export function dashboardGridStyle(layout: DashboardGridLayout): CSSProperties {
  return {
    gridColumn: `${layout.x + 1} / span ${layout.w}`,
    gridRow: `${layout.y + 1} / span ${layout.h}`,
    minWidth: 0,
    minHeight: 0,
  };
}

export function dashboardPanelHeight(layout: DashboardGridLayout) {
  return layout.h * DASHBOARD_GRID_ROW_HEIGHT + Math.max(0, layout.h - 1) * DASHBOARD_GRID_GAP;
}
