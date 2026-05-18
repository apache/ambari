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

import { useEffect, useState, memo } from 'react';
import { format } from 'timeago.js';

interface LastStatusChangedProps {
    timestamp: string | number | null;
    rawTimestamp?: string | number | null; // Add support for raw timestamp like Ember.js
}

const parseCustomTimestamp = (timestamp: string | number | null): Date | null => {
    if (!timestamp) return null;
    
    try {
        // If it's a number (unix timestamp), convert directly
        if (typeof timestamp === 'number') {
            // Check if the timestamp is in milliseconds (13 digits) or seconds (10 digits)
            const timestampMs = timestamp.toString().length <= 10 ? timestamp * 1000 : timestamp;
            const date = new Date(timestampMs);
            // Validate date is within reasonable range (1970-2100)
            if (isNaN(date.getTime()) || date.getFullYear() < 1970 || date.getFullYear() > 2100) {
                return null;
            }
            return date;
        }

        // If it's a string in "DD/MM/YYYY, HH:mm:ss" format
        if (typeof timestamp === 'string' && timestamp.includes(',')) {
            const [datePart, timePart] = timestamp.split(', ');
            if (!datePart || !timePart) return null;

            const [day, month, year] = datePart.split('/').map(num => parseInt(num, 10));
            const [hours, minutes, seconds] = timePart.split(':').map(num => parseInt(num, 10));

            if ([day, month, year, hours, minutes, seconds].some(isNaN)) return null;
            if (month < 1 || month > 12 || day < 1 || day > 31) return null;

            // Month is 0-based in Date constructor
            const date = new Date(year, month - 1, day, hours, minutes, seconds);
            if (isNaN(date.getTime())) return null;
            return date;
        }

        // Try parsing as a regular date string
        const date = new Date(timestamp);
        if (isNaN(date.getTime()) || date.getFullYear() < 1970 || date.getFullYear() > 2100) {
            return null;
        }
        return date;
    } catch (error) {
        console.error("Error parsing timestamp:", timestamp, error);
        return null;
    }
};

const LastStatusChanged = memo(({ timestamp, rawTimestamp }: LastStatusChangedProps) => {
    const [formattedTime, setFormattedTime] = useState<string>('-');

    useEffect(() => {
        const updateFormattedTime = () => {
            try {
                // Use rawTimestamp if available (like Ember.js lastTriggeredRaw), otherwise fall back to timestamp
                const timestampToUse = rawTimestamp || timestamp;
                const date = parseCustomTimestamp(timestampToUse);
                if (date) {
                    setFormattedTime(format(date));
                } else {
                    setFormattedTime('-');
                }
            } catch (error) {
                console.error("Error formatting time:", error);
                setFormattedTime('-');
            }
        };

        updateFormattedTime();
        const intervalId = setInterval(updateFormattedTime, 60000);
        return () => clearInterval(intervalId);
    }, [timestamp, rawTimestamp]);

    // For the tooltip, use the original format if it's a string with comma, otherwise use locale string
    let tooltipDate = '-';
    try {
        if (typeof timestamp === 'string' && timestamp.includes(',')) {
            tooltipDate = timestamp;
        } else {
            const timestampToUse = rawTimestamp || timestamp;
            const date = parseCustomTimestamp(timestampToUse);
            tooltipDate = date ? date.toLocaleString() : '-';
        }
    } catch (error) {
        console.error("Error creating tooltip:", error);
    }

    return (
        <div className="text-nowrap" title={tooltipDate}>
            {formattedTime}
        </div>
    );
});

LastStatusChanged.displayName = 'LastStatusChanged';

export default LastStatusChanged;
