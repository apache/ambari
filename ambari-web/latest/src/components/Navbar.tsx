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

import { useContext, useState, useEffect } from "react";
import UserSettingsModal from "./UserSettingsModal";
import {
  Container,
  Navbar,
  Nav,
  Dropdown,
  DropdownDivider,
  Badge,
} from "react-bootstrap";
import NotificationDropdown from "./NotificationDropdown";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faHome,
  faCog,
  faUser,
  faThLarge,
  faMedkit,
} from "@fortawesome/free-solid-svg-icons";
import { AppContext } from "../store/context.tsx";
import { Notifications } from "../screens/Alerts/types";
import { useLocation, useNavigate } from "react-router-dom";
import AmbariAboutModal from "../AmbariAboutModal.tsx";
import "../styles/app.scss";
import usePolling from "../hooks/usePolling.ts";
import { AlertsApi } from "../api/alertsApi.ts";
import { redirectToAdminView } from "../Utils/adminViewRedirect";
import modalManager from "../store/ModalManager.ts";
import BackgroundOperations from "../screens/BackgroundOperations/index.tsx";
import { db } from "../Utils/db";
import Cookies from "universal-cookie";
import { useCallback } from "react";
import { isString } from "lodash";
import { useAuth } from "../hooks/useAuth";

type NavbarOption = {
  label: string;
  callback: () => void;
};

// type SignOutResponse = {
//   status: number;
// };

type NavBarProps = {
  subPath: string;
  viewsList: any[];
  runningRequestsCount?: number;
  hostMaintenanceState?: string;
  hostname?: string;
};

