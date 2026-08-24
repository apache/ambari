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
  faAdd,
  faChevronDown,
  faChevronRight,
  faDownload,
  faEllipsisH,
  faPlay,
  faRefresh,
  faStop,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { MouseEventHandler, useContext } from "react";
import { Dropdown, DropdownButton } from "react-bootstrap";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  downloadClientConfigsCall,
  restartAllRequired,
  startAllServices,
  stopAllServices,
} from "../../Utils/taskUtils";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";
import showBackgroundModal from "../../Utils/showBg";
import modalManager from "../../store/ModalManager";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import { serviceNameModelMapping } from "../../constants";
import RunAllServiceCheck from "./RunAllServiceCheck";
import { checkNnLastCheckpointTime } from "../../screens/Hosts/actions";
import { get } from "lodash";

// interface SidebarElement {
//   id: string;
//   path: string;
//   className?: string;
//   style?: React.CSSProperties;
//   icon?: React.ReactNode;
//   name: React.ReactNode;
//   children?: Array<SidebarElement>;
//   sideItems?: React.ReactNode;
// }

interface SidebarItemProps {
  ele: any;
  onClick?: MouseEventHandler<HTMLDivElement>;
  isSelected: boolean;
  isOpen?: boolean;
  hasChildren?: boolean;
}

type NameNodeHostComponent = {
  HostRoles?: { host_name?: string; state?: string };
  hostName?: string;
  state?: string;
};

type NameNodeComponent = {
  componentName?: string;
  hostComponents?: NameNodeHostComponent[];
};

