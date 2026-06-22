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
import { useAuth } from "../../hooks/useAuth";
import { serviceNameModelMapping } from "../../constants";
import RunAllServiceCheck from "./RunAllServiceCheck";

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
    upgradeIsRunning,
    upgradeSuspended,
    services: contextServices,
  } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const location = useLocation();

  // Authorization hooks - implementing Ember.js service menu authorization patterns
  const { hasAuthorization } = useAuth();

  // Check specific authorizations for service operations
  const canAddDeleteServices = hasAuthorization("SERVICE.ADD_DELETE_SERVICES");
  const canStartStopServices = hasAuthorization("SERVICE.START_STOP");
  const canDownloadConfigs =
    hasAuthorization("SERVICE.VIEW_CONFIGS") ||
    hasAuthorization("CLUSTER.VIEW_CONFIGS");

  // Check if upgrade is blocking operations (matches Ember.js logic)
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

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
    let totalServices = 0;
    let startedServicesCount = 0;
    let stoppedServicesCount = 0;

    contextServices.forEach((service: any) => {
      const serviceName = service.ServiceInfo?.service_name;
      const serviceState =
        allServiceModels?.[serviceNameModelMapping[serviceName]]?.serviceState;

      // Count total services (excluding client-only services for start/stop operations)
      const isClientOnlyService = allServiceModels?.[serviceNameModelMapping[serviceName]]?.isClientOnlyService;
      if (!isClientOnlyService) {
        totalServices++;
      }

      // Following Ember.js logic: check for red status (stopped/installed services)
      // Ember: stoppedServices = content.filter(_service => _service.get('healthStatus') === 'red')
      if (
        serviceState === "INSTALLED" ||
        serviceState === "INIT" ||
        serviceState === "INSTALL_FAILED" ||
        serviceState === "STOPPED"
      ) {
        hasStoppedServices = true;
        if (!isClientOnlyService) {
          stoppedServicesCount++;
        }
      }

      // Following Ember.js logic: check for green status (started services)
      // Ember: !content.someProperty('healthStatus', 'green')
      if (serviceState === "STARTED") {
        hasStartedServices = true;
        if (!isClientOnlyService) {
          startedServicesCount++;
        }
      }

      if (allServiceModels?.[serviceNameModelMapping[serviceName]]?.isRestartRequiredForService) {
        hasServicesRequiringRestart = true;
      }
    });

    // Check if ALL non-client-only services are started
    const allServicesStarted = totalServices > 0 && startedServicesCount === totalServices;
    
    // Check if ALL non-client-only services are stopped
    const allServicesStopped = totalServices > 0 && stoppedServicesCount === totalServices;

    return {
      hasStoppedServices,
      hasStartedServices,
      hasServicesRequiringRestart,
      allServicesStarted,
      allServicesStopped,
    };
  };

  const {
    hasServicesRequiringRestart,
    allServicesStarted,
    allServicesStopped,
  } = getServiceStates();

  // Check if user has any service operation permissions and upgrade is not blocking
  const hasAnyServiceOperationPermissions =
    (canAddDeleteServices || canStartStopServices) && !isUpgradeBlocking;
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
                {/* Add Service - Requires SERVICE.ADD_DELETE_SERVICES authorization and no upgrade blocking */}
                {canAddDeleteServices && !isUpgradeBlocking && (
                  <Dropdown.Item
                    onClick={() => {
                      navigate("/main/service/add/step1");
                    }}
                  >
                    <FontAwesomeIcon icon={faAdd} className="me-1" />
                    Add Service
                  </Dropdown.Item>
                )}

                {/* Start All - Disabled when all services are started (TLHASD-997 fix) */}
                {canStartStopServices && (
                  <Dropdown.Item
                    disabled={allServicesStarted}
                    onClick={async () => {
                      if (!allServicesStarted) {
                        await startAllServices(clusterName);
                        showBackgroundModal();
                      }
                    }}
                  >
                    <FontAwesomeIcon icon={faPlay} className="me-1" />
                    Start All
                  </Dropdown.Item>
                )}

                {/* Stop All - Disabled when all services are stopped (TLHASD-997 consistency fix) */}
                {canStartStopServices && (
                  <Dropdown.Item
                    disabled={allServicesStopped}
                    onClick={async () => {
                      if (!allServicesStopped) {
                        await stopAllServices(clusterName);
                        showBackgroundModal();
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
                    onClick={async () => {
                      if (hasServicesRequiringRestart) {
                        modalManager.show({
                          modalTitle: "Confirmation",
                          modalBody:
                            "This will trigger alerts as the service is restarted. To suppress alerts, turn on Maintenance Mode for services listed above prior to running Restart All Required",
                          successCallback: async () => {
                            modalManager.hide();
                            await restartAllRequired(clusterName);
                            showBackgroundModal();
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