export default function NavBar({
  subPath,
  viewsList,
  runningRequestsCount,
  hostMaintenanceState,
  hostname,
}: NavBarProps) {
  const [notifications, setNotifications] = useState<Notifications[]>([]);
  const [showUserSettingsModal, setShowUserSettingsModal] = useState(false);
  const [filteredNotifications, setFilteredNotifications] = useState<
    Notifications[]
  >([]);
  const [alertCounts, setAlertCounts] = useState({
    all: 0,
    critical: 0,
    warning: 0,
  });
  const location = useLocation();
  const localStorageData = db.getItem("ambari");
  let login = "";
  if (localStorageData) {
    let parsedData = {};
    try {
      parsedData = JSON.parse(localStorageData || "{}");
      if (isString(parsedData)) {
        parsedData = JSON.parse(parsedData);
      }
    } catch (err) {
      console.log("Error parsing ambari data", err);
      parsedData = {};
    }

    let ambari: any = parsedData;
    if (ambari?.app?.loginName) {
      login = ambari.app.loginName;
      // If we already have a username, we can consider the user authenticated
    }
  }

  const [selectedFilter, setSelectedFilter] = useState("all");
  const { clusterName, cluster } = useContext(AppContext);
  const [showAmbariAboutModal, setShowAmbariAboutModal] = useState(false);
  
  const isClusterInstalled = cluster?.provisioning_state === "INSTALLED";
  const navigate = useNavigate();
  const cookies = new Cookies();

  const isInstaller = () => {
    const path = location.pathname;
    if (path.includes("installer") || path.includes("install")) {
      return true;
    }
    return false;
  };

  const fetchAlerts = async () => {
    // TLHASD-745: Only fetch alerts if cluster is installed
    if (!clusterName) {
      pausePolling(); // Pause polling if clusterName is not available
      return;
    } else {
      resumePolling(); // Resume polling if clusterName is available
    }
    
    if (!isClusterInstalled) {
      pausePolling(); // Pause polling if cluster is not installed
      return;
    }
    
    resumePolling(); // Resume polling if clusterName is available and cluster is installed
    
    try {
      const fields =
        "Alert/component_name,Alert/definition_id,Alert/definition_name,Alert/host_name,Alert/id,Alert/instance,Alert/label,Alert/latest_timestamp,Alert/maintenance_state,Alert/original_timestamp,Alert/scope,Alert/service_name,Alert/state,Alert/text,Alert/repeat_tolerance,Alert/repeat_tolerance_remaining&Alert/state.in(CRITICAL,WARNING)&Alert/maintenance_state.in(OFF)&from=0&page_size=100";
      const time = Date.now();
      const data = await AlertsApi.getAlertsNotifications(
        clusterName,
        fields,
        time
      );
      setNotifications(data.items);
      calculateAlertCounts(data.items);
    } catch (error) {
      console.error("Error fetching alerts:", error);
    }
  };

  const { stopPolling, pausePolling, resumePolling } = usePolling(
    fetchAlerts,
    30000
  );

  useEffect(() => {
    if (clusterName && isClusterInstalled) {
      fetchAlerts();
    }
  }, [clusterName, isClusterInstalled]);

  useEffect(() => {
    filterNotifications();
  }, [selectedFilter, notifications]);

  const calculateAlertCounts = (alerts: Notifications[]) => {
    const counts = { all: alerts.length, critical: 0, warning: 0 };
    alerts.forEach((alert) => {
      if (alert.Alert.state === "CRITICAL") counts.critical++;
      if (alert.Alert.state === "WARNING") counts.warning++;
    });
    setAlertCounts(counts);
  };

  useEffect(() => {
    filterNotifications();
  }, [selectedFilter, notifications]);

  const filterNotifications = () => {
    if (selectedFilter === "all") {
      setFilteredNotifications(notifications);
    } else {
      setFilteredNotifications(
        notifications.filter(
          (notification) =>
            notification.Alert.state === selectedFilter.toUpperCase()
        )
      );
    }
  };

  const getViewsLength = () => {
    return viewsList?.length;
  };

  const logOffBeforeSend = useCallback((headers: Record<string, string>) => {
    headers["Authorization"] = "";
    return headers;
  }, []);

  const handleSignOut = useCallback(async () => {
    try {
      stopPolling();
      db.cleanUp();
      db.setItem("ambari", JSON.stringify(db.getInitialData()));

      localStorage.removeItem("ambari");

      sessionStorage.clear();

      cookies.remove("AMBARISESSIONID", {
        path: "/", // Cookie is set with path '/'
        domain: "localhost", // Cookie is set for localhost
        secure: true, // Cookie has secure flag
        sameSite: "lax", // Default SameSite value
      });

      const response = await fetch("/logout", {
        method: "GET",
        headers: logOffBeforeSend({
          "Content-Type": "text/plain",
        }),
      });

      if (response.ok) {
        window.location.href = "/#/login";
      } else {
        window.location.href = "/#/login";
      }
    } catch (error) {
      window.location.href = "/#/login";
    }
  }, [navigate, stopPolling]);

  // Authorization hooks - implementing Ember.js showSettingsPopup authorization pattern
  const { isAdmin } = useAuth();
  const { upgradeIsRunning, upgradeSuspended } = useContext(AppContext);

  // Check if upgrade is blocking operations (running but not suspended)
  // FIXED: Add additional check for upgrade suspended state to prevent flaky behavior
  // When upgrade is suspended/paused, admin options should be available
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

  const navbarOptions: NavbarOption[] = [
    {
      label: "About",
      callback: () => setShowAmbariAboutModal(true),
    },
    // FIXED: Consolidate admin checks to eliminate code duplication (AI code review suggestion)
    // Admin options should be available when upgrade is suspended/paused
    ...(isAdmin()
      ? [
          {
            label: "Manage Ambari",
            callback: () => {
              // First navigate to adminView route
              redirectToAdminView();
            },
          },
          {
            label: "Settings",
            callback: () => setShowUserSettingsModal(true),
          },
        ]
      : []),
    {
      label: "Sign out",
      callback: handleSignOut,
    },
  ];

  // FIXED: Apply upgrade blocking logic only to the callback functions, not to the dropdown items visibility
  // This prevents flaky behavior where dropdown items disappear during upgrade pause
  const getNavbarOptionsWithUpgradeCheck = () => {
    return navbarOptions.map(option => {
      // Block admin actions during active upgrades (not suspended)
      if ((option.label === "Manage Ambari" || option.label === "Settings") && isUpgradeBlocking) {
        return {
          ...option,
          callback: () => {
            // Could show a message that admin functions are disabled during upgrade
            console.log("Admin functions are disabled during active upgrade");
          }
        };
      }
      return option;
    });
  };

  const finalNavbarOptions = getNavbarOptionsWithUpgradeCheck();

  return (
    <div>
      {showAmbariAboutModal ? (
        <AmbariAboutModal
          isOpen={showAmbariAboutModal}
          onClose={() => setShowAmbariAboutModal(false)}
        />
      ) : null}
      {showUserSettingsModal && (
        <UserSettingsModal
          isOpen={showUserSettingsModal}
          onClose={() => setShowUserSettingsModal(false)}
        />
      )}
      <Navbar collapseOnSelect expand="lg" className="bg-white">
        <Container className="d-flex justify-content-between">
          <Navbar.Brand
            className="text-black m-0 breadcrumb d-flex align-items-center"
            style={{ fontSize: 24 }}
          >
            <div className="navbar-text ms-1 d-flex" style={{ fontSize: 24 }}>
              <FontAwesomeIcon
                className="me-1"
                icon={faHome}
                style={{ fontSize: 24, cursor: "pointer" }}
                onClick={() => navigate("/main/dashboard/metrics")}
              />
              {hostname && hostMaintenanceState === "ON" ? (
                <div className="d-flex align-items-center">
                  {subPath.split(hostname).map((part, index) => (
                    <div key={index} style={{fontSize: 24}}>
                      {part}
                      {index === 0 && (
                        <>
                          {hostname}
                          <FontAwesomeIcon
                            icon={faMedkit}
                            className="text-dark ms-1 me-1"
                            style={{ fontSize: 18 }}
                            title={`Host ${hostname} is in maintenance mode`}
                          />
                        </>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                subPath
              )}
            </div>
          </Navbar.Brand>
          <div className="right-nav-container d-flex align-items-center">
            <div style={{ width: "10px" }}></div>
            <Nav.Link className="navbar-text navbar-size me-4">
              {clusterName}
            </Nav.Link>
            <div style={{ width: "20px" }}></div>
            {isInstaller() ? null : (
              <div
                onClick={() => {
                  modalManager.show(
                    <BackgroundOperations
                      isExplicitClick
                      isOpen
                      onClose={() => modalManager.hide()}
                    />
                  );
                }}
                className="d-flex align-items-center cursor-pointer"
              >
                <FontAwesomeIcon
                  icon={faCog}
                  className="navbar-text navbar-size"
                />
                <Badge
                  className={`${
                    Number(runningRequestsCount) > 0
                      ? "bg-info"
                      : "bg-secondary-subtle"
                  } rounded-5`}
                >
                  {runningRequestsCount}
                </Badge>
              </div>
            )}
            <div style={{ width: "20px" }}></div>
            {isInstaller() ? null : (
              <NotificationDropdown
                notifications={filteredNotifications}
                onFilterChange={setSelectedFilter}
                alertCounts={alertCounts}
              />
            )}
            <div style={{ width: "20px" }}></div>
            {isInstaller() ? null : (
              <Dropdown>
                <Dropdown.Toggle
                  as="div"
                  className="d-flex align-items-center navbar-item"
                >
                  <FontAwesomeIcon
                    icon={faThLarge}
                    className="navbar-text navbar-size"
                  />
                </Dropdown.Toggle>
                <Dropdown.Menu className="rounded-0">
                  <Dropdown.Header>Views</Dropdown.Header>
                  <DropdownDivider />
                  {getViewsLength() ? (
                    viewsList.map((item, index) => {
                      const displayName =
                        item.label ||
                        item.instance_name ||
                        item.view_name ||
                        "Unknown View";
                      return (
                        <Dropdown.Item
                          key={`${item.view_name || "unknown"}-${
                            item.version || "unknown"
                          }-${item.instance_name || "unknown"}-${index}`}
                          onClick={() => {
                            navigate(
                              `/main/views/${item.view_name}/${item.version}/${item.instance_name}`
                            );
                          }}
                        >
                          {displayName}
                        </Dropdown.Item>
                      );
                    })
                  ) : (
                    <Dropdown.Item disabled>No Views</Dropdown.Item>
                  )}
                </Dropdown.Menu>
              </Dropdown>
            )}
            <div style={{ width: "20px" }}></div>
            <Dropdown>
              <Dropdown.Toggle
                as="div"
                className="navbar-item d-flex align-items-center border rounded px-2 py-1 navbar-text"
                data-bs-toggle="dropdown"
              >
                <FontAwesomeIcon icon={faUser} className="me-2" />
                <div className="navbar-text navbar-size">{login}</div>
              </Dropdown.Toggle>
              <Dropdown.Menu className="rounded-0">
                {finalNavbarOptions.slice(0, -1).map((option) => (
                  <Dropdown.Item key={option.label} onClick={option.callback}>
                    {option.label}
                  </Dropdown.Item>
                ))}
                <DropdownDivider />
                <Dropdown.Item
                  onClick={finalNavbarOptions[finalNavbarOptions.length - 1].callback}
                >
                  {finalNavbarOptions[finalNavbarOptions.length - 1].label}
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </Container>
      </Navbar>
    </div>
  );
}
