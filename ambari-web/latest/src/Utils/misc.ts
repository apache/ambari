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

export const misc = {
  /**
   * Convert value from bytes to appropriate measure 24364716
   */
  formatBandwidth(value: number, unit = "") {
    let formattedValue: any = value;
    if (value) {
      if (value < 1024) {
        formattedValue = "<1KB";
      } else {
        if (value < 1048576) {
          formattedValue = (value / 1024).toFixed(1) + unit || "KB";
        } else if (value >= 1048576 && value < 1073741824) {
          formattedValue = (value / 1048576).toFixed(1) + unit || "MB";
        } else {
          formattedValue = (value / 1073741824).toFixed(2) + unit || "GB";
        }
      }
    }
    return formattedValue;
  },

  /**
   * Convert IP address to integer
   * @param ip
   * @return integer | false
   */
  ipToInt(ip: string): number | false {
    // Verify IP format.
    if (
      !/^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(
        ip
      )
    ) {
      return false; // Invalid format.
    }
    // Reuse ip variable for component counter.
    const d = ip.split(".").map(Number);
    return ((d[0] * 256 + d[1]) * 256 + d[2]) * 256 + d[3];
  },

  /**
   * Sort array by order
   * @param sortOrder
   * @param array
   * @return sorted array
   */
  sortByOrder<T>(
    sortOrder: (string | number)[],
    array: T[],
    getId: (item: T) => string | number
  ): T[] {
    const sorted: T[] = [];
    for (let i = 0; i < sortOrder.length; i++) {
      for (let j = 0; j < array.length; j++) {
        if (sortOrder[i] === getId(array[j])) {
          sorted.push(array[j]);
        }
      }
    }
    return sorted;
  },

  /**
   * Convert UTF-8 string to Base64
   * @param stringToEncode
   * @return base64 encoded string
   */
  utf8ToB64(stringToEncode: string): string {
    return window.btoa(unescape(encodeURIComponent(stringToEncode)));
  },

  /**
   * Convert Base64 string to UTF-8
   * @param stringToDecode
   * @return decoded string
   */
  b64ToUtf8(stringToDecode: string): string {
    return decodeURIComponent(escape(window.atob(stringToDecode)));
  },
};