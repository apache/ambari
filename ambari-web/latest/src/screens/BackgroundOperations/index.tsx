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
import { calculateDurationSummary } from "../../Utils/dateUtils";
import {
  canAbortOperation,
  isUpgradeRequest,
  shouldShowBackgroundOperations,
  statusMatchesFilter,
} from "../../Utils/backgroundOperations";
import { useAuth } from "../../hooks/useAuth";
import toast from "react-hot-toast";

type PropTypes = {
  isOpen: boolean;
  onClose: any;
  rootLevel?: ViewLevel;
  requestId?: string | number;
  host?: string;
  clusterName?: string;
  isExplicitClick?: boolean;
};

function BackgroundOperations({
  isOpen,
  onClose,
  rootLevel = ViewLevel.REQUESTS,
  requestId = 0,
  host = "",
  clusterName,
  isExplicitClick = false,
}: PropTypes) {
  const initialLevel = requestId && rootLevel === ViewLevel.REQUESTS
    ? ViewLevel.HOSTS
    : rootLevel;
  const [selectedRequest, setSelectedRequest] = useState<any>({});
  const [selectedRequestId, setSelectedRequestId] = useState(requestId);
  const [filteredClusterRequests, setFilteredClusterRequests] = useState<any[]>([]);
  const [selectedFilter, setSelectedFilter] = useState<any>(null);
  const [noMoreRequestsToShow, setNoMoreRequestsToShow] = useState(false);
  const [isLoadingRequests, setIsLoadingRequests] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [isAborting, setIsAborting] = useState(false);
  const { hasAuthorization, isClusterUser } = useAuth();
  const canAbortRequests = hasAuthorization("SERVICE.START_STOP");
  const isBackgroundOperationsRestricted = isClusterUser();
  const {
    clusterName: cName,
    userBgPreferences,
    setUserBgPreferences,
    backgroundOperations,
    fetchBackgroundOperationsSnapshot,
    backgroundOperationsPageSize: pageSize,
    setBackgroundOperationsPageSize: setPageSize,
    updateBackgroundOperations,
    runningOperationsCount,
    parsedSocketMessages,
    isClusterInstalled,
  } = useContext(AppContext);
  const shouldHideAutomaticPopup = !shouldShowBackgroundOperations(
    userBgPreferences,
    isExplicitClick,
    isBackgroundOperationsRestricted,
  );
  const latestMessage = parsedSocketMessages.find((message) =>
    message.destination === "/events/requests"
      && Number(message.requestId) === Number(selectedRequestId)
  ) || {};
  
  // Check if this is truly the first load (no cached data)
  const isInitialLoad = backgroundOperations.length === 0;

  function getSetFilteredRequests() {
    if (selectedFilter && selectedFilter.value) {
      const filteredRequests = filter(
        backgroundOperations,
        (req: any) =>
          statusMatchesFilter(
            get(req, "Requests.request_status"),
            selectedFilter.value,
          )
      );
      setFilteredClusterRequests(filteredRequests as any);
    } else {
      setFilteredClusterRequests(backgroundOperations);
    }
  }
  
  async function getClusterRequests() {
    try {
      setIsLoadingRequests(true);
      setLoadError("");
      const canUseSharedSnapshot = (!clusterName || clusterName === cName)
        && Boolean(isClusterInstalled);
      const allClusterRequests = canUseSharedSnapshot
        ? await fetchBackgroundOperationsSnapshot(pageSize)
        : await ClusterApi.getRequests(clusterName || cName, pageSize);
      if (!allClusterRequests) return;
      setNoMoreRequestsToShow(Number(allClusterRequests.itemTotal) <= pageSize);
      if (!canUseSharedSnapshot) {
        const newRequests = allClusterRequests.items.filter((request: any) => {
          return !isUpgradeRequest(request);
        });
        updateBackgroundOperations(newRequests);
      }
    } catch (error) {
      console.error("Error fetching cluster requests:", error);
      setLoadError("Ambari could not load background operations.");
    } finally {
      setIsLoadingRequests(false);
    }
  }
  async function getRequestDetails() {
    if (requestId) {
      try {
        setLoadError("");
        const requestDetails = await ClusterApi.getRequestById(
          clusterName || cName,
          requestId
        );
        setSelectedRequest(requestDetails.Requests);
      } catch {
        setLoadError("Ambari could not load the selected background operation.");
      }
    }
  }
  const [selectedLevel, setSelectedLevel] = useState(initialLevel);
  const [showModal, setShowModal] = useState(false);
  const [showAbortedModal, setShowAbortedModal] = useState(false);
  const [selectedHost, setSelectedHost] = useState(host);
  const [selectedTask, setSelectedTask] = useState<any>({});
  useEffect(() => {
    getSetFilteredRequests();
  }, [backgroundOperations, selectedFilter]);
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
    if (isOpen && !isBackgroundOperationsRestricted && !shouldHideAutomaticPopup) {
      getClusterRequests();
    }
    if (isOpen && requestId && !isBackgroundOperationsRestricted && !shouldHideAutomaticPopup) {
      getRequestDetails();
    }
  }, [
    pageSize,
    isOpen,
    requestId,
    clusterName,
    cName,
    isBackgroundOperationsRestricted,
    shouldHideAutomaticPopup,
  ]);

  useEffect(() => {
    if (isOpen && (isBackgroundOperationsRestricted || shouldHideAutomaticPopup)) {
      onClose();
    }
  }, [isBackgroundOperationsRestricted, isOpen, onClose, shouldHideAutomaticPopup]);

  const getStatus = (requestStatus: string) => {
    if (requestStatus == "COMPLETED") {
      return ["SUCCESS", faCheck, "success", false];
    }
    if (requestStatus === "FAILED" || requestStatus === "SKIPPED_FAILED") {
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
            {canAbortOperation(
              request.request_status,
              canAbortRequests,
              isAborting && Number(selectedRequest.id) === Number(request.id),
            ) ? (
              <Tooltip message="Abort Operation">
                <Button
                  variant="link"
                  className="border-0 p-0 text-danger"
                  aria-label="Abort Operation"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedRequest(request);
                    setShowModal(true);
                  }}
                >
                  <FontAwesomeIcon icon={faStop} />
                </Button>
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
              if (isAborting) {
                return;
              }
              setIsAborting(true);
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
                await getClusterRequests();
              } catch (error: any) {
                const message = error?.response?.data?.message
                  || "Ambari could not abort the selected operation.";
                toast.error(message);
              } finally {
                setIsAborting(false);
              }
            }}
            isOkDisabled={isAborting}
          />
          {loadError ? (
            <Center>
              <Stack direction="vertical" className="align-items-center gap-2">
                <div className="text-danger">{loadError}</div>
                <Button size="sm" variant="outline-primary" onClick={() => void getClusterRequests()}>
                  Retry
                </Button>
              </Stack>
            </Center>
          ) : null}
          {!loadError && isLoadingRequests && isInitialLoad ? (
            <Center>
              <div className="d-flex align-items-center">
                <div className="spinner-border spinner-border-sm me-2" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                Loading background operations...
              </div>
            </Center>
          ) : !loadError && !filteredClusterRequests.length ? (
            <Center>
              <div>No requests to show</div>
            </Center>
          ) : !loadError ? (
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
          ) : null}
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

  if (isBackgroundOperationsRestricted || shouldHideAutomaticPopup) {
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
                  <Modal.Title key={`${breadcrumb.level}-${index}`} className="text-muted">
                    <h2>{breadcrumb?.name}</h2>
                  </Modal.Title>
                ) : (
                  <Stack key={`${breadcrumb.level}-${index}`} direction="horizontal">
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
            setUserBgPreferences(!e.target.checked);
          }}
          checked={!userBgPreferences}
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
