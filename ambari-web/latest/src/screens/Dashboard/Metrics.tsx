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

import React, { useContext, useEffect, useState } from "react";
import { dashboardWidgets } from "./dashboard_widgets";
import Spinner from "../../components/Spinner";
import { Card, Row, Col, Dropdown } from "react-bootstrap";
import WidgetContainer from "./WidgetContainer";
import { AppContext } from "../../store/context";
import { cloneDeep, map } from "lodash";
import ClusterApi from "../../api/clusterApi";
import SelectTimeRangeModal from "../../components/SelectTimeRangeModal";
import { formatDate } from "../../Utils/Utility";
import BrowseWidgetsModal from "../Metrics/BrowseWidgetsModal";

// Define widget interface
interface Widget {
  id: number;
  viewName: any;
  sourceName: string;
  title: string;
  threshold: number[];
  groupName?: string;
  isVisible?: boolean;
  isHiddenByDefault?: boolean;
}

// Define persisted state interface
interface PersistedWidgetsState {
  visible: number[];
  hidden: number[];
  threshold: Record<string, number[]>;
  groups: Record<string, any>;
}

export default function DashboardMetrics() {
  const [dashboardWidgetsState, setDashboardWidgetsState] =
    useState<Widget[]>(dashboardWidgets);
  const [persistedWidgetsState, setPersistedWidgetsState] =
    useState<PersistedWidgetsState>({
      visible: [],
      hidden: [],
      threshold: {},
      groups: {},
    });
  const [showBrowseWidgetsModal, setShowBrowseWidgetsModal] = useState(false);
  const { services } = useContext(AppContext);
  const serviceNames = map(services, "ServiceInfo.service_name").join(",");

  // Time range state management
  const timeRangeOptions = [
    "Last 1 hour",
    "Last 2 hours",
    "Last 4 hours",
    "Last 12 hours",
    "Last 24 hours",
    "Last 1 week",
    "Last 1 month",
    "Last 1 year",
  ];

  const [selectedTimeRangeOption, setSelectedTimeRangeOption] =
    useState<string>(timeRangeOptions[0]);
  const [customTimeRange, setCustomTimeRange] = useState<{
    startTime: number;
    endTime: number;
  } | null>(null);
  const [showSelectTimeModal, setShowSelectTimeModal] =
    useState<boolean>(false);

  useEffect(() => {
    getDashboardWidgets();
  }, []);

  useEffect(() => {
    const updatedWidgets = cloneDeep(dashboardWidgetsState);
    updatedWidgets.forEach((widget) => {
      const widgetId = widget.id;
      // Check if widget ID is in visible array
      const visibleWidgets = persistedWidgetsState.visible || [];
      const hiddenWidgets = persistedWidgetsState.hidden || [];

      if (visibleWidgets.indexOf(widgetId) !== -1) {
        widget.isVisible = true;
      }
      if (hiddenWidgets.indexOf(widgetId) !== -1) {
        widget.isVisible = false;
      }

      // Get threshold from persisted state or use default
      const thresholdKey = `${widgetId}`;
      if (
        persistedWidgetsState.threshold &&
        thresholdKey in persistedWidgetsState.threshold
      ) {
        widget.threshold = persistedWidgetsState.threshold[thresholdKey];
      }

      // Update cluster metrics widgets with timeRange prop
      if (widget.sourceName === "HOST_METRICS" && React.isValidElement(widget.viewName)) {
        widget.viewName = React.cloneElement(widget.viewName as React.ReactElement<any>, {
          timeRange: selectedTimeRangeOption,
          customTimeRange: customTimeRange
        });
      }
    });
    setDashboardWidgetsState(updatedWidgets);
  }, [persistedWidgetsState, selectedTimeRangeOption, customTimeRange]);

  const getDashboardWidgets = async () => {
    const response = await ClusterApi.getPersistData(
      "user-pref-admin-dashboard"
    );
    if (response) {
      setPersistedWidgetsState(response);
    }
  };

  const onDeleteWidget = async (widget: Widget) => {
    // Create a new state object with properly typed arrays
    const updatedPersistedState: PersistedWidgetsState = {
      visible: persistedWidgetsState.visible.filter((id) => id !== widget.id),
      hidden: [...persistedWidgetsState.hidden],
      threshold: { ...persistedWidgetsState.threshold },
      groups: { ...persistedWidgetsState.groups },
    };

    // Add widget ID to hidden array if it's not already there
    if (!updatedPersistedState.hidden.includes(widget.id)) {
      updatedPersistedState.hidden.push(widget.id);
    }

    // Update local state
    setPersistedWidgetsState(updatedPersistedState);

    // Persist to backend
    await ClusterApi.postPersistData({
      "user-pref-admin-dashboard": JSON.stringify(updatedPersistedState),
    });
  };

  const onViewDetails = (widget: Widget) => {
    console.log("View details for widget:", widget);
    // Implement view details functionality
  };

  const onShareWidget = (widget: Widget) => {
    console.log("Share widget:", widget);
    // Implement share functionality
  };

  if (!dashboardWidgetsState) {
    return <Spinner />;
  }

  return (
    <>
      {showSelectTimeModal ? (
        <SelectTimeRangeModal
          isOpen={showSelectTimeModal}
          onClose={() => setShowSelectTimeModal(false)}
          successCallback={(data) => {
            setSelectedTimeRangeOption(
              "CUSTOM: " +
                formatDate(new Date(data.startTime * 1000))
                  .split("T")
                  .join(" ")
            );
            setCustomTimeRange({
              startTime: data.startTime,
              endTime: data.endTime
            });
            setShowSelectTimeModal(false);
          }}
        />
      ) : null}
      <Card className="p-4">
        <div className="d-flex justify-content-end mb-4">
          <Dropdown className="mb-3 justify-content-end">
            <Dropdown.Toggle variant="transparent" className="btn-default">
              <span className="me-2">{selectedTimeRangeOption}</span>
            </Dropdown.Toggle>
            <Dropdown.Menu className="rounded-0">
              {timeRangeOptions.map((option) => (
                <Dropdown.Item
                  key={option}
                  onClick={() => {
                    setSelectedTimeRangeOption(option);
                    setCustomTimeRange(null); // Clear custom time range when selecting predefined option
                  }}
                >
                  {option}
                </Dropdown.Item>
              ))}
              <Dropdown.Item onClick={() => setShowSelectTimeModal(true)}>
                Custom
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
        <Row>
          {dashboardWidgetsState.map(
            (widget) =>
              widget?.isVisible &&
              (serviceNames?.includes(widget?.sourceName) ||
                widget?.sourceName === "HOST_METRICS") && (
                <Col md={3} key={widget.id} className="mb-3">
                  <WidgetContainer
                    onDelete={() => onDeleteWidget(widget)}
                    onViewDetails={() => onViewDetails(widget)}
                    onShare={() => onShareWidget(widget)}
                    widgetHeader={widget.title}
                  >
                    <div>{widget?.viewName}</div>
                  </WidgetContainer>
                </Col>
              )
          )}
        </Row>
      </Card>

      {showBrowseWidgetsModal && (
        <BrowseWidgetsModal
          isOpen={showBrowseWidgetsModal}
          onClose={() => setShowBrowseWidgetsModal(false)}
          serviceName={serviceNames}
          onWidgetAdded={() => getDashboardWidgets()}
        />
      )}
    </>
  );
}
