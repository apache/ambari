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

import { useState, useEffect, useMemo, useContext } from 'react';
import { Form, Row, Col, Dropdown } from 'react-bootstrap';
import { MergedAlert } from './types';
import { AlertStatus } from "./alertStatus.ts";
import { ServiceApi } from '../../api/ServiceApi';
import { AppContext } from '../../store/context';

interface AlertFiltersProps {
    data: MergedAlert[];
    onFilter: (filteredData: MergedAlert[]) => void;
}

interface AlertItem {
    serviceDisplayName: string;
    label: string;
    statuses: Array<{ status: string; count: number }>;
    latest_text?: string;
}

const formatServiceName = (name: string): string => {
    return name.replace(/_/g, ' ');
};

const AlertFilters = ({ data, onFilter }: AlertFiltersProps) => {
    const { clusterName } = useContext(AppContext);
    const [serviceFilter, setServiceFilter] = useState<string>('');
    const [alertNameFilter, setAlertNameFilter] = useState<string>('');
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [responseFilter, setResponseFilter] = useState<string>('');
    const [services, setServices] = useState<string[]>([]);

    const uniqueStatuses = Object.values(AlertStatus).map(status => status.toUpperCase());

    useEffect(() => {
        const fetchServices = async () => {
            try {
                const response = await ServiceApi.getAllServices(clusterName);
                const serviceNames = response.items.map(item => item.ServiceInfo.service_name);
                setServices(['All', ...serviceNames]);
            } catch (error) {
                console.error('Error fetching services:', error);
                setServices(['All']);
            }
        };
        fetchServices();
    }, [clusterName]);

    const filteredData = useMemo(() => {
        return data.filter((item: AlertItem) => {
            const matchesService = serviceFilter === '' || item.serviceDisplayName === serviceFilter;
            const matchesAlertName = item.label?.toLowerCase().includes(alertNameFilter.toLowerCase()) || false;
            const matchesStatus = statusFilter === '' || item.statuses.some(status => status.status.toUpperCase() === statusFilter);
            const matchesResponse = !responseFilter || (item.latest_text?.toLowerCase() || '').includes(responseFilter.toLowerCase());
            return matchesService && matchesAlertName && matchesStatus && matchesResponse;
        });
    }, [serviceFilter, alertNameFilter, statusFilter, responseFilter, data]);

    useEffect(() => {
        onFilter(filteredData);
    }, [filteredData, onFilter]);

    return (
        <Form className="filter-container mb-3">
            <Row>
                <Col>
                    <Form.Group controlId="serviceFilter">
                        <Form.Label>Service</Form.Label>
                        <Dropdown>
                            <Dropdown.Toggle variant="default" id="dropdown-basic">
                                {serviceFilter ? formatServiceName(serviceFilter) : "All"}
                            </Dropdown.Toggle>

                            <Dropdown.Menu>
                                {services.map((service, index) => (
                                    <Dropdown.Item 
                                        key={index} 
                                        onClick={() => setServiceFilter(service === 'All' ? '' : service)}
                                    >
                                        {service === 'All' ? service : formatServiceName(service)}
                                    </Dropdown.Item>
                                ))}
                            </Dropdown.Menu>
                        </Dropdown>
                    </Form.Group>
                </Col>
                <Col>
                    <Form.Group controlId="alertNameFilter">
                        <Form.Label>Alert Definition Name</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Any"
                            value={alertNameFilter}
                            onChange={(e) => setAlertNameFilter(e.target.value)}
                        />
                    </Form.Group>
                </Col>
                <Col>
                    <Form.Group controlId="statusFilter">
                        <Form.Label>Status</Form.Label>
                        <Dropdown>
                            <Dropdown.Toggle variant="default" id="dropdown-basic">
                                {statusFilter || "All"}
                            </Dropdown.Toggle>

                            <Dropdown.Menu>
                                <Dropdown.Item onClick={() => setStatusFilter('')}>All</Dropdown.Item>
                                {uniqueStatuses.map((status, index) => (
                                    <Dropdown.Item key={index} onClick={() => setStatusFilter(status)}>
                                        {status}
                                    </Dropdown.Item>
                                ))}
                            </Dropdown.Menu>
                        </Dropdown>
                    </Form.Group>
                </Col>
                <Col>
                    <Form.Group controlId="responseFilter">
                        <Form.Label>Response</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Any"
                            value={responseFilter}
                            onChange={(e) => setResponseFilter(e.target.value)}
                        />
                    </Form.Group>
                </Col>
            </Row>
        </Form>
    );
};

export default AlertFilters;