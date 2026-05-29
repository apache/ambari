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

import { Button, Card } from "react-bootstrap";
import { kdcProperties } from "./constants";
import { saveAs } from "file-saver";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import KerberosApi from "../../api/kerberosApi";
import { useContext, useState } from "react";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context"
import { get } from "lodash";
import { AppContext } from "../../store/context";

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

  const fetchCSVData = async () => {
    setIsDownloading(true);
    setNextEnabled(false)
    try {
      const response = await KerberosApi.downloadKerberosIdentitiesCsv(
        clusterName
      );
      const blob = new Blob([response], { type: "text/csv" });
      saveAs(blob, `kerberos.csv`);
    } catch (error) {
      console.error("Error downloading CSV:", error);
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
          Using the Download CSV button, you can download a csv file which
          contains a list of the principals and keytabs that will automatically be
          created by Ambari.
        </p>

        <Card className="p-4">
          <Card className="p-4 grey-card">
            {kdcProps[kdcType].map((property: string) => {
              const [name, section] = property.split(":");
              return (
                <div key={property} className="mb-2">
                  {
                    configs["KERBEROS"][section].properties[name]
                      .propertyDisplayname
                  }{" "}
                  : {configs["KERBEROS"][section].properties[name].value}
                </div>
              );
            })}
          </Card>
          <div className="mt-4 d-flex justify-content-end">
            <Button className="mx-1" variant="outline-secondary">
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