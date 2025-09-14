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
  Badge,
  Button,
  Card,
  Collapse,
  Form,
  ProgressBar,
  Tab,
  Tabs,
} from "react-bootstrap";
import _, { get } from "lodash";
import Modal from "../../../components/Modal";
import Spinner from "../../../components/Spinner";
import VersionsApi from "../../../api/VersionsApi";
import { AppContext } from "../../../store/context";
import toast from "react-hot-toast";
import modalManager from "../../../store/ModalManager";
import { getUpgradeDisplayName, translate, translateWithVariables } from "../../../Utils/Utility";
import { useUpgrade } from "../../../hooks/useUpgrade";
import NestedCollapse from "../../../components/NestedCollapse";

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
  const [localLogs, setLocalLogs] = useState<any>(null);
  const [localTasks, setLocalTasks] = useState<any[]>([]);
  const { clusterName, setUpgradeState, setUpgradeId, isPatchUpgrade, upgradeVersionDisplayName } = useContext(AppContext);

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
                          logsToUse?.Tasks?.output_log ?? ""
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
                          logsToUse?.Tasks?.error_log ?? ""
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
    return `${getUpgradeDisplayName(data?.Upgrade.upgrade_type || "rolling")} ${
      isPatchUpgrade ? "Patch" : ""
    } ${data?.Upgrade.direction} ${
      data?.Upgrade.direction == "DOWNGRADE" ? "from" : "to"
    } ${
      upgradeVersionDisplayName != ""
        ? upgradeVersionDisplayName
        : data?.Upgrade.associated_version
    }`;
  }

  async function startDowngrade() {
    const payload = JSON.stringify({
      "Upgrade": {
        "upgrade_type": data?.Upgrade.upgrade_type,
        "direction": "DOWNGRADE",
      }
    })

    try {
      const response = await VersionsApi.getUpgradeId(payload, clusterName);
      const downgradeId = response?.resources[0]?.Upgrade?.request_id;
      setUpgradeId(downgradeId);
      setUpgradeState("PENDING");
      setUpgradeModal(false);
      
      modalManager.show(<Upgrade upgradeId={downgradeId} />);
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
        // const header = translate( "admin.stackDowngrade.state.paused.fail.header");
        let body = translate( "admin.stackDowngrade.state.paused.fail.body");
        if(error) {
          body = body + ' ' + error;
        }
        toast(body.toString());
      } else {
        // const header = translate( "admin.stackUpgrade.state.paused.fail.header");
        let body = translate( "admin.stackUpgrade.state.paused.fail.body");
        if(error) {
          body = body + ' ' + error;
        }
        toast(body.toString());
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
        let body = translate( "admin.stackDowngrade.state.paused.fail.body");
        if(error) {
          body = body + ' ' + error;
        }
        toast(body.toString());
      } else {
        let body = translate( "admin.stackUpgrade.state.paused.fail.body");
        if(error) {
          body = body + ' ' + error;
        }
        toast(body.toString());
      }
    }
  }

  function getFailedHostsMessage(slaveComponentStructuredInfo: any) {
    const count = get(slaveComponentStructuredInfo, "hosts.length", 0);
    return translateWithVariables("admin.stackUpgrade.failedHosts, showHosts", count);
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
          <div>{translate( `${upgradeParameters.upgradeStatusLabel}`)}</div>
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
                translate( "admin.stackUpgrade.pauseDowngrade") : translate( "admin.stackUpgrade.pauseUpgrade")
              }
            </Button>
          )}
        </div>
        <Card className="mb-4">
          <Card.Body>
            <div className="d-flex-column">
            {upgradeParameters.runningItem && (
              <>
                <div className="d-flex justify-content-between align-items-center">
                  <div>{translate( "admin.stackUpgrade.dialog.inProgress")} {" "} {currUpgradeItem?.UpgradeItem.context}</div>
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

            {upgradeParameters.failedItem && !upgradeParameters.upgradeSuspended && !upgradeParameters.runningItem &&
            !upgradeParameters.isSlaveComponentFailuresItem ? (
              <>
                <div className="d-flex justify-content-between">
                  <div>
                    {translate( "admin.stackUpgrade.dialog.failed")}
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
                        variant="link"
                        className="text-uppercase"
                        disabled={upgradeParameters.requestInProgress}
                        onClick={() => setConfirmDowngradeModal(true)}
                      >
                        Downgrade
                      </Button>
                    )} 
                    {upgradeParameters.canSkipFailedItem && (
                      <Button
                        variant="warning"
                        className="text-uppercase"
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
                        {translate( "admin.stackUpgrade.dialog.continue")}
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
                      {translate( "common.retry")}
                    </Button>
                  </div>
                </div>
              </>
            ) : null}

            { upgradeParameters.isSlaveComponentFailuresItem && !upgradeParameters.upgradeSuspended && 
              <>
                <div>
                  <div>{translate( "admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.areSlaveComponentFailuresHostsLoaded ? (
                    <div>
                      <div>{translate( "admin.stackUpgrade.failedHosts.message")}</div>
                      <Badge onClick={() => setFailedhostsModal(true)} >{getFailedHostsMessage(upgradeParameters.slaveComponentStructuredInfo)}</Badge>
                    </div>
                  ) : <Spinner />}
                  <div>
                    <p>{translate( "admin.stackUpgrade.failedHosts.options")}</p>
                    <ul>
                      <li>{translate("admin.stackUpgrade.failedHosts.options.first")}</li>
                      <li>{translate("admin.stackUpgrade.failedHosts.options.second")}</li>
                    </ul>
                  </div>
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
                    {upgradeParameters.isDowngradeAvailable ? (
                      <Button
                        variant="danger"
                        onClick={() => setConfirmDowngradeModal(true)}
                        disabled={upgradeParameters.requestInProgress}
                      >
                        {translate( "common.downgrade")}
                      </Button>
                    ) : null}
                    <Button
                      variant="primary"
                      onClick={() => {
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "PENDING")
                      }
                      }
                    >
                      {translate( "common.retry")}
                    </Button>
                    <Button
                      variant="primary"
                      onClick={() => {
                        if(currUpgradeItem)
                          setUpgradeItemStatus(currUpgradeItem, "COMPLETED")
                      }
                      }
                    >
                      {translate( "common.proceed")}
                    </Button>
                  </div>
              </>
            }

            { upgradeParameters.isServiceCheckFailuresItem && !upgradeParameters.upgradeSuspended && 
              <>
                <div>
                  <div>{translate( "admin.stackUpgrade.dialog.manual")}</div>
                  {upgradeParameters.areServiceCheckFailuresServicenamesLoaded ? (
                    upgradeParameters.serviceCheckFailuresServicenames.length ? (
                      <div>
                        <div>{translate( "admin.stackUpgrade.dialog.manual.serviceCheckFailures.title")}</div>
                        <div>{translate( "admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg1")}</div>
                        <ul className="failed-info-list">
                          {upgradeParameters.serviceCheckFailuresServicenames.map((serviceName: any, index: any) => (
                            <li key={index}>{serviceName}</li>
                          ))}
                        </ul>
                        <div>{translate( "admin.stackUpgrade.dialog.manual.serviceCheckFailures.msg2")}</div>
                      </div>
                    ) : upgradeParameters.slaveComponentStructuredInfo.hosts.length ? (
                      <div>
                        <div>{translate( "admin.stackUpgrade.dialog.manual.slaveComponentFailures.title")}</div>
                        <div>
                          <div>{translate( "admin.stackUpgrade.failedHosts.message")}</div>
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
                          onClick={() => setConfirmDowngradeModal(true)}
                          disabled={upgradeParameters.requestInProgress}
                        >
                          {translate( "common.downgrade")}
                        </Button>
                      ) : null}
                      <Button
                        variant="primary"
                        onClick={() => {
                          if(currUpgradeItem)
                            setUpgradeItemStatus(currUpgradeItem, "PENDING")
                        }
                        }
                      >
                        {translate( "common.retry")}
                      </Button>
                    </div>
                  </div>
              </>
            }

            {upgradeParameters.isFinalizeItem && !upgradeParameters.upgradeSuspended ? (
              <>
                <div className="mb-2">
                  <div className="text-dark mb-2">{translate( "admin.stackUpgrade.dialog.manual")}</div>
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

            {upgradeParameters.plainManualItem && !upgradeParameters.isFinalizeItem && !upgradeParameters.upgradeSuspended ? (
              <div>
                <div className="mb-3">
                  <div className="me-2 mt-1 text-dark">{translate( "admin.stackUpgrade.dialog.manual")}</div>
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
                        {translate( "common.downgrade")}
                      </Button>
                    ) : null}
                    <Button
                      variant="primary"
                      className="mx-2"
                      disabled={!isManualDone}
                      onClick={() => {
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
                    <label>{translate( "admin.stackUpgrade.dialog.suspended.downgrade")}</label>
                  ) : (
                    <label>{translate( "admin.stackUpgrade.dialog.suspended")}</label>
                  )}
                </div>

                {upgradeParameters.isDowngrade ? (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                  >{translate( "admin.stackUpgrade.dialog.resume.downgrade")}</Button>
                ) : (
                  <Button
                    variant="primary"
                    onClick={() => resumeUpgrade()}
                  >{translate( "admin.stackUpgrade.dialog.resume")}</Button>
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
        modalTitle={translate( "admin.stackUpgrade.failedHosts.header")}
        modalBody={upgradeParameters.slaveComponentStructuredInfo}
        options={{
          okButtonText: translate( "common.close"),
          modalSize: "modal-sm",
          cancelableViaIcon: true,
        }}
        successCallback={() => setFailedhostsModal(false)}
        />
      )}
    </>
  );
}