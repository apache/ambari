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

App.Helpers.ambari = (function () {
  /**
   * Stores parameters from Ambari.
   */
  var ambariParameters = {},

  /**
   * Constructs URL for fetching Ambari view instance parameters.
   * @return {String}
   */
  getURL = function () {
    var urlParts = location.pathname.split('/');

    return "/api/v1/views/%@/versions/%@/instances/%@/resources/status".fmt(
      urlParts[2],
      urlParts[3],
      urlParts[4]
    );
  };

  return {
    /**
     * Key for the property representing ATS url.
     */
    TIMELINE_URL: 'yarn.ats.url',
    RM_URL: 'yarn.resourcemanager.url',

    /**
     * Returns parameter value.
     * @param {String} name
     * @return {String}
     * @method getParam
     */
    getParam: function (name) {
      Ember.assert('Parameter not found!', ambariParameters[name]);
      return ambariParameters[name];
    },

    /**
     * Get view instance properties provided by user.
     * @returns {$.ajax}
     * @method getInstanceParameters
     */
    getInstanceParameters: function () {
      var hashArray = location.pathname.split('/'),
      params = {
        view: hashArray[2],
        version: hashArray[3],
        instanceName: hashArray[4],
        clusterName: App.get('clusterName')
      };

      return $.ajax({
        type: 'GET',
        dataType: 'json',
        async: true,
        context: this,
        url: getURL(),
        success: this.getInstanceParametersSuccessCallback,
        error: this.getInstanceParametersErrorCallback,
      });
    },

    /**
     * Success callback for getInstanceParameters-request.
     * @param {object} data
     * @method getInstanceParametersSuccessCallback
     */
    getInstanceParametersSuccessCallback: function (data) {
      Ember.assert('Ambari instance parameter weren`t returned by the service!', data.parameters);
      ambariParameters = data.parameters;
    },

    /**
     * Error callback for getInstanceParameters-request.
     * @method getInstanceParametersErrorCallback
     */
    getInstanceParametersErrorCallback: function (request, ajaxOptions, error) {
      Ember.assert('Ambari instance parameter fetch failed: ' + error);
    }
  };

})();

App.deferReadiness();
App.Helpers.ambari.getInstanceParameters().then(function () {
  $.extend(true, App.Configs, {
    envDefaults: {
      isStandalone: false,
      timelineBaseUrl: App.Helpers.ambari.getParam(App.Helpers.ambari.TIMELINE_URL),
      RMWebUrl: App.Helpers.ambari.getParam(App.Helpers.ambari.RM_URL)
    }
  });

  App.advanceReadiness();
});

Ember.$.ajaxSetup({
  beforeSend: function (jqXHR, settings) {
    settings.url = location.pathname + 'proxy?url=' + encodeURIComponent(settings.url);
  }
});
