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

import { useState, useEffect } from 'react';
import Spinner from '../../components/Spinner';
import { AlertsApi } from '../../api/alertsApi';
import Table from '../../components/Table';
import "../../styles/app.scss";
import { Button, Form } from 'react-bootstrap';
import Modal from '../../components/Modal';
import CopyButton from '../../components/CopyButton';
import Paginator from "../../components/Paginator";
import usePagination from '../../hooks/usePagination';
import { SortingState } from "@tanstack/react-table";
import { Link } from "react-router-dom";
import LastStatusChanged from "../../components/LastStatusChanged";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBriefcase } from '@fortawesome/free-solid-svg-icons';
import { formatStatus } from '../../Utils/Utility';
import {
    countAlertHistoryByHost,
    filterAndSortAlertInstances,
    openAlertResponseInNewWindow,
} from '../../Utils/alertDefinitions';

interface AlertInstancesTableProps {
    clusterName: string;
    alert_id: number | string;
    definitionName: string;
    refreshTrigger?: number;
    alertEnabled?: boolean;
}

interface AlertInstance {
    service_name: string;
    host_name: string;
    state: string;
    maintenance_state?: string;
    last_updated_time: number;
    text: string;
    [key: string]: any; // For any additional properties
}

interface FilterState {
    service: string;
    hostName: string;
    state: string;
}

