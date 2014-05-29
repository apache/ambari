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

/**
 * Config for each ajax-request
 *
 * Fields example:
 *  mock - testMode url
 *  real - real url (without API prefix)
 *  type - request type (also may be defined in the format method)
 *  format - function for processing ajax params after default formatRequest. May be called with one or two parameters (data, opt). Return ajax-params object
 *  testInProduction - can this request be executed on production tests (used only in tests)
 *
 * @type {Object}
 */
var urls = {

  'mapper.applicationTypes': {
    real: 'apptypes?fields=*',
    mock: '/data/apptypes/all_fields.json',
    headers: {
      Accept : "text/plain; charset=utf-8",
      "Content-Type": "text/plain; charset=utf-8"
    }
  },

  'mapper.applicationApps': {
    real: 'apps/?fields=*',
    mock: '/data/apps/apps.json',
    headers: {
      Accept : "text/plain; charset=utf-8",
      "Content-Type": "text/plain; charset=utf-8"
    }
  },

  'mapper.applicationStatus': {
    real: 'resources/status',
    mock: '/data/resource/status_true.json'
  },

  'createNewApp': {
    real: 'apps',
    mock: '',
    headers: {
      "Content-Type": "text/plain; charset=utf-8"
    },
    format: function(data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },

  'destroyApp': {
    real: 'apps/{id}',
    mock: '',
    format: function() {
      return {
        method: 'DELETE'
      }
    }
  },

  'changeAppState': {
    real: 'apps/{id}',
    mock: '',
    headers: {
      "Content-Type": "text/plain; charset=utf-8"
    },
    format: function(data) {
      return {
        method: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },
  'flexApp': {
    real: 'apps/{id}',
    mock: '',
    headers: {
      "Content-Type": "text/plain; charset=utf-8"
    },
    format: function(data) {
      return {
        method: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },

  'metrics': {
    real: 'apps/{id}/metrics/{metric}',
    mock: '/data/metrics/metric.json'
  },

  'metrics2': {
    real: 'apps/{id}/metrics/{metric}',
    mock: '/data/metrics/metric2.json'
  },

  'metrics3': {
    real: 'apps/{id}/metrics/{metric}',
    mock: '/data/metrics/metric3.json'
  },

  'metrics4': {
    real: 'apps/{id}/metrics/{metric}',
    mock: '/data/metrics/metric4.json'
  }

};
/**
 * Replace data-placeholders to its values
 *
 * @param {String} url
 * @param {Object} data
 * @return {String}
 */
var formatUrl = function (url, data) {
  if (!url) return null;
  var keys = url.match(/\{\w+\}/g);
  keys = (keys === null) ? [] : keys;
  if (keys) {
    keys.forEach(function (key) {
      var raw_key = key.substr(1, key.length - 2);
      var replace;
      if (!data || !data[raw_key]) {
        replace = '';
      }
      else {
        replace = data[raw_key];
      }
      url = url.replace(new RegExp(key, 'g'), replace);
    });
  }
  return url;
};

/**
 * this = object from config
 * @return {Object}
 */
var formatRequest = function (data) {
  var opt = {
    type: this.type || 'GET',
    dataType: 'json',
    async: true,
    headers: this.headers || {Accept: "application/json; charset=utf-8"}
  };
  if (App.get('testMode')) {
    opt.url = formatUrl(this.mock ? this.mock : '', data);
    opt.type = 'GET';
  }
  else {
    var prefix = App.get('urlPrefix');
    opt.url = prefix + formatUrl(this.real, data);
  }

  if (this.format) {
    jQuery.extend(opt, this.format(data, opt));
  }
  return opt;
};

/**
 * Wrapper for all ajax requests
 *
 * @type {Object}
 */
var ajax = Em.Object.extend({
  /**
   * Send ajax request
   *
   * @param {Object} config
   * @return {$.ajax} jquery ajax object
   *
   * config fields:
   *  name - url-key in the urls-object *required*
   *  sender - object that send request (need for proper callback initialization) *required*
   *  data - object with data for url-format
   *  beforeSend - method-name for ajax beforeSend response callback
   *  success - method-name for ajax success response callback
   *  error - method-name for ajax error response callback
   *  callback - callback from <code>App.updater.run</code> library
   */
  send: function (config) {

    Ember.assert('Ajax sender should be defined!', config.sender);
    Ember.assert('Invalid config.name provided - ' + config.name, urls[config.name]);

    var opt = {},
      params = {};

    if (config.data) {
      jQuery.extend(params, config.data);
    }

    opt = formatRequest.call(urls[config.name], params);
    opt.context = this;

    // object sender should be provided for processing beforeSend, success, error and complete responses
    opt.beforeSend = function (xhr) {
      if (config.beforeSend) {
        config.sender[config.beforeSend](opt, xhr, params);
      }
    };

    opt.success = function (data) {
      console.log("TRACE: The url is: " + opt.url);
      if (config.success) {
        config.sender[config.success](data, opt, params);
      }
    };

    opt.error = function (request, ajaxOptions, error) {
      if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt, params);
      }
    };

    opt.complete = function (xhr, status) {
      if (config.complete) {
        config.sender[config.complete](xhr, status);
      }
    };

    return $.ajax(opt);
  }

});

App.ajax = ajax.create({});
