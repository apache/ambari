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
import { clearClientSession } from "./utils/session";
import Routes from "./router/Routes";
import { SideItemLabels } from "./SideItemList";
import SideBar from "./SideBar";
import { Container, Card, Alert, Button } from "react-bootstrap";
import { useEffect, useState } from "react";
import { ManagementProvider } from "./context/ManagementContext";
import NavBar from "./NavBar";
import AppContent from "./context/AppContext";
import { HostCluster } from "./types";
import { get } from "lodash";
import { Toaster } from "react-hot-toast";
import { HashRouter } from "react-router-dom";
import ClusterApi from "./api/clusterApi";
import Spinner from "./components/Spinner";
import usePolling from "./hooks/usePolling.ts";
import clusterApi from "./api/clusterApi";
import InactivityTimeout from "./InactivityTimeout.tsx";
import { latestAmbariUrl } from "./utils/navigation.ts";

function App() {
  const [clusterInfo, setClusterInfo] = useState<HostCluster>(
    {} as HostCluster
  );
  const [loading, setLoading] = useState(true);
  const [availableClusters, setAvailableClusters] = useState<HostCluster[]>([]);
  const [clusterError, setClusterError] = useState("");
  const [clusterLoadAttempt, setClusterLoadAttempt] = useState(0);
  const [selectedOption, setSelectedOption] = useState<string>(
    SideItemLabels.CLUSTEROVERVIEW
  );
  const [rbacData, setRbacData] = useState({});
  const [ambariVersion, setAmbariVersion] = useState<string>("");
  const [permissionLabelList, setPermissionLabelList] = useState<string[]>([]);
  const [clusterExists, setClusterExists] = useState(false);
  const [clusterInfoLoading, setClusterInfoLoading] = useState(true);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [userSessiontTimeout, setUserSessiontTimeout] = useState<number>();

  useEffect(() => {
    async function getUserTimeout() {
      try {
        const response = await clusterApi.getUserTimeout();
        if (response.status === 200) {
          const userTimeoutInSeconds = response.data.RootServiceComponents.properties["server.http.session.inactive_timeout"];
          setUserSessiontTimeout(userTimeoutInSeconds * 1000);
        }
      } catch (error) {

      }
    }
    getUserTimeout();
  }, []);

  async function pollNoopUserTimeout() {
    try {
      await clusterApi.noopPolling();
    } catch (error) {
      if ((error as { response?: { status?: number } }).response?.status === 401) {
        clearClientSession();
        window.location.replace(latestAmbariUrl("/login"));
      }
    }
  }

  usePolling(pollNoopUserTimeout, 10000);

  useEffect(() => {
    let active = true;
    async function loadClusters() {
      setLoading(true);
      setClusterError("");
      try {
        const data = await ClusterApi.hostClustersInfo();
        if (!Array.isArray(data?.items)) throw new Error("Invalid cluster response");
        const clusters: HostCluster[] = data.items.map((item: { Clusters: HostCluster }) => item.Clusters);
        const requestedName = new URLSearchParams(window.location.search).get("cluster");
        const selected = requestedName
          ? clusters.find((item) => item.cluster_name === requestedName)
          : clusters.length === 1 ? clusters[0] : undefined;
        if (requestedName && !selected) throw new Error("Cluster access unavailable");
        if (!active) return;
        setAvailableClusters(clusters);
        setClusterInfo(selected || {} as HostCluster);
        setClusterExists(selected?.provisioning_state === "INSTALLED");
        setClusterInfoLoading(false);
      } catch {
        if (active) setClusterError("Unable to load the selected cluster. It may be unavailable or your access may have changed.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void loadClusters();
    return () => { active = false; };
  }, [clusterLoadAttempt]);

  const selectCluster = (clusterName: string) => {
    const target = new URL(window.location.href);
    target.searchParams.set("cluster", clusterName);
    target.hash = "/clusterInformation";
    window.location.assign(target.href);
  };

  const updateClusterInfo = (next: HostCluster) => {
    setClusterInfo(next);
    setAvailableClusters((items) => items.map((item) => item.cluster_id === next.cluster_id ? next : item));
    const target = new URL(window.location.href);
    target.searchParams.set("cluster", next.cluster_name);
    window.history.replaceState(null, "", target.href);
  };

  if (loading) {
    return <Spinner />;
  }
  if (clusterError) {
    return <Alert variant="danger" className="m-4">
      <p>{clusterError}</p>
      <Button onClick={() => setClusterLoadAttempt((value) => value + 1)}>Retry</Button>
      <Button variant="link" href={latestAmbariUrl("/")}>Return to Ambari</Button>
    </Alert>;
  }
  return (
    <HashRouter>
      <AppContent.Provider
        value={{
          selectedOption,
          setSelectedOption,
          cluster: clusterInfo,
          setClusterInfo: updateClusterInfo,
          availableClusters,
          selectCluster,
          rbacData,
          setRbacData,
          permissionLabelList,
          setPermissionLabelList,
          clusterExists,
          clusterInfoLoading,
          ambariVersion,
          setAmbariVersion,
        }}
      >
        <ManagementProvider>
        <Toaster />
        <div className="d-flex h-100" style={{ maxHeight: "100vh" }}>
          <SideBar
            clusterExists={clusterExists}
            isSidebarCollapsed={isSidebarCollapsed}
            setIsSidebarCollapsed={setIsSidebarCollapsed}
            isRoot
          />
          <div
            className={`d-flex flex-column ${isSidebarCollapsed?"main-content-collapsed":"main-content"}`}
            style={{
              background: "#e6e6e6",
              maxHeight: "100%",
              overflowY: "scroll",
              height: "100%",
              position: "absolute",
              left: isSidebarCollapsed?"60px":"230px",
            }}
          >
            <NavBar
              subPath={selectedOption}
              clusterName={get(clusterInfo, "cluster_name", "")}
            />
            <Container fluid className="mt-4 px-4">
              <Card className="p-4 rounded-0"><Routes /></Card>
            </Container>
          </div>
        </div>
        </ManagementProvider>
      </AppContent.Provider>
      <InactivityTimeout timeout={userSessiontTimeout ?? 900} />
    </HashRouter>
  );
}

export default App;
