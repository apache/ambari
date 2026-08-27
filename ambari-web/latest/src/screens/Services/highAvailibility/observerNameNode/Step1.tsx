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
  Card,
  CardBody,
  Col,
  FormControl,
  Row,
  Stack,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMultiply } from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { AddObserverNamenodeContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { messages } from "../../../messages";
import { get } from "lodash";
import useConfigsTags from "../../../../hooks/useConfigsTags";

function Step1() {
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const [nameServiceIds, setNameServiceIds] = useState<string[]>([]);
  const [existingNameServiceId, setExistingNameServiceId] = useState("");
  const [selectedNameServiceId, setSelectedNameServiceId] = useState("");
  const [nameError, setNameError] = useState("");
  const {
    dispatch,
    stepWizardUtilities: { currentStep, handleNextImperitive },
    flushStateToDb,
  } = useContext(AddObserverNamenodeContext);
  const { configsData } = useConfigsTags();

  useEffect(() => {
    getExistingNameServiceIds();
  }, [configsData]);

  useEffect(() => {
    if (!selectedNameServiceId) {
      setNameError("");
      setIsNextEnabled(false);
      return;
    }
    if (nameServiceIds.includes(selectedNameServiceId)) {
      setNameError("");
      setIsNextEnabled(true);
    } else {
      setIsNextEnabled(false);
      setNameError(
        get(messages, "admin.observerNameNode.wizard.step1.nameserviceid.error")
      );
    }
  }, [selectedNameServiceId, nameServiceIds]);

  const getExistingNameServiceIds = () => {
    let nameService = "";
    if (configsData && Array.isArray(configsData.items)) {
      configsData.items.forEach((item: any) => {
        if (
          item.type === "hdfs-site" &&
          item.properties &&
          item.properties["dfs.nameservices"]
        ) {
          nameService = item.properties["dfs.nameservices"];
        }
      });
    }
    setExistingNameServiceId(nameService);
    const ids = nameService
      .split(",")
      .map((id: string) => id.trim())
      .filter((id: string) => id);
    setNameServiceIds(ids);
  };

  return (
    <>
      <h2 className="step-title">
        {get(messages, "admin.observerNameNode.wizard.step1.header")}
      </h2>
      <h3 className="step-description light-text">
        {get(messages, "admin.observerNameNode.wizard.step1.body")}
      </h3>
      <Alert className="mt-2" variant="warning">
        {get(messages, "admin.observerNameNode.wizard.step1.alert")}
      </Alert>
      <Card className="mt-2">
        <CardBody>
          <Row className="align-items-center mb-2">
            <Col md={3} className="bolder">
              {get(
                messages,
                "admin.observerNameNode.wizard.step1.nameserviceid.existing"
              )}
              :
            </Col>
            <Col md={4}>
              <div>{existingNameServiceId}</div>
            </Col>
          </Row>
          <Row className="align-items-center">
            <Col md={3} className="bolder">
              {get(
                messages,
                "admin.observerNameNode.wizard.step1.nameserviceid"
              )}
              :
            </Col>
            <Col md={4}>
              <FormControl
                type="text"
                value={selectedNameServiceId}
                className={classNames({ "is-invalid": nameError })}
                onChange={(e) => setSelectedNameServiceId(e.target.value)}
              />
            </Col>
            <Col>
              {nameError ? (
                <Stack direction="horizontal">
                  <FontAwesomeIcon icon={faMultiply} color="red" />
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
        onNext={() => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                nameServiceId: selectedNameServiceId,
              },
            },
          });
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step1;
