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

import { Suspense} from "react";
import { Toaster } from "react-hot-toast";
import "./styles/app.scss";
import {AppProvider} from "./store/context";
import { UserProvider } from "./store/UserContext";
import useWebSocket from "react-use-websocket";
import Spinner from "./components/Spinner";
import AppLoader from "./AppLoader";
import { ModalProvider } from "./store/ModalContext";
import CustomModal from "./store/CustomModal";
import clusterApi from "./api/clusterApi";
import { Login } from "./screens/Authentication/Login";
import { useEffect, useState } from "react";
import InactivityTimeout from "./InactivityTimeout";
import { HelmetProvider } from "react-helmet-async";
import DocumentTitleUpdater from "./components/DocumentTitleUpdater";

function App() {
  const [userSessionTimeout, setUserSessionTimeout] = useState<number>(0);
  const socketUrl = "/api/stomp/v1/websocket";
  const { readyState: _readyState } = useWebSocket(socketUrl, {
    onOpen: () => console.log("opened"),
    //Will attempt to reconnect on all close events, such as server shutting down
    shouldReconnect: () => false,
  });

  // Fetch timeout value once when app loads
  useEffect(() => {
    const fetchTimeout = async () => {
      try {
        const response = await clusterApi.getUserTimeout();
        if (response.status === 200) {
          const timeoutInSeconds = response.data.RootServiceComponents.properties["server.http.session.inactive_timeout"];
          setUserSessionTimeout(timeoutInSeconds * 1000);
        }
      } catch (error) {
        console.error("Error fetching timeout:", error);
        setUserSessionTimeout(90000); // Default 90 seconds
      }
    };
    fetchTimeout();
  }, []);
  
  const isLoginPage = window.location.hash === '#/login';
  
  console.log("app")
  return (
    <HelmetProvider>
      <Suspense fallback={<Spinner />}>
        <UserProvider>
          {isLoginPage ? (
            <>
              <Login />
            </>
          ) : (
            <AppProvider>
              <ModalProvider>
                <DocumentTitleUpdater />
                <Toaster />
                {userSessionTimeout > 0 && (
                   <InactivityTimeout timeout={userSessionTimeout} />
                 )}
                <CustomModal />
                <AppLoader />
              </ModalProvider>
            </AppProvider>
          )}
        </UserProvider>
      </Suspense>
    </HelmetProvider>
  );
}

export default App;
