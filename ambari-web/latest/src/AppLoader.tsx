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

import { useContext, useEffect, useState } from "react";
import { AppContext } from "./store/context";
import { createHashRouter, RouterProvider } from "react-router-dom";
import RoutesList from "./router/RoutesList";
import { LocalStorageOps } from "./Utils/LocalStorageOps";
import clusterApi from "./api/clusterApi";
import { useLocation } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { toBePreservedPaths } from "./constants";
import ClusterApi from "./api/clusterApi";
import { ProgressBar } from "react-bootstrap";
import usePolling from "./hooks/usePolling";

// Separate component for route tracking that will be used inside router
export function RouteTracker() {
  const [lastVisitedURL] = useState(
    () => LocalStorageOps.getItem("lastVisitedURL") || "/#/installer/step0"
  );
  const [hasCluster, setHasCluster] = useState<boolean | null>(null);
  const [isClusterChecked, setIsClusterChecked] = useState(false);
  const unpreservedPaths = ["/", "/login"];

  const location = useLocation();
  const navigate = useNavigate();

  async function saveUserUrl() {
    const currentURL = location.pathname;
    await ClusterApi.postPersistData(
      JSON.stringify({
        USER_REDIRECTION_URL: currentURL,
      })
    );
  }

  // URL tracking effect
  useEffect(() => {
    const currentURL = location.pathname;
    console.log("currentURL", currentURL);
    if (!unpreservedPaths.includes(currentURL)) {
      LocalStorageOps.setItem("lastVisitedURL", "/#" + currentURL);
    }
    //Check if toBePreservedPaths has any key with location.pathname substring
    if (
      Object.keys(toBePreservedPaths).some((path) => currentURL.includes(path))
    ) {
      saveUserUrl();
    }
  }, [location]);

  // Check if cluster exists when component mounts
  useEffect(() => {
    async function checkCluster() {
      try {
        const response = await clusterApi.getClusterData();
        const firstCluster = response?.items?.[0]?.Clusters;
        if (firstCluster && firstCluster?.provisioning_state === "INSTALLED")
          setHasCluster(true);
      } catch (error) {
        console.error("Error checking cluster existence:", error);
        setHasCluster(false);
      } finally {
        setIsClusterChecked(true);
      }
    }
    checkCluster();
  }, []); // Run only once on mount

  // Handle routing after cluster check
  useEffect(() => {
    if (!isClusterChecked) return;

    const currentURL = location.pathname;
    const hashExclusiveUrl = LocalStorageOps.getItem("lastVisitedURL")?.replace(
      "/#",
      ""
    );
    let relocationPath = currentURL;
    if (hasCluster && currentURL.includes("/installer")) {
      relocationPath = "/main/dashboard/metrics";
    } else if (!hasCluster && currentURL.includes("/main")) {
      relocationPath = "/installer/step0";
    } else {
      if (currentURL === "/") {
        if (hashExclusiveUrl&&hasCluster) {
          relocationPath = hashExclusiveUrl;
        } else {
          if (hasCluster) {
            relocationPath = "/main/dashboard/metrics";
          } else {
            relocationPath = "/installer/step0";
          }
        }
      } else {
        relocationPath = currentURL;
      }
    }

    navigate(relocationPath);
  }, [isClusterChecked, hasCluster, lastVisitedURL]);

  useEffect(() => {
    if (hasCluster === null) {
      return;
    }
    const currentURL = location.pathname;
    if (hasCluster && currentURL.startsWith("/installer")) {
      navigate("/main/dashboard/metrics");
    } else if (!hasCluster && currentURL.startsWith("/main")) {
      navigate("/installer/step0");
    }
  }, [location, hasCluster]);

  return null;
}

function AppLoader() {
  const { isAppLoaded } = useContext(AppContext);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (isAppLoaded) {
      stopPolling();
    }
  }, [isAppLoaded]);
  function incrementProgress() {
    setProgress((p) => p + 20);
  }

  const { stopPolling } = usePolling(incrementProgress,500);

  const router = createHashRouter([...(RoutesList as any)]);
  if (!isAppLoaded) {
    return (
      <div className="vh-100 mt-0">
        <div className="p-5">
          <h2>Loading...</h2>
          <ProgressBar  variant="blue" now={progress} className="mt-3" />
        </div>
      </div>
    );
  }
  return (
    <>
      <RouterProvider router={router} />
    </>
  );
}

export default AppLoader;
