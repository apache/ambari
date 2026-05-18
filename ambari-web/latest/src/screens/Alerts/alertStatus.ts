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

export enum AlertStatus {
    CRITICAL = 'critical',
    WARNING = 'warning',
    OK = 'ok',
    UNKNOWN = 'unknown',
    NONE = 'none'
}

// Map for displaying shortened status text
export const AlertStatusDisplay: { [key: string]: string } = {
    [AlertStatus.CRITICAL]: 'CRIT',
    [AlertStatus.WARNING]: 'WARN',
    [AlertStatus.OK]: 'OK',
    [AlertStatus.UNKNOWN]: 'UNKWN',
    [AlertStatus.NONE]: 'NONE'
};

// Format status for display, preserving original API values
export const formatAlertStatusDisplay = (status: string, count: number = 0): string => {
    const normalizedStatus = status?.toLowerCase();
    const displayText = AlertStatusDisplay[normalizedStatus] || status?.toUpperCase() || 'NONE';
    return count <= 1 ? displayText : `${displayText} (${count})`;
};