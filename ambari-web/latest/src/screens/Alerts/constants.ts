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

export const STATUS_PRIORITY_ORDER = ['unknown', 'ok', 'warning', 'critical'];

export const ALERT_SEARCH_CATEGORIES = [
    'Status',
    'Alert Definition Name',
    'Service',
    'Last Status Changed',
    'State',
    'Group',
] as const;

export const TIME_RANGES = {
    'Past 1 hour': 1,
    'Past 1 day': 24,
    'Past 2 days': 48,
    'Past 7 days': 168,
    'Past 14 days': 336,
    'Past 30 days': 720
} as const;

export const STATUS_OPTIONS = [
    AlertStatus.OK,
    AlertStatus.WARNING,
    AlertStatus.CRITICAL,
    AlertStatus.UNKNOWN
] as const;