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
  FormControl,
  Row,
  Stack,
} from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMultiply } from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import { ActionTypes } from "./store/types";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { configValidator } from "../../../../Utils/validators";

function Step1() {
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const [loadBalancerUrl, setLoadBalancerUrl] = useState("");
  const {
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const [nameError, setNameError] = useState("");
  useEffect(() => {
    if (loadBalancerUrl) {
      if (configValidator.isValidURL(loadBalancerUrl)) {
        setNameError("");
        setIsNextEnabled(true);
      } else {
        setIsNextEnabled(false);
        setNameError("Must be valid URL.");
      }
    }
    if (!loadBalancerUrl) {
      setNameError("");
      setIsNextEnabled(false);
    }
  }, [loadBalancerUrl]);
  return (
    <>
      <h3 className="step-title">Get Started</h3>
      <p className="step-description light-text">
        This wizard will walk you through enabling Ranger Admin HA on your
        cluster.<br/>Once enabled, you will be running a Standby Ranger Admin in
        addition to your Active Ranger Admin.<br/>This allows for an Active-Standby
        Ranger Admin configuration that automatically performs failover.
        <div className="step-description light-text mt-3 bolder">
          You should plan a cluster maintenance window and prepare for cluster
          downtime when enabling Ranger Admin HA.
        </div>
      </p>
      <p className="step-description light-text">
        Please setup the load balancer and provide the URL to be used. Make sure
        that the load balancer is setup properly before proceeding.
      </p>
      <p className="step-description text-dark mt-2">
        Be sure that Ranger Admin and load balancer are located on separate
        hosts.
      </p>

      <Card className="mt-2">
        <CardBody>
          <Row className="align-items-center">
            <Col md={3} className="bolder">
              URL to load balancer:
            </Col>
            <Col md={4}>
              <FormControl
                type="text"
                value={loadBalancerUrl}
                className={classNames({
                  "is-invalid": nameError,
                })}
                onChange={(e) => {
                  setLoadBalancerUrl(e.target.value);
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
        onBack={() => {
          flushStateToDb("back");
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onNext={() => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                loadBalancerUrl,
              },
            },
          });
          flushStateToDb("next");
          handleNextImperitive();
        }}
      />
    </>
  );
}

export default Step1;
