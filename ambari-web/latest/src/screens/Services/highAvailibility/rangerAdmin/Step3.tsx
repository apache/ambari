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
  Accordion,
  Alert,
  Button,
  Card,
  Col,
  Form,
  Row,
  Stack,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { get, map } from "lodash";
import { AppContext } from "../../../../store/context";
import ConfigsApi from "../../../../api/configsApi";
import Center from "../../../../components/Center";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { getStepData } from "../../../../Utils/Utility";
import { ActionTypes } from "./store/types";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { enableRangerAdminSteps } from "./wizardSteps";
import {
  buildRangerAdminPreview,
  getRangerAdminHosts,
  RangerAdminPreviewCategory,
  RangerAdminPreviewProperty,
  validateRangerAdminAssignments,
} from "./rangerAdminHaUtils";

function Step3() {
  const { services, cluster } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const selectedServicesKey = selectedServices.join(",");
  const stackId = String(get(cluster, "version", ""));
  const stack = get(cluster, "stack", "") || stackId.split("-")[0];
  const version =
    get(cluster, "versionNum", "") || stackId.split("-").slice(1).join("-");
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const [categories, setCategories] = useState<RangerAdminPreviewCategory[]>([]);
  const [properties, setProperties] = useState<RangerAdminPreviewProperty[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  const loadBalancerUrl = getStepData(
    state,
    enableRangerAdminSteps.GET_STARTED,
    "loadBalancerUrl",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const assignments = getStepData(
    state,
    enableRangerAdminSteps.SELECT_HOSTS,
    "masterComponentHosts",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const assignmentErrors = validateRangerAdminAssignments(
    Array.isArray(assignments) ? assignments : [],
  );
  const rangerAdminHosts = getRangerAdminHosts(
    Array.isArray(assignments) ? assignments : [],
  );

  useEffect(() => {
    let active = true;
    async function loadPreview() {
      setIsLoading(true);
      setLoadError("");
      try {
        if (!stack || !version) {
          throw new Error("The current stack version is unavailable.");
        }
        const response = await ConfigsApi.getConfigProperties(
          stack,
          version,
          selectedServicesKey,
        );
        const preview = buildRangerAdminPreview(
          response?.items || [],
          selectedServicesKey.split(",").filter(Boolean),
          String(loadBalancerUrl || ""),
        );
        if (!preview.properties.length) {
          throw new Error(
            "No Ranger Admin HA configuration properties are available for the installed services.",
          );
        }
        if (active) {
          setCategories(preview.categories);
          setProperties(preview.properties);
        }
      } catch (error: unknown) {
        const requestError = error as {
          message?: string;
          response?: { data?: { message?: string } };
        };
        if (active) {
          setLoadError(
            requestError.response?.data?.message ||
              requestError.message ||
              "Unable to load Ranger Admin HA configuration properties.",
          );
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }
    void loadPreview();
    return () => {
      active = false;
    };
  }, [stack, version, selectedServicesKey, loadBalancerUrl, retryCount]);

  return (
    <>
      <div className="step-title">Review</div>
      <div className="step-description my-2">
        Confirm the Ranger Admin hosts and load balancer configuration.
      </div>
      <Card>
        <Card.Body>
          <div className="bg-light-subtle border p-3">
            <Row>
              <Col md={4} className="fw-bold">
                Current Ranger Admin:
              </Col>
              <Col>{rangerAdminHosts.currentHosts.join(", ")}</Col>
            </Row>
            <Row className="mt-3">
              <Col md={4} className="fw-bold">
                Additional Ranger Admins:
              </Col>
              <Col>
                <Stack gap={2}>
                  {rangerAdminHosts.additionalHosts.map((host) => (
                    <Stack direction="horizontal" gap={2} key={host}>
                      <span>{host}</span>
                      <FontAwesomeIcon icon={faPlus} className="text-success" />
                      <span className="text-success">TO BE INSTALLED</span>
                    </Stack>
                  ))}
                </Stack>
              </Col>
            </Row>
          </div>

          {assignmentErrors.map((error) => (
            <Alert variant="danger" className="mt-3" key={error}>
              {error}
            </Alert>
          ))}
          {loadError && (
            <Alert variant="danger" className="mt-3">
              {loadError}
              <Button
                size="sm"
                className="ms-3"
                onClick={() => setRetryCount((value) => value + 1)}
              >
                Retry
              </Button>
            </Alert>
          )}
          {isLoading ? (
            <Center>
              <Spinner />
            </Center>
          ) : (
            !loadError && (
              <>
                <Alert variant="info" className="mt-4">
                  <div className="fw-bold">Review Configuration Changes</div>
                  <div className="fs-12 mt-2">
                    These changes are for review only and cannot be edited in
                    this wizard.
                  </div>
                </Alert>
                <Accordion
                  defaultActiveKey={categories[0]?.name}
                  className="mt-4"
                  alwaysOpen
                >
                  {categories.map((category) => (
                    <Accordion.Item
                      eventKey={category.name}
                      key={category.name}
                    >
                      <Accordion.Header>
                        {category.displayName}
                      </Accordion.Header>
                      <Accordion.Body>
                        {properties
                          .filter(
                            (property) =>
                              property.serviceName === category.name,
                          )
                          .map((property) => (
                            <Row
                              key={property.id}
                              className="mt-3 align-items-center"
                            >
                              <Col md={5}>
                                <small>{property.displayName}</small>
                              </Col>
                              <Col>
                                <Form.Control
                                  type="text"
                                  size="sm"
                                  value={property.value}
                                  disabled
                                  readOnly
                                />
                              </Col>
                            </Row>
                          ))}
                      </Accordion.Body>
                    </Accordion.Item>
                  ))}
                </Accordion>
              </>
            )
          )}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={
          !isLoading && !loadError && assignmentErrors.length === 0
        }
        onBack={async () => {
          await flushStateToDb("back");
          jumpToStep(2);
        }}
        onNext={async () => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: { categories, properties },
            },
          });
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}

export default Step3;
