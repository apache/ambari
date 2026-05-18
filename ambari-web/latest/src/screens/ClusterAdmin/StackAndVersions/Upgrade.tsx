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

import { JSX, useContext, useState } from "react";
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
import { useUpgrade } from "../../../hooks/useUpgrade";
import Spinner from "../../../components/Spinner";
import VersionsApi from "../../../api/versionsApi";
import { AppContext } from "../../../store/context";
import toast from "react-hot-toast";
import modalManager from "../../../store/ModalManager";
import { getUpgradeDisplayName, initialUpgradeMethods, translate, translateWithVariables } from "../../../Utils/Utility";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faGears, faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import Tooltip from "../../../components/Tooltip";
import ClusterApi from "../../../api/clusterApi";
import { iconMapping } from "./ListVersion";

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
  const [localLogs, setLocalLogs] = useState<any>(null);
  const [localTasks, setLocalTasks] = useState<any[]>([]);
  const [slaveComponentFailures, setSlaveComponentFailures] = useState(upgradeParameters.slaveComponentFailures);
  const [serviceCheckFailures, setServiceCheckFailures] = useState(upgradeParameters.serviceCheckFailures);
  const { clusterName, setUpgradeState, setUpgradeId, isPatchUpgrade, upgradeVersionDisplayName, setUpgradeIsFinalizeItem } = useContext(AppContext);

  function getShowDetailsButton() {
    
    const tasksToUse = localTasks.length > 0 ? localTasks : currUpgradeItem?.tasks;
    const logsToUse = localLogs || (currUpgradeItem?.tasks?.[0]?.logs);
    
    return (
      <Collapse in={showDetails}>
        <div className="mt-2 ms-2">
          {loadingLogs ? (
            <Spinner />
          ) : tasksToUse && tasksToUse.length > 0 ? (
            <Tabs
              defaultActiveKey="stdout"
              id={`logs-tabs-${tasksToUse[0].id}`}
            >
              <Tab eventKey="stdout" title="STDOUT">
                <div className="mt-3">
                  Host: {logsToUse?.Tasks?.host_name || 'N/A'}
                </div>
                <div className="d-flex justify-content-between">
                  <div className="mt-2">
                    Output Log:{" "}
                    {logsToUse?.Tasks?.output_log || 'N/A'}
                  </div>
                  <div>
                    <Button
                      variant="link"
                      onClick={() =>
                        handleCopy(
                          logsToUse?.Tasks?.stdout ?? ""
                        )
                      }
                    >
                      Copy
                    </Button>
                    <Button
                      variant="link"
                      onClick={() =>
                        handleOpenInNewTab(
                          logsToUse?.Tasks?.stdout ?? ""
                        )
                      }
                    >
                      Open
                    </Button>
                  </div>
                </div>
                <Card className="no-border">
                  <Card.Body>
                    <pre>{logsToUse?.Tasks?.stdout || 'No stdout logs available'}</pre>
                  </Card.Body>
                </Card>
              </Tab>
              <Tab eventKey="stderr" title="STDERR">
                <div className="mt-3">
                  Host: {logsToUse?.Tasks?.host_name || 'N/A'}
                </div>
                <div className="d-flex justify-content-between">
                  <div className="mt-2">
                    Error Log: {logsToUse?.Tasks?.error_log || 'N/A'}
                  </div>
                  <div>
                    <Button
                      variant="link"
                      onClick={() =>
                        handleCopy(
                          logsToUse?.Tasks?.stderr ?? ""
                        )
                      }
                    >
                      Copy
                    </Button>
                    <Button
                      variant="link"
                      onClick={() =>
                        handleOpenInNewTab(
                          logsToUse?.Tasks?.stderr ?? ""
                        )
                      }
                    >
                      Open
                    </Button>
                  </div>
                </div>
                <Card className="no-border">
                  <Card.Body>
                    <pre>{logsToUse?.Tasks?.stderr || 'No stderr logs available'}</pre>
                  </Card.Body>
                </Card>
              </Tab>
            </Tabs>
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
        {!onlyView && (
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
    await ClusterApi.postPersistData(
      JSON.stringify({
        upgradeIsFinalizeItem: JSON.stringify(false)
      })
    )
    const payload = {
      "Upgrade": { 
        "upgrade_type": data?.Upgrade.upgrade_type,
        "direction": "DOWNGRADE",
      }
    }

    try {
      const response = await VersionsApi.getUpgradeId(payload, clusterName);
      const downgradeId = response?.resources[0]?.Upgrade?.request_id;
      setUpgradeId(downgradeId);
      setUpgradeState("PENDING");
      setUpgradeModal(false);
      
      modalManager.show(<Upgrade upgradeId={downgradeId} />);
      window.location.reload();
    } catch (error) {
      modalManager.show({
        modalTitle: "Upgrade could not be started",
        modalBody: (
          <div>
            {error instanceof Error ? error.message : String(error)}
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

    try {
      abortUpgrade();
      var interval = setInterval(async function() {
        const response = await VersionsApi.getUpgradeOperations(upgradeId, clusterName);
        if (response?.Upgrade?.request_status === "ABORTED") {
          clearInterval(interval);
          startDowngrade();
        }
      }, 1000);
    } catch (error) {
      console.log("Error aborting upgrade:", error);
    }
  }

  async function abortUpgrade() {
    try {
      await VersionsApi.abortUpgrade(clusterName, upgradeId);
    } catch (error) {
      console.error("Error aborting upgrade:", error);
      if(upgradeParameters.isDowngrade) {
        // const header = get(messages, "admin.stackDowngrade.state.paused.fail.header");
        let body = "Downgrade could not be paused. Try again later."
        if(error) {
          body = body + ' ' + error;
        }
        toast(body);
      } else {
        // const header = get(messages, "admin.stackUpgrade.state.paused.fail.header");
        let body = "Downgrade could not be paused. Try again later."
        if(error) {
          body = body + ' ' + error;
        }
        toast(body);
      }
    }
  }

  async function pauseUpgrade() {
    try {
      await suspendUpgrade();
      setPauseUpgradeModal(false);
      setUpgradeModal(false);
    } catch (error) {
      console.log("can't abort upgrade with suspend.");
    }
  }

  async function suspendUpgrade() {
    try {
      await abortUpgradeWithSuspend();
      setUpgradeState("ABORTED");
    } catch (error) {
      toast.error("Error suspending upgrade");
      console.log("can't abort upgrade with suspend.");
    }
  }

  async function resumeUpgrade() {
    try {
      await retryUpgrade();
      setUpgradeState("PENDING");
    } catch (error) {
      console.log("can't resume upgrade")
    }
  }

  async function retryUpgrade() {
    await VersionsApi.retryUpgrade(clusterName, upgradeId);
  }

  async function abortUpgradeWithSuspend() {
    try {
      await VersionsApi.suspendUpgrade(clusterName, upgradeId);
      setUpgradeState("ABORTED");
    } catch (error) {
      console.error("Error aborting upgrade:", error);
      if(upgradeParameters.isDowngrade) {
        let body = "Downgrade could not be paused. Try again later.";
        if(error) {
          body = body + ' ' + error;
        }
        toast(body);
      } else {
        let body = "Downgrade could not be paused. Try again later.";
        if(error) {
          body = body + ' ' + error;
        }
        toast(body);
      }
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
      // Only fetch if we don't already have logs
      if (!currUpgradeItem?.tasks?.[0]?.logs || currUpgradeItem?.tasks?.[0]?.logs === null) {
        setLoadingLogs(true);
        try {
          const groupId = item?.UpgradeItem.group_id ?? 0;
          const stageId = item?.UpgradeItem.stage_id ?? 0;
          
          // First fetch tasks if they don't exist
          if (!currUpgradeItem?.tasks) {
            await fetchTasks(groupId, stageId);
            // Wait a bit for state to update
            await new Promise(resolve => setTimeout(resolve, 100));
          }
          
          const tasksResponse = await VersionsApi.getTasksList(upgradeId, groupId, stageId, clusterName);
          if (tasksResponse.tasks && tasksResponse.tasks.length > 0) {
            const taskId = tasksResponse.tasks[0].Tasks.id;
            
            const tasksData = tasksResponse.tasks.map((task: any) => ({ ...task.Tasks, logs: null }));
            setLocalTasks(tasksData);
            
            const logs = await VersionsApi.getTasksLogs(upgradeId, groupId, stageId, taskId, clusterName);
            setLocalLogs(logs);
            
            await fetchTasks(groupId, stageId);
            await fetchLogs(groupId, stageId, taskId);
          }
        } catch (error) {
          console.error("Error fetching logs:", error);
        } finally {
          setLoadingLogs(false);
        }
      }
    }
  }

  function getUpgradeModalBody() {
    return (
      <>
        <div className="d-flex justify-content-between mb-4">
          <div>{translate(`${upgradeParameters.upgradeStatusLabel}`)}</div>
          <div className="d-flex">
            <ProgressBar
              now={data?.Upgrade.progress_percent}
              className="progress-bar-width-upgrade me-10"
            />
            <div className="ms-2">{Math.round(data?.Upgrade.progress_percent || 0)}% </div>
          </div>
          {upgradeParameters.showPauseButton && !onlyView && (
            <Button
              size="sm"
              variant="light"
              className="text-uppercase"
              onClick={() => setPauseUpgradeModal(true)}
            >
              {upgradeParameters.isDowngrade ? 
                translate("admin.stackUpgrade.pauseDowngrade") : translate("admin.stackUpgrade.pauseUpgrade")
              }
            </Button>
          )}
        </div>
        <Card className="mb-4">
          <Card.Body>
            <div className="d-flex-column">
            {upgradeParameters.runningItem && !onlyView && (
              <>
                <div className="d-flex justify-content-between align-items-center">
                  <div>{translate("admin.stackUpgrade.dialog.inProgress")} {" "} {currUpgradeItem?.UpgradeItem.context}</div>
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

            {upgradeParameters.failedItem && !onlyView && !upgradeParameters.upgradeSuspended && !upgradeParameters.runningItem &&
            !upgradeParameters.isSlaveComponentFailuresItem ? (
              <>
                <div className="d-flex justify-content-between">
                  <div>
                    {translate("admin.stackUpgrade.dialog.failed")}
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
                        disabled={upgradeParameters.requestInProgress}
                        onClick={() => setConfirmDowngradeModal(true)}
                      >
                        Downgrade
                      </Button>
                    )} 
                    {upgradeParameters.canSkipFailedItem && (
                      <Button
                        variant="warning"
                        className="text-uppercase me-2"
                        disabled={upgradeParameters.requestInProgress}
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
                        {translate("admin.stackUpgrade.dialog.continue")}
                      </Button>
                    )} 
                    <Button
                      variant="info"
                      className="text-uppercase mx-1"
                      disabled={upgradeParameters.requestInProgress}
                      onClick={() => {
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "PENDING")
                      }
                      }
                    >
                      {translate("common.retry")}
                    </Button>
                  </div>
                </div>
              </>
            ) : null}

            { upgradeParameters.isSlaveComponentFailuresItem && !onlyView && !upgradeParameters.upgradeSuspended && 
              <>
                <div className="manual-steps-section">
                  <div className="manual-steps-title">{translate("admin.stackUpgrade.dialog.manual")}</div>
                  <div className="upgrade-failed-message">
                    {translate("admin.stackUpgrade.failedHosts.message")}
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
                    <div className="options-title">{translate("admin.stackUpgrade.failedHosts.options")}</div>
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
                        label={translate("admin.stackUpgrade.dialog.manualDone")}
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
                        disabled={upgradeParameters.requestInProgress}
                      >
                        {translate("common.downgrade")}
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
                      disabled={upgradeParameters.requestInProgress}
                    >
                      {translate("common.retry")}
                    </Button>
                    <Button
                      variant="success"
                      size="sm"
                      onClick={() => {
                        setManualDone(false);
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "COMPLETED")
                      }}
                      disabled={upgradeParameters.requestInProgress || !isManualDone}
                    >
                      {translate("common.proceed")}
                    </Button>
                  </div>
                  
                </div>
              </>
            }

            { upgradeParameters.isServiceCheckFailuresItem && !onlyView && !upgradeParameters.upgradeSuspended && 
              <>
                <div>
                  <div>{translate("admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.areServiceCheckFailuresServicenamesLoaded ? (
                    upgradeParameters.serviceCheckFailuresServicenames.length ? (
                      <div>
                        <div>{translate("admin.stackUpgrade.dialog.manual.serviceCheckFailures.title")}</div>
                        <div>{translate("admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg1")}</div>
                        <ul className="failed-info-list">
                          {upgradeParameters.serviceCheckFailuresServicenames.map((serviceName: any, index: any) => (
                            <li key={index}>{serviceName}</li>
                          ))}
                        </ul>
                        <div>{translate("admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg2")}</div>
                      </div>
                    ) : upgradeParameters.slaveComponentStructuredInfo.hosts.length ? (
                      <div>
                        <div>{translate("admin.stackUpgrade.dialog.manual.slaveComponentFailures.title")}</div>
                        <div>
                          <div>{translate("admin.stackUpgrade.failedHosts.message")}</div>
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
                          label={translate(
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
                          disabled={upgradeParameters.requestInProgress}
                        >
                          {translate("common.downgrade")}
                        </Button>
                      ) : null}
                      <Button
                        variant="primary"
                        className="me-2"
                        onClick={() => {
                          setManualDone(false);
                          if(currUpgradeItem)
                            setUpgradeItemStatus(currUpgradeItem, "PENDING")
                        }
                        }
                      >
                        {translate("common.retry")}
                      </Button>
                    </div>
                  </div>
              </>
            }

            {upgradeParameters.isFinalizeItem && !onlyView && !upgradeParameters.upgradeSuspended ? (
              <>
                <div className="mb-2">
                  <div className="text-dark mb-2">{translate("admin.stackUpgrade.dialog.manual")}</div>
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
                          disabled={upgradeParameters.requestInProgress}
                        >
                          {translate("common.downgrade")}
                        </Button>
                      ) : null}
                      <Button
                        variant="light"
                        className="ms-2"
                        onClick={() => setPauseUpgradeModal(true)}
                      >
                        {translate("admin.stackUpgrade.finalize.later")}
                      </Button>
                      <Button
                        variant="primary"
                        className="ms-2"
                        disabled={!isManualDone}
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

            {upgradeParameters.plainManualItem && !upgradeParameters.isFinalizeItem && !onlyView && !upgradeParameters.upgradeSuspended ? (
              <div>
                <div className="mb-3">
                  <div className="me-2 mt-1 text-dark">{translate("admin.stackUpgrade.dialog.manual")}</div>
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
                        label={translate(
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
                        disabled={upgradeParameters.requestInProgress}
                      >
                        {translate("common.downgrade")}
                      </Button>
                    ) : null}
                    <Button
                      variant="primary"
                      className="mx-2"
                      disabled={!isManualDone}
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

            {upgradeParameters.upgradeSuspended && !onlyView && (
              <>
                <div className="d-flex justify-content-between">
                <div className="mt-3">
                  {upgradeParameters.isDowngrade ? (
                    <label>{translate("admin.stackUpgrade.dialog.suspended.downgrade")}</label>
                  ) : (
                    <label>{translate("admin.stackUpgrade.dialog.suspended")}</label>
                  )}
                </div>

                {upgradeParameters.isDowngrade ? (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                  >
                    {translate("admin.stackUpgrade.dialog.resume.downgrade")}
                  </Button>
                ) : (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                  >
                    {translate("admin.stackUpgrade.dialog.resume")}
                  </Button>
                )}
              </div>
            </>
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
          onlyView={onlyView}
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
    if (!upgradeId) {
      toast.error("No active upgrade found");
      return;
    }

    const payload = {
      Upgrade: {
        skip_failures: slaveComponentFailures.toString(),
        skip_service_check_failures: serviceCheckFailures.toString(),
      },
    };

    try {
      await VersionsApi.updateUpgrade(upgradeId, payload, clusterName);
      toast.success("Upgrade options updated successfully");
      setUpgradeOptionsModal(false);
    } catch (error) {
      toast.error("Failed to update upgrade options");
      console.error("Error updating upgrade options:", error);
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
              (method: any) =>
                method.allowed
            )
            .map((method: any) => (
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
                onChange={(e) => setServiceCheckFailures(e.target.checked)}
              />
            </Form.Group>
            <Form.Group controlId="slaveComponentFailures">
              <Form.Check
                type="checkbox"
                label="Skip all Slave Component failures"
                checked={slaveComponentFailures}
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
      {data && upgradeModal && (
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
        modalTitle={translate("admin.stackUpgrade.failedHosts.header")}
        modalBody={getFailedHostsModalBody()}
        options={{
          okButtonText: translate("common.close"),
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
          }}
          successCallback={() => updateUpgradeOptions()}
        />
      )}
    </>
  );
}
