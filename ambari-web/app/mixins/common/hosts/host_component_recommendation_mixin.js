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

require('mixins/common/blueprint');

var App = require('app');

/**
 * @typedef {object} ServiceInstanceObject
 * @property {string} name name of the service instance
 * @property {string} type of the service instance (usually the name of the service, i.e. ZOOKEEPER)
 */

/**
 * @typedef {object} MpackInstanceObject
 * @property {string} name name of the mpack instances (usually the name of a service group)
 * @property {string} type of the mpack instance (usually the name of the mpack, i.e. HDPCORE)
 * @property {string} version of the mpack
 * @property {ServiceInstanceObject[]} service_instances list of service instances
 */

/**
 * @typedef {object} HostComponentRecommendationOptions
 * @property {string[]} hosts list of host names, in most cases all available host names
 * @property {MpackInstanceObject[]} mpack_instances list of mpack instances
 */

/**
 * @typedef {object} HostRecommendationRequestData
 * @property {string} recommend recommendation type e.g. 'host_groups'
 * @property {string[]} hosts list of host names
 * @property {object} recommendations blueprint object
 */

/**
 * Contains methods to get server recommendation and validation for host components
 * @type {Em.Mixin}
 */
App.HostComponentRecommendationMixin = Em.Mixin.create(App.BlueprintMixin, {

  /**
   * Get recommendations for selected components
   * @method getRecommendedHosts
   * @param {HostComponentRecommendationOptions} recommend list of the components
   * @return {$.Deferred}
   */
  getRecommendedHosts: function(options) {
    var opts = $.extend({
      hosts: [],
      components: [],
      blueprint: null
    }, options || {});

    opts.components = this.formatRecommendComponents(opts.components);
    return this.loadComponentsRecommendationsFromServer(this.getRecommendationRequestData(opts));
  },

  /**
   * @method formatRecommendComponents
   * @param {RecommendComponentsObject[]} components
   * @returns {Em.Object[]}
   */
  formatRecommendComponents: function(components) {
    var res = [];
    if (!components) return [];
    components.forEach(function(component) {
      if (Em.get(component, 'hosts.length')) {
        Em.get(component, 'hosts').forEach(function(hostName) {
          res.push(Em.Object.create({
            componentName: Em.get(component, 'componentName'),
            mpackInstance: Em.get(component, 'mpackInstance'),
            serviceInstance: Em.get(component, 'serviceInstance'),
            hostName: hostName
          }));
        });
      }
    });
    return res;
  },

  /**
   * Returns request data for recommendation request
   * @param {HostComponentRecommendationOptions} options
   * @return {HostRecommendationRequestData}
   * @method getRecommendationRequestData
   */
  getRecommendationRequestData: function(options) {
    const requestData = {
      data: {
        recommend: 'host_groups',
        hosts: options.hosts,
        recommendations: options.blueprint || this.getComponentsBlueprint(options.components)
      }
    }

    requestData.data.recommendations.blueprint.mpack_instances = options.mpack_instances;

    return requestData;
  },

  /**
   * Get recommendations info from API
   * @method loadComponentsRecommendationsFromServer
   * @param {object} recommendationData
   * @returns {$.Deferred}
   */
  loadComponentsRecommendationsFromServer: function(recommendationData) {
    return App.ajax.send({
      name: 'mpack.advisor.recommendations',
      sender: this,
      data: recommendationData,
      success: 'loadRecommendationsSuccessCallback',
      error: 'loadRecommendationsErrorCallback'
    });
  },

  //these can be overridden in the derived object
  loadRecommendationsSuccessCallback: function() {},
  loadRecommendationsErrorCallback: function() {}
});
