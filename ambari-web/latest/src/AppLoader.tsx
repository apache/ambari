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
import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Alert, Button, ProgressBar } from "react-bootstrap";
import { AppContext, AppProvider } from "./store/context";
import { ModalProvider } from "./store/ModalContext";
import { useAuth } from "./hooks/useAuth";
import useAuthorizationPolicy from "./hooks/useAuthorizationPolicy";
import usePolling from "./hooks/usePolling";
import ClusterApi from "./api/clusterApi";
import { toBePreservedPaths } from "./constants";
import {
  consumePreferredPath,
  normalizeInternalPath,
  savePreferredPath,
} from "./Utils/authNavigation";
import CustomModal from "./store/CustomModal";
import DocumentTitleUpdater from "./components/DocumentTitleUpdater";
import InactivityTimeout from "./InactivityTimeout";
import LoginMessageModal from "./screens/Authentication/LoginMessageModal";
import {
  clusterProvisioningRedirect,
  isViewOnlyUser,
  selectLandingPath,
} from "./Utils/authPolicy";

export function AuthenticatedApplication() {
  const {
    isAuthenticated,
    isLoading,
    loginMessage,
    retrySession,
    sessionError,
  } = useAuth();
  const location = useLocation();

  useEffect(() => {
    if (!isLoading && !isAuthenticated && location.pathname !== "/") {
      savePreferredPath(`${location.pathname}${location.search}`);
    }
  }, [isAuthenticated, isLoading, location.pathname, location.search]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    let stopped = false;
    let timeoutId: ReturnType<typeof setTimeout> | undefined;
    const keepAlive = async () => {
      try {
        await ClusterApi.noopPolling();
      } catch {
        // The global response handler owns authentication failures.
      } finally {
        if (!stopped) {
          timeoutId = setTimeout(keepAlive, 60_000);
        }
      }
    };
    timeoutId = setTimeout(keepAlive, 60_000);
    return () => {
      stopped = true;
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [isAuthenticated]);

  if (isLoading) {
    return <div className="p-5"><h2>Loading...</h2></div>;
  }
  if (sessionError) {
    return (
      <div className="container py-5">
        <Alert variant="danger">
          <Alert.Heading>Unable to validate the Ambari session</Alert.Heading>
          <p>{sessionError}</p>
          <Button variant="outline-danger" onClick={() => void retrySession()}>
            Retry
          </Button>
        </Alert>
      </div>
    );
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (loginMessage) {
    return <LoginMessageModal />;
  }

  return (
    <AppProvider>
      <ModalProvider>
        <ApplicationLoader />
      </ModalProvider>
    </AppProvider>
  );
}

export function LandingRoute() {
  const { cluster, isClusterInstalled } = useContext(AppContext);
  const { authorizations } = useAuth();
  const preferredPath = consumePreferredPath();
  const landingPath = selectLandingPath({
    clusterInstalled: Boolean(isClusterInstalled),
    clusterName: cluster?.cluster_name,
    preferredPath,
    viewOnly: isViewOnlyUser(authorizations),
  });
  return (
    <Navigate
      to={landingPath}
      replace
      state={landingPath === "/adminView" ? { noClusterLanding: true } : undefined}
    />
  );
}

export function RouteTracker() {
  const { cluster, isClusterInstalled } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const { isAuthorized } = useAuthorizationPolicy();
  const location = useLocation();
  const navigate = useNavigate();
  const canAddDeleteClusters = hasAuthorization("AMBARI.ADD_DELETE_CLUSTERS");
  const canPersistRoute = isAuthorized("CLUSTER.MANAGE_USER_PERSISTED_DATA");

  useEffect(() => {
    const currentPath = normalizeInternalPath(`${location.pathname}${location.search}`);
    if (!currentPath || location.pathname === "/") {
      return;
    }

    savePreferredPath(currentPath);
    if (
      canPersistRoute
      && Object.keys(toBePreservedPaths).some((path) => location.pathname.includes(path))
    ) {
      void ClusterApi.postPersistData({ USER_REDIRECTION_URL: currentPath });
    }
  }, [canPersistRoute, location.pathname, location.search]);

  useEffect(() => {
    const redirect = clusterProvisioningRedirect({
      canAddDeleteClusters,
      clusterInstalled: isClusterInstalled,
      clusterName: cluster?.cluster_name,
      pathname: location.pathname,
    });
    if (redirect) {
      navigate(redirect, { replace: true });
    }
  }, [
    canAddDeleteClusters,
    cluster?.cluster_name,
    isClusterInstalled,
    location.pathname,
    navigate,
  ]);

  return null;
}

function ApplicationLoader() {
  const { initializationError, isAppLoaded, retryInitialization } = useContext(AppContext);
  const [progress, setProgress] = useState(0);
  const { stopPolling } = usePolling(
    () => setProgress((value) => Math.min(value + 10, 90)),
    500,
  );

  useEffect(() => {
    if (isAppLoaded) {
      stopPolling();
    }
  }, [isAppLoaded, stopPolling]);

  if (initializationError) {
    return (
      <div className="container py-5">
        <Alert variant="danger">
          <Alert.Heading>Unable to initialize Ambari</Alert.Heading>
          <p>{initializationError}</p>
          <Button variant="outline-danger" onClick={retryInitialization}>Retry</Button>
        </Alert>
      </div>
    );
  }
  if (!isAppLoaded) {
    return (
      <div className="vh-100 mt-0">
        <div className="p-5">
          <h2>Loading...</h2>
          <ProgressBar variant="blue" now={progress} className="mt-3" />
        </div>
      </div>
    );
  }

  return (
    <>
      <RouteTracker />
      <DocumentTitleUpdater />
      <InactivityTimeout />
      <CustomModal />
      <Outlet />
    </>
  );
}

export default ApplicationLoader;
