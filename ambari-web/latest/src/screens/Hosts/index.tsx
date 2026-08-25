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

import { Alert, Button, Col, Nav, Row, Tab } from "react-bootstrap";
import HostsSummary from "./HostSummary";
import HostConfigs from "./HostConfigs";
import HostAlerts from "../Alerts/HostAlerts";
import HostStackVersions from "../Alerts/HostStackVersions";
import NestedDropdown from "../../components/NestedDropdown";
import { forEach, get } from "lodash";
import { useContext, useEffect, useMemo, useState } from "react";
import IHost from "../../models/host";
import { useHostConfigUpdater } from "../../hooks/useHostConfigUpdater";
import { useParams, useNavigate } from "react-router-dom";
import { IHostComponent } from "../../models/hostComponent";
import { AppContext } from "../../store/context";
import { confirmRecoverHost, doAction } from "./details";
import { useHostChecks } from "../../hooks/useHostChecks";
import HostChecks from "./HostChecks";
import modalManager from "../../store/ModalManager";
import useStackVersion from "../../hooks/useStackVersion";
import { HostsApi } from "../../api/hostsApi";
import Spinner from "../../components/Spinner";
import { ServiceContext } from "../../store/ServiceContext";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import useStackServices from "../../hooks/useStackServices";
import { useConfigs } from "../../hooks/useConfigs";
import useComponentAddDelete from "./hooks/useComponentAddDelete";
import { translate, translateWithVariables } from "../../Utils/Utility";
import classNames from "classnames";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import HostLogs from "./HostLogs";
import useKerberosMode from "../../hooks/useKerberosMode";

