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

import { useContext, useEffect, useRef, useState } from "react";
import metricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import WidgetContainer from "../Dashboard/WidgetContainer";
import { Card, Col, Dropdown, Row } from "react-bootstrap";
import BrowseWidgetsModal from "./BrowseWidgetsModal";
import { WidgetInfo } from "./type";
import { cloneDeep, get, isEmpty, set } from "lodash";
import ChartContainer from "../Dashboard/ChartContainer";
import GraphWidgetView from "./GraphWidgetView";
import GaugeWidgetView from "./GaugeWidgetView";
import SelectTimeRangeModal from "../../components/SelectTimeRangeModal";
import { createFallbackWidgetsForService, formatDate, getTimeInNumber } from "../../Utils/Utility";
import { durationMap } from "../../components/constants";
import Spinner from "../../components/Spinner";
import modalManager from "../../store/ModalManager";
import ConfirmationModal from "../../components/ConfirmationModal";
import { messages } from "../messages";
import { useUserContext } from "../../store/UserContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlus, faTh } from "@fortawesome/free-solid-svg-icons";

type MetricsProps = {
  serviceName: string;
};

export default function Metrics({ serviceName }: MetricsProps) {
  const aggregatorFunc = ["._sum", "._avg", "._min", "._max", "._rate"];
  const {user}=useUserContext();
  enum MetricType {
    TEMPORAL = "TEMPORAL",
    POINT_IN_TIME = "POINT_IN_TIME",
  }

  enum WidgetType {
    GRAPH = "GRAPH",
    NUMBER = "NUMBER",
    GAUGE = "GAUGE",
  }

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

  const timeStep = 15;

  const { clusterName } = useContext(AppContext);
  const [widgets, setWidgets] = useState<WidgetInfo[]>();
  const [metricsData, setMetricsData] = useState<any[]>([]);
  const [selectedTimeRangerOption, setSelectedTimeRangerOption] =
    useState<string>(timeRangeOptions[0]);
  const [showSelectTimeModal, setShowSelectTimeModal] =
    useState<boolean>(false);
  
  const [loading, setLoading] = useState<boolean>(true);
  const [showBrowseWidgetsModal, setShowBrowseWidgetsModal] = useState<boolean>(false);

  const activeWidgetData = useRef({});

  useEffect(() => {
    fetchActiveWidgets();
  }, [serviceName]);

  useEffect(() => {
    if (widgets) {
      loadMetrics(widgets);
    }
  }, [selectedTimeRangerOption]);

  const fetchActiveWidgets = async () => {
    const userName = user?.user_name || "";

    try {
      setLoading(true);
      const sectionName = `${serviceName.toUpperCase()}_SUMMARY`;

      // First, try to get active widget layouts
      try {
        const response = await metricsApi.getWidgets(
          userName,
          `WidgetLayoutInfo/section_name=${sectionName}`
        );

        if (response.items && response.items.length > 0) {
          // Active layouts found, use them
          activeWidgetData.current = response;
          const widgetsData = extractWidgets(response);
          setWidgets(widgetsData);
          loadMetrics(widgetsData);
          return;
        }
      } catch (error) {
        console.warn("No active widget layouts found, will create default layout", error);
      }

      // No active layouts found, create default layout
      await createDefaultWidgetLayout(sectionName, userName);

    } catch (error) {
      console.error("Error fetching active widgets:", error);
      // Fallback to basic widgets if everything fails
      await handleFallbackWidgets(serviceName, userName);
    } finally {
      setLoading(false);
    }
  };

  const createDefaultWidgetLayout = async (sectionName: string, userName: string) => {
    try {
      // Step 1: Get all active widget layouts for the user
      const activeWidgetLayoutsData = await metricsApi.getAllActiveWidgetLayouts(userName);
      
      // Step 2: Get default layout for this service
      const defaultLayoutName = `default_${serviceName.toLowerCase()}_dashboard`;
      const defaultWidgetLayoutData = await metricsApi.getDefaultWidgetLayoutByName(defaultLayoutName);
      
      if (!defaultWidgetLayoutData.items || defaultWidgetLayoutData.items.length === 0) {
        console.warn("No default layout found, using service-specific widgets");
        await createLayoutFromServiceWidgets(sectionName, userName);
        return;
      }
      
      // Step 3: Create user-specific layout based on default
      const defaultLayout = defaultWidgetLayoutData.items[0].WidgetLayoutInfo;
      const userLayoutName = `${userName}_${serviceName.toLowerCase()}_dashboard`;
      
      const userLayoutData = {
        WidgetLayoutInfo: {
          display_name: defaultLayout.display_name,
          layout_name: userLayoutName,
          scope: "USER",
          section_name: sectionName,
          user_name: userName,
          widgets: defaultLayout.widgets.map((widget: any) => ({
            id: widget.WidgetInfo ? widget.WidgetInfo.id : widget.id
          }))
        }
      };
      
      // Step 4: Create the user layout
      const userLayoutResponse = await metricsApi.createUserWidgetLayout(clusterName, userLayoutData);
      const newLayoutId = userLayoutResponse.resources[0].WidgetLayoutInfo.id;
      
      // Step 5: Update active widget layouts
      const existingLayouts = activeWidgetLayoutsData.items || [];
      const widgetLayouts = existingLayouts.map((item: any) => ({ id: item.WidgetLayoutInfo.id }));
      widgetLayouts.push({ id: newLayoutId });
      
      const activeWidgetLayouts = { WidgetLayouts: widgetLayouts };
      await metricsApi.saveActiveWidgetLayouts(userName, activeWidgetLayouts);
      
      // Step 6: Fetch the newly created layout
      const response = await metricsApi.getWidgets(
        userName,
        `WidgetLayoutInfo/section_name=${sectionName}`
      );
      
      activeWidgetData.current = response;
      const widgetsData = extractWidgets(response);
      setWidgets(widgetsData);
      loadMetrics(widgetsData);
      
    } catch (error) {
      console.error("Error creating default widget layout:", error);
      await createLayoutFromServiceWidgets(sectionName, userName);
    }
  };

  const createLayoutFromServiceWidgets = async (sectionName: string, userName: string) => {
    try {
      // Get widgets specific to this service
      const serviceWidgets = await metricsApi.getWidgetsByService(clusterName, serviceName);
      
      if (!serviceWidgets.items || serviceWidgets.items.length === 0) {
        console.warn("No service-specific widgets found, using fallback");
        await handleFallbackWidgets(serviceName, userName);
        return;
      }
      
      // Create a layout with these widgets
      const userLayoutName = `${userName}_${serviceName.toLowerCase()}_dashboard`;
      const userLayoutData = {
        WidgetLayoutInfo: {
          display_name: `${serviceName} Dashboard`,
          layout_name: userLayoutName,
          scope: "USER",
          section_name: sectionName,
          user_name: userName,
          widgets: serviceWidgets.items.map((widget: any) => ({
            id: widget.WidgetInfo.id
          }))
        }
      };
      
      const userLayoutResponse = await metricsApi.createUserWidgetLayout(clusterName, userLayoutData);
      const newLayoutId = userLayoutResponse.resources[0].WidgetLayoutInfo.id;
      
      // Set as active layout
      const activeWidgetLayouts = { WidgetLayouts: [{ id: newLayoutId }] };
      await metricsApi.saveActiveWidgetLayouts(userName, activeWidgetLayouts);
      
      // Fetch the created layout
      const response = await metricsApi.getWidgets(
        userName,
        `WidgetLayoutInfo/section_name=${sectionName}`
      );
      
      activeWidgetData.current = response;
      const widgetsData = extractWidgets(response);
      setWidgets(widgetsData);
      loadMetrics(widgetsData);
      
    } catch (error) {
      console.error("Error creating layout from service widgets:", error);
      await handleFallbackWidgets(serviceName, userName);
    }
  };

  const handleFallbackWidgets = async (serviceName: string, userName: string) => {
    console.warn("Using fallback widget data for", serviceName);
    
    // Create fallback widgets based on service type
    const fallbackWidgets = createFallbackWidgetsForService(serviceName, userName);
    
    const fallbackResponse = {
      items: [
        {
          WidgetLayoutInfo: {
            id: "fallback-layout",
            layout_name: `${serviceName.toLowerCase()}_dashboard`,
            section_name: `${serviceName}_SUMMARY`,
            scope: "USER",
            widgets: fallbackWidgets
          }
        }
      ]
    };
    
    activeWidgetData.current = fallbackResponse;
    const widgetsData = extractWidgets(fallbackResponse);
    setWidgets(widgetsData);
    loadMetrics(widgetsData);
  };

  const getRequestData = (widgets: WidgetInfo[]) => {
    const metricsData: { [key: string]: any[] } = {};

    for (const widget of widgets || []) {
      const parsedMetrics = JSON.parse(widget.metrics);

      for (const metric of parsedMetrics) {
        const serviceName = get(metric, "service_name", "");
        const componentName = get(metric, "component_name", "");
        const hostComponentCriteria = get(
          metric,
          "host_component_criteria",
          ""
        );

        const key = hostComponentCriteria
          ? `${serviceName}_${componentName}_${hostComponentCriteria}`
          : `${serviceName}_${componentName}`;

        metric["metric_type"] =
          widget.widget_type === "GRAPH"
            ? MetricType.TEMPORAL
            : MetricType.POINT_IN_TIME;

        if (!metricsData[key]) {
          metricsData[key] = [];
        }
        metricsData[key].push(metric);
      }
    }

    return metricsData;
  };

  const loadMetrics = (widgetsData: WidgetInfo[]) => {
    // Clear old metrics data before loading new ones
    setMetricsData([]);

    const metricsData: { [key: string]: any[] } = getRequestData(widgetsData);

    for (const key in metricsData) {
      try {
        const metrics = metricsData[key];

        const componentName = get(metrics, "0.component_name", "");
        const hostComponentCriteria = get(
          metrics,
          "0.host_component_criteria",
          ""
        );
        const serviceName = get(metrics, "0.service_name", "");

        if (hostComponentCriteria.length > 0) {
          getHostComponentMetrics(metrics, componentName, hostComponentCriteria);
        } else {
          getServiceComponentMetrics(metrics, componentName, serviceName);
        }
      } catch (error) {
        console.error("Error processing metrics for key:", key, error);
      }
    }
  };

  const getServiceComponentMetrics = async (
    requestMetrics: any,
    componentName: string,
    serviceName: string
  ) => {
    let temporalFields = [];
    let pointInTimeFields = [];

    for (let requestMetric of requestMetrics) {
      const metricType = get(requestMetric, "metric_type");

      if (metricType === MetricType.TEMPORAL) {
        const pathWithTime = get(requestMetric, "metric_path") + addTimeRange(selectedTimeRangerOption);
        temporalFields.push(pathWithTime);
      } else {
        const path = get(requestMetric, "metric_path");
        pointInTimeFields.push(path);
      }
    }

    const temporalMetricsPath = temporalFields.join(",");
    const pointInTimeMetricsPath = pointInTimeFields.join(",");

    try {
      if (temporalMetricsPath) {
        const temporalData = await metricsApi.getServiceComponentMetrics(
          clusterName,
          componentName,
          serviceName,
          temporalMetricsPath
        );

        const temporalMetrics = requestMetrics.filter(
          (metric: any) => metric.metric_type === MetricType.TEMPORAL
        );
        addDataValues(temporalData, temporalMetrics);
      }

      if (pointInTimeMetricsPath) {
        const pointInTimeData = await metricsApi.getServiceComponentMetrics(
          clusterName,
          componentName,
          serviceName,
          pointInTimeMetricsPath
        );

        const pointInTimeMetrics = requestMetrics.filter(
          (metric: any) => metric.metric_type === MetricType.POINT_IN_TIME
        );
        addDataValues(pointInTimeData, pointInTimeMetrics);
      }
    } catch (error) {
      console.error("Error fetching service component metrics:", error);
    }
  };

  const getHostComponentMetrics = async (
    requestMetrics: any,
    componentName: string,
    hostComponentCriteria: string
  ) => {
    let temporalFields = [];
    let pointInTimeFields = [];

    for (let requestMetric of requestMetrics) {
      const metricType = get(requestMetric, "metric_type");

      if (metricType === MetricType.TEMPORAL) {
        const pathWithTime = get(requestMetric, "metric_path") + addTimeRange(selectedTimeRangerOption);
        temporalFields.push(pathWithTime);
      } else {
        const path = get(requestMetric, "metric_path");
        pointInTimeFields.push(path);
      }
    }

    const temporalMetricsPath = temporalFields.join(",");
    const pointInTimeMetricsPath = pointInTimeFields.join(",");
    const cleanedCriteria = hostComponentCriteria.replace("host_components/", "");

    try {
      if (temporalMetricsPath) {
        const temporalData = await metricsApi.getHostComponentMetrics(
          clusterName,
          componentName,
          cleanedCriteria,
          temporalMetricsPath
        );

        if (temporalData.items && temporalData.items.length > 0) {
          const temporalMetrics = requestMetrics.filter(
            (metric: any) => metric.metric_type === MetricType.TEMPORAL
          );
          addDataValues(temporalData.items[0], temporalMetrics);
        }
      }

      if (pointInTimeMetricsPath) {
        const pointInTimeData = await metricsApi.getHostComponentMetrics(
          clusterName,
          componentName,
          cleanedCriteria,
          pointInTimeMetricsPath
        );

        if (pointInTimeData.items && pointInTimeData.items.length > 0) {
          const pointInTimeMetrics = requestMetrics.filter(
            (metric: any) => metric.metric_type === MetricType.POINT_IN_TIME
          );
          addDataValues(pointInTimeData.items[0], pointInTimeMetrics);
        }
      }
    } catch (error) {
      console.error("Error fetching host component metrics:", error);
    }
  };

  const addDataValues = (data: any, metricsToAdd: any) => {
    const metricsToAddCopy = cloneDeep(metricsToAdd);

    for (const metric of metricsToAddCopy) {
      const metricPath = get(metric, "metric_path");

      if (!metricPath) {
        continue;
      }

      let dataValue = null;
      let isAggregatorFunc = false;

      // First try to get the data directly using the full path
      const directDataPath = metricPath.split("/").join(".");

      dataValue = get(data, directDataPath, null);

      // If data is null, check if it's an aggregate function
      if (dataValue === null) {
        for (const func of aggregatorFunc) {
          if (metricPath.endsWith(func) && !isAggregatorFunc) {
            isAggregatorFunc = true;

            const metricPathParts = metricPath.split("/");
            const metricBeanProperty = metricPathParts.pop();
            let basePath = metricPathParts.join("/");

            // Remove trailing slash if present
            if (basePath.endsWith("/")) {
              basePath = basePath.slice(0, -1);
            }

            const baseDataPath = basePath.split("/").join(".");

            const metricBean = get(data, baseDataPath, null);

            if (metricBean !== null && metricBeanProperty) {
              dataValue = get(metricBean, metricBeanProperty, null);
            }
            break;
          }
        }
      }

      if (dataValue !== null && dataValue !== undefined) {
        metric.data = dataValue;
      }
    }

    const metricsWithData = metricsToAddCopy.filter((m: any) => m.data !== undefined);

    setMetricsData((prevMetricsData) => {
      const newMetricsData = [...prevMetricsData, ...metricsWithData];
      return newMetricsData;
    });
  };

  const handleClone = (widget: WidgetInfo) => {
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle={get(messages, "popup.confirmation.commonHeader", "")}
        modalBody={get(messages, "widget.clone.body").replace(
          "{0}",
          get(widget, "widget_name")
        )}
        successCallback={() => {
          onCloneWidget(widget);
          modalManager.hide();
        }}
        okButtonText={get(messages, "common.clone").toUpperCase()}
      />
    );
  };

  const onCloneWidget = async (widget: WidgetInfo) => {
    const clonedWidgetData = {
      WidgetInfo: {
        widget_name: widget.widget_name + "(Copy)",
        widget_type: widget.widget_type,
        scope: "USER",
        metrics: JSON.parse(widget.metrics),
        values: JSON.parse(widget.values),
        properties: JSON.parse(widget.properties),
      },
    };
    try {
      const createWidgetResponse = await metricsApi.createWidget(
        clusterName,
        clonedWidgetData
      );
      const widgetId = get(
        createWidgetResponse,
        "resources.[0].WidgetInfo.id",
        ""
      );
      if (widgetId) {
        const widgetLayoutInfo = get(
          activeWidgetData.current,
          "items.[0].WidgetLayoutInfo",
          ""
        );
        if (!isEmpty(widgetLayoutInfo)) {
          let newWidgetsList = get(widgetLayoutInfo, "widgets", []).map(
            (w: any) => {
              return {
                id: get(w, "WidgetInfo.id", ""),
              };
            }
          );
          newWidgetsList.push({
            id: widgetId,
          });
          const updatedWidgetLayoutData = {
            WidgetLayoutInfo: {
              display_name: get(widgetLayoutInfo, "display_name", ""),
              id: get(widgetLayoutInfo, "id", ""),
              layout_name: get(widgetLayoutInfo, "layout_name", ""),
              scope: "USER",
              section_name: get(widgetLayoutInfo, "section_name", ""),
              widgets: newWidgetsList,
            },
          };
          await metricsApi.updateWidgetLayout(
            clusterName,
            updatedWidgetLayoutData,
            get(widgetLayoutInfo, "id", "")
          );
          fetchActiveWidgets();
        }
      }
    } catch (error) {
      console.error("Error cloning widget:", error);
    }
  };

  const onDeleteWidget = async (widget: WidgetInfo) => {
    const widgetLayoutInfo = get(
      activeWidgetData.current,
      "items.[0].WidgetLayoutInfo",
      ""
    );
    if (isEmpty(widgetLayoutInfo)) return;
    const widgetId = get(widget, "id", "");
    let newWidgetsList = get(widgetLayoutInfo, "widgets", [])
      .map((w: any) => {
        return {
          id: get(w, "WidgetInfo.id", ""),
        };
      })
      .filter((w: any) => w.id !== widgetId);
    const updatedWidgetLayoutData = {
      WidgetLayoutInfo: {
        display_name: get(widgetLayoutInfo, "display_name", ""),
        id: get(widgetLayoutInfo, "id", ""),
        layout_name: get(widgetLayoutInfo, "layout_name", ""),
        scope: "USER",
        section_name: get(widgetLayoutInfo, "section_name", ""),
        widgets: newWidgetsList,
      },
    };
    try {
      await metricsApi.updateWidgetLayout(
        clusterName,
        updatedWidgetLayoutData,
        get(widgetLayoutInfo, "id", "")
      );
      setWidgets((prevWidgets) =>
        prevWidgets?.filter((w) => w.id !== widgetId)
      );
      let widgets = get(
        activeWidgetData.current,
        "items.[0].WidgetLayoutInfo.widgets",
        []
      );
      widgets = widgets.filter((w: any) => w.WidgetInfo.id !== widgetId);
      set(
        activeWidgetData.current,
        "items.[0].WidgetLayoutInfo.widgets",
        widgets
      );
    } catch (error) {
      console.error("Error deleting widget:", error);
    }
  };

  const onEditWidget = (_widget: WidgetInfo) => {
    // Implement the edit functionality here
  };

  const renderWidgets = (widget: WidgetInfo) => {
    switch (widget.widget_type) {
      case WidgetType.GRAPH:
        return (
          <WidgetContainer
            onClone={() => handleClone(widget)}
            onDelete={() => onDeleteWidget(widget)}
            onEdit={() => onEditWidget(widget)}
            widgetHeader={widget.widget_name}
          >
            <GraphWidgetView widgetInfo={widget} metricsValues={metricsData} />
          </WidgetContainer>
        );
      case WidgetType.NUMBER:
        return (
          <WidgetContainer
            onClone={() => handleClone(widget)}
            onDelete={() => onDeleteWidget(widget)}
            onEdit={() => onEditWidget(widget)}
            widgetHeader={widget.widget_name}
          >
            {RenderNumberWidget(widget)}
          </WidgetContainer>
        );
      case WidgetType.GAUGE:
        return (
          <WidgetContainer
            onClone={() => handleClone(widget)}
            onDelete={() => onDeleteWidget(widget)}
            onEdit={() => onEditWidget(widget)}
            widgetHeader={widget.widget_name}
          >
            <GaugeWidgetView widgetInfo={widget} metricsValues={metricsData} />
          </WidgetContainer>
        );
    }
  };

  const RenderNumberWidget = (widget: WidgetInfo) => {
    const metricData = JSON.parse(widget.metrics);
    const metricName = metricData[0].name;
    const metricValue = metricsData.find(
      (item: any) => item.name === metricName
    );
    const metricDataValue = metricValue ? metricValue.data : [];

    return <ChartContainer text={metricDataValue}></ChartContainer>;
  };

  const extractWidgets = (data: any) => {
    if (!data || !data.items) {
      return [];
    }

    const widgets = data.items.flatMap((item: any) => {
      const widgetInfos = item.WidgetLayoutInfo.widgets.map((widget: any) => {
        return widget.WidgetInfo;
      });
      return widgetInfos;
    });

    return widgets;
  };

  const addTimeRange = (timeRange: string): string => {
    const now = new Date();
    const formattedDate = formatDate(now);
    const startTime = getTimeInNumber(formattedDate);
    const duration = durationMap[timeRange.replace("Last ", "")];

    if (duration === undefined) {
      throw new Error(`Invalid time range: ${timeRange}`);
    }

    const endTime = startTime - duration;
    const timeRangeStr = `[${endTime},${startTime},${timeStep}]`;

    return timeRangeStr;
  };

  if (loading) {
    return <Spinner />;
  }

  if (isEmpty(widgets)) {
    return (
      <Card className="mx-3 my-4 p-5">
        <Card.Body className="d-flex align-items-center justify-content-center">
          <h4 className="text-muted mb-0">No Widgets to show</h4>
        </Card.Body>
      </Card>
    );
  }
  return (
    <>
      {showSelectTimeModal ? (
        <SelectTimeRangeModal
          isOpen={showSelectTimeModal}
          onClose={() => setShowSelectTimeModal(false)}
          successCallback={(data) => {
            setSelectedTimeRangerOption(
              "CUSTOM: " +
                formatDate(new Date(data.startTime * 1000))
                  .split("T")
                  .join(" ")
            );
            setShowSelectTimeModal(false);
          }}
        />
      ) : null}
      
      <BrowseWidgetsModal
        isOpen={showBrowseWidgetsModal}
        onClose={() => setShowBrowseWidgetsModal(false)}
        serviceName={serviceName}
        onWidgetAdded={() => {
          setShowBrowseWidgetsModal(false);
          fetchActiveWidgets();
        }}
      />
      <Card className="p-5 mt-2">
        <div className="d-flex justify-content-between mb-4">
          <div></div> {/* Empty div for flex spacing */}
          <div className="d-flex">
            <Dropdown className="me-2">
              <Dropdown.Toggle 
                variant="success" 
                className="text-uppercase"
              >
                ACTIONS
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.Item onClick={() => {}}>
                  <FontAwesomeIcon icon={faPlus} className="me-2" /> Create Widget
                </Dropdown.Item>
                <Dropdown.Item onClick={() => setShowBrowseWidgetsModal(true)}>
                  <FontAwesomeIcon icon={faTh} className="me-2" /> Browse Widgets
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
            
            <Dropdown>
              <Dropdown.Toggle variant="transparent" className="btn-default">
                <span className="me-2">{selectedTimeRangerOption}</span>
              </Dropdown.Toggle>
              <Dropdown.Menu className="rounded-0">
                {timeRangeOptions.map((option) => (
                  <Dropdown.Item
                    key={option}
                    onClick={() => {
                      setSelectedTimeRangerOption(option);
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
        </div>
        <Row>
          {widgets?.map((widget: WidgetInfo) => (
            <Col md={3} key={widget.id} className="mb-3">
              {renderWidgets(widget)}
            </Col>
          ))}
        </Row>
      </Card>
    </>
  );
}
