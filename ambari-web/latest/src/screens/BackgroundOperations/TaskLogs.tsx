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
import { Stack } from "react-bootstrap";
import { AppContext } from "../../store/context";
import { useCopyToClipboard } from "../../hooks/useCopyToClipboard";
import toast from "react-hot-toast";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCopy,
  faExternalLinkAlt,
} from "@fortawesome/free-solid-svg-icons";

type TaskLogsProps = {
  requestId: number | string;
  task: any;
  clusterName?: string;
};
function TaskLogs({ requestId, task,clusterName:clusterNameProps }: TaskLogsProps) {
  const [logs, setLogs] = useState<any>({});
  const { isSocketConnected, client } = useContext(AppContext);
  const [, copy] = useCopyToClipboard();
  const { clusterName:cName } = useContext(AppContext);
  const clusterName= clusterNameProps || cName;
  async function getTaskLogs() {
    const allTaskLogs = await ClusterApi.getClusterRequestTaskLogs(
      clusterName,
      requestId,
      task.id
    );
    setLogs(allTaskLogs?.Tasks);
  }
  useEffect(() => {
    let subscription: any;
    if (isSocketConnected) {
      subscription = client.subscribe(
        `/events/tasks/${task.id}`,
        (taskMessage: any) => {
          const message = JSON.parse(taskMessage.body);
          const { stderr, stdout } = message;
          setLogs({ ...logs, stderr: stderr ? stderr : "None", stdout });
        }
      );
    }
    return () => {
      subscription?.unsubscribe();
    };
  }, [isSocketConnected]);
  useEffect(() => {
    getTaskLogs();
  }, []);
  return (
    <>
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
              const logText = (
                `<pre>
stderr:
${logs.stderr}
stdout:
${logs.stdout}
                </pre>`
              );
              tab?.document.write(logText as any);
              tab?.document.close(); // to finish loading the page
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
