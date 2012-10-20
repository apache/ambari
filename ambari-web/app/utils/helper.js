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
String.prototype.capitalize = function () {
  return this.charAt(0).toUpperCase() + this.slice(1);
}

Em.CoreObject.reopen({
  t:function (key, attrs) {
    return Em.I18n.t(key, attrs)
  }
});

Handlebars.registerHelper('log', function (variable) {
  console.log(variable);
});

Handlebars.registerHelper('warn', function (variable) {
  console.warn(variable);
});

String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};

/**
 * Convert byte size to other metrics.
 * @param {Number} precision  Number to adjust precision of return value. Default is 0.
 * @param {String} parseType  JS method name for parse string to number. Default is "parseInt".
 * @remarks The parseType argument can be "parseInt" or "parseFloat".
 * @return {String) Returns converted value with abbreviation.
 */
Number.prototype.bytesToSize = function (precision, parseType/* = 'parseInt' */) {
  if (arguments[1] === undefined) {
    parseType = 'parseInt';
  }

  var value = this;
  var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  var posttxt = 0;
  if (this == 0) return 'n/a';
  while (value >= 1024) {
    posttxt++;
    value = value / 1024;
  }
  var parsedValue = window[parseType](value);

  return parsedValue.toFixed(precision) + " " + sizes[posttxt];
}

Number.prototype.toDaysHoursMinutes = function () {
  var formatted = {},
    dateDiff = this,
    minK = 60, // sec
    hourK = 60 * minK, // sec
    dayK = 24 * hourK;

  dateDiff = parseInt(dateDiff / 1000);
  formatted.d = Math.floor(dateDiff / dayK);
  dateDiff -= formatted.d * dayK;
  formatted.h = Math.floor(dateDiff / hourK);
  dateDiff -= formatted.h * hourK;
  formatted.m = Math.floor(dateDiff / minK);
  dateDiff -= formatted.m * minK;

  return formatted;
}

Number.prototype.countPercentageRatio = function (maxValue) {
  var usedValue = this;
  return Math.round((usedValue / maxValue) * 100) + "%";
}