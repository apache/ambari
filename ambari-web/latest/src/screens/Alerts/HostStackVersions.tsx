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

import { useContext, useState } from "react";
import { Alert, Badge, Button, Col, Container, Form, Row } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCog, faPowerOff, faQuestionCircle } from "@fortawesome/free-solid-svg-icons";
import { ColumnDef, SortingState } from "@tanstack/react-table";
import { get } from "lodash";
import VersionsApi from "../../api/versionsApi";
import ConfirmationModal from "../../components/ConfirmationModal";
import Paginator from "../../components/Paginator";
import Table from "../../components/Table";
import usePagination from "../../hooks/usePagination";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import IHost from "../../models/host";
import HostStackVersion, { IHostStackVersion } from "../../models/hostStackVersion";
import { AppContext } from "../../store/context";
import BackgroundOperations from "../BackgroundOperations";
import { translateWithVariables } from "../../Utils/Utility";
import { messages } from "../messages";

type HostStackVersionsProps = {
  host?: IHost;
};

type VersionRow = {
  key: string;
  version: IHostStackVersion;
  status: string;
};

type VersionColumn = ColumnDef<VersionRow, any> & { width?: string };

function versionKey(version: IHostStackVersion): string {
  return `${version.stack}:${version.version}:${version.repoVersion}`;
}

function requestIdFrom(response: any): string | number | null {
  return get(response, "Requests.id", get(response, "data.Requests.id", null));
}

