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

import type { ReactNode } from "react";
import ReactGridLayout, { WidthProvider, type Layout } from "react-grid-layout";
import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";
import type { DashboardPanel } from "../types";
import {
  dashboardGridStyle,
  normalizeDashboardLayout,
  sortPositionedPanels,
  type DashboardGridLayout,
} from "./layout/dashboardLayout";

interface DashboardLayoutProps {
  panels: DashboardPanel[];
  renderPanel: (panel: DashboardPanel, layout: DashboardGridLayout, index: number) => ReactNode;
  editable?: boolean;
  renderActions?: (panel: DashboardPanel) => ReactNode;
  onLayoutChange?: (layout: Layout[]) => void;
}

const EditableGrid = WidthProvider(ReactGridLayout);

export default function DashboardLayout({
  panels,
  renderPanel,
  editable = false,
  renderActions,
  onLayoutChange,
}: DashboardLayoutProps) {
  const positionedPanels = sortPositionedPanels(normalizeDashboardLayout(panels));

  if (editable) {
    const layout: Layout[] = positionedPanels.map(({ panel, layout: item }) => ({
      ...item,
      minW: panel.type === "row" ? 24 : 3,
      maxW: panel.type === "row" ? 24 : 24,
      minH: panel.type === "row" ? 1 : 2,
      maxH: panel.type === "row" ? 1 : 16,
      isDraggable: true,
      isResizable: panel.type !== "row",
    }));

    return (
      <EditableGrid
        className="dashboard-layout-editor"
        cols={24}
        rowHeight={56}
        margin={[12, 12]}
        containerPadding={[0, 0]}
        layout={layout}
        compactType="vertical"
        draggableHandle=".dashboard-panel-drag-handle"
        draggableCancel=".dashboard-panel-actions, button, input, textarea, select, a"
        onDragStop={(nextLayout) => onLayoutChange?.(nextLayout)}
        onResizeStop={(nextLayout) => onLayoutChange?.(nextLayout)}
      >
        {positionedPanels.map(({ panel, layout: item }, index) => (
          <div key={item.i} className={panel.type === "row" ? "dashboard-layout-item dashboard-layout-row" : "dashboard-layout-item"}>
            <div className="dashboard-panel-drag-handle" title="Drag panel" />
            {renderActions && <div className="dashboard-panel-actions">{renderActions(panel)}</div>}
            {renderPanel(panel, item, index)}
          </div>
        ))}
      </EditableGrid>
    );
  }

  return (
    <div className="dashboard-layout-grid">
      {positionedPanels.map(({ panel, layout }, index) => (
        <div
          key={layout.i}
          className={panel.type === "row" ? "dashboard-layout-item dashboard-layout-row" : "dashboard-layout-item"}
          style={dashboardGridStyle(layout)}
        >
          {renderPanel(panel, layout, index)}
        </div>
      ))}
    </div>
  );
}
