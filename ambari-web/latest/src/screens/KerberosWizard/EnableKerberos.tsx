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

// @ts-nocheck
import { useContext, useEffect, useState } from "react";
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
import ManageKdcCredentials from "../Kerberos/manageKdcCredentials";
import { useLocation, useNavigate } from "react-router-dom";
import { discardChanges, EnableKerberosContext } from "./KerberosStore/context";
import UpgradeGuard from "../../components/UpgradeGuard";

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

  const { clusterName } = useContext(AppContext);
  const {
    flushStateToDb
  } = useContext(EnableKerberosContext);
  const navigate = useNavigate();
  const location = useLocation();

  const isSecurityEnabled = async () => {
    try {
      const response = await KerberosApi.getSecurityType(clusterName);
      setIsKerberosEnabled(response.Clusters.security_type === "KERBEROS");
      setLoading(false);
    } catch (error) {
      console.error("Error fetching security configuration:", error);
    }
  };

  useEffect(() => {
    isSecurityEnabled();
  }, []);

  useEffect(() => {
    if (location.pathname.includes("/kerberos/enable")) {
      setEnableKerberosModal(true);
    } else {
      setEnableKerberosModal(false);
    }
  }, [location])

  const fetchCSVData = async () => {
    try {
      const response = await KerberosApi.downloadKerberosIdentitiesCsv(
        clusterName
      );
      const blob = new Blob([response], { type: "text/csv" });
      saveAs(blob, `kerberos.csv`);
    } catch (error) {
      console.error("Error downloading CSV:", error);
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

  return (
    <UpgradeGuard>
      {isKerberosEnabled ? (
        <div className="p-3">
          <div className="d-flex align-items-center">
            <h4 className="text-success mb-0 mr-2">
              Kerberos security is Enabled{" "}
            </h4>
            <Button
              className="mx-2"
              variant="warning"
              onClick={() => setDisableKerberosModal(true)}
            >
              DISABLE KERBEROS
            </Button>
            <Button
              onClick={() => {
                setKeytabsModal(true);
              }}
            >
              REGENERATE KEYTABS
            </Button>
            <Button
              onClick={() => {renderManageKdcCredential()}}
              className="mx-2">
              MANAGE KDC CREDENTIALS
            </Button>
            <Button className="mx-2" onClick={fetchCSVData}>
              DOWNLOAD CSV
            </Button>
          </div>
          <div>
            <KerberosIdentities/>
          </div>
        </div>
      ) : (
        <div className="d-flex align-items-center p-4">
          <p className="mt-3 mx-3">Kerberos security is disabled</p>{" "}
          <Button onClick={() => {
                navigate(`/main/admin/kerberos/enable/step1`);
              }
            }>
            Enable Kerberos
          </Button>
        </div>
      )}
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
          <KerberosWizard />
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
        successCallback={() => {
          setDisableKerberos(false);
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
        }}
        successCallback={() => {
          discardChanges(clusterName);
          setConfirmQuitWizardModal(false);
          setEnableKerberosModal(false);
          flushStateToDb("cancel");
        }}
        modalTitle="Confirmation"
        modalBody="Configuring Kerberos is in progress. Do you really want to exit the Enable Kerberos Wizard?"
        options={{ okButtonText: "EXIT ANYWAY" }}
      />
      {regenerateKeytabs ? (
        <RegenerateKeytabs
          missingHostCheck={missingHostCheck}
          restartComponentsCheck={restartComponentsCheck}
        />
      ) : null}
    </UpgradeGuard>
  );
}
