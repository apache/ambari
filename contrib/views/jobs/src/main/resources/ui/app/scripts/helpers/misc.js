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

App.Helpers.misc = {

  /**
   * Convert value from bytes to appropriate measure
   */
  formatBandwidth: function (value) {
    if (value) {
      if (value < 1024) {
        value = '<1KB';
      } else {
        if (value < 1048576) {
          value = (value / 1024).toFixed(1) + 'KB';
        } else  if (value >= 1048576 && value < 1073741824){
          value = (value / 1048576).toFixed(1) + 'MB';
        } else {
          value = (value / 1073741824).toFixed(2) + 'GB';
        }
      }
    }
    return value;
  }

};
