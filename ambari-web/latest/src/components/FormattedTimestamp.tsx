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

import { memo } from 'react';

interface FormattedTimestampProps {
    timestamp: string | number | null;
}

const parseCustomTimestamp = (timestamp: string | number | null): Date | null => {
    if (!timestamp) return null;
    
    try {
        // If it's already a number (unix timestamp), convert directly
        if (typeof timestamp === 'number') {
            // Check if it's in seconds (10 digits) or milliseconds (13 digits)
            const timestampMs = timestamp.toString().length <= 10 ? timestamp * 1000 : timestamp;
            const date = new Date(timestampMs);
            // Validate date is within reasonable range (1970-2100)
            if (isNaN(date.getTime()) || date.getFullYear() < 1970 || date.getFullYear() > 2100) {
                return null;
            }
            return date;
        }

        // If it's a string, try different parsing methods
        if (typeof timestamp === 'string') {
            // First try parsing as a standard date string
            let date = new Date(timestamp);
            if (!isNaN(date.getTime())) {
                return date;
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
                            return date;
                        }
                    }
                }
            }

            // Try parsing as unix timestamp string
            const numericTimestamp = parseInt(timestamp, 10);
            if (!isNaN(numericTimestamp)) {
                const timestampMs = numericTimestamp.toString().length <= 10 ? numericTimestamp * 1000 : numericTimestamp;
                date = new Date(timestampMs);
                if (!isNaN(date.getTime()) && date.getFullYear() >= 1970 && date.getFullYear() <= 2100) {
                    return date;
                }
            }
        }

        return null;
    } catch (error) {
        console.error("Error parsing timestamp:", timestamp, error);
        return null;
    }
};

const formatTimestamp = (date: Date): string => {
    // Format to match Ember's 'ddd, MMM DD, YYYY HH:mm' format
    // Example: "Tue, Sep 23, 2025 00:16"
    
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    
    const dayName = days[date.getDay()];
    const monthName = months[date.getMonth()];
    const day = date.getDate().toString().padStart(2, '0');
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    
    return `${dayName}, ${monthName} ${day}, ${year} ${hours}:${minutes}`;
};

const FormattedTimestamp = memo(({ timestamp }: FormattedTimestampProps) => {
    try {
        const date = parseCustomTimestamp(timestamp);
        if (date) {
            return <span>{formatTimestamp(date)}</span>;
        } else {
            return <span>-</span>;
        }
    } catch (error) {
        console.error("Error formatting timestamp:", error);
        return <span>-</span>;
    }
});

FormattedTimestamp.displayName = 'FormattedTimestamp';

export default FormattedTimestamp;
