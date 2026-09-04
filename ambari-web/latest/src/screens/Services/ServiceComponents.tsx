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

import {
  cloneDeep,
  filter,
  find,
  isEmpty,
  isObject,
  lowerCase,
  startCase,
} from "lodash";
import {
  componentCategories,
  ServiceComponentsMap,
  statusIconMap,
} from "./constants";
import { Badge, Col, Row, Stack } from "react-bootstrap";
import { useContext, useEffect } from "react";
import { ServiceContext } from "../../store/ServiceContext";
import Spinner from "../../components/Spinner";
import { pluralize } from "../../Utils/Utility";
import modalManager from "../../store/ModalManager";
import { AlertsModal } from "./ServiceAlerts";
import { useNavigate } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Tooltip from "../../components/Tooltip";
import { getComponentAlerts } from "./alertUtils";
import FlumeSummary from "./FlumeSummary";
import HDFSFederationTopology from "./HDFSFederationTopology";

// Helper function to get component display name for alert modal titles
const getComponentDisplayName = (componentName: string): string => {
  const componentDisplayNames: { [key: string]: string } = {
    'NAMENODE': 'NameNode',
    'SECONDARY_NAMENODE': 'Secondary NameNode',
    'DATANODE': 'DataNode',
    'HBASE_MASTER': 'HBase Master',
    'HBASE_REGIONSERVER': 'HBase RegionServer',
    'RANGER_ADMIN': 'Ranger Admin',
    'RANGER_USERSYNC': 'Ranger UserSync',
    'ZOOKEEPER_SERVER': 'ZooKeeper Server',
    'HISTORYSERVER': 'History Server',
    'SPARK3_JOBHISTORYSERVER': 'Spark3 History Server',
    'RANGER_KMS_SERVER': 'Ranger KMS Server',
    'TRINO_COORDINATOR': 'Trino Coordinator',
    'TRINO_GATEWAY': 'Trino Gateway',
    'SSM_SERVER': 'SSM Server',
    'APP_TIMELINE_SERVER': 'Timeline Service',
    'RESOURCEMANAGER': 'ResourceManager',
    'YARN_REGISTRY_DNS': 'YARN Registry DNS',
    'HIVE_METASTORE': 'Hive Metastore',
    'HIVE_SERVER': 'HiveServer2',
    'KYUUBI': 'Kyuubi',
    'PINOT_CONTROLLER': 'Pinot Controller',
  };
  return componentDisplayNames[componentName] || componentName;
};

const TOOLTIP_MESSAGES = {
  GENERAL: {
    MAINTENANCE_MODE: 'Service is in maintenance mode',
    COMPONENT_HEALTH: 'Component health status',
  }
};

function HDFSSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["hdfs"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["hdfs"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const hdfsModel = allServiceModels["hdfs"];
    const masterComponents = allServiceModels?.["hdfs"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["hdfs"]?.slaveComponents || [];
    const federationNamespaces = hdfsModel?.federationNamespaces || [];
    const isFederated = federationNamespaces.length > 1;
    if (!hdfsModel) {
      return <Spinner />;
    }
    return (
      <>
        {isFederated && (
          <HDFSFederationTopology
            namespaces={federationNamespaces}
            masterComponents={masterComponents}
            slaveComponents={slaveComponents}
          />
        )}
        <Row>
          {!isFederated && find(masterComponents, [
            "componentName",
            "NAMENODE",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
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
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]
                                  ?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {hostComponent.haStatus} NAMENODE
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {!isFederated && find(masterComponents, [
            "componentName",
            "SECONDARY_NAMENODE",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
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
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]
                                  ?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      SNAMENODE
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {!isFederated && find(slaveComponents, [
            "componentName",
            "ZKFC",
          ])?.hostComponents?.map((hostComponent: any) => {
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                  </Stack>

                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                  <div 
                    className="custom-link text-uppercase fs-12 text-nowrap mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                      );
                    }}
                  >
                    {
                      find(slaveComponents, ["componentName", "ZKFC"])
                        ?.displayName
                    }
                  </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row>
          {slaveComponents.map((slaveComponent: any) => {
            if (slaveComponent.componentName === "ZKFC") {
              return null;
            }
            return (
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                    Started
                  </h3>
                  <div
                    className="custom-link text-uppercase fs-12 mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/component/${slaveComponent.componentName}`
                      );
                    }}
                  >
                    {pluralize(
                      2,
                      slaveComponent.displayName as string,
                      "s",
                      false
                    )}
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function HBASESummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["hbase"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["hbase"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const hbaseModel = allServiceModels["hbase"];
    const masterComponents =
      allServiceModels?.["hbase"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["hbase"]?.slaveComponents || [];
    if (!hbaseModel) {
      return <Spinner />;
    }
    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "HBASE_MASTER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]
                                  ?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {hostComponent.state === "INSTALLED"
                        ? "HBASE MASTER"
                        : hostComponent.isActiveMaster === "true"
                        ? "ACTIVE HBASE MASTER"
                        : "STANDBY HBASE MASTER"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row>
          {slaveComponents.map((slaveComponent: any) => {
            return (
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                    Started
                  </h3>
                  <div
                    className="custom-link text-uppercase fs-12 mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/component/${slaveComponent.componentName}`
                      );
                    }}
                  >
                    {pluralize(
                      2,
                      slaveComponent.displayName as string,
                      "s",
                      false
                    )}
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function RANGERSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["ranger"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["ranger"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const rangerModel = allServiceModels["ranger"];
    const masterComponents =
      allServiceModels?.["ranger"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["ranger"]?.slaveComponents || [];
    if (!rangerModel) {
      return <Spinner />;
    }
    const rangerComponents = ServiceComponentsMap["ranger"];
    const groupsWithComponents = cloneDeep(componentCategories["ranger"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = rangerComponents.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "RANGER_ADMIN",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]
                                  ?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"RANGER ADMIN"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {find(masterComponents, [
            "componentName",
            "RANGER_USERSYNC",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
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
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]
                                  ?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"RANGER USERSYNC"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = rangerModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {isObject(metricValue)
                                ? isEmpty(metricValue)
                                  ? "n/a"
                                  : JSON.stringify(metricValue)
                                : metricValue}
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? rangerModel[component.descriptionKey as any]
                                  ? rangerModel[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div className="text-uppercase fs-12 text-light">
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
        <Row>
          {slaveComponents.map((slaveComponent: any) => {
            return (
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                    Started
                  </h3>
                  <div
                    className="custom-link text-uppercase fs-12 mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/component/${slaveComponent.componentName}`
                      );
                    }}
                  >
                    {pluralize(
                      2,
                      slaveComponent.displayName as string,
                      "s",
                      false
                    )}
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function ZOOKEEPERSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["zk"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["zk"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const zkModel = allServiceModels["zk"];
    const masterComponents = allServiceModels?.["zk"]?.masterComponents || [];
    if (!zkModel) {
      return <Spinner />;
    }
    const zkComponents = ServiceComponentsMap["zk"];
    const groupsWithComponents = cloneDeep(componentCategories["zk"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = zkComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "ZOOKEEPER_SERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"ZOOKEEPER SERVER"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = zkModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"} Installed
                            </h3>
                            <div className="d-flex align-items-center">
                              <div className="fs-12 text-light flex-grow-1">
                                {component.descriptionKey
                                  ? zkModel[component.descriptionKey as any]
                                    ? zkModel[component.descriptionKey as any]
                                    : component.description
                                  : component.description}
                              </div>
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                )
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function KYUUBISummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["kyuubi"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["kyuubi"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const kyuubiModel = allServiceModels["kyuubi"];
    const masterComponents =
      allServiceModels?.["kyuubi"]?.masterComponents || [];
    if (!kyuubiModel) {
      return <Spinner />;
    }
    const kyuubiComponents = ServiceComponentsMap["kyuubi"];
    const groupsWithComponents = cloneDeep(componentCategories["kyuubi"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = kyuubiComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "KYUUBI",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"KYUUBI"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = kyuubiModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"}
                            </h3>
                            <div className="d-flex align-items-center">
                              <div className="fs-12 text-light flex-grow-1">
                                {component.descriptionKey
                                  ? kyuubiModel[component.descriptionKey as any]
                                    ? kyuubiModel[
                                        component.descriptionKey as any
                                      ]
                                    : component.description
                                  : component.description}
                              </div>
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                )
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function TRINOGATEWAYSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["trino_gateway"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["trino_gateway"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const trinogatewayModel = allServiceModels["trino_gateway"];
    const masterComponents =
      allServiceModels?.["trino_gateway"]?.masterComponents || [];
    if (!trinogatewayModel) {
      return <Spinner />;
    }
    const trinogatewayComponents = ServiceComponentsMap["trino_gateway"];
    const groupsWithComponents = cloneDeep(componentCategories["trino_gateway"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = trinogatewayComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "TRINO_GATEWAY",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
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
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"TRINO GATEWAY"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = trinogatewayModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"}
                            </h3>
                            <div className="d-flex align-items-center">
                              <div className="fs-12 text-light flex-grow-1">
                                {component.descriptionKey
                                  ? trinogatewayModel[component.descriptionKey as any]
                                    ? trinogatewayModel[
                                        component.descriptionKey as any
                                      ]
                                    : component.description
                                  : component.description}
                              </div>
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                )
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function MAPREDUCE2Summary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(
    allServiceModels?.["mapreduce2"] || {}
  );

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["mapreduce2"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const mr2Model = allServiceModels["mapreduce2"];
    const masterComponents =
      allServiceModels?.["mapreduce2"]?.masterComponents || [];
    if (!mr2Model) {
      return <Spinner />;
    }
    const mr2Components = ServiceComponentsMap["mapreduce2"];
    const groupsWithComponents = cloneDeep(componentCategories["mapreduce2"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = mr2Components?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }


    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "HISTORYSERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"HISTORY SERVER"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = mr2Model[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"} Installed
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? mr2Model[component.descriptionKey as any]
                                  ? mr2Model[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                )
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function TEZSummary() {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["tez"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["tez"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const tezModel = allServiceModels["tez"];
    //const masterComponents = allServiceModels?.["tez"]?.masterComponents || [];
    if (!tezModel) {
      return <Spinner />;
    }
    const tezComponents = ServiceComponentsMap["tez"];
    const groupsWithComponents = cloneDeep(componentCategories["tez"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = tezComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    // function getComponentAlerts(componentName: string) {
    //   console.log("Component Name is", componentName);
    //   const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
    //   const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
    //   const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
    //     (alert) => alert.component_name === componentName
    //   );
    //   return alertsForComponent;
    // }

    return (
      <>
        <Row>
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = tezModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? 0
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : 0} Installed
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? tezModel[component.descriptionKey as any]
                                  ? tezModel[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                metricValue > 0 ?
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                ) : ""
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function KERBEROSSummary() {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["kerberos"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["kerberos"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const kerberosModel = allServiceModels["kerberos"];
    const clientComponents = allServiceModels?.["kerberos"]?.clientComponents || [];
    if (!kerberosModel) {
      return <Spinner />;
    }
    const kerberosComponents = ServiceComponentsMap["kerberos"];
    const groupsWithComponents = cloneDeep(componentCategories["kerberos"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = kerberosComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {clientComponents.map((clientComponent: any) => (
            <Col md={2} key={clientComponent.componentName}>
              <Stack>
                <h3 className="text-dark mb-0">
                  {clientComponent.installedCount} Installed
                </h3>
                <div
                  className="custom-link text-uppercase fs-12 mt-2"
                  onClick={() => {
                    navigate(
                      `/main/hosts/component/${clientComponent.componentName}`
                    );
                  }}
                >
                  {pluralize(
                    2,
                    clientComponent.displayName as string,
                    "s",
                    false
                  )}
                </div>
              </Stack>
            </Col>
          ))}
        </Row>
            </>
    );
  }

  return <>{renderComponents()}</>;
}

function SPARK3Summary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["spark3"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["spark3"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const spark3Model = allServiceModels["spark3"];
    const masterComponents =
      allServiceModels?.["spark3"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["spark3"]?.slaveComponents || [];
    if (!spark3Model) {
      return <Spinner />;
    }
    const spark3Components = ServiceComponentsMap["spark3"];
    const groupsWithComponents = cloneDeep(componentCategories["spark3"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = spark3Components?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "SPARK3_JOBHISTORYSERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"Spark3 History Server"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {slaveComponents.map((slaveComponent: any) => (
            <Col md={2} key={slaveComponent.componentName}>
              <Stack>
                <h3 className="text-dark mb-0">
                  {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                  Started
                </h3>
                <div
                  className="custom-link text-uppercase fs-12 mt-2"
                  onClick={() => {
                    navigate(
                      `/main/hosts/component/${slaveComponent.componentName}`
                    );
                  }}
                >
                  {pluralize(
                    2,
                    slaveComponent.displayName as string,
                    "s",
                    false
                  )}
                </div>
              </Stack>
            </Col>
          ))}

          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => (
              <Col key={group.id} md={12} className="mt-4">
                <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                  {group.display_name}
                </h3>
                <Row className="w-100 mb-4">
                  {group?.components?.map((component) => {
                    const metricValue = spark3Model[component.modelKey];
                    return (
                      <Col md={2} key={component.display_name}>
                        <Stack>
                          <h3 className="text-dark mb-0">
                            {isObject(metricValue)
                              ? isEmpty(metricValue)
                                ? "n/a"
                                : JSON.stringify(metricValue)
                              : metricValue} Installed
                          </h3>
                          <div className="fs-12 text-light">
                            {component.descriptionKey
                              ? spark3Model[component.descriptionKey as any]
                                ? spark3Model[component.descriptionKey as any]
                                : component.description
                              : component.description}
                          </div>
                          <div
                            className="custom-link text-uppercase fs-12"
                            onClick={() =>
                              navigate(
                                `/main/hosts/component/${component.display_name
                                  .split(" ")
                                  .join("_")
                                  .slice(0, -1)}`
                              )
                            }
                          >
                            {component.display_name}
                          </div>
                        </Stack>
                      </Col>
                    );
                  })}
                </Row>
              </Col>
            )
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function RANGER_KMSSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(
    allServiceModels?.["ranger_kms"] || {}
  );

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["ranger_kms"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const zkModel = allServiceModels["ranger_kms"];
    const masterComponents =
      allServiceModels?.["ranger_kms"]?.masterComponents || [];
    if (!zkModel) {
      return <Spinner />;
    }
    const groupsWithComponents = cloneDeep(componentCategories["ranger_kms"]);
    // for (const groupCategory of groupsWithComponents) {
    //   for (const group of groupCategory.groups) {
    //     const matchingServiceComponents = zkComponents?.filter(
    //         (component) =>
    //             component.group_id == group.id &&
    //             component.category === groupCategory.name
    //     );
    //     group.components = matchingServiceComponents;
    //   }
    // }


    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "RANGER_KMS_SERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(alerts, component, hostComponent?.passiveState)?.count > 0 ? (
                      <Badge
                        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
                        onClick={() => {
                          modalManager.show(
                            <AlertsModal
                              alerts={getComponentAlerts(alerts, component, hostComponent?.passiveState).alerts}
                              navigate={navigate}
                              serviceName={
                                getComponentAlerts(alerts, component, hostComponent?.passiveState)?.alerts?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(alerts, component, hostComponent?.passiveState).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"Ranger KMS Server"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = zkModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"}
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? zkModel[component.descriptionKey as any]
                                  ? zkModel[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div className="text-uppercase fs-12 text-light">
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function TRINOSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["trino"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["trino"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const trinoModel = allServiceModels["trino"];
    const masterComponents =
      allServiceModels?.["trino"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["trino"]?.slaveComponents || [];
    const clientComponents = allServiceModels?.["trino"]?.clientComponents || [];
    if (!trinoModel) {
      return <Spinner />;
    }
    const spark3Components = ServiceComponentsMap["trino"];
    const groupsWithComponents = cloneDeep(componentCategories["trino"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = spark3Components?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    function getComponentAlerts(componentName: string) {
      const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
      const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
      const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
        (alert) => alert.component_name === componentName
      );
      return alertsForComponent;
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "TRINO_COORDINATOR",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(component)?.length ? (
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
                                getComponentAlerts(component)?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).length}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"Trino Coordinator"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {slaveComponents.map((slaveComponent: any) => (
            <Col md={2} key={slaveComponent.componentName}>
              <Stack>
                <h3 className="text-dark mb-0">
                  {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                  Live
                </h3>
                <div
                  className="custom-link text-uppercase fs-12 mt-2"
                  onClick={() => {
                    navigate(
                      `/main/hosts/component/${slaveComponent.componentName}`
                    );
                  }}
                >
                  {slaveComponent.displayName}
                </div>
              </Stack>
            </Col>
          ))}
          {clientComponents.map((clientComponent: any) => (
            <Col md={2} key={clientComponent.componentName}>
              <Stack>
                <h3 className="text-dark mb-0">
                  {clientComponent.installedCount} Installed
                </h3>
                <div
                  className="custom-link text-uppercase fs-12 mt-2"
                  onClick={() => {
                    navigate(
                      `/main/hosts/component/${clientComponent.componentName}`
                    );
                  }}
                >
                  {pluralize(
                    2,
                    clientComponent.displayName as string,
                    "s",
                    false
                  )}
                </div>
              </Stack>
            </Col>
          ))}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function SSMSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["ssm"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["ssm"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const ssmModel = allServiceModels["ssm"];
    const masterComponents = allServiceModels?.["ssm"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["ssm"]?.slaveComponents || [];
    if (!ssmModel) {
      return <Spinner />;
    }
    //const ssmComponents = ServiceComponentsMap["ssm"];
    // const groupsWithComponents = cloneDeep(componentCategories["ssm"]);
    // for (const groupCategory of groupsWithComponents) {
    //   for (const group of groupCategory.groups) {
    //     const matchingServiceComponents = ssmComponents.filter(
    //         (component) =>
    //             component.group_id == group.id &&
    //             component.category === groupCategory.name
    //     );
    //     group.components = matchingServiceComponents;
    //   }
    //}

    function getComponentAlerts(componentName: string) {
      const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
      const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
      const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
        (alert) => alert.component_name === componentName
      );
      const criticalCount =
        alertsForComponent?.[0]?.summary?.["CRITICAL"]?.count || 0;
      const warningCount = alertsForComponent?.[0]?.summary?.["WARNING"]?.count || 0;
      return {
        alerts: alertsForComponent,
        count: criticalCount + warningCount,
      };
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "SSM_SERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    {
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      ></FontAwesomeIcon>
                    }
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
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
                                  displayName={getComponentDisplayName(component)}
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
                    {hostComponent.is_active_ssm_ip ? "ACTIVE" : "STANDBY"}{" "}
                    SMART SERVER
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {slaveComponents.map((slaveComponent: any) => {
            if (slaveComponent.componentName === "SSM_AGENT") {
              // return null;
            }
            return (
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                    Live
                  </h3>
                  <div
                    className="custom-link text-uppercase fs-12 mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/component/${slaveComponent.componentName}`
                      );
                    }}
                  >
                    {pluralize(
                      2,
                      slaveComponent.displayName as string,
                      "s",
                      false
                    )}
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function YARNSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["yarn"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["yarn"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const yarnModel = allServiceModels["yarn"];
    const masterComponents = allServiceModels?.["yarn"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["yarn"]?.slaveComponents || [];
    if (!yarnModel) {
      return <Spinner />;
    }
    const yarnComponents = ServiceComponentsMap["yarn"];
    const groupsWithComponents = cloneDeep(componentCategories["yarn"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = yarnComponents.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    function getComponentAlerts(componentName: string) {
      const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
      const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
      const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
        (alert) => alert.component_name === componentName
      );
      const criticalCount =
        alertsForComponent?.[0]?.summary?.["CRITICAL"]?.count;
      const warningCount = alertsForComponent?.[0]?.summary?.["WARNING"]?.count;
      return {
        alerts: alertsForComponent,
        count: criticalCount + warningCount,
      };
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "APP_TIMELINE_SERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
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
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      Timeline Service V1.5
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {find(masterComponents, [
            "componentName",
            "RESOURCEMANAGER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
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
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {masterComponents.filter((mc: any) => mc.componentName === "RESOURCEMANAGER")[0]?.hostComponents?.length > 1 && hostComponent.HostRoles.ha_state
                        ? `${hostComponent.HostRoles.ha_state.toUpperCase()} `
                        : ""}RESOURCEMANAGER
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {find(masterComponents, [
            "componentName",
            "YARN_REGISTRY_DNS",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
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
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {hostComponent.haStatus} YARN Registry DNS
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          <Stack direction={"horizontal"} className="align-items-center">
            {slaveComponents.map((slaveComponent: any) => {
              return (
                <Col md={2}>
                  <Stack>
                    <h3 className="text-dark mb-0">
                      {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                      Started
                    </h3>
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/component/${slaveComponent.componentName}`
                        );
                      }}
                    >
                      {pluralize(
                        2,
                        slaveComponent.displayName as string,
                        "s",
                        false
                      )}
                    </div>
                  </Stack>
                </Col>
              );
            })}
            {find(groupsWithComponents, ["name", "CLIENT"])?.groups.map(
              (group) => {
                return (
                  <>
                    <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                      {group.display_name}
                    </h3>
                    <Row key={group.id} className="w-100 mb-4">
                      {group?.components?.map((component) => {
                        const metricValue = yarnModel[component.modelKey];
                        return (
                          <Col md={2} key={component.display_name}>
                            <Stack>
                              <h3 className="text-dark mb-0">
                                {isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue} Installed
                              </h3>
                              <div className="fs-12 text-light">
                                {component.descriptionKey
                                  ? yarnModel[component.descriptionKey as any]
                                    ? yarnModel[component.descriptionKey as any]
                                    : component.description
                                  : component.description}
                              </div>
                              <div
                                className="custom-link text-uppercase fs-12"
                                onClick={() =>
                                  navigate(
                                    `/main/hosts/component/${component.display_name
                                      .split(" ")
                                      .join("_")
                                      .slice(0, -1)}`
                                  )
                                }
                              >
                                {component.display_name}
                              </div>
                            </Stack>
                          </Col>
                        );
                      })}
                    </Row>
                  </>
                );
              }
            )}
          </Stack>
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function HIVESummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["hive"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["hive"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const hiveModel = allServiceModels["hive"];
    const masterComponents = allServiceModels?.["hive"]?.masterComponents || [];
    if (!hiveModel) {
      return <Spinner />;
    }
    const hiveComponents = ServiceComponentsMap["hive"];
    const groupsWithComponents = cloneDeep(componentCategories["hive"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = hiveComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    function getComponentAlerts(componentName: string) {
      const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
      const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
      const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
        (alert) => alert.component_name === componentName
      );
      return alertsForComponent;
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "HIVE_METASTORE",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(component)?.length ? (
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
                                getComponentAlerts(component)?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).length}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"HIVE METASTORE"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
          {find(masterComponents, [
            "componentName",
            "HIVE_SERVER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
                    {getComponentAlerts(component)?.length ? (
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
                                getComponentAlerts(component)?.[0]?.service_name
                              }
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).length}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      {"HIVESERVER2"}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {find(groupsWithComponents, ["name", "CLIENT"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = hiveModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? "n/a"
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : "n/a"} Installed
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? hiveModel[component.descriptionKey as any]
                                  ? hiveModel[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                )
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function SQOOPSummary() {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["sqoop"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["sqoop"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const sqoopModel = allServiceModels["sqoop"];
    if (!sqoopModel) {
      return <Spinner />;
    }
    const sqoopComponents = ServiceComponentsMap["sqoop"];
    const groupsWithComponents = cloneDeep(componentCategories["sqoop"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = sqoopComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    return (
      <>
        <Row>
          {find(groupsWithComponents, ["name", "MASTER"])?.groups.map(
            (group) => {
              return (
                <>
                  <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light mb-0">
                    {group.display_name}
                  </h3>
                  <Row key={group.id} className="w-100 mb-4">
                    {group?.components?.map((component) => {
                      const metricValue = sqoopModel[component.modelKey];
                      return (
                        <Col md={2} key={component.display_name}>
                          <Stack>
                            <h3 className="text-dark mb-0">
                              {metricValue
                                ? isObject(metricValue)
                                  ? isEmpty(metricValue)
                                    ? 0
                                    : JSON.stringify(metricValue)
                                  : metricValue
                                : 0} Installed
                            </h3>
                            <div className="fs-12 text-light">
                              {component.descriptionKey
                                ? sqoopModel[component.descriptionKey as any]
                                  ? sqoopModel[component.descriptionKey as any]
                                  : component.description
                                : component.description}
                            </div>
                            <div
                              className="custom-link text-uppercase fs-12"
                              onClick={() =>
                                metricValue > 0 ? 
                                navigate(
                                  `/main/hosts/component/${component.display_name
                                    .split(" ")
                                    .join("_")
                                    .slice(0, -1)}`
                                ) : ""
                              }
                            >
                              {component.display_name}
                            </div>
                          </Stack>
                        </Col>
                      );
                    })}
                  </Row>
                </>
              );
            }
          )}
        </Row>
      </>
    );
  }

  return <>{renderComponents()}</>;
}

function PINOTSummary({ alerts }: { alerts: any }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(allServiceModels?.["pinot"] || {});

  const navigate = useNavigate();

  useEffect(() => {
    if (allServiceModels["pinot"]) {
      renderComponents();
    }
  }, [stringifiedModel]);

  function renderComponents() {
    const pinotModel = allServiceModels["pinot"];
    const masterComponents =
      allServiceModels?.["pinot"]?.masterComponents || [];
    const slaveComponents = allServiceModels?.["pinot"]?.slaveComponents || [];
    if (!pinotModel) {
      return <Spinner />;
    }
    const pinotComponents = ServiceComponentsMap["pinot"];
    const groupsWithComponents = cloneDeep(componentCategories["pinot"]);
    for (const groupCategory of groupsWithComponents) {
      for (const group of groupCategory.groups) {
        const matchingServiceComponents = pinotComponents?.filter(
          (component) =>
            component.group_id == group.id &&
            component.category === groupCategory.name
        );
        group.components = matchingServiceComponents;
      }
    }

    function getComponentAlerts(componentName: string) {
      const criticalAlerts = filter(alerts, ["highestStatus", "CRITICAL"]);
      const warningAlerts = filter(alerts, ["highestStatus", "WARNING"]);
      const alertsForComponent = [...criticalAlerts, ...warningAlerts].filter(
        (alert) => alert.component_name === componentName
      );
      const criticalCount =
        alertsForComponent?.[0]?.summary?.["CRITICAL"]?.count;
      const warningCount = alertsForComponent?.[0]?.summary?.["WARNING"]?.count;
      return {
        alerts: alertsForComponent,
        count: criticalCount + warningCount,
      };
    }

    return (
      <>
        <Row>
          {find(masterComponents, [
            "componentName",
            "PINOT_CONTROLLER",
          ])?.hostComponents?.map((hostComponent: any) => {
            const component = hostComponent.HostRoles.component_name;
            const icon =
              hostComponent.passiveState == "OFF"
                ? statusIconMap[lowerCase(hostComponent?.state)]
                : hostComponent?.passiveState
                ? statusIconMap["Maintenance"]
                : null;
            return (
              <Col md={2}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    <Tooltip
                      message={hostComponent?.passiveState ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH}
                      heading="Component Status"
                      placement="top"
                    >
                      <FontAwesomeIcon
                        icon={icon?.icon}
                        className={`me-1 fw-bold fs-12 text-${icon?.color}`}
                      />
                    </Tooltip>
                    <h3 className="text-dark mb-0">
                      {startCase(hostComponent?.state?.toLowerCase()) ===
                      "Installed"
                        ? "Stopped"
                        : startCase(hostComponent?.state?.toLowerCase())}
                    </h3>
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
                              displayName={getComponentDisplayName(component)}
                            />
                          );
                        }}
                      >
                        {getComponentAlerts(component).count}
                      </Badge>
                    ) : null}
                  </Stack>
                  <Tooltip
                    message={hostComponent.HostRoles.host_name}
                    placement="top"
                  >
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() => {
                        navigate(
                          `/main/hosts/${hostComponent.HostRoles.host_name}/summary`
                        );
                      }}
                    >
                      PINOT CONTROLLER
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          })}
        </Row>
        <Row className="mt-4">
          {slaveComponents.map((slaveComponent: any) => {
            return (
              <Col md={2}>
                <Stack>
                  <h3 className="text-dark mb-0">
                    {slaveComponent.startedCount}/{slaveComponent.totalCount}{" "}
                    Live
                  </h3>
                  <div
                    className="custom-link text-uppercase fs-12 mt-2"
                    onClick={() => {
                      navigate(
                        `/main/hosts/component/${slaveComponent.componentName}`
                      );
                    }}
                  >
                    {slaveComponent.displayName}
                  </div>
                </Stack>
              </Col>
            );
          })}
        </Row>
      </>
    );
  }
  return <>{renderComponents()}</>;
}

function ServiceComponents({
  serviceName,
  alerts,
}: {
  serviceName: string;
  alerts: any;
}) {
  function renderComponents() {
    switch (serviceName?.toLowerCase()) {
      case "hdfs":
        return <HDFSSummary alerts={alerts} />;
      case "hbase":
        return <HBASESummary alerts={alerts} />;
      case "ranger":
        return <RANGERSummary alerts={alerts} />;
      case "zookeeper":
        return <ZOOKEEPERSummary alerts={alerts} />;
      case "mapreduce2":
        return <MAPREDUCE2Summary alerts={alerts} />;
      case "tez":
        return <TEZSummary />;
      case "kerberos":
        return <KERBEROSSummary />;
      case "spark3":
        return <SPARK3Summary alerts={alerts} />;
      case "ranger_kms":
        return <RANGER_KMSSummary alerts={alerts} />;
      case "trino":
        return <TRINOSummary alerts={alerts} />;
      case "ssm":
        return <SSMSummary alerts={alerts} />;
      case "yarn":
        return <YARNSummary alerts={alerts} />;
      case "hive":
        return <HIVESummary alerts={alerts} />;
      case "sqoop":
        return <SQOOPSummary />;
      case "kyuubi":
        return <KYUUBISummary alerts={alerts} />;
      case "trino_gateway":
        return <TRINOGATEWAYSummary alerts={alerts} />;
      case "pinot":
        return <PINOTSummary alerts={alerts} />;
      case "flume":
        return <FlumeSummary />;

      default:
        return (
          <GenericServiceSummary serviceName={serviceName} alerts={alerts} />
        );
    }
  }
  return (
    <Row className="mt-4">
      <Col md={2}>
        <h3 className="text-light">Components</h3>
      </Col>
      <Col>{renderComponents()}</Col>
    </Row>
  );
}

export function GenericServiceSummary({
  serviceName,
  alerts,
}: {
  serviceName: string;
  alerts: any[];
}) {
  const { masterSlaveClientsData } = useContext(ServiceContext);
  const navigate = useNavigate();
  const components = Array.isArray(masterSlaveClientsData)
    ? masterSlaveClientsData.filter(
        (item: any) =>
          item.ServiceComponentInfo?.service_name === serviceName.toUpperCase()
      )
    : [];

  if (!components.length) {
    return <div className="text-light">No components to display</div>;
  }

  const renderAlertBadge = (
    componentName: string,
    displayName: string,
    maintenanceState = "OFF"
  ) => {
    const componentAlerts = getComponentAlerts(
      alerts,
      componentName,
      maintenanceState
    );
    if (!componentAlerts.count) {
      return null;
    }

    return (
      <Badge
        className="rounded-5 bg-danger cursor-pointer ms-2 fs-10"
        onClick={() => {
          modalManager.show(
            <AlertsModal
              alerts={componentAlerts.alerts}
              navigate={navigate}
              serviceName={serviceName.toUpperCase()}
              displayName={displayName}
            />
          );
        }}
      >
        {componentAlerts.count}
      </Badge>
    );
  };

  return (
    <Row className="g-4">
      {components.flatMap((component: any) => {
        const info = component.ServiceComponentInfo || {};
        const componentName = info.component_name || "";
        const displayName = info.display_name || startCase(lowerCase(componentName));
        const hostComponents = component.host_components || [];

        if (info.category === "MASTER") {
          return hostComponents.map((hostComponent: any) => {
            const hostRoles = hostComponent.HostRoles || {};
            const state = hostRoles.state || "UNKNOWN";
            const maintenanceState = hostRoles.maintenance_state || "OFF";
            const inMaintenance = maintenanceState !== "OFF";
            const icon = inMaintenance
              ? statusIconMap["Maintenance"]
              : statusIconMap[lowerCase(state)];
            const statusLabel =
              startCase(state.toLowerCase()) === "Installed"
                ? "Stopped"
                : startCase(state.toLowerCase());

            return (
              <Col md={3} key={`${componentName}-${hostRoles.host_name}`}>
                <Stack>
                  <Stack direction="horizontal" className="align-items-center">
                    {icon ? (
                      <Tooltip
                        message={
                          inMaintenance
                            ? TOOLTIP_MESSAGES.GENERAL.MAINTENANCE_MODE
                            : TOOLTIP_MESSAGES.GENERAL.COMPONENT_HEALTH
                        }
                        heading="Component Status"
                        placement="top"
                      >
                        <FontAwesomeIcon
                          icon={icon.icon}
                          className={`me-1 fw-bold fs-12 text-${
                            icon.color || "secondary"
                          }`}
                        />
                      </Tooltip>
                    ) : null}
                    <h3 className="text-dark mb-0">{statusLabel}</h3>
                    {renderAlertBadge(
                      componentName,
                      displayName,
                      maintenanceState
                    )}
                  </Stack>
                  <Tooltip message={hostRoles.host_name} placement="top">
                    <div
                      className="custom-link text-uppercase fs-12 mt-2"
                      onClick={() =>
                        navigate(
                          `/main/hosts/${encodeURIComponent(
                            hostRoles.host_name
                          )}/summary`
                        )
                      }
                    >
                      {displayName}
                    </div>
                  </Tooltip>
                </Stack>
              </Col>
            );
          });
        }

        const totalCount = Number(info.total_count) || hostComponents.length;
        const isClient = info.category === "CLIENT";
        const count = isClient
          ? Number(info.installed_count) || 0
          : Number(info.started_count) || 0;

        return [
          <Col md={3} key={componentName}>
            <Stack>
              <Stack direction="horizontal" className="align-items-center">
                <h3 className="text-dark mb-0">
                  {isClient ? `${count} Installed` : `${count}/${totalCount} Live`}
                </h3>
                {renderAlertBadge(componentName, displayName)}
              </Stack>
              <div
                className="custom-link text-uppercase fs-12 mt-2"
                onClick={() =>
                  navigate(
                    `/main/hosts/component/${encodeURIComponent(componentName)}`
                  )
                }
              >
                {pluralize(totalCount, displayName, "s", false)}
              </div>
            </Stack>
          </Col>,
        ];
      })}
    </Row>
  );
}

export default ServiceComponents;
