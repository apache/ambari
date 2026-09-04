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
import {
  applyDashboardLayout,
  createDashboardPanel,
  duplicateDashboardPanel,
} from "./dashboardWorkspace";

describe("dashboard workspace", () => {
  it("creates query panels below the current layout", () => {
    const existing = createDashboardPanel("timeseries", [], 9);
    existing.layout.y = 4;
    existing.layout.h = 3;

    const panel = createDashboardPanel("gauge", [existing], 12);

    expect(panel.layout.y).toBe(7);
    expect(panel.datasourceValue).toBe(12);
    expect(panel.targets).toHaveLength(1);
    expect(panel.targets[0].instant).toBe(true);
  });

  it("creates fixed full-width rows", () => {
    const row = createDashboardPanel("row", []);

    expect(row.layout).toMatchObject({ w: 24, h: 1, isResizable: false });
    expect(row.targets).toEqual([]);
  });

  it("duplicates a panel with independent identity and placement", () => {
    const source = createDashboardPanel("stat", [], 3);
    const copy = duplicateDashboardPanel(source, [source]);

    expect(copy.id).not.toBe(source.id);
    expect(copy.layout.i).toBe(copy.id);
    expect(copy.layout.y).toBe(source.layout.y + source.layout.h);
    expect(copy.name).toBe(`${source.name} copy`);
  });

  it("applies layout geometry while preserving row constraints", () => {
    const row = createDashboardPanel("row", []);
    const panel = createDashboardPanel("timeseries", [row], 4);
    const updated = applyDashboardLayout([row, panel], [
      { i: row.id, x: 4, y: 2, w: 10, h: 5 },
      { i: panel.id, x: 8, y: 3, w: 8, h: 7 },
    ]);

    expect(updated[0].layout).toMatchObject({ x: 0, y: 2, w: 24, h: 1, isResizable: false });
    expect(updated[1].layout).toMatchObject({ x: 8, y: 3, w: 8, h: 7, isResizable: true });
  });
});