export function Hosts() {
  const params = useParams();
  const navigate = useNavigate();
  const {
    isKerberosEnabled,
    clusterName,
    services,
    supports,
  } =
    useContext(AppContext);
  const { allServiceModels: serviceModels, polledHostComponentsData } = useContext(ServiceContext);
  const [allHostModels, setAllHostModels] = useState<IHost[]>([]);
  const { stackVersion, stackVersionList } = useStackVersion();
  const [showHostCheck, setShowHostCheck] = useState(false);
  const [clusterComponents, setClusterComponents] = useState<any>({});
  const [loading, setLoading] = useState(true);
  const [clusterLoadError, setClusterLoadError] = useState<string | null>(null);
  const [clusterRetryCount, setClusterRetryCount] = useState(0);

  const { services: stackServices } = useStackServices();
  const { getConfigByName } = useConfigs([], stackServices as any);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const [activeTab, setActiveTab] = useState(params.tab || "summary");

  // Authorization hooks - implementing Ember.js host authorization patterns
  const { havePermissions, isAuthorized } = useAuthorizationPolicy();
  const { isLoaded: isKerberosModeLoaded, isManualKerberos } = useKerberosMode();

  // Check specific authorizations for host operations
  const canStartStopServices = isAuthorized("SERVICE.START_STOP");
  const canToggleHostMaintenance = isAuthorized("HOST.TOGGLE_MAINTENANCE");
  const canAddDeleteHosts = isAuthorized("HOST.ADD_DELETE_HOSTS");
  const canViewOperationalLogs = havePermissions("SERVICE.VIEW_OPERATIONAL_LOGS");
  const canRunHostChecks = isAuthorized(
    "AMBARI.ADMINISTRATOR, CLUSTER.ADMINISTRATOR",
  );
  const isLogSearchInstalled = services.some(
    (service: any) => get(service, "ServiceInfo.service_name") === "LOGSEARCH",
  );
  const canShowLogs = Boolean(
    supports.logSearch && isLogSearchInstalled && canViewOperationalLogs,
  );
  const stackVersionsAvailable = stackVersionList.length > 0;

  const hostApiQueryParams = useMemo(() => {
    return {
      RequestInfo: {
        query: `Hosts/host_name.in(${params.hostname})`,
      },
    };
  }, [params.hostname]);

  const hostData = useHostConfigUpdater(
    hostApiQueryParams,
    allHostModels,
    setAllHostModels,
  );
  const { startHostCheck, stopHostCheck, isHostCheckRunning, hostCheckResult } =
    useHostChecks();

  const {
    _doDeleteHostComponent,
    applyConfigsCustomization,
    putConfigsToServer,
    clearConfigsChanges,
    loadComponentRelatedConfigs,
    groupedPropertiesToChange,
  } = useComponentAddDelete(
    clusterComponents,
    stackServices,
    getConfigByName,
    setAllHostModels
  );

  const getBootHostsProp = () => {
    let bootHosts = [];
    const hostName = get(allHostModels, "[0].hostName", "");
    const host = {
      name: hostName,
    };
    bootHosts.push(host);
    return bootHosts;
  };

  useEffect(() => {
    if (params.tab && params.tab !== activeTab) {
      setActiveTab(params.tab);
    }
  }, [params.tab]);

  // Reuse centralized polled data from CachedServiceApi instead of making a separate API call
  // This eliminates the duplicate /components/ call
  useEffect(() => {
    if (polledHostComponentsData?.items) {
      setClusterComponents(polledHostComponentsData);
      setClusterLoadError(null);
      setLoading(false);
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    if (clusterName && allHostModels.length && clusterRetryCount > 0) {
      void getClusterComponents();
    }
  }, [clusterName, get(allHostModels, "[0].hostComponents", []).length, clusterRetryCount]);

  useEffect(() => {
    if (!hostData.isLoading && !hostData.error && allHostModels.length === 0) {
      navigate("/main/hosts", { replace: true });
    }
  }, [allHostModels.length, hostData.error, hostData.isLoading, navigate]);

  useEffect(() => {
    if (showHostCheck) {
      startHostCheck(getBootHostsProp());
    } else {
      stopHostCheck();
    }
  }, [showHostCheck]);

  const getAlerts = () => {
    return {
      critical: get(allHostModels, "[0].alertsSummary.CRITICAL", 0),
      warning: get(allHostModels, "[0].alertsSummary.WARNING", 0),
    };
  };

  const tabs: Record<string, { title: React.ReactNode; component: React.ReactNode }> = {
    summary: {
      title: "SUMMARY",
      component: (
        <HostsSummary
          allHostModels={allHostModels}
          setAllHostModels={setAllHostModels}
          clusterComponents={clusterComponents}
        />
      ),
    },
    configs: {
      title: "CONFIGS",
      component: <HostConfigs />,
    },
    alerts: {
      title: (
        <>
          ALERTS{" "}
          <Button
            className={classNames("me-1 text-white fs-10 rounded-1 px-1 py-0", {
              "bg-danger": getAlerts().critical > 0,
              "bg-orange":
                getAlerts().warning > 0 && getAlerts().critical === 0,
              "bg-light":
                getAlerts().critical === 0 && getAlerts().warning === 0,
            })}
          >
            {getAlerts().critical + getAlerts().warning}
          </Button>
        </>
      ),
      component: <HostAlerts key={params.hostname} hostname={params.hostname} />,
    },
  };
  if (stackVersionsAvailable) {
    tabs.stackVersions = {
      title: "VERSIONS",
      component: <HostStackVersions host={allHostModels[0]} />,
    };
  }
  if (canShowLogs) {
    tabs.logs = {
      title: "LOGS",
      component: <HostLogs hostName={params.hostname || ""} />,
    };
  }

  useEffect(() => {
    if (params.tab === "versions" && stackVersionsAvailable) {
      navigate(`/main/hosts/${params.hostname}/stackVersions`, { replace: true });
      return;
    }
    const versionStateLoaded = stackVersion !== undefined;
    if (params.tab && versionStateLoaded && !Object.hasOwn(tabs, params.tab)) {
      navigate(`/main/hosts/${params.hostname}/summary`, { replace: true });
    }
  }, [canShowLogs, params.hostname, params.tab, stackVersion, stackVersionsAvailable]);

  const isActive = () => {
    return get(allHostModels, "[0]")?.isActive();
  };

  const getClusterComponents = async () => {
    setLoading(true);
    setClusterLoadError(null);
    try {
      const response = await HostsApi.getClusterComponents(
        clusterName,
        "ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,&minimal_response=true"
      );
      setClusterComponents(response);
    } catch (error: any) {
      setClusterLoadError(
        error?.response?.data?.message || "Ambari could not load host component data.",
      );
    } finally {
      setLoading(false);
    }
  };

  const getMaintenanceOptions = () => {
    const isNotHeartBeating =
      get(allHostModels, "[0].state", "") === "HEARTBEAT_LOST";
    const hostName = get(allHostModels, "[0].hostName", "");
    let result: any[] = [];

    // SERVICE.START_STOP authorization check for component operations
    if (canStartStopServices) {
      result = result.concat([
        {
          label: translate("hosts.host.details.startAllComponents"),
          isDisabled: isNotHeartBeating,
          icon: "play",
          iconClass: "text-success",
          onClick: () => {
            if (!isNotHeartBeating) {
              doAction({
                action: "startAllComponents",
                hostName: hostName,
                host: get(allHostModels, "[0]"),
              });
            }
          },
        },
        {
          label: translate("hosts.host.details.stopAllComponents"),
          isDisabled: isNotHeartBeating,
          icon: "stop",
          iconClass: "text-danger",
          onClick: () => {
            if (!isNotHeartBeating) {
              doAction({
                action: "stopAllComponents",
                hostName: hostName,
                host: get(allHostModels, "[0]"),
                clusterName: clusterName,
              });
            }
          },
        },
        {
          label: translate("hosts.host.details.restartAllComponents"),
          isDisabled: isNotHeartBeating,
          icon: "repeat",
          onClick: () => {
            if (!isNotHeartBeating) {
              doAction({
                action: "restartAllComponents",
                hostName: hostName,
                host: get(allHostModels, "[0]"),
                clusterName: clusterName,
              });
            }
          },
        },
      ]);
    }

    // HOST.TOGGLE_MAINTENANCE authorization check for maintenance operations
    if (canToggleHostMaintenance) {
      result = result.concat([
        {
          label: translate("hosts.host.details.setRackId"),
          icon: "cog",
          onClick: () => {
            doAction({
              action: "setRackId",
              hostName: hostName,
              clusterName: clusterName,
              rack: get(allHostModels, "[0].rack", ""),
              callback: setAllHostModels,
            });
          },
        },
        {
          label: translate("passiveState.turn" + (isActive() ? "On" : "Off")),
          icon: "medkit",
          onClick: () => {
            doAction({
              action: "onOffPassiveModeForHost",
              hostName: hostName,
              host: get(allHostModels, "[0]"),
              clusterStackVersion: stackVersionList,
              clusterName: clusterName,
              active: isActive(),
              label: translate(
                "passiveState.turn" + (isActive() ? "On" : "Off")
              ),
            });
          },
        },
      ]);
    }

    if (
      isKerberosEnabled &&
      supports.regenerateKeytabsOnSingleHost &&
      isKerberosModeLoaded &&
      !isManualKerberos
    ) {
      result.push({
        label: translate("admin.kerberos.button.regenerateKeytabs"),
        icon: "repeat",
        onClick: () => {
          doAction({
            action: "regenerateKeytabFileOperations",
            hostName: hostName,
            clusterName: clusterName,
          });
        },
      });
    }

    // HOST.ADD_DELETE_HOSTS authorization check for delete host operation
    if (canAddDeleteHosts) {
      result.push({
        label: translate("hosts.host.details.deleteHost"),
        icon: "remove",
        iconClass: "text-danger",
        onClick: () => {
          doAction({
            action: "deleteHost",
            hostName: hostName,
            clusterName: clusterName,
            host: get(allHostModels, "[0]"),
            clusterComponents: get(clusterComponents, "items", []),
            serviceModels: serviceModels,
            doDeleteHostComponent: _doDeleteHostComponent,
            applyConfigsCustomization,
            putConfigsToServer,
            clearConfigsChanges,
            loadComponentRelatedConfigs,
            groupedPropertiesToChange,
          });
        },
      });
    }

    // Admin/Operator authorization check for host check operation
    if (canRunHostChecks) {
      result.push({
        label: translate("host.host.details.checkHost"),
        icon: "check",
        onClick: () => {
          if (allHostModels.length) {
            let modalProps = {
              isOpen: true,
              onClose: () => {},
              modalTitle: translate("popup.confirmation.commonHeader"),
              modalBody: translateWithVariables("hosts.checkHost.popup", {
                "0": hostName,
              }),
              successCallback: () => {
                setShowHostCheck(true);
                modalManager.hide();
              },
              options: {
                buttonSize: "sm" as "sm" | "lg" | undefined,
                cancelableViaIcon: true,
                cancelableViaBtn: true,
                okButtonVariant: "primary",
              },
            };
            modalManager.show(modalProps);
          }
        },
      });
    }

    return result;
  };

  const getClients = () => {
    let clients: any[] = [];
    const hostName = get(allHostModels, "[0].hostName", "");
    const hostComponents = get(allHostModels, "[0].hostComponents", []);
    const clientComponents = hostComponents.filter((component: any) =>
      get(component, "isClient", false)
    );

    forEach(clientComponents, (component: IHostComponent) => {
      clients.push({
        label: component.displayName,
        onClick: () => {
          doAction({
            action: "downloadClientConfigs",
            componentName: component.componentName,
            hostName: hostName,
            clusterName: clusterName,
          });
        },
      });
    });
    if (clients.length > 1) {
      clients.unshift({
        label: translate("host.host.details.downloadAllClients"),
        onClick: () => {
          doAction({
            action: "downloadAllClientConfigs",
            hostName: hostName,
            clusterName: clusterName,
          });
        },
      });
    }
    return clients;
  };

  const buildHostActionsSubmenu = () => {
    const submenu = [...getMaintenanceOptions()];

    const clients = getClients();
    if (clients.length > 0) {
      submenu.push({
        label: translate("services.service.actions.downloadClientConfigs"),
        submenu: clients,
        icon: "download",
      });
    }

    // Add recover host - no authorization check needed (matches Ember.js pattern)
    submenu.push({
      action: "confirmRecoverHost",
      label: translate("hosts.host.details.recoverHost"),
      icon: "clockRotateLeft",
      onClick: () => {
        confirmRecoverHost({
          host: get(allHostModels, "[0]"),
          clusterName: clusterName,
          isKerberosEnabled: isKerberosEnabled,
          getKDCSessionState: getKDCSessionState,
        });
      },
    });

    return submenu;
  };

  const hostActionsMenu = {
    label: String(translate("hosts.host.details.hostActions")).toUpperCase(),
    submenu: buildHostActionsSubmenu(),
  };

  return (
    <div className="p-4">
      {showHostCheck ? (
        <HostChecks
          isOpen={showHostCheck}
          onClose={() => {
            setShowHostCheck(false);
          }}
          successCallback={() => {
            startHostCheck(getBootHostsProp());
          }}
          loading={isHostCheckRunning}
          hostCheckResult={hostCheckResult}
        />
      ) : null}
      <Tab.Container activeKey={activeTab}>
        <Row className="mx-5">
          <Col>
            <Nav
              variant="underline"
              onSelect={(selectedKey) => {
                if (selectedKey && activeTab !== selectedKey) {
                  setActiveTab(selectedKey);
                  navigate(`/main/hosts/${params.hostname}/${selectedKey}`);
                }
              }}
            >
              {Object.entries(tabs).map(([key, tab]) => (
                <Nav.Item>
                  <Nav.Link eventKey={key} className="ambari-nav">
                    {tab.title}
                  </Nav.Link>
                </Nav.Item>
              ))}
            </Nav>
          </Col>
          <Col className="d-flex justify-content-end">
            <NestedDropdown menu={hostActionsMenu} dropDirection="start" />
          </Col>
        </Row>
        <Row>
          {hostData.error || clusterLoadError ? (
            <Col className="p-4">
              <Alert variant="danger">
                {hostData.error || clusterLoadError}{" "}
                <Button
                  size="sm"
                  variant="outline-danger"
                  onClick={() => {
                    if (hostData.error) {
                      hostData.retry();
                    }
                    if (clusterLoadError) {
                      setClusterRetryCount((value) => value + 1);
                    }
                  }}
                >
                  Retry
                </Button>{" "}
                <Button size="sm" variant="outline-secondary" onClick={() => navigate("/main/hosts")}>
                  Back to Hosts
                </Button>
              </Alert>
            </Col>
          ) : loading || hostData.isLoading ? (
            <Spinner />
          ) : (
            <Col className="p-4">
              {Object.entries(tabs).map(([key, tab]) => {
                if (activeTab === key) {
                  return tab.component;
                }
                return null;
              })}
            </Col>
          )}
        </Row>
      </Tab.Container>
    </div>
  );
}