const HostStackVersions = ({ host }: HostStackVersionsProps) => {
  const {
    backgroundOperations,
    clusterName,
  } = useContext(AppContext);
  const { havePermissions, isAuthorized } = useAuthorizationPolicy();
  const [stackFilter, setStackFilter] = useState("");
  const [nameFilter, setNameFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [sorting, setSorting] = useState<SortingState>([]);
  const [versionToInstall, setVersionToInstall] = useState<IHostStackVersion | null>(null);
  const [installingVersionKey, setInstallingVersionKey] = useState<string | null>(null);
  const [installError, setInstallError] = useState("");
  const [submittedRequestId, setSubmittedRequestId] = useState<string | number | null>(null);
  const [showProgress, setShowProgress] = useState(false);
  const [optimisticInstalling, setOptimisticInstalling] = useState<Set<string>>(new Set());

  const canViewStackVersionActions = havePermissions("AMBARI.MANAGE_STACK_VERSIONS");
  const canManageStackVersions = isAuthorized("AMBARI.MANAGE_STACK_VERSIONS");
  const visibleVersions = (host?.stackVersions || []).filter(
    (version) => version.isVisible !== false,
  );
  const rows: VersionRow[] = visibleVersions.map((version) => ({
    key: versionKey(version),
    version,
    status: optimisticInstalling.has(versionKey(version)) ? "INSTALLING" : version.status,
  }));
  const filteredRows = rows.filter((row) => (
    (!stackFilter || row.version.stack === stackFilter)
    && (!nameFilter || row.version.displayName === nameFilter)
    && (!statusFilter || row.status === statusFilter)
  ));
  const uniqueStacks = [...new Set(rows.map((row) => row.version.stack))];
  const uniqueNames = [...new Set(rows.map((row) => row.version.displayName))];
  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(filteredRows);

  const lastInstallRequestId = submittedRequestId || get(
    backgroundOperations.find((request: any) => (
      get(request, "Requests.request_context", "").startsWith("Install version")
    )),
    "Requests.id",
    null,
  );

  const installVersion = async () => {
    if (!versionToInstall || !host || installingVersionKey) {
      return;
    }

    const key = versionKey(versionToInstall);
    setInstallingVersionKey(key);
    setInstallError("");
    try {
      const response = await VersionsApi.installHostStackVersion(
        clusterName,
        host.hostName,
        versionToInstall,
      );
      const requestId = requestIdFrom(response);
      setOptimisticInstalling((current) => new Set(current).add(key));
      setSubmittedRequestId(requestId);
      setVersionToInstall(null);
      setShowProgress(requestId !== null);
    } catch (error: any) {
      setInstallError(
        error?.response?.data?.message || "Ambari could not start the version installation.",
      );
    } finally {
      setInstallingVersionKey(null);
    }
  };

  const columns: VersionColumn[] = [
    {
      header: "Stack",
      accessorFn: (row) => row.version.stack,
      id: "stack",
      width: "25%",
    },
    {
      header: "Name",
      accessorFn: (row) => row.version.displayName,
      id: "displayName",
      width: "25%",
    },
    {
      header: "Status",
      accessorKey: "status",
      width: "25%",
      cell: ({ row }) => {
        const { status } = row.original;
        if (status === "CURRENT") {
          return <Badge bg="success">Current</Badge>;
        }
        if (status === "INSTALLING") {
          return (
            <Button
              variant="link"
              className="p-0"
              disabled={!lastInstallRequestId}
              onClick={() => setShowProgress(true)}
            >
              <FontAwesomeIcon className="me-1" icon={faCog} spin />
              {HostStackVersion.formatStatus(status)}
            </Button>
          );
        }
        return (
          <>
            {HostStackVersion.formatStatus(status)}
            {status === "OUT_OF_SYNC" ? (
              <FontAwesomeIcon
                className="ms-2"
                icon={faQuestionCircle}
                title={get(messages, "hosts.host.stackVersions.outOfSync.tooltip", "")}
              />
            ) : null}
          </>
        );
      },
    },
    {
      header: "",
      id: "install",
      enableSorting: false,
      cell: ({ row }) => canViewStackVersionActions ? (
        <Button
          variant="secondary"
          disabled={
            !["OUT_OF_SYNC", "INSTALL_FAILED"].includes(row.original.status)
            || installingVersionKey !== null
            || !canManageStackVersions
          }
          onClick={() => {
            setInstallError("");
            setVersionToInstall(row.original.version);
          }}
        >
          <FontAwesomeIcon className="me-1" icon={faPowerOff} />
          Install
        </Button>
      ) : null,
    },
  ];

  return (
    <div className="mx-5">
      <Container className="p-4 bg-white">
        <h2 className="table-title pb-2">Versions</h2>
        <Form className="filter-container mb-3">
          <Row className="border-top border-bottom py-2 mx-1">
            <Col xs={4}>
              <Form.Select
                aria-label="Filter versions by stack"
                value={stackFilter}
                onChange={(event) => setStackFilter(event.target.value)}
              >
                <option value="">All Versions</option>
                {uniqueStacks.map((stack) => <option key={stack}>{stack}</option>)}
              </Form.Select>
            </Col>
            <Col xs={4}>
              <Form.Select
                aria-label="Filter versions by name"
                value={nameFilter}
                onChange={(event) => setNameFilter(event.target.value)}
              >
                <option value="">All Names</option>
                {uniqueNames.map((name) => <option key={name}>{name}</option>)}
              </Form.Select>
            </Col>
            <Col xs={4}>
              <Form.Select
                aria-label="Filter versions by status"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
              >
                <option value="">All Statuses</option>
                {HostStackVersion.statusDefinition.map((status) => (
                  <option key={status} value={status}>{HostStackVersion.formatStatus(status)}</option>
                ))}
              </Form.Select>
            </Col>
          </Row>
        </Form>
        <Table
          columns={columns as ColumnDef<unknown, unknown>[]}
          data={currentItems}
          hover
          entityName="stack versions"
          sorting={sorting}
          onSortingChange={setSorting}
        />
        {filteredRows.length ? (
          <Paginator
            currentPage={currentPage}
            maxPage={maxPage}
            changePage={changePage}
            itemsPerPage={itemsPerPage}
            setItemsPerPage={setItemsPerPage}
            totalItems={filteredRows.length}
          />
        ) : null}
      </Container>
      <ConfirmationModal
        isOpen={versionToInstall !== null}
        onClose={() => {
          if (!installingVersionKey) {
            setVersionToInstall(null);
            setInstallError("");
          }
        }}
        modalTitle="Confirm Installation"
        modalBody={(
          <>
            {versionToInstall ? translateWithVariables(
              "hosts.host.stackVersions.install.confirmation",
              { "0": versionToInstall.displayName },
            ) : null}
            {installError ? <Alert className="mt-3 mb-0" variant="danger">{installError}</Alert> : null}
          </>
        )}
        successCallback={() => void installVersion()}
        okButtonText={installError ? "Retry" : "Install"}
        isOkDisabled={installingVersionKey !== null}
      />
      {showProgress && lastInstallRequestId ? (
        <BackgroundOperations
          isOpen
          isExplicitClick
          requestId={lastInstallRequestId}
          onClose={() => setShowProgress(false)}
        />
      ) : null}
    </div>
  );
};

export default HostStackVersions;
