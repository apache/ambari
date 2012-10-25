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

var validator = require('utils/validator');

module.exports = {
  dateMonths:['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
  dateDays:['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
  dateFormatZeroFirst:function (time) {
    if (time < 10) return '0' + time;
    return time;
  },
  /**
   * Convert timestamp to date-string 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES'
   * @param timestamp
   * @return date-string
   */
  dateFormat:function (timestamp) {
    if (!validator.isValidInt(timestamp)) return timestamp;
    var date = new Date(timestamp * 1);
    var months = this.dateMonths;
    var days = this.dateDays;
    return days[date.getDay()] + ', ' + months[date.getMonth()] + ' ' + this.dateFormatZeroFirst(date.getDate()) + ', ' + date.getFullYear() + ' ' + this.dateFormatZeroFirst(date.getHours()) + ':' + this.dateFormatZeroFirst(date.getMinutes());
  },
  /**
   * Convert time in seconds to 'HOURS:MINUTES:SECONDS'
   * @param seconds
   * @return formated date-string
   */
  dateFormatInterval:function (timestamp_interval) {
    if (!validator.isValidInt(timestamp_interval)) return timestamp_interval;
    var hours = Math.floor(timestamp_interval / (60 * 60));
    var divisor_for_minutes = timestamp_interval % (60 * 60);
    var minutes = Math.floor(divisor_for_minutes / 60);
    var divisor_for_seconds = divisor_for_minutes % 60;
    var seconds = Math.ceil(divisor_for_seconds);
    return (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
  }
}
