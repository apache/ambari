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

import dayjs from "dayjs";
import duration from "dayjs/plugin/duration";

dayjs.extend(duration);

/**
 * Duration calculation utilities matching Ember.js implementation
 * Based on ui/app/utils/date/date.js
 */

/**
 * Check if a timestamp represents an invalid date (1969 or less than 1)
 * @param timestamp - Unix timestamp in milliseconds
 * @returns boolean indicating if timestamp is invalid
 */
function isInvalidTimestamp(timestamp: number): boolean {
  if (!timestamp || timestamp < 1) return true;
  const date = new Date(timestamp);
  return date.getFullYear() === 1969;
}

/**
 * Format duration in the same way as Ember.js timingFormat
 * Format: "{#days}d {#hours}h {#minutes}m {#seconds}s"
 * Display optimization rules:
 *   - if time more than a day then hide time lower than minute
 *   - if time more than a minute and less than an hour then hide time lower than second
 * @param timeMs - Duration in milliseconds
 * @returns Formatted duration string
 */
function formatDuration(timeMs: number): string {
  if (timeMs === 0) return '0s';
  
  const time = Math.abs(timeMs);
  const fullTime = time;
  let formattedDuration = '';

  const oneSecMs = 1000;
  const oneMinMs = 60000;
  const oneHourMs = 3600000;
  const oneDayMs = 86400000;

  // Extract time units
  const days = Math.floor(time / oneDayMs);
  const remainingAfterDays = time % oneDayMs;
  
  const hours = Math.floor(remainingAfterDays / oneHourMs);
  const remainingAfterHours = remainingAfterDays % oneHourMs;
  
  const minutes = Math.floor(remainingAfterHours / oneMinMs);
  const remainingAfterMinutes = remainingAfterHours % oneMinMs;
  
  const seconds = Math.floor(remainingAfterMinutes / oneSecMs);

  // Build duration string
  if (days > 0) {
    formattedDuration += `${days}d `;
  }
  if (hours > 0) {
    formattedDuration += `${hours}h `;
  }
  if (minutes > 0) {
    formattedDuration += `${minutes}m `;
  }
  
  // Only show seconds if less than a day
  if (fullTime < oneDayMs) {
    if (seconds > 0) {
      formattedDuration += `${seconds}s `;
    } else if (fullTime < oneSecMs) {
      formattedDuration += '1s ';
    }
  }

  return formattedDuration.trim();
}

/**
 * Calculate duration summary matching Ember.js durationSummary function
 * @param startTimestamp - Start time in milliseconds
 * @param endTimestamp - End time in milliseconds (optional, uses current time if not provided for running operations)
 * @returns Duration string or "n/a" for invalid cases
 */
export function calculateDurationSummary(startTimestamp: number, endTimestamp?: number): string {
  // Check if start time is invalid - return "n/a"
  if (isInvalidTimestamp(startTimestamp)) {
    return 'n/a';
  }

  let durationMs: number;

  if (endTimestamp && endTimestamp > 0 && !isInvalidTimestamp(endTimestamp)) {
    // Task completed - calculate actual duration
    durationMs = endTimestamp - startTimestamp;
  } else {
    // Task still running or no end time - calculate duration till now
    const currentTime = Date.now();
    durationMs = currentTime - startTimestamp;
    
    // Ensure we don't show negative duration for running tasks
    if (durationMs < 0) {
      durationMs = 0;
    }
  }

  // If duration is negative (invalid case), return "n/a"
  if (durationMs < 0) {
    return 'n/a';
  }

  return formatDuration(durationMs);
}

/**
 * Check if a request status indicates the operation is still running
 * @param status - Request status string
 * @returns boolean indicating if operation is running
 */
export function isOperationRunning(status: string): boolean {
  return ['IN_PROGRESS', 'PENDING', 'QUEUED'].includes(status?.toUpperCase());
}

/**
 * Check if a request status indicates the operation is finished
 * @param status - Request status string
 * @returns boolean indicating if operation is finished
 */
export function isOperationFinished(status: string): boolean {
  return ['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT', 'SKIPPED_FAILED'].includes(status?.toUpperCase());
}

/**
 * Get current time with timezone (matching App.dateTimeWithTimeZone() from Ember.js)
 * @returns Current timestamp in milliseconds
 */
export function getCurrentTimeWithTimeZone(): number {
  return Date.now();
}
