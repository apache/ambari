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

import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { cloneDeep, get, has, set } from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Button, ProgressBar, Stack } from "react-bootstrap";
import { RequestApi } from "../api/requestApi";
import { ProgressStatus, ViewLevel } from "../constants";
import BackgroundOperations from "../screens/BackgroundOperations";
import { AppContext } from "../store/context";
import modalManager from "../store/ModalManager";
import { getStatusIcon } from "../Utils/statusIcons";
import { isFailed, isFinished } from "../Utils/Utility";

type Operation = {
  id: string | number;
  label: string;
  callback: () => Promise<unknown>;
  skippable: boolean;
  requestId?: string | number;
  status?: string;
  progress?: number;
  error?: string;
  requestInfo?: Record<string, unknown> & { id?: string | number };
  skipCallback?: () => Promise<unknown>;
  retryFromOperationId?: string | number;
};

type OperationResponse = {
  Requests?: { id: string | number; status?: string };
  status?: number;
};

type RequestError = {
  message?: string;
  status?: number;
  response?: { status?: number; data?: { message?: string } };
};

type PropTypes = {
  title: string;
  description: string;
  setCompletionStatus: (completed: boolean) => void;
  operations: Operation[];
  dispatch?: (operationsState: Operation[]) => void | Promise<void>;
  errorCallback?: (errorMsg: string) => void;
};

