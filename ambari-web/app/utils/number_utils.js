/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
module.exports = {

  /**
   * Convert byte size to other metrics.
   * 
   * @param {Number} Bytes to convert to string
   * @param {Number} Precision Number to adjust precision of return value. Default is 0.
   * @param {String}
   *          parseType JS method name for parse string to number. Default is
   *          "parseInt".
   * @param {Number} Multiplies bytes by this number if given. This is needed
   *          as <code>null * 1024 = 0</null>
   * @remarks The parseType argument can be "parseInt" or "parseFloat".
   * @return {String) Returns converted value with abbreviation.
   */
  bytesToSize: function (bytes, precision, parseType/* = 'parseInt' */, multiplyBy) {
    if (bytes === null || bytes === undefined) {
      return 'n/a';
    } else {
      if (arguments[2] === undefined) {
        parseType = 'parseInt';
      }
      if (arguments[3] === undefined) {
        multiplyBy = 1;
      }
      var value = bytes * multiplyBy;
      var sizes = [ 'Bytes', 'KB', 'MB', 'GB', 'TB', 'PB' ];
      var posttxt = 0;
      while (value >= 1024) {
        posttxt++;
        value = value / 1024;
      }
      if (value === 0) {
        precision = 0;
      }
      var parsedValue = window[parseType](value);
      return parsedValue.toFixed(precision) + " " + sizes[posttxt];
    }
  }
};
