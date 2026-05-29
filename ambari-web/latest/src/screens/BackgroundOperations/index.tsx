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
import ClusterApi from "../../api/clusterApi";
import {  filter, get } from "lodash";
import { parseRequestContext, commandDetail } from "../../Utils/Utility";
import Table from "../../components/Table";
import {
  Button,
  Form,
  Modal,
  ModalTitle,
  ProgressBar,
  Stack,
} from "react-bootstrap";
import dayjs from "dayjs";
import HostProgress from "./HostProgress";
import { AppContext } from "../../store/context";
import ConfirmationModal from "../../components/ConfirmationModal";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheck,
  faClock,
  faCog,
  faCogs,
  faExclamation,
  faMinus,
  faStop,
} from "@fortawesome/free-solid-svg-icons";
import Tooltip from "../../components/Tooltip";
import { ViewLevel } from "../../constants";
import TasksList from "./TasksList";
import TaskLogs from "./TaskLogs";
import Filters from "./Filters";
import Center from "../../components/Center";
import { calculateDurationSummary, isOperationRunning } from "../../Utils/dateUtils";

type PropTypes = {
  isOpen: boolean;
  onClose: any;
  rootLevel?: ViewLevel;
  requestId?: string | number;
  host?: string;
  clusterName?: string;
  isExplicitClick?: boolean;
};

export function isUpgradeRequest(request: any): boolean {
  const context = request?.Requests?.request_context || request?.request_context;
  return context ? /(upgrading|downgrading)/.test(context.toLowerCase()) : false;
}

