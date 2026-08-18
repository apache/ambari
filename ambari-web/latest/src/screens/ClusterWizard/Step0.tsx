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
import {
  Card,
  CardBody,
  Col,
  Form,
  OverlayTrigger,
  Popover,
} from "react-bootstrap";
import { ActionTypes } from "./clusterStore/types";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ContextWrapper } from ".";
import { get } from "lodash";

function Step0({ wizardName = "clusterCreation" }) {
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  }: any = useContext(Context);
  const MAX_CLUSTER_NAME_LENGTH = 80;
  const [errorMessage, setErrorMessage] = useState("");
  const [clusterName, setClusterName] = useState("");
  const [isClusterNameValid, setIsClusterNameValid] = useState(false);
  const [nextEnabled, setNextEnabled] = useState(false);
  const { t } = useTranslation();

  useEffect(() => {
    setClusterName(
      get(state, `${wizardName}Steps.${currentStep.name}.data.clusterName`, "")
    );
  }, [state]);

  useEffect(() => {
    if (validateClusterName(clusterName)) {
      setNextEnabled(true);
    }
  }, [clusterName]);

  const enableNext = () => {
    setNextEnabled(true);
  };

  const disableNext = () => {
    setNextEnabled(false);
  };

  const validateClusterName = (name: string) => {
    if (!name) {
      disableNext();
      return false;
    } else if (name.length > MAX_CLUSTER_NAME_LENGTH) {
      disableNext();
      setErrorMessage("Cluster name is too long");
      return false;
    } else if (/\s/.test(name)) {
      disableNext();
      setErrorMessage("Cluster Name cannot contain whitespace.");
      return false;
    } else if (/[^\w\s]/gi.test(name)) {
      disableNext();
      setErrorMessage("Cluster Name cannot contain special characters.");
      return false;
    } else {
      setErrorMessage("");
      enableNext();
      return true;
    }
  };

  const handleInputChange = (inputString: string) => {
    setClusterName(inputString);
    setIsClusterNameValid(validateClusterName(inputString));
  };

  return (
    <>
      <div className="d-flex flex-column">
        <div className="step-title">{t("installer.step0.getStarted")}</div>
        <div className="step-description">{t("installer.step0.description")}</div>
        <Card className="mb-4 mt-2">
          <CardBody>
            <span> {t("installer.step0.nameClusterInstruction")}</span>
            <OverlayTrigger
              trigger={["hover", "focus"]}
              key="learn more"
              placement="right"
              overlay={
                <Popover id="popover-positioned-right">
                  <Popover.Header as="h3">
                    {t("installer.step0.clusterNamePlaceholder")}
                  </Popover.Header>
                  <Popover.Body>
                    {t("installer.step0.uniqueCluster")}
                  </Popover.Body>
                </Popover>
              }
            >
              <span className="text-info"> {t("common.learnMore")}</span>
            </OverlayTrigger>
            <Col className="d-flex my-2">
              <Form.Control
                type="text"
                placeholder={t("installer.step0.clusterNamePlaceholder")}
                onChange={(e) => handleInputChange(e.target.value)}
                style={{ width: "25%" }}
                value={clusterName}
              />
              {!isClusterNameValid && errorMessage && (
                <div className="mt-2 mx-2 d-flex">
                  <FontAwesomeIcon icon={faCircleXmark} />
                  <p className="text-danger mx-2">{errorMessage}</p>
                </div>
              )}
            </Col>
          </CardBody>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled}
        step={currentStep}
        onNext={async () => {
          if (validateClusterName(clusterName)) {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: { step: currentStep.name, data: { clusterName } },
            });
            await Promise.resolve(flushStateToDb("next"));
            handleNextImperitive();
          }
        }}
        onCancel={() => void flushStateToDb("cancel")}
        onBack={() => {}}
      />
    </>
  );
}
export default Step0;