const AlertInstancesTable = ({ clusterName, alert_id, definitionName, refreshTrigger, alertEnabled = true }: AlertInstancesTableProps) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const [instances, setInstances] = useState<AlertInstance[]>([]);
    const [filters, setFilters] = useState<FilterState>({
        service: '',
        hostName: '',
        state: '',
    });
    const [showModal, setShowModal] = useState(false);
    const [modalText, setModalText] = useState('');
    const [sorting, setSorting] = useState<SortingState>([]);
    const [availableServices, setAvailableServices] = useState<string[]>([]);
    const [historyCounts, setHistoryCounts] = useState<Record<string, number>>({});
    const [loadError, setLoadError] = useState('');
    const [historyError, setHistoryError] = useState('');
    const [refreshError, setRefreshError] = useState('');
    const [retryTrigger, setRetryTrigger] = useState(0);

    useEffect(() => {
        let active = true;
        let pollTimer: ReturnType<typeof setTimeout> | undefined;

        const applyInstances = (data: any) => {
            const instancesData = (data?.items || []).map(
                (alertObj: { Alert: AlertInstance }) => alertObj.Alert,
            );
            const services = [...new Set(instancesData.map(
                (instance: AlertInstance) => instance.service_name?.toString() || '',
            ))] as string[];
            setAvailableServices(services);
            setInstances(instancesData);
        };

        const fetchData = async () => {
            // If alert is disabled, don't fetch data and clear existing instances
            if (!alertEnabled) {
                setInstances([]);
                setAvailableServices([]);
                setHistoryCounts({});
                setHistoryError('');
                setIsLoaded(true);
                return;
            }
            
            setIsLoaded(false);
            setLoadError('');
            setHistoryError('');
            setRefreshError('');

            const now = Date.now();
            const [instancesResult, historyResult] = await Promise.allSettled([
                AlertsApi.getAlertInstancesByDefinition(clusterName, alert_id, now),
                AlertsApi.getAlertHistory(clusterName, definitionName, now - 24 * 60 * 60 * 1000),
            ]);
            if (!active) return;

            if (instancesResult.status === 'fulfilled') {
                applyInstances(instancesResult.value);
            } else {
                console.error("Error fetching alert instances:", instancesResult.reason);
                setInstances([]);
                setLoadError('Failed to load alert instances.');
            }

            if (historyResult.status === 'fulfilled') {
                setHistoryCounts(countAlertHistoryByHost(historyResult.value?.items || []));
            } else {
                console.error("Error fetching 24-hour alert history:", historyResult.reason);
                setHistoryCounts({});
                setHistoryError('24-hour alert history is temporarily unavailable.');
            }
            setIsLoaded(true);
        };

        const pollInstances = async () => {
            try {
                const data = await AlertsApi.getAlertInstancesByDefinition(clusterName, alert_id, Date.now());
                if (!active) return;
                applyInstances(data);
                setLoadError('');
                setRefreshError('');
                setIsLoaded(true);
            } catch (error) {
                if (active) setRefreshError('The latest instance refresh failed. Existing data is still displayed.');
            } finally {
                if (active) pollTimer = setTimeout(pollInstances, 30000);
            }
        };

        void fetchData().then(() => {
            if (active && alertEnabled) pollTimer = setTimeout(pollInstances, 30000);
        });
        return () => {
            active = false;
            if (pollTimer) clearTimeout(pollTimer);
        };
    }, [clusterName, alert_id, definitionName, refreshTrigger, alertEnabled, retryTrigger]);

    const filteredInstances = filterAndSortAlertInstances(
        instances,
        filters,
        sorting[0],
        historyCounts,
    );

    // Use pagination hook
    const {
        currentItems,
        changePage,
        currentPage,
        maxPage,
        itemsPerPage,
        setItemsPerPage
    } = usePagination(filteredInstances, 10);

    const handleFilterChange = (field: keyof FilterState, value: string) => {
        setFilters(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const columns = [
        {
            header: 'Service',
            accessorKey: 'service_name',
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                const serviceName = row.original.service_name;
                if (serviceName) {
                    // Link to the service page - following the same pattern as Ember
                    return (
                        <Link 
                            to={`/main/services/${serviceName}/summary`}
                            className="custom-link"
                        >
                            {serviceName}
                        </Link>
                    );
                }
                return serviceName;
            },
        },
        {
            header: 'Host',
            accessorKey: 'host_name',
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                const hostName = row.original.host_name;
                if (hostName) {
                    // Link to the host alerts page - following the same pattern as Ember
                    return (
                        <Link 
                            to={`/main/hosts/${hostName}/alerts`}
                            className="custom-link"
                        >
                            {hostName}
                        </Link>
                    );
                }
                return hostName;
            },
        },
        {
            header: 'Status',
            accessorKey: 'state',
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                const status = row.original.state;
                const maintenanceState = row.original.maintenance_state;
                const timestamp = row.original.original_timestamp || row.original.latest_timestamp;
                const isMaintenanceMode = maintenanceState === 'ON';

                const statusClassMap: { [key: string]: string } = {
                    critical: 'status-critical',
                    warning: 'status-warning',
                    ok: 'status-ok',
                    unknown: 'status-unknown',
                    none: 'status-none',
                };

                const getStatusClass = (status: string) =>
                    statusClassMap[status.toLowerCase()] || 'status-none';

                return (
                    <div className='d-flex'>
                        <Button
                            key={status}
                            className={`alert-item alert-status-box ${getStatusClass(status)} ${
                                isMaintenanceMode ? 'status-maintenance alert-maintenance' : ''
                            }`}
                        >
                            {isMaintenanceMode && (
                                <FontAwesomeIcon
                                    icon={faBriefcase}
                                    className="fs-12 me-2 text-white"
                                />
                            )}
                            {formatStatus(status, 1)}
                        </Button>
                        <div className="mt-1 ms-2">
                            <LastStatusChanged timestamp={timestamp || null} />
                        </div>
                    </div>
                );
            },
        },
        {
            header: '24-Hour',
            accessorKey: 'history_count',
            sortingFn: (rowA: { original: AlertInstance }, rowB: { original: AlertInstance }) =>
                (historyCounts[rowA.original.host_name] || 0) - (historyCounts[rowB.original.host_name] || 0),
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                return historyCounts[row.original.host_name] || 0;
            },
        },
        {
            header: 'Response',
            accessorKey: 'text',
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                const text = row.original.text || '';
                const maxLength = 100;
                const truncatedText =
                    text.length > maxLength ? text.substring(0, maxLength) + '...' : text;

                const handleClick = () => {
                    setModalText(text);
                    setShowModal(true);
                };

                return (
                    <span
                        onClick={handleClick}
                        style={{ cursor: 'pointer', color: 'blue', textDecoration: 'underline' }}
                    >
                        {truncatedText}
                    </span>
                );
            },
        },
    ];

    if (!isLoaded) {
        return <Spinner />;
    }

    if (loadError) {
        return (
            <div className="col-md-12">
                <div className="alert alert-danger">{loadError}</div>
                <Button variant="primary" onClick={() => setRetryTrigger((value) => value + 1)}>Retry</Button>
            </div>
        );
    }

    return (
        <div className="col-md-12">
            <h2 className="table-title mb-3">Instances</h2>

            {refreshError && (
                <div className="alert alert-warning d-flex justify-content-between align-items-center">
                    <span>{refreshError}</span>
                    <Button variant="outline-warning" onClick={() => setRetryTrigger((value) => value + 1)}>Retry</Button>
                </div>
            )}

            {historyError && (
                <div className="alert alert-warning d-flex justify-content-between align-items-center">
                    <span>{historyError}</span>
                    <Button variant="outline-warning" onClick={() => setRetryTrigger((value) => value + 1)}>Retry</Button>
                </div>
            )}

            {/* Filters - only show when alert is enabled */}
            {alertEnabled && (
                <div className="mb-3 d-flex">
                    <div style={{ width: '20%', marginRight: '15px' }}>
                        <Form.Group>
                            <Form.Label>Service</Form.Label>
                            <Form.Select 
                                value={filters.service} 
                                onChange={(e) => handleFilterChange('service', e.target.value)}
                                size="sm"
                            >
                                <option value="">All</option>
                                {availableServices.map((service, index) => (
                                    <option key={index} value={service}>{service}</option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </div>
                    <div style={{ width: '20%', marginRight: '15px' }}>
                        <Form.Group>
                            <Form.Label>Host</Form.Label>
                            <Form.Control 
                                type="text" 
                                placeholder="Any" 
                                value={filters.hostName}
                                onChange={(e) => handleFilterChange('hostName', e.target.value)}
                                size="sm"
                            />
                        </Form.Group>
                    </div>
                    <div style={{ width: '20%' }}>
                        <Form.Group>
                            <Form.Label>Status</Form.Label>
                            <Form.Select 
                                value={filters.state} 
                                onChange={(e) => handleFilterChange('state', e.target.value)}
                                size="sm"
                            >
                                <option value="">All</option>
                                <option value="OK">OK</option>
                                <option value="WARNING">Warning</option>
                                <option value="CRITICAL">Critical</option>
                                <option value="UNKNOWN">Unknown</option>
                                <option value="NONE">None</option>
                            </Form.Select>
                        </Form.Group>
                    </div>
                </div>
            )}

            {!alertEnabled ? (
                <div className="alert-no-instances bg-info-subtle p-4 text-center border rounded">
                    <p className="text-muted mb-0">
                        <i className="fa fa-info-circle me-2"></i>
                        This alert definition is disabled. No alert instances will be generated.
                    </p>
                </div>
            ) : filteredInstances.length === 0 ? (
                <div className="alert-no-instances bg-white p-4 text-center border rounded">
                    <p className="text-muted mb-0">No alerts to display</p>
                </div>
            ) : (
                <Table
                    columns={columns}
                    data={currentItems}
                    entityName="Alerts"
                    hover
                    scrollable
                    sorting={sorting}
                    onSortingChange={setSorting}
                />
            )}

            {filteredInstances.length > 0 && (
                <Paginator
                    currentPage={currentPage}
                    maxPage={maxPage}
                    changePage={changePage}
                    itemsPerPage={itemsPerPage}
                    setItemsPerPage={setItemsPerPage}
                    totalItems={filteredInstances.length}
                />
            )}

            {/* Modal for displaying full response text */}
            <Modal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                modalTitle="Instance Response"
                modalBody={
                    <div id="logs-popup">
                        <div className="controls-block pull-right">
                            <CopyButton textToCopy={modalText} buttonText="Copy" />
                            <a title="Open in New Window" onClick={() => {
                                openAlertResponseInNewWindow(modalText);
                            }} className="task-detail-open-dialog">
                                <i className="icon-external-link"></i> <span>Open</span>
                            </a>
                        </div>
                        <div className="clearfix"></div>
                        <div className="task-detail-log-info">
                            <div className="content-area">
                                <div className="task-detail-log-clipboard-wrap"></div>
                                <div className="task-detail-log-maintext">
                                    <pre>{modalText}</pre>
                                </div>
                            </div>
                        </div>
                    </div>
                }
                successCallback={() => setShowModal(false)}
                options={{
                    okButtonVariant: "primary",
                    cancelableViaIcon: true,
                }}
            />
        </div>
    );
};

export default AlertInstancesTable;