type PendingPersistence = {
  operations: Operation[];
  continuation?: () => void | Promise<void>;
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
  const [skippingOperationId, setSkippingOperationId] = useState<
    string | number | null
  >(null);
  const [persistenceError, setPersistenceError] = useState("");
  const [isRetryingPersistence, setIsRetryingPersistence] = useState(false);
  const { clusterName } = useContext(AppContext);
  const startedTasks = useRef<Set<string | number>>(new Set());
  const operationsStateRef = useRef(operations);
  const pollingTimers = useRef<Set<ReturnType<typeof setTimeout>>>(new Set());
  const operationGeneration = useRef<Map<string | number, number>>(new Map());
  const pendingPersistence = useRef<PendingPersistence | null>(null);
  const isMounted = useRef(true);
  const setCompletionStatusRef = useRef(setCompletionStatus);
  const errorCallbackRef = useRef(errorCallback);
  const reportedError = useRef("");
  const executeTaskRef = useRef<(
    operationId?: string | number | null,
    forceRetry?: boolean,
  ) => Promise<void>>(async () => undefined);
  setCompletionStatusRef.current = setCompletionStatus;
  errorCallbackRef.current = errorCallback;

  const updateOperationsState = (nextOperations: Operation[]) => {
    operationsStateRef.current = nextOperations;
    if (isMounted.current) setOperationsState(nextOperations);
  };

  const errorMessage = (error: unknown, fallback: string) => {
    const requestError = error as RequestError;
    return requestError.response?.data?.message || requestError.message || fallback;
  };

  const getGeneration = (operationId: string | number) =>
    operationGeneration.current.get(operationId) ?? 0;

  const invalidateOperation = (operationId: string | number) => {
    operationGeneration.current.set(operationId, getGeneration(operationId) + 1);
  };

  const persistAndContinue = async (
    nextOperations: Operation[],
    continuation?: () => void | Promise<void>,
    fallbackMessage = "Ambari could not persist the operation checkpoint.",
  ) => {
    updateOperationsState(nextOperations);
    try {
      await dispatch?.(nextOperations);
    } catch (error) {
      pendingPersistence.current = { operations: nextOperations, continuation };
      if (isMounted.current) {
        setPersistenceError(errorMessage(error, fallbackMessage));
      }
      return false;
    }
    pendingPersistence.current = null;
    if (!isMounted.current) return false;
    setPersistenceError("");
    await continuation?.();
    return true;
  };

  const retryPersistence = async () => {
    const pending = pendingPersistence.current;
    if (!pending || isRetryingPersistence) return;
    setIsRetryingPersistence(true);
    try {
      await dispatch?.(pending.operations);
      pendingPersistence.current = null;
      if (!isMounted.current) return;
      setPersistenceError("");
      await pending.continuation?.();
    } catch (error) {
      if (isMounted.current) {
        setPersistenceError(
          errorMessage(
            error,
            "Ambari could not persist the operation checkpoint.",
          ),
        );
      }
    } finally {
      if (isMounted.current) setIsRetryingPersistence(false);
    }
  };

  const moveToNextOperation = (
    currentOperations: Operation[],
    operationId: string | number,
  ) => {
    const currentOperation = currentOperations.find(
      (operation) => operation.id == operationId,
    );
    if (!currentOperation || currentOperation.status !== ProgressStatus.COMPLETED) {
      return;
    }
    if (operationId == currentOperations.at(-1)?.id) {
      setCompletionStatusRef.current(true);
      return;
    }
    const currentIndex = currentOperations.findIndex(
      (operation) => operation.id == operationId,
    );
    setActiveOperationId(currentOperations[currentIndex + 1]?.id ?? null);
  };

  const scheduleStatusPoll = (
    requestId: string | number,
    operationId: string | number,
    generation: number,
  ) => {
    if (!isMounted.current || generation !== getGeneration(operationId)) return;
    const timer = setTimeout(() => {
      pollingTimers.current.delete(timer);
      void trackCurrentRequestStatus(requestId, operationId, generation);
    }, 2000);
    pollingTimers.current.add(timer);
  };

  const persistFailedOperation = async (
    operationId: string | number,
    error: unknown,
    generation = getGeneration(operationId),
  ) => {
    if (!isMounted.current || generation !== getGeneration(operationId)) return;
    const failedState = cloneDeep(operationsStateRef.current);
    const failedOperation = failedState.find(
      (operation) => operation.id == operationId,
    );
    if (!failedOperation) return;
    const statusCode = get(error, "response.status", get(error, "status", ""));
    const message = errorMessage(error, "The operation failed.");
    set(failedOperation, "status", ProgressStatus.FAILED);
    set(failedOperation, "error", statusCode ? `Error ${statusCode}: ${message}` : message);
    await persistAndContinue(failedState);
  };

  const trackCurrentRequestStatus = async (
    requestId: string | number,
    operationId: string | number,
    generation = getGeneration(operationId),
  ) => {
    if (!requestId || generation !== getGeneration(operationId)) return;
    try {
      const requestStatus = await RequestApi.getRequestStatus(
        clusterName,
        String(requestId),
      );
      if (!isMounted.current || generation !== getGeneration(operationId)) return;

      const nextState = cloneDeep(operationsStateRef.current);
      const trackedOperation = nextState.find(
        (operation) => operation.id == operationId,
      );
      if (
        !trackedOperation ||
        String(trackedOperation.requestId) !== String(requestId)
      ) {
        return;
      }
      const request = requestStatus?.Requests;
      if (!request?.request_status) {
        throw new Error("Ambari returned an invalid request status response.");
      }

      const requestState = ["ABORTED", "TIMEDOUT"].includes(
        request.request_status,
      )
        ? ProgressStatus.FAILED
        : request.request_status;
      set(trackedOperation, "status", requestState);
      set(trackedOperation, "progress", request.progress_percent);
      set(trackedOperation, "requestInfo", request);
      delete trackedOperation.error;

      await persistAndContinue(nextState, () => {
        if (isFinished(requestState)) {
          moveToNextOperation(nextState, operationId);
        } else {
          scheduleStatusPoll(requestId, operationId, generation);
        }
      });
    } catch (error) {
      await persistFailedOperation(operationId, error, generation);
    }
  };

  const runOperationCallback = async (
    operationId: string | number,
    generation: number,
  ) => {
    const operation = operationsStateRef.current.find(
      (candidate) => candidate.id == operationId,
    );
    if (!operation || generation !== getGeneration(operationId)) return;
    try {
      const response = await operation.callback() as
        | OperationResponse
        | Array<{ status?: number }>
        | undefined;
      if (!isMounted.current || generation !== getGeneration(operationId)) return;
      const nextState = cloneDeep(operationsStateRef.current);
      const updatedOperation = nextState.find(
        (candidate) => candidate.id == operationId,
      );
      if (!updatedOperation) return;

      if (!Array.isArray(response) && response?.Requests) {
        const requestId = response.Requests.id;
        set(updatedOperation, "requestId", requestId);
        set(updatedOperation, "status", ProgressStatus.IN_PROGRESS);
        await persistAndContinue(
          nextState,
          () => trackCurrentRequestStatus(requestId, operationId, generation),
          "Ambari could not persist the Ambari request ID.",
        );
        return;
      }

      const statusCode = Array.isArray(response)
        ? response[0]?.status
        : response?.status;
      if (
        (statusCode !== undefined && statusCode >= 200 && statusCode < 300) ||
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
      await persistAndContinue(nextState, () =>
        moveToNextOperation(nextState, operationId),
      );
    } catch (error) {
      await persistFailedOperation(operationId, error, generation);
    }
  };

  async function executeTask(
    operationId: string | number | null = activeOperationId,
    forceRetry = false,
  ) {
    if (operationId === null || startedTasks.current.has(operationId)) return;
    startedTasks.current.add(operationId);
    const operation = operationsStateRef.current.find(
      (candidate) => candidate.id == operationId,
    );
    if (!operation) return;
    const generation = getGeneration(operationId);

    if (
      !forceRetry &&
      operation.requestId &&
      !isFinished(operation.status || "")
    ) {
      await trackCurrentRequestStatus(operation.requestId, operationId, generation);
      return;
    }
    if (!forceRetry && operation.status === "QUEUED") {
      await runOperationCallback(operationId, generation);
      return;
    }
    if (!forceRetry && operation.status === ProgressStatus.IN_PROGRESS) {
      await persistFailedOperation(
        operationId,
        new Error(
          "The persisted operation has no Ambari request ID and cannot be recovered.",
        ),
        generation,
      );
      return;
    }
    if (!forceRetry && isFinished(operation.status || "")) return;

    const queuedState = cloneDeep(operationsStateRef.current);
    const queuedOperation = queuedState.find(
      (candidate) => candidate.id == operationId,
    );
    if (!queuedOperation) return;
    set(queuedOperation, "status", "QUEUED");
    delete queuedOperation.error;
    await persistAndContinue(
      queuedState,
      () => runOperationCallback(operationId, generation),
      "Ambari could not persist the task checkpoint before execution.",
    );
  }
  executeTaskRef.current = executeTask;

  const retryOperation = (operationId: string | number) => {
    if (pendingPersistence.current) return;
    const retryState = cloneDeep(operationsStateRef.current);
    const failedOperation = retryState.find(
      (operation) => operation.id == operationId,
    );
    if (!failedOperation) return;
    const retryOperationId = failedOperation.retryFromOperationId ?? operationId;
    const retryIndex = retryState.findIndex(
      (operation) => operation.id == retryOperationId,
    );
    if (retryIndex < 0) return;

    retryState.slice(retryIndex).forEach((operation) => {
      invalidateOperation(operation.id);
      startedTasks.current.delete(operation.id);
      delete operation.requestId;
      delete operation.requestInfo;
      delete operation.progress;
      delete operation.error;
      delete operation.status;
    });
    setCompletionStatusRef.current(false);
    setActiveOperationId(retryOperationId);
    void persistAndContinue(retryState, () => executeTask(retryOperationId));
  };

  const skipOperation = async (operationId: string | number) => {
    if (skippingOperationId !== null || pendingPersistence.current) return;
    const operation = operationsStateRef.current.find(
      (candidate) => candidate.id == operationId,
    );
    if (!operation?.skippable || !operation.skipCallback) return;

    invalidateOperation(operationId);
    const generation = getGeneration(operationId);
    setSkippingOperationId(operationId);
    const pendingState = cloneDeep(operationsStateRef.current);
    const pendingOperation = pendingState.find(
      (candidate) => candidate.id == operationId,
    );
    if (!pendingOperation) return;
    delete pendingOperation.requestId;
    delete pendingOperation.requestInfo;
    delete pendingOperation.progress;
    delete pendingOperation.error;
    set(pendingOperation, "status", ProgressStatus.IN_PROGRESS);
    updateOperationsState(pendingState);

    try {
      const response = await operation.skipCallback() as
        | OperationResponse
        | Array<{ status?: number }>
        | undefined;
      if (!isMounted.current || generation !== getGeneration(operationId)) return;
      const nextState = cloneDeep(operationsStateRef.current);
      const skippedOperation = nextState.find(
        (candidate) => candidate.id == operationId,
      );
      if (!skippedOperation) return;
      if (!Array.isArray(response) && response?.Requests?.id) {
        const requestId = response.Requests.id;
        set(skippedOperation, "requestId", requestId);
        set(skippedOperation, "status", ProgressStatus.IN_PROGRESS);
        await persistAndContinue(
          nextState,
          () => trackCurrentRequestStatus(requestId, operationId, generation),
          "Ambari could not persist the Ambari request ID.",
        );
      } else {
        const statusCode = Array.isArray(response)
          ? response[0]?.status
          : response?.status;
        if (
          response &&
          !(statusCode !== undefined && statusCode >= 200 && statusCode < 300)
        ) {
          throw new Error("Ambari returned an invalid skip response.");
        }
        set(skippedOperation, "status", ProgressStatus.COMPLETED);
        await persistAndContinue(nextState, () =>
          moveToNextOperation(nextState, operationId),
        );
      }
    } catch (error) {
      await persistFailedOperation(operationId, error, generation);
    } finally {
      if (isMounted.current) setSkippingOperationId(null);
    }
  };

  useEffect(() => {
    if (activeOperationId !== null) {
      void executeTaskRef.current(activeOperationId);
    }
  }, [activeOperationId]);

  useEffect(() => {
    const failedOperation = operationsState.find(
      (operation) => operation.error && isFinished(operation.status || ""),
    );
    if (!failedOperation) {
      reportedError.current = "";
      return;
    }
    const message =
      failedOperation.error || "Error: An error occurred during the operation.";
    const errorKey = `${failedOperation.id}:${failedOperation.status}:${message}`;
    if (errorCallbackRef.current && reportedError.current !== errorKey) {
      reportedError.current = errorKey;
      errorCallbackRef.current(message);
    }
  }, [operationsState]);

  useEffect(() => {
    let activeIndex = 0;
    while (
      activeIndex < operationsStateRef.current.length &&
      operationsStateRef.current[activeIndex].status === ProgressStatus.COMPLETED
    ) {
      startedTasks.current.add(operationsStateRef.current[activeIndex].id);
      activeIndex += 1;
    }
    if (activeIndex >= operationsStateRef.current.length) {
      setCompletionStatusRef.current(true);
      return;
    }
    const activeOperation = operationsStateRef.current[activeIndex];
    if (isFinished(activeOperation.status || "")) {
      startedTasks.current.add(activeOperation.id);
    }
    setActiveOperationId(activeOperation.id);
  }, []);

  useEffect(() => {
    isMounted.current = true;
    const timers = pollingTimers.current;
    const generations = operationGeneration.current;
    return () => {
      isMounted.current = false;
      timers.forEach(clearTimeout);
      timers.clear();
      generations.clear();
    };
  }, []);

  return (
    <div className="p-3">
      {persistenceError && (
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
      )}
      <Stack direction="vertical">
        {operationsState.map((operation) => (
          <Stack
            direction="horizontal"
            className="justify-content-between mt-3"
            key={operation.id}
          >
            <div className="d-flex align-items-center">
              {getStatusIcon(operation.status)}
              <div
                onClick={() => {
                  if (operation.requestId || operation.requestInfo?.id) {
                    modalManager.show(
                      <BackgroundOperations
                        isExplicitClick
                        isOpen
                        onClose={() => modalManager.hide()}
                        rootLevel={ViewLevel.HOSTS}
                        requestId={operation.requestId || operation.requestInfo?.id}
                      />,
                    );
                  }
                }}
                className={
                  operation.requestId || operation.requestInfo?.id
                    ? "custom-link"
                    : ""
                }
              >
                {operation.label}{" "}
              </div>
              {isFailed(operation.status || "") && (
                <Button
                  size="sm"
                  onClick={() => retryOperation(operation.id)}
                  disabled={
                    skippingOperationId !== null || Boolean(persistenceError)
                  }
                  variant="success"
                  className="mx-2"
                >
                  <FontAwesomeIcon className="me-2" icon={faUndo} />
                  Retry Operation
                </Button>
              )}
              {operation.skippable &&
                operation.skipCallback &&
                isFailed(operation.status || "") && (
                  <Button
                    size="sm"
                    onClick={() => void skipOperation(operation.id)}
                    disabled={
                      skippingOperationId !== null || Boolean(persistenceError)
                    }
                    variant="warning"
                    className="mx-2"
                  >
                    {skippingOperationId == operation.id
                      ? "Skipping..."
                      : "Skip Operation"}
                  </Button>
                )}
            </div>
            {has(operation, "progress") && !isFinished(operation.status || "") && (
              <ProgressBar
                className="w-25"
                variant="info"
                now={operation.progress}
                label={`${Math.floor(operation.progress || 0)}%`}
              />
            )}
            {operation.error && !isFinished(operation.status || "") && (
              <Alert variant="warning" className="scrollable-h15 mt-3">
                {operation.error}
              </Alert>
            )}
            {operation.error &&
              isFinished(operation.status || "") &&
              !errorCallback && (
                <Alert variant="danger" className="scrollable-h15 mt-3">
                  {operation.error}
                </Alert>
              )}
          </Stack>
        ))}
      </Stack>
    </div>
  );
}

export default OperationsProgress;
