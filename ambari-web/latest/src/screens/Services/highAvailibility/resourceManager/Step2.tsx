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
import { Alert, Button, Card, Col, Form, Row } from "react-bootstrap";
import Select from "react-select";
import { AppContext } from "../../../../store/context";
import { EnableHighAvailibilityContext } from "./store/context";
import { ActionTypes } from "./store/types";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import Spinner from "../../../../components/Spinner";
import { messages } from "../../../messages";
import { misc } from "../../../../Utils/misc";
import rmHaApi from "./rmHaApi";
import {
  buildHostRecommendationPayload,
  chooseAdditionalRmHost,
  createRmHaAssignment,
  flattenClusterTopology,
  getRmHaAssignment,
  parseRmHaHosts,
  recommendedHostsForComponent,
  responseErrorMessage,
  stackCoordinates,
  visibleHostOptions,
} from "./rmHaUtils";
import { RmHaHost, RmHaTopologyEntry } from "./rmHaTypes";
import { enableResourceManagerSteps } from "./wizardSteps";

type HostOption = {
  value: string;
  label: string;
  isDisabled: boolean;
};

function hostSummary(host: RmHaHost | undefined) {
  if (!host) return "";
  const details = [];
  if (host.totalMemory) {
    details.push(`${misc.formatBandwidth(host.totalMemory, "GB")} RAM`);
  }
  if (host.cpuCount !== undefined) details.push(`${host.cpuCount} cores`);
  return details.join(", ");
}

