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

import { cloneDeep, get, has, set } from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import { RequestApi } from "../api/requestApi";
import { AppContext } from "../store/context";
import { isFailed, isFinished } from "../Utils/Utility";
import { ProgressStatus } from "../constants";
import { Alert, Button, ProgressBar, Stack } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
//TODO: uncomment below code when background operations modal is implemented
// import modalManager from "../store/ModalManager";
// import BackgroundOperations from "../screens/BackgroundOperations";
import { getStatusIcon } from "../Utils/statusIcons";

type PropTypes = {
  title: string;
  description: string;
  setCompletionStatus: (completed: boolean) => void;
  operations: [
    {
      id: string | number;
      label: "string";
      callback: any;
      skippable: boolean;
      requestId?: string | number;
      status?: string;
      progress?: number;
      error?: string;
    }
  ];
  dispatch?: (operationsState: any) => void;
  errorCallback?: (errorMsg: string) => void;
};

type OperationRequestResponse = {
  Requests: {
    id: number | string;
    status: string;
  };
  href: string;
  status?: number;
};

function OperationsProgress({
  setCompletionStatus,
  operations,
  dispatch,
  errorCallback,
}: PropTypes) {
  const [operationsState, setOperationsState] = useState(operations);
  const [activeOperationId, setActiveOperationId] = useState(-1);
  const { clusterName } = useContext(AppContext);
  const startedTasks: any = useRef([]);

  const trackCurrentRequestStatus = async (
    requestId: number,
    operationId: number
  ) => {
    const operationsStateCopy = cloneDeep(operationsState);
    const trackingStatusForOperation: any = operationsStateCopy.find(
      (operation) => operation.id == operationId
    );
    if (requestId) {
      set(trackingStatusForOperation, "requestId", requestId);
      const requestStatus = await RequestApi.getRequestStatus(
        clusterName,
        String(requestId)
      );
      if (requestStatus?.Requests?.request_status) {
        const { Requests } = requestStatus;
        set(trackingStatusForOperation, "status", Requests?.request_status);
        set(trackingStatusForOperation, "progress", Requests?.progress_percent);
        set(trackingStatusForOperation, "requestInfo", Requests);
        if (has(trackingStatusForOperation, "error")) {
          delete trackingStatusForOperation.error;
        }


        setOperationsState(operationsStateCopy);

        if (!isFinished(Requests?.request_status)) {
          setTimeout(() => {
            trackCurrentRequestStatus(requestId, operationId);
          }, 2000);
        }
      }
    }
  };

  async function executeTask() {
    if (!startedTasks.current.includes(activeOperationId)) {
      startedTasks.current.push(activeOperationId);
      const operationsStateCopy = cloneDeep(operationsState);
      const matchingOperation: any = operationsStateCopy.find(
        (operation) => operation.id == activeOperationId
      );

      if (
        matchingOperation &&
        matchingOperation?.status === ProgressStatus.IN_PROGRESS
      ) {
        trackCurrentRequestStatus(
          matchingOperation.requestId,
          activeOperationId
        );
        return;
      }

      if (matchingOperation) {
        try {
          const operationCallbackResponse: OperationRequestResponse =
            await matchingOperation?.callback();

          if (operationCallbackResponse?.Requests) {
            set(
              matchingOperation,
              "requestId",
              operationCallbackResponse?.Requests?.id
            );
            trackCurrentRequestStatus(
              matchingOperation.requestId,
              activeOperationId
            );
          } else {
            const statusCode = get(operationCallbackResponse, "[0].status", operationCallbackResponse?.status);

            // Handle status codes by ranges in case of success or unknown response
            if (
              (statusCode && statusCode >= 200 && statusCode < 300) ||
              !operationCallbackResponse
            ) {
              // 2xx Success status codes or empty response with no status code - treat as success
              set(matchingOperation, "status", ProgressStatus.COMPLETED);
              if (has(matchingOperation, "error")) {
                delete matchingOperation.error;
              }
            } else {
              // Unknown status code or response exists but no status code
              set(matchingOperation, "status", ProgressStatus.FAILED);
              if (statusCode) {
                set(
                  matchingOperation,
                  "error",
                  JSON.stringify(operationCallbackResponse) ||
                    `Unknown Status Code (${statusCode}): ${JSON.stringify(
                      operationCallbackResponse
                    )}`
                );
              } else {
                set(
                  matchingOperation,
                  "error",
                  JSON.stringify(operationCallbackResponse) ||
                    `Unknown response format: ${JSON.stringify(
                      operationCallbackResponse
                    )}`
                );
              }
            }
            setOperationsState(operationsStateCopy);
          }
        } catch (error: any) {
          const statusCode = get(error, "status", "");
          const errorMessage = get(error, "response.data.message", "");
          // Handle status codes by ranges in case of error
          if (statusCode && statusCode >= 300 && statusCode < 400) {
            // 3xx Redirection status codes - treat as error for operations
            set(matchingOperation, "status", ProgressStatus.FAILED);
            set(
              matchingOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Redirection Error (${statusCode}): Operation requires manual intervention`
            );
          } else if (statusCode && statusCode >= 400 && statusCode < 500) {
            // 4xx Client error status codes
            set(matchingOperation, "status", ProgressStatus.FAILED);
            set(
              matchingOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Client Error (${statusCode}): Please check the request parameters`
            );
          } else if (statusCode && statusCode >= 500 && statusCode < 600) {
            // 5xx Server error status codes
            set(matchingOperation, "status", ProgressStatus.FAILED);
            set(
              matchingOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Server Error (${statusCode}): Please try again later or contact support`
            );
          } else {
            // Unknown status code or response exists but no status code
            set(matchingOperation, "status", ProgressStatus.FAILED);
            if (statusCode) {
              set(
                matchingOperation,
                "error",
                JSON.stringify(errorMessage) ||
                  `Unknown Status Code (${statusCode}): ${JSON.stringify(
                    errorMessage
                  )}`
              );
            } else {
              set(
                matchingOperation,
                "error",
                JSON.stringify(errorMessage) ||
                  `Unknown response format: ${JSON.stringify(errorMessage)}`
              );
            }
          }
          setOperationsState(operationsStateCopy);
        }
      }
    }
  }

  const handleMoveToNextOperation = () => {
    const currentActiveOperation = operationsState.find(
      (operation) => operation.id == activeOperationId
    );
    if (
      currentActiveOperation &&
      isFinished(currentActiveOperation?.status || "")
    ) {
      if (currentActiveOperation?.status !== ProgressStatus.FAILED) {
        if (activeOperationId == operationsState?.[operationsState?.length - 1]?.id) {
          setCompletionStatus(true);
        } else {
          const currentActiveIndex = operationsState.findIndex(
            (operation) => operation.id == activeOperationId
          );
          setActiveOperationId(
            Number(operationsState[Number(currentActiveIndex) + 1]?.id)
          );
        }
      }
    }
  };

  const retryOperation = () => {
    startedTasks.current = startedTasks.current.filter((task: any) => {
      task != activeOperationId;
    });
    executeTask();
  };

  useEffect(() => {
    if (activeOperationId >= 0) {
      executeTask();
    }
  }, [activeOperationId]);

  useEffect(() => {
    if (dispatch) {
      dispatch(operationsState);
    }
    handleMoveToNextOperation();
  }, [JSON.stringify(operationsState)]);

  useEffect(() => {
    let activeIdx = -1;
    let toBeStartedIdx = -1;
    for (let i = operationsState.length - 1; i >= 0; i--) {
      if (
        get(operationsState[i], "requestId", "") ||
        get(operationsState[i], "status", "")
      ) {
        activeIdx = i;
        break;
      }
    }

    if (activeIdx === -1) {
      toBeStartedIdx = 0;
    } else {
      for (let i = 0; i <= activeIdx; i++) {
        if (
          get(operationsState[i], "requestId", "") ||
          get(operationsState[i], "status", "")
        ) {
          startedTasks.current.push(operationsState[i].id);
          if (isFinished(operationsState[i]?.status || "")) {
            if (operationsState[i]?.status === ProgressStatus.FAILED) {
              toBeStartedIdx = i;
            } else {
              toBeStartedIdx = i + 1;
            }
          } else {
            toBeStartedIdx = i;
          }
        }
      }
    }
    setActiveOperationId(Number(operationsState?.[toBeStartedIdx]?.id));
    if(operationsState?.[toBeStartedIdx]?.requestId){
      trackCurrentRequestStatus(operationsState?.[toBeStartedIdx]?.requestId as number, operationsState?.[toBeStartedIdx]?.id as number);
    }

  }, []);

  return (
    <div className="p-3">
      <Stack direction="vertical">
        {operationsState.map((operation: any) => {
          return (
            <Stack
              direction="horizontal"
              className="justify-content-between mt-3"
              key={operation.label}
            >
              <div className="d-flex align-items-center">
                {getStatusIcon(operation?.status)}
                <div
                  onClick={() => {
                    if (operation.requestId || operation?.requestInfo?.id) {
                      // modalManager.show(
                      //   <BackgroundOperations
                      //     isExplicitClick
                      //     isOpen
                      //     onClose={() => {
                      //       modalManager.hide();
                      //     }}
                      //     rootLevel={ViewLevel.HOSTS}
                      //     requestId={
                      //       operation.requestId || operation?.requestInfo?.id
                      //     }
                      //   />
                      // );
                    }
                  }}
                  className={`${
                    has(operation, "requestId") ||
                    has(operation, "requestInfo.id")
                      ? "custom-link"
                      : ""
                  }`}
                >
                  {operation.label}{" "}
                </div>
                {isFailed(operation.status) ? (
                  <Button
                    size="sm"
                    onClick={retryOperation}
                    variant="success"
                    className="mx-2"
                  >
                    <FontAwesomeIcon className="me-2" icon={faUndo} />
                    Retry Operation
                  </Button>
                ) : null}
              </div>
              {has(operation, "progress") && !isFinished(operation.status) ? (
                <ProgressBar
                  className={`w-25`}
                  variant="info"
                  now={operation.progress}
                  label={`${Math.floor(operation.progress)}%`}
                />
              ) : null}

              {has(operation, "error") &&
                isFinished(operation.status) &&
                (errorCallback ? (
                  (errorCallback(
                    operation.error ||
                      "Error: An error occurred during the operation."
                  ),
                  null)
                ) : (
                  <Alert variant="danger" className="scrollable-h15 mt-3">
                    {operation.error ||
                      "Error: An error occurred during the operation."}
                  </Alert>
                ))}
            </Stack>
          );
        })}
      </Stack>
    </div>
  );
}

export default OperationsProgress;
