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
import { isWithin24Hours } from '../../Utils/Utility';
import Paginator from "../../components/Paginator";
import usePagination from '../../hooks/usePagination';
import { SortingState } from "@tanstack/react-table";
import { Link } from "react-router-dom";
import LastStatusChanged from "../../components/LastStatusChanged";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBriefcase } from '@fortawesome/free-solid-svg-icons';
import { formatStatus } from '../../Utils/Utility';

interface AlertInstancesTableProps {
    clusterName: string;
    alert_id: number | string;
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

const AlertInstancesTable = ({ clusterName, alert_id, refreshTrigger, alertEnabled = true }: AlertInstancesTableProps) => {
    const [isLoaded, setIsLoaded] = useState(false);
    const [instances, setInstances] = useState<AlertInstance[]>([]);
    const [filteredInstances, setFilteredInstances] = useState<AlertInstance[]>([]);
    const [filters, setFilters] = useState<FilterState>({
        service: '',
        hostName: '',
        state: '',
    });
    const [showModal, setShowModal] = useState(false);
    const [modalText, setModalText] = useState('');
    const [sorting, setSorting] = useState<SortingState>([]);
    const [availableServices, setAvailableServices] = useState<string[]>([]);
    //@ts-ignore
    const copyToClipboard = async (text: string) => {
        try {
            // Check if clipboard API is available (HTTPS required)
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
                return;
            }
            
            // Fallback for HTTP or older browsers
            const textArea = document.createElement('textarea');
            textArea.value = text;
            textArea.style.position = 'fixed';
            textArea.style.left = '-999999px';
            textArea.style.top = '-999999px';
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();
            
            try {
                document.execCommand('copy');
            } catch (fallbackErr) {
                console.error("Fallback copy failed: ", fallbackErr);
                // Show user a message to manually copy
                alert(`Please copy this manually: ${text}`);
            }
            
            document.body.removeChild(textArea);
        } catch (err) {
            console.error('Failed to copy:', err);
            // Final fallback - show text for manual copy
            alert(`Please copy this manually: ${text}`);
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            // If alert is disabled, don't fetch data and clear existing instances
            if (!alertEnabled) {
                setInstances([]);
                setFilteredInstances([]);
                setAvailableServices([]);
                setIsLoaded(true);
                return;
            }
            
            setIsLoaded(false);
            
            try {
                const time = new Date().getTime();
                // FIXED: Get all alert instances including maintenance mode ones
                const data = await AlertsApi.getAlertsList(clusterName, time);
                // Filter by the specific alert definition ID
                const allInstances = data.items.map((alertObj: { Alert: AlertInstance }) => alertObj.Alert);
                const instancesData = allInstances.filter((instance: AlertInstance) => 
                    instance.definition_id === parseInt(String(alert_id))
                );

                // Extract unique service names for the service filter
                const services = [...new Set(instancesData.map((instance: { service_name: { toString: () => any; }; }) => instance.service_name?.toString() || ''))] as string[];
                setAvailableServices(services);

                setInstances(instancesData);
                setFilteredInstances(instancesData);
                setIsLoaded(true);
            } catch (error) {
                console.error("Error fetching alert instances:", error);
                setIsLoaded(true);
                setInstances([]);
                setFilteredInstances([]);
            }
        };

        fetchData();
    }, [clusterName, alert_id, refreshTrigger, alertEnabled]);

    useEffect(() => {
        let filtered = instances;
        if (filters.service && filters.service !== 'All') {
            filtered = filtered.filter(
                (instance) => instance.service_name === filters.service
            );
        }
        if (filters.hostName) {
            filtered = filtered.filter(
                (instance) => instance.host_name.toLowerCase().includes(filters.hostName.toLowerCase())
            );
        }
        if (filters.state && filters.state !== 'All') {
            filtered = filtered.filter((instance) => instance.state.toLowerCase() === filters.state.toLowerCase());
        }
        setFilteredInstances(filtered);
    }, [instances, filters]);

    // Apply sorting
    useEffect(() => {
        if (sorting.length === 0) return;

        const sortedData = [...filteredInstances].sort((a, b) => {
            const sortField = sorting[0].id;
            const isAsc = sorting[0].desc === false;
            
            if (sortField === 'last_updated_time') {
                const aValue = a.last_updated_time || 0;
                const bValue = b.last_updated_time || 0;
                return isAsc ? aValue - bValue : bValue - aValue;
            }
            
            const aValue = a[sortField] || '';
            const bValue = b[sortField] || '';
            
            if (typeof aValue === 'string' && typeof bValue === 'string') {
                return isAsc 
                    ? aValue.localeCompare(bValue) 
                    : bValue.localeCompare(aValue);
            }
            
            return isAsc 
                ? (aValue > bValue ? 1 : -1) 
                : (bValue > aValue ? 1 : -1);
        });
        
        setFilteredInstances(sortedData);
    }, [sorting]);

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
            accessorKey: 'last_updated_time',
            cell: ({ row }: { row: { original: AlertInstance } }) => {
                const lastUpdatedTime = row.original.last_updated_time;
                return isWithin24Hours(lastUpdatedTime);
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

    return (
        <div className="col-md-12">
            <h2 className="table-title mb-3">Instances</h2>

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
                                const newWindow = window.open();
                                if (newWindow) {
                                    newWindow.document.write(`<pre>${modalText}</pre>`);
                                }
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
