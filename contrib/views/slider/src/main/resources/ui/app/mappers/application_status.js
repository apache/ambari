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
   * List of services, we need to get status of them
   * @type {Array}
   */
  servicesWeNeed: [
    'HDFS',
    'YARN',
    'ZOOKEEPER'
  ],

  /**
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function () {
    return App.ajax.send({
      name: 'mapper.applicationStatus',
      sender: this,
      success: 'setResourcesVersion'
    });
  },

  /**
   * Set <code>App</code> properties
   * @param {object} data received from server data
   * @method setResourcesVersion
   */
  setResourcesVersion: function (data) {
    App.set('resourcesVersion', Em.getWithDefault(data, "version", 'version'));
    if (App.get('clusterName')) {
      this.loadServicesStatus();
    }
  },

  /**
   * Get Services status from Ambari-server
   * @returns {$.ajax}
   * @method loadServicesStatus
   */
  loadServicesStatus: function () {
    return App.ajax.send({
      name: 'service_status',
      data: {
        urlPrefix: '/api/v1/'
      },
      sender: this,
      success: 'setErrors'
    });
  },

  /**
   * Success-callback for load services status
   * Set Slider state basing on <code>ServiceInfo.state</code>
   * @param {object} data
   * @method setErrors
   */
  setErrors: function (data) {
    var self = this,
      errors = [];
    this.get('servicesWeNeed').forEach(function (serviceName) {
      var e = self.findError(data.items.findProperty("ServiceInfo.service_name", serviceName));
      if (!Em.isNone(e)) {
        errors.push(e);
      }
    });

    App.set('viewEnabled', errors.length === 0);
    App.set('viewErrors', errors);
  },

  /**
   * Get error for service (missed or not started)
   * @param {object} data
   * @returns {string|null}
   */
  findError: function (data) {
    var name = Em.get(data, "ServiceInfo.service_name"),
      error = null;
    if (data) {
      if (Em.get(data, "ServiceInfo.state") != "STARTED")
        error = Em.I18n.t('error.start' + name);
    }
    else {
      error = Em.I18n.t('error.no' + name);
    }
    return error;
  }

});