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

import { Badge, Col, Row, Stack } from "react-bootstrap";
import { filter, find, lowerCase, startCase } from "lodash";
import { useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useHostsFilterNavigation } from "../Hosts/hostsFilterNavigation";
import { statusIconMap } from "./constants";
import modalManager from "../../store/ModalManager";
import { AlertsModal } from "./ServiceAlerts";

interface HDFSFederationSummaryProps {
  hdfsModel: any;
  masterComponents: any[];
  alerts: any;
}

function HDFSFederationSummary({
  hdfsModel,
  masterComponents,
  alerts,
}: HDFSFederationSummaryProps) {
  const navigate = useNavigate();
  const { goToHostsFilteredByComponent } = useHostsFilterNavigation();

  function getComponentAlerts(componentName: string) {
    const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
    const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
    const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
      (alert) => alert.component_name === componentName
    );
    const criticalCount =
      alertsForComponent?.[0]?.summary?.["CRITICAL"]?.count || 0;
    const warningCount =
      alertsForComponent?.[0]?.summary?.["WARNING"]?.count || 0;
    return {
      alerts: alertsForComponent,
      count: criticalCount + warningCount,
    };
  }

  // Get the active NameNode host for a specific namespace (following Ember logic)
  function getActiveNameNodeHostForNamespace(
    _namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Find all NameNode host components in this namespace
    const nameNodeHostComponents = namespaceHosts
      .map((hostName) => {
        return find(masterComponents, [
          "componentName",
          "NAMENODE",
        ])?.hostComponents?.find(
          (hc: any) => hc.HostRoles.host_name === hostName
        );
      })
      .filter((hc) => hc); // Filter out null/undefined

    // Find the active NameNode first (following Ember logic)
    const activeNameNode = nameNodeHostComponents.find(
      (hc) => hc.haStatus === "active"
    );

    // For federation, we ONLY use active NameNodes, not standby ones
    return activeNameNode;
  }

  // Get namenode uptime for a specific namespace
  function getNameNodeUptime(namespaceName: string, namespaceHosts: string[]) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    // Only show uptime if there's an active NameNode (not standby)
    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.haStatus !== "active"
    ) {
      return "Not Running";
    }

    // Get uptime from the NameNode's metrics.runtime.StartTime
    const startTime = activeNameNodeComponent.metrics?.runtime?.StartTime;

    if (startTime && hdfsModel.timingFormat) {
      // Calculate uptime similar to Ember's approach
      const currentTime = Date.now();
      const diff = currentTime - startTime;
      return hdfsModel.timingFormat(diff);
    }

    return "Not Running";
  }

  // Get namenode heap usage for a specific namespace
  function getNameNodeHeapUsage(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    // Only show heap usage if there's an active NameNode (not standby)
    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.haStatus !== "active"
    ) {
      return "0.0%";
    }

    // Get heap data from the NameNode's metrics.jvm
    const heapUsed = activeNameNodeComponent.metrics?.jvm?.HeapMemoryUsed;
    const heapMax = activeNameNodeComponent.metrics?.jvm?.HeapMemoryMax;

    if (heapUsed && heapMax) {
      const percentage = ((heapUsed / heapMax) * 100).toFixed(1);
      return `${percentage}%`;
    }

    return "0.0%";
  }

  // Get namenode heap details for a specific namespace
  function getNameNodeHeapDetails(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    // Only show heap details if there's an active NameNode (not standby)
    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.haStatus !== "active"
    ) {
      return "N/A / N/A";
    }

    // Get heap data from the NameNode's metrics.jvm
    const heapUsed = activeNameNodeComponent.metrics?.jvm?.HeapMemoryUsed;
    const heapMax = activeNameNodeComponent.metrics?.jvm?.HeapMemoryMax;

    if (heapUsed && heapMax && hdfsModel.diskPart) {
      return hdfsModel.diskPart(heapUsed, heapMax);
    }

    return "N/A / N/A";
  }

  // Get namespace-specific block values (following Ember logic)
  function getNamespaceBlockValue(
    namespaceName: string,
    namespaceHosts: string[],
    blockType: string
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.state !== "STARTED"
    ) {
      return "n/a";
    }

    // Get block data from the NameNode's metrics.dfs.FSNamesystem
    const fsNamesystem = activeNameNodeComponent.metrics?.dfs?.FSNamesystem;

    if (!fsNamesystem) {
      return "n/a";
    }

    switch (blockType) {
      case "total":
        return fsNamesystem.BlocksTotal || "n/a";
      case "corrupt":
        return fsNamesystem.CorruptBlocks || "n/a";
      case "missing":
        return fsNamesystem.MissingBlocks || "n/a";
      case "underReplicated":
        return fsNamesystem.UnderReplicatedBlocks || "n/a";
      default:
        return "n/a";
    }
  }

  // Get namespace-specific file values (following Ember logic)
  function getNamespaceFileValue(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.state !== "STARTED"
    ) {
      return "n/a";
    }

    // Get file data from the NameNode's metrics.dfs.FSNamesystem
    const fsNamesystem = activeNameNodeComponent.metrics?.dfs?.FSNamesystem;

    return fsNamesystem?.FilesTotal || "n/a";
  }

  // Get namespace-specific upgrade status (following Ember logic)
  function getNamespaceUpgradeStatus(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.state !== "STARTED"
    ) {
      return "n/a";
    }

    // Get upgrade status from the NameNode's metrics.dfs.namenode
    const upgradeFinalized =
      activeNameNodeComponent.metrics?.dfs?.namenode?.UpgradeFinalized;
    const hostName = activeNameNodeComponent.HostRoles.host_name;
    const healthStatus = hdfsModel.workStatusValues?.[hostName]?.healthStatus;

    if (upgradeFinalized !== undefined && hdfsModel.findUpgradeStatus) {
      return hdfsModel.findUpgradeStatus(upgradeFinalized, healthStatus);
    }

    return "n/a";
  }

  // Get namespace-specific safe mode status (following Ember logic)
  function getNamespaceSafeModeStatus(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    // Get the active NameNode host for this namespace (following Ember logic)
    const activeNameNodeComponent = getActiveNameNodeHostForNamespace(
      namespaceName,
      namespaceHosts
    );

    if (
      !activeNameNodeComponent ||
      activeNameNodeComponent.state !== "STARTED"
    ) {
      return "n/a";
    }

    // Get safe mode status from the NameNode's metrics.dfs.namenode
    const safeModeStatus =
      activeNameNodeComponent.metrics?.dfs?.namenode?.Safemode;

    if (safeModeStatus !== undefined && hdfsModel.findSafeModeStatus) {
      return hdfsModel.findSafeModeStatus(safeModeStatus);
    }

    return "n/a";
  }

  // Get DataNode status counts from NameNode metrics (following Ember logic)
  function getDataNodeStatusCounts() {
    // Get all active NameNodes across all namespaces
    const allActiveNameNodes = hdfsModel.federationNamespaces
      ?.map((namespace: any) => {
        return getActiveNameNodeHostForNamespace(
          namespace.name,
          namespace.hosts
        );
      })
      .filter((nn:any) => nn && nn.haStatus === "active");

    let totalLive = 0;
    let totalDead = 0;
    let totalDecommissioned = 0;

    // Aggregate DataNode status from all active NameNodes
    allActiveNameNodes.forEach((activeNameNode: any) => {
      const liveNodesJson = activeNameNode.metrics?.dfs?.namenode?.LiveNodes;
      const deadNodesJson = activeNameNode.metrics?.dfs?.namenode?.DeadNodes;
      const decomNodesJson = activeNameNode.metrics?.dfs?.namenode?.DecomNodes;

      if (liveNodesJson) {
        try {
          const liveNodes = JSON.parse(liveNodesJson);
          totalLive += Object.keys(liveNodes).length;
        } catch (e) {
          console.error("Error parsing LiveNodes:", e);
        }
      }

      if (deadNodesJson) {
        try {
          const deadNodes = JSON.parse(deadNodesJson);
          totalDead += Object.keys(deadNodes).length;
        } catch (e) {
          console.error("Error parsing DeadNodes:", e);
        }
      }

      if (decomNodesJson) {
        try {
          const decomNodes = JSON.parse(decomNodesJson);
          totalDecommissioned += Object.keys(decomNodes).length;
        } catch (e) {
          console.error("Error parsing DecomNodes:", e);
        }
      }
    });

    return {
      live: totalLive || "n/a",
      dead: totalDead || "n/a",
      decommissioned: totalDecommissioned || "n/a",
    };
  }

  // Get NameNode info for a specific namespace
  function getNameNodeInfoForNamespace(
    namespaceName: string,
    namespaceHosts: string[]
  ) {
    return {
      uptime: getNameNodeUptime(namespaceName, namespaceHosts),
      heapPercent: getNameNodeHeapUsage(namespaceName, namespaceHosts),
      heapDetails: getNameNodeHeapDetails(namespaceName, namespaceHosts),
    };
  }

  const dataNodeStatus = getDataNodeStatusCounts();

  return (
    <>
      {/* Render each namespace */}
      {hdfsModel.federationNamespaces?.map((namespace: any, index: number) => {
        const namespaceName =
          namespace.name || `something${index > 0 ? index + 1 : ""}`;
        const namespaceHosts = namespace.hosts || [];
        const namespaceInfo = getNameNodeInfoForNamespace(
          namespaceName,
          namespaceHosts
        );

        return (
          <div key={namespaceName} className="mb-5">
            {/* Namespace header with NameNodes */}
            <Row>
              <Col md={2}>
                <h3 className="mb-0 text-light">{namespaceName}</h3>
              </Col>

              {/* Render NameNodes for this namespace */}
              {namespaceHosts
                .slice(0, 2)
                .map((hostName: string) => {
                  const hostComponent = find(masterComponents, [
                    "componentName",
                    "NAMENODE",
                  ])?.hostComponents?.find(
                    (hc: any) => hc.HostRoles.host_name === hostName
                  );

                  if (!hostComponent) return null;

                  const component = hostComponent.HostRoles.component_name;
                  const state = hostComponent?.state;
                  const haStatus = hostComponent.haStatus || "";

                  const icon =
                    hostComponent.passiveState == "OFF"
                      ? statusIconMap[lowerCase(state)]
                      : hostComponent?.passiveState
                      ? statusIconMap["Maintenance"]
                      : statusIconMap[lowerCase(state)];

                  const displayState =
                    startCase(state?.toLowerCase()) === "Installed"
                      ? "Stopped"
                      : startCase(state?.toLowerCase());

                  return (
                    <Col md={2} key={hostName}>
                      <Stack>
                        <Stack
                          direction="horizontal"
                          className="align-items-center"
                        >
                          <FontAwesomeIcon
                            icon={icon?.icon}
                            className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                          />
                          <h3 className="text-dark mb-0">{displayState}</h3>
                          {getComponentAlerts(component)?.alerts?.length ? (
                            <Badge
                              className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                              onClick={() => {
                                modalManager.show(
                                  <AlertsModal
                                    alerts={filter(alerts, [
                                      "component_name",
                                      component,
                                    ])}
                                    navigate={navigate}
                                    serviceName={
                                      getComponentAlerts(component)?.alerts?.[0]
                                        ?.service_name
                                    }
                                  />
                                );
                              }}
                            >
                              {getComponentAlerts(component).count}
                            </Badge>
                          ) : null}
                        </Stack>
                        <div
                          className="custom-link text-uppercase fs-12 mt-2"
                          onClick={() => {
                            navigate(
                              `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                            );
                          }}
                        >
                          {haStatus} NAMENODE
                        </div>
                      </Stack>
                    </Col>
                  );
                })}

              {/* Render ZKFailoverControllers for this namespace */}
              {namespaceHosts
                .slice(0, 2)
                .map((hostName: string) => {
                  // Look for ZKFC in both master and slave components
                  let zkfcComponent = null;

                  // First try to find in master components
                  const masterZkfc = find(masterComponents, [
                    "componentName",
                    "ZKFC",
                  ]);
                  if (masterZkfc) {
                    zkfcComponent = masterZkfc.hostComponents?.find(
                      (hc: any) => hc.HostRoles.host_name === hostName
                    );
                  }

                  // If not found in master, try slave components
                  if (!zkfcComponent && hdfsModel.slaveComponents) {
                    const slaveZkfc = find(hdfsModel.slaveComponents, [
                      "componentName",
                      "ZKFC",
                    ]);
                    if (slaveZkfc) {
                      zkfcComponent = slaveZkfc.hostComponents?.find(
                        (hc: any) => hc.HostRoles.host_name === hostName
                      );
                    }
                  }

                  // If still not found, create a default ZKFC component for federation
                  if (!zkfcComponent) {
                    // In federation, ZKFC should exist on the same hosts as NameNodes
                    const nameNodeComponent = find(masterComponents, [
                      "componentName",
                      "NAMENODE",
                    ])?.hostComponents?.find(
                      (hc: any) => hc.HostRoles.host_name === hostName
                    );

                    if (nameNodeComponent) {
                      zkfcComponent = {
                        HostRoles: {
                          component_name: "ZKFC",
                          host_name: hostName,
                        },
                        state: nameNodeComponent.state, // ZKFC typically has same state as NameNode
                        passiveState: nameNodeComponent.passiveState,
                      };
                    }
                  }

                  if (!zkfcComponent) return null;

                  const state = zkfcComponent?.state;

                  const icon =
                    zkfcComponent.passiveState == "OFF"
                      ? statusIconMap[lowerCase(state)]
                      : zkfcComponent?.passiveState
                      ? statusIconMap["Maintenance"]
                      : statusIconMap[lowerCase(state)];

                  const displayState =
                    startCase(state?.toLowerCase()) === "Installed"
                      ? "Stopped"
                      : startCase(state?.toLowerCase());

                  return (
                    <Col md={2} key={`zkfc-${hostName}`}>
                      <Stack>
                        <Stack
                          direction="horizontal"
                          className="align-items-center"
                        >
                          <FontAwesomeIcon
                            icon={icon?.icon}
                            className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                          />
                          <h3 className="text-dark mb-0">{displayState}</h3>
                          {getComponentAlerts("ZKFC")?.alerts?.length ? (
                            <Badge
                              className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                              onClick={() => {
                                modalManager.show(
                                  <AlertsModal
                                    alerts={filter(alerts, [
                                      "component_name",
                                      "ZKFC",
                                    ])}
                                    navigate={navigate}
                                    serviceName={
                                      getComponentAlerts("ZKFC")?.alerts?.[0]
                                        ?.service_name
                                    }
                                  />
                                );
                              }}
                            >
                              {getComponentAlerts("ZKFC").count}
                            </Badge>
                          ) : null}
                        </Stack>
                        <div
                          className="custom-link text-uppercase fs-12 mt-2"
                          onClick={() => {
                            navigate(
                              `/main/hosts/${zkfcComponent.HostRoles.host_name}/summary`
                            );
                          }}
                        >
                          ZKFAILOVERCONTROLLER
                        </div>
                      </Stack>
                    </Col>
                  );
                })}
            </Row>

            {/* NameNode Uptime and Heap for this namespace - displayed once per namespace */}
            <Row className="mt-4">
              <Col md={2}></Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">{namespaceInfo.uptime}</h3>
                  <div className="fs-12 text-light">NAMENODE UPTIME</div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {namespaceInfo.heapPercent}
                  </h3>
                  <div className="fs-12 text-light">
                    {namespaceInfo.heapDetails}
                  </div>
                  <div className="fs-12 text-light">NAMENODE HEAP</div>
                </Stack>
              </Col>
            </Row>

            {/* Blocks section for this namespace */}
            <Row className="mt-2">
              <Col md={2} />
              <Col md={2}>
                <div className="fs-12 text-light">BLOCKS</div>
              </Col>
              <Col />
            </Row>
            <Row className="mt-2">
              <Col md={2}></Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceBlockValue(
                      namespaceName,
                      namespaceHosts,
                      "total"
                    )}
                  </h3>
                  <div className="fs-12 text-light">Total</div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceBlockValue(
                      namespaceName,
                      namespaceHosts,
                      "corrupt"
                    )}
                  </h3>
                  <div className="fs-12 text-light">Corrupt Replica</div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceBlockValue(
                      namespaceName,
                      namespaceHosts,
                      "missing"
                    )}
                  </h3>
                  <div className="fs-12 text-light">Missing</div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceBlockValue(
                      namespaceName,
                      namespaceHosts,
                      "underReplicated"
                    )}
                  </h3>
                  <div className="fs-12 text-light">Under Replicated</div>
                </Stack>
              </Col>
            </Row>

            {/* Files and status section */}
            <Row className="mt-4">
              <Col md={2}></Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceFileValue(namespaceName, namespaceHosts)}
                  </h3>
                  <div className="fs-12 text-light">
                    TOTAL FILES + DIRECTORIES
                  </div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceUpgradeStatus(namespaceName, namespaceHosts)}
                  </h3>
                  <div className="fs-12 text-light">UPGRADE STATUS</div>
                </Stack>
              </Col>
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {getNamespaceSafeModeStatus(namespaceName, namespaceHosts)}
                  </h3>
                  <div className="fs-12 text-light">SAFE MODE STATUS</div>
                </Stack>
              </Col>
            </Row>
          </div>
        );
      })}

      {/* Service Metrics Section */}
      <Row className="mt-4">
        <Col md={2}>
          <h3 className="text-light">Service Metrics</h3>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.findCapacityPercentage
                ? hdfsModel.findCapacityPercentage(
                    hdfsModel.capacityUsed,
                    hdfsModel.capacityTotal
                  )
                : "0%"}
            </h3>
            <div className="fs-12 text-light">
              {hdfsModel.diskPart
                ? hdfsModel.diskPart(
                    hdfsModel.capacityUsed,
                    hdfsModel.capacityTotal
                  )
                : "N/A / N/A"}
            </div>
            <div className="fs-12 text-light">DISK USAGE (DFS USED)</div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.findCapacityPercentage
                ? hdfsModel.findCapacityPercentage(
                    hdfsModel.capacityNonDfsUsed,
                    hdfsModel.capacityTotal
                  )
                : "0%"}
            </h3>
            <div className="fs-12 text-light">
              {hdfsModel.diskPart
                ? hdfsModel.diskPart(
                    hdfsModel.capacityNonDfsUsed,
                    hdfsModel.capacityTotal
                  )
                : "N/A / N/A"}
            </div>
            <div className="fs-12 text-light">DISK USAGE (NON DFS USED)</div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.findCapacityPercentage
                ? hdfsModel.findCapacityPercentage(
                    hdfsModel.capacityRemaining,
                    hdfsModel.capacityTotal
                  )
                : "0%"}
            </h3>
            <div className="fs-12 text-light">
              {hdfsModel.diskPart
                ? hdfsModel.diskPart(
                    hdfsModel.capacityRemaining,
                    hdfsModel.capacityTotal
                  )
                : "N/A / N/A"}
            </div>
            <div className="fs-12 text-light">DISK REMAINING</div>
          </Stack>
        </Col>
      </Row>

      {/* Components Section */}
      <Row className="mt-4">
        <Col md={2}>
          <h3 className="text-light">Components</h3>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.dataNodesStarted || 2}/{hdfsModel.dataNodesTotal || 2}{" "}
              Started
            </h3>
            <div
              className="custom-link text-uppercase fs-12 mt-2"
              onClick={() => goToHostsFilteredByComponent("DATANODE", "DataNode")}
            >
              DATANODES
            </div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.routersStarted || 0}/{hdfsModel.routersTotal || 0}{" "}
              Started
            </h3>
            <div
              className="custom-link text-uppercase fs-12 mt-2"
              onClick={() => goToHostsFilteredByComponent("HDFS_ROUTER", "Router")}
            >
              ROUTERS
            </div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.journalNodes?.length || 3}/
              {hdfsModel.journalNodes?.length || 3} Live
            </h3>
            <div
              className="custom-link text-uppercase fs-12 mt-2"
              onClick={() => goToHostsFilteredByComponent("JOURNALNODE", "JournalNode")}
            >
              JOURNALNODES
            </div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">
              {hdfsModel.nfsGatewaysStarted || 0}/
              {hdfsModel.nfsGatewaysTotal || 0} Started
            </h3>
            <div
              className="custom-link text-uppercase fs-12 mt-2"
              onClick={() => goToHostsFilteredByComponent("NFS_GATEWAY", "NFS Gateway")}
            >
              NFSGATEWAYS
            </div>
          </Stack>
        </Col>
      </Row>

      {/* DataNodes Status Section */}
      <Row className="mt-4">
        <Col md={2} />
        <Col md={2}>
          <div className="fs-12 text-light">DATANODES STATUS</div>
        </Col>
      </Row>
      <Row className="mt-2">
        <Col md={2} />
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">{dataNodeStatus.live}</h3>
            <div className="fs-12 text-light">Live</div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">{dataNodeStatus.dead}</h3>
            <div className="fs-12 text-light">Dead</div>
          </Stack>
        </Col>
        <Col md={2}>
          <Stack>
            <h3 className="text-dark mb-0">{dataNodeStatus.decommissioned}</h3>
            <div className="fs-12 text-light">Decommissioned</div>
          </Stack>
        </Col>
      </Row>
    </>
  );
}

export default HDFSFederationSummary;
