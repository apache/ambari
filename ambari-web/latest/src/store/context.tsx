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

import React, {
  createContext,
  Dispatch,
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { State, Action } from "./types";
import { reducer, initialState } from "./reducer";
import { Client } from "@stomp/stompjs";
import ClusterApi from "../api/clusterApi";
import { ChooseServicesApi } from "../api/chooseServicesApi";
import { ServicesApi } from "../api/servicesApi";
import { forEach, get, isEmpty, isUndefined, map, set } from "lodash";
import ConfigsApi from "../api/configsApi";
import VersionsApi from "../api/versionsApi";
import { mapStackConfigProperties } from "../Utils/Utility";
import useAuth from "../hooks/useAuth";
import { parsePersistedValue, persistedPayload } from "../Utils/persistedSettings";
import { DEFAULT_SUPPORTS } from "../constants";
import { detectUserTimezone } from "../Utils/timezone";
import {
  BackgroundRequest,
  replaceRequestSnapshot,
  upsertRequestEvent,
} from "../Utils/backgroundOperations";
import {
  createStompTransport,
  shouldFallbackToSockJs,
} from "../Utils/stompTransport";
// import {LocalStorageOps} from "../Utils/LocalStorageOps";

interface AppContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  client: any;
  isSocketConnected: boolean;
  parsedSocketMessages: any[];
  clusterName: string;
  services: any[];
  cluster: any;
  isAppLoaded: boolean;
  initializationError: string | null;
  retryInitialization: () => void;
  serviceComponentInfo: any;
  isKerberosEnabled: boolean;
  stackConfigurations: any;
  allHostNames: string[];
  ambariProperties: any;
  ambariServerVersion?: string;
  serverClock: number | string | null;
  supports: Record<string, boolean>;
  setSupports: (supports: Record<string, boolean>) => void;
  upgradeState: string;
  setUpgradeState: (state: string) => void;
  upgradeDirection: string;
  setUpgradeDirection: (direction: string) => void;
  upgradeSuspend: boolean;
  currentStackVersion: string;
  setCurrentStackVersion: (version: string) => void;
  upgradeId: number;
  setUpgradeId: (id: number) => void;
  isPatchUpgrade: boolean;
  setIsPatchUpgrade?: (isPatch: boolean) => void;
  upgradeVersionDisplayName?: string;
  setUpgradeVersionDisplayName?: (name: string) => void;
  upgradeIsFinalizeItem: boolean;
  setUpgradeIsFinalizeItem: (isFinalize: boolean) => void;
  userUrl?: string;
  sessionsValidated: boolean;
  sessionExists: boolean;
  clusterState: any;
  userBgPreferences: boolean;
  setUserBgPreferences: (value: boolean) => void;
  syncUserBgPreferences: (value: boolean) => void;
  userTimezone: string;
  syncUserTimezone: (value: string) => void;
  // Background Operations - persistent cache like Ember.js singleton
  backgroundOperations: any[];
  setBackgroundOperations: React.Dispatch<React.SetStateAction<BackgroundRequest[]>>;
  updateBackgroundOperations: (newRequests: any[]) => void;
  fetchBackgroundOperationsSnapshot: (
    pageSize?: number,
  ) => Promise<BackgroundRequestPage | null>;
  backgroundOperationsPageSize: number;
  setBackgroundOperationsPageSize: (size: number) => void;
  runningOperationsCount: number;
  // Ember.js upgrade computed properties
  upgradeInit: boolean;
  upgradeInProgress: boolean;
  upgradeCompleted: boolean;
  upgradeHolding: boolean;
  upgradeAborted: boolean;
  upgradeSuspended: boolean;
  upgradeIsRunning: boolean;
  wizardIsNotFinished: boolean;
  isNonWizardUser: boolean;
  wizardUser: string;
  isClusterInstalled?: boolean;
  loginName: string;
  clockDistance: number;
  serviceCheckSupportedMap: Record<string, boolean>;
  stackVersion: any;
  stackVersionList: any[];
}

type BackgroundRequestPage = {
  items: BackgroundRequest[];
  itemTotal?: number;
};

