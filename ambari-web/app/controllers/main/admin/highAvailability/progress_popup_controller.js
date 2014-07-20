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

App.HighAvailabilityProgressPopupController = Ember.Controller.extend({

  name: 'highAvailabilityProgressPopupController',

  /**
   * Id of current request
   * @type {Array}
   */
  requestIds: [],

  /**
   * Title for popup header
   * @type {String}
   */
  popupTitle: '',

  /**
   * Array with Hosts tasks data used in <code>App.HostPopup</code>
   * @type {Array}
   */
  services: [],

  /**
   * Timestamp used in <code>App.HostPopup</code>
   * @type {Number}
   */
  serviceTimestamp: null,

  /**
   * Progress controller. Used to get tasks data.
   * @type {App.HighAvailabilityProgressPageController}
   */
  progressController: null,

  /**
   * Requests data with tasks
   * @type {Array}
   */
  hostsData: [],

  /**
   * During loading and calculations show popup with spinner
   * @type {Object}
   */
  spinnerPopup: null,

  /**
   * Get info for <code>requestIds</code> and initialize <code>App.HostPopup</code>
   * @param popupTitle {String}
   * @param requestIds {Array}
   * @param progressController {App.HighAvailabilityProgressPageController}
   * @param showSpinner {Boolean}
   */
  initPopup: function (popupTitle, requestIds, progressController, showSpinner) {
    if(showSpinner){
      var loadingPopup = App.ModalPopup.show({
        header: Em.I18n.t('jobs.loadingTasks'),
        primary: false,
        secondary: false,
        bodyClass: Ember.View.extend({
          template: Ember.Handlebars.compile('<div class="spinner"></div>')
        })
      });
      this.set('spinnerPopup', loadingPopup);
    }
    this.set('progressController', progressController);
    this.set('popupTitle', popupTitle);
    this.set('requestIds', requestIds);
    this.set('hostsData', []);
    this.getHosts();
  },

  /**
   * Send AJAX request to get hosts tasks data
   */
  getHosts: function () {
    var requestIds = this.get('requestIds');
    requestIds.forEach(function (requestId) {
      App.ajax.send({
        name: 'admin.high_availability.polling',
        sender: this,
        data: {
          requestId: requestId
        },
        success: 'onGetHostsSuccess'
      })
    }, this);
  },

  /**
   * Callback for <code>getHosts</code> request
   * @param data
   */
  onGetHostsSuccess: function (data) {
    var hostsData = this.get('hostsData');
    hostsData.push(data);
    if (this.get('requestIds.length') === this.get('hostsData.length')) {
      var popupTitle = this.get('popupTitle');
      this.calculateHostsData(hostsData);
      App.HostPopup.initPopup(popupTitle, this);
      if (this.isRequestRunning(hostsData)) {
        this.addObserver('progressController.logs.length', this, 'getDataFromProgressController');
      }
    }
    if(this.get('spinnerPopup')){
      this.get('spinnerPopup').hide();
      this.set('spinnerPopup', null);
    }
  },

  /**
   * Convert data to format used in <code>App.HostPopup</code>
   * @param data {Array}
   */
  calculateHostsData: function (data) {
    var hosts = [];
    var hostsMap = {};
    var popupTitle = this.get('popupTitle');
    data.forEach(function (request) {
      request.tasks.forEach(function (task) {
        var host = task.Tasks.host_name;
        if (hostsMap[host]) {
          hostsMap[host].logTasks.push(task);
        } else {
          hostsMap[host] = {
            name: task.Tasks.host_name,
            publicName: task.Tasks.host_name,
            logTasks: [task]
          };
        }
      });
    });
    for (var host in hostsMap) {
      hosts.push(hostsMap[host]);
    }
    this.set('services', [
      {name: popupTitle, hosts: hosts}
    ]);
    this.set('serviceTimestamp', App.dateTime());
    if (!this.isRequestRunning(data)) {
      this.removeObserver('progressController.logs.length', this, 'getDataFromProgressController');
    }
  },

  /**
   * Get hosts tasks data from <code>progressController</code>
   */
  getDataFromProgressController: function () {
    var data = this.get('hostsData');
    var tasksData = this.get('progressController.logs');
    if (tasksData.length) {
      var tasks = [];
      tasksData.forEach(function (logs) {
        tasks.pushObjects(logs);
      }, this);
      data.forEach(function (request) {
        tasks = tasks.filterProperty('Tasks.request_id', request.Requests.id);
        request.tasks = tasks;
      });
      this.calculateHostsData(data);
    }
  },

  /**
   * Identify whether request is running by task counters
   * @param requests {Array}
   * @return {Boolean}
   */
  isRequestRunning: function (requests) {
    var result = false;
    requests.forEach(function (request) {
      if ((request.Requests.task_count -
          (request.Requests.aborted_task_count + request.Requests.completed_task_count + request.Requests.failed_task_count
              + request.Requests.timed_out_task_count - request.Requests.queued_task_count)) > 0) {
        result = true;
      }
    });
    return result;
  }
});

