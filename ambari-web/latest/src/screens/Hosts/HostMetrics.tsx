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

import { Card, Dropdown } from "react-bootstrap";
import { hostMetricsOption } from "./constants";
import { get } from "lodash";
import { getComponentName } from "./utils";
import { IHost } from "../../models/host";
import NameNodeHeap from "../Dashboard/widgets/NameNodeHeap";
import NameNodeRpc from "../Dashboard/widgets/NameNodeRpc";
import NameNodeUptime from "../Dashboard/widgets/NameNodeUptime";
import HostMetricsGraph from "./HostMetricsGraph";
import { translate } from "../../Utils/Utility";
import NameNodeCpuPieChartView from "../Dashboard/widgets/NameNodeCpuPieChartView";

type HostMetricsProps = {
  metricsData: any;
  allHostModels: IHost[];
  selectedMetricsOption: string;
  setSelectedMetricsOption: (option: string) => void;
  setShowSelectTimeModal: (show: boolean) => void;
};

export const HostMetrics = ({
  metricsData,
  allHostModels,
  selectedMetricsOption,
  setSelectedMetricsOption,
  setShowSelectTimeModal,
}: HostMetricsProps) => {
  const hasNameNode = () => {
    return get(allHostModels, "[0].hostComponents", []).some(
      (component: any) => getComponentName(component) === "NAMENODE"
    );
  };

  return (
    <Card className="w-50 rounded-0">
      <div className="d-flex justify-content-between px-3 pt-3">
        <h3 className="mt-2">{translate("hosts.host.summary.hostMetrics")}</h3>
        <Dropdown>
          <Dropdown.Toggle variant="transparent" className="btn-default">
            <span className="me-2">{selectedMetricsOption}</span>
          </Dropdown.Toggle>
          <Dropdown.Menu className="rounded-0">
            {hostMetricsOption.map((option) => (
              <Dropdown.Item
                key={option}
                onClick={() => {
                  setSelectedMetricsOption(option);
                }}
              >
                {option}
              </Dropdown.Item>
            ))}
            <Dropdown.Item onClick={() => setShowSelectTimeModal(true)}>
              {translate("common.custom")}
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </div>
      <hr />
      <div>
        <HostMetricsGraph
          selectedMetricsOption={selectedMetricsOption}
          metricsData={get(metricsData, "metrics", {})}
        />
        {hasNameNode() ? (
          <div>
            <div className="d-flex mb-4">
              <Card className="widget-card h-100 border-light border-2 w-50 mx-4 rounded-0">
                <div className="px-4 pt-4">
                  {translate("dashboard.widgets.NameNodeHeap")}
                </div>
                <div className="d-flex justify-content-center pb-4 pt-3 text-muted">
                  <NameNodeHeap />
                </div>
              </Card>
              <Card className="widget-card h-100 border-light border-2 w-50 mx-4 rounded-0">
                <div className="px-4 pt-4">
                  {translate("dashboard.widgets.NameNodeCpu")}
                </div>
                <div className="d-flex justify-content-center pb-4 pt-3 text-muted">
                  <NameNodeCpuPieChartView />
                </div>
              </Card>
            </div>
            <div className="d-flex mb-4">
              <Card className="widget-card h-100 border-light border-2 w-50 mx-4 rounded-0">
                <div className="px-4 pt-4">
                  {translate("dashboard.widgets.NameNodeRpc")}
                </div>
                <div className="px-4 pb-4 pt-3 text-center text-muted">
                  <NameNodeRpc />
                </div>
              </Card>
              <Card className="widget-card h-100 border-light border-2 w-50 mx-4 rounded-0">
                <div className="px-4 pt-4">
                  {translate("dashboard.widgets.NameNodeUptime")}
                </div>
                <div className="px-4 pb-4 pt-3 text-center text-muted">
                  <NameNodeUptime />
                </div>
              </Card>
            </div>
          </div>
        ) : null}
      </div>
    </Card>
  );
};
