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

import { Row as TableRow } from "@tanstack/react-table";
import { Alert, Button, Col, Container, Row } from "react-bootstrap";
import Table from "../../components/Table";
import Spinner from "../../components/Spinner";
import viewIcon from "../../assets/img/ambari-view-default.png";
import { openViewInstance, ViewInstance } from "../../Utils/viewUtils";
import { useViewInstances } from "./ViewInstancesContext";

export default function ViewsListPage() {
  const { error, instances, isLoading, reload } = useViewInstances();

  if (isLoading) {
    return <div className="p-5"><Spinner /></div>;
  }

  const columns = [
    {
      accessorKey: "iconPath",
      header: "",
      cell: ({ row }: { row: TableRow<ViewInstance> }) => (
        <div className="d-flex justify-content-center align-items-center" style={{ width: 60 }}>
          <img
            src={row.original.iconPath || viewIcon}
            alt=""
            style={{ height: 40, objectFit: "contain", width: 40 }}
          />
        </div>
      ),
      width: "60px",
    },
    {
      accessorKey: "label",
      header: "",
      cell: ({ row }: { row: TableRow<ViewInstance> }) => {
        const instance = row.original;
        return (
          <button
            type="button"
            className="btn btn-link p-0 text-start text-decoration-none"
            onClick={() => openViewInstance(instance)}
          >
            <span className="h4 d-block mb-1 text-body">
              {instance.label}
              <small className="text-muted"> &nbsp;&nbsp;({instance.version})</small>
            </span>
            <span className="text-muted">{instance.description}</span>
          </button>
        );
      },
    },
  ];

  return (
    <Container fluid className="p-4" id="views">
      <Row className="mb-4">
        <Col><h2>Your Views</h2></Col>
      </Row>
      {error ? (
        <Alert variant="danger">
          <Alert.Heading>Unable to load Views</Alert.Heading>
          <p>{error}</p>
          <Button variant="outline-danger" onClick={() => void reload()}>Retry</Button>
        </Alert>
      ) : instances.length ? (
        <Row>
          <Col>
            <Table
              columns={columns}
              data={instances}
              hover
              className="views-table"
              scrollable={false}
            />
          </Col>
        </Row>
      ) : (
        <p className="text-muted">No views</p>
      )}
    </Container>
  );
}