export const AppContext = createContext<AppContextProps>({
  state: initialState,
  dispatch: () => undefined,
  client: null,
  isSocketConnected: false,
  parsedSocketMessages: [],
  clusterName: "",
  services: [],
  cluster: {},
  isAppLoaded: false,
  initializationError: null,
  retryInitialization: () => {},
  serviceComponentInfo: {},
  isKerberosEnabled: false,
  stackConfigurations: [],
  allHostNames: [],
  ambariProperties: {},
  ambariServerVersion: "",
  serverClock: null,
  supports: DEFAULT_SUPPORTS,
  setSupports: () => {},
  upgradeState: "",
  setUpgradeState: () => {},
  upgradeDirection: "",
  setUpgradeDirection: () => {},
  upgradeSuspend: false,
  currentStackVersion: "",
  setCurrentStackVersion: () => {},
  upgradeId: 0,
  setUpgradeId: () => {},
  isPatchUpgrade: false,
  setIsPatchUpgrade: () => {},
  upgradeVersionDisplayName: "",
  setUpgradeVersionDisplayName: () => {},
  sessionExists: false,
  sessionsValidated: false,
  clusterState: {},
  userBgPreferences: true,
  setUserBgPreferences: () => {},
  syncUserBgPreferences: () => {},
  userTimezone: detectUserTimezone(),
  syncUserTimezone: () => {},
  // Background Operations defaults
  backgroundOperations: [],
  setBackgroundOperations: () => {},
  updateBackgroundOperations: () => {},
  fetchBackgroundOperationsSnapshot: async () => null,
  backgroundOperationsPageSize: 20,
  setBackgroundOperationsPageSize: () => {},
  runningOperationsCount: 0,
  // Ember.js upgrade computed properties defaults
  upgradeInit: true,
  upgradeInProgress: false,
  upgradeCompleted: false,
  upgradeHolding: false,
  upgradeAborted: false,
  upgradeSuspended: false,
  upgradeIsRunning: false,
  upgradeIsFinalizeItem: false,
  setUpgradeIsFinalizeItem: () => {},
  wizardIsNotFinished: false,
  isNonWizardUser: false,
  wizardUser: "",
  isClusterInstalled: false,
  loginName: "",
  clockDistance: 0,
  serviceCheckSupportedMap: {},
  stackVersion: undefined,
  stackVersionList: [],
});

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [isSocketConnected, setIsSocketConnected] = useState(false);
  const [socketClient, setSocketClient] = useState(null);
  const [isAppLoaded, setAppLoaded] = useState(false);
  const [initializationError, setInitializationError] = useState<string | null>(null);
  const [initializationAttempt, setInitializationAttempt] = useState(0);
  const [parsedSocketMessages, setParsedSocketMessages] = useState<any[]>([]);
  const [clusterName, setClusterName] = useState<string>("");
  const [isKerberosEnabled, setIsKerberosEnabled] = useState(false);
  const [cluster, setCluster] = useState<any>({});
  const [isClusterInstalled, setIsClusterInstalled] = useState<boolean|undefined>(undefined);
  const [serviceComponentInfo, setServiceComponentInfo] = useState<any>({});
  const [upgradeState, setUpgradeState] = useState<string>("NOT_REQUIRED");
  const [upgradeDirection, setUpgradeDirection] = useState<string>("");
  const [upgradeSuspend, setUpgradeSuspend] = useState<boolean>(false);
  const [upgradeId, setUpgradeId] = useState<number>(0);
  const [upgradeIsFinalizeItem, setUpgradeIsFinalizeItem] =
    useState<boolean>(false);
  const [isPatchUpgrade, setIsPatchUpgrade] = useState<boolean>(false);
  const [upgradeVersionDisplayName, setUpgradeVersionDisplayName] =
    useState<string>("");
  const [currentStackVersion, setCurrentStackVersion] = useState<string>("");
  const [ambariProperties, setAmbariProperties] = useState({});
  const [ambariServerVersion, setAmbariServerVersion] = useState("");
  const [serverClock, setServerClock] = useState<number | string | null>(null);
  const [supports, setSupports] = useState(DEFAULT_SUPPORTS);
  const [wizardUser, setWizardUser] = useState("");
  const [clusterState, setClusterState] = useState({});
  const [userUrl, setUserUrl] = useState("");
  const { authorizations, user } = useAuth();
  const loginName = user?.user_name;
  const isOnlyViewUser = authorizations.length === 0
    || (authorizations.length === 1
      && authorizations[0].authorization_id === "VIEW.USE");
  const useSockJs = useRef(typeof WebSocket === "undefined");
  const hasConnectedSocket = useRef(false);
  const backgroundFetchPromise = useRef<{
    pageSize: number;
    promise: Promise<BackgroundRequestPage>;
  } | null>(null);
  const backgroundOperationsPageSizeRef = useRef(20);
  const [client] = useState(() => new Client({
    webSocketFactory: () => createStompTransport(useSockJs.current, window.location),
    reconnectDelay: 6000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  }));
  const [services, setServices] = useState([]);
  const [stackConfigurations, setStackConfigurations] = useState([]);
  // Service check supported map - static stack property, loaded once from initial stack configs fetch
  const [serviceCheckSupportedMap, setServiceCheckSupportedMap] = useState<Record<string, boolean>>({});

  // Stack versions - fetched once at startup, refreshed on /events/upgrade (like Ember's DS.Model store)
  const [stackVersion, setStackVersion] = useState<any>(undefined);
  const [stackVersionList, setStackVersionList] = useState<any[]>([]);

  const fetchStackVersionList = async () => {
    try {
      const response = await VersionsApi.getServices(clusterName);
      setStackVersion(response);
      const items = get(response, "items", []);
      const list: any[] = [];
      forEach(items, (item: any) => {
        const repoVersionId = get(item, "ClusterStackVersions.repository_version");
        const repoVersion = get(item, "repository_versions", []).find(
          (repo: any) => get(repo, "RepositoryVersions.id") === repoVersionId
        )?.RepositoryVersions?.repository_version;
        list.push({
          id: get(item, "ClusterStackVersions.id"),
          cluster_name: get(item, "ClusterStackVersions.cluster_name"),
          stack: get(item, "ClusterStackVersions.stack"),
          version: get(item, "ClusterStackVersions.version"),
          state: get(item, "ClusterStackVersions.state"),
          displayName: get(item, "repository_versions.[0].RepositoryVersions.display_name"),
          not_installed_hosts: get(item, "ClusterStackVersions.host_states.NOT_REQUIRED"),
          installing_hosts: get(item, "ClusterStackVersions.host_states.INSTALLING"),
          installed_hosts: get(item, "ClusterStackVersions.host_states.INSTALLED"),
          install_failed_hosts: get(item, "ClusterStackVersions.host_states.INSTALL_FAILED"),
          out_of_sync_hosts: get(item, "ClusterStackVersions.host_states.OUT_OF_SYNC"),
          current_hosts: get(item, "ClusterStackVersions.host_states.CURRENT"),
          supports_revert: get(item, "ClusterStackVersions.supports_revert"),
          repository_version_id: repoVersionId,
          repository_version: repoVersion,
        });
      });
      setStackVersionList(list);
    } catch (error) {
      console.error("Failed to fetch stack versions:", error);
    }
  };
  const [userBgPreferences, setUserBgPreferences] = useState(true);
  const [userTimezone, setUserTimezone] = useState(detectUserTimezone());
  const [allHostNames, setAllHostNames] = useState([]);

  // Clock distance (server - client offset), derived from serverClock (fetched once by
  // getAmbariProperties) instead of a separate RootServiceComponents/server_clock request.
  const [clockDistance, setClockDistance] = useState<number>(0);

  useEffect(() => {
    if (serverClock === null) {
      return;
    }
    const clientClock = Date.now();
    let serverClockMs = serverClock.toString();
    serverClockMs = serverClockMs.length < 13 ? serverClockMs + "000" : serverClockMs;
    setClockDistance(parseInt(serverClockMs, 10) - clientClock);
  }, [serverClock]);

  // Background Operations - persistent cache like Ember.js singleton
  const [backgroundOperations, setBackgroundOperations] = useState<BackgroundRequest[]>([]);
  const [backgroundOperationsPageSize, setBackgroundOperationsPageSizeState] = useState(20);

  const setBackgroundOperationsPageSize = useCallback((size: number) => {
    backgroundOperationsPageSizeRef.current = size;
    setBackgroundOperationsPageSizeState(size);
  }, []);

  const updateBackgroundOperations = useCallback((newRequests: BackgroundRequest[]) => {
    setBackgroundOperations(replaceRequestSnapshot(newRequests));
  }, []);

  const fetchBackgroundOperations = useCallback(async (
    requestedPageSize = backgroundOperationsPageSizeRef.current,
  ): Promise<BackgroundRequestPage | null> => {
    if (!clusterName || !isClusterInstalled) {
      return null;
    }
    if (requestedPageSize > backgroundOperationsPageSizeRef.current) {
      setBackgroundOperationsPageSize(requestedPageSize);
    }

    const activeRequest = backgroundFetchPromise.current;
    if (activeRequest) {
      const response = await activeRequest.promise;
      return backgroundOperationsPageSizeRef.current > activeRequest.pageSize
        ? fetchBackgroundOperations(backgroundOperationsPageSizeRef.current)
        : response;
    }

    const pageSize = backgroundOperationsPageSizeRef.current;
    const requestEntry = {
      pageSize,
      promise: Promise.resolve({ items: [] } as BackgroundRequestPage),
    };
    const operation = ClusterApi.getRequests(clusterName, pageSize).then((response) => {
      const page = response as BackgroundRequestPage;
      updateBackgroundOperations(page.items);
      return page;
    });
    requestEntry.promise = operation.finally(() => {
      if (backgroundFetchPromise.current === requestEntry) {
        backgroundFetchPromise.current = null;
      }
    });
    backgroundFetchPromise.current = requestEntry;
    const response = await requestEntry.promise;
    return backgroundOperationsPageSizeRef.current > pageSize
      ? fetchBackgroundOperations(backgroundOperationsPageSizeRef.current)
      : response;
  }, [
    clusterName,
    isClusterInstalled,
    setBackgroundOperationsPageSize,
    updateBackgroundOperations,
  ]);

  // Computed property for running operations count (like Ember.js)
  const runningOperationsCount = backgroundOperations.filter((request: any) => {
    const status = request?.Requests?.request_status;
    return ["IN_PROGRESS", "QUEUED", "PENDING"].includes(status);
  }).length;

  const fetchClusterServices = async () => {
    if (!isClusterInstalled) {
      return;
    }

    try {
      const clusterServices = await ChooseServicesApi.servicesList(clusterName);
      setServices(clusterServices.items);
      setAppLoaded(true);
    } catch (err) {
      setServices([]);
      setInitializationError("Ambari could not load the installed cluster services.");
    }
  };

  const fetchClusterState = async () => {
    try {
      const state = await ClusterApi.getPersistData("CLUSTER_STATE");
      setClusterState(state);
    } catch (error) {
      console.error("Failed to fetch cluster state:", error);
    }
  };

  useEffect(() => {
    if (!isOnlyViewUser) {
      void fetchClusterState();
    }
  }, [isOnlyViewUser]);

  useEffect(() => {
    if (isOnlyViewUser) {
      return;
    }
    if (clusterName && isClusterInstalled) {
      fetchClusterServices();
      fetchAllHostNames();
      fetchUpgradeStates();
      fetchStackVersionList();
    } else if (!isUndefined(isClusterInstalled) && !isClusterInstalled) {
      setAppLoaded(true);
    }
  }, [clusterName, isClusterInstalled, isOnlyViewUser, initializationAttempt]);

  useEffect(() => {
    async function fetchStackConfigs() {
      const stack = get(cluster, "version", "").split("-")[0];
      const version = get(cluster, "version", "").split("-")[1];
      const serviceNames = map(services, "ServiceInfo.service_name").join(",");
      if (stack && version && serviceNames) {
        const response = await ConfigsApi.getServiceConfigurations(
          stack,
          version,
          serviceNames
        );
        const stackConfigs = mapStackConfigProperties(response);
        setStackConfigurations(stackConfigs);

        // Extract service_check_supported map (static stack property, like Ember's App.services.supportsServiceCheck)
        const checkSupportedMap: Record<string, boolean> = {};
        if (response?.items) {
          response.items.forEach((item: any) => {
            const svcName = get(item, "StackServices.service_name", "");
            const supported = get(item, "StackServices.service_check_supported", false);
            if (svcName) {
              checkSupportedMap[svcName] = supported;
            }
          });
        }
        setServiceCheckSupportedMap(checkSupportedMap);
      }
    }
    fetchStackConfigs();
  }, [services, cluster]);

  const fetchClusterData = async () => {
      const clusterData = await ClusterApi.getClusterData();
      set(
        clusterData,
        "items.[0].Clusters.stack",
        get(clusterData, "items.[0].Clusters.version", "")?.split("-")[0]
      );
      set(
        clusterData,
        "items[0].Clusters.versionNum",
        get(clusterData, "items.[0].Clusters.version", "")?.split("-")[1]
      );
      const clusterInfo = clusterData?.items[0]?.Clusters;
      setCluster(clusterInfo || {});
      setClusterName(clusterInfo?.cluster_name || "");
      setIsKerberosEnabled(
        clusterData?.items?.[0]?.Clusters?.security_type === "KERBEROS"
      );

      const isInstalled = clusterInfo?.provisioning_state === "INSTALLED";
      setIsClusterInstalled(isInstalled);
  };

  const fetchServiceComponentInfo = async () => {
    const stack = get(cluster, "version", "").split("-")[0];
    const version = get(cluster, "version", "").split("-")[1];
    try {
      const data = await ServicesApi.getServices(stack, version);
      setServiceComponentInfo(data);
    } catch (error) {
      console.error("Failed to fetch service component info:", error);
    }
  };

  const fetchAllHostNames = async () => {
    if (!isClusterInstalled) {
      return;
    }

    try {
      const data = await ClusterApi.getHosts(clusterName);
      const hostNames = data.items.map((item: any) => item.Hosts.host_name);
      setAllHostNames(hostNames);
    } catch (error) {}
  };
  const getAmbariProperties = async () => {
    const response = await ClusterApi.loadAmbariProperties();
    setAmbariProperties(response?.RootServiceComponents?.properties || {});
    setAmbariServerVersion(response?.RootServiceComponents?.component_version || "");
    setServerClock(response?.RootServiceComponents?.server_clock ?? null);
  };

  const fetchUpgradeStates = async () => {
    if (!isClusterInstalled) {
      return;
    }

    try {
      const response = await ClusterApi.getUpgradeState(clusterName);
      // response would have items get the last item.
      const lastItemIndex = response?.items?.length - 1;
      const upgradeState = get(
        response,
        `items[${lastItemIndex}].Upgrade.request_status`,
        "NOT_REQUIRED"
      );
      const upgradeSuspend = get(
        response,
        `items[${lastItemIndex}].Upgrade.suspended`,
        false
      );
      const upgradeId = get(
        response,
        `items[${lastItemIndex}].Upgrade.request_id`,
        0
      );
      const upgradeDirection = get(
        response,
        `items[${lastItemIndex}].Upgrade.direction`,
        "UPGRADE"
      );
      setUpgradeDirection(upgradeDirection);
      setUpgradeId(upgradeId);
      setUpgradeState(upgradeState);
      setUpgradeSuspend(upgradeSuspend);

      const persistedUpgradeState = await Promise.allSettled([
        ClusterApi.getPersistData("isPatchUpgrade"),
        ClusterApi.getPersistData("upgradeIsFinalizeItem"),
        ClusterApi.getPersistData("upgradeVersionDisplayName"),
        ClusterApi.getPersistData("wizard-data"),
      ]);
      const [isPatch, isFinalizeItem, upgradeVersionDisplayName, wizardData] = persistedUpgradeState;
      if (isPatch.status === "fulfilled") {
        setIsPatchUpgrade(parsePersistedValue(isPatch.value, false));
      }
      if (isFinalizeItem.status === "fulfilled") {
        setUpgradeIsFinalizeItem(parsePersistedValue(isFinalizeItem.value, false));
      }
      if (upgradeVersionDisplayName.status === "fulfilled") {
        setUpgradeVersionDisplayName(parsePersistedValue(upgradeVersionDisplayName.value, ""));
      }
      if (wizardData.status === "fulfilled") {
        setWizardUser(parsePersistedValue<{ userName?: string }>(wizardData.value, {}).userName || "");
      }
    } catch (error) {
      console.error("Failed to fetch upgrade state:", error);
    }
  };

  async function getUserUrl() {
    const persistedData = await ClusterApi.getPersistData(
      "USER_REDIRECTION_URL"
    );
    setUserUrl(persistedData);
  }

  async function getUserSettings() {
    try {
      const persistedData = await ClusterApi.getPersistData();
      setUserBgPreferences(parsePersistedValue(
        persistedData?.[`admin-settings-show-bg-${loginName}`],
        true,
      ));
      setUserTimezone(parsePersistedValue(
        persistedData?.[`admin-settings-timezone-${loginName}`],
        detectUserTimezone(),
      ));
    } catch (err) {
      console.error("Could not fetch user settings", err);
    }
  }
  async function setUserBgPreferencesData(value: boolean) {
    try {
      setUserBgPreferences(value);
      await ClusterApi.postPersistData(persistedPayload({
        ["admin-settings-show-bg-" + loginName]: value,
      }));
    } catch (err) {
      console.error("Could not fetch user bg preferences", err);
    }
  }

  useEffect(() => {
    async function moveAppToReadyState() {
      setAppLoaded(false);
      setInitializationError(null);
      const supportsKey = `user-pref-${loginName || ""}-supports`;
      try {
        const [savedSupports, wizardData] = await Promise.all([
          ClusterApi.getPersistData(supportsKey).catch(() => null),
          isOnlyViewUser
            ? Promise.resolve(null)
            : ClusterApi.getPersistData("wizard-data").catch(() => null),
        ]);
        setSupports({
          ...DEFAULT_SUPPORTS,
          ...parsePersistedValue(savedSupports, {}),
        });
        setWizardUser(parsePersistedValue<{ userName?: string }>(wizardData, {}).userName || "");

        await getAmbariProperties();
        await fetchClusterData();
        if (isOnlyViewUser) {
          setAppLoaded(true);
          return;
        }

        await getUserUrl().catch(() => undefined);
      } catch (error: any) {
        setInitializationError(
          error?.response?.data?.message || "Ambari could not initialize the application shell.",
        );
      }
    }
    void moveAppToReadyState();
  }, [initializationAttempt, isOnlyViewUser, loginName]);

  useEffect(() => {
    if (!isOnlyViewUser && !isEmpty(cluster) && cluster?.versionNum && cluster?.stack) {
      fetchServiceComponentInfo();
    }
  }, [cluster, isOnlyViewUser]);

  useEffect(() => {
    if (loginName && !isOnlyViewUser) {
      void getUserSettings();
    }
  }, [isOnlyViewUser, loginName]);

  useEffect(() => {
    const message = parsedSocketMessages[0];
    if (!message) return;

    if (get(message, "destination") === "/events/upgrade") {
      // Fix for flaky service actions dropdown during upgrade pause
      // Update upgrade state immediately from WebSocket message to prevent UI flicker
      if (get(message, "type") === "CREATE" || get(message, "type") === "UPDATE") {
        // Extract upgrade state directly from WebSocket message if available
        const upgradeStatus = get(message, "requestStatus") || get(message, "request_status");
        const suspended = get(message, "suspended");
        
        if (upgradeStatus) {
          setUpgradeState(upgradeStatus);
        }
        if (suspended !== undefined) {
          setUpgradeSuspend(suspended);
        }
      }
      fetchUpgradeStates();
      fetchStackVersionList();
    }

  }, [parsedSocketMessages]);

  useEffect(() => {
    if (isOnlyViewUser) {
      return;
    }
    let active = true;
    const destinations = [
      "/events/hostcomponents",
      "/events/alerts",
      "/events/ui_topologies",
      "/events/configs",
      "/events/services",
      "/events/hosts",
      "/events/alert_definitions",
      "/events/alert_group",
      "/events/upgrade",
      "/events/requests",
    ];
    client.onConnect = () => {
      hasConnectedSocket.current = true;
      setSocketClient(client as any);
      setIsSocketConnected(true);
      void fetchBackgroundOperations().catch(() => {
        // The existing snapshot remains visible until the next REST reconciliation.
      });
      destinations.forEach((destination) => {
        client.subscribe(destination, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            parsedMessage.destination = destination;
            if (destination === "/events/requests") {
              setBackgroundOperations((current) => (
                upsertRequestEvent(
                  current,
                  parsedMessage,
                  backgroundOperationsPageSizeRef.current,
                )
              ));
            }
            setParsedSocketMessages((current) => [parsedMessage, ...current].slice(0, 200));
          } catch {
            console.error(`Ambari ignored a malformed STOMP message from ${destination}.`);
          }
        });
      });
    };
    const switchInitialConnectionToSockJs = () => {
      if (!shouldFallbackToSockJs(useSockJs.current, hasConnectedSocket.current)) {
        return false;
      }
      useSockJs.current = true;
      void client.deactivate({ force: true }).then(() => {
        if (active) client.activate();
      });
      return true;
    };
    client.onStompError = (frame) => {
      console.error("Ambari STOMP broker error:", frame.headers.message, frame.body);
      switchInitialConnectionToSockJs();
    };
    client.onWebSocketError = () => {
      switchInitialConnectionToSockJs();
    };
    client.onDisconnect = () => {
      setSocketClient(null);
      setIsSocketConnected(false);
    };
    client.onWebSocketClose = () => {
      setSocketClient(null);
      setIsSocketConnected(false);
      switchInitialConnectionToSockJs();
    };
    client.activate();
    return () => {
      active = false;
      client.onConnect = () => undefined;
      client.onDisconnect = () => undefined;
      client.onWebSocketClose = () => undefined;
      client.onWebSocketError = () => undefined;
      void client.deactivate();
    };
  }, [client, fetchBackgroundOperations, isOnlyViewUser]);

  // Ember.js upgrade computed properties - implementing the same logic as ui/app/app.js
  const upgradeInit = upgradeState === "NOT_REQUIRED";
  const upgradeInProgress = upgradeState === "IN_PROGRESS";
  const upgradeCompleted = upgradeState === "COMPLETED";
  const upgradeHolding =
    upgradeState.includes("HOLDING") ||
    (upgradeState === "ABORTED" && !upgradeSuspend);
  const upgradeAborted = upgradeState === "ABORTED" && !upgradeSuspend;
  const upgradeSuspended = upgradeState === "ABORTED" && upgradeSuspend;
  const upgradeIsRunning = upgradeInProgress || upgradeHolding;
  const isNonWizardUser = Boolean(wizardUser && wizardUser !== loginName);
  const wizardIsNotFinished = upgradeIsRunning || upgradeSuspended || isNonWizardUser;

  return (
    <AppContext.Provider
      value={{
        state,
        dispatch,
        client: socketClient,
        isSocketConnected,
        parsedSocketMessages,
        clusterName,
        cluster,
        isAppLoaded,
        initializationError,
        retryInitialization: () => setInitializationAttempt((value) => value + 1),
        services,
        serviceComponentInfo,
        ambariProperties,
        ambariServerVersion,
        serverClock,
        supports,
        setSupports,
        isKerberosEnabled,
        stackConfigurations,
        allHostNames,
        upgradeState,
        setUpgradeState,
        upgradeDirection,
        setUpgradeDirection,
        upgradeSuspend,
        currentStackVersion,
        setCurrentStackVersion,
        upgradeId,
        setUpgradeId,
        isPatchUpgrade,
        setIsPatchUpgrade,
        upgradeVersionDisplayName,
        setUpgradeVersionDisplayName,
        userUrl,
        sessionExists: true,
        sessionsValidated: true,
        clusterState,
        userBgPreferences,
        setUserBgPreferences: setUserBgPreferencesData,
        syncUserBgPreferences: setUserBgPreferences,
        userTimezone,
        syncUserTimezone: setUserTimezone,
        // Background Operations
        backgroundOperations,
        setBackgroundOperations,
        updateBackgroundOperations,
        fetchBackgroundOperationsSnapshot: fetchBackgroundOperations,
        backgroundOperationsPageSize,
        setBackgroundOperationsPageSize,
        runningOperationsCount,
        upgradeInit,
        upgradeInProgress,
        upgradeCompleted,
        upgradeHolding,
        upgradeAborted,
        upgradeSuspended,
        upgradeIsRunning,
        upgradeIsFinalizeItem,
        setUpgradeIsFinalizeItem,
        wizardIsNotFinished,
        isNonWizardUser,
        wizardUser,
        isClusterInstalled,
        loginName: loginName || "",
        clockDistance,
        serviceCheckSupportedMap,
        stackVersion,
        stackVersionList,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
