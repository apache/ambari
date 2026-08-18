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

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, Modal as ReactModal } from "react-bootstrap";
import Modal from "../../components/Modal";
import KerberosApi from "../../api/kerberosApi";
import Spinner from "../../components/Spinner";
import saveAs from "file-saver";
import { Form } from "react-bootstrap";
import RegenerateKeytabs from "../Kerberos/RegenerateKeytabs";
import DisableKerberos from "../Kerberos/DisableKerberos";
import KerberosWizard from "./index";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faXmark } from "@fortawesome/free-solid-svg-icons";
import { AppContext } from "../../store/context";
import KerberosIdentities from "./KerberosIdentities";
import modalManager from "../../store/ModalManager";
import {
  useBeforeUnload,
  useBlocker,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { discardChanges } from "./KerberosStore/context";
import UpgradeGuard from "../../components/UpgradeGuard";
import ManageKdcCredentials from "../Kerberos/manageKdcCredentials";
import credentialsUtils from "../../Utils/credentialsUtils";
import useKerberosMode from "../../hooks/useKerberosMode";
import { useAuth, useAuthorization } from "../../hooks/useAuth";
import { responseErrorMessage } from "../../Utils/httpError";
import {
  failedPreKerberizeChecks,
  kerberosWizardPersistenceResetPayload,
  kerberosWizardRecoveryPath,
  kerberosWizardStartPayload,
  shouldBlockKerberosWizardNavigation,
} from "../../Utils/kerberosWizard";
import { postKerberosWizardPersistData } from "../../Utils/kerberosWizardPersistence";

export default function EnableKerberos() {
  const [isKerberosEnabled, setIsKerberosEnabled] = useState(false);
  const [disbleKerberosModal, setDisableKerberosModal] = useState(false);
  const [disableKerberosModal2, setDisableKerberosModal2] = useState(false);
  const [enableKerberosModal, setEnableKerberosModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [keytabsModal, setKeytabsModal] = useState(false);
  const [keytabsModal2, setKeytabsModal2] = useState(false);
  const [regenerateKeytabs, setRegenerateKeytabs] = useState(false);
  const [disableKerberos, setDisableKerberos] = useState(false);
  const [missingHostCheck, setMissingHostCheck] = useState(false);
  const [restartComponentsCheck, setRestartComponentsCheck] = useState(false);
  const [quitDisableKerberosModal, setQuitDisableKerberosModal] =
    useState(false);
  const [disableKerberosInProgress, setDisableKerberosInProgress] =
    useState(false);
  const [confirmQuitWizardModal, setConfirmQuitWizardModal] = useState(false);
  const [securityLoadError, setSecurityLoadError] = useState("");
  const [csvError, setCsvError] = useState("");
  const [credentialStorePersistent, setCredentialStorePersistent] =
    useState(false);
  const [identitiesEditing, setIdentitiesEditing] = useState(false);
  const [preCheckFailures, setPreCheckFailures] = useState<any[]>([]);
  const [preCheckError, setPreCheckError] = useState("");
  const [preCheckRunning, setPreCheckRunning] = useState(false);
  const [wizardExitError, setWizardExitError] = useState("");
  const [wizardExitRunning, setWizardExitRunning] = useState(false);
  const allowWizardExit = useRef(false);
  const disableRouteStarted = useRef(false);

  const { clusterName, clusterState, services, supports } = useContext(AppContext);
  const { user } = useAuth();
  const canDownloadCsv = useAuthorization("CLUSTER.UPGRADE_DOWNGRADE_STACK");
  const {
    isLoaded: isKerberosModeLoaded,
    isManualKerberos,
    loadError: kerberosModeError,
    retry: retryKerberosMode,
  } = useKerberosMode();
  const navigate = useNavigate();
  const location = useLocation();
  const isEnableWizardRoute = location.pathname.includes("/kerberos/enable/");
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    shouldBlockKerberosWizardNavigation(
      currentLocation.pathname,
      nextLocation.pathname,
      allowWizardExit.current,
    ),
  );

  const finishWizardExit = () => {
    allowWizardExit.current = true;
    setConfirmQuitWizardModal(false);
    setEnableKerberosModal(false);
    if (blocker.state === "blocked") {
      blocker.proceed();
    } else {
      navigate("/main/admin/kerberos/", { replace: true });
    }
  };

  useBeforeUnload((event) => {
    if (isEnableWizardRoute && !allowWizardExit.current) {
      event.preventDefault();
      event.returnValue = "";
    }
  });

  useEffect(() => {
    if (blocker.state === "blocked") {
      setConfirmQuitWizardModal(true);
    }
  }, [blocker.state]);

  const isSecurityEnabled = async () => {
    setLoading(true);
    setSecurityLoadError("");
    try {
      const response = await KerberosApi.getSecurityType(clusterName);
      setIsKerberosEnabled(response.Clusters.security_type === "KERBEROS");
    } catch (error) {
      setSecurityLoadError(
        responseErrorMessage(
          error,
          "Ambari could not load the cluster security status.",
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    isSecurityEnabled();
  }, [clusterName]);

  useEffect(() => {
    let active = true;
    if (!isKerberosEnabled || isManualKerberos || kerberosModeError) {
      setCredentialStorePersistent(false);
      return () => {
        active = false;
      };
    }

    void credentialsUtils.isStorePersisted(clusterName)
      .then((isPersistent) => {
        if (active) {
          setCredentialStorePersistent(isPersistent);
        }
      })
      .catch(() => {
        if (active) {
          setCredentialStorePersistent(false);
        }
      });
    return () => {
      active = false;
    };
  }, [clusterName, isKerberosEnabled, isManualKerberos, kerberosModeError]);

  useEffect(() => {
    if (location.pathname.includes("/kerberos/enable")) {
      setEnableKerberosModal(true);
    } else {
      setEnableKerberosModal(false);
    }
  }, [location])

  useEffect(() => {
    const recoveryPath = kerberosWizardRecoveryPath(clusterState);
    if (recoveryPath && !location.pathname.includes("/kerberos/enable/")) {
      navigate(recoveryPath, { replace: true });
    }
  }, [clusterState, location.pathname, navigate]);

  const fetchCSVData = async () => {
    setCsvError("");
    try {
      const response = await KerberosApi.downloadKerberosIdentitiesCsv(
        clusterName
      );
      const blob = new Blob([response], { type: "text/csv" });
      saveAs(blob, `kerberos.csv`);
    } catch (error) {
      setCsvError(
        responseErrorMessage(
          error,
          "Ambari could not download the Kerberos identities CSV.",
        ),
      );
    }
  };

  const hasYarn = services.some(
    (service) => service.ServiceInfo?.service_name === "YARN",
  );

  const enterEnableKerberosWizard = async () => {
    setPreCheckError("");
    if (!user?.user_name) {
      setPreCheckError("Ambari could not identify the Kerberos wizard owner.");
      return;
    }
    try {
      await postKerberosWizardPersistData(
        kerberosWizardStartPayload(user.user_name),
      );
      setDisableKerberosModal(false);
      navigate(`/main/admin/kerberos/enable/step1`);
    } catch (error) {
      setPreCheckError(responseErrorMessage(
        error,
        "Ambari could not initialize Enable Kerberos recovery state.",
      ));
    }
  };

  const continueEnableKerberos = () => {
    if (hasYarn) {
      setDisableKerberosModal(true);
    } else {
      void enterEnableKerberosWizard();
    }
  };

  const startEnableKerberos = async () => {
    setPreCheckError("");
    setPreCheckFailures([]);
    if (!supports.preKerberizeCheck) {
      continueEnableKerberos();
      return;
    }

    setPreCheckRunning(true);
    try {
      const response = await KerberosApi.runPreKerberizeChecks();
      const failures = failedPreKerberizeChecks(response);
      if (failures.length > 0) {
        setPreCheckFailures(failures);
        return;
      }
      continueEnableKerberos();
    } catch (error) {
      setPreCheckError(responseErrorMessage(
        error,
        "Ambari could not run the pre-Kerberize checks.",
      ));
    } finally {
      setPreCheckRunning(false);
    }
  };

  const startDisableKerberos = () => {
    if (hasYarn) {
      setDisableKerberosModal(true);
    } else {
      setDisableKerberosModal2(true);
    }
  };

  useEffect(() => {
    const isDisableRoute = location.pathname.endsWith(
      "/kerberos/disableSecurity",
    );
    if (!isDisableRoute) {
      disableRouteStarted.current = false;
      return;
    }
    if (loading || securityLoadError || disableRouteStarted.current) {
      return;
    }
    if (!isKerberosEnabled) {
      navigate("/main/admin/kerberos/", { replace: true });
      return;
    }
    disableRouteStarted.current = true;
    if (hasYarn) {
      setDisableKerberosModal(true);
    } else {
      setDisableKerberosModal2(true);
    }
  }, [
    hasYarn,
    isKerberosEnabled,
    loading,
    location.pathname,
    navigate,
    securityLoadError,
  ]);

  const regenerateAfterIdentitySave = () => {
    setMissingHostCheck(false);
    setRestartComponentsCheck(false);
    if (isManualKerberos) {
      setRegenerateKeytabs(true);
    } else {
      setKeytabsModal2(true);
    }
  };

  const renderManageKdcCredential = () => {
    modalManager.show(<ManageKdcCredentials 
      isOpen={true} 
      onClose={() => modalManager.hide()} />
    )
  }

  if (loading) {
    return <Spinner />;
  }

  if (securityLoadError) {
    return (
      <Alert variant="danger" className="m-3">
        <div>{securityLoadError}</div>
        <Button className="mt-3" onClick={() => void isSecurityEnabled()}>
          Retry
        </Button>
      </Alert>
    );
  }

  return (
    <UpgradeGuard>
      {preCheckError && (
        <Alert variant="danger" className="m-3">
          <div>{preCheckError}</div>
          <Button
            className="mt-3"
            disabled={preCheckRunning}
            onClick={() => void startEnableKerberos()}
          >
            Retry
          </Button>
        </Alert>
      )}
      {isKerberosEnabled ? (
        <div className="p-3">
          {kerberosModeError && (
            <Alert variant="danger">
              <div>{kerberosModeError}</div>
              <Button className="mt-3" onClick={retryKerberosMode}>
                Retry
              </Button>
            </Alert>
          )}
          {csvError && <Alert variant="danger">{csvError}</Alert>}
          <div className="d-flex align-items-center">
            <h4 className="text-success mb-0 mr-2">
              Kerberos security is Enabled{" "}
            </h4>
            <Button
              className="mx-2"
              variant="warning"
              disabled={identitiesEditing}
              onClick={startDisableKerberos}
            >
              DISABLE KERBEROS
            </Button>
            {isKerberosModeLoaded && !kerberosModeError && !isManualKerberos && (
              <Button
                disabled={identitiesEditing || regenerateKeytabs}
                onClick={() => setKeytabsModal(true)}
              >
                REGENERATE KEYTABS
              </Button>
            )}
            {isKerberosModeLoaded &&
              !kerberosModeError &&
              !isManualKerberos &&
              credentialStorePersistent && (
                <Button
                  onClick={renderManageKdcCredential}
                  className="mx-2"
                >
                  MANAGE KDC CREDENTIALS
                </Button>
              )}
            {canDownloadCsv && (
              <Button className="mx-2" onClick={() => void fetchCSVData()}>
                DOWNLOAD CSV
              </Button>
            )}
          </div>
          <div>
            <KerberosIdentities
              onEditModeChange={setIdentitiesEditing}
              onIdentitiesSaved={regenerateAfterIdentitySave}
            />
          </div>
        </div>
      ) : (
        <div className="d-flex align-items-center p-4">
          <p className="mt-3 mx-3">Kerberos security is disabled</p>{" "}
          <Button
            disabled={preCheckRunning}
            onClick={() => void startEnableKerberos()}
          >
            {preCheckRunning ? "Checking..." : "Enable Kerberos"}
          </Button>
        </div>
      )}
      <Modal
        isOpen={preCheckFailures.length > 0}
        onClose={() => setPreCheckFailures([])}
        modalTitle="Pre-Kerberize Checks"
        modalBody={
          <div>
            <Alert variant="danger">
              The cluster must pass all pre-Kerberize checks before the wizard can start.
            </Alert>
            <ul>
              {preCheckFailures.map((item, index) => {
                const check = item.UpgradeChecks ?? {};
                return (
                  <li key={check.id ?? check.check ?? index}>
                    <strong>{check.check ?? check.id ?? "Kerberos check"}</strong>
                    {check.reason ? `: ${check.reason}` : ""}
                  </li>
                );
              })}
            </ul>
          </div>
        }
        successCallback={() => setPreCheckFailures([])}
        options={{ cancelableViaBtn: false, okButtonText: "CLOSE" }}
      />
      <ReactModal show={enableKerberosModal} className="modal-wizard">
        <ReactModal.Header className="d-flex justify-content-between align-items-center">
          <ReactModal.Title>Enable Kerberos Wizard</ReactModal.Title>
          <div>
            <FontAwesomeIcon
              className="ms-auto icon-click"
              icon={faXmark}
              onClick={() => setConfirmQuitWizardModal(true)}
            />
          </div>
        </ReactModal.Header>
        <ReactModal.Body>
          <KerberosWizard onWizardExitReady={finishWizardExit} />
        </ReactModal.Body>
      </ReactModal>
      <Modal
        isOpen={disbleKerberosModal}
        onClose={() => setDisableKerberosModal(false)}
        modalTitle="Warning"
        modalBody="YARN log and local dir will be deleted and ResourceManager state will be formatted as part of Enabling/Disabling Kerberos?"
        successCallback={() => {
          if (isKerberosEnabled) {
            setDisableKerberosModal(false);
            setDisableKerberosModal2(true);
          } else {
            void enterEnableKerberosWizard();
          }
        }}
        options={{ okButtonText: "PROCEED ANYWAY" }}
      />
      <Modal
        isOpen={disableKerberosModal2}
        onClose={() => {
          setDisableKerberosModal2(false);
        }}
        successCallback={() => {
          setDisableKerberosModal2(false);
          setDisableKerberos(true);
        }}
        modalTitle="Confirmation"
        modalBody={
          <Alert variant="warning">
            You are about to disable Kerberos on the cluster. This will stop all
            the services in your cluster and remove the Kerberos configurations.
            Are you sure you wish to proceed with disabling Kerberos?
          </Alert>
        }
        options={{ cancelableViaIcon: true }}
      />
      <Modal
        isOpen={keytabsModal}
        onClose={() => {
          setKeytabsModal(false);
        }}
        modalTitle="Regenerate Keytabs"
        modalBody={
          <div>
            <Alert variant="warning">
              Regenerating keytabs for <strong>all</strong> hosts in the cluster
              is a disruptive operation, and requires all components to be
              restarted. Optionally, keytabs can be regenerated{" "}
              <strong>only</strong> for missing hosts and components, and this
              operation requires selectively restarting those affected hosts and
              services.
            </Alert>
            <Form.Check
              type="checkbox"
              id="missing-host-keytabs-checkbox"
              label="Only regenerate keytabs for missing hosts and components"
              checked={missingHostCheck}
              onChange={() => setMissingHostCheck(!missingHostCheck)}
            />
          </div>
        }
        successCallback={() => {
          setKeytabsModal(false);
          setKeytabsModal2(true);
        }}
        options={{ okButtonText: "OK" }}
      />
      <Modal
        isOpen={keytabsModal2}
        onClose={() => {
          setKeytabsModal2(false);
        }}
        modalTitle="Regenerate Keytabs"
        modalBody={
          <div>
            <Alert variant="warning">
              After keytab regenerate is complete, services relying on them{" "}
              <strong>must</strong> be restarted. This can be done
              automatically, or manually.
            </Alert>
            <Form.Check
              type="checkbox"
              id="restart-components-keytabs-checkbox"
              label="Automatically restart components after keytab regeneration"
              checked={restartComponentsCheck}
              onChange={() => {
                setRestartComponentsCheck(!restartComponentsCheck);
              }}
            />
          </div>
        }
        successCallback={() => {
          setKeytabsModal2(false);
          setRegenerateKeytabs(true);
        }}
        options={{ okButtonText: "OK" }}
      />
      <Modal
        isOpen={disableKerberos}
        onClose={() => {
          setQuitDisableKerberosModal(true);
        }}
        modalTitle=""
        modalBody={
          <DisableKerberos
            setDisableKerberosInProgress={setDisableKerberosInProgress}
          />
        }
        successCallback={async () => {
          setDisableKerberos(false);
          navigate("/main/admin/kerberos/", { replace: true });
          await isSecurityEnabled();
        }}
        options={{
          okButtonText: "Complete",
          cancelableViaBtn: false,
          cancelableViaIcon: true,
          modalSize: "modal-lg",
          okButtonDisabled: disableKerberosInProgress,
        }}
      />
      <Modal
        isOpen={quitDisableKerberosModal}
        onClose={() => setQuitDisableKerberosModal(false)}
        successCallback={() => {
          setQuitDisableKerberosModal(false);
          setDisableKerberos(false);
          navigate("/main/admin/kerberos/", { replace: true });
        }}
        modalTitle="Confirmation"
        modalBody="You are in the process of disabling security on your cluster. Are you sure you want to quit?
"
        options={{}}
      />
      <Modal
        isOpen={confirmQuitWizardModal}
        onClose={() => {
          setConfirmQuitWizardModal(false);
          if (blocker.state === "blocked") {
            blocker.reset();
          }
        }}
        successCallback={async () => {
          if (wizardExitRunning) {
            return;
          }
          setWizardExitRunning(true);
          setWizardExitError("");
          try {
            if (!location.pathname.endsWith("/step8")) {
              await discardChanges(clusterName);
            }
            await postKerberosWizardPersistData(
              kerberosWizardPersistenceResetPayload(),
            );
            finishWizardExit();
          } catch (error) {
            setWizardExitError(responseErrorMessage(
              error,
              "Ambari could not clear the Kerberos wizard recovery state.",
            ));
          } finally {
            setWizardExitRunning(false);
          }
        }}
        modalTitle="Confirmation"
        modalBody={
          <div>
            {wizardExitError && <Alert variant="danger">{wizardExitError}</Alert>}
            Configuring Kerberos is in progress. Do you really want to exit the Enable Kerberos Wizard?
          </div>
        }
        options={{
          okButtonText: "EXIT ANYWAY",
          okButtonDisabled: wizardExitRunning,
        }}
      />
      {regenerateKeytabs ? (
        <RegenerateKeytabs
          missingHostCheck={missingHostCheck}
          restartComponentsCheck={restartComponentsCheck}
          onFinished={() => setRegenerateKeytabs(false)}
        />
      ) : null}
    </UpgradeGuard>
  );
}