const SidebarItem = ({
  ele,
  onClick,
  isSelected,
  isOpen = false,
  hasChildren = false,
}: SidebarItemProps) => {
  const navigate = useNavigate();
  const {
    clusterName,
    services: contextServices,
    supports,
    runningOperationsCount,
  } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const location = useLocation();

  // Authorization hooks - implementing Ember.js service menu authorization patterns
  const { havePermissions, isAuthorized } = useAuthorizationPolicy();

  // Check specific authorizations for service operations
  const canAddDeleteServices = isAuthorized("SERVICE.ADD_DELETE_SERVICES");
  const canStartStopServices = isAuthorized("SERVICE.START_STOP");
  const canDownloadConfigs = havePermissions(
    "SERVICE.VIEW_CONFIGS, CLUSTER.VIEW_CONFIGS",
  );

  // Check service states for conditional button enabling/disabling
  // Following Ember.js pattern where buttons are enabled based on actual service states
  const getServiceStates = () => {
    if (!contextServices || !allServiceModels) {
      return {
        hasStoppedServices: false,
        hasStartedServices: false,
        hasServicesRequiringRestart: false,
        allServicesStarted: false,
      };
    }

    let hasStoppedServices = false;
    let hasStartedServices = false;
    let hasServicesRequiringRestart = false;
    contextServices.forEach((service: any) => {
      const serviceName = service.ServiceInfo?.service_name;
      const serviceModel =
        allServiceModels?.[serviceNameModelMapping[serviceName]];
      const serviceState = serviceModel?.serviceState;
      const healthStatus = serviceModel?.healthStatus;

      const isClientOnlyService = serviceModel?.isClientOnlyService;

      // Following Ember.js logic: check for red status (stopped/installed services)
      // Ember: stoppedServices = content.filter(_service => _service.get('healthStatus') === 'red')
      if (!isClientOnlyService && (healthStatus === "red" ||
          serviceState === "INSTALLED" ||
          serviceState === "INIT" ||
          serviceState === "INSTALL_FAILED" ||
          serviceState === "STOPPED")) {
        hasStoppedServices = true;
      }

      // Following Ember.js logic: check for green status (started services)
      // Ember: !content.someProperty('healthStatus', 'green')
      if (!isClientOnlyService &&
          (healthStatus === "green" || serviceState === "STARTED")) {
        hasStartedServices = true;
      }

      if (allServiceModels?.[serviceNameModelMapping[serviceName]]?.isRestartRequiredForService) {
        hasServicesRequiringRestart = true;
      }
    });

    return {
      hasStoppedServices,
      hasStartedServices,
      hasServicesRequiringRestart,
    };
  };

  const {
    hasStoppedServices,
    hasStartedServices,
    hasServicesRequiringRestart,
  } = getServiceStates();

  const canAddService = canAddDeleteServices && supports.enableAddDeleteServices;
  const isStartStopBusy = runningOperationsCount > 0;

  const hasAnyServiceOperationPermissions = canAddDeleteServices || canStartStopServices;

  const executeAllServicesAction = async (
    label: string,
    action: () => Promise<unknown>
  ) => {
    try {
      await action();
      showBackgroundModal();
    } catch (error) {
      const message = get(
        error,
        "response.data.message",
        get(error, "message", `${label} could not be submitted.`)
      );
      modalManager.show({
        modalTitle: `${label} Failed`,
        modalBody: message,
        successCallback: () => {
          modalManager.hide();
          void executeAllServicesAction(label, action);
        },
        onClose: () => modalManager.hide(),
        options: { okButtonText: "RETRY" },
      });
    }
  };

  const confirmAllServicesAction = (
    label: string,
    message: string,
    action: () => Promise<unknown>
  ) => {
    modalManager.show({
      modalTitle: "Confirmation",
      modalBody: message,
      successCallback: () => {
        modalManager.hide();
        void executeAllServicesAction(label, action);
      },
      onClose: () => modalManager.hide(),
      options: { okButtonText: `CONFIRM ${label.toUpperCase()}` },
    });
  };

  const confirmStopAllServices = () => {
    const showConfirmation = () =>
      confirmAllServicesAction(
        "Stop All",
        "You are about to stop all services.",
        () => stopAllServices(clusterName)
      );
    const hdfsMasterComponents = get(
      allServiceModels,
      "hdfs.masterComponents",
      []
    ) as NameNodeComponent[];
    const nameNodeHost = hdfsMasterComponents
      .find((component) => component.componentName === "NAMENODE")
      ?.hostComponents?.find(
        (hostComponent) =>
          get(hostComponent, "HostRoles.state", hostComponent.state) ===
          "STARTED"
      );
    const hostName = get(
      nameNodeHost,
      "HostRoles.host_name",
      nameNodeHost?.hostName
    );

    if (hostName) {
      void checkNnLastCheckpointTime(showConfirmation, hostName, clusterName);
    } else {
      showConfirmation();
    }
  };
  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    // If this is the Cluster Admin item and it's currently closed, navigate to the first admin route
    // Only navigate when opening the menu, not when closing it, and only if not already on an admin page
    if (ele.id === "cluster_admin" && ele.sideItems && !isOpen && !location.pathname.startsWith("/main/admin")) {
      e.preventDefault();
      navigate("/main/admin");
    }
    // Call the original onClick handler
    if (onClick) {
      onClick(e);
    }
  };

  return (
    <Link
      to={ele.sideItems ? location.pathname : ele.path}
      className="text-decoration-none"
    >
      <div
        className={`d-flex justify-content-between ${
          ele.className
        } sideitem align-items-center ${isSelected ? "selected-item" : ""}`}
        style={{
          ...(ele.style as any),
          fontSize: 14,
          cursor: "pointer",
          padding: "10px 5px 10px 20px",
          position: "relative",
        }}
        onClick={handleClick}
      >
        <div className="d-flex">
          <div>{ele.icon}</div>
          <div className="ms-2">{ele.name}</div>
        </div>
        <div className="d-flex align-items-center">
          {ele.sideItems &&
          ele.id === "services" &&
          hasAnyServiceOperationPermissions ? (
            <Dropdown>
              <DropdownButton
                variant="transparent"
                onClick={(e) => {
                  e.stopPropagation();
                }}
                className="service-dropdown"
                title={
                  <FontAwesomeIcon
                    className="text-light me-1"
                    icon={faEllipsisH}
                  />
                }
              >
                {canAddService && (
                  <Dropdown.Item
                    onClick={() => {
                      navigate("/main/service/add/step1");
                    }}
                  >
                    <FontAwesomeIcon icon={faAdd} className="me-1" />
                    Add Service
                  </Dropdown.Item>
                )}

                {canStartStopServices && (
                  <Dropdown.Item
                    disabled={!hasStoppedServices || isStartStopBusy}
                    onClick={() => {
                      if (hasStoppedServices && !isStartStopBusy) {
                        confirmAllServicesAction(
                          "Start All",
                          "You are about to start all services.",
                          () => startAllServices(clusterName)
                        );
                      }
                    }}
                  >
                    <FontAwesomeIcon icon={faPlay} className="me-1" />
                    Start All
                  </Dropdown.Item>
                )}

                {canStartStopServices && (
                  <Dropdown.Item
                    disabled={!hasStartedServices || isStartStopBusy}
                    onClick={() => {
                      if (hasStartedServices && !isStartStopBusy) {
                        confirmStopAllServices();
                      }
                    }}
                  >
                    <FontAwesomeIcon icon={faStop} className="me-1" />
                    Stop All
                  </Dropdown.Item>
                )}

                {/* Restart All Required - Disabled when no services require restart (following Ember.js isRestartAllRequiredDisabled logic) */}
                {canStartStopServices && (
                  <Dropdown.Item
                    disabled={!hasServicesRequiringRestart}
                    onClick={() => {
                      if (hasServicesRequiringRestart) {
                        modalManager.show({
                          modalTitle: "Confirmation",
                          modalBody:
                            "This will trigger alerts as the service is restarted. To suppress alerts, turn on Maintenance Mode for services listed above prior to running Restart All Required",
                          successCallback: () => {
                            modalManager.hide();
                            void executeAllServicesAction(
                              "Restart All Required",
                              () => restartAllRequired(clusterName)
                            );
                          },
                          onClose: () => {
                            modalManager.hide();
                          },
                          options: {},
                        });
                      }
                    }}
                  >
                    <FontAwesomeIcon icon={faRefresh} className="me-1" />
                    Restart All required
                  </Dropdown.Item>
                )}

                {/* Download All Client Configs - Requires config view permissions */}
                {canDownloadConfigs && (
                  <Dropdown.Item
                    onClick={() => {
                      downloadClientConfigsCall({}, clusterName);
                    }}
                  >
                    <FontAwesomeIcon icon={faDownload} className="me-1" />
                    Download All Client Configs
                  </Dropdown.Item>
                )}
                {/* Run All Service Check - Requires SERVICE.RUN_CUSTOM_COMMAND, SERVICE.RUN_SERVICE_CHECK, SERVICE.TOGGLE_MAINTENANCE, or SERVICE.ENABLE_HA */}
                <RunAllServiceCheck />
              </DropdownButton>
            </Dropdown>
          ) : null}
          {hasChildren ? (
            <>
              <FontAwesomeIcon
                icon={isOpen ? faChevronRight : faChevronDown}
                className="me-1"
              />
            </>
          ) : null}
        </div>
      </div>
    </Link>
  );
};

export default SidebarItem;
