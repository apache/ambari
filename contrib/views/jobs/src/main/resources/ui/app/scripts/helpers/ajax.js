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
 *
 * Any property inside {braces} is substituted dynamically by the formatUrl function provided that the property is passed into the "data" dictionary
 * by the ajax call.
 * E.g.,
   App.ajax.send({
     name: 'key_foo',
     data: {
          property1: value1,
          property2: value2
     }
   });

   Where the "urls" dictionary contains 'key_foo': {real: 'some_value_with_{property1}_and_{property2}' }
 */
var urls = {

  'load_jobs': {
    real: '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/HIVE_QUERY_ID{filtersLink}',
    mock: '/scripts/assets/hive-queries.json',
    apiPrefix: ''
  },

  'jobs_lastID': {
    // The filters "TEZ:true" are needed because ATS is case sensitive,
    // and in HDP 2.1, "tez" was used, while in HDP 2.2, "TEZ" was used.
    real: '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/HIVE_QUERY_ID?limit=1&secondaryFilter=TEZ:true',
    mock: '/scripts/assets/hive-queries.json',
    apiPrefix: ''
  },

  'job_details': {
    real: '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/HIVE_QUERY_ID/{job_id}?fields=events,otherinfo',
    mock: '/scripts/assets/hive-query-2.json',
    apiPrefix: ''
  },

  'jobs.tezDag.NametoID': {
    'real': '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/TEZ_DAG_ID?primaryFilter=dagName:{tezDagName}',
    'mock': '/scripts/assets/tezDag-name-to-id.json',
    'apiPrefix': ''
  },

  'jobs.tezDag.tezDagId': {
    'real': '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/TEZ_DAG_ID/{tezDagId}?fields=relatedentities,otherinfo',
    'mock': '/scripts/assets/tezDag.json',
    'apiPrefix': ''
  },

  'jobs.tezDag.tezDagVertexId': {
    'real': '/views/{view}/{version}/{instanceName}/proxy?url={atsURL}/ws/v1/timeline/TEZ_VERTEX_ID/{tezDagVertexId}?fields=otherinfo',
    'mock': '/scripts/assets/tezDagVertex.json',
    'apiPrefix': ''
  },

  'cluster_name': {
    real: 'clusters',
    mock: '/scripts/assets/clusters.json'
  },

  'services': {
    real: 'clusters/{clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state&minimal_response=true',
    mock: '/scripts/assets/services.json'
  },

  'components': {
    real: 'clusters/{clusterName}/components/?fields=ServiceComponentInfo/state&minimal_response=true',
    mock: '/scripts/assets/components.json'
  },

  'components_hosts': {
    real: 'clusters/{clusterName}/hosts?host_components/HostRoles/component_name={componentName}&minimal_response=true',
    mock: '/scripts/assets/components_hosts.json'
  },

  'config_tags': {
    real: 'clusters/{clusterName}/?fields=Clusters/desired_configs',
    mock: '/scripts/assets/desired_configs.json'
  },

  'configurations': {
    real: 'clusters/{clusterName}/configurations?{params}',
    mock: '/scripts/assets/configurations.json'
  },

  'instance_parameters': {
    real: 'views/{view}/versions/{version}/instances/{instanceName}/resources/status',
    mock: ''
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

    var pattern = "/proxy?url=";
    var index = url.indexOf(pattern);
    if (index > -1) {
      url = url.substring(0, index) + pattern + escape(url.substring(index + pattern.length));
    }
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
    headers: this.headers
  };
  if (App.get('testMode')) {
    opt.url = formatUrl(this.mock ? this.mock : '', data);
    opt.type = 'GET';
  }
  else {
    var prefix = this.apiPrefix != null ? this.apiPrefix : App.get('urlPrefix');
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
   */
  send: function (config) {

    Ember.assert('Ajax sender should be defined!', config.sender);
    Ember.assert('Invalid config.name provided - ' + config.name, urls[config.name]);

    var opt = {},
      params = {clusterName: App.get('clusterName')};

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
