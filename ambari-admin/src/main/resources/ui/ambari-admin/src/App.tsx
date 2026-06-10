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
import Routes from "./router/Routes";
import { SideItemLabels } from "./SideItemList";
import SideBar from "./SideBar";
import { Container, Card } from "react-bootstrap";
import { useEffect, useState } from "react";
import NavBar from "./NavBar";
import AppContent from "./context/AppContext";
import { HostCluster } from "./types";
import { get } from "lodash";
import { Toaster } from "react-hot-toast";
import { HashRouter, Route } from "react-router-dom";
import ClusterApi from "./api/clusterApi";
import Spinner from "./components/Spinner";
import InstallClusterButton from "./components/InstallClusterButton.tsx";
import { Form } from "react-bootstrap";
import ClusterInformationNavigate from "./components/ClusterInformationNavigate.tsx";
import usePolling from "./hooks/usePolling.ts";
import clusterApi from "./api/clusterApi";
import InactivityTimeout from "./InactivityTimeout.tsx";
import InstallBox from "./assets/img/install-box.svg"

function App() {
  const [clusterInfo, setClusterInfo] = useState<HostCluster>(
    {} as HostCluster
  );
  const [loading, setLoading] = useState(false);
  const [selectedOption, setSelectedOption] = useState<string>(
    SideItemLabels.CLUSTERINFORMATION
  );
  const [rbacData, setRbacData] = useState({});
  const [ambariVersion, setAmbariVersion] = useState<string>("");
  const [permissionLabelList, setPermissionLabelList] = useState<string[]>([]);
  const [clusterExists, setClusterExists] = useState(false);
  const [clusterInfoLoading, setClusterInfoLoading] = useState(true);
  const [isInstallWizardLaunched, setInstallWizardLaunched] = useState(false);
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
      const response = await clusterApi.noopPolling();
      if (response.status === 403) {
        localStorage.clear();
        window.location.replace("/#/login");
      }
    } catch (error) {
      console.error("Error in noop polling", error);
      localStorage.clear();
      window.location.replace("/#/login");
    }
  }
  usePolling(pollNoopUserTimeout, 10000);

  useEffect(() => {
    async function getClusterInfoData() {
      setLoading(true);
      const data = await ClusterApi.hostClustersInfo();
      const hostClusterInfo = get(data, "items[0].Clusters", "");
      setClusterInfo(hostClusterInfo);
      setLoading(false);
    }
    getClusterInfoData();
  }, []);

  useEffect(() => {
    async function checkClusterExists() {
      console.log("checking cluster exists");
      const response = await ClusterApi.clusterInfo(
        "Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id"
      );
      if (
        response &&
        response.items &&
        response.items.length > 0 &&
        response.items[0].Clusters.provisioning_state === "INSTALLED"
      ) {
        setClusterExists(true);
        setClusterInfoLoading(false);
      } else {
        setClusterExists(false);
        setClusterInfoLoading(true);
      }
    }
    checkClusterExists();
  }, []);

  if (loading) {
    return <Spinner />;
  }
  const handleRedirectToInstallCluster = () => {
    setInstallWizardLaunched(true);
  };
  return (
    <HashRouter>
      <AppContent.Provider
        value={{
          selectedOption,
          setSelectedOption,
          cluster: clusterInfo,
          setClusterInfo,
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
            <Container className="mt-4">
                {clusterInfoLoading && !isInstallWizardLaunched && !clusterExists &&
                (window.location.hash.endsWith('/clusterInformation') || window.location.hash.endsWith('/')) ? (
                    <>
                        <Card.Title className="text-center text-dark mt-4">
                            Welcome to Apache Ambari
                        </Card.Title>
                        <Card.Text className="text-center text-muted mb-10 mt-3">
                            Provision a cluster, manage who can access the cluster, and customize views for Ambari users.
                        </Card.Text>
                    </>
                ) : null}
              <Card className="p-4 rounded-0">
                  {clusterInfoLoading && !isInstallWizardLaunched && !clusterExists &&
                  (window.location.hash.endsWith('/clusterInformation') || window.location.hash.endsWith('/')) ? (
                  <Form.Group className="d-flex flex-column justify-content-center align-items-center text-center">
                    <Card.Title>Create a Cluster</Card.Title>
                    <Card.Text className="text-muted">
                      Use the Install Wizard to select services and configure
                      your cluster
                    </Card.Text>
                    <Card.Img
                      variant="middle"
                      src={InstallBox}
                      width="100"
                      height="100"
                      alt="Install Box"
                    />
                    <InstallClusterButton
                      onButtonClick={handleRedirectToInstallCluster}
                      setInstallWizardLaunched={setInstallWizardLaunched}
                    />
                  </Form.Group>
                ) : null}
                <Route path="/clusterInformation">
                  <ClusterInformationNavigate
                    setInstallWizardLaunched={setInstallWizardLaunched}
                  />
                </Route>
                <Routes />
              </Card>
            </Container>
          </div>
        </div>
      </AppContent.Provider>
      <InactivityTimeout timeout={userSessiontTimeout ?? 900} />
    </HashRouter>
  );
}

export default App;
