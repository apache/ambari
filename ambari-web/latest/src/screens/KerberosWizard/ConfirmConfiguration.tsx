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

import { Alert, Button, Card } from "react-bootstrap";
import { kdcProperties } from "./constants";
import { saveAs } from "file-saver";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import KerberosApi from "../../api/kerberosApi";
import { useContext, useState } from "react";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context"
import { get } from "lodash";
import { AppContext } from "../../store/context";
import { isManualKdcPlan } from "../../Utils/kerberosWizard";
import { responseErrorMessage } from "../../Utils/httpError";

export default function ConfirmConfiguration() {

  const {
    state,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: { currentStep, handleNextImperitive, wizardSteps, handleBackImperitive},
  } = useContext(EnableKerberosContext);

  const kdcType = get(state, `kerberosWizardSteps.${wizardSteps[1].name}.data.selectedKdcPlan`, {})
  const kdcProps: { [key: string]: string[] } = kdcProperties;

  const configs: ConfigPropertiesType = get(state, `kerberosWizardSteps.${wizardSteps[2].name}.data.configProperties`, {})
  const { clusterName } = useContext(AppContext);
  const [isDownloading, setIsDownloading] = useState(false);
  const [nextEnabled, setNextEnabled ] = useState(true)
  const [downloadError, setDownloadError] = useState("");

  const fetchCSVData = async () => {
    setIsDownloading(true);
    setNextEnabled(false)
    setDownloadError("");
    try {
      const response = await KerberosApi.downloadKerberosIdentitiesCsv(
        clusterName
      );
      const blob = new Blob([response], { type: "text/csv" });
      saveAs(blob, `kerberos.csv`);
    } catch (error) {
      setDownloadError(responseErrorMessage(
        error,
        "Ambari could not download the Kerberos identities CSV.",
      ));
    }
    setIsDownloading(false);
    setNextEnabled(true)
  };

  return (
    <>
      <div className="p-3">
        <h4>Confirm Configurations</h4>
        <p>Please review the configuration before continuing the setup process</p>
        <p>
          {isManualKdcPlan(kdcType)
            ? "Download the required principals and keytabs. Do not continue until they have been created and distributed to the cluster hosts."
            : "Using the Download CSV button, you can download a CSV file containing the principals and keytabs that Ambari will create."}
        </p>
        {isManualKdcPlan(kdcType) && (
          <Alert variant="warning">
            Ambari cannot verify that manually managed principals and keytabs have been distributed.
          </Alert>
        )}
        {downloadError && <Alert variant="danger">{downloadError}</Alert>}

        <Card className="p-4">
          <Card className="p-4 grey-card">
            {(kdcProps[kdcType] ?? []).map((property: string) => {
              const [name, section] = property.split(":");
              const config = configs?.KERBEROS?.[section]?.properties?.[name];
              if (config?.value === undefined || config?.value === "") {
                return null;
              }
              return (
                <div key={property} className="mb-2">
                  {config.propertyDisplayname || name}: {String(config.value)}
                </div>
              );
            })}
          </Card>
          <div className="mt-4 d-flex justify-content-end">
            <Button
              className="mx-1"
              variant="outline-secondary"
              onClick={() => onExitPopUp(false, false)}
            >
              EXIT WIZARD
            </Button>
            <Button variant="outline-primary" disabled={isDownloading} onClick={fetchCSVData}>
              DOWNLOAD CSV
            </Button>
          </div>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled}
        step={currentStep}
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
        onCancel={() => {
          onExitPopUp(false, false);
        }}
      />
    </>
  );
}
