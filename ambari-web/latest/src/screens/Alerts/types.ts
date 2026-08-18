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

import {AlertStatus} from "./alertStatus";

export interface AlertDefinition {
    repeat_tolerance_enabled: any;
    repeat_tolerance: any;
    enabled: boolean;
    name: string;
    label: string;
    description: string;
    service_name: string;
    component_name: string;
    id: number;
    source_type: string;
}

export type AlertDefinitionReference = number | { id: number };

export interface AlertGroup {
    default: boolean;
    definitions: AlertDefinitionReference[];
    cluster_name: string;
    name: string;
    id: number;
    targets?: Array<number | AlertTarget>;
    _deleted?: boolean;
    _isModified?: boolean;
    _isNew?: boolean;
}

export interface AlertGroupItem {
    AlertGroup: AlertGroup;
}

export interface AlertGroupState {
    id: number;
    name: string;
    default: boolean;
    definitions: AlertDefinitionReference[];
    targets: Array<number | AlertTarget>;
    _isModified?: boolean;
    _isNew?: boolean;
    _deleted?: boolean;
}

export interface AlertTarget {
    id?: number;
    name: string;
    description?: string;
    notification_type: string; // EMAIL, SNMP, Custom SNMP, Alert Script
    properties?: Record<string, any>;
    global?: boolean;
    groups?: number[];
    alert_states?: string[];
    is_enabled?: boolean; // Only used for UI display, not sent to API
    _isModified?: boolean;
    _isNew?: boolean;
    _deleted?: boolean;
}

export interface AlertNotification {
    AlertTarget: AlertTarget;
}

export interface AlertRow {
    original: {
        statuses:any,
        status?: string;
        state: string;
        alert_count?: number;
        label?: string;
        alert_definition_id: number;
        serviceDisplayName?: string;
        last_status_changed?: string;
        last_updated_time: number;
        text: string;
        service_name: string;
        host_name: string;
        enabled?: boolean;
        [key: string]: any;
    };
}

export interface StateSummary {
    count: number;
    maintenance_count?: number;
    latest_text: string;
    original_timestamp: string | null;
}

export interface Summary {
    [state: string]: StateSummary;
}

export interface SummariesItem {
    definition_id: string;
    summary: Summary;
}

export interface MergedAlert {
    cluster_name: string;
    alert_group_id: number;
    alert_group_name: string;
    enabled: boolean;
    name: string;
    label: string;
    description: string;
    serviceName?: string;
    serviceDisplayName: string;
    component_name: string;
    alert_definition_id: number;
    source_type: string;
    statuses: { status: AlertStatus; count: number; maintenance_count?: number; last_status_changed: string | null; latest_text: string }[];
    last_status_changed: string | null;
    lastTriggeredFormatted: string;
    lastTriggeredAgoFormatted: string;
    lastTriggeredRaw: string | number | null; // Raw timestamp for proper duration calculation
    state: string;
    latest_text?: string;
    groups?: string;
    typeIconClass?: string;
    help_url?: string;
    repeat_tolerance?: any;
    repeat_tolerance_enabled?: boolean;
    maintenance_state?: string;
}

export interface AlertEditorHandle {
    save: () => Promise<boolean>;
    discard: () => void;
}

export interface SummaryData {
    last_status_changed: string | null;
    statuses: { last_status_changed: string | null; count: number; latest_text: string; status: AlertStatus }[];
    latest_text: string;
}

export type AlertStatusObject = {
    status: AlertStatus;
    count: number;
    last_status_changed: string | null;
    latest_text: string;
};

export type AlertDetailsForNotifications = {
    id: number;
    definition_id: number;
    state: string;
    label: string;
    text: string;
    latest_timestamp: number;
};

export type Notifications = {
    Alert: AlertDetailsForNotifications;
};

export interface SearchFilter {
    category: string;
    value: string;
}

export interface SortConfig {
    id: string;
    desc: boolean;
}
