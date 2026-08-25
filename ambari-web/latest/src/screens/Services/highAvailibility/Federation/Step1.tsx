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
  Alert,
  Button,
  Card,
  CardBody,
  Col,
  FormControl,
  Row,
  Stack,
} from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { EnableNamenodeFederationContext } from "./store/context";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMultiply } from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import { ActionTypes } from "./store/types";
import { messages } from "../../../messages";
import { get } from "lodash";
import useConfigsTags from "../../../../hooks/useConfigsTags";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getHdfsNamespaces } from "../haWorkflowUtils";
import { getStepData } from "../../../../Utils/Utility";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import { validateNameserviceId } from "./workflowUtils";
import Spinner from "../../../../components/Spinner";

function Step1() {
  const [newNameServiceId, setNewNameServiceId] = useState("");
  const {
    state,
    dispatch,
    stepWizardUtilities: { currentStep, handleNextImperitive },
    flushStateToDb,
  } = useContext(EnableNamenodeFederationContext);
  const [persistenceError, setPersistenceError] = useState("");
  const { allModelsLoaded, allServiceModels } = useContext(ServiceContext);
  const { configsData, configsError, isConfigsLoading, reloadConfigs } =
    useConfigsTags();
  const hdfsModel: any = allServiceModels.hdfs;
  const topologyReady = Boolean(
    allModelsLoaded && hdfsModel?.isNamespaceLoaded && !isConfigsLoading,
  );
  const modelNames = getHdfsNamespaces(hdfsModel).map(
    (namespace) => namespace.name,
  );
  const configuredNames = Array.isArray(configsData?.items)
    ? String(
        configsData.items.find((item: any) => item.type === "hdfs-site")
          ?.properties?.["dfs.nameservices"] || "",
      )
        .split(",")
        .map((name) => name.trim())
        .filter(Boolean)
    : [];
  const existingNameservices = [...new Set([...modelNames, ...configuredNames])];
  const nameError = newNameServiceId
    ? validateNameserviceId(
        newNameServiceId,
        existingNameservices,
        topologyReady,
      )
    : "";
  const isNextEnabled = Boolean(
    newNameServiceId && !nameError && !configsError && topologyReady,
  );

  useEffect(() => {
    const savedName = getStepData(
      state,
      enableNamenodeFederationSteps.GET_STARTED,
      "nameserviceIds.newNameServiceId",
      "enableNamenodeFederationSteps",
    );
    if (savedName) setNewNameServiceId(savedName);
  }, []);

  return (
    <>
      <h2 className="step-title">Get Started</h2>
      <h3 className="step-description light-text">
        {get(messages, "admin.nameNodeFederation.wizard.step1.body")}
      </h3>
      <Alert className="mt-2" variant="danger">
        {get(messages, "admin.nameNodeFederation.wizard.step1.alert")}
      </Alert>
      {configsError ? (
        <Alert variant="danger">
          {configsError}
          <Button size="sm" className="ms-3" onClick={reloadConfigs}>
            Retry
          </Button>
        </Alert>
      ) : null}
      {!configsError && !topologyReady ? (
        <Alert variant="info" className="d-flex align-items-center gap-2">
          <Spinner /> Loading the current HDFS namespace topology...
        </Alert>
      ) : null}
      {persistenceError ? <Alert variant="danger">{persistenceError}</Alert> : null}
      <Card className="mt-2">
        <CardBody>
          <Row className="align-items-center mb-2">
            <Col md={3} className="bolder">
              Existing Nameservice IDs:
            </Col>
            <Col md={4}>
              <div>{existingNameservices.join(", ")}</div>
            </Col>
          </Row>
          <Row className="align-items-center">
            <Col md={3} className="bolder">
              New Nameservice ID:
            </Col>
            <Col md={4}>
              <FormControl
                type="text"
                value={newNameServiceId}
                className={classNames({
                  "is-invalid": nameError,
                })}
                onChange={(e) => {
                  setNewNameServiceId(e.target.value);
                }}
              ></FormControl>
            </Col>
            <Col>
              {nameError ? (
                <Stack direction="horizontal">
                  <FontAwesomeIcon
                    icon={faMultiply}
                    color="red"
                  ></FontAwesomeIcon>
                  <div className="ms-2 text-muted text-nowrap">{nameError}</div>
                </Stack>
              ) : null}
            </Col>
          </Row>
        </CardBody>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onBack={() => {}}
        onNext={async () => {
          setPersistenceError("");
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                nameserviceIds: {
                  existingNameServiceId: existingNameservices.join(","),
                  newNameServiceId,
                },
              },
            },
          });
          try {
            await flushStateToDb("next");
            await handleNextImperitive();
          } catch (error: any) {
            setPersistenceError(
              error?.response?.data?.message ||
                error?.message ||
                "Ambari could not persist the wizard state.",
            );
          }
        }}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}

export default Step1;
