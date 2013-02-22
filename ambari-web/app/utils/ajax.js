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
 * Config for each ajax-request
 *
 * Fields example:
 *  mock - testMode url
 *  real - real url (without API prefix)
 *  type - request type (also may be defined in the format method)
 *  format - function for processing ajax params after default formatRequest. Return ajax-params object
 *  testInProduction - can this request be executed on production tests (used only in tests)
 *
 * @type {Object}
 */
var urls = {
  'background_operations': {
    'mock': '/data/background_operations/list_on_start.json',
    'real': '/clusters/{clusterName}/requests/?fields=tasks/*',
    'testInProduction': true
  },
  'service.item.start_stop': {
    'mock': '/data/wizard/deploy/poll_1.json',
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          ServiceInfo: {
            state: data.state
          }
        })
      };
    }
  },
  'service.item.smoke': {
    'mock': '/data/wizard/deploy/poll_1.json',
    'real': '/clusters/{clusterName}/services/{serviceName}/actions/{serviceName}_SERVICE_CHECK',
    'type': 'POST'
  }
};
/**
 * Replace data-placeholders to its values
 *
 * @param {String} url
 * @param {Object} data
 * @return {String}
 */
var formatUrl = function(url, data) {
  var keys = url.match(/\{\w+\}/g);
  keys.forEach(function(key){
    var raw_key = key.substr(1, key.length - 2);
    var replace;
    if (!data[raw_key]) {
      replace = '';
    }
    else {
      replace = data[raw_key];
    }
    url = url.replace(new RegExp(key, 'g'), replace);
  });
  return url;
};

/**
 * this = object from config
 * @return {Object}
 */
var formatRequest = function(data) {
  var opt = {
    type : this.type || 'GET',
    timeout : App.timeout,
    dataType: 'json',
    statusCode: require('data/statusCodes')
  };
  if(App.testMode) {
    opt.url = this.mock;
  }
  else {
    opt.url = App.apiPrefix + formatUrl(this.real, data);
  }

  if(this.format) {
    jQuery.extend(opt, this.format(data, opt));
  }
  return opt;
};

/**
 * Wrapper for all ajax requests
 *
 * @type {Object}
 */
App.ajax = {
  /**
   * Send ajax request
   *
   * @param {Object} config
   * @return {Boolean}
   *
   * config fields:
   *  name - url-key in the urls-object *required*
   *  sender - object that send request (need for proper callback initialization) *required*
   *  data - object with data for url-format
   *  success - method-name for ajax success response callback
   *  error - method-name for ajax error response callback
   */
  send: function(config) {
    if (!config.sender) {
      console.warn('Ajax sender should be defined!');
      return false;
    }

    // default parameters
    var params = {
      clusterName: App.router.get('clusterController.clusterName')
    };

    // extend default parameters with provided
    if (config.data) {
      jQuery.extend(params, config.data);
    }

    var opt = {};
    opt = formatRequest.call(urls[config.name], params);

    // object sender should be provided for processing success and error responses
    opt.success = function(data) {
      if(config.success) {
        config.sender[config.success](data, opt);
      }
    };
    opt.error = function(request, ajaxOptions, error) {
      if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt);
      }
    };
    $.ajax(opt);
  }
}
