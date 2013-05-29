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
   * Convert timestamp to date-string 'DAY_OF_THE_WEEK MONTH DAY YEAR'
   * @param timestamp
   * @return {*}
   */
  dateFormatShort: function(timestamp) {
    if (!validator.isValidInt(timestamp)) return timestamp;

    var date = new Date(timestamp*1);
    var today = new Date();
    if (date.toDateString() === today.toDateString()) {
      return 'Today ' + date.toLocaleTimeString();
    }
    return date.toDateString();
  },
  /**
   * Convert date-string 'DAY_OF_THE_WEEK MONTH DAY YEAR' to the timestamp
   * @param date_string
   * @return {Number}
   */
  dateUnformatShort: function(date_string) {
    var date = new Date(date_string);
    return date.getTime();
  },
  /**
   * Convert time in mseconds to 'HOURS:MINUTES:SECONDS'
   * @param ms_interval
   * @return string formatted date
   */
  dateFormatInterval:function (ms_interval) {
    if (!validator.isValidInt(ms_interval)) return ms_interval;
    var hours = Math.floor(ms_interval / (60 * 60000));
    var divisor_for_minutes = ms_interval % (60 * 60000);
    var minutes = Math.floor(divisor_for_minutes / 60000);
    var divisor_for_seconds = divisor_for_minutes % 60000;
    var seconds = (divisor_for_seconds / 1000).toFixed(2);

    return (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
  },
  /**
   * Convert 'HOURS:MINUTES:SECONDS' to time in mseconds
   * @param formattedDate date string
   * @return time in mseconds
   */
  dateUnformatInterval: function(formattedDate) {
    var formattedDateArray = formattedDate.split(' ');

    if (Object.prototype.toString.call( formattedDateArray ) === '[object Array]' && formattedDateArray.length == 2) {
      var oneMinMs = 60000;
      var oneHourMs = 3600000;
      var oneDayMs = 86400000;

      if (formattedDateArray['1'] == 'ms') {
        return formattedDateArray['0'];
      } else if (formattedDateArray['1'] == 'secs') {
        return formattedDateArray['0'] * 1000;
      } else if (formattedDateArray['1'] == 'mins') {
        return formattedDateArray['0'] * oneMinMs;
      } else if (formattedDateArray['1'] == 'hours') {
        return formattedDateArray['0'] * oneHourMs;
      } else if (formattedDateArray['1'] == 'days') {
        return formattedDateArray['0'] * oneDayMs;
      } else {
        console.warn('function dateUnformatInterval: Undefined format');
      }
    } else {
      console.warn('function dateUnformatInterval: formattedDateArray');
    }
  },
  /**
   * Convert time in mseconds to
   * 30 ms = 30 ms
   * 300 ms = 300 ms
   * 999 ms = 999 ms
   * 1000 ms = 1.00 secs
   * 3000 ms = 3.00 secs
   * 35000 ms = 35.00 secs
   * 350000 ms = 350.00 secs
   * 999999 ms = 999.99 secs
   * 1000000 ms = 16.66 mins
   * 3500000 secs = 58.33 mins
   * @param time
   * @return string formatted date
   */
  timingFormat:function (time) {
    var intTime  = parseInt(time);
    var timeStr = intTime.toString();
    var lengthOfNumber = timeStr.length;
    var oneMinMs = 60000;
    var oneHourMs = 3600000;
    var oneDayMs = 86400000;

    if (lengthOfNumber < 4) {
      return time + ' ms';
    } else if (lengthOfNumber < 7) {
      time = (time / 1000).toFixed(2);
      return time + ' secs';
    } else if (time < oneHourMs) {
      time = (time / oneMinMs).toFixed(2);
      return time + ' mins';
    } else if (time < oneDayMs) {
      time = (time / oneHourMs).toFixed(2);
      return time + ' hours';
    } else {
      time = (time / oneDayMs).toFixed(2);
      return time + ' days';
    }
  }
}