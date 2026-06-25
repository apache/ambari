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
  useEffect,
  useReducer,
  useState,
} from "react";
import { State, Action } from "./types";
import { reducer, initialState } from "./reducer";
import { Client } from "@stomp/stompjs";
import ClusterApi from "../api/clusterApi";
import { ChooseServicesApi } from "../api/chooseServicesApi";
import { ServicesApi } from "../api/servicesApi";
import { get, isEmpty, isString, isUndefined, map, set } from "lodash";
import ConfigsApi from "../api/configsApi";
import { mapStackConfigProperties, redirectToLogin } from "../Utils/Utility";
import LoginApi from "../api/loginApi";
import { db } from "../Utils/db";
import useAuth from "../hooks/useAuth";
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
  serviceComponentInfo: any;
  isKerberosEnabled: boolean;
  stackConfigurations: any;
  allHostNames: string[];
  ambariProperties: any;
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
  // Background Operations - persistent cache like Ember.js singleton
  backgroundOperations: any[];
  setBackgroundOperations: (operations: any[]) => void;
  updateBackgroundOperations: (newRequests: any[]) => void;
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
  isClusterInstalled?: boolean;
}

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
  serviceComponentInfo: {},
  isKerberosEnabled: false,
  stackConfigurations: [],
  allHostNames: [],
  ambariProperties: {},
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
  userBgPreferences: false,
  setUserBgPreferences: () => {},
  // Background Operations defaults
  backgroundOperations: [],
  setBackgroundOperations: () => {},
  updateBackgroundOperations: () => {},
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
  isClusterInstalled: false,
});

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const [isSocketConnected, setIsSocketConnected] = useState(false);
  const [socketClient, setSocketClient] = useState(null);
  const [isAppLoaded, setAppLoaded] = useState(false);
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
  const [sessionsValidated, setSessionsValidated] = useState(false);
  const [sessionExists, setSessionExists] = useState(false);
  const [clusterState, setClusterState] = useState({});
  const [userUrl, setUserUrl] = useState("");
  const { user } = useAuth();
  const loginName = user?.user_name;
  const client = new Client({
    brokerURL: "/api/stomp/v1/websocket", // 'ws://localhost:15674/ws'
    debug: function (str) {
      console.log(str);
    },
    reconnectDelay: 1000,
    heartbeatIncoming: 1000,
    heartbeatOutgoing: 1000,
  });
  const [services, setServices] = useState([]);
  const [stackConfigurations, setStackConfigurations] = useState([]);
  const [userBgPreferences, setUserBgPreferences] = useState(false);
  const [allHostNames, setAllHostNames] = useState([]);

  // Background Operations - persistent cache like Ember.js singleton
  const [backgroundOperations, setBackgroundOperations] = useState<any[]>([]);

  const isUpgradeRequest = (request: any): boolean => {
    const context =
      request?.Requests?.request_context || request?.request_context;
    return context
      ? /(upgrading|downgrading)/.test(context.toLowerCase())
      : false;
  };

  const fetchBackgroundOperations = async () => {
    if (!clusterName || !isClusterInstalled) {
      return;
    }

    const allClusterRequests = await ClusterApi.getRequests(clusterName, 20);
    const newRequests = allClusterRequests.items.filter((request: any) => {
      return !isUpgradeRequest(request);
    });

    updateBackgroundOperations(newRequests);
  };

  const updateBackgroundOperations = (newRequests: any[]) => {
    const currentRequestIds: string[] = [];
    const updatedRequests = [...backgroundOperations];

    newRequests.forEach((newRequest: any) => {
      currentRequestIds.push(newRequest.Requests.id);
      const existingRequestIndex = updatedRequests.findIndex(
        (existing: any) => existing.Requests.id === newRequest.Requests.id
      );

      if (existingRequestIndex >= 0) {
        // Update existing request (like Ember.js rq.setProperties)
        updatedRequests[existingRequestIndex] = newRequest;
      } else {
        // Add new request to the beginning (like Ember.js unshift)
        updatedRequests.unshift(newRequest);
      }
    });

    // Remove old requests that are no longer in the API response (like Ember.js removeOldRequests)
    const finalRequests = updatedRequests.filter((request: any) =>
      currentRequestIds.includes(request.Requests.id)
    );

    // Sort by request ID descending (like Ember.js sortProperty('id').reverse())
    finalRequests.sort((a: any, b: any) => b.Requests.id - a.Requests.id);

    setBackgroundOperations(finalRequests);
  };

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
    fetchClusterState();
  }, []);

  useEffect(() => {
    if (clusterName && isClusterInstalled) {
      fetchClusterServices();
      fetchAllHostNames();
      fetchUpgradeStates();
      fetchBackgroundOperations();
    } else if (!isUndefined(isClusterInstalled) && !isClusterInstalled) {
      setAppLoaded(true);
    }
  }, [clusterName, isClusterInstalled]);

  useEffect(() => {
    if (!clusterName || !isClusterInstalled) return;

    let pollTimeout: NodeJS.Timeout | null = null;
    let isPollingActive = true;

    const poll = async () => {
      if (!isPollingActive) return;

      try {
        await fetchBackgroundOperations();
      } catch (error) {
        console.error('Error polling background operations:', error);
      } finally {
        // Schedule next poll ONLY after current request completes
        if (isPollingActive) {
          pollTimeout = setTimeout(poll, 30000); // Poll every 30 seconds like Ember.js
        }
      }
    };

    // Start initial poll
    poll();

    return () => {
      isPollingActive = false;
      if (pollTimeout) {
        clearTimeout(pollTimeout);
      }
    };
  }, [clusterName, isClusterInstalled]);

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
      }
    }
    fetchStackConfigs();
  }, [services, cluster]);

  const fetchClusterName = async () => {
    try {
      const name = await ClusterApi.getClusterName();
      setClusterName(name);
    } catch (error) {
      console.error("Failed to fetch cluster name:", error);
    }
  };
  const fetchClusterData = async () => {
    try {
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
      setCluster(clusterInfo);
      setIsKerberosEnabled(
        clusterData?.items?.[0]?.Clusters?.security_type === "KERBEROS"
      );

      const isInstalled = clusterInfo?.provisioning_state === "INSTALLED";
      setIsClusterInstalled(isInstalled);
    } catch (error) {
      setIsClusterInstalled(false);
    }
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
    setAmbariProperties(response);
  };
  const fetchUserInfo = async () => {
    try {
      try {
        let username = "";
        const ambariLocalData = db.getItem("ambari");
        if (ambariLocalData) {
          let parsedData = {};
          try {
            parsedData = JSON.parse(db.getItem("ambari") || "{}");
            if (isString(parsedData)) {
              parsedData = JSON.parse(parsedData);
            }
          } catch (err) {
            console.log("Error parsing ambari data", err);
            parsedData = {};
          }

          let ambari: any = parsedData;
          if (ambari?.app?.loginName) {
            username = ambari.app.loginName;
            // If we already have a username, we can consider the user authenticated
            if (!username) {
              window.location.href = "/#/login";
            }
            setSessionsValidated(true);
            setSessionExists(true);
          }
        } else {
          redirectToLogin();
        }
        // If we don't have user info in localStorage

        //fetch initial LS value for key ambari with empty fields
        let initialAmbariLsData: any = { app: {} };

        //set login name in the app object within the ambari object
        initialAmbariLsData.app.loginName = encodeURIComponent(username);
        initialAmbariLsData.app.authenticated = true;
        const params = { usr: "", loginName: encodeURIComponent(username) };
        const response = await LoginApi.handleSuccessfulLogin(params);
        initialAmbariLsData.app.user = response.data.Users;
        //convert JS object to JSON String and then encrypt the JSON String
        initialAmbariLsData = JSON.stringify(initialAmbariLsData);
        //encrypt the data and store it in Local Storage
        db.setItem("ambari", initialAmbariLsData);
        return true;
      } catch (error) {
        setSessionsValidated(true);
        setSessionExists(false);
        return false;
      }
    } catch (err) {
      console.log("Error in fetching user info", err);
      setSessionsValidated(true);
      setSessionExists(false);
      return false;
    }
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

      const isPatch = await ClusterApi.getPersistData("isPatchUpgrade");
      setIsPatchUpgrade(isPatch);

      const isFinalizeItem = await ClusterApi.getPersistData(
        "upgradeIsFinalizeItem"
      );
      setUpgradeIsFinalizeItem(isFinalizeItem);

      const upgradeVersionDisplayName = await ClusterApi.getPersistData(
        "upgradeVersionDisplayName"
      );
      setUpgradeVersionDisplayName(upgradeVersionDisplayName);
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

  async function getUserBgPreferences() {
    try {
      const persistedData = await ClusterApi.getPersistData(
        "admin-settings-show-bg-" + loginName
      );
      if (!!persistedData) {
        setUserBgPreferences(true);
      } else {
        setUserBgPreferences(false);
      }
    } catch (err) {
      console.error("Could not fetch user bg preferences", err);
    }
  }
  async function setUserBgPreferencesData(value: boolean) {
    try {
      setUserBgPreferences(value);
      await ClusterApi.postPersistData(
        JSON.stringify({
          ["admin-settings-show-bg-" + loginName]: `${value}`,
        })
      );
    } catch (err) {
      console.error("Could not fetch user bg preferences", err);
    }
  }

  useEffect(() => {
    async function moveAppToReadyState() {
      const isAuthenticated = await fetchUserInfo();
      setSessionsValidated(true);
      setSessionExists(true);
      if (isAuthenticated) {
        await fetchClusterName();
        await fetchClusterData();
        await getAmbariProperties();
        try {
          await getUserUrl();
        } catch (err) {}
      } else {
      }
    }
    moveAppToReadyState();
  }, []);

  useEffect(() => {
    if (!isEmpty(cluster) && cluster?.versionNum && cluster?.stack) {
      fetchServiceComponentInfo();
    }
  }, [cluster]);

  useEffect(() => {
    if (loginName) {
      getUserBgPreferences();
    }
  }, [loginName]);

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
    }

    // Handle background operations real-time updates
    if (get(message, "destination") === "/events/requests") {
      const requestContext = message.requestContext;
      // Skip upgrade requests (same logic as in BackgroundOperations component)
      if (
        requestContext &&
        /(upgrading|downgrading)/.test(requestContext.toLowerCase())
      ) {
        return;
      }

      // Only update if we have background operations to update
      if (backgroundOperations.length > 0) {
        const updatedOperations = [...backgroundOperations];
        const matchingRequestIndex = updatedOperations.findIndex(
          (existing: any) => existing.Requests.id === message.requestId
        );

        if (matchingRequestIndex >= 0) {
          // Transform WebSocket message to match API format
          const { Tasks, ...restProperties } = message;
          const newRequestBody: any = {};
          for (const property in restProperties) {
            const transformedPropertyName = property
              .replace(/([A-Z])/g, "_$1")
              .toLowerCase();
            newRequestBody[transformedPropertyName] = restProperties[property];
          }

          // Update existing request
          updatedOperations[matchingRequestIndex] = {
            ...updatedOperations[matchingRequestIndex],
            Requests: newRequestBody,
          };

          setBackgroundOperations(updatedOperations);
        }
        setBackgroundOperations(updatedOperations);
      } else {
        fetchBackgroundOperations();
      }
    }
  }, [parsedSocketMessages.length]);

  client.onConnect = function () {
    setSocketClient(client as any);
    setIsSocketConnected(true);
    client.subscribe("/events/requests", (message: any) => {
      // called when the client receives a STOMP message from the server
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          //TODO: parsedSocketMessages can exceed to very long list, need to limit it to some constant
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      } else {
      }
    });
    client.subscribe("/events/services", (message: any) => {
      // called when the client receives a STOMP message from the server
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          //TODO: parsedSocketMessages can exceed to very long list, need to limit it to some constant
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      } else {
      }
    });
    client.subscribe("/events/upgrade", (message: any) => {
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          set(parsedMessage, "destination", message.headers.destination);
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      }
    });
    client.subscribe("/events/hosts", (message: any) => {
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          set(parsedMessage, "destination", message.headers.destination);
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      } else {
      }
    });
    client.subscribe("/events/requests", (message: any) => {
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          set(parsedMessage, "destination", message.headers.destination);
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      } else {
      }
    });
    client.subscribe("/events/hostcomponents", (message: any) => {
      if (message.body) {
        try {
          const parsedMessage = JSON.parse(message.body);
          set(parsedMessage, "destination", message.headers.destination);
          setParsedSocketMessages((prevMessages) => [
            parsedMessage,
            ...prevMessages,
          ]);
        } catch {
          console.log("Error in parsing socket message");
        }
      } else {
      }
    });
  };

  client.onStompError = function (frame: any) {
    // Will be invoked in case of error encountered at Broker
    console.error("Broker reported error: " + frame.headers["message"]);
    console.error("Additional details: " + frame.body);
  };

  useEffect(() => {
    client.activate();
  }, []);

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
  // TODO: Add wizardWatcherController.isNonWizardUser check when available
  const wizardIsNotFinished = upgradeIsRunning || upgradeSuspended;

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
        services,
        serviceComponentInfo,
        ambariProperties,
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
        sessionExists,
        sessionsValidated,
        clusterState,
        userBgPreferences,
        setUserBgPreferences: setUserBgPreferencesData,
        // Background Operations
        backgroundOperations,
        setBackgroundOperations,
        updateBackgroundOperations,
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
        isClusterInstalled,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
