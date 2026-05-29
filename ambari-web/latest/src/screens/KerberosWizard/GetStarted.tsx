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

import { cloneDeep } from "lodash";
import { useContext, useEffect, useState } from "react";
import { Alert, Card, Form } from "react-bootstrap";
import { preconditionOptions } from "./constants";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./KerberosStore/types";
import { EnableKerberosContext } from "./KerberosStore/context"

type PreconditionOptions = {
  [key: string]: {
    Options: {
      [key: string]: boolean;
    };
  };
};

export default function GetStartedKerberos() {
  const {
    dispatch,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(EnableKerberosContext);

  const [preconditions, setPreconditions] = useState<PreconditionOptions>(preconditionOptions);
  const [selectedKdcPlan, setSelectedKdcPLan] = useState("Existing MIT KDC");
  //@ts-ignore
  const [validate, setValidate] = useState(false);
  const [nextEnabled, setNextEnabled ] = useState(false);

  const handleKdcPlanChange = (kdcPlan: string) => {
    setPreconditions(preconditionOptions);
    setSelectedKdcPLan(kdcPlan);
  };

  const enableNext = () => {
    setNextEnabled(true);
  };

  const disableNext = () => {
    setNextEnabled(false);
  };

  const handleOptionChange = (kdcPlan: string, option: string) => {
    const preconditionsCopy = cloneDeep(preconditions);
    preconditionsCopy[kdcPlan].Options[option] =
      !preconditionsCopy[kdcPlan].Options[option];
    setPreconditions(preconditionsCopy);
  };

  const isValid = () => {
    const selectedPlan = preconditions[selectedKdcPlan];
    if (selectedPlan.Options) {
      const flag = Object.values(selectedPlan.Options).every(
        (value) => value === true
      );
      if(flag) {
        enableNext();
        return flag;
      }
    }
    disableNext();
    return false;
  };

  useEffect(() => {
    setValidate(isValid());
  }, [preconditions, selectedKdcPlan]);


  return (
    <>
      <div className="p-4">
        <h4>Get Started</h4>
        <p>
          Welcome to the Ambari Security Wizard. Use this wizard to enable
          kerberos security in your cluster.
          <br />
          Let's get started
        </p>
        <Alert variant="warning">
          Note: This process requires services to be restarted and cluster
          downtime. As well, depending on the options you select, might require
          support from your Security administrators. Please plan accordingly.
        </Alert>
        <Card className="p-3">
          <p>What type of KDC do you plan on using?</p>
          <div className="p-1">
            {Object.keys(preconditions).map((kdcPlan: string) => {
              const radioId = `kdc-plan-${kdcPlan.replace(/\s+/g, '-').toLowerCase()}`;
              return (
                <div key={kdcPlan}>
                  <div className="d-flex">
                    <Form.Check
                      type="radio"
                      id={radioId}
                      name="kdcPlan"
                      className="mx-2"
                      checked={selectedKdcPlan === kdcPlan}
                      onChange={() => handleKdcPlanChange(kdcPlan)}
                      label={kdcPlan}
                    />
                  </div>
                </div>
              );
            })}
            <Card className="m-1 mt-3 blue-card">
              {preconditions[selectedKdcPlan].Options && (
                <div className="p-3">
                  <p>{selectedKdcPlan} :</p>
                  <p>
                    Following prerequisites needs to be checked to progress ahead
                    in the wizard.
                  </p>
                  <div className="mt-2">
                    {Object.keys(preconditions[selectedKdcPlan].Options).map(
                      (option: string) => {
                        const checkboxId = `prerequisite-${selectedKdcPlan.replace(/\s+/g, '-').toLowerCase()}-${option.replace(/\s+/g, '-').toLowerCase()}`;
                        return (
                          <div key={option} className="d-flex">
                            <Form.Check
                              type="checkbox"
                              id={checkboxId}
                              checked={
                                preconditions[selectedKdcPlan]?.Options[option]
                              }
                              onChange={() =>
                                handleOptionChange(selectedKdcPlan, option)
                              }
                              label={option}
                            />
                          </div>
                        );
                      }
                    )}
                  </div>
                </div>
              )}
            </Card>
          </div>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled}
        step={currentStep}
        onNext={() => {
          if(isValid()) {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: { step: currentStep.name, data: { selectedKdcPlan }}
            })
            flushStateToDb("next");
            handleNextImperitive();
          }
        }}
        onBack={() => {}}
        onCancel={() => {
          onExitPopUp(false, true );
        }}
      />
    </>
  );
}
