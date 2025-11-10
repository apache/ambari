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

import {
  Collapse,
  Card,
  Button,
  Tabs,
  Tab,
  ProgressBar,
} from "react-bootstrap";
import {
  Task,
  UpgradeGroup,
  UpgradeItem,
} from "../screens/ClusterAdmin/StackAndVersions/types";
import { useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { getIconObject } from "../Utils/Utility";

type NestedCollapseProps = {
  groups: UpgradeGroup[];
  activeKeys: string[];
  setActiveKeys: any;
  fetchTasks: (groupId: number, stageId: number) => Promise<void>;
  fetchLogs: (
    groupId: number,
    stageId: number,
    taskId: number
  ) => Promise<void>;
  handleCopy: (text: string) => void;
  handleOpenInNewTab: (content: string) => void;
  onlyView: boolean;
};

type NestedSubCollapseProps = {
  items: UpgradeItem[];
  groupId: number;
  activeKeys: string[];
  setActiveKeys: any;
  fetchTasks: (groupId: number, stageId: number) => Promise<void>;
  fetchLogs: (
    groupId: number,
    stageId: number,
    taskId: number
  ) => Promise<void>;
  handleCopy: (text: string) => void;
  handleOpenInNewTab: (content: string) => void;
  onlyView: boolean;
};

const SubCollapse = ({
  items,
  groupId,
  activeKeys,
  setActiveKeys,
  fetchTasks,
  fetchLogs,
  handleCopy,
  handleOpenInNewTab,
  onlyView
}: NestedSubCollapseProps) => {
  const [logsActiveKeys, setLogsActiveKeys] = useState<{
    [key: number]: boolean;
  }>({});

  const handleFetchLogs = async (
    groupId: number,
    stageId: number,
    taskId: number
  ) => {
    await fetchLogs(groupId, stageId, taskId);
    setLogsActiveKeys((prevKeys) => ({
      ...prevKeys,
      [taskId]: !prevKeys[taskId],
    }));
  };

  const handleToggle = (key: string) => {
    setActiveKeys((prevKeys: string[]) =>
      prevKeys.includes(key)
        ? prevKeys.filter((k) => k !== key)
        : [...prevKeys, key]
    );
  };

  return (
    <>
      {items &&
        items.map((item: UpgradeItem) => {
          const eventKey = `${groupId}-${item.UpgradeItem.stage_id}`;
          return (
            <Card key={eventKey} border="light">
              <Card.Header>
                <Button
                  className="custom-link"
                  variant="link"
                  onClick={async () => {
                    handleToggle(eventKey);
                    await fetchTasks(groupId, item.UpgradeItem.stage_id);
                  }}
                  aria-controls={eventKey}
                  aria-expanded={activeKeys.includes(eventKey)}
                >
                  <div className="d-flex justify-content-between">
                    { getIconObject(item.UpgradeItem.display_status, onlyView) && (
                      <FontAwesomeIcon 
                        icon={getIconObject(item.UpgradeItem.display_status, onlyView).icon}
                        size="lg"
                        className={`me-2 ${getIconObject(item.UpgradeItem.display_status, onlyView).color}`}
                      />
                    )}
                    <div>{item.UpgradeItem.context}</div>
                    {item.UpgradeItem.status === "IN_PROGRESS" && (
                      <div>
                        <ProgressBar
                          className="ms-2 progress-bar-width-upgrade"
                          now={item.UpgradeItem.progress_percent}
                          label={`${item.UpgradeItem.progress_percent}%`}
                        />
                      </div>
                    )}
                  </div>
                </Button>
              </Card.Header>
              <Collapse in={activeKeys.includes(eventKey)} className="ms-4">
                <div id={eventKey}>
                  <Card.Body>
                    {item.tasks
                      ? item.tasks.map((task: Task, taskIndex: number) => (
                          <div key={taskIndex}>
                            {task?.id ? (
                              <Button
                                className="custom-link"
                                variant="link"
                                onClick={async () =>
                                  await handleFetchLogs(
                                    groupId,
                                    item.UpgradeItem.stage_id,
                                    task.id
                                  )
                                }
                              >
                                { getIconObject(task?.status, onlyView) ? (
                                  <FontAwesomeIcon 
                                    icon={getIconObject(task?.status, onlyView).icon}
                                    size="lg"
                                    className={`me-2 ${getIconObject(item.UpgradeItem.display_status, onlyView).color}`}
                                  />
                                  ) : null
                                }
                                {task?.command_detail}
                              </Button>
                            ) : null}
                            {task.logs && (
                              <>
                                <Collapse in={logsActiveKeys[task.id]}>
                                  <div id={`logs-${task.id}`} className="mt-2 ms-2">
                                    <Tabs
                                      defaultActiveKey="stdout"
                                      id={`logs-tabs-${task.id}`}
                                    >
                                      <Tab eventKey="stdout" title="STDOUT">
                                        <div className="mt-3">
                                          Host: {task.logs?.Tasks?.host_name}
                                        </div>
                                        <div className="d-flex justify-content-between">
                                          <div className="mt-2">
                                            Output Log:{" "}
                                            {task.logs?.Tasks?.output_log}
                                          </div>
                                          <div>
                                            <Button
                                              variant="link"
                                              onClick={() =>
                                                handleCopy(
                                                  task.logs?.Tasks?.stdout as any
                                                )
                                              }
                                            >
                                              Copy
                                            </Button>
                                            <Button
                                              variant="link"
                                              onClick={() =>
                                                handleOpenInNewTab(
                                                  task?.logs?.Tasks?.stdout as any
                                                )
                                              }
                                            >
                                              Open
                                            </Button>
                                          </div>
                                        </div>
                                        <Card>
                                          <Card.Body>
                                            <pre>{task?.logs?.Tasks?.stdout}</pre>
                                          </Card.Body>
                                        </Card>
                                      </Tab>
                                      <Tab eventKey="stderr" title="STDERR">
                                        <div className="mt-3">
                                          Host: {task.logs?.Tasks?.host_name}
                                        </div>
                                        <div className="d-flex justify-content-between">
                                          <div className="mt-2">
                                            Error Log: {task?.logs?.Tasks?.error_log}
                                          </div>
                                          <div>
                                            <Button
                                              variant="link"
                                              onClick={() =>
                                                handleCopy(
                                                  task.logs?.Tasks?.stderr as any
                                                )
                                              }
                                            >
                                              Copy
                                            </Button>
                                            <Button
                                              variant="link"
                                              onClick={() =>
                                                handleOpenInNewTab(
                                                  task.logs?.Tasks?.stderr as any
                                                )
                                              }
                                            >
                                              Open
                                            </Button>
                                          </div>
                                        </div>
                                        <Card>
                                          <Card.Body>
                                            <pre>{task.logs?.Tasks?.stderr}</pre>
                                          </Card.Body>
                                        </Card>
                                      </Tab>
                                    </Tabs>
                                  </div>
                                </Collapse>
                              </>
                            )}
                          </div>
                        ))
                      : null}
                  </Card.Body>
                </div>
              </Collapse>
            </Card>
          );
        })}
    </>
  );
};

export default function NestedCollapse({
  groups,
  activeKeys,
  setActiveKeys,
  fetchTasks,
  fetchLogs,
  handleCopy,
  handleOpenInNewTab,
  onlyView
}: NestedCollapseProps) {
  const handleToggle = (key: string) => {
    setActiveKeys((prevKeys: string[]) =>
      prevKeys.includes(key)
        ? prevKeys.filter((k) => k !== key)
        : [...prevKeys, key]
    );
  };
  return (
    <>
      {groups &&
        groups.map((group: UpgradeGroup) => {
          const eventKey = `group-${group.UpgradeGroup.group_id}`;
          return (
            <Card key={eventKey} border="light">
              <Card.Header>
                <Button
                  className="custom-link"
                  variant="link"
                  disabled={!onlyView && group.UpgradeGroup.display_status === "ABORTED"}
                  onClick={() => handleToggle(eventKey)}
                  aria-controls={eventKey}
                  aria-expanded={activeKeys.includes(eventKey)}
                >

                  <div className="d-flex justify-content-between">
                    { getIconObject(group.UpgradeGroup.display_status, onlyView) && (
                      <FontAwesomeIcon 
                        icon={getIconObject(group.UpgradeGroup.display_status, onlyView).icon}
                        size="lg"
                        className={`me-2 ${getIconObject(group.UpgradeGroup.display_status, onlyView).color}`}
                      />
                    )}
                    <div>{group.UpgradeGroup.title}</div>
                    {group.UpgradeGroup.status === "IN_PROGRESS" && (
                      <div>
                        <ProgressBar
                          className="ms-2 progress-bar-width-upgrade"
                          now={group.UpgradeGroup.progress_percent}
                          label={`${group.UpgradeGroup.completed_task_count}/${group.UpgradeGroup.total_task_count}`}
                        />
                      </div>
                    )}
                  </div>
                </Button>
              </Card.Header>
              <Collapse in={activeKeys.includes(eventKey)}>
                <div id={eventKey}>
                  <Card.Body>
                    <SubCollapse
                      items={[...group.upgrade_items].filter(item => 
                        onlyView || (item.UpgradeItem.status !== "PENDING" && item.UpgradeItem.display_status !== "ABORTED")
                      ).reverse()}
                      groupId={group.UpgradeGroup.group_id}
                      activeKeys={activeKeys}
                      setActiveKeys={setActiveKeys}
                      fetchTasks={fetchTasks}
                      fetchLogs={fetchLogs}
                      handleCopy={handleCopy}
                      handleOpenInNewTab={handleOpenInNewTab}
                      onlyView={onlyView}
                    />
                  </Card.Body>
                </div>
              </Collapse>
            </Card>
          );
        })}
    </>
  );
}