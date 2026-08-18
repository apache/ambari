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

type Operation = {
  id: string | number;
  label: string;
  callback: any;
  skippable: boolean;
  requestId?: string | number;
  status?: string;
  progress?: number;
  error?: string;
  requestInfo?: any;
};

type PropTypes = {
  title: string;
  description: string;
  setCompletionStatus: (completed: boolean) => void;
  operations: Operation[];
  dispatch?: (operationsState: any) => void | Promise<void>;
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
  const [activeOperationId, setActiveOperationId] = useState<
    string | number | null
  >(null);
  const [persistenceError, setPersistenceError] = useState("");
  const [isRetryingPersistence, setIsRetryingPersistence] = useState(false);
  const { clusterName } = useContext(AppContext);
  const startedTasks = useRef<Set<string | number>>(new Set());
  const operationsStateRef = useRef(operations);
  const pollingTimers = useRef<Set<number>>(new Set());
  const pendingPersistence = useRef<{
    operations: Operation[];
    continuation?: () => void | Promise<void>;
  } | null>(null);
  const isMounted = useRef(true);

  const updateOperationsState = (nextOperations: typeof operations) => {
    operationsStateRef.current = nextOperations;
    if (isMounted.current) {
      setOperationsState(nextOperations);
    }
  };

  const persistenceMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

  const persistAndContinue = async (
    nextOperations: Operation[],
    continuation?: () => void | Promise<void>,
    fallbackMessage = "Ambari could not persist the operation checkpoint.",
  ) => {
    updateOperationsState(nextOperations);
    try {
      if (dispatch) {
        await dispatch(nextOperations);
      }
    } catch (error: any) {
      pendingPersistence.current = {
        operations: nextOperations,
        continuation,
      };
      if (isMounted.current) {
        setPersistenceError(persistenceMessage(error, fallbackMessage));
      }
      return false;
    }
    pendingPersistence.current = null;
    if (isMounted.current) {
      setPersistenceError("");
    }
    await continuation?.();
    return true;
  };

  const retryPersistence = async () => {
    const pending = pendingPersistence.current;
    if (!pending || isRetryingPersistence) return;
    setIsRetryingPersistence(true);
    try {
      if (dispatch) {
        await dispatch(pending.operations);
      }
      pendingPersistence.current = null;
      if (isMounted.current) {
        setPersistenceError("");
      }
      await pending.continuation?.();
    } catch (error: any) {
      if (isMounted.current) {
        setPersistenceError(
          persistenceMessage(
            error,
            "Ambari could not persist the operation checkpoint.",
          ),
        );
      }
    } finally {
      if (isMounted.current) {
        setIsRetryingPersistence(false);
      }
    }
  };

  const scheduleStatusPoll = (
    requestId: string | number,
    operationId: string | number,
  ) => {
    if (!isMounted.current) return;
    const timer = window.setTimeout(() => {
      pollingTimers.current.delete(timer);
      void trackCurrentRequestStatus(requestId, operationId);
    }, 2000);
    pollingTimers.current.add(timer);
  };

  const trackCurrentRequestStatus = async (
    requestId: string | number,
    operationId: string | number,
  ) => {
    const operationsStateCopy = cloneDeep(operationsStateRef.current);
    const trackingStatusForOperation: any = operationsStateCopy.find(
      (operation) => operation.id == operationId
    );
    if (requestId) {
      set(trackingStatusForOperation, "requestId", requestId);
      let requestStatus;
      try {
        requestStatus = await RequestApi.getRequestStatus(
          clusterName,
          String(requestId)
        );
      } catch (error) {
        set(
          trackingStatusForOperation,
          "error",
          "Ambari could not read the request status. Polling will retry.",
        );
        updateOperationsState(operationsStateCopy);
        scheduleStatusPoll(requestId, operationId);
        return;
      }
      if (requestStatus?.Requests?.request_status) {
        const { Requests } = requestStatus;
        set(trackingStatusForOperation, "status", Requests?.request_status);
        set(trackingStatusForOperation, "progress", Requests?.progress_percent);
        set(trackingStatusForOperation, "requestInfo", Requests);
        if (has(trackingStatusForOperation, "error")) {
          delete trackingStatusForOperation.error;
        }


        await persistAndContinue(operationsStateCopy, () => {
          if (isFinished(Requests.request_status)) {
            moveToNextOperation(operationsStateCopy, operationId);
          } else {
            scheduleStatusPoll(requestId, operationId);
          }
        });
      } else {
        set(
          trackingStatusForOperation,
          "error",
          "Ambari returned an incomplete request status. Polling will retry.",
        );
        updateOperationsState(operationsStateCopy);
        scheduleStatusPoll(requestId, operationId);
      }
    }
  };

  const moveToNextOperation = (
    currentOperations: Operation[],
    operationId: string | number,
  ) => {
    const currentActiveOperation = currentOperations.find(
      (operation) => operation.id == operationId,
    );
    if (
      currentActiveOperation &&
      isFinished(currentActiveOperation?.status || "")
    ) {
      if (!isFailed(currentActiveOperation?.status || "")) {
        if (operationId == currentOperations.at(-1)?.id) {
          setCompletionStatus(true);
        } else {
          const currentActiveIndex = currentOperations.findIndex(
            (operation) => operation.id == operationId,
          );
          setActiveOperationId(currentOperations[currentActiveIndex + 1]?.id);
        }
      }
    }
  };

  const persistFailedOperation = async (
    operationId: string | number,
    error: any,
  ) => {
    const operationsStateCopy = cloneDeep(operationsStateRef.current);
    const matchingOperation: any = operationsStateCopy.find(
      (operation) => operation.id == operationId,
    );
    if (!matchingOperation) return;
    const statusCode = get(error, "status", get(error, "response.status", ""));
    const errorMessage =
      get(error, "response.data.message", "") ||
      error?.message ||
      "The operation failed.";
    set(matchingOperation, "status", ProgressStatus.FAILED);
    set(
      matchingOperation,
      "error",
      statusCode ? `Error ${statusCode}: ${errorMessage}` : errorMessage,
    );
    await persistAndContinue(operationsStateCopy);
  };

  const runOperationCallback = async (operationId: string | number) => {
    const matchingOperation = operationsStateRef.current.find(
      (operation) => operation.id == operationId,
    );
    if (!matchingOperation) return;
    try {
      const response: OperationRequestResponse =
        await matchingOperation.callback();
      const operationsStateCopy = cloneDeep(operationsStateRef.current);
      const updatedOperation: any = operationsStateCopy.find(
        (operation) => operation.id == operationId,
      );
      if (!updatedOperation) return;

      if (response?.Requests) {
        set(updatedOperation, "requestId", response.Requests.id);
        set(updatedOperation, "status", ProgressStatus.IN_PROGRESS);
        await persistAndContinue(
          operationsStateCopy,
          () => trackCurrentRequestStatus(response.Requests.id, operationId),
          "Ambari could not persist the Ambari request ID.",
        );
        return;
      }

      const statusCode = get(response, "[0].status", response?.status);
      if (
        (statusCode && statusCode >= 200 && statusCode < 300) ||
        !response
      ) {
        set(updatedOperation, "status", ProgressStatus.COMPLETED);
        delete updatedOperation.error;
      } else {
        set(updatedOperation, "status", ProgressStatus.FAILED);
        set(
          updatedOperation,
          "error",
          statusCode
            ? `Unknown Status Code (${statusCode}): ${JSON.stringify(response)}`
            : `Unknown response format: ${JSON.stringify(response)}`,
        );
      }
      await persistAndContinue(operationsStateCopy, () =>
        moveToNextOperation(operationsStateCopy, operationId),
      );
    } catch (error: any) {
      await persistFailedOperation(operationId, error);
    }
  };

  async function executeTask(
    operationId: string | number | null = activeOperationId,
    forceRetry = false,
  ) {
    if (operationId === null || startedTasks.current.has(operationId)) return;
    startedTasks.current.add(operationId);
    const matchingOperation = operationsStateRef.current.find(
      (operation) => operation.id == operationId,
    );
    if (!matchingOperation) return;

    if (
      !forceRetry &&
      matchingOperation.status === ProgressStatus.IN_PROGRESS &&
      matchingOperation.requestId
    ) {
      await trackCurrentRequestStatus(matchingOperation.requestId, operationId);
      return;
    }
    if (!forceRetry && matchingOperation.status === "QUEUED") {
      await runOperationCallback(operationId);
      return;
    }
    if (!forceRetry && isFinished(matchingOperation.status || "")) return;

    const operationsStateCopy = cloneDeep(operationsStateRef.current);
    const queuedOperation: any = operationsStateCopy.find(
      (operation) => operation.id == operationId,
    );
    set(queuedOperation, "status", "QUEUED");
    delete queuedOperation.error;
    await persistAndContinue(
      operationsStateCopy,
      () => runOperationCallback(operationId),
      "Ambari could not persist the task checkpoint before execution.",
    );
  }

  const retryOperation = () => {
    if (activeOperationId === null) return;
    startedTasks.current.delete(activeOperationId);
    void executeTask(activeOperationId, true);
  };

  useEffect(() => {
    if (activeOperationId !== null) {
      void executeTask(activeOperationId);
    }
  }, [activeOperationId]);

  useEffect(() => {
    let toBeStartedIdx = 0;
    while (
      toBeStartedIdx < operations.length &&
      operations[toBeStartedIdx].status === ProgressStatus.COMPLETED
    ) {
      startedTasks.current.add(operations[toBeStartedIdx].id);
      toBeStartedIdx += 1;
    }
    if (toBeStartedIdx >= operations.length && operations.length) {
      setCompletionStatus(true);
      return;
    }
    const operation = operations[toBeStartedIdx];
    if (operation && isFailed(operation.status || "")) {
      startedTasks.current.add(operation.id);
    }
    setActiveOperationId(operation?.id ?? null);
  }, []);

  useEffect(() => {
    return () => {
      isMounted.current = false;
      pollingTimers.current.forEach((timer) => window.clearTimeout(timer));
      pollingTimers.current.clear();
    };
  }, []);

  return (
    <div className="p-3">
      {persistenceError ? (
        <Alert variant="danger">
          {persistenceError}
          <Button
            size="sm"
            className="ms-3"
            disabled={isRetryingPersistence}
            onClick={() => void retryPersistence()}
          >
            Retry checkpoint
          </Button>
        </Alert>
      ) : null}
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
                !isFinished(operation.status) && (
                  <Alert variant="warning" className="scrollable-h15 mt-3">
                    {operation.error}
                  </Alert>
                )}

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
