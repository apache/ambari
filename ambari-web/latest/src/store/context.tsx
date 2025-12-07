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
import { get, isEmpty, isString, map, set } from "lodash";
import ConfigsApi from "../api/configsApi";
import {
//   mapStackConfigProperties,
  redirectToLogin,
} from "../Utils/Utility";
import LoginApi from "../api/loginApi";
import { db } from "../Utils/db";
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
  upgradeIsRunning: boolean;
  upgradeSuspended: boolean;
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
  upgradeIsFinalizeItem: false,
  setUpgradeIsFinalizeItem: () => {},
  sessionExists: false,
  sessionsValidated: false,
  clusterState: {},
  upgradeIsRunning: false,
  upgradeSuspended: false,
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
  const [serviceComponentInfo, setServiceComponentInfo] = useState<any>({});
  const [upgradeState, setUpgradeState] = useState<string>("");
  const [upgradeDirection, setUpgradeDirection] = useState<string>("");
  const [upgradeSuspend, setUpgradeSuspend] = useState<boolean>(false);
  const [upgradeId, setUpgradeId] = useState<number>(0);
  const [upgradeIsFinalizeItem, setUpgradeIsFinalizeItem] = useState<boolean>(false);
  const [isPatchUpgrade, setIsPatchUpgrade] = useState<boolean>(false);
  const [upgradeVersionDisplayName, setUpgradeVersionDisplayName] = useState<string>("");
  const [currentStackVersion, setCurrentStackVersion] = useState<string>("");
  const [ambariProperties, setAmbariProperties] = useState({});
  const [sessionsValidated, setSessionsValidated] = useState(false);
  const [sessionExists, setSessionExists] = useState(false);
  const [clusterState, setClusterState] = useState({});
  const [userUrl, setUserUrl] = useState("");
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

  const [allHostNames, setAllHostNames] = useState([]);

  // TODO: These will be implemented soon to check upgrade status
  const upgradeIsRunning = false;
  const upgradeSuspended = false;

  const fetchClusterServices = async () => {
    try {
      const clusterServices = await ChooseServicesApi.servicesList(clusterName);
      setServices(clusterServices.items);
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
    if (clusterName) {
      fetchClusterServices();
      fetchAllHostNames();
      fetchUpgradeStates();
    }
  }, [clusterName]);

  useEffect(() => {
    async function fetchStackConfigs() {
      const stack = get(cluster, "version", "").split("-")[0];
      const version = get(cluster, "version", "").split("-")[1];
      const serviceNames = map(services, "ServiceInfo.service_name").join(",");
      if (stack && version && serviceNames) {
        //@ts-ignore
        const response = await ConfigsApi.getServiceConfigurations(
          stack,
          version,
          serviceNames
        );
        //TODO: Uncomment this once mapStackConfigProperties is defined
        // const stackConfigs = mapStackConfigProperties(response);
        const stackConfigs:never[]=[];

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
      setCluster(clusterData?.items[0]?.Clusters);
      setIsKerberosEnabled(
        clusterData?.items?.[0]?.Clusters?.security_type === "KERBEROS"
      );
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
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
    try {
      const data = await ClusterApi.getHosts(clusterName);
      const hostNames = data.items.map((item: any) => item.Hosts.host_name);
      setAllHostNames(hostNames);
    } catch (error) {
      console.log("Error getting hosts");
    }
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
    const upgradeDirection = get(response, `items[${lastItemIndex}].Upgrade.direction`, "UPGRADE");
    setUpgradeDirection(upgradeDirection);
    setUpgradeId(upgradeId);
    setUpgradeState(upgradeState);
    setUpgradeSuspend(upgradeSuspend);

    const isPatch = await ClusterApi.getPersistData("isPatchUpgrade");
    setIsPatchUpgrade(isPatch);

    const isFinalizeItem = await ClusterApi.getPersistData("upgradeIsFinalizeItem");
    setUpgradeIsFinalizeItem(isFinalizeItem);

    const upgradeVersionDisplayName = await ClusterApi.getPersistData("upgradeVersionDisplayName");
    setUpgradeVersionDisplayName(upgradeVersionDisplayName);
  };

  async function getUserUrl() {
    const persistedData = await ClusterApi.getPersistData(
      "USER_REDIRECTION_URL"
    );
    setUserUrl(persistedData);
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
        } catch (err) {
        } finally {
          setAppLoaded(true);
        }
      } else {
      }
    }
    moveAppToReadyState();
  }, []);

  useEffect(() => {
    if (!isEmpty(cluster)&&cluster?.versionNum&&cluster?.stack) {
      fetchServiceComponentInfo();
    }
  }, [cluster]);

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

  // add a  method to check if clusterExists if yes return clusterName from API otherwise return empty string

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
        upgradeIsFinalizeItem,
        setUpgradeIsFinalizeItem,
        userUrl,
        sessionExists,
        sessionsValidated,
        clusterState,
        upgradeIsRunning,
        upgradeSuspended,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
