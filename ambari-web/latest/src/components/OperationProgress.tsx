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
import { cloneDeep, filter, findIndex, get, has, set } from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import usePolling from "../hooks/usePolling";
import { RequestApi } from "../api/requestApi";
import { AppContext } from "../store/context";
import { isFailed, isFinished } from "../Utils/Utility";
import { ProgressStatus} from "../constants";
import { Button, ProgressBar, Stack } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCircleCheck,
  faTimes,
  faUndo,
} from "@fortawesome/free-solid-svg-icons";
// import modalManager from "../store/ModalManager";
// import BackgroundOperations from "../screens/BackgroundOperations";

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
    }
  ];
  dispatch?: (operationsState: any) => void;
};

type OperationRequestResponse = {
  Requests: {
    id: number | string;
    status: string;
  };
  href: string;
  status?:number;
};

function OperationsProgress({
  // title,
  // description,
  setCompletionStatus,
  operations,
  dispatch,
}: PropTypes) {
  const [operationsState, setOperationsState] = useState(operations);
  const operationsRef = useRef(operations);
  const activeOperationId = useRef<any>(null);
  const { clusterName } = useContext(AppContext);
  const { stopPolling, pausePolling, resumePolling } = usePolling(
    trackCurrentRequestStatus
  );
  const activeRequestId = useRef<string|number>(0);
  const startedTasks: any = useRef([]);
  async function trackCurrentRequestStatus() {
    const operationsStateCopy = cloneDeep(operationsRef.current);
    const trackingStatusForOperation: any = operationsStateCopy.find(
      (operation) => operation.id == activeOperationId.current
    );
    if (activeRequestId.current) {
      const activeRequestStatus = await RequestApi.getRequestStatus(
        clusterName,
        activeRequestId.current as any
      );
      const { Requests } = activeRequestStatus;
      if (activeRequestStatus?.Requests?.request_status) {
        set(
          trackingStatusForOperation,
          "requestId",
          activeRequestStatus?.Requests?.id
        );
        set(
          trackingStatusForOperation,
          "status",
          activeRequestStatus?.Requests?.request_status||"FAILED"
        );
        set(
          trackingStatusForOperation,
          "progress",
          activeRequestStatus?.Requests?.progress_percent
        );
        set(
          trackingStatusForOperation,
          "requestInfo",
          activeRequestStatus?.Requests
        );
        const requestStages = filter(
          activeRequestStatus.stages,
          function (stage) {
            return has(stage, "Stage.context");
          }
        );
        set(trackCurrentRequestStatus, "stages", requestStages);
        setOperationsState(operationsStateCopy);
        operationsRef.current = operationsStateCopy;
        console.log("Current request status is", Requests.request_status);
        if (isFinished(Requests.request_status)) {
          console.log("Operation Progress operation finished");
          if (Requests.request_status === ProgressStatus.FAILED) {
            pausePolling();
          } else {
            if (activeOperationId.current == (operations as any)?.at(-1)?.id) {
              stopPolling();
              setCompletionStatus(true);
            } else {
              const currentActiveIndex = findIndex(operations, [
                "id",
                Number(activeOperationId?.current),
              ]);
              executeTask(operationsRef.current[Number(currentActiveIndex) + 1]?.id);
            }
          }
        }
      }
    }
  }
  async function executeTask(id: string | number) {
    id = Number(id);
    if (!startedTasks.current.includes(id)) {
      activeOperationId.current = id;
      startedTasks.current.push(id);
      const operationsStateCopy = cloneDeep(operationsRef.current);
      const matchingOperation: any = operationsStateCopy.find(
        (operation) => operation.id == id
      );
      if (matchingOperation) {
        try {
        const operationCallbackResponse:OperationRequestResponse = await matchingOperation?.callback();
        if (operationCallbackResponse?.Requests) {
          matchingOperation.requestId = operationCallbackResponse?.Requests?.id;
          activeRequestId.current = operationCallbackResponse?.Requests?.id;
        }
        //TODO: @vhassija Please verify for all statusCode
        else if(operationCallbackResponse?.status === 200|| !operationCallbackResponse){
          if (activeOperationId.current == (operationsRef.current as any)?.at(-1)?.id) {
            stopPolling();
            setCompletionStatus(true);
          } else {
            const currentActiveIndex = operationsRef.current.findIndex((operation) => operation.id == activeOperationId.current);
            executeTask(operationsRef.current[Number(currentActiveIndex) + 1]?.id);
          }
        }
        else{
          console.error("Operation failed with response", operationCallbackResponse);
          matchingOperation.status = "FAILED";
        }
        }
      catch(err){
        console.error("Got request", err)
        matchingOperation.status = "FAILED";

      }
      }
      setOperationsState(operationsStateCopy);
      operationsRef.current = operationsStateCopy;
    }
  }
  const retryOperation = () => {
    startedTasks.current = startedTasks.current.filter((task:any) => {
      task != activeOperationId.current;
    });
    executeTask(activeOperationId.current as any);
    resumePolling();
  };
  const renderStagesForOperation = (operation: any) => {
    return (
      <Stack direction="vertical">
        {operation.stages.map((stage: any) => {
          return (
            <Stack
              direction="horizontal"
              className="justify-content-between mt-3"
              key={stage.context}
            >
              <div className="d-flex align-items-center">
                {isFinished(stage.status) && (
                  <FontAwesomeIcon icon={faCircleCheck} color="success" />
                )}
                {isFailed(stage.status) && (
                  <FontAwesomeIcon icon={faTimes} color="danger" />
                )}
                <div>{stage.context} </div>
                {isFailed(stage.status) ? (
                  <Button
                    size="sm"
                    onClick={retryOperation}
                    variant="success"
                    className="ms-2"
                  >
                    <FontAwesomeIcon className="me-2" icon={faUndo} />
                    Retry Operation
                  </Button>
                ) : null}
              </div>
              {get(stage, "progress_percent", 0) &&
              !isFinished(stage.status) ? (
                <ProgressBar
                  striped
                  className={`w-25`}
                  variant="info"
                  now={stage.progress_percent}
                  label={`${Math.floor(stage.progress)}%`}
                />
              ) : null}
            </Stack>
          );
        })}
      </Stack>
    );
  };

  useEffect(() => {
    if(dispatch){
      dispatch(operationsState);
    }
  }, [JSON.stringify(operationsState)]);

  useEffect(() => {
    let idx = -1;
    for (let i = operationsRef.current.length - 1; i >= 0; i--) {
      if (
        get(operationsRef.current[i], "requestId", "") ||
        get(operationsRef.current[i], "status", "")
      ) {
        idx = i;
        activeOperationId.current = operationsRef.current?.[i]?.id;
        break;
      }
    }
    if (idx === -1) {
      executeTask(operationsRef.current?.[0]?.id);
    } else {
      for (let i = 0; i <= idx; i++) {
        if (
          get(operationsRef.current[i], "requestId", "") ||
          get(operationsRef.current[i], "status", "")
        ) {
          startedTasks.current.push(operationsRef.current[i].id);
        }
      }
    }
  }, []);


  return (
    <div className="p-3">
      <Stack direction="vertical">
        {operationsState.map((operation: any) => {
          const operationStages = operation.stages || [];
          if (operationStages.length) {
            return renderStagesForOperation(operation);
          } else {
            return (
              <Stack
                direction="horizontal"
                className="justify-content-between mt-3"
                key={operation.label}
              >
                <div className="d-flex align-items-center">
                  <div
                    onClick={() => {
                    //   modalManager.show(
                    //     <BackgroundOperations
                    //       isOpen
                    //       onClose={() => {
                    //         modalManager.hide();
                    //       }}
                    //       rootLevel={ViewLevel.HOSTS}
                    //       requestId={
                    //         operation.requestId || operation?.requestInfo?.id
                    //       }
                    //     />
                    //   );
                    }}
                    className={`${
                      isFinished(operation.status) ||
                      has(operation, "progress") ||
                      has(operation, "requestId")
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
                      className="ms-2"
                    >
                      <FontAwesomeIcon className="me-2" icon={faUndo} />
                      Retry Operation
                    </Button>
                  ) : null}
                </div>
                {has(operation, "progress") && !isFinished(operation.status) ? (
                  <ProgressBar
                    striped
                    className={`w-25`}
                    variant="info"
                    now={operation.progress}
                    label={`${Math.floor(operation.progress)}%`}
                  />
                ) : null}
              </Stack>
            );
          }
        })}
      </Stack>
    </div>
  );
}

export default OperationsProgress;
