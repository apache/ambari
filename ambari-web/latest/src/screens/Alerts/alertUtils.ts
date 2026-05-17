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

import { AlertGroupItem, MergedAlert, SummariesItem, SummaryData, SearchFilter, AlertDefinition } from "./types";
import { TIME_RANGES } from "./constants";
import { AlertStatus } from "./alertStatus";

// Helper function to get status priority (CRITICAL > WARNING > OK > UNKNOWN)
const getStatusPriority = (status: string): number => {
    const priorities: Record<string, number> = {
        [AlertStatus.CRITICAL]: 4,
        [AlertStatus.WARNING]: 3,
        [AlertStatus.OK]: 2,
        [AlertStatus.UNKNOWN]: 1,
        [AlertStatus.NONE]: 0
    };
    
    const normalizedStatus = status.toLowerCase();
    return priorities[normalizedStatus] || 0;
};

export const isWithinTimeRange = (lastStatusChanged: string | null, timeRangeValue: string): boolean => {
    if (!lastStatusChanged || lastStatusChanged === 'Unknown') return false;

    const now = new Date().getTime();
    const alertTime = new Date(lastStatusChanged).getTime();
    const hours = TIME_RANGES[timeRangeValue as keyof typeof TIME_RANGES] || 0;
    const timeRange = hours * 60 * 60 * 1000;

    return (now - alertTime) <= timeRange;
};

export const sortAlerts = (alerts: MergedAlert[], sortField: string, isAsc: boolean): MergedAlert[] => {
    return [...alerts].sort((a, b) => {
        let comparison = 0;

        switch (sortField) {
            case 'status':
                // Get highest priority status for each alert
                const aStatuses = a.statuses.map(s => s.status.toLowerCase());
                const bStatuses = b.statuses.map(s => s.status.toLowerCase());
                const aHighestPriority = Math.max(...aStatuses.map(s => getStatusPriority(s)));
                const bHighestPriority = Math.max(...bStatuses.map(s => getStatusPriority(s)));
                comparison = bHighestPriority - aHighestPriority;
                break;

            case 'last_status_changed':
                const aTime = a.statuses[0]?.last_status_changed ? new Date(a.statuses[0].last_status_changed).getTime() : 0;
                const bTime = b.statuses[0]?.last_status_changed ? new Date(b.statuses[0].last_status_changed).getTime() : 0;
                comparison = aTime - bTime;
                break;

            case 'service_name':
                comparison = (a.serviceDisplayName || '').localeCompare(b.serviceDisplayName || '');
                break;

            case 'name':
                comparison = (a.name || '').localeCompare(b.name || '');
                break;

            default:
                comparison = 0;
        }

        return isAsc ? comparison : -comparison;
    });
};

export const filterAlerts = (alerts: MergedAlert[], filters: SearchFilter[]): MergedAlert[] => {
    return alerts.filter(alert => {
        return filters.every(filter => {
            switch (filter.category) {
                case 'Alert Definition Name':
                    return alert.label.toLowerCase().includes(filter.value.toLowerCase());

                case 'Service':
                    return alert.serviceDisplayName.toLowerCase() === filter.value.toLowerCase();

                case 'Status':
                    // Handle empty statuses as NONE
                    if (filter.value.toLowerCase() === AlertStatus.NONE) {
                        return !alert.statuses.length || alert.statuses.some(s => s.status.toLowerCase() === AlertStatus.NONE);
                    }
                    return alert.statuses.some(s =>
                        s.status.toLowerCase() === filter.value.toLowerCase()
                    );

                case 'Last Status Changed':
                    return alert.statuses[0] && isWithinTimeRange(alert.statuses[0].last_status_changed, filter.value);

                case 'Group':
                    return alert.groups === filter.value;

                case 'State':
                    return alert.state === filter.value;

                default:
                    return true;
            }
        });
    });
};

export const getTimeRangeValue = (range: string): Date | null => {
    const now = new Date();
    const hours = TIME_RANGES[range as keyof typeof TIME_RANGES] || 0;

    if (!hours) return null;
    return new Date(now.getTime() - (hours * 60 * 60 * 1000));
};

export const getCategoryValues = (category: string, alertGroups: AlertGroupItem[]): string[] => {
    if (!alertGroups || !Array.isArray(alertGroups) || alertGroups.length === 0) {
        console.log('No alert groups available for category:', category);
        return [];
    }

    switch (category) {
        case 'Alert Definition Name':
            return Array.from(new Set(alertGroups.flatMap(group =>
                (group.AlertGroup.definitions || []).map(def => {
                    if (typeof def === 'object' && 'label' in def) {
                        return (def as AlertDefinition).label;
                    }
                    return '';
                }).filter(name => name !== '')
            )));
        case 'Service':
            return Array.from(new Set(alertGroups.flatMap(group =>
                (group.AlertGroup.definitions || []).map(def => {
                    if (typeof def === 'object' && 'service_name' in def) {
                        return (def as AlertDefinition).service_name;
                    }
                    return '';
                }).filter(name => name !== '')
            )));
        case 'Group':
            return Array.from(new Set(alertGroups.map(group =>
                group.AlertGroup.name + (group.AlertGroup.default ? ' Default' : '')
            )));
        case 'Status':
            return [AlertStatus.CRITICAL, AlertStatus.WARNING, AlertStatus.OK, AlertStatus.UNKNOWN, AlertStatus.NONE];
        case 'State':
            return ['Enabled', 'Disabled'];
        case 'Last Status Changed':
            return Object.keys(TIME_RANGES);
        default:
            return [];
    }
};

