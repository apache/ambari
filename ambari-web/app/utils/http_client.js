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

var App = require('app');

/**
 * App.HttpClient perform an ajax request
 */
App.HttpClient = Em.Object.create({

  /**
   *
   * @param jqXHR
   * @param textStatus
   * @param errorThrown
   */
  defaultErrorHandler: function (jqXHR, textStatus, errorThrown) {
    var json = $.parseJSON(jqXHR.responseText);
    if (json) {
      Em.assert("HttpClient:", json);
    } else {
      Em.assert("HttpClient:", errorThrown);
    }
  },

  /**
   * @param {string} url
   * @param {Object} ajaxOptions
   * @param {App.ServerDataMapper} mapper - json processor
   * @param {function} errorHandler
   * @param {number} interval - frequecy request
   */
  request: function (url, ajaxOptions, mapper, errorHandler) {

    if (!errorHandler) {
      errorHandler = this.defaultErrorHandler;
    }

    var options = {dataType: 'json'}; // default ajax options;

    $.extend(options, {
      url: url,
      success: function (data) {
        mapper.map(data);
      },
      error: errorHandler
    });

    $.extend(options, ajaxOptions);

    $.ajax(options);
  },

  /**
   * @param {string} url
   * @param {App.ServerDataMapper} mapper - json processor
   * @param {Object} data - ajax data property
   * @param {function} errorHandler
   * @param {number} interval - frequecy request
   */
  get: function (url, mapper, data, errorHandler, interval) {
    var client = this;
    var request = function () {
      client.request(url, data, mapper, errorHandler);
    }

    interval = "" + interval;
    if (interval.match(/\d+/)) {
      $.periodic({period: interval}, request);
    } else {
      request();
    }
  },

  /**
   * @param {string} url
   * @param {Object} data - ajax data property
   * @param {App.ServerDataMapper} mapper - json processor
   * @param {function} errorHandler
   * @param {number} interval - frequecy request
   */
  post: function (url, data, mapper, errorHandler, interval) {
    this.get(url, data, mapper, errorHandler, interval);
  }

//  not realized yet
//  put:function (url, mapper, errorHandler) {
//    this.request(url, {}, mapper, errorHandler);
//  },
//
//  delete:function (url, mapper, errorHandler) {
//    this.request(url, {}, mapper, errorHandler);
//  }
});

/*App.HttpClient.get(
  'http://nagiosserver/hdp/nagios/nagios_alerts.php?q1=alerts&alert_type=all',
  App.alertsMapper,
  { dataType: 'jsonp', jsonp: 'jsonp' }
);*/
