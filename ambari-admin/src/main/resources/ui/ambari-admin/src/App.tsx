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
import { SideItemLabels } from "./layout/SideItemList";
import SideBar from "./layout/SideBar";
import { Container, Card } from "react-bootstrap";
import { useState } from "react";
import NavBar from "./layout/NavBar";
import AppContent from "./context/AppContext";
import { Toaster } from "react-hot-toast";
import { HashRouter } from "react-router-dom";

function App() {
  const [selectedOption, setSelectedOption] = useState<string>(
    SideItemLabels.CLUSTERINFORMATION
  );
  const [rbacData, setRbacData] = useState({});
  const [ambariVersion, setAmbariVersion] = useState<string>("");
  const [permissionLabelList, setPermissionLabelList] = useState<string[]>([]);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  console.log("In App");

  return (
    <HashRouter>
      <AppContent.Provider
        value={{
          selectedOption,
          setSelectedOption,
          rbacData,
          setRbacData,
          permissionLabelList,
          setPermissionLabelList,
          ambariVersion,
          setAmbariVersion,
        }}
      >
        <Toaster />
        <div className="d-flex h-100" style={{ maxHeight: "100vh" }}>
          <SideBar
            clusterExists={false}
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
              clusterName={""}
            />
            <Container className="mt-4">
              <Card className="p-4 rounded-0">
                <Routes />
              </Card>
            </Container>
          </div>
        </div>
      </AppContent.Provider>
    </HashRouter>
  );
}

export default App;
