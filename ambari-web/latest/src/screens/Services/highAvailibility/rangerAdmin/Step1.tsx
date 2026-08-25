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

import { useContext, useState } from "react";
import { Card, Col, Form, Row, Stack } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { configValidator } from "../../../../Utils/validators";
import { getStepData } from "../../../../Utils/Utility";
import { ActionTypes } from "./store/types";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { enableRangerAdminSteps } from "./wizardSteps";

function Step1() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const savedUrl = getStepData(
    state,
    enableRangerAdminSteps.GET_STARTED,
    "loadBalancerUrl",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const [loadBalancerUrl, setLoadBalancerUrl] = useState(
    typeof savedUrl === "string" ? savedUrl : "",
  );
  const isValid = configValidator.isValidURL(loadBalancerUrl);
  const validationError = loadBalancerUrl && !isValid ? "Must be a valid URL." : "";

  return (
    <>
      <h3 className="step-title">Get Started</h3>
      <div className="step-description light-text">
        This wizard will walk you through enabling Ranger Admin HA on your
        cluster.
        <br />
        Once enabled, a Standby Ranger Admin will run in addition to the Active
        Ranger Admin and provide automatic failover.
      </div>
      <div className="fw-bold fs-12 mt-3">
        Plan a cluster maintenance window and prepare for cluster downtime.
      </div>
      <p className="step-description light-text mt-3">
        Set up the load balancer before proceeding and provide the URL that
        Ranger clients will use.
      </p>
      <p className="step-description text-dark">
        Keep the load balancer on a host separate from every Ranger Admin.
      </p>

      <Card className="mt-2">
        <Card.Body>
          <Row className="align-items-center">
            <Col md={3} className="fw-bold">
              URL to load balancer:
            </Col>
            <Col md={5}>
              <Form.Control
                aria-label="URL to load balancer"
                type="text"
                value={loadBalancerUrl}
                className={classNames({ "is-invalid": validationError })}
                onChange={(event) => setLoadBalancerUrl(event.target.value)}
              />
            </Col>
            <Col>
              {validationError && (
                <Stack direction="horizontal" gap={2} className="text-danger">
                  <FontAwesomeIcon icon={faCircleXmark} />
                  <span>{validationError}</span>
                </Stack>
              )}
            </Col>
          </Row>
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={isValid}
        onBack={() => undefined}
        onCancel={() => void flushStateToDb("cancel")}
        onNext={async () => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: { loadBalancerUrl },
            },
          });
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
      />
    </>
  );
}

export default Step1;
