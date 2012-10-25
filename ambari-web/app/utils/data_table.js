/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

jQuery.extend(jQuery.fn.dataTableExt.oSort, {
  // @see utils/date.js
  "ambari-date-pre":function (date_string) {
    date_string = $(date_string).text(); // strip Ember script tags
    var date = date_string.substring(4);
    var month = date.substring(1, 4);
    var day = date.substring(5, 7);
    var year = date.substring(9, 13);
    var hours = date.substring(14, 16);
    var minutes = date.substring(17, 19);

    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    month = months.indexOf(month);
    if (month < 10) month = '0' + month;
    return year + month + day + hours + minutes;
  },

  "ambari-date-asc":function (a, b) {
    return a - b;
  },

  "ambari-date-desc":function (a, b) {
    return b - a;
  }
});