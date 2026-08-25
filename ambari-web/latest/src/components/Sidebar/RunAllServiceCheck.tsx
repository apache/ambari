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
import { Dropdown } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faThumbsUp } from "@fortawesome/free-solid-svg-icons";
import { AppContext } from "../../store/context.tsx";
import { ServiceContext } from "../../store/ServiceContext.tsx";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy.ts";
import ConfirmationModal from "../ConfirmationModal.tsx";
import { ActionsApi } from "../../api/actionsApi.ts";
import BackgroundOperations from "../../screens/BackgroundOperations";
import modalManager from "../../store/ModalManager.ts";
import { get, map } from "lodash";

const RunAllServiceCheck = () => {
  const { clusterName, services, upgradeIsRunning, upgradeSuspended, serviceCheckSupportedMap } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const { isAuthorized } = useAuthorizationPolicy();
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isRunning, setIsRunning] = useState(false);

  const canRunServiceCheck = isAuthorized("SERVICE.RUN_SERVICE_CHECK");

  // Block during active upgrade (not suspended)
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

  if (!canRunServiceCheck || isUpgradeBlocking) {
    return null;
  }

  const runAllServiceChecks = async () => {
    setIsRunning(true);
    setShowConfirmation(false);

    try {
      const installedServiceNames = map(services, "ServiceInfo.service_name");

      // Build array of service check promises to execute in parallel
      const serviceCheckPromises = installedServiceNames.map(async (serviceName) => {
        try {
          // Use cached service_check_supported from initial stack configs fetch (no per-service API call)
          const isServiceCheckSupported = serviceCheckSupportedMap[serviceName] || false;

          // Skip if service doesn't support service check
          if (!isServiceCheckSupported) {
            return null;
          }

          // Check if service is client-only
          const serviceModel = allServiceModels[serviceName.toLowerCase()];
          const isClientOnly = serviceModel?.hasOwnProperty("isClientOnlyService");

          // Prepare payload based on service type
          let payloadData: any;
          const payloadOperationLevel = {
            level: "CLUSTER",
            cluster_name: clusterName,
          };

          if (serviceName === "ZOOKEEPER") {
            payloadData = {
              RequestInfo: {
                command: `${serviceName}_QUORUM_SERVICE_CHECK`,
                context: `${serviceName} Service Check`,
                operation_level: payloadOperationLevel,
              },
              "Requests/resource_filters": [
                {
                  service_name: serviceName,
                },
              ],
            };
          } else if (serviceName === "TEZ" || serviceName === "SQOOP" || isClientOnly) {
            payloadData = {
              RequestInfo: {
                context: `${serviceName} Service Check`,
                command: `${serviceName}_SERVICE_CHECK`,
              },
              "Requests/resource_filters": [
                {
                  service_name: serviceName,
                },
              ],
            };
          } else {
            payloadData = {
              RequestInfo: {
                command: `${serviceName}_SERVICE_CHECK`,
                context: `${serviceName} Service Check`,
                operation_level: payloadOperationLevel,
              },
              "Requests/resource_filters": [
                {
                  service_name: serviceName,
                },
              ],
            };
          }

          // Execute service check
          const response = await ActionsApi.actionRequestRebalanceHDFS(
            clusterName,
            payloadData
          );

          const requestId = get(response, "data.Requests.id", -1);
          return requestId !== -1 ? requestId : null;
        } catch (error) {
          console.error(`Error running service check for ${serviceName}`, error);
          return null; // Return null for failed service checks
        }
      });

      // Execute all service checks in parallel
      const requestIds = (await Promise.all(serviceCheckPromises)).filter(
        (id) => id !== null
      ) as number[];

      // Show background operations modal with the first request ID
      if (requestIds.length > 0) {
        modalManager.show(
          <BackgroundOperations
            isOpen={true}
            onClose={() => {
              modalManager.hide();
            }}
            requestId={requestIds[0]}
          />
        );
      } else {
        // Show message if no service checks were run
        modalManager.show(
          <ConfirmationModal
            isOpen={true}
            onClose={() => modalManager.hide()}
            modalTitle="Run All Service Checks"
            modalBody="No services support service check or all services are stopped."
            cancellable={false}
            successCallback={() => modalManager.hide()}
          />
        );
      }
    } catch (error) {
      console.error("Error running all service checks", error);
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle="Error"
          modalBody="An error occurred while running service checks. Please check the logs for details."
          cancellable={false}
          successCallback={() => modalManager.hide()}
        />
      );
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <>
      <Dropdown.Item
        onClick={() => setShowConfirmation(true)}
        disabled={isRunning}
      >
        <FontAwesomeIcon className="text-muted me-2" icon={faThumbsUp} />
        Run All Service Checks
      </Dropdown.Item>

      <ConfirmationModal
        isOpen={showConfirmation}
        onClose={() => setShowConfirmation(false)}
        modalTitle="Run All Service Checks"
        modalBody="Would you like to proceed with running service checks for all installed services? This process may take a while to complete."
        cancellable={true}
        successCallback={runAllServiceChecks}
      />
    </>
  );
};

export default RunAllServiceCheck;
