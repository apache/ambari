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
  OverlayTrigger,
  Popover,
  Row,
  Stack,
} from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { EnableHighAvailibilityContext } from "./store/context";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMultiply } from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import { ActionTypes } from "./store/types";
import { AppContext } from "../../../../store/context";
import ConfigsApi from "../../../../api/configsApi";
import { getStepData } from "../../../../Utils/Utility";
import { getHdfsUser } from "../haWorkflowUtils";

function Step1() {
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const [nameserviceId, setNameserviceId] = useState("");
  const {
    state,
    dispatch,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
    flushStateToDb
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const [hdfsUser, setHdfsUser] = useState("hdfs");
  const [nameError, setNameError] = useState("");
  const isHawqInstalled = services.some(
    (service: any) => service?.ServiceInfo?.service_name === "HAWQ",
  );
  useEffect(() => {
    const savedData = getStepData(
      state,
      "GET_STARTED",
      "",
      "enableHighAvailibilitySteps",
    );
    if (savedData?.nameserviceId) setNameserviceId(savedData.nameserviceId);
    if (savedData?.hdfsUser) setHdfsUser(savedData.hdfsUser);
  }, [state]);
  useEffect(() => {
    async function loadHdfsUser() {
      try {
        const configData = await ConfigsApi.getConfigValues(clusterName, "HDFS");
        setHdfsUser(getHdfsUser(configData));
      } catch {
        setHdfsUser((currentUser) => currentUser || "hdfs");
      }
    }
    if (clusterName) void loadHdfsUser();
  }, [clusterName]);
  useEffect(() => {
    if (nameserviceId) {
      let nameSarviceIdRegex =
        /^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])$/;
      if (nameSarviceIdRegex.test(nameserviceId)) {
        setNameError("");
        setIsNextEnabled(true);
      } else {
        setIsNextEnabled(false);
        setNameError(
          "Must consist of letters, numbers, and hyphens. Cannot begin or end with a hyphen."
        );
      }
    }
    if (!nameserviceId) {
      setNameError("");
      setIsNextEnabled(false);
    }
  }, [nameserviceId]);
  return (
    <>
      <h3 className="step-title">Get Started</h3>
      <p className="step-description light-text">
        This wizard will walk you through enabling NameNode HA on your
        cluster.Once enabled, you will be running a Standby NameNode in addition
        to your Active NameNode.This allows for an Active-Standby NameNode
        configuration that automatically performs failover.The process to enable
        HA involves a combination of <b>automated steps</b> (that will be
        handled by the wizard) and manual steps (that you must perform in
        sequence as instructed by the wizard).
        <b className="step-description light-text">
          You should plan a cluster maintenance window and prepare for cluster
          downtime when enabling NameNode HA.
        </b>
      </p>
      <Alert className="mt-2" variant="warning">
        If you have HBase running, please exit this wizard and stop HBase first
        {isHawqInstalled ? (
          <div className="mt-2">
            HAWQ filespace must be updated manually after NameNode HA is enabled.
          </div>
        ) : null}
      </Alert>
      <Card className="mt-2">
        <CardBody>
          <Row className="align-items-center">
            <Col md={3} className="bolder">
              Nameservice ID:
            </Col>
            <Col md={4}>
              <OverlayTrigger
                trigger="hover"
                key="learn more"
                placement="right"
                overlay={
                  <Popover id="popover-positioned-right">
                    <Popover.Header as="h3">Nameservice ID</Popover.Header>
                    <Popover.Body>
                      This will be the ID for the NameNode HA cluster. For
                      example, if you set Nameservice ID to{" "}
                      <strong className="bolder">mycluster</strong>, the logical
                      URI for HDFS will be hdfs:://mycluster
                    </Popover.Body>
                  </Popover>
                }
              >
                <FormControl
                  type="text"
                  value={nameserviceId}
                  className={classNames({
                    "is-invalid": nameError,
                  })}
                  onChange={(e) => {
                    setNameserviceId(e.target.value);
                  }}
                ></FormControl>
              </OverlayTrigger>
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
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(0);
        }}
        onNext={async () => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                nameserviceId,
                hdfsUser,
              },
            },
          });
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step1;