function Step2() {
  const { clusterName, cluster, services } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      handleNextImperitive,
      handleBackImperitive,
      currentStep,
    },
  } = useContext(EnableHighAvailibilityContext);
  const [hosts, setHosts] = useState<RmHaHost[]>([]);
  const [topology, setTopology] = useState<RmHaTopologyEntry[]>([]);
  const [currentRM, setCurrentRM] = useState("");
  const [additionalRM, setAdditionalRM] = useState("");
  const [hostSearch, setHostSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [persistenceError, setPersistenceError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const savedAssignment = getRmHaAssignment(
    state.enableHighAvailibilitySteps?.[
      enableResourceManagerSteps.SELECT_HOSTS
    ],
  );
  const savedCurrentRM = savedAssignment?.currentRM || "";
  const savedAdditionalRM = savedAssignment?.additionalRM || "";

  useEffect(() => {
    if (!clusterName) return;
    let cancelled = false;

    async function loadHostSelection() {
      setIsLoading(true);
      setLoadError("");
      try {
        const [hostsData, componentsData] = await Promise.all([
          rmHaApi.getHosts(clusterName),
          rmHaApi.getClusterComponents(clusterName),
        ]);
        const loadedHosts = parseRmHaHosts(hostsData).sort(
          (left, right) =>
            (right.totalMemory || 0) - (left.totalMemory || 0) ||
            (right.cpuCount || 0) - (left.cpuCount || 0) ||
            left.hostName.localeCompare(right.hostName),
        );
        const loadedTopology = flattenClusterTopology(componentsData);
        const selectedServices = services
          .map((service) => service?.ServiceInfo?.service_name)
          .filter(Boolean) as string[];
        if (!loadedHosts.length) {
          throw new Error("Ambari did not return any hosts for ResourceManager HA.");
        }
        const resourceManagers = loadedTopology.filter(
          ({ component }) => component === "RESOURCEMANAGER",
        );
        const installedRM =
          resourceManagers.find(
            ({ hostName }) => hostName === savedCurrentRM,
          ) || resourceManagers[0];
        if (!installedRM?.hostName) {
          throw new Error("Ambari did not return the installed ResourceManager.");
        }

        const hostNames = loadedHosts.map(({ hostName }) => hostName);
        const { stack, version } = stackCoordinates(cluster);
        const recommendationData = await rmHaApi.getHostRecommendations(
          stack,
          version,
          buildHostRecommendationPayload({
            hostNames,
            services: selectedServices,
            topology: loadedTopology,
          }),
        );
        const recommendedHosts = recommendedHostsForComponent(
          recommendationData,
          "RESOURCEMANAGER",
        );
        const savedAdditionalHost = loadedHosts.find(
          ({ hostName, maintenanceState }) =>
            hostName === savedAdditionalRM &&
            hostName !== installedRM.hostName &&
            (!maintenanceState || maintenanceState === "OFF"),
        )?.hostName;
        const selectedAdditionalRM =
          savedAdditionalHost ||
          chooseAdditionalRmHost(
            recommendedHosts,
            loadedHosts,
            installedRM.hostName,
          );
        if (!selectedAdditionalRM) {
          throw new Error(
            "No eligible host is available for the additional ResourceManager.",
          );
        }
        if (!cancelled) {
          setHosts(loadedHosts);
          setTopology(loadedTopology);
          setCurrentRM(installedRM.hostName);
          setAdditionalRM(selectedAdditionalRM);
        }
      } catch (error) {
        if (!cancelled) {
          setLoadError(
            responseErrorMessage(
              error,
              "Ambari could not prepare ResourceManager host selection.",
            ),
          );
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    void loadHostSelection();
    return () => {
      cancelled = true;
    };
  }, [
    clusterName,
    cluster,
    services,
    savedCurrentRM,
    savedAdditionalRM,
    retryCount,
  ]);

  let assignmentError = "";
  if (!isLoading && !loadError) {
    try {
      createRmHaAssignment(currentRM, additionalRM, hosts, topology);
    } catch (error) {
      assignmentError = responseErrorMessage(
        error,
        "Select an eligible host for the additional ResourceManager.",
      );
    }
  }

  const hostOptions: HostOption[] = hosts
    .filter(({ hostName }) => hostName !== currentRM)
    .map((host) => ({
      value: host.hostName,
      label: `${host.hostName}${hostSummary(host) ? ` (${hostSummary(host)})` : ""}`,
      isDisabled:
        Boolean(host.maintenanceState) && host.maintenanceState !== "OFF",
    }));
  const displayedHostOptions = visibleHostOptions(
    hostOptions,
    hostSearch,
    hosts.length > 25,
  );

  async function goBack() {
    setPersistenceError("");
    try {
      await flushStateToDb("back");
      await handleBackImperitive();
    } catch (error) {
      setPersistenceError(
        responseErrorMessage(
          error,
          "Ambari could not save the ResourceManager HA checkpoint.",
        ),
      );
    }
  }

  async function continueToReview() {
    setPersistenceError("");
    try {
      const assignment = createRmHaAssignment(
        currentRM,
        additionalRM,
        hosts,
        topology,
      );
      dispatch({
        type: ActionTypes.STORE_INFORMATION,
        payload: { step: currentStep.name, data: assignment },
      });
      await flushStateToDb("next");
      await handleNextImperitive();
    } catch (error) {
      setPersistenceError(
        responseErrorMessage(
          error,
          "Ambari could not save the ResourceManager host assignment.",
        ),
      );
    }
  }

  async function cancelWorkflow() {
    setPersistenceError("");
    try {
      await flushStateToDb("cancel");
    } catch (error) {
      const message = responseErrorMessage(
        error,
        "Ambari could not clear the ResourceManager HA workflow.",
      );
      setPersistenceError(message);
      throw new Error(message);
    }
  }

  return (
    <>
      <div className="step-title">Select Hosts</div>
      <div className="step-description">
        {messages["admin.rm_highAvailability.wizard.step2.body"]}
      </div>

      {isLoading && (
        <div className="d-flex align-items-center gap-2 mt-4">
          <Spinner />
          <span>Loading hosts and Stack Advisor recommendations...</span>
        </div>
      )}

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

      {!isLoading && !loadError && (
        <Card className="mt-3">
          <Card.Body>
            <Row className="g-4">
              <Col md={6}>
                <Form.Group controlId="current-resource-manager">
                  <Form.Label className="fw-bold">
                    Current ResourceManager
                  </Form.Label>
                  <Form.Control value={currentRM} disabled readOnly />
                  {hostSummary(
                    hosts.find(({ hostName }) => hostName === currentRM),
                  ) && (
                    <Form.Text>
                      {hostSummary(
                        hosts.find(({ hostName }) => hostName === currentRM),
                      )}
                    </Form.Text>
                  )}
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group controlId="additional-resource-manager">
                  <Form.Label className="fw-bold">
                    Additional ResourceManager
                  </Form.Label>
                  <Select<HostOption, false>
                    inputId="additional-resource-manager"
                    value={
                      hostOptions.find(({ value }) => value === additionalRM) ||
                      null
                    }
                    options={displayedHostOptions}
                    isOptionDisabled={(option) => option.isDisabled}
                    onChange={(option) => setAdditionalRM(option?.value || "")}
                    onInputChange={(inputValue) => setHostSearch(inputValue)}
                    placeholder="Select a host"
                  />
                </Form.Group>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      )}

      {assignmentError && (
        <Alert variant="danger" className="mt-3">
          {assignmentError}
        </Alert>
      )}
      {persistenceError && (
        <Alert variant="danger" className="mt-3">
          {persistenceError}
        </Alert>
      )}

      <WizardFooter
        step={currentStep}
        isNextEnabled={
          !isLoading && !loadError && !assignmentError && Boolean(additionalRM)
        }
        onBack={() => void goBack()}
        onNext={() => void continueToReview()}
        onCancel={cancelWorkflow}
      />
    </>
  );
}

export default Step2;
