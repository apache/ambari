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
   * @return string date
   */
  dateFormat:function (timestamp) {
    if (!validator.isValidInt(timestamp)) return timestamp;
    var date = new Date(timestamp * 1);
    var months = this.dateMonths;
    var days = this.dateDays;
    return days[date.getDay()] + ', ' + months[date.getMonth()] + ' ' + this.dateFormatZeroFirst(date.getDate()) + ', ' + date.getFullYear() + ' ' + this.dateFormatZeroFirst(date.getHours()) + ':' + this.dateFormatZeroFirst(date.getMinutes());
  },
  /**
   * Convert date-string 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES' to timestamp
   * @param date_string
   * @return {String}
   */
  dateUnformat: function(date_string) {
    var date = date_string.substring(4);
    var month = date.substring(1, 4);
    var day = date.substring(5, 7);
    var year = date.substring(9, 13);
    var hours = date.substring(14, 16);
    var minutes = date.substring(17, 19);

    var months = this.dateMonths;
    month = months.indexOf(month) + 1;
    if (month < 10) month = '0' + month;
    return year + month + day + hours + minutes;
  },
  /**
   * Convert time in seconds to 'HOURS:MINUTES:SECONDS'
   * @param timestamp_interval
   * @return string formatted date
   */
  dateFormatInterval:function (timestamp_interval) {
    if (!validator.isValidInt(timestamp_interval)) return timestamp_interval;
    var hours = Math.floor(timestamp_interval / (60 * 60));
    var divisor_for_minutes = timestamp_interval % (60 * 60);
    var minutes = Math.floor(divisor_for_minutes / 60);
    var divisor_for_seconds = divisor_for_minutes % 60;
    var seconds = Math.ceil(divisor_for_seconds);
    return (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
  },
  /**
   * Convert 'HOURS:MINUTES:SECONDS' to time in seconds
   * @param formattedDate date string
   * @return time in seconds
   */
  dateUnformatInterval: function(formattedDate) {
    var d = formattedDate.split(':');
    for (var k in d) {
      d[k] = parseInt(d[k], 10);
    }
    return d[0]*3600+d[1]*60+d[2];
  }
}
