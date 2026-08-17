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

import { useContext, useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Form, Table } from "react-bootstrap";
import HostLogsApi from "../../api/hostLogsApi";
import Paginator from "../../components/Paginator";
import Spinner from "../../components/Spinner";
import { ServiceContext } from "../../store/ServiceContext";
import { AppContext } from "../../store/context";
import {
  buildLogSearchUrl,
  HostLogRow,
  mapHostLogRows,
} from "../../Utils/hostLogs";
import HostLogTailModal from "./HostLogTailModal";
import { useLazyQuicklinks } from "../../hooks/useLazyQuicklinks";

type HostLogsProps = { hostName: string };
type SortField = "componentDisplayName" | "serviceDisplayName";

export default function HostLogs({ hostName }: HostLogsProps) {
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const { loadQuicklinks, quicklinks } = useLazyQuicklinks("LOGSEARCH");
  const [rows, setRows] = useState<HostLogRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const [serviceFilter, setServiceFilter] = useState("");
  const [componentFilter, setComponentFilter] = useState("");
  const [extensionFilter, setExtensionFilter] = useState("");
  const [sortField, setSortField] = useState<SortField>("serviceDisplayName");
  const [sortAscending, setSortAscending] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [selectedLog, setSelectedLog] = useState<{
    componentName: string;
    filePath: string;
    logSearchUrl: string;
  } | null>(null);

  useEffect(() => {
    void loadQuicklinks();
  }, [loadQuicklinks]);

  const logSearchBaseUrl = quicklinks
    .flatMap((quicklink: any) => quicklink.links || [])
    .find((link: any) => link.label === "Log Search UI")?.url || "";

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    const serviceNames = Object.fromEntries(
      (allServiceModels || []).map((service: any) => [
        service.serviceName || service.ServiceInfo?.service_name,
        service.displayName || service.ServiceInfo?.service_name,
      ]),
    );
    void HostLogsApi.fetchHostLogs(clusterName, hostName)
      .then((response) => {
        if (active) {
          setRows(mapHostLogRows(response, hostName, serviceNames));
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError?.response?.data?.message || "Ambari could not load host logs.",
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [allServiceModels, clusterName, hostName, retryCount]);

  const serviceOptions = useMemo(
    () => Array.from(new Map(rows.map((row) => [row.serviceName, row.serviceDisplayName])).entries()),
    [rows],
  );
  const componentOptions = useMemo(
    () => Array.from(new Map(rows.map((row) => [row.componentName, row.componentDisplayName])).entries()),
    [rows],
  );
  const filteredRows = useMemo(() => rows
    .map((row) => ({
      ...row,
      files: row.files.filter((file) => !extensionFilter || file.filePath.endsWith(extensionFilter)),
    }))
    .filter((row) =>
      (!serviceFilter || row.serviceName === serviceFilter)
      && (!componentFilter || row.componentName === componentFilter)
      && (!extensionFilter || row.files.length > 0),
    )
    .sort((left, right) => {
      const result = left[sortField].localeCompare(right[sortField]);
      return sortAscending ? result : -result;
    }), [componentFilter, extensionFilter, rows, serviceFilter, sortAscending, sortField]);

  useEffect(() => setCurrentPage(1), [componentFilter, extensionFilter, serviceFilter]);
  const maxPage = Math.max(1, Math.ceil(filteredRows.length / itemsPerPage));
  const page = Math.min(currentPage, maxPage);
  const pageRows = filteredRows.slice((page - 1) * itemsPerPage, page * itemsPerPage);
  const changeSort = (field: SortField) => {
    if (sortField === field) {
      setSortAscending((value) => !value);
    } else {
      setSortField(field);
      setSortAscending(true);
    }
  };

  if (loading) {
    return <Spinner />;
  }

  return (
    <Card className="p-3 rounded-0">
      {selectedLog && (
        <HostLogTailModal
          componentName={selectedLog.componentName}
          filePath={selectedLog.filePath}
          hostName={hostName}
          logSearchUrl={selectedLog.logSearchUrl}
          onClose={() => setSelectedLog(null)}
        />
      )}
      <h2>Logs</h2>
      {error && (
        <Alert variant="danger">
          {error}{" "}
          <Button size="sm" variant="outline-danger" onClick={() => setRetryCount((value) => value + 1)}>
            Retry
          </Button>
        </Alert>
      )}
      <div className="d-flex gap-2 mb-3 flex-wrap">
        <Form.Select aria-label="Filter logs by service" className="w-auto" value={serviceFilter} onChange={(event) => setServiceFilter(event.target.value)}>
          <option value="">All services</option>
          {serviceOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </Form.Select>
        <Form.Select aria-label="Filter logs by component" className="w-auto" value={componentFilter} onChange={(event) => setComponentFilter(event.target.value)}>
          <option value="">All components</option>
          {componentOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </Form.Select>
        <Form.Select aria-label="Filter logs by extension" className="w-auto" value={extensionFilter} onChange={(event) => setExtensionFilter(event.target.value)}>
          <option value="">All extensions</option>
          <option value=".log">.log</option>
          <option value=".out">.out</option>
        </Form.Select>
      </div>
      <Table hover responsive>
        <thead>
          <tr>
            <th><Button variant="link" onClick={() => changeSort("serviceDisplayName")}>Service</Button></th>
            <th><Button variant="link" onClick={() => changeSort("componentDisplayName")}>Component</Button></th>
            <th>Files</th>
          </tr>
        </thead>
        <tbody>
          {pageRows.map((row) => (
            <tr key={row.id}>
              <td>{row.serviceDisplayName}</td>
              <td>{row.componentDisplayName}</td>
              <td>
                {row.files.map((file) => (
                  <div key={file.filePath}>
                    <Button
                      variant="link"
                      title={file.filePath}
                      onClick={() => setSelectedLog({
                        componentName: row.logComponentName,
                        filePath: file.filePath,
                        logSearchUrl: buildLogSearchUrl(
                          logSearchBaseUrl,
                          hostName,
                          row.logComponentName,
                          file.filePath,
                        ),
                      })}
                    >
                      {file.fileName}
                    </Button>
                    {logSearchBaseUrl ? (
                      <a
                        className="ms-2"
                        href={buildLogSearchUrl(
                          logSearchBaseUrl,
                          hostName,
                          row.logComponentName,
                          file.filePath,
                        )}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Open in Log Search
                      </a>
                    ) : null}
                  </div>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      {!error && filteredRows.length === 0 && <p className="text-muted">No logs match the current filters.</p>}
      <Paginator
        currentPage={page}
        maxPage={maxPage}
        changePage={(newPage) => setCurrentPage(Math.min(maxPage, Math.max(1, newPage)))}
        itemsPerPage={itemsPerPage}
        setItemsPerPage={(value) => {
          setItemsPerPage(value);
          setCurrentPage(1);
        }}
        totalItems={filteredRows.length}
      />
    </Card>
  );
}
