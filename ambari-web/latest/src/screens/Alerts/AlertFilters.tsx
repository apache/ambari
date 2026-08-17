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
import { Col, Form, Row } from "react-bootstrap";
import { ServiceContext } from "../../store/ServiceContext";
import { AlertStatus } from "./alertStatus";
import { MergedAlert } from "./types";

interface AlertFiltersProps {
  data: MergedAlert[];
  onFilter: (filteredData: MergedAlert[]) => void;
}

type ServiceOption = {
  name: string;
  displayName: string;
};

const AlertFilters = ({ data, onFilter }: AlertFiltersProps) => {
  const { allServiceModels } = useContext(ServiceContext);
  const [serviceFilter, setServiceFilter] = useState("");
  const [alertNameFilter, setAlertNameFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [responseFilter, setResponseFilter] = useState("");

  const services = useMemo<ServiceOption[]>(() => {
    const options = new Map<string, string>();
    Object.values(allServiceModels || {}).forEach((service: any) => {
      const name = String(service?.serviceName || "").toUpperCase();
      if (name) {
        options.set(name, service.displayName || name.replaceAll("_", " "));
      }
    });
    data.forEach((alert) => {
      const name = alert.serviceName || alert.serviceDisplayName;
      if (name) {
        options.set(name, alert.serviceDisplayName || name.replaceAll("_", " "));
      }
    });
    return [...options].map(([name, displayName]) => ({ name, displayName }));
  }, [allServiceModels, data]);

  const filteredData = useMemo(() => data.filter((item) => {
    const serviceName = item.serviceName || item.serviceDisplayName;
    const matchesService = !serviceFilter || serviceName === serviceFilter;
    const matchesAlertName = item.label?.toLowerCase().includes(alertNameFilter.toLowerCase()) || false;
    const matchesStatus = !statusFilter || item.statuses.some(
      (status) => status.status.toUpperCase() === statusFilter,
    );
    const matchesResponse = !responseFilter || (item.latest_text || "")
      .toLowerCase()
      .includes(responseFilter.toLowerCase());
    return matchesService && matchesAlertName && matchesStatus && matchesResponse;
  }), [alertNameFilter, data, responseFilter, serviceFilter, statusFilter]);

  useEffect(() => {
    onFilter(filteredData);
  }, [filteredData, onFilter]);

  return (
    <Form className="filter-container mb-3">
      <Row>
        <Col>
          <Form.Group controlId="hostAlertServiceFilter">
            <Form.Label>Service</Form.Label>
            <Form.Select
              value={serviceFilter}
              onChange={(event) => setServiceFilter(event.target.value)}
            >
              <option value="">All</option>
              {services.map((service) => (
                <option key={service.name} value={service.name}>{service.displayName}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col>
          <Form.Group controlId="hostAlertNameFilter">
            <Form.Label>Alert Definition Name</Form.Label>
            <Form.Control
              type="text"
              placeholder="Any"
              value={alertNameFilter}
              onChange={(event) => setAlertNameFilter(event.target.value)}
            />
          </Form.Group>
        </Col>
        <Col>
          <Form.Group controlId="hostAlertStatusFilter">
            <Form.Label>Status</Form.Label>
            <Form.Select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="">All</option>
              {Object.values(AlertStatus).map((status) => (
                <option key={status} value={status.toUpperCase()}>{status.toUpperCase()}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col>
          <Form.Group controlId="hostAlertResponseFilter">
            <Form.Label>Response</Form.Label>
            <Form.Control
              type="text"
              placeholder="Any"
              value={responseFilter}
              onChange={(event) => setResponseFilter(event.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>
    </Form>
  );
};

export default AlertFilters;
