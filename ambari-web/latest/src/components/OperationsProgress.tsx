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
import { ProgressStatus, ViewLevel } from "../constants";
import { Alert, Button, ProgressBar, Stack } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
import modalManager from "../store/ModalManager";
import BackgroundOperations from "../screens/BackgroundOperations";
import { getStatusIcon } from "../Utils/statusIcons";

type PropTypes = {
  title: string;
  description: string;
  setCompletionStatus: (completed: boolean) => void;
  operations: Operation[];
  dispatch?: (operationsState: any) => void;
  errorCallback?: (errorMsg: string) => void;
};

type Operation = {
  id: string | number;
  label: string;
  callback: () => Promise<any>;
  skippable: boolean;
  requestId?: string | number;
  status?: string;
  progress?: number;
  error?: string;
  requestInfo?: Record<string, any>;
  skipCallback?: () => Promise<any>;
  retryFromOperationId?: string | number;
  [key: string]: any;
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
  const [skippingOperationId, setSkippingOperationId] = useState<
    string | number | null
  >(null);
  const { clusterName } = useContext(AppContext);
  const startedTasks = useRef<Array<string | number>>([]);
  const operationsStateRef = useRef(operations);
  const pollingTimers = useRef<Set<ReturnType<typeof setTimeout>>>(new Set());
  const pollingGeneration = useRef<Map<string | number, number>>(new Map());
  const isMounted = useRef(true);
  const setCompletionStatusRef = useRef(setCompletionStatus);
  const errorCallbackRef = useRef(errorCallback);
  const reportedError = useRef("");
  const trackCurrentRequestStatusRef = useRef<(
    requestId: number | string,
    operationId: number | string,
  ) => Promise<void>>(async () => undefined);
  setCompletionStatusRef.current = setCompletionStatus;
  errorCallbackRef.current = errorCallback;

  const updateOperationsState = (nextState: any) => {
    operationsStateRef.current = nextState;
    if (isMounted.current) {
      setOperationsState(nextState);
    }
  };

  const getPollingGeneration = (operationId: string | number) =>
    pollingGeneration.current.get(operationId) ?? 0;

  const invalidatePolling = (operationId: string | number) => {
    pollingGeneration.current.set(
      operationId,
      getPollingGeneration(operationId) + 1,
    );
  };

  const trackCurrentRequestStatus = async (
    requestId: number | string,
    operationId: number | string,
    generation = getPollingGeneration(operationId),
  ) => {
    if (requestId) {
      try {
        const requestStatus = await RequestApi.getRequestStatus(
          clusterName,
          String(requestId)
        );
        if (
          !isMounted.current ||
          generation !== getPollingGeneration(operationId)
        ) {
          return;
        }

        const operationsStateCopy = cloneDeep(operationsStateRef.current);
        const trackingStatusForOperation: any = operationsStateCopy.find(
          (operation) => operation.id == operationId
        );
        if (
          !trackingStatusForOperation ||
          String(trackingStatusForOperation.requestId) !== String(requestId)
        ) {
          return;
        }

        const { Requests } = requestStatus ?? {};
        if (!Requests?.request_status) {
          throw new Error("Ambari returned an invalid request status response.");
        }

        const requestState = ["ABORTED", "TIMEDOUT"].includes(
          Requests.request_status,
        )
          ? ProgressStatus.FAILED
          : Requests.request_status;
        set(trackingStatusForOperation, "status", requestState);
        set(trackingStatusForOperation, "progress", Requests.progress_percent);
        set(trackingStatusForOperation, "requestInfo", Requests);
        if (has(trackingStatusForOperation, "error")) {
          delete trackingStatusForOperation.error;
        }

        updateOperationsState(operationsStateCopy);

        if (!isFinished(requestState)) {
          const timer = setTimeout(() => {
            pollingTimers.current.delete(timer);
            void trackCurrentRequestStatus(
              requestId,
              operationId,
              generation,
            );
          }, 2000);
          pollingTimers.current.add(timer);
        }
      } catch (error: any) {
        if (
          !isMounted.current ||
          generation !== getPollingGeneration(operationId)
        ) {
          return;
        }
        const operationsStateCopy = cloneDeep(operationsStateRef.current);
        const trackingStatusForOperation: any = operationsStateCopy.find(
          (operation) => operation.id == operationId
        );
        if (
          !trackingStatusForOperation ||
          String(trackingStatusForOperation.requestId) !== String(requestId)
        ) {
          return;
        }
        set(trackingStatusForOperation, "status", ProgressStatus.FAILED);
        set(
          trackingStatusForOperation,
          "error",
          get(error, "response.data.message", get(error, "message", "Unable to track the Ambari request.")),
        );
        updateOperationsState(operationsStateCopy);
      }
    }
  };
  trackCurrentRequestStatusRef.current = trackCurrentRequestStatus;

  async function executeTask(operationId = activeOperationId) {
    if (!startedTasks.current.includes(operationId)) {
      startedTasks.current.push(operationId);
      const operationsStateCopy = cloneDeep(operationsStateRef.current);
      const matchingOperation: any = operationsStateCopy.find(
        (operation) => operation.id == operationId
      );

      if (
        matchingOperation &&
        matchingOperation?.status === ProgressStatus.IN_PROGRESS
      ) {
        if (matchingOperation.requestId) {
          void trackCurrentRequestStatus(
            matchingOperation.requestId,
            operationId,
          );
        } else {
          set(matchingOperation, "status", ProgressStatus.FAILED);
          set(
            matchingOperation,
            "error",
            "The persisted operation has no Ambari request ID and cannot be recovered.",
          );
          updateOperationsState(operationsStateCopy);
        }
        return;
      }

      if (matchingOperation) {
        try {
          const operationCallbackResponse: OperationRequestResponse =
            await matchingOperation?.callback();

          if (operationCallbackResponse?.Requests) {
            const latestOperationsState = cloneDeep(operationsStateRef.current);
            const latestOperation: any = latestOperationsState.find(
              (operation) => operation.id == operationId,
            );
            if (!latestOperation) {
              return;
            }
            set(
              latestOperation,
              "requestId",
              operationCallbackResponse?.Requests?.id
            );
            set(latestOperation, "status", ProgressStatus.IN_PROGRESS);
            updateOperationsState(latestOperationsState);
            void trackCurrentRequestStatus(
              latestOperation.requestId,
              operationId,
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
            updateOperationsState(operationsStateCopy);
          }
        } catch (error: any) {
          const latestOperationsState = cloneDeep(operationsStateRef.current);
          const failedOperation: any = latestOperationsState.find(
            (operation) => operation.id == operationId,
          );
          if (!failedOperation) {
            return;
          }
          const statusCode = get(error, "response.status", get(error, "status", ""));
          const errorMessage = get(
            error,
            "response.data.message",
            get(error, "message", "The operation failed."),
          );
          // Handle status codes by ranges in case of error
          if (statusCode && statusCode >= 300 && statusCode < 400) {
            // 3xx Redirection status codes - treat as error for operations
            set(failedOperation, "status", ProgressStatus.FAILED);
            set(
              failedOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Redirection Error (${statusCode}): Operation requires manual intervention`
            );
          } else if (statusCode && statusCode >= 400 && statusCode < 500) {
            // 4xx Client error status codes
            set(failedOperation, "status", ProgressStatus.FAILED);
            set(
              failedOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Client Error (${statusCode}): Please check the request parameters`
            );
          } else if (statusCode && statusCode >= 500 && statusCode < 600) {
            // 5xx Server error status codes
            set(failedOperation, "status", ProgressStatus.FAILED);
            set(
              failedOperation,
              "error",
              JSON.stringify(errorMessage) ||
                `Server Error (${statusCode}): Please try again later or contact support`
            );
          } else {
            // Unknown status code or response exists but no status code
            set(failedOperation, "status", ProgressStatus.FAILED);
            if (statusCode) {
              set(
                failedOperation,
                "error",
                JSON.stringify(errorMessage) ||
                  `Unknown Status Code (${statusCode}): ${JSON.stringify(
                    errorMessage
                  )}`
              );
            } else {
              set(
                failedOperation,
                "error",
                JSON.stringify(errorMessage) ||
                  `Unknown response format: ${JSON.stringify(errorMessage)}`
              );
            }
          }
          updateOperationsState(latestOperationsState);
        }
      }
    }
  }

  const handleMoveToNextOperation = () => {
    const currentActiveOperation = operationsStateRef.current.find(
      (operation) => operation.id == activeOperationId
    );
    if (currentActiveOperation?.status === ProgressStatus.COMPLETED) {
      if (activeOperationId == operationsStateRef.current?.at(-1)?.id) {
        setCompletionStatus(true);
      } else {
        const currentActiveIndex = operationsStateRef.current.findIndex(
          (operation) => operation.id == activeOperationId
        );
        setActiveOperationId(
          Number(operationsStateRef.current[Number(currentActiveIndex) + 1]?.id)
        );
      }
    }
  };

  const retryOperation = (operationId: string | number) => {
    const operationsStateCopy = cloneDeep(operationsStateRef.current);
    const matchingOperation: any = operationsStateCopy.find(
      (operation) => operation.id == operationId,
    );
    if (matchingOperation) {
      const retryOperationId =
        matchingOperation.retryFromOperationId ?? operationId;
      const retryIndex = operationsStateCopy.findIndex(
        (operation) => operation.id == retryOperationId,
      );
      if (retryIndex < 0) {
        return;
      }
      const retryIds = operationsStateCopy
        .slice(retryIndex)
        .map((operation) => operation.id);
      retryIds.forEach(invalidatePolling);
      startedTasks.current = startedTasks.current.filter(
        (task) => !retryIds.some((retryId) => retryId == task),
      );
      operationsStateCopy.slice(retryIndex).forEach((operation) => {
        delete operation.requestId;
        delete operation.requestInfo;
        delete operation.progress;
        delete operation.error;
        delete operation.status;
      });
      updateOperationsState(operationsStateCopy);
      setCompletionStatus(false);
      if (retryOperationId == activeOperationId) {
        void executeTask(retryOperationId);
      } else {
        setActiveOperationId(Number(retryOperationId));
      }
    }
  };

  const skipOperation = async (operationId: string | number) => {
    const operation = operationsStateRef.current.find(
      (candidate) => candidate.id == operationId,
    );
    if (
      !operation?.skippable ||
      !operation.skipCallback ||
      skippingOperationId !== null
    ) {
      return;
    }

    invalidatePolling(operationId);
    setSkippingOperationId(operationId);
    const pendingState = cloneDeep(operationsStateRef.current);
    const pendingOperation: any = pendingState.find(
      (candidate) => candidate.id == operationId,
    );
    delete pendingOperation.requestId;
    delete pendingOperation.requestInfo;
    delete pendingOperation.progress;
    delete pendingOperation.error;
    set(pendingOperation, "status", ProgressStatus.IN_PROGRESS);
    updateOperationsState(pendingState);

    try {
      const response = await operation.skipCallback();
      const nextState = cloneDeep(operationsStateRef.current);
      const skippedOperation: any = nextState.find(
        (candidate) => candidate.id == operationId,
      );
      if (response?.Requests?.id) {
        set(skippedOperation, "requestId", response.Requests.id);
        set(skippedOperation, "status", ProgressStatus.IN_PROGRESS);
        updateOperationsState(nextState);
        void trackCurrentRequestStatus(response.Requests.id, operationId);
      } else {
        const statusCode = get(response, "[0].status", response?.status);
        if (
          response &&
          (!statusCode || statusCode < 200 || statusCode >= 300)
        ) {
          throw new Error("Ambari returned an invalid skip response.");
        }
        set(skippedOperation, "status", ProgressStatus.COMPLETED);
        updateOperationsState(nextState);
      }
    } catch (error) {
      const failedState = cloneDeep(operationsStateRef.current);
      const failedOperation: any = failedState.find(
        (candidate) => candidate.id == operationId,
      );
      set(failedOperation, "status", ProgressStatus.FAILED);
      set(
        failedOperation,
        "error",
        get(
          error,
          "response.data.message",
          get(error, "message", "Unable to skip the operation."),
        ),
      );
      updateOperationsState(failedState);
    } finally {
      setSkippingOperationId(null);
    }
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
    const failedOperation = operationsState.find(
      (operation) => operation.error && isFinished(operation.status || ""),
    );
    if (!failedOperation) {
      reportedError.current = "";
      return;
    }
    const message = failedOperation.error
      || "Error: An error occurred during the operation.";
    const errorKey = `${failedOperation.id}:${failedOperation.status}:${message}`;
    if (errorCallbackRef.current && reportedError.current !== errorKey) {
      reportedError.current = errorKey;
      errorCallbackRef.current(message);
    }
  }, [operationsState]);

  useEffect(() => {
    let activeIdx = -1;
    for (let i = 0; i < operationsStateRef.current.length; i++) {
      const operation = operationsStateRef.current[i];
      if (operation.status === ProgressStatus.COMPLETED) {
        startedTasks.current.push(operation.id);
        continue;
      }
      activeIdx = i;
      if (operation.requestId || operation.status) {
        startedTasks.current.push(operation.id);
      }
      break;
    }

    if (activeIdx === -1) {
      setCompletionStatusRef.current(true);
      return;
    }

    const activeOperation = operationsStateRef.current[activeIdx];
    setActiveOperationId(Number(activeOperation.id));
    if (activeOperation.requestId && !isFinished(activeOperation.status || "")) {
      void trackCurrentRequestStatusRef.current?.(
        activeOperation.requestId,
        activeOperation.id,
      );
    }
  }, []);

  useEffect(() => {
    const timers = pollingTimers.current;
    const generations = pollingGeneration.current;
    isMounted.current = true;
    return () => {
      isMounted.current = false;
      timers.forEach(clearTimeout);
      timers.clear();
      generations.clear();
    };
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
                      modalManager.show(
                        <BackgroundOperations
                          isExplicitClick
                          isOpen
                          onClose={() => {
                            modalManager.hide();
                          }}
                          rootLevel={ViewLevel.HOSTS}
                          requestId={
                            operation.requestId || operation?.requestInfo?.id
                          }
                        />
                      );
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
                {isFailed(operation.status) ||
                (isFinished(operation.status) &&
                  operation.status !== ProgressStatus.COMPLETED) ? (
                  <Button
                    size="sm"
                    onClick={() => retryOperation(operation.id)}
                    disabled={skippingOperationId !== null}
                    variant="success"
                    className="mx-2"
                  >
                    <FontAwesomeIcon className="me-2" icon={faUndo} />
                    Retry Operation
                  </Button>
                ) : null}
                {operation.skippable &&
                operation.skipCallback &&
                isFailed(operation.status) ? (
                  <Button
                    size="sm"
                    onClick={() => void skipOperation(operation.id)}
                    disabled={skippingOperationId !== null}
                    variant="warning"
                    className="mx-2"
                  >
                    {skippingOperationId == operation.id
                      ? "Skipping..."
                      : "Skip Operation"}
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
                (!errorCallback ? (
                  <Alert variant="danger" className="scrollable-h15 mt-3">
                    {operation.error ||
                      "Error: An error occurred during the operation."}
                  </Alert>
                ) : null)}
            </Stack>
          );
        })}
      </Stack>
    </div>
  );
}

export default OperationsProgress;
