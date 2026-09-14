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

import { useTranslation } from "react-i18next";
import { useContext, useEffect, useState } from "react";
import {
  Link,
  Navigate,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { Alert, Button, Modal, ProgressBar } from "react-bootstrap";
import { AppContext, AppProvider } from "./store/context";
import { AlertsProvider } from "./store/AlertsContext";
import { HostsListStateProvider } from "./store/HostsListStateContext";
import { ModalProvider } from "./store/ModalContext";
import { useAuth } from "./hooks/useAuth";
import usePolling from "./hooks/usePolling";
import ClusterApi from "./api/clusterApi";
import {
  consumePreferredPath,
  peekPreferredPath,
  recalledClusterId,
  rememberCluster,
  savePreferredPath,
} from "./Utils/authNavigation";
import CustomModal from "./store/CustomModal";
import DocumentTitleUpdater from "./components/DocumentTitleUpdater";
import InactivityTimeout from "./InactivityTimeout";
import LoginMessageModal from "./screens/Authentication/LoginMessageModal";
import {
  clusterProvisioningRedirect,
  isViewOnlyUser,
} from "./Utils/authPolicy";
import {
  clusterNameFromPath,
  clusterPath,
  legacyMainContinuation,
  normalizeLegacyMainPath,
} from "./Utils/clusterRoute";
import { clusterDraftPath } from "./Utils/scopedWorkflow";
import useClusterPath from "./hooks/useClusterPath";
import { applicationScopeKey } from "./Utils/runtimeIdentity";
import ClusterDirectory from "./screens/Directories/ClusterDirectory";

export function AuthenticatedApplication() {
  const {
    isAuthenticated,
    isLoading,
    loginMessage,
    retrySession,
    sessionError,
    canAccessCluster,
    user,
  } = useAuth();
  const location = useLocation();
  const requestedClusterName = clusterNameFromPath(location.pathname);

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

  if (
    location.pathname.startsWith("/clusters/")
    && (!requestedClusterName || !canAccessCluster(requestedClusterName))
  ) {
    return (
      <div className="container py-5" role="alert">
        <Alert variant="danger">
          <Alert.Heading>Cluster access unavailable</Alert.Heading>
          <p>
            This cluster does not exist or your account is not authorized to open it.
            No other cluster was selected.
          </p>
          <Link className="btn btn-outline-danger" to="/clusters">Choose a cluster</Link>
        </Alert>
      </div>
    );
  }

  return (
    <AppProvider
      key={applicationScopeKey(user?.user_name || "", requestedClusterName || undefined)}
      requestedClusterName={requestedClusterName || undefined}
    >
      <AlertsProvider>
        <HostsListStateProvider>
          <ModalProvider>
            <ApplicationLoader />
          </ModalProvider>
        </HostsListStateProvider>
      </AlertsProvider>
    </AppProvider>
  );
}

export function LandingRoute() {
  const { availableClusters } = useContext(AppContext);
  const { authorizations, canAccessCluster, hasGlobalAuthorization, user } = useAuth();
  const [preferredPath] = useState(peekPreferredPath);
  const [installerPath] = useState(() => clusterDraftPath());
  useEffect(() => { consumePreferredPath(); }, []);
  if (isViewOnlyUser(authorizations)) return <Navigate to="/main/view" replace />;

  const clusters = availableClusters.filter(
    (item) => canAccessCluster(item?.Clusters?.cluster_name),
  );
  if (preferredPath?.startsWith("/clusters/")) {
    const preferredCluster = clusterNameFromPath(preferredPath);
    if (preferredCluster && clusters.some((item) => item.Clusters.cluster_name === preferredCluster)) {
      return <Navigate to={preferredPath} replace />;
    }
  }
  const legacyPath = preferredPath ? normalizeLegacyMainPath(preferredPath) : undefined;
  const rememberedId = recalledClusterId(user?.user_name);
  const selected = clusters.find((item) => item.Clusters.cluster_id === rememberedId)
    || (clusters.length === 1 ? clusters[0] : undefined);
  if (selected) {
    return <Navigate to={clusterPath(selected.Clusters.cluster_name, legacyPath || undefined)} replace />;
  }
  if (clusters.length > 1) {
    return <ClusterSelectionModal clusters={clusters} continuation={legacyPath || undefined} />;
  }
  return hasGlobalAuthorization("AMBARI.ADD_DELETE_CLUSTERS")
    ? <Navigate to={installerPath} replace />
    : <Navigate to="/adminView" replace state={{ noClusterLanding: true }} />;
}

export function LegacyMainRedirect() {
  const { availableClusters } = useContext(AppContext);
  const { canAccessCluster, user } = useAuth();
  const location = useLocation();
  const continuation = legacyMainContinuation(location.pathname, location.search);
  const clusters = availableClusters.filter(
    (item) => canAccessCluster(item?.Clusters?.cluster_name),
  );
  const rememberedId = recalledClusterId(user?.user_name);
  const selected = clusters.find((item) => item.Clusters.cluster_id === rememberedId)
    || (clusters.length === 1 ? clusters[0] : undefined);
  if (selected) {
    return <Navigate replace to={clusterPath(selected.Clusters.cluster_name, continuation)} />;
  }
  return clusters.length
    ? <ClusterSelectionModal clusters={clusters} continuation={continuation} />
    : <LandingRoute />;
}

type ClusterSelection = { Clusters: { cluster_name: string; cluster_id: number } };

function ClusterSelectionModal({ clusters, continuation }: {
  clusters: ClusterSelection[];
  continuation?: string;
}) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { t } = useTranslation();
  return (
    <Modal show backdrop="static" keyboard={false} centered aria-labelledby="select-cluster-title">
      <Modal.Header><Modal.Title id="select-cluster-title">{t("clusterNavigation.choose")}</Modal.Title></Modal.Header>
      <Modal.Body>
        <p>{t("clusterNavigation.description")}</p>
        <div className="d-grid gap-2">
          {clusters.map(({ Clusters: cluster }) => (
            <Button key={cluster.cluster_id} variant="outline-primary" onClick={() => {
              rememberCluster(user?.user_name, cluster.cluster_id);
              navigate(clusterPath(cluster.cluster_name, continuation), { replace: true });
            }}>{cluster.cluster_name}</Button>
          ))}
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="link" onClick={async () => {
          await logout();
          navigate("/login", { replace: true });
        }}>{t("clusterNavigation.signOut")}</Button>
      </Modal.Footer>
    </Modal>
  );
}

export function ClusterChooser() {
  return <ClusterDirectory />;
}

export function RouteTracker() {
  const { cluster, isClusterInstalled } = useContext(AppContext);
  const { hasGlobalAuthorization, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const canAddDeleteClusters = hasGlobalAuthorization("AMBARI.ADD_DELETE_CLUSTERS");
  const scopedPath = useClusterPath();

  useEffect(() => {
    if (clusterNameFromPath(location.pathname) === cluster?.cluster_name) {
      rememberCluster(user?.user_name, cluster?.cluster_id);
    }
  }, [user?.user_name, cluster?.cluster_id, cluster?.cluster_name, location.pathname]);

  useEffect(() => {
    const clusterRelativePath = location.pathname.replace(/^\/clusters\/[^/]+/, "");
    const redirect = clusterProvisioningRedirect({
      canAddDeleteClusters,
      clusterInstalled: isClusterInstalled,
      clusterName: cluster?.cluster_name,
      pathname: clusterRelativePath,
    });
    if (redirect) {
      navigate(scopedPath(redirect), { replace: true });
    }
  }, [
    canAddDeleteClusters,
    cluster?.cluster_name,
    isClusterInstalled,
    location.pathname,
    navigate,
    scopedPath,
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
