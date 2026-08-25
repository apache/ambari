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
import { Accordion, Alert, Badge, Button, Stack } from "react-bootstrap";
import { AppContext } from "../../../../store/context";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { EnableHighAvailibilityContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableResourceManagerSteps } from "./wizardSteps";
import { loadRmHaReview, runWithKdcSession } from "./rmHaWorkflow";
import {
  getRmHaAssignment,
  responseErrorMessage,
  stackCoordinates,
} from "./rmHaUtils";
import { RmHaReviewConfig } from "./rmHaTypes";

function displayValue(value: unknown) {
  if (typeof value === "boolean") return value ? "true" : "false";
  if (value === undefined || value === null) return "";
  return String(value);
}

function Step3() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, cluster, services } = useContext(AppContext);
  const { isLoaded: isKdcLoaded, getKDCSessionState } =
    useKDCSessionState(() => {});
  const [reviewConfig, setReviewConfig] = useState<RmHaReviewConfig | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [workflowError, setWorkflowError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const assignment = getRmHaAssignment(
    state.enableHighAvailibilitySteps?.[
      enableResourceManagerSteps.SELECT_HOSTS
    ],
  );
  const assignmentSnapshot = JSON.stringify(assignment);

  useEffect(() => {
    let cancelled = false;

    async function loadReview() {
      setIsLoading(true);
      setLoadError("");
      setReviewConfig(null);
      try {
        const currentAssignment = JSON.parse(assignmentSnapshot || "null");
        if (!currentAssignment?.topologyHosts?.length) {
          throw new Error(
            "The ResourceManager host assignment is missing. Return to Select Hosts.",
          );
        }
        const { stack, version } = stackCoordinates(cluster);
        const selectedServices = services
          .map((service) => service?.ServiceInfo?.service_name)
          .filter(Boolean) as string[];
        const loadedReview = await loadRmHaReview({
          clusterName,
          stack,
          version,
          hostNames: currentAssignment.hosts.map(
            ({ hostName }: { hostName: string }) => hostName,
          ),
          services: selectedServices,
          topology: currentAssignment.topologyHosts,
        });
        if (!cancelled) setReviewConfig(loadedReview);
      } catch (error) {
        if (!cancelled) {
          setLoadError(
            responseErrorMessage(
              error,
              "Ambari could not generate the ResourceManager HA configuration review.",
            ),
          );
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    if (clusterName) void loadReview();
    return () => {
      cancelled = true;
    };
  }, [
    clusterName,
    cluster,
    services,
    assignmentSnapshot,
    retryCount,
  ]);

  async function goBack() {
    setWorkflowError("");
    try {
      await flushStateToDb("back");
      await handleBackImperitive();
    } catch (error) {
      setWorkflowError(
        responseErrorMessage(
          error,
          "Ambari could not save the ResourceManager HA checkpoint.",
        ),
      );
    }
  }

  async function continueToDeployment() {
    if (!reviewConfig || isSubmitting) return;
    setIsSubmitting(true);
    setWorkflowError("");
    try {
      await runWithKdcSession(getKDCSessionState, async () => {
        dispatch({
          type: ActionTypes.STORE_INFORMATION,
          payload: {
            step: currentStep.name,
            data: { reviewConfig, stepConfigs: reviewConfig },
          },
        });
        await flushStateToDb("next");
        await handleNextImperitive();
      });
    } catch (error) {
      setWorkflowError(
        responseErrorMessage(
          error,
          "KDC validation or ResourceManager HA checkpoint persistence failed.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  async function cancelWorkflow() {
    setWorkflowError("");
    try {
      await flushStateToDb("cancel");
    } catch (error) {
      const message = responseErrorMessage(
        error,
        "Ambari could not clear the ResourceManager HA workflow.",
      );
      setWorkflowError(message);
      throw new Error(message);
    }
  }

  return (
    <>
      <h3 className="step-title">Review</h3>
      <div className="step-description">
        Review the ResourceManager hosts and configuration changes before
        Ambari stops services and updates the cluster.
      </div>

      {assignment && (
        <Alert variant="info" className="mt-3">
          <Stack direction="horizontal" gap={3} className="flex-wrap">
            <span>
              <Badge bg="secondary" className="me-2">
                Current
              </Badge>
              {assignment.currentRM}
            </span>
            <span>
              <Badge bg="success" className="me-2">
                Additional
              </Badge>
              {assignment.additionalRM}
            </span>
          </Stack>
        </Alert>
      )}

      {isLoading && (
        <div className="d-flex align-items-center gap-2 mt-4">
          <Spinner />
          <span>Generating ResourceManager HA configurations...</span>
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

      {reviewConfig && (
        <Accordion
          alwaysOpen
          defaultActiveKey={reviewConfig.configCategories.map(
            ({ name }) => name,
          )}
          className="mt-3"
        >
          {reviewConfig.configCategories.map((category) => {
            const categoryConfigs = reviewConfig.configs.filter(
              (config) => config.category === category.name,
            );
            return (
              <Accordion.Item eventKey={category.name} key={category.name}>
                <Accordion.Header>{category.displayName}</Accordion.Header>
                <Accordion.Body className="p-0">
                  {categoryConfigs.length ? (
                    <div className="table-responsive">
                      <table className="table table-sm mb-0 align-middle">
                        <thead>
                          <tr>
                            <th className="ps-3">Property</th>
                            <th>New value</th>
                            <th>Configuration</th>
                          </tr>
                        </thead>
                        <tbody>
                          {categoryConfigs.map((config) => (
                            <tr key={`${config.filename}:${config.name}`}>
                              <td className="ps-3 font-monospace">
                                {config.name}
                              </td>
                              <td className="text-break">
                                {displayValue(config.value)}
                              </td>
                              <td>{config.filename}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="p-3 text-muted">
                      No Advisor-generated property changes are required. The
                      current configuration will still be versioned.
                    </div>
                  )}
                </Accordion.Body>
              </Accordion.Item>
            );
          })}
        </Accordion>
      )}

      {workflowError && (
        <Alert variant="danger" className="mt-3">
          {workflowError}
        </Alert>
      )}

      <WizardFooter
        step={currentStep}
        isNextEnabled={
          Boolean(reviewConfig) &&
          !isLoading &&
          !loadError &&
          isKdcLoaded &&
          !isSubmitting
        }
        isBackEnabled={!isSubmitting}
        onBack={() => void goBack()}
        onNext={() => void continueToDeployment()}
        onCancel={cancelWorkflow}
      />
    </>
  );
}

export default Step3;