export const formatDate = (dateString: string | null | undefined): string => {
    if (!dateString) return 'Unknown';
    try {
        const parts = dateString.split(', ');
        if (parts.length < 2) return dateString;
        
        const [day, month, year] = parts[0].split('/');
        const time = parts[1];
        const date = new Date(`${year}-${month}-${day}T${time}`);
        
        return date.toLocaleDateString('en-UK', {
            weekday: 'short',
            year: 'numeric',
            month: 'short',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    } catch (error) {
        console.error('Error formatting date:', error);
        return dateString;
    }
};

export const processData = (
    alertsResponse: { items: AlertGroupItem[] },
    summariesResponse: { alerts_summary_grouped: SummariesItem[] }
): MergedAlert[] => {
    if (!alertsResponse?.items || !Array.isArray(alertsResponse.items)) {
        console.error('Invalid alerts response:', alertsResponse);
        return [];
    }

    // Use a Map to deduplicate alert definitions by their ID
    const uniqueAlerts = new Map<number, MergedAlert>();
    const groupsByDefinitionId = new Map<number, string[]>();

    alertsResponse.items.forEach((item) => {
        if (!item?.AlertGroup?.definitions || !Array.isArray(item.AlertGroup.definitions)) {
            console.warn('Invalid alert group:', item);
            return;
        }
        
        item.AlertGroup.definitions.forEach((def) => {
            if (typeof def === 'object' && 'name' in def && def.name) {
                const definition = def as AlertDefinition;
                const definitionId = definition.id;
                
                // Track which groups this definition belongs to
                if (!groupsByDefinitionId.has(definitionId)) {
                    groupsByDefinitionId.set(definitionId, []);
                }
                const groupName = item.AlertGroup.default ? `${item.AlertGroup.name} Default` : item.AlertGroup.name;
                groupsByDefinitionId.get(definitionId)!.push(groupName);
                
                // Only create the alert if we haven't seen this definition ID before
                if (!uniqueAlerts.has(definitionId)) {
                    uniqueAlerts.set(definitionId, {
                        cluster_name: item.AlertGroup.cluster_name,
                        alert_group_name: item.AlertGroup.name,
                        alert_group_id: item.AlertGroup.id,
                        enabled: definition.enabled,
                        name: definition.name,
                        label: definition.label,
                        description: definition.description,
                        serviceDisplayName: definition.service_name,
                        component_name: definition.component_name,
                        alert_definition_id: definition.id,
                        source_type: definition.source_type,
                        repeat_tolerance: definition.repeat_tolerance,
                        repeat_tolerance_enabled: definition.repeat_tolerance_enabled,
                        statuses: [],
                        last_status_changed: 'Unknown',
                        lastTriggeredFormatted: 'Unknown',
                        lastTriggeredAgoFormatted: 'Unknown',
                        state: definition.enabled ? 'Enabled' : 'Disabled',
                        groups: groupName
                    } as any);
                }
            }
        });
    });

    // Convert Map to array and update groups field with all groups for each definition
    const parsedAlerts: MergedAlert[] = Array.from(uniqueAlerts.values()).map(alert => ({
        ...alert,
        groups: groupsByDefinitionId.get(alert.alert_definition_id)?.join(', ') || alert.groups
    }));

    const summariesMap: { [key: string]: SummaryData } = {};

    summariesResponse.alerts_summary_grouped.forEach((item) => {
        const definitionId = String(item.definition_id);
        const summary = item.summary;

        // FIXED: Include both regular alerts and maintenance mode alerts
        const statusesWithCounts = Object.keys(summary || {})
            .filter((state) => {
                const stateData = summary?.[state];
                return stateData && (stateData.count > 0 || (stateData.maintenance_count && stateData.maintenance_count > 0));
            })
            .map((state) => {
                const stateData = summary?.[state];
                return {
                    status: state as AlertStatus,
                    count: stateData?.count || 0,
                    maintenance_count: stateData?.maintenance_count || 0,
                    last_status_changed: stateData?.original_timestamp || null,
                    latest_text: stateData?.latest_text || ''
                };
            });

        // Sort by priority (CRITICAL first, then WARNING, OK, UNKNOWN)
        statusesWithCounts.sort((a, b) => getStatusPriority(b.status) - getStatusPriority(a.status));

        summariesMap[definitionId] = {
            statuses: statusesWithCounts,
            last_status_changed:
                statusesWithCounts.length > 0
                    ? statusesWithCounts[0].last_status_changed
                    : null,
            latest_text:
                statusesWithCounts.length > 0 ? statusesWithCounts[0].latest_text : '',
        };
    });

    return parsedAlerts.map((alert) => {
        const summary = summariesMap[String(alert.alert_definition_id)];
        if (summary) {
            // Use the original timestamp format instead of formatting it
            // The LastStatusChanged component can handle the original format
            const originalTimestamp = summary.last_status_changed || 'Unknown';

            return {
                ...alert,
                statuses: summary.statuses,
                last_status_changed: originalTimestamp,
                lastTriggeredFormatted: originalTimestamp,
                latest_text: summary.latest_text,
            };
        } else {
            return {
                ...alert,
                statuses: [],
                last_status_changed: 'Unknown',
                lastTriggeredFormatted: 'Unknown',
                lastTriggeredRaw: null,
                latest_text: '',
            };
        }
    });
};

export type { MergedAlert };
