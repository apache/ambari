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

App.Helpers.date = {

  /**
   * List of monthes short names
   * @type {string[]}
   */
  dateMonths: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],

  /**
   * List of days short names
   * @type {string[]}
   */
  dateDays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],

  /**
   * Add leading zero
   *
   * @param {string} time
   * @returns {string}
   * @method dateFormatZeroFirst
   */
  dateFormatZeroFirst: function (time) {
    if (time < 10) return '0' + time;
    return ""  + time;
  },

  /**
   * Convert timestamp to date-string 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES'
   *
   * @param {number} timestamp
   * @param {bool} showSeconds should seconds be added to result string
   * @param {bool} showMilliseconds should miliseconds be added to result string (if <code>showSeconds</code> is false, milliseconds wouldn't be added)
   * @return {*} date
   * @method dateFormat
   */
  dateFormat: function (timestamp, showSeconds, showMilliseconds) {
    if (!App.Helpers.validator.isValidInt(timestamp)) {
      return timestamp;
    }
    var format = 'ddd, MMM DD, YYYY HH:mm';
    if (showSeconds) {
      format += ':ss';
      if (showMilliseconds) {
        format += ':SSS';
      }
    }
    return moment(new Date(timestamp)).format(format);
  },

  /**
   * Convert starTimestamp to 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES', except for the case: year equals 1969
   *
   * @param {string} startTimestamp
   * @return {string} startTimeSummary
   * @method startTime
   */
  startTime: function (startTimestamp) {
    if (!App.Helpers.validator.isValidInt(startTimestamp)) {
      return '';
    }
    var startDate = new Date(startTimestamp);
    var months = this.dateMonths;
    var days = this.dateDays;
    // generate start time
    if (startDate.getFullYear() == 1969 || startTimestamp < 1) {
      return 'Not started';
    }
    var startTimeSummary = '';
    if (new Date(startTimestamp).setHours(0, 0, 0, 0) == new Date().setHours(0, 0, 0, 0)) { //today
      startTimeSummary = 'Today ' + this.dateFormatZeroFirst(startDate.getHours()) + ':' + this.dateFormatZeroFirst(startDate.getMinutes());
    } else {
      startTimeSummary = days[startDate.getDay()] + ' ' + months[startDate.getMonth()] + ' ' +
        this.dateFormatZeroFirst(startDate.getDate()) + ' ' + startDate.getFullYear() + ' '
        + this.dateFormatZeroFirst(startDate.getHours()) + ':' + this.dateFormatZeroFirst(startDate.getMinutes());
    }
    return startTimeSummary;
  },

  /**
   * Provides the duration between the given start and end timestamp. If start time
   * not valid, duration will be ''. If end time is not valid, duration will
   * be till now, showing 'Lasted for xxx secs'.
   *
   * @param {string} startTimestamp
   * @param {string} endTimestamp
   * @return {string} durationSummary
   * @method durationSummary
   */
  durationSummary: function (startTimestamp, endTimestamp) {
    // generate duration
    var durationSummary = '';
    var startDate = new Date(startTimestamp);
    var endDate = new Date(endTimestamp);
    if (startDate.getFullYear() == 1969 || startTimestamp < 1) {
      // not started
      return Em.I18n.t('common.na');
    }
    if (endDate.getFullYear() != 1969 && endTimestamp > 0) {
      return '' + this.timingFormat(endTimestamp - startTimestamp, 1); //lasted for xx secs
    } else {
      // still running, duration till now
      var t = new Date().getTime(),
        time = (t - startTimestamp) < 0 ? 0 : (t - startTimestamp);
      durationSummary = '' + this.timingFormat(time, 1);
    }
    return durationSummary;
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
   *
   * @param {number} time
   * @param {bool} zeroValid for the case to show 0 when time is 0, not null
   * @return {string|null} formatted date
   * @method timingFormat
   */
  timingFormat: function (time, /* optional */ zeroValid) {
    var intTime = parseInt(time);
    if (zeroValid && intTime == 0) {
      return 0 + ' secs';
    }
    if (!intTime) {
      return null;
    }
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
  },

  /**
   * Provides the duration between the given start and end time. If start time
   * is not given, duration will be 0. If end time is not given, duration will
   * be till now.
   *
   * @param {Number} startTime Start time from epoch
   * @param {Number} endTime End time from epoch
   * @return {Number} duration
   * @method duration
   */
  duration: function (startTime, endTime) {
    var duration = 0;
    if (startTime && startTime > 0) {
      if (!endTime || endTime < 1) {
        endTime = new Date().getTime();
      }
      duration = endTime - startTime;
    }
    return duration;
  }
};
