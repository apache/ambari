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

import { useCallback, useContext, useEffect, useState } from "react";
import ClusterApi from "../../api/clusterApi";
import { Button, Stack } from "react-bootstrap";
import type { IMessage, StompSubscription } from "@stomp/stompjs";
import { AppContext } from "../../store/context";
import { useCopyToClipboard } from "../../hooks/useCopyToClipboard";
import toast from "react-hot-toast";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCopy,
  faExternalLinkAlt,
} from "@fortawesome/free-solid-svg-icons";
import { isOperationTerminal } from "../../Utils/backgroundOperations";
import Center from "../../components/Center";

type TaskLogsProps = {
  requestId: number | string;
  task: {
    id: number | string;
    status?: string;
  };
  clusterName?: string;
};

type TaskLogState = {
  error_log?: string;
  output_log?: string;
  status?: string;
  stderr?: string;
  stdout?: string;
  [key: string]: unknown;
};

type TaskLogEvent = TaskLogState & {
  errorLog?: string;
  outLog?: string;
};

function TaskLogs({ requestId, task, clusterName: clusterNameProps }: TaskLogsProps) {
  const [logs, setLogs] = useState<TaskLogState>({});
  const [taskStatus, setTaskStatus] = useState(task.status);
  const [error, setError] = useState("");
  const { isSocketConnected, client } = useContext(AppContext);
  const [, copy] = useCopyToClipboard();
  const { clusterName: contextClusterName } = useContext(AppContext);
  const clusterName = clusterNameProps || contextClusterName;
  const getTaskLogs = useCallback(async (
    isActive: () => boolean = () => true,
  ): Promise<boolean> => {
    setError("");
    try {
      const allTaskLogs = await ClusterApi.getClusterRequestTaskLogs(
        clusterName,
        requestId,
        task.id
      );
      if (isActive()) {
        setLogs(allTaskLogs?.Tasks || {});
        setTaskStatus(allTaskLogs?.Tasks?.status || task.status);
      }
      return isOperationTerminal(allTaskLogs?.Tasks?.status);
    } catch {
      if (isActive()) setError("Ambari could not load task output.");
      return false;
    }
  }, [clusterName, requestId, task.id, task.status]);

  const taskIsTerminal = isOperationTerminal(taskStatus);
  useEffect(() => {
    let subscription: StompSubscription | undefined;
    if (isSocketConnected && !taskIsTerminal) {
      subscription = client.subscribe(
        `/events/tasks/${task.id}`,
        (taskMessage: IMessage) => {
          try {
            const message = JSON.parse(taskMessage.body) as TaskLogEvent;
            const { stderr, stdout } = message;
            setLogs((current) => ({
              ...current,
              ...message,
              error_log: message.errorLog ?? current.error_log,
              output_log: message.outLog ?? current.output_log,
              stderr: stderr || "None",
              stdout: stdout || "",
            }));
            setTaskStatus((current) => message.status || current);
            if (isOperationTerminal(message.status)) {
              subscription?.unsubscribe();
            }
          } catch {
            console.error(`Ambari ignored a malformed task update for task ${task.id}.`);
          }
        }
      );
    }
    return () => {
      subscription?.unsubscribe();
    };
  }, [client, isSocketConnected, task.id, taskIsTerminal]);
  useEffect(() => {
    setTaskStatus(task.status);
  }, [task.id, task.status]);
  useEffect(() => {
    let active = true;
    let timeout: ReturnType<typeof setTimeout> | undefined;
    const poll = async () => {
      const finished = await getTaskLogs(() => active);
      if (active && !isSocketConnected && !finished) {
        timeout = setTimeout(poll, 5000);
      }
    };
    void poll();
    return () => {
      active = false;
      if (timeout) clearTimeout(timeout);
    };
  }, [getTaskLogs, isSocketConnected]);

  if (error && !logs.stdout && !logs.stderr) {
    return (
      <Center>
        <Stack direction="vertical" className="align-items-center gap-2">
          <div className="text-danger">{error}</div>
          <Button size="sm" variant="outline-primary" onClick={() => void getTaskLogs()}>
            Retry
          </Button>
        </Stack>
      </Center>
    );
  }
  return (
    <>
      {error ? (
        <div className="d-flex align-items-center justify-content-between text-danger">
          <span>{error}</span>
          <Button size="sm" variant="outline-danger" onClick={() => void getTaskLogs()}>
            Retry
          </Button>
        </div>
      ) : null}
      <Stack
        direction="horizontal"
        className="justify-content-between mt-3 w-100"
      >
        <h2>Task Logs</h2>
        <Stack direction="horizontal">
          <div
            className="custom-link"
            onClick={() => {
              copy(`stderr:
${logs.stderr}
stdout:
${logs.stdout}`).then(() => {
                toast.success("Copied to clipboard");
              });
            }}
          >
            <FontAwesomeIcon icon={faCopy} className="me-1" />
            COPY
          </div>
          <div
            className="custom-link ms-2"
            onClick={() => {
              const tab = window.open("about:blank", "_blank");
              if (tab) {
                const pre = tab.document.createElement("pre");
                pre.textContent = `stderr:\n${logs.stderr || ""}\nstdout:\n${logs.stdout || ""}`;
                tab.document.body.replaceChildren(pre);
              }
            }}
          >
            <FontAwesomeIcon icon={faExternalLinkAlt} className="me-1" />
            OPEN
          </div>
        </Stack>
      </Stack>
      <div className="task-logs">
        <Stack direction="vertical" className="mt-2">
          <small className="text-muted">stderr:{logs.error_log}</small>
          <pre className="mt-2">{logs.stderr}</pre>
        </Stack>
        <Stack direction="vertical" className="mt-2">
          <small className="text-muted">stdout:{logs.output_log}</small>
          <pre className="mt-2">{logs.stdout}</pre>
        </Stack>
      </div>
    </>
  );
}

export default TaskLogs;