function BackgroundOperations({
  isOpen,
  onClose,
  rootLevel = ViewLevel.REQUESTS,
  requestId = 0,
  host = "",
  clusterName,
  isExplicitClick = false,
}: PropTypes) {
  const [selectedRequest, setSelectedRequest] = useState<any>({});
  const [selectedRequestId, setSelectedRequestId] = useState(requestId);
  const [filteredClusterRequests, setFilteredClusterRequests] = useState<any[]>([]);
  const [selectedFilter, setSelectedFilter] = useState<any>(null);
  const [noMoreRequestsToShow, setNoMoreRequestsToShow] = useState(false);
  const [pageSize, setPageSize] = useState<number>(20);
  const [latestMessage, setLatestMessage] = useState({});
  const [isLoadingRequests, setIsLoadingRequests] = useState(false);
  const {
    clusterName: cName,
    userBgPreferences,
    setUserBgPreferences,
    backgroundOperations,
    updateBackgroundOperations,
    runningOperationsCount,
  } = useContext(AppContext);
  
  // Check if this is truly the first load (no cached data)
  const isInitialLoad = backgroundOperations.length === 0;

  function getSetFilteredRequests() {
    if (selectedFilter && selectedFilter.value) {
      const filteredRequests = filter(
        backgroundOperations,
        (req: any) =>
          get(req, "Requests.request_status") ===
          selectedFilter.value.toUpperCase()
      );
      setFilteredClusterRequests(filteredRequests as any);
    } else {
      setFilteredClusterRequests(backgroundOperations);
    }
  }
  
  async function getClusterRequests() {
    try {
      setIsLoadingRequests(true);
      const allClusterRequests = await ClusterApi.getRequests(
        clusterName || cName,
        pageSize
      );
      if (Number(allClusterRequests.itemTotal) < pageSize) {
        setNoMoreRequestsToShow(true);
      }
      const newRequests = allClusterRequests.items.filter((request: any) => {
        return !isUpgradeRequest(request);
      });
      
      // Use global context update function (like Ember.js singleton)
      updateBackgroundOperations(newRequests);
    } catch (error) {
      console.error("Error fetching cluster requests:", error);
      // On error, keep existing requests if we have them (better UX)
      if (backgroundOperations.length === 0) {
        updateBackgroundOperations([]);
      }
    } finally {
      setIsLoadingRequests(false);
    }
  }
  async function getRequestDetails() {
    if (requestId) {
      const requestDetails = await ClusterApi.getRequestById(
        clusterName || cName,
        requestId
      );
      setSelectedRequest(requestDetails.Requests);
    }
  }
  const [selectedLevel, setSelectedLevel] = useState(rootLevel);
  const [showModal, setShowModal] = useState(false);
  const [showAbortedModal, setShowAbortedModal] = useState(false);
  const [selectedHost, setSelectedHost] = useState(host);
  const [selectedTask, setSelectedTask] = useState<any>({});
  const { client, isSocketConnected } = useContext(AppContext);
  
  useEffect(() => {
    getSetFilteredRequests();
  }, [backgroundOperations, selectedFilter]);
  
  // Add WebSocket subscription for real-time updates when modal is open
  useEffect(() => {
    let requestSubscription: any;
    if (isSocketConnected && isOpen && client) {
      requestSubscription = client.subscribe(
        "/events/requests",
        (subscriptionMessage: any) => {
          const message = JSON.parse(subscriptionMessage.body);
          
          if (isUpgradeRequest({Requests: {request_context: message.requestContext}})) {
            return;
          }
          
          setLatestMessage(message);
          
          // Update global background operations cache
          const updatedOperations = [...backgroundOperations];
          const matchingRequestIndex = updatedOperations.findIndex(
            (existing: any) => existing.Requests.id === message.requestId
          );
          
          if (matchingRequestIndex >= 0) {
            const { Tasks, ...restProperties } = message;
            const newRequestBody: any = {};
            for (const property in restProperties) {
              const transformedPropertyName = property
                .replace(/([A-Z])/g, "_$1")
                .toLowerCase();
              newRequestBody[transformedPropertyName] = restProperties[property];
            }
            
            
            // Update the global cache directly
            updatedOperations[matchingRequestIndex] = {
              ...updatedOperations[matchingRequestIndex],
              Requests: newRequestBody
            };
            
            updateBackgroundOperations(updatedOperations.map(op => op));
          } else {
            getClusterRequests();
          }
        }
      );
      
      return () => {
        if (requestSubscription) {
          requestSubscription.unsubscribe();
        }
      };
    }
  }, [isSocketConnected, isOpen, client, backgroundOperations]);
  function getBreadcrumbs() {
    const inferredBreadcrumbs = [];
    //@ts-expect-error
    const breadcrumbsConfig = {
      [ViewLevel.REQUESTS]: {
        breadcrumb: { name: "Background Operation", level: ViewLevel.REQUESTS },
        conditions: [{ active: true }],
      },
      [ViewLevel.HOSTS]: {
        breadcrumb: {
          name: parseRequestContext(get(selectedRequest, "request_context", ""))
            ?.requestContext,
          level: ViewLevel.HOSTS,
        },
        conditions: [
          { active: true, if: rootLevel === ViewLevel.HOSTS },
          {
            breadcrumb: {
              name: "Background Operation",
              level: ViewLevel.REQUESTS,
            },
          },
        ],
      },
      [ViewLevel.TASKS_LIST]: {
        breadcrumb: { name: selectedHost, level: ViewLevel.TASKS_LIST },
        conditions: [
          { active: true, if: rootLevel === ViewLevel.TASKS_LIST },
          {
            breadcrumb: {
              name: parseRequestContext(
                get(selectedRequest, "request_context", "")
              )?.requestContext,
              level: ViewLevel.HOSTS,
            },
            if: rootLevel === ViewLevel.HOSTS,
          },
          {
            breadcrumb: {
              name: "Background Operation",
              level: ViewLevel.REQUESTS,
            },
          },
        ],
      },
      [ViewLevel.TASK_LOGS]: {
        breadcrumb: {
          name: selectedTask.command_detail,
          level: ViewLevel.TASK_LOGS,
        },
        conditions: [
          { active: true, if: rootLevel === ViewLevel.TASK_LOGS },
          {
            breadcrumb: {
              name: parseRequestContext(
                get(selectedRequest, "request_context", "")
              )?.requestContext,
              level: ViewLevel.HOSTS,
            },
            if: rootLevel === ViewLevel.HOSTS,
          },
          { breadcrumb: { name: selectedHost, level: ViewLevel.TASKS_LIST } },
          {
            breadcrumb: {
              name: "Background Operation",
              level: ViewLevel.REQUESTS,
            },
          },
        ],
      },
    };

    switch (selectedLevel) {
      case ViewLevel.REQUESTS:
        inferredBreadcrumbs.push({
          name: "Background Operation",
          level: ViewLevel.REQUESTS,
          active: true,
        });
        break;
      case ViewLevel.HOSTS:
        if (rootLevel === ViewLevel.HOSTS) {
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
            active: true,
          });
        } else {
          inferredBreadcrumbs.push({
            name: "Background Operation",
            level: ViewLevel.REQUESTS,
          });
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
            active: true,
          });
        }
        break;
      case ViewLevel.TASKS_LIST:
        if (rootLevel === ViewLevel.TASKS_LIST) {
          inferredBreadcrumbs.push({
            name: selectedHost,
            level: ViewLevel.TASKS_LIST,
            active: true,
          });
        } else if (rootLevel === ViewLevel.HOSTS) {
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
          });
          inferredBreadcrumbs.push({
            name: selectedHost,
            level: ViewLevel.TASKS_LIST,
            active: true,
          });
        } else {
          inferredBreadcrumbs.push({
            name: "Background Operation",
            level: ViewLevel.REQUESTS,
          });
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
          });
          inferredBreadcrumbs.push({
            name: selectedHost,
            level: ViewLevel.TASKS_LIST,
            active: true,
          });
        }
        break;
      case ViewLevel.TASK_LOGS:
        if (rootLevel === ViewLevel.TASK_LOGS) {
          inferredBreadcrumbs.push({
            name: commandDetail(
              selectedTask.command_detail,
              selectedTask.request_inputs,
              selectedTask.ops_display_name
            ),
            level: ViewLevel.TASK_LOGS,
            active: true,
          });
        } else if (rootLevel === ViewLevel.HOSTS) {
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
          });
          inferredBreadcrumbs.push({
            name: selectedHost,
            level: ViewLevel.TASKS_LIST,
          });
          inferredBreadcrumbs.push({
            name: commandDetail(
              selectedTask.command_detail,
              selectedTask.request_inputs,
              selectedTask.ops_display_name
            ),
            level: ViewLevel.TASK_LOGS,
            active: true,
          });
        } else {
          inferredBreadcrumbs.push({
            name: "Background Operation",
            level: ViewLevel.REQUESTS,
          });
          inferredBreadcrumbs.push({
            name: parseRequestContext(
              get(selectedRequest, "request_context", "")
            )?.requestContext,
            level: ViewLevel.HOSTS,
          });
          inferredBreadcrumbs.push({
            name: selectedHost,
            level: ViewLevel.TASKS_LIST,
          });
          inferredBreadcrumbs.push({
            name: commandDetail(
              selectedTask.command_detail,
              selectedTask.request_inputs,
              selectedTask.ops_display_name
            ),
            level: ViewLevel.TASK_LOGS,
            active: true,
          });
        }
        break;
    }

    return inferredBreadcrumbs;
  }
  useEffect(() => {
    // Always fetch fresh data when modal opens, but show cached data immediately
    if (isOpen) {
      getClusterRequests();
    }
    if (isOpen && requestId) {
      getRequestDetails();
    }
  }, [pageSize, isOpen]);
  //@ts-ignore
  function isFinished(status: string) {
    return ["FAILED", "ABORTED", "COMPLETED"].includes(status);
  }
  //@ts-ignore
  function isRunning(status: string) {
    return isOperationRunning(status);
  }

  const getStatus = (requestStatus: string) => {
    if (requestStatus == "COMPLETED") {
      return ["SUCCESS", faCheck, "success", false];
    }
    if (requestStatus === "FAILED") {
      return ["FAILED", faExclamation, "danger", false];
    }
    if (requestStatus === "ABORTED") {
      return ["ABORTED", faMinus, "warning", false];
    }
    if (requestStatus === "TIMEDOUT") {
      return ["TIMEDOUT", faClock, "warning", false];
    }
    if (requestStatus === "IN_PROGRESS") {
      return ["IN_PROGRESS", faCogs, "info", true];
    }
    return ["IN_PROGRESS", faCog, "info opacity-75", true];
  };

  const openDetails = (rowData: any) => {
    const id = rowData?.Requests?.request_id || rowData?.Requests?.id;
    setSelectedLevel(ViewLevel.HOSTS);
    setSelectedRequestId(id);
    setSelectedRequest(get(rowData, "Requests", 0));
  };

  const requestDetailsColumns = [
    {
      header: "Operations",
      id: "name",
      width: "30%",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});
        return (
          <Stack
            className="p-1 cursor-pointer"
            onClick={() => openDetails(rowData)}
            direction="horizontal"
          >
            <FontAwesomeIcon
              icon={getStatus(request.request_status)[1] as any}
              className={`text-${getStatus(request.request_status)[2]}`}
            />
            <div className="text-info ms-2">
              {parseRequestContext(get(request, "request_context", ""))
                ?.requestContext || "No Request name"}
            </div>
          </Stack>
        );
      },
    },
    {
      header: "Status",
      id: "status",
      width: "20%",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});
        return (
          <Stack
            className="p-1 cursor-pointer w-100"
            onClick={() => openDetails(rowData)}
            direction="horizontal"
          >
            <ProgressBar
              className="rounded-1 w-100 request-progress-bar"
              now={request.progress_percent}
              variant={getStatus(request.request_status)[2] as any}
            />
            <div className="text-nowrap ms-2">{`${Math.floor(
              request.progress_percent
            )}%`}</div>
          </Stack>
        );
      },
    },
    {
      header: "User",
      accessorKey: "Requests.user_name",
      id: "username",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});
        return (
          <div
            className="p-1 cursor-pointer"
            onClick={() => openDetails(rowData)}
          >
            {request.user_name || "N/A"}
          </div>
        );
      },
    },
    {
      header: "Start Time",
      id: "Start Time",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});
        return ["PENDING"].includes(request.request_status) ? (
          <div onClick={() => openDetails(rowData)}>Not Started</div>
        ) : (
          <div
            className="p-1 cursor-pointer"
            onClick={() => openDetails(rowData)}
          >
            {dayjs(request.start_time).format("ddd MMM DD YYYY HH:mm")}
          </div>
        );
      },
    },
    {
      header: "Duration",
      id: "duration",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});

        // Use the new duration calculation utility that matches Ember.js logic
        const duration = calculateDurationSummary(
          request.start_time,
          request.end_time
        );

        return (
          <div
            className="p-1 cursor-pointer"
            onClick={() => openDetails(rowData)}
          >
            {duration}
          </div>
        );
      },
    },
    {
      header: "",
      id: "Actions",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const request = get(rowData, "Requests", {});
        return (
          <div
            className="p-1 cursor-pointer"
            onClick={() => openDetails(rowData)}
          >
            {["IN_PROGRESS", "PENDING"].includes(request.request_status) ? (
              <Tooltip message="Abort Operation">
                <FontAwesomeIcon
                  icon={faStop}
                  className="text-danger"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedRequest(request);
                    setShowModal(true);
                  }}
                ></FontAwesomeIcon>
              </Tooltip>
            ) : null}
          </div>
        );
      },
    },
  ];
  function getSelectedLevelView() {
    if (selectedLevel === ViewLevel.REQUESTS) {
      return (
        <>
          <ConfirmationModal
            modalTitle="Abort request sent"
            successCallback={() => {
              setShowAbortedModal(false);
            }}
            modalBody={`The abort request for ${
              parseRequestContext(selectedRequest.request_context)
                ?.requestContext
            } has been sent to the server. Note: Some tasks that are already running may have time to complete or fail before the abort request is applied.`}
            isOpen={showAbortedModal}
            onClose={() => {
              setShowAbortedModal(false);
            }}
          />
          <ConfirmationModal
            modalTitle="Confirmation"
            isOpen={showModal}
            onClose={() => {
              setShowModal(false);
            }}
            modalBody={`Are you sure you want to abort ${
              parseRequestContext(selectedRequest.request_context)
                ?.requestContext
            } operation`}
            successCallback={async () => {
              try {
                await ClusterApi.updateRequest(
                  clusterName || cName,
                  selectedRequest.request_id || selectedRequest.id,
                  {
                    Requests: {
                      abort_reason: "Aborted by user",
                      request_status: "ABORTED",
                    },
                  }
                );
                setShowAbortedModal(true);
                setShowModal(false);
              } catch (_err) {
                //Do Nothing
              }
            }}
          />
          {isLoadingRequests && isInitialLoad ? (
            <Center>
              <div className="d-flex align-items-center">
                <div className="spinner-border spinner-border-sm me-2" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                Loading background operations...
              </div>
            </Center>
          ) : !filteredClusterRequests.length ? (
            <Center>
              <div>No requests to show</div>
            </Center>
          ) : (
            <>
              <Table
                // showHeader={false}
                hover
                columns={requestDetailsColumns}
                data={filteredClusterRequests}
              />
              {!noMoreRequestsToShow ? (
                <div
                  className="text-info d-flex justify-content-center cursor-pointer mt-4"
                  onClick={() => {
                    if (!isLoadingRequests) {
                      setPageSize(pageSize + 10);
                    }
                  }}
                >
                  {isLoadingRequests && !isInitialLoad ? (
                    <div className="d-flex align-items-center">
                      <div className="spinner-border spinner-border-sm me-2" role="status">
                        <span className="visually-hidden">Loading...</span>
                      </div>
                      Loading more...
                    </div>
                  ) : (
                    "Show More..."
                  )}
                </div>
              ) : null}
            </>
          )}
        </>
      );
    }
    if (selectedLevel === ViewLevel.HOSTS) {
      return (
        <HostProgress
          latestMessage={latestMessage}
          requestId={selectedRequestId}
          setSelectedHost={setSelectedHost}
          setSelectedLevel={setSelectedLevel}
          setSelectedRequestId={setSelectedRequestId}
          clusterName={clusterName || cName}
        />
      );
    }
    if (selectedLevel === ViewLevel.TASKS_LIST) {
      return (
        <TasksList
          latestMessage={latestMessage}
          requestId={selectedRequestId}
          setSelectedHost={setSelectedHost}
          setSelectedLevel={setSelectedLevel}
          setSelectedRequestId={setSelectedRequestId}
          selectedHost={selectedHost}
          getStatus={getStatus}
          setSelectedTask={setSelectedTask}
          clusterName={clusterName || cName}
        />
      );
    }
    if (selectedLevel === ViewLevel.TASK_LOGS) {
      return (
        <TaskLogs
          requestId={selectedRequestId}
          task={selectedTask}
          clusterName={clusterName || cName}
        />
      );
    }
  }

  function getSelectedLevelViewHeader() {
    if (selectedLevel === ViewLevel.REQUESTS) {
      return (
        <Stack
          direction="horizontal"
          className="justify-content-between mt-3 w-100"
        >
          <h2>{runningOperationsCount} Background Operations Running</h2>
          <Filters
            items={filteredClusterRequests}
            allItems={backgroundOperations}
            statusKey={"Requests.request_status"}
            selectedFilter={selectedFilter}
            setSelectedFilter={setSelectedFilter}
          />
        </Stack>
      );
    }
  }

  if (userBgPreferences && !isExplicitClick) {
    onClose();
    return null;
  }

  return (
    <Modal show={isOpen} onHide={onClose} className="bg-operations-modal">
      <Modal.Header closeButton>
        <Modal.Title className="w-100">
          <Stack direction="vertical">
            <Stack direction="horizontal">
              {getBreadcrumbs()?.map((breadcrumb: any, index: number) => {
                return index === getBreadcrumbs().length - 1 ? (
                  <Modal.Title className="text-muted">
                    <h2>{breadcrumb?.name}</h2>
                  </Modal.Title>
                ) : (
                  <Stack direction="horizontal">
                    <Modal.Title
                      className="text-info cursor-pointer"
                      onClick={() => {
                        setSelectedLevel(breadcrumb.level);
                      }}
                    >
                      <h2>{breadcrumb.name}</h2>
                    </Modal.Title>
                    <ModalTitle className="text-muted mx-1">
                      <h2>/</h2>
                    </ModalTitle>
                  </Stack>
                );
              })}
            </Stack>
            {getSelectedLevelViewHeader()}
          </Stack>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "50vh", overflowY: "auto" }}>
        {getSelectedLevelView()}
      </Modal.Body>
      <Modal.Footer className="justify-content-between">
        <Form.Check // prettier-ignore
          type={"checkbox"}
          id={`hide-bg-operations`}
          onChange={(e) => {
            setUserBgPreferences(e.target.checked);
          }}
          checked={userBgPreferences}
          label={
            <div className="mt-1">
              Do not show this dialog again when starting a background operation
            </div>
          }
        />
        <Button
          variant="success"
          size="sm"
          onClick={onClose}
          className="rounded-1"
        >
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default BackgroundOperations;
