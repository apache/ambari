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

import type { JSX } from "react";
import { useContext, useEffect, useRef, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Collapse,
  Form,
  ProgressBar,
  Tab,
  Tabs,
} from "react-bootstrap";
import NestedCollapse from "../../../components/NestedCollapse";
import _, { get } from "lodash";
import Modal from "../../../components/Modal";
import { messages } from "../../messages";
import { useUpgrade } from "../../../hooks/useUpgrade";
import Spinner from "../../../components/Spinner";
import VersionsApi from "../../../api/versionsApi";
import { AppContext } from "../../../store/context";
import toast from "react-hot-toast";
import modalManager from "../../../store/ModalManager";
import { getUpgradeDisplayName, initialUpgradeMethods, translate, translateWithVariables } from "../../../Utils/Utility";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faGears, faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import { iconMapping } from "./ListVersion";
import Tooltip from "../../../components/Tooltip";
import ClusterApi from "../../../api/clusterApi";
import { waitForUpgradeStatus } from "./upgradeUtils";
import { persistedPayload } from "../../../Utils/persistedSettings";

type upgradeProps = {
  upgradeId: number;
  onlyView?:boolean;
  onClose?:() => void;
};

export default function Upgrade({ upgradeId, onlyView=false, onClose }: upgradeProps): JSX.Element {
  const {
    data,
    groups,
    currUpgradeItem,
    fetchTasks,
    fetchLogs,
    currentStack,
    updateCurrentStackVersion,
    handleCopy,
    handleOpenInNewTab,
    upgradeParameters,
    setUpgradeItemStatus,
    loadError,
    loading,
    detailLoadError,
    statusUpdateError,
    resumePolling,
    retryFetch,
    retryFailureDetails,
  } = useUpgrade(upgradeId, onlyView);

  const [showDetails, setShowDetails] = useState(false);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [activeKeys, setActiveKeys] = useState<string[]>([]);
  const [isManualDone, setManualDone] = useState(false);
  const [upgradeModal, setUpgradeModal] = useState(true);
  const [confirmDowngradeModal, setConfirmDowngradeModal] = useState(false);
  const [pauseUpgradeModal, setPauseUpgradeModal] = useState(false);
  const [failedHostsModal, setFailedhostsModal] = useState(false);
  const [upgradeOptionsModal, setUpgradeOptionsModal] = useState(false);
  const [localTasks, setLocalTasks] = useState<any[]>([]);
  const [slaveComponentFailures, setSlaveComponentFailures] = useState(upgradeParameters.slaveComponentFailures);
  const [serviceCheckFailures, setServiceCheckFailures] = useState(upgradeParameters.serviceCheckFailures);
  const [mutationInProgress, setMutationInProgress] = useState(false);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const downgradeWaitController = useRef<AbortController | null>(null);
  const { clusterName, setUpgradeState, setUpgradeId, isPatchUpgrade, upgradeVersionDisplayName, setUpgradeIsFinalizeItem, isNonWizardUser, wizardUser } = useContext(AppContext);
  const readOnly = onlyView || isNonWizardUser;
  const actionInProgress = upgradeParameters.requestInProgress || mutationInProgress;

  useEffect(() => () => downgradeWaitController.current?.abort(), []);

  useEffect(() => {
    setShowDetails(false);
    setLocalTasks([]);
  }, [currUpgradeItem?.UpgradeItem.group_id, currUpgradeItem?.UpgradeItem.stage_id]);

  useEffect(() => {
    if (!upgradeOptionsModal) {
      setSlaveComponentFailures(upgradeParameters.slaveComponentFailures);
      setServiceCheckFailures(upgradeParameters.serviceCheckFailures);
    }
  }, [
    upgradeOptionsModal,
    upgradeParameters.serviceCheckFailures,
    upgradeParameters.slaveComponentFailures,
  ]);

  function getShowDetailsButton() {
    const tasksToUse = localTasks.length > 0 ? localTasks : currUpgradeItem?.tasks;

    return (
      <Collapse in={showDetails}>
        <div className="mt-2 ms-2">
          {loadingLogs ? (
            <Spinner />
          ) : tasksToUse && tasksToUse.length > 0 ? (
            tasksToUse.map((task: any) => {
              const logs = task.logs;
              return (
                <div key={task.id} className="mb-3">
                  <div className="fw-semibold">
                    {task.command_detail || task.role || `Task ${task.id}`} on {task.host_name || "N/A"}
                  </div>
                  <Tabs defaultActiveKey="stdout" id={`logs-tabs-${task.id}`}>
                    <Tab eventKey="stdout" title="STDOUT">
                      <div className="d-flex justify-content-between mt-2">
                        <div>Output Log: {logs?.Tasks?.output_log || "N/A"}</div>
                        <div>
                          <Button variant="link" onClick={() => handleCopy(logs?.Tasks?.stdout ?? "")}>Copy</Button>
                          <Button variant="link" onClick={() => handleOpenInNewTab(logs?.Tasks?.stdout ?? "")}>Open</Button>
                        </div>
                      </div>
                      <Card className="no-border"><Card.Body><pre>{logs?.Tasks?.stdout || "No stdout logs available"}</pre></Card.Body></Card>
                    </Tab>
                    <Tab eventKey="stderr" title="STDERR">
                      <div className="d-flex justify-content-between mt-2">
                        <div>Error Log: {logs?.Tasks?.error_log || "N/A"}</div>
                        <div>
                          <Button variant="link" onClick={() => handleCopy(logs?.Tasks?.stderr ?? "")}>Copy</Button>
                          <Button variant="link" onClick={() => handleOpenInNewTab(logs?.Tasks?.stderr ?? "")}>Open</Button>
                        </div>
                      </div>
                      <Card className="no-border"><Card.Body><pre>{logs?.Tasks?.stderr || "No stderr logs available"}</pre></Card.Body></Card>
                    </Tab>
                  </Tabs>
                </div>
              );
            })
          ) : (
            <div>No tasks available for this upgrade item</div>
          )}
        </div>
      </Collapse>
    );
  }

  function getUpgradeModalTitle() {
    const titleText = `${getUpgradeDisplayName(data?.Upgrade.upgrade_type || "rolling")} ${
      isPatchUpgrade ? "Patch" : ""
    } ${data?.Upgrade.direction} ${
      data?.Upgrade.direction == "DOWNGRADE" ? "from" : "to"
    } ${
      upgradeVersionDisplayName != ""
        ? upgradeVersionDisplayName
        : data?.Upgrade.associated_version
    }`;
    
    return (
      <div className="d-flex justify-content-between align-items-center w-100">
        <span>{titleText}</span>
        {!readOnly && (
          <Button
            variant="link"
            size="sm"
            disabled={upgradeParameters.isDowngrade}
            className="p-0 ms-2 custom-link"
            onClick={() => setUpgradeOptionsModal(true)}
            title="Upgrade Options"
          >
            <FontAwesomeIcon icon={faGears} className="custom-link me-1" />
            Upgrade Options
          </Button>
        )}
      </div>
    );
  }

  async function startDowngrade() {
    setUpgradeIsFinalizeItem(false);
    const payload = {
      "Upgrade": { 
        "upgrade_type": data?.Upgrade.upgrade_type,
        "direction": "DOWNGRADE",
      }
    }

    try {
      const response = await VersionsApi.getUpgradeId(payload, clusterName);
      const downgradeId = response?.resources[0]?.Upgrade?.request_id;
      if (!downgradeId) {
        throw new Error("The server did not return a downgrade request ID");
      }
      setUpgradeId(downgradeId);
      setUpgradeState("PENDING");
      setUpgradeModal(false);
      await ClusterApi.postPersistData(
        persistedPayload({ upgradeIsFinalizeItem: false }),
      ).catch(() => {
        toast.error("The downgrade started, but its browser state could not be persisted");
      });
      
      modalManager.show(<Upgrade upgradeId={downgradeId} />);
      window.location.reload();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setMutationError(message);
      modalManager.show({
        modalTitle: "Upgrade could not be started",
        modalBody: (
          <div>
            {message}
          </div>
        ),
        onClose: () => {
          modalManager.hide();
        },
        successCallback: () => {
          modalManager.hide();
        },
        options: {
          cancelableViaIcon: true,
          cancelableViaBtn: false,
        },
      });
    }
  }

  function confirmDowngrade() {
    if (!currentStack) {
      updateCurrentStackVersion();
    }

    if (mutationInProgress || readOnly) return;
    setMutationInProgress(true);
    setMutationError(null);
    downgradeWaitController.current?.abort();
    downgradeWaitController.current = new AbortController();
    void (async () => {
      try {
        await VersionsApi.abortUpgrade(clusterName, upgradeId);
        await waitForUpgradeStatus(
          async () => (await VersionsApi.getUpgradeOperations(upgradeId, clusterName))?.Upgrade?.request_status,
          "ABORTED",
          { signal: downgradeWaitController.current?.signal },
        );
        await startDowngrade();
      } catch (error: any) {
        if (error?.name !== "AbortError") {
          const message = error?.response?.data?.message || error?.message || "The upgrade could not be aborted for downgrade";
          setMutationError(message);
          toast.error(message);
        }
      } finally {
        setMutationInProgress(false);
      }
    })();
  }

  async function pauseUpgrade() {
    if (mutationInProgress || readOnly) return;
    setMutationInProgress(true);
    setMutationError(null);
    try {
      await VersionsApi.suspendUpgrade(clusterName, upgradeId);
      setUpgradeState("ABORTED");
      setPauseUpgradeModal(false);
      setUpgradeModal(false);
      if (onClose) onClose();
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || "Error suspending upgrade";
      setMutationError(message);
      toast.error(message);
    } finally {
      setMutationInProgress(false);
    }
  }

  async function resumeUpgrade() {
    if (mutationInProgress || readOnly) return;
    setMutationInProgress(true);
    setMutationError(null);
    try {
      await VersionsApi.retryUpgrade(clusterName, upgradeId);
      setUpgradeState("PENDING");
      resumePolling();
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || "Error resuming upgrade";
      setMutationError(message);
      toast.error(message);
    } finally {
      setMutationInProgress(false);
    }
  }

  function getFailedHostsMessage(slaveComponentStructuredInfo: any) {
    const count = get(slaveComponentStructuredInfo, "hosts.length", 0);
    return translateWithVariables("admin.stackUpgrade.failedHosts.showHosts", {"0": count});
  }

  function getFailedHostsModalBody() {
    const hostInfo = upgradeParameters.slaveComponentStructuredInfo;
    const hosts = hostInfo?.hosts || [];
    const hostDetails = hostInfo?.host_detail || {};
    
    return (
      <div className="failed-hosts-modal-content">
        <div className="mb-3">
          <div className="text-muted">Upgrade failed on {hosts.length} hosts</div>
        </div>
        
        {hosts.map((hostname: string, index: number) => {
          const hostComponents = hostDetails[hostname] || [];
          return (
            <div key={index} className="host-item mb-3">
              <div className="d-flex align-items-center mb-2">
                <span className="me-2">▶</span>
                <span className="text-primary fw-bold">{hostname}</span>
                <span className="badge bg-info text-white ms-2 rounded-pill">
                  {hostComponents.length}
                </span>
              </div>
              
              <div className="ms-3">
                <div className="row">
                  {hostComponents.map((component: any, componentIndex: number) => (
                    <div key={componentIndex} className="col-6 mb-2">
                      <div className="p-2 border rounded">
                        <div className="fw-bold">{component.service}</div>
                        <div className="text-muted small">{component.component}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  function parseManualItemText(text: any) {
    try {
      const parsed = JSON.parse(text);
      return Array.isArray(parsed) ? parsed : [{ message: text }];
    } catch (error) {
      return [{ message: text }];
    }
  };

  async function handleShowDetailsClick(item: any) {
    const isCurrentlyOpen = showDetails;
    setShowDetails(!isCurrentlyOpen);

    if (!isCurrentlyOpen) {
      if (!currUpgradeItem?.tasks?.length || currUpgradeItem.tasks.some((task) => !task.logs)) {
        setLoadingLogs(true);
        try {
          const groupId = item?.UpgradeItem.group_id ?? 0;
          const stageId = item?.UpgradeItem.stage_id ?? 0;
          const tasks = await fetchTasks(groupId, stageId);
          const tasksWithLogs = await Promise.all(tasks.map(async (task: any) => ({
            ...task,
            logs: await fetchLogs(groupId, stageId, task.id),
          })));
          setLocalTasks(tasksWithLogs);
        } catch (error) {
          toast.error(error instanceof Error ? error.message : "Task logs could not be loaded");
        } finally {
          setLoadingLogs(false);
        }
      }
    }
  }

  function getUpgradeModalBody() {
    if (!data) {
      return (
        <>
          {isNonWizardUser && (
            <Alert variant="info">
              Upgrade started by {wizardUser || "another user"}. This view is read-only.
            </Alert>
          )}
          {loadError ? (
            <Alert variant="danger" className="d-flex justify-content-between align-items-center">
              <span>{loadError}</span>
              <Button size="sm" variant="outline-danger" onClick={() => void retryFetch().catch(() => undefined)}>
                Retry
              </Button>
            </Alert>
          ) : loading ? <Spinner /> : <Alert variant="warning">No upgrade data was returned by the server.</Alert>}
        </>
      );
    }
    return (
      <>
        {isNonWizardUser && (
          <Alert variant="info">
            Upgrade started by {wizardUser || "another user"}. This view is read-only.
          </Alert>
        )}
        {loadError && (
          <Alert variant="danger" className="d-flex justify-content-between align-items-center">
            <span>{loadError}</span>
            <Button size="sm" variant="outline-danger" onClick={() => void retryFetch().catch(() => undefined)}>
              Retry
            </Button>
          </Alert>
        )}
        {mutationError && <Alert variant="danger">{mutationError}</Alert>}
        {statusUpdateError && <Alert variant="danger">{statusUpdateError}</Alert>}
        {detailLoadError && (
          <Alert variant="danger" className="d-flex justify-content-between align-items-center">
            <span>{detailLoadError}</span>
            <Button size="sm" variant="outline-danger" onClick={retryFailureDetails}>
              Retry
            </Button>
          </Alert>
        )}
        <div className="d-flex justify-content-between mb-4">
          <div>{get(messages, `${upgradeParameters.upgradeStatusLabel}`, "hello")}</div>
          <div className="d-flex">
            <ProgressBar
              now={data?.Upgrade.progress_percent}
              className="progress-bar-width-upgrade me-10"
            />
            <div className="ms-2">{Math.round(data?.Upgrade.progress_percent || 0)}% </div>
          </div>
          {upgradeParameters.showPauseButton && !readOnly && (
            <Button
              size="sm"
              variant="light"
              className="text-uppercase"
              onClick={() => setPauseUpgradeModal(true)}
              disabled={mutationInProgress}
            >
              {upgradeParameters.isDowngrade ? 
                get(messages, "admin.stackUpgrade.pauseDowngrade") : get(messages, "admin.stackUpgrade.pauseUpgrade")
              }
            </Button>
          )}
        </div>
        <Card className="mb-4">
          <Card.Body>
            <div className="d-flex-column">
            {upgradeParameters.runningItem && !readOnly && (
              <>
                <div className="d-flex justify-content-between align-items-center">
                  <div>{get(messages, "admin.stackUpgrade.dialog.inProgress")} {" "} {currUpgradeItem?.UpgradeItem.context}</div>
                  <Button
                    variant="link"
                    onClick={() => handleShowDetailsClick(currUpgradeItem)}
                  >
                    {showDetails ? "Hide Details" : "Show Details"}
                  </Button>
                </div>
                {getShowDetailsButton()}
              </>
            )}

            {upgradeParameters.failedItem && !readOnly && !upgradeParameters.upgradeSuspended && !upgradeParameters.runningItem &&
            !upgradeParameters.isSlaveComponentFailuresItem ? (
              <>
                <div className="d-flex justify-content-between">
                  <div>
                    {get(messages, "admin.stackUpgrade.dialog.failed")}
                    {upgradeParameters.failedItem.UpgradeItem.text}
                  </div>
                  <Button
                    variant="link"
                    onClick={() => handleShowDetailsClick(upgradeParameters.failedItem)}
                  >
                    {showDetails ? "Hide Details" : "Show Details"}
                  </Button>
                </div>
                {getShowDetailsButton()}
                <div className="d-flex justify-content-between">
                  <div></div>
                  <div>
                    {upgradeParameters.isDowngradeAvailable && (
                      <Button
                        variant="danger"
                        className="text-uppercase me-2"
                        disabled={actionInProgress}
                        onClick={() => setConfirmDowngradeModal(true)}
                      >
                        Downgrade
                      </Button>
                    )} 
                    {upgradeParameters.canSkipFailedItem && (
                      <Button
                        variant="warning"
                        className="text-uppercase me-2"
                        disabled={actionInProgress}
                        onClick={() => {
                            if(currUpgradeItem) {
                              setUpgradeItemStatus(
                                currUpgradeItem,
                                currUpgradeItem?.UpgradeItem.status.slice(8)
                              )
                            }
                          }
                        }
                      >
                        {get(messages, "admin.stackUpgrade.dialog.continue")}
                      </Button>
                    )} 
                    <Button
                      variant="info"
                      className="text-uppercase mx-1"
                      disabled={actionInProgress}
                      onClick={() => {
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "PENDING")
                      }
                      }
                    >
                      {get(messages, "common.retry")}
                    </Button>
                  </div>
                </div>
              </>
            ) : null}

            {upgradeParameters.isSlaveComponentFailuresItem && !readOnly && !upgradeParameters.upgradeSuspended &&
              <>
                <div className="manual-steps-section">
                  <div className="manual-steps-title">{get(messages, "admin.stackUpgrade.dialog.manual")}</div>
                  <div className="upgrade-failed-message">
                    {get(messages, "admin.stackUpgrade.failedHosts.message")}
                  </div>
                  {upgradeParameters.areSlaveComponentFailuresHostsLoaded ? (
                    <div className="failed-hosts-info">
                      <span 
                        className="custom-link" 
                        onClick={() => setFailedhostsModal(true)}
                      >
                        {getFailedHostsMessage(upgradeParameters.slaveComponentStructuredInfo)}
                      </span>
                    </div>
                  ) : <Spinner />}
                  <div className="upgrade-options mt-2">
                    <div className="options-title">{get(messages, "admin.stackUpgrade.failedHosts.options")}</div>
                    <ul className="options-list mt-2">
                      <li className="mt-1">{translate("admin.stackUpgrade.failedHosts.options.first")}</li>
                      <li className="mt-1">{translate("admin.stackUpgrade.failedHosts.options.second")}</li>
                    </ul>
                  </div>
                  {!upgradeParameters.isHoldingState && (
                    <div className="manual-checkbox-container">
                      <Form.Check
                        type="checkbox"
                        checked={isManualDone}
                        onChange={() => setManualDone(!isManualDone)}
                        label={get(messages, "admin.stackUpgrade.dialog.manualDone")}
                        className="manual-done-checkbox"
                      />
                    </div>
                  )}
                </div>
                <div className="d-flex justify-content-between mt-3">
                  <div></div>
                  <div>
                    {upgradeParameters.isDowngradeAvailable && (
                      <Button
                        variant="danger"
                        size="sm"
                        className="me-2"
                        onClick={() => setConfirmDowngradeModal(true)}
                        disabled={actionInProgress}
                      >
                        {get(messages, "common.downgrade")}
                      </Button>
                    )}
                    <Button
                      variant="success"
                      size="sm"
                      className="me-2"
                      onClick={() => {
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "PENDING")
                      }}
                      disabled={actionInProgress}
                    >
                      {get(messages, "common.retry")}
                    </Button>
                    <Button
                      variant="success"
                      size="sm"
                      onClick={() => {
                        setManualDone(false);
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "COMPLETED")
                      }}
                      disabled={actionInProgress || !isManualDone}
                    >
                      {get(messages, "common.proceed")}
                    </Button>
                  </div>
                  
                </div>
              </>
            }

            {upgradeParameters.isServiceCheckFailuresItem && !readOnly && !upgradeParameters.upgradeSuspended &&
              <>
                <div>
                  <div>{get(messages, "admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.areServiceCheckFailuresServicenamesLoaded ? (
                    upgradeParameters.serviceCheckFailuresServicenames.length ? (
                      <div>
                        <div>{get(messages, "admin.stackUpgrade.dialog.manual.serviceCheckFailures.title")}</div>
                        <div>{get(messages, "admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg1")}</div>
                        <ul className="failed-info-list">
                          {upgradeParameters.serviceCheckFailuresServicenames.map((serviceName: any, index: any) => (
                            <li key={index}>{serviceName}</li>
                          ))}
                        </ul>
                        <div>{get(messages, "admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg2")}</div>
                      </div>
                    ) : upgradeParameters.slaveComponentStructuredInfo.hosts.length ? (
                      <div>
                        <div>{get(messages, "admin.stackUpgrade.dialog.manual.slaveComponentFailures.title")}</div>
                        <div>
                          <div>{get(messages, "admin.stackUpgrade.failedHosts.message")}</div>
                          <Badge onClick={() => setFailedhostsModal(true)}>
                            {getFailedHostsMessage(upgradeParameters.slaveComponentStructuredInfo)}
                          </Badge>
                        </div>
                        <ul>
                          <li>{translate("admin.stackUpgrade.failedHosts.options.first")}</li>
                          <li>{translate("admin.stackUpgrade.failedHosts.options.third")}</li>
                          {upgradeParameters.isDowngradeAvailable && (
                            <li>{translate("admin.stackUpgrade.failedHosts.options.second")}</li>
                          )}
                        </ul>
                      </div>
                    ) : (
                      <Spinner />
                    )
                  ) : (
                    <Spinner />
                  )}

                  {!upgradeParameters.isHoldingState && (
                    <Form className="mx-2">
                      <Form.Group controlId="manualCheck">
                        <Form.Check
                          type="checkbox"
                          checked={isManualDone}
                          onChange={() => setManualDone(!isManualDone)}
                          label={get(
                            messages,
                            "admin.stackUpgrade.dialog.manualDone"
                          )}
                        />
                      </Form.Group>
                    </Form>
                  )}
                </div>
                <div className="justify-content-between">
                    <div></div>
                    <div>
                      {upgradeParameters.isDowngradeAvailable ? (
                        <Button
                          variant="danger"
                          className="me-2"
                          onClick={() => setConfirmDowngradeModal(true)}
                          disabled={actionInProgress}
                        >
                          {get(messages, "common.downgrade")}
                        </Button>
                      ) : null}
                      <Button
                        variant="primary"
                        className="me-2"
                        disabled={actionInProgress}
                        onClick={() => {
                          setManualDone(false);
                          if(currUpgradeItem)
                            setUpgradeItemStatus(currUpgradeItem, "PENDING")
                        }
                        }
                      >
                        {get(messages, "common.retry")}
                      </Button>
                      <Button
                        variant="success"
                        disabled={actionInProgress || (!upgradeParameters.isHoldingState && !isManualDone)}
                        onClick={() => {
                          setManualDone(false);
                          if (currUpgradeItem) {
                            setUpgradeItemStatus(currUpgradeItem, "COMPLETED");
                          }
                        }}
                      >
                        {get(messages, "common.continue")}
                      </Button>
                    </div>
                  </div>
              </>
            }

            {upgradeParameters.isFinalizeItem && !readOnly && !upgradeParameters.upgradeSuspended ? (
              <>
                <div className="mb-2">
                  <div className="text-dark mb-2">{get(messages, "admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.isDowngrade ? (
                    <p className="manual-steps-content">
                      {translate(
                        "admin.stackUpgrade.finalize.message.downgrade"
                      )}
                    </p>
                  ) : isPatchUpgrade ? (
                    <p className="manual-steps-content">
                      {translateWithVariables(
                        "admin.stackUpgrade.finalize.message.revertible", {
                          "0": "PATCH",
                          "1": upgradeParameters.upgradeAssociatedversion
                        }
                      )}
                    </p>
                  ) : (
                    <p className="manual-steps-content">
                      {translate(
                        "admin.stackUpgrade.finalize.message.upgrade"
                      )}
                    </p>
                  )}

                  <div className="text-dark">
                    {translate(
                      "admin.stackUpgrade.finalize.message.autoStart"
                    )}
                  </div>
                  {upgradeParameters.skippedServiceChecks.length > 0 && (
                    <Alert variant="warning" className="mt-2">
                      <div>Failed service checks:</div>
                      <ul className="mb-0">
                        {upgradeParameters.skippedServiceChecks.map((serviceName) => (
                          <li key={serviceName}>{serviceName}</li>
                        ))}
                      </ul>
                    </Alert>
                  )}
                  <Form className="mx-2 mt-2">
                    <Form.Group controlId="manualCheck">
                      <Form.Check
                        type="checkbox"
                        checked={isManualDone}
                        onChange={() => setManualDone(!isManualDone)}
                        label={translate(
                          "admin.stackUpgrade.dialog.manualDone"
                        )}
                      />
                    </Form.Group>
                  </Form>
                  <div className="d-flex justify-content-between">
                    <div></div>
                    <div>
                      {upgradeParameters.isDowngradeAvailable ? (
                        <Button
                          variant="danger"
                          onClick={() => setConfirmDowngradeModal(true)}
                          disabled={actionInProgress}
                        >
                          {translate("common.downgrade")}
                        </Button>
                      ) : null}
                      <Button
                        variant="light"
                        className="ms-2"
                        onClick={() => setPauseUpgradeModal(true)}
                        disabled={actionInProgress}
                      >
                        {translate("admin.stackUpgrade.finalize.later")}
                      </Button>
                      <Button
                        variant="primary"
                        className="ms-2"
                        disabled={actionInProgress || !isManualDone}
                        onClick={() => {
                          setManualDone(false);
                          if(currUpgradeItem)
                            setUpgradeItemStatus(currUpgradeItem, "COMPLETED")
                        }
                        }
                      >
                        {translate("common.finalize")}
                      </Button>
                    </div>
                  </div>
                </div>
              </>
            ) : null}

            {upgradeParameters.plainManualItem && !upgradeParameters.isFinalizeItem && !readOnly && !upgradeParameters.upgradeSuspended ? (
              <div>
                <div className="mb-3">
                  <div className="me-2 mt-1 text-dark">{get(messages, "admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.manualItem?.UpgradeItem.text && upgradeParameters.manualItem.UpgradeItem.text.length > 0 ? (
                    parseManualItemText(upgradeParameters.manualItem.UpgradeItem.text).map((item, index) => {
                      return (
                        <p key={index} className="mt-2 manual-steps-content">
                          {item.message}
                        </p>
                      );
                    })
                  ): 
                    <div className="manual-steps-content">{translate("admin.stackUpgrade.dialog.skipped.failure")}</div>
                  }
                  <Form className="mx-2">
                    <Form.Group controlId="manualCheck">
                      <Form.Check
                        type="checkbox"
                        checked={isManualDone}
                        onChange={() => setManualDone(!isManualDone)}
                        label={get(
                          messages,
                          "admin.stackUpgrade.dialog.manualDone"
                        )}
                      />
                    </Form.Group>
                  </Form>
                </div>
                <div className="mt-1 d-flex justify-content-between">
                  <div></div>
                  <div>
                    {upgradeParameters.isDowngradeAvailable ? (
                      <Button
                        variant="danger"
                        onClick={() => setConfirmDowngradeModal(true)}
                        disabled={actionInProgress}
                      >
                        {get(messages, "common.downgrade")}
                      </Button>
                    ) : null}
                    <Button
                      variant="primary"
                      className="mx-2"
                      disabled={actionInProgress || !isManualDone}
                      onClick={() => {
                        setManualDone(false);
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "COMPLETED")
                      }
                      }
                    >
                      {translate('common.proceed')}
                    </Button>
                  </div>
                </div>
              </div>
            ) : null}

            {upgradeParameters.upgradeSuspended && !readOnly && (
              <>
                <div className="d-flex justify-content-between">
                <div className="mt-3">
                  {upgradeParameters.isDowngrade ? (
                    <label>{get(messages, "admin.stackUpgrade.dialog.suspended.downgrade")}</label>
                  ) : (
                    <label>{get(messages, "admin.stackUpgrade.dialog.suspended")}</label>
                  )}
                </div>

                {upgradeParameters.isDowngrade ? (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                    disabled={actionInProgress}
                  >{get(messages, "admin.stackUpgrade.dialog.resume.downgrade")}</Button>
                ) : (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                    disabled={actionInProgress}
                  >{get(messages, "admin.stackUpgrade.dialog.resume")}</Button>
                )}
              </div>
            </>
            )}
            {!readOnly
              && !upgradeParameters.upgradeSuspended
              && ["ABORTED", "FAILED", "TIMEDOUT"].includes(upgradeParameters.upgradeStatus)
              && !upgradeParameters.runningItem
              && (
                <div className="d-flex justify-content-between align-items-center">
                  <Alert variant="warning" className="mb-0 me-3 flex-grow-1">
                    This upgrade is not running. Retry it to return the request to PENDING.
                  </Alert>
                  <Button variant="primary" onClick={() => void resumeUpgrade()} disabled={actionInProgress}>
                    Retry Upgrade
                  </Button>
                </div>
              )}
            </div>
          </Card.Body>
        </Card>

        <NestedCollapse
          groups={[...groups]
            .filter(group => 
              // Show all groups when upgrade is paused
              upgradeParameters.upgradeSuspended || 
              // Otherwise, hide aborted groups during upgrade
              !upgradeParameters.upgradeInProgress || 
              group.UpgradeGroup.display_status !== "ABORTED"
            )
            .reverse()}
          activeKeys={activeKeys}
          setActiveKeys={setActiveKeys}
          fetchTasks={fetchTasks}
          fetchLogs={fetchLogs}
          handleCopy={handleCopy}
          handleOpenInNewTab={handleOpenInNewTab}
          onlyView={readOnly}
        />
      </>
    );
  }

  function getPauseUpgradeModalBody() {
    const replacingText = upgradeParameters.isDowngrade ? 'downgrade' : 'upgrade';
    return (
      <div className="mt-1">
        {translateWithVariables("admin.stackUpgrade.pauseUpgrade.warning", { "0": replacingText })}
      </div>
    );
  }
  
  function getPauseUpgradeModalOkButtonText() {
    return upgradeParameters.isDowngrade 
      ? translate("admin.stackUpgrade.pauseDowngrade") 
      : translate("admin.stackUpgrade.pauseUpgrade");
  }

  // Function to update upgrade options during active upgrade
  async function updateUpgradeOptions() {
    if (!upgradeId || readOnly || mutationInProgress) {
      toast.error("No active upgrade found");
      return;
    }

    const payload = {
      Upgrade: {
        skip_failures: slaveComponentFailures.toString(),
        skip_service_check_failures: serviceCheckFailures.toString(),
      },
    };

    setMutationInProgress(true);
    setMutationError(null);
    try {
      await VersionsApi.updateUpgrade(upgradeId, payload, clusterName);
      toast.success("Upgrade options updated successfully");
      setUpgradeOptionsModal(false);
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || "Failed to update upgrade options";
      setMutationError(message);
      toast.error(message);
    } finally {
      setMutationInProgress(false);
    }
  }

  // Function to generate upgrade options modal content (for active upgrades)
  function getUpgradeOptionsModalContent() {
    
    return (
      <div className="upgrade-modal">
        <div>Choose the upgrade method:</div>
        
        <div className="upgrade-options-container">
          {initialUpgradeMethods
            .filter(
              (method) =>
                method.allowed
            )
            .map((method) => (
              <div
                key={method.type}
                className={`upgrade-method disabled ${
                  upgradeParameters.upgradeMethod === method.type ? "selected" : ""
                }`}
              >
                <FontAwesomeIcon
                  className="upgrade-method-icon"
                  icon={iconMapping[method.icon]}
                />
                <div className="upgrade-method-title">{method.displayName}</div>
                <div className="upgrade-method-description">
                  {method.description}
                </div>
              </div>
            ))}
        </div>

        <div className="upgrade-failure-tolerance mt-4">
          <div>Select optional upgrade failure tolerance:
            <Tooltip message={translate("admin.stackVersions.version.upgrade.upgradeOptions.tolerance.tooltip")}>
              <FontAwesomeIcon
                className="ms-1 custom-link cursor-pointer"
                icon={faQuestionCircle}
              />
            </Tooltip>
          </div>
          <Form className="pt-2">
            <Form.Group controlId="serviceCheckFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Service Check failures"
                checked={serviceCheckFailures}
                disabled={actionInProgress}
                onChange={(e) => setServiceCheckFailures(e.target.checked)}
              />
            </Form.Group>
            <Form.Group controlId="slaveComponentFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Slave Component failures"
                checked={slaveComponentFailures}
                disabled={actionInProgress}
                onChange={(e) => setSlaveComponentFailures(e.target.checked)}
              />
            </Form.Group>
          </Form>
        </div>

        <Alert variant="warning" className="mt-3">
          Cluster alerts will still be visible and recorded in Ambari but
          notifications (such as Email and SNMP) will be suppressed during the
          upgrade.
        </Alert>
      </div>
    );
  }
  
  return (
    <>
      {upgradeModal && (
        <Modal
          isOpen={upgradeModal}
          onClose={() => {
            setUpgradeModal(false);
            if (onClose) onClose();
          }}
          modalTitle={getUpgradeModalTitle()}
          modalBody={getUpgradeModalBody()}
          options={{
            okButtonText: "DISMISS",
            cancelableViaIcon: true,
            cancelableViaBtn: false,
            modalSize: "modal-lg",
            okButtonDisabled: actionInProgress,
          }}
          successCallback={() => {
            setUpgradeModal(false);
            if (onClose) onClose();
          }}
        />
      )}

      {pauseUpgradeModal ? (
        <Modal
          isOpen={pauseUpgradeModal}
          onClose={() => setPauseUpgradeModal(false)}
          modalTitle="Warning"
          modalBody={getPauseUpgradeModalBody()}
          options={{
            okButtonText: getPauseUpgradeModalOkButtonText(),
            cancelableViaIcon: true,
            cancelableViaBtn: true,
            modalSize: "modal-md",
            okButtonDisabled: actionInProgress,
          }}
          successCallback={() => {
            pauseUpgrade();
          }}
        />
      ) : null}

      {confirmDowngradeModal ? (
        <Modal
          isOpen={confirmDowngradeModal}
          onClose={() => setConfirmDowngradeModal(false)}
          modalTitle="Warning"
          modalBody="Are you sure you want to downgrade?"
          options={{
            okButtonText: "PROCEED WITH DOWNGRADE",
            cancelableViaIcon: true,
            cancelableViaBtn: true,
            modalSize: "modal-sm",
            okButtonDisabled: actionInProgress,
          }}
          successCallback={() => {
            setConfirmDowngradeModal(false);
            confirmDowngrade()
          }}
        />
      ) : null}

      { failedHostsModal && (
        <Modal
        isOpen={failedHostsModal}
        onClose={() => setFailedhostsModal(false)}
        modalTitle={get(messages, "admin.stackUpgrade.failedHosts.header")}
        modalBody={getFailedHostsModalBody()}
        options={{
          okButtonText: get(messages, "common.close"),
          modalSize: "modal-sm",
          cancelableViaIcon: true,
        }}
        successCallback={() => setFailedhostsModal(false)}
        />
      )}

      {upgradeOptionsModal && (
        <Modal
          isOpen={upgradeOptionsModal}
          onClose={() => setUpgradeOptionsModal(false)}
          modalTitle="Upgrade Options"
          modalBody={getUpgradeOptionsModalContent()}
          options={{
            modalSize: "modal-sm",
            cancelableViaBtn: true,
            cancelableViaIcon: true,
            okButtonText: "OK",
            cancelButtonText: "CANCEL",
            okButtonDisabled: actionInProgress,
          }}
          successCallback={() => updateUpgradeOptions()}
        />
      )}
    </>
  );
}
