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
  'background_operations.update_task': {
    'mock': '/data/background_operations/one_task.json',
    'real': '/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}',
    'testInProduction': true
  },
  'background_operations.get_most_recent': {
    'mock': '/data/background_operations/list_on_start.json',
    'real': '/clusters/{clusterName}/requests?fields=*,tasks/Tasks/*',
    'testInProduction': true
  },
  'service.item.start_stop': {
    'mock': '/data/wizard/deploy/poll_1.json',
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo : {
            "context" : data.requestInfo
          },
          Body:{
            ServiceInfo: {
              state: data.state
            }
          }
        })
      };
    }
  },
  'service.item.smoke': {
    'mock': '/data/wizard/deploy/poll_1.json',
    'real': '/clusters/{clusterName}/services/{serviceName}/actions/{serviceName}_SERVICE_CHECK',
    'format': function () {
      return {
        'type': 'POST',
        data: JSON.stringify({
          RequestInfo : {
            "context" : "Smoke Test"
          }
        })
      };
    }
  },
  'reassign.stop_service': {
    'mock': '/data/wizard/reassign/request_id.json',
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo : {
            "context" : "Stop service " + data.serviceName
          },
          Body:{
            ServiceInfo: {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'reassign.create_master': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name={hostName}',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "host_components": [
            {
              "HostRoles": {
                "component_name": data.componentName
              }
            }
          ]
        })
      }
    }
  },
  'reassign.maintenance_mode': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'type': 'PUT',
    'format': function () {
      return {
        data: JSON.stringify(
            {
              "HostRoles": {
                "state": "MAINTENANCE"
              }
            }
        )
      }
    }
  },
  'reassign.install_component': {
    'mock': '/data/wizard/reassign/request_id.json',
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo : {
            "context" : "Install " + data.componentName
          },
          Body:{
            "HostRoles": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'reassign.start_components': {
    'mock': '/data/wizard/reassign/request_id.json',
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo : {
            "context" : "Start service " + data.serviceName
          },
          Body:{
            ServiceInfo: {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'reassign.remove_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'type': 'DELETE'
  },
  'reassign.get_logs': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=tasks/*',
    'type': 'GET'
  },
  'reassign.create_configs': {
    'real': '/clusters/{clusterName}/configurations',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify(data.configs),
        configs: data.configs
      }
    }
  },
  'reassign.check_configs': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'type': 'GET'
  },
  'reassign.apply_configs': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(data.configs)
      }
    }
  },
  'config.advanced': {
    'real': '{stackVersionUrl}/services/{serviceName}',
    'mock': '/data/wizard/stack/hdp/version130/{serviceName}.json',
    'format': function(data){
      return {
        async: false
      };
    }
  },
  'config.advanced.global': {
    'real': '{stack2VersionUrl}/stackServices?fields=configurations/StackConfigurations/filename',
    'mock': '/data/wizard/stack/hdp/version130/global.json',
    'format': function(data){
      return {
        async: false
      };
    }
  },
  'config.tags': {
    'real': '/clusters/{clusterName}',
    'mock': '/data/clusters/cluster.json'
  },
  'config.on-site': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/cluster_level_configs.json?{params}',
    'format': function(data){
      return {
        async: false
      };
    }
  },
  'config.host_overrides': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/host_level_overrides_configs.json?{params}',
    'format': function(data){
      return {
        async: false
      };
    }
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
  keys = (keys === null) ? [] :  keys;
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
    opt.url = formatUrl(this.mock, data);
    opt.type = 'GET';
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
   * @return Object jquery ajax object
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
  send: function(config) {
    if (!config.sender) {
      console.warn('Ajax sender should be defined!');
      return null;
    }

    // default parameters
    var params = {
      clusterName: App.get('clusterName')
    };

    // extend default parameters with provided
    if (config.data) {
      jQuery.extend(params, config.data);
    }

    var opt = {};
    opt = formatRequest.call(urls[config.name], params);

    // object sender should be provided for processing beforeSend, success and error responses
    opt.beforeSend = function() {
      if(config.beforeSend) {
        config.sender[config.beforeSend](opt);
      }
    };
    opt.success = function(data) {
      if(config.success) {
        config.sender[config.success](data, opt, params);
      }
    };
    opt.error = function(request, ajaxOptions, error) {
      if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt);
      }
    };
    opt.complete = function(){
      if(config.callback){
        config.callback();
      }
    };
    if($.mocho){
      opt.url = 'http://' + $.hostName + opt.url;
    }

    return $.ajax(opt);
  }
}
