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
import { useLocation, useNavigate } from "react-router-dom";
import AmbariAboutModal from "../AmbariAboutModal.tsx";
import "../styles/app.scss";
import {
  classicExperienceUrl,
  redirectToAdminView,
} from "../Utils/adminViewRedirect";
import modalManager from "../store/ModalManager.ts";
import BackgroundOperations from "../screens/BackgroundOperations/index.tsx";
import { useCallback } from "react";
import { useAuth } from "../hooks/useAuth";
import { useAlerts } from "../store/AlertsContext";
import useAuthorizationPolicy from "../hooks/useAuthorizationPolicy";
import { clusterNavigationEnabled } from "../Utils/authPolicy";
import { openViewInstance, ViewInstance } from "../Utils/viewUtils";
import DigitalClock from "./DigitalClock";

type NavbarOption = {
  label: string;
  callback: () => void;
};

// type SignOutResponse = {
//   status: number;
// };

type NavBarProps = {
  subPath: string;
  viewsList: ViewInstance[];
  clusterControls?: boolean;
  homePath?: string;
  runningRequestsCount?: number;
  hostMaintenanceState?: string;
  hostname?: string;
  enableDigitalClock?: boolean;
};

export default function NavBar({
  subPath,
  viewsList,
  clusterControls = true,
  homePath = "/main/dashboard/metrics",
  runningRequestsCount,
  hostMaintenanceState,
  hostname,
  enableDigitalClock,
}: NavBarProps) {
  const [showUserSettingsModal, setShowUserSettingsModal] = useState(false);
  const [filteredNotifications, setFilteredNotifications] = useState<any[]>([]);
  const [alertCounts, setAlertCounts] = useState({
    all: 0,
    critical: 0,
    warning: 0,
  });

  // FOLLOWING EMBERJS PATTERN: Get alert data from useAlerts hook (WebSocket updates)
  // EmberJS: mainAlertDefinitionsController.unhealthyAlertInstancesCount
  // - Computed from content.@each.summary (App.AlertDefinition.find())
  // - Updated via WebSocket /events/alerts
  // - NO POLLING for navbar alerts
  const { unhealthyAlertInstances } = useAlerts();

  const location = useLocation();
  const [selectedFilter, setSelectedFilter] = useState("all");
  const {
    clusterName,
    serverClock,
    userTimezone,
  } = useContext(AppContext);
  const [showAmbariAboutModal, setShowAmbariAboutModal] = useState(false);

  const navigate = useNavigate();

  const clusterNavigation = clusterNavigationEnabled(
    clusterControls,
    location.pathname,
  );

  // REMOVED fetchAlerts and polling - using WebSocket data from useAlerts hook
  // EmberJS pattern: navbar alerts come from mainAlertDefinitionsController computed properties
  // which are based on alert definitions summary (updated via WebSocket /events/alerts)

  // Update local state when unhealthyAlertInstances changes (from WebSocket)
  useEffect(() => {
    if (unhealthyAlertInstances && unhealthyAlertInstances.length > 0) {
      calculateAlertCounts(unhealthyAlertInstances);
    } else {
      setAlertCounts({ all: 0, critical: 0, warning: 0 });
      setFilteredNotifications([]);
    }
  }, [unhealthyAlertInstances]);

  useEffect(() => {
    filterNotifications();
  }, [selectedFilter, unhealthyAlertInstances]);

  const calculateAlertCounts = (alerts: any[]) => {
    const counts = { all: alerts.length, critical: 0, warning: 0 };
    alerts.forEach((alert) => {
      const state = alert.Alert?.state;
      if (state === "CRITICAL") counts.critical++;
      if (state === "WARNING") counts.warning++;
    });
    setAlertCounts(counts);
  };

  const filterNotifications = () => {
    const alerts = unhealthyAlertInstances || [];
    if (selectedFilter === "all") {
      setFilteredNotifications(alerts);
    } else {
      setFilteredNotifications(
        alerts.filter(
          (notification: any) =>
            notification.Alert?.state === selectedFilter.toUpperCase()
        )
      );
    }
  };

  const getViewsLength = () => {
    return viewsList?.length;
  };

  // Authorization hooks - implementing Ember.js showSettingsPopup authorization pattern
  const { user, isClusterUser, logout } = useAuth();
  const { isAuthorized } = useAuthorizationPolicy();

  const handleSignOut = useCallback(async () => {
    // REMOVED stopPolling() - no longer polling for navbar alerts (using WebSocket)
    await logout();
    navigate("/login", { replace: true });
  }, [logout, navigate]);

  const canManageAmbari = isAuthorized(
    "AMBARI.ADD_DELETE_CLUSTERS, AMBARI.ASSIGN_ROLES, AMBARI.EDIT_STACK_REPOS, AMBARI.MANAGE_GROUPS, AMBARI.MANAGE_STACK_VERSIONS, AMBARI.MANAGE_USERS, AMBARI.MANAGE_VIEWS, AMBARI.RENAME_CLUSTER"
  );
  const canSeeSettings = isAuthorized("AMBARI.MANAGE_SETTINGS");
  const canOpenSettings = isAuthorized("CLUSTER.UPGRADE_DOWNGRADE_STACK");
  const canOpenBackgroundOperations = !isClusterUser();
  const shouldShowDigitalClock = enableDigitalClock ??
    String(import.meta.env.VITE_ENABLE_DIGITAL_CLOCK || "").toLowerCase() ===
      "true";

  const navbarOptions: NavbarOption[] = [
    {
      label: "About",
      callback: () => setShowAmbariAboutModal(true),
    },
    {
      label: "Switch Experience",
      callback: () => window.location.assign(
        classicExperienceUrl(window.location.pathname),
      ),
    },
    ...(canManageAmbari
      ? [
          {
            label: "Manage Ambari",
            callback: () => void redirectToAdminView(),
          },
        ]
      : []),
    ...(canSeeSettings
      ? [
          {
            label: "Settings",
            callback: () => {
              if (canOpenSettings) setShowUserSettingsModal(true);
            },
          },
        ]
      : []),
    {
      label: "Sign out",
      callback: handleSignOut,
    },
  ];

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
      <Navbar id="top-nav" collapseOnSelect expand="lg" className="bg-white">
        <Container className="d-flex justify-content-between">
          <Navbar.Brand
            className="text-black m-0 breadcrumb d-flex align-items-center"
            style={{ fontSize: 24 }}
          >
            <div className="navbar-text ms-1 d-flex" style={{ fontSize: 24 }}>
              <FontAwesomeIcon
                className="me-1"
                icon={faHome}
                style={{
                  fontSize: 24,
                  cursor: clusterNavigation ? "pointer" : "default",
                }}
                aria-disabled={!clusterNavigation}
                onClick={clusterNavigation ? () => navigate(homePath) : undefined}
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
            {shouldShowDigitalClock ? (
              <>
                <DigitalClock
                  serverClock={serverClock}
                  timeZone={userTimezone}
                />
                <div style={{ width: "20px" }}></div>
              </>
            ) : null}
            <div style={{ width: "10px" }}></div>
            <Nav.Link className="navbar-text navbar-size me-4">
              {clusterName}
            </Nav.Link>
            <div style={{ width: "20px" }}></div>
            {!clusterNavigation || !canOpenBackgroundOperations ? null : (
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
            {!clusterNavigation ? null : (
              <NotificationDropdown
                notifications={filteredNotifications}
                onFilterChange={setSelectedFilter}
                alertCounts={alertCounts}
              />
            )}
            <div style={{ width: "20px" }}></div>
            {!clusterNavigation ? null : (
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
                    viewsList.map((item) => {
                      return (
                        <Dropdown.Item
                          key={`${item.viewName}-${item.version}-${item.instanceName}`}
                          onClick={() => {
                            openViewInstance(item);
                          }}
                        >
                          {item.label}
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
                <div className="navbar-text navbar-size">{user?.user_name || ""}</div>
              </Dropdown.Toggle>
              <Dropdown.Menu className="rounded-0">
                {navbarOptions.slice(0, -1).map((option) => (
                  <Dropdown.Item key={option.label} onClick={option.callback}>
                    {option.label}
                  </Dropdown.Item>
                ))}
                <DropdownDivider />
                <Dropdown.Item
                  onClick={navbarOptions[navbarOptions.length - 1].callback}
                >
                  {navbarOptions[navbarOptions.length - 1].label}
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </Container>
      </Navbar>
    </div>
  );
}
