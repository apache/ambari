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
 * Mapper for SLIDER_1 status
 * Save mapped data to App properties
 * @type {App.Mapper}
 */
App.ApplicationStatusMapper = App.Mapper.createWithMixins(App.RunPeriodically, {

  /**
   * Map for parsing JSON received from server
   * Format:
   *  <code>
   *    {
   *      key1: 'path1',
   *      key2: 'path2',
   *      key3: 'path3'
   *    }
   *  </code>
   *  Keys - names for properties in App
   *  Values - pathes in JSON
   * @type {object}
   */
  map: {
    viewEnabled: 'viewEnabled',
    viewErrors: 'viewErrors',
    resourcesVersion: 'version'
  },

  /**
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function() {
    console.log('App.ApplicationStatusMapper loading data');
    return App.ajax.send({
      name: 'mapper.applicationStatus',
      sender: this,
      success: 'parse'
    });
  },

  /**
   * Parse loaded data according to <code>map</code>
   * Set <code>App</code> properties
   * @param {object} data received from server data
   * @method parse
   */
  parse: function(data) {
    var map = this.get('map');
    Ember.keys(map).forEach(function(key) {
      App.set(key, Ember.getWithDefault(data, map[key], ''));
    });
  },

  /**
   * Get cluster name from server
   * @returns {$.ajax}
   * @method getClusterName
   */
  getClusterName: function() {
    return App.ajax.send({
      name: 'cluster_name',
      sender: this,
      data: {
        urlPrefix: '/api/v1/'
      },
      success: 'getClusterNameSuccessCallback'
    });
  },

  /**
   * Success callback for clusterName-request
   * @param {object} data
   * @method getClusterNameSuccessCallback
   */
  getClusterNameSuccessCallback: function(data) {
    App.set('clusterName', Em.get(data.items[0], 'Clusters.cluster_name'));
  }

});