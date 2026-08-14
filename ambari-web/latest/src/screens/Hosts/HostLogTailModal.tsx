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
import { Alert, Button, Form, Modal, Stack } from "react-bootstrap";
import HostLogsApi from "../../api/hostLogsApi";
import Spinner from "../../components/Spinner";
import { AppContext } from "../../store/context";
import {
  HostLogEntry,
  hostLogsToText,
  mapHostLogEntries,
  mergeHostLogEntries,
  openTextInNewWindow,
} from "../../Utils/hostLogs";

type HostLogTailModalProps = {
  componentName: string;
  filePath: string;
  hostName: string;
  logSearchUrl?: string;
  onClose: () => void;
};

const POLL_INTERVAL = 2000;

export default function HostLogTailModal({
  componentName,
  filePath,
  hostName,
  logSearchUrl = "",
  onClose,
}: HostLogTailModalProps) {
  const { clusterName } = useContext(AppContext);
  const [rows, setRows] = useState<HostLogEntry[]>([]);
  const [tailCount, setTailCount] = useState(50);
  const [loading, setLoading] = useState(true);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [hasOlderRows, setHasOlderRows] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [copyStatus, setCopyStatus] = useState("");

  useEffect(() => {
    let active = true;
    let timeout: ReturnType<typeof setTimeout> | undefined;

    const poll = async () => {
      try {
        const response = await HostLogsApi.fetchLogTail(
          clusterName,
          componentName,
          hostName,
          tailCount,
          0,
        );
        if (!active) {
          return;
        }
        setRows((current) => mergeHostLogEntries(current, mapHostLogEntries(response)));
        setError(null);
      } catch (requestError: any) {
        if (active) {
          setError(
            requestError?.response?.data?.message || "Ambari could not load this log.",
          );
        }
      } finally {
        if (active) {
          setLoading(false);
          timeout = setTimeout(poll, POLL_INTERVAL);
        }
      }
    };

    setRows([]);
    setLoading(true);
    setHasOlderRows(true);
    void poll();
    return () => {
      active = false;
      if (timeout) {
        clearTimeout(timeout);
      }
    };
  }, [clusterName, componentName, hostName, retryCount, tailCount]);

  const loadOlder = async () => {
    setLoadingOlder(true);
    try {
      const response = await HostLogsApi.fetchLogTail(
        clusterName,
        componentName,
        hostName,
        tailCount,
        rows.length,
      );
      const olderRows = mapHostLogEntries(response);
      setRows((current) => mergeHostLogEntries(current, olderRows));
      setHasOlderRows(olderRows.length > 0);
      setError(null);
    } catch (requestError: any) {
      setError(
        requestError?.response?.data?.message || "Ambari could not load older log entries.",
      );
    } finally {
      setLoadingOlder(false);
    }
  };

  const text = hostLogsToText(rows);
  const copyLogs = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopyStatus("Copied");
    } catch {
      setCopyStatus("Copy failed");
    }
  };

  return (
    <Modal show onHide={onClose} size="xl" centered scrollable>
      <Modal.Header closeButton>
        <Modal.Title>{filePath.split("/").pop() || filePath}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Stack direction="horizontal" gap={3} className="mb-3 flex-wrap">
          <span><strong>File:</strong> {filePath}</span>
          <Form.Select
            aria-label="Tail count"
            className="w-auto ms-auto"
            value={tailCount}
            onChange={(event) => setTailCount(Number(event.target.value))}
          >
            {[50, 100, 200].map((count) => (
              <option key={count} value={count}>Tail {count}</option>
            ))}
          </Form.Select>
          <Button variant="outline-secondary" onClick={() => void copyLogs()}>
            Copy
          </Button>
          <Button variant="outline-secondary" onClick={() => openTextInNewWindow(text)}>
            Open
          </Button>
          {logSearchUrl ? (
            <Button
              as="a"
              href={logSearchUrl}
              target="_blank"
              rel="noopener noreferrer"
              variant="outline-secondary"
            >
              Open in Log Search
            </Button>
          ) : null}
          <span role="status" className="text-muted">{copyStatus}</span>
        </Stack>
        {error && (
          <Alert variant="danger">
            {error}{" "}
            <Button size="sm" variant="outline-danger" onClick={() => setRetryCount((value) => value + 1)}>
              Retry
            </Button>
          </Alert>
        )}
        {loading ? <Spinner /> : (
          <>
            <Button
              size="sm"
              variant="outline-secondary"
              className="mb-2"
              disabled={loadingOlder || !hasOlderRows}
              onClick={() => void loadOlder()}
            >
              {loadingOlder ? "Loading..." : hasOlderRows ? "Load older" : "No older entries"}
            </Button>
            <pre className="bg-dark text-light p-3 overflow-auto" style={{ maxHeight: "60vh" }}>
              {text || "No log entries were returned."}
            </pre>
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="success" onClick={onClose}>Dismiss</Button>
      </Modal.Footer>
    </Modal>
  );
}
