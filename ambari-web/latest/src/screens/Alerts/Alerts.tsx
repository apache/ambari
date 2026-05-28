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

import Table from '../../components/Table';
import LastStatusChanged from "../../components/LastStatusChanged";
import Paginator from "../../components/Paginator";
import usePagination from '../../hooks/usePagination';
import Spinner from "../../components/Spinner";
import {useEffect, useState, useContext} from 'react';
import {AlertsApi} from "../../api/alertsApi";
import {getCurrTimeInSec} from "../../Utils/Utility";
import {AlertDefinition, AlertGroupItem, AlertRow, MergedAlert} from "./types";
import {Container} from 'react-bootstrap';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPowerOff, faBriefcase} from '@fortawesome/free-solid-svg-icons';
import Modal from '../../components/Modal';
import {AppContext} from '../../store/context';
import { processData, filterAlerts, sortAlerts } from './alertUtils';
import '../../styles/app.scss';
import {Link} from "react-router-dom";
import {SortingState} from "@tanstack/react-table";
import MenuBar from './MenuBar'
import {formatAlertStatusDisplay} from "./alertStatus";
import { useAuth } from '../../hooks/useAuth';

interface SearchFilter {
    category: string;
    value: string;
}

const Alerts = () => {
    const { clusterName, upgradeIsRunning, upgradeSuspended } = useContext(AppContext);
    const [alerts, setAlerts] = useState<MergedAlert[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [sorting, setSorting] = useState<SortingState>([
        {
            id: 'statuses',
            desc: true // Sort in descending order to show critical alerts first
        }
    ]);
    const [alertGroups, setAlertGroups] = useState<AlertGroupItem[]>([]);
    const [alertDefinitions, setAlertDefinitions] = useState<AlertDefinition[]>([]);
    const [searchFilters, setSearchFilters] = useState<SearchFilter[]>([]);
    const [filteredAlerts, setFilteredAlerts] = useState<MergedAlert[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    
    // Authorization hooks - implementing Ember.js alert authorization patterns
    const { hasAuthorization } = useAuth();
    
    // Check specific authorizations for alert operations
    const canToggleAlerts = hasAuthorization('SERVICE.TOGGLE_ALERTS');
    const canManageNotifications = hasAuthorization('CLUSTER.MANAGE_ALERT_NOTIFICATIONS');
    
    // Check if user has any alert management permissions
    const hasAnyAlertPermissions = canToggleAlerts || canManageNotifications;
    
    // Check if upgrade is blocking operations (running but not suspended)
    const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

    const countAlertsByService = (alertGroups: AlertGroupItem[]) => {
        if (!alertGroups || !Array.isArray(alertGroups) || alertGroups.length === 0) {
            console.log('No alert groups available for counting');
            return {};
        }

        const serviceCounts: Record<string, number> = {};

        alertGroups.forEach(group => {
            if (!group?.AlertGroup?.definitions) {
                console.warn('Invalid alert group structure:', group);
                return;
            }

            const serviceName = group.AlertGroup.name + (group.AlertGroup.default ? " Default" : "");
            group.AlertGroup.definitions.forEach(() => {
                if (serviceCounts[serviceName]) {
                    serviceCounts[serviceName]++;
                } else {
                    serviceCounts[serviceName] = 1;
                }
            });
        });

        return serviceCounts;
    };

    const fetchData = async () => {
        if (!clusterName) {
            console.error('No cluster name provided');
            return;
        }

        const currTime = getCurrTimeInSec();
        try {
            console.log('Fetching alerts for cluster:', clusterName);
            const [alertsResponse, summariesResponse] = await Promise.all([
                AlertsApi.getAlerts(
                    clusterName,
                    'AlertGroup/default,AlertGroup/definitions,AlertGroup/id,AlertGroup/name,AlertGroup/targets',
                    currTime
                ),
                AlertsApi.getGroupFormattedAlertsNotifications(clusterName, currTime),
            ]);

            console.log('Alert Groups Response:', alertsResponse);
            console.log('Alert Summary Response:', summariesResponse);

            if (!alertsResponse || !alertsResponse.items) {
                console.error('Invalid alert groups response:', alertsResponse);
                return;
            }

            const processedAlerts = processData(alertsResponse, summariesResponse);
            setAlerts(processedAlerts);
            const alertGroups = alertsResponse.items;
            console.log('Setting alert groups:', alertGroups);
            setAlertGroups(alertGroups);
        } catch (error) {
            console.error('Error fetching data:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        console.log('Initial render with clusterName:', clusterName);
        if (clusterName) {
            fetchData();
            fetchAlertDefinitions();

            // Only set up polling if modal is not open
            if (!isModalOpen) {
                const pollInterval = setInterval(() => {
                    fetchData();
                }, 30000); // 30 seconds

                return () => clearInterval(pollInterval);
            }
        } else {
            console.error('No cluster name available');
        }
    }, [clusterName, isModalOpen]);

    const fetchAlertDefinitions = async () => {
        try {
            if (!clusterName) return;
            const response = await AlertsApi.getAlertDefinition(
                clusterName,
                'AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/id,AlertDefinition/label,AlertDefinition/name,AlertDefinition/service_name',
                Date.now()
            );
            if (response && response.items) {
                const definitions = response.items.map((item: any) => ({
                    ...item.AlertDefinition,
                    label: item.AlertDefinition.label || item.AlertDefinition.name,
                    component_name: item.AlertDefinition.component_name || 'N/A'
                }));
                console.log('Fetched alert definitions:', definitions);
                setAlertDefinitions(definitions);
            } else {
                console.error('No alert definitions found in response:', response);
            }
        } catch (error) {
            console.error('Error fetching alert definitions:', error);
        }
    };

    // Fetch alert groups when needed

    const handleSearch = (filters: SearchFilter[]) => {
        setSearchFilters(filters);
    };

    const alertCounts = countAlertsByService(alertGroups);

    function formatStatus(status: string, count: number) {
        return formatAlertStatusDisplay(status, count);
    }

    // Helper to get status priority (CRITICAL=4, WARNING=3, OK=2, UNKNOWN=1, NONE=0)
    const getStatusPriority = (status: string): number => {
        const statusOrder = ["NONE", "UNKNOWN", "OK", "WARNING", "CRITICAL"];
        return statusOrder.indexOf(status.toUpperCase());
    };

    // Get sorting score for a status combination
    const getStatusScore = (statuses: any[]): number => {
        if (!statuses.length) return -1; // NONE case

        // Sort statuses by priority
        const sortedStatuses = [...statuses].sort((a, b) => {
            return getStatusPriority(b.status) - getStatusPriority(a.status);
        });

        const highestPriority = getStatusPriority(sortedStatuses[0].status);
        const secondPriority = sortedStatuses.length > 1 ? getStatusPriority(sortedStatuses[1].status) : -1;

        // Base score from highest priority (multiplied by 10 to leave room for combinations)
        let score = highestPriority * 10;

        // Pure status (single status) gets highest score within its priority group
        if (sortedStatuses.length === 1) {
            score += 9;
        } else {
            // Add points based on second status (lower priority combinations get lower scores)
            score += secondPriority;
        }

        return score;
    };

    // Define the AlertDefinitionState component for the State column
    const AlertDefinitionState = ({ alert }: { alert: { enabled: boolean, id?: number } }) => {
        const [isEnabled, setIsEnabled] = useState(alert.enabled);
        const [showModal, setShowModal] = useState(false);
        const [isUpdating, setIsUpdating] = useState(false);

        const handleToggleState = () => {
            setShowModal(true);
        };

        const handleConfirmToggle = async () => {
            if (!alert.id) {
                console.error('Alert ID is missing');
                setShowModal(false);
                return;
            }
            
            setIsUpdating(true);
            try {
                const newState = !isEnabled;
                await AlertsApi.updateAlertDefinitionState(clusterName, alert.id, newState);
                setIsEnabled(newState);
                // Refresh the alerts list
                fetchData();
                fetchAlertDefinitions();
            } catch (error) {
                console.error('Error updating alert state:', error);
            } finally {
                setIsUpdating(false);
                setShowModal(false);
            }
        };

        const handleCancelToggle = () => {
            setShowModal(false);
        };

        return (
            <>
                {/* Show enabled status for all users, but only make it clickable for authorized users and when not upgrading */}
                {canToggleAlerts && !isUpgradeBlocking ? (
                    <div className="custom-link" onClick={handleToggleState}>
                        <FontAwesomeIcon className={'mx-1'} icon={faPowerOff} />
                        {isEnabled ? 'Enabled' : 'Disabled'}
                        {isUpdating && <span className="ms-2">Updating...</span>}
                    </div>
                ) : (
                    <div>
                        <FontAwesomeIcon className={'mx-1'} icon={faPowerOff} />
                        {isEnabled ? 'Enabled' : 'Disabled'}
                    </div>
                )}
                <Modal
                    isOpen={showModal}
                    onClose={handleCancelToggle}
                    modalTitle="Confirmation"
                    modalBody={isEnabled ? "You are about to Disable this alert definition" : "You are about to Enable this alert definition"}
                    successCallback={handleConfirmToggle}
                    options={{
                        okButtonText: isEnabled ? "Confirm Disable" : "Confirm Enable",
                        cancelButtonText: "Cancel",
                    }}
                />
            </>
        );
    };

    // Define columns before the useEffect to ensure they're available for sorting
    const baseColumns = [
        {
            header: 'Status',
            accessorKey: 'statuses',
            sortingFn: (rowA: any, rowB: any) => {
                try {
                    const aStatuses = rowA.original.statuses || [];
                    const bStatuses = rowB.original.statuses || [];

                    // Empty statuses go to the end
                    if (!aStatuses.length && !bStatuses.length) return 0;
                    if (!aStatuses.length) return -1; // NONE goes to bottom
                    if (!bStatuses.length) return 1; // NONE goes to bottom

                    // Get scores based on status combinations
                    const aScore = getStatusScore(aStatuses);
                    const bScore = getStatusScore(bStatuses);

                    // Higher score means higher priority
                    if (aScore !== bScore) {
                        return aScore - bScore;
                    }

                    // If scores are equal, compare counts of highest priority status
                    return aStatuses[0].count - bStatuses[0].count;
                } catch (error) {
                    console.error('Error in status sorting:', error, rowA, rowB);
                    return 0;
                }
            },
            cell: ({ row }: { row: AlertRow }) => {
                const statuses = row.original.statuses;

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
                    <div className="status-container">
                        {statuses.length > 0 ? (
                            statuses.map((statusItem: any, index: any) => (
                                <div key={statusItem.status} className="status-row d-flex align-items-center">
                                    {/* Show maintenance mode alerts with grayed-out styling and maintenance icon */}
                                    {statusItem.maintenance_count > 0 ? (
                                        <button
                                            key={`${statusItem.status}-maintenance`}
                                            className={`alert-item alert-status-box ${getStatusClass(
                                                statusItem.status
                                            )} status-maintenance alert-maintenance ${index > 0 ? "mt-1" : ""}`}
                                        >
                                            <FontAwesomeIcon
                                                icon={faBriefcase}
                                                className="fs-12 me-2 text-white"
                                            />
                                            {formatStatus(statusItem.status, statusItem.maintenance_count)}
                                        </button>
                                    ) : null}
                                    {/* Show regular alerts with normal styling */}
                                    {statusItem.count > 0 ? (
                                        <button
                                            key={`${statusItem.status}-normal`}
                                            className={`alert-item alert-status-box ${getStatusClass(
                                                statusItem.status
                                            )} ${index > 0 || statusItem.maintenance_count > 0 ? "mt-1" : ""}`}
                                        >
                                            {formatStatus(statusItem.status, statusItem.count)}
                                        </button>
                                    ) : null}
                                </div>
                            ))
                        ) : (
                            <button className="alert-item alert-status-box status-none">
                                None
                            </button>
                        )}
                    </div>
                );
            },
        },
        {
            header: 'Alert Definition Name',
            accessorKey: 'label',
            sortingFn: (rowA: any, rowB: any) => {
                const aLabel = rowA.original.label || '';
                const bLabel = rowB.original.label || '';
                return aLabel.localeCompare(bLabel);
            },
            cell: ({ row }: { row: AlertRow }) => (
                <Link
                    to={`/main/alerts/${row.original.alert_definition_id}`}
                    className="custom-link"
                >
                    {row.original.label}
                </Link>
            )
        },
        {
            header: 'Service',
            accessorKey: 'serviceDisplayName',
            sortingFn: (rowA: any, rowB: any) => {
                const aService = rowA.original.serviceDisplayName || '';
                const bService = rowB.original.serviceDisplayName || '';
                return aService.localeCompare(bService);
            }
        },
        {
            header: 'Last Status Changed',
            accessorKey: 'last_status_changed',
            sortingFn: (rowA: any, rowB: any) => {
                const parseTimestamp = (timestamp: string | number | undefined): number => {
                    if (!timestamp || timestamp === 'Unknown') return 0;

                    try {
                        // If it's already a number (unix timestamp), use it directly
                        if (typeof timestamp === 'number') {
                            // Check if it's in seconds (10 digits) or milliseconds (13 digits)
                            const timestampMs = timestamp.toString().length <= 10 ? timestamp * 1000 : timestamp;
                            return timestampMs;
                        }

                        // If it's a string, try different parsing methods
                        if (typeof timestamp === 'string') {
                            // First try parsing as a standard date string
                            let date = new Date(timestamp);
                            if (!isNaN(date.getTime())) {
                                return date.getTime();
                            }

                            // Try parsing as "DD/MM/YYYY, HH:mm:ss" format
                            if (timestamp.includes(', ')) {
                                const [datePart, timePart] = timestamp.split(', ');
                                if (datePart && timePart) {
                                    const [day, month, year] = datePart.split('/').map(Number);
                                    const [hours, minutes, seconds] = timePart.split(':').map(Number);

                                    // Validate all parts are numbers
                                    if (![day, month, year, hours, minutes, seconds].some(isNaN)) {
                                        date = new Date(Date.UTC(year, month - 1, day, hours, minutes, seconds));
                                        if (!isNaN(date.getTime())) {
                                            return date.getTime();
                                        }
                                    }
                                }
                            }

                            // Try parsing as unix timestamp string
                            const numericTimestamp = parseInt(timestamp, 10);
                            if (!isNaN(numericTimestamp)) {
                                const timestampMs = numericTimestamp.toString().length <= 10 ? numericTimestamp * 1000 : numericTimestamp;
                                return timestampMs;
                            }
                        }

                        return 0;
                    } catch (error) {
                        console.error('Error parsing timestamp:', timestamp, error);
                        return 0;
                    }
                };

                const aTime = parseTimestamp(rowA.original.last_status_changed);
                const bTime = parseTimestamp(rowB.original.last_status_changed);

                // Handle empty values
                if (aTime === 0 && bTime === 0) return 0;
                if (aTime === 0) return 1;  // Empty values go to the end for ascending
                if (bTime === 0) return -1; // Empty values go to the end for ascending

                // For non-empty values, sort by timestamp (descending: recent to old)
                return bTime - aTime;  // Reversed so newer dates appear first by default
            },
            cell: ({ row }: { row: AlertRow }) => {
                const { last_status_changed, lastTriggeredRaw } = row.original;
                return <LastStatusChanged 
                    timestamp={last_status_changed || null} 
                    rawTimestamp={lastTriggeredRaw || null}
                />;
            }
        }
    ];

    // Always show the State column, but control editability within the component
    const columns = [
        ...baseColumns,
        {
            header: 'State',
            accessorKey: 'enabled',
            sortingFn: (rowA: any, rowB: any) => {
                const aEnabled = rowA.original.enabled ?? false;
                const bEnabled = rowB.original.enabled ?? false;
                return aEnabled === bEnabled ? 0 : aEnabled ? -1 : 1;
            },
            cell: ({ row }: { row: AlertRow }) => <AlertDefinitionState alert={{ enabled: row.original.enabled ?? false, id: row.original.alert_definition_id }} />,
        }
    ];

    const getFilteredAlerts = (alerts: any[]) => {
        if (!searchFilters.length) return alerts;
        return filterAlerts(alerts, searchFilters);
    };

    // Process alerts in the correct order: filter -> sort -> paginate
    useEffect(() => {
        if (!alerts.length) {
            setFilteredAlerts([]);
            return;
        }

        // Start with the original alerts
        let result = [...alerts];

        // Apply filters first
        result = getFilteredAlerts(result);

        // Apply sorting if needed
        if (sorting.length > 0) {
            // Use the custom sorting functions defined in columns
            const sortColumn = columns.find(col => col.accessorKey === sorting[0].id);
            if (sortColumn && sortColumn.sortingFn) {
                result = [...result].sort((a, b) => {
                    // Create row-like objects for the sorting function
                    const rowA = { original: a };
                    const rowB = { original: b };
                    const sortResult = sortColumn.sortingFn(rowA, rowB);
                    return sorting[0].desc ? -sortResult : sortResult;
                });
            } else {
                // Fall back to the generic sortAlerts function
                result = sortAlerts(result, sorting[0].id, !sorting[0].desc);
            }
        }

        // Update state with filtered and sorted data
        setFilteredAlerts(result);
    }, [alerts, sorting, searchFilters]);

    // Get paginated data
    const {
        currentItems: currentAlerts,
        changePage,
        currentPage,
        maxPage,
        itemsPerPage,
        setItemsPerPage
    } = usePagination(filteredAlerts, 10);

    return (
        <Container className="p-4 bg-white mt-4">
            {isLoading ? (
                <Spinner />
            ) : (
                <>
                    <MenuBar
                        title="Alerts"
                        alertGroups={alertGroups}
                        alertCounts={alertCounts}
                        onSearch={handleSearch}
                        alertDefinitions={alertDefinitions}
                        onModalStateChange={setIsModalOpen}
                        hasAnyAlertPermissions={hasAnyAlertPermissions}
                    />
                    <Table
                        columns={columns}
                        data={currentAlerts}
                        hover
                        entityName="alerts"
                        sorting={sorting}
                        onSortingChange={setSorting}
                    />
                    <Paginator
                        currentPage={currentPage}
                        maxPage={maxPage}
                        changePage={changePage}
                        itemsPerPage={itemsPerPage}
                        setItemsPerPage={setItemsPerPage}
                        totalItems={filteredAlerts.length}
                    />
                </>
            )}
        </Container>
    );
};

export default Alerts;
