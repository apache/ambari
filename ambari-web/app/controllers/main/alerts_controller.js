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

App.MainAlertsController = Em.Controller.extend({
  name: 'mainAlertsController',

  alerts: [],
  isLoaded: false,
  isUpdating: false,
  // name of resource(hostName or ServiceName)
  resourceName: null,
  //"HOST" or "SERVICE"
  resourceType: null,
  updateTimer: null,

  /**
   * load alerts for service or host
   * @param name
   * @param type
   */
  loadAlerts: function (name, type) {
    this.set('isLoaded', false);
    this.set('resourceName', name);
    this.set('resourceType', type);
    this.getFromServer();
  },
  /**
   * update alerts
   */
  update: function () {
    var self = this;
    if (this.get('isUpdating')) {
      this.set('updateTimer', setTimeout(function () {
        self.getFromServer();
        self.update();
      }, App.componentsUpdateInterval));
    } else {
      clearTimeout(this.get('updateTimer'));
    }
  }.observes('isUpdating'),

  /**
   * ask alerts from server by type
   */
  getFromServer: function () {
    if (App.router.get('clusterController.isNagiosInstalled')) {
      if (this.get('resourceType') === "SERVICE") {
        this.getAlertsByService();
      } else if (this.get('resourceType') === "HOST") {
        this.getAlertsByHost();
      } else {
        console.warn("GET Alerts error: unknown resourceType");
      }
    } else {
      this.set('isLoaded', true);
    }
  },
  /**
   * request alerts from server, which belong to particular host
   * @return {Boolean}
   */
  getAlertsByHost: function () {
    if (this.get('resourceName')) {
      App.ajax.send({
        name: 'alerts.get_by_host',
        sender: this,
        data: {
          hostName: this.get('resourceName')
        },
        success: 'getAlertsSuccessCallback',
        error: 'getAlertsErrorCallback'
      });
      return true;
    } else {
      console.warn('GET Alerts error: hostName parameter is missing');
      return false;
    }
  },

  /**
   * Saved request for alerts
   * Should be aborted if user navigate away from current page (aborting done in router)
   * @type {$.ajax|null}
   */
  servicesRequest: null,

  /**
   * request alerts from server, which belong to particular service
   * @return {Boolean}
   */
  getAlertsByService: function () {
    if (this.get('resourceName')) {
      var request = App.ajax.send({
        name: 'alerts.get_by_service',
        sender: this,
        data: {
          serviceName: this.get('resourceName')
        },
        success: 'getAlertsSuccessCallback',
        error: 'getAlertsErrorCallback'
      });
      this.set('servicesRequest', request);
      return true;
    } else {
      console.warn('GET Alerts error: serviceName parameter is missing');
      return false;
    }
  },

  /**
   * map to associate old status format with and maintain sorting
   */
  statusNumberMap: {
    "OK": "0",
    "WARNING": "1",
    "CRITICAL": "2",
    "PASSIVE": "3"
  },
  /**
   * format json data and push into @alerts array
   * @param json
   */
  getAlertsSuccessCallback: function (json) {
    var alerts = [];
    if (json && json.alerts && json.alerts.detail) {
      json.alerts.detail.forEach(function (_alert) {
        alerts.pushObject(App.Alert.create({
          id: _alert.description + "_" + _alert.host_name + "_" + _alert.serviceType,
          title: _alert.description,
          serviceType: _alert.service_name,
          lastTime: _alert.last_status_time,
          status: this.get('statusNumberMap')[_alert.status] || "4",
          message: _alert.output,
          hostName: _alert.host_name,
          lastCheck: _alert.status_time,
          isLoaded: true
        }));
      }, this);
    }
    this.set('alerts', alerts.sortProperty('status', 'date').reverse());
    this.set('isLoaded', true);
  },
  /**
   * finish loading if call failed
   */
  getAlertsErrorCallback: function () {
    this.set('isLoaded', true);
  }
});
