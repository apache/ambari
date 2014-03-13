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
var batchUtils = require('utils/batch_scheduled_requests');
var date = require('utils/date');

/**
 * App.HostPopup is for the popup that shows up upon clicking already-performed or currently-in-progress operations
 */
App.HostPopup = Em.Object.create({

  servicesInfo: null,
  hosts: null,
  inputData: null,

  /**
   * @type {string}
   */
  serviceName: "",

  /**
   * @type {Number}
   */
  currentServiceId: null,
  previousServiceId: null,

  /**
   * @type {string}
   */
  popupHeaderName: "",

  /**
   * @type {App.Controller}
   */
  dataSourceController: null,

  /**
   * @type {bool}
   */
  isBackgroundOperations: false,

  /**
   * @type {string}
   */
  currentHostName: null,

  /**
   * @type {App.ModalPopup}
   */
  isPopup: null,

  /**
   * Entering point of this component
   * @param {String} serviceName
   * @param {App.Controller} controller
   * @param {bool} isBackgroundOperations
   */
  initPopup: function (serviceName, controller, isBackgroundOperations) {
    if (!isBackgroundOperations) {
      this.clearHostPopup();
      this.set("popupHeaderName", serviceName);
    }
    this.set("serviceName", serviceName);
    this.set("dataSourceController", controller);
    this.set("isBackgroundOperations", isBackgroundOperations);
    this.set("inputData", this.get("dataSourceController.services"));
    if(isBackgroundOperations){
      this.onServiceUpdate();
    } else {
      this.onHostUpdate();
    }
    return this.createPopup();
  },

  /**
   * clear info popup data
   */
  clearHostPopup: function () {
    this.set('servicesInfo', null);
    this.set('hosts', null);
    this.set('inputData', null);
    this.set('serviceName', "");
    this.set('currentServiceId', null);
    this.set('previousServiceId', null);
    this.set('popupHeaderName', "");
    this.set('dataSourceController', null);
    this.set('currentHostName', null);
    this.get('isPopup') ? this.get('isPopup').remove() : null;
  },

  /**
   * Depending on tasks status
   * @param {Array} tasks
   * @return {Array} [Status, Icon type, Progressbar color, is IN_PROGRESS]
   */
  getStatus: function(tasks){
    var isCompleted = true;
    var status;
    var tasksLength = tasks.length;
    var isFailed = false;
    var isAborted = false;
    var isTimedout = false;
    var isInProgress = false;
    for (var i = 0; i < tasksLength; i++) {
      if (tasks[i].Tasks.status !== 'COMPLETED') {
        isCompleted = false;
      }
      if(tasks[i].Tasks.status === 'FAILED'){
        isFailed = true;
      }
      if (tasks[i].Tasks.status === 'ABORTED') {
        isAborted = true;
      }
      if (tasks[i].Tasks.status === 'TIMEDOUT') {
        isTimedout = true;
      }
      if (tasks[i].Tasks.status === 'IN_PROGRESS') {
        isInProgress = true;
      }
    }
    if (isFailed) {
      status = ['FAILED', 'icon-exclamation-sign', 'progress-danger', false];
    } else if (isAborted) {
      status = ['CANCELLED', 'icon-minus', 'progress-warning', false];
    } else if (isTimedout) {
      status = ['TIMEDOUT', 'icon-time', 'progress-warning', false];
    } else if (isInProgress) {
      status = ['IN_PROGRESS', 'icon-cogs', 'progress-info', true];
    }
    if(status){
      return status;
    } else if(isCompleted){
      return ['SUCCESS', 'icon-ok', 'progress-success', false];
    } else {
      return ['PENDING', 'icon-cog', 'progress-info', true];
    }
  },

  /**
   * Progress of host or service depending on tasks status
   * @param {Array} tasks
   * @return {Number} percent of completion
   */
  getProgress: function (tasks) {
    var completedActions = 0;
    var queuedActions = 0;
    var inProgressActions = 0;
    tasks.forEach(function(task) {
      if(['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT'].contains(task.Tasks.status)){
        completedActions++;
      } else if(task.Tasks.status === 'QUEUED'){
        queuedActions++;
      } else if(task.Tasks.status === 'IN_PROGRESS'){
        inProgressActions++;
      }
    });
    return Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / tasks.length * 100);
  },

  /**
   * Count number of operations for select box options
   * @param {Object[]} obj
   * @param {Object[]} categories
   */
  setSelectCount: function (obj, categories) {
    if (!obj) return;
    var countAll = obj.length;
    var countPending = 0;
    var countInProgress = 0;
    var countFailed = 0;
    var countCompleted = 0;
    var countAborted = 0;
    var countTimedout = 0;
    obj.forEach(function(item){
      switch (item.status){
        case 'pending':
          countPending++;
          break;
        case 'queued':
          countPending++;
          break;
        case 'in_progress':
          countInProgress++;
          break;
        case 'failed':
          countFailed++;
          break;
        case 'success':
          countCompleted++;
          break;
        case 'completed':
          countCompleted++;
          break;
        case 'aborted':
          countAborted++;
          break;
        case 'timedout':
          countTimedout++;
          break;
      }
    });

    categories.findProperty("value", 'all').set("count", countAll);
    categories.findProperty("value", 'pending').set("count", countPending);
    categories.findProperty("value", 'in_progress').set("count", countInProgress);
    categories.findProperty("value", 'failed').set("count", countFailed);
    categories.findProperty("value", 'completed').set("count", countCompleted);
    categories.findProperty("value", 'aborted').set("count", countAborted);
    categories.findProperty("value", 'timedout').set("count", countTimedout);
  },

  /**
   * For Background operation popup calculate number of running Operations, and set popup header
   * @param {bool} isServiceListHidden
   */
  setBackgroundOperationHeader: function (isServiceListHidden) {
    if (this.get('isBackgroundOperations') && !isServiceListHidden) {
      var numRunning =  App.router.get('backgroundOperationsController.allOperationsCount');
      this.set("popupHeaderName", numRunning + Em.I18n.t('hostPopup.header.postFix'));
    }
  },

  /**
   * Create services obj data structure for popup
   * Set data for services
   * @param {bool} isServiceListHidden
   */
  onServiceUpdate: function (isServiceListHidden) {
    if (this.get('isBackgroundOperations') && this.get("inputData")) {
      var self = this;
      var allNewServices = [];
      var statuses = {
        'FAILED': ['FAILED', 'icon-exclamation-sign', 'progress-danger', false],
        'ABORTED': ['CANCELLED', 'icon-minus', 'progress-warning', false],
        'TIMEDOUT': ['TIMEDOUT', 'icon-time', 'progress-warning', false],
        'IN_PROGRESS': ['IN_PROGRESS', 'icon-cogs', 'progress-info', true],
        'COMPLETED': ['SUCCESS', 'icon-ok', 'progress-success', false]
      };
      var pendingStatus = ['PENDING', 'icon-cog', 'progress-info', true];
      this.set("servicesInfo", null);
      this.get("inputData").forEach(function (service) {
        var status = statuses[service.status] || pendingStatus;
        var newService = Ember.Object.create({
          id: service.id,
          displayName: service.displayName,
          progress: service.progress,
          status: App.format.taskStatus(status[0]),
          isRunning: service.isRunning,
          name: service.name,
          isVisible: true,
          startTime: date.startTime(service.startTime),
          duration: date.durationSummary(service.startTime, service.endTime),
          icon: status[1],
          barColor: status[2],
          isInProgress: status[3],
          barWidth: "width:" + service.progress + "%;",
          sourceRequestScheduleId: service.get('sourceRequestScheduleId'),
          contextCommand: service.get('contextCommand')
        });
        allNewServices.push(newService);
      });
      self.set('servicesInfo', allNewServices);
      this.setBackgroundOperationHeader(isServiceListHidden);
    }
  },

  /**
   * create task Ember object
   * @param {Object} _task
   * @return {Em.Object}
   */
  createTask: function (_task) {
    return Em.Object.create({
      id: _task.Tasks.id,
      hostName: _task.Tasks.host_name,
      command: ( _task.Tasks.command.toLowerCase() != 'service_check') ? _task.Tasks.command.toLowerCase() : '',
      commandDetail: App.format.commandDetail(_task.Tasks.command_detail),
      status: App.format.taskStatus(_task.Tasks.status),
      role: App.format.role(_task.Tasks.role),
      outputLog: Em.I18n.t('common.hostLog.popup.logDir.path') + Em.I18n.t('common.hostLog.popup.outputLog.value').format(_task.Tasks.id),
      errorLog: Em.I18n.t('common.hostLog.popup.logDir.path') + Em.I18n.t('common.hostLog.popup.errorLog.value').format(_task.Tasks.id),
      stderr: _task.Tasks.stderr,
      stdout: _task.Tasks.stdout,
      isVisible: true,
      startTime: date.startTime(_task.Tasks.start_time),
      duration: date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time),
      icon: function () {
        var statusIconMap = {
          'pending': 'icon-cog',
          'queued': 'icon-cog',
          'in_progress': 'icon-cogs',
          'completed': 'icon-ok',
          'failed': 'icon-exclamation-sign',
          'aborted': 'icon-minus',
          'timedout': 'icon-time'
        };
        return statusIconMap[this.get('status')] || 'icon-cog';
      }.property('status')
    });
  },

  /**
   * Create hosts and tasks data structure for popup
   * Set data for hosts and tasks
   */
  onHostUpdate: function () {
    var self = this;
    var inputData = this.get("inputData");
    if (inputData) {
      var hostsArr = [];
      var hostsData;
      var hostsMap = {};

      if(this.get('isBackgroundOperations') && this.get("currentServiceId")){
        //hosts popup for Background Operations
        hostsData = inputData.findProperty("id", this.get("currentServiceId"));
      } else if (this.get("serviceName")) {
        //hosts popup for Wizards
        hostsData = inputData.findProperty("name", this.get("serviceName"));
      }
      if (hostsData) {
        if (hostsData.hostsMap) {
          //hosts data come from Background Operations as object map
          hostsMap = hostsData.hostsMap;
        } else if (hostsData.hosts) {
          //hosts data come from Wizard as array
          hostsData.hosts.forEach(function (_host) {
            hostsMap[_host.name] = _host;
          });
        }
      }
      var existedHosts = self.get('hosts');

      if (existedHosts && (existedHosts.length > 0) && this.get('currentServiceId') === this.get('previousServiceId')) {
        existedHosts.forEach(function (host) {
          var newHostInfo = hostsMap[host.get('name')];
          //update only hosts with changed tasks or currently opened tasks of host
          if (newHostInfo && (newHostInfo.isModified || this.get('currentHostName') === host.get('name'))) {
            var hostStatus = self.getStatus(newHostInfo.logTasks);
            var hostProgress = self.getProgress(newHostInfo.logTasks);
            host.set('status', App.format.taskStatus(hostStatus[0]));
            host.set('icon', hostStatus[1]);
            host.set('barColor', hostStatus[2]);
            host.set('isInProgress', hostStatus[3]);
            host.set('progress', hostProgress);
            host.set('barWidth', "width:" + hostProgress + "%;");
            host.set('logTasks', newHostInfo.logTasks);
            var existTasks = host.get('tasks');
            if (existTasks) {
              newHostInfo.logTasks.forEach(function (_task) {
                var existTask = existTasks.findProperty('id', _task.Tasks.id);
                if (existTask) {
                  existTask.set('status', App.format.taskStatus(_task.Tasks.status));
                  existTask.set('stdout', _task.Tasks.stdout);
                  existTask.set('stderr', _task.Tasks.stderr);
                  existTask.set('startTime', date.startTime(_task.Tasks.start_time));
                  existTask.set('duration', date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time));
                } else {
                  existTasks.pushObject(this.createTask(_task));
                }
              }, this);
            }
          }
        }, this);
      } else {
        for (var hostName in hostsMap) {
          var _host = hostsMap[hostName];
          var tasks = _host.logTasks;
          var hostInfo = Ember.Object.create({
            name: hostName,
            publicName: _host.publicName,
            displayName: function () {
              return this.get('name').length < 43 ? this.get('name') : (this.get('name').substr(0, 40) + '...');
            }.property('name'),
            progress: 0,
            status: App.format.taskStatus("PENDING"),
            serviceName: _host.serviceName,
            isVisible: true,
            icon: "icon-cog",
            barColor: "progress-info",
            barWidth: "width:0%;"
          });

          if (tasks.length) {
            tasks = tasks.sortProperty('Tasks.id');
            var hostStatus = self.getStatus(tasks);
            var hostProgress = self.getProgress(tasks);
            hostInfo.set('status', App.format.taskStatus(hostStatus[0]));
            hostInfo.set('icon', hostStatus[1]);
            hostInfo.set('barColor', hostStatus[2]);
            hostInfo.set('isInProgress', hostStatus[3]);
            hostInfo.set('progress', hostProgress);
            hostInfo.set('barWidth', "width:" + hostProgress + "%;");
          }
          hostInfo.set('logTasks', tasks);
          hostsArr.push(hostInfo);
        }

        hostsArr = hostsArr.sortProperty('name');
        hostsArr.setEach("serviceName", this.get("serviceName"));
        self.set("hosts", hostsArr);
        self.set('previousServiceId', this.get('currentServiceId'));
      }
    }
  },

  /**
   * Show popup
   * @return {App.ModalPopup} PopupObject For testing purposes
   */
  createPopup: function () {
    var self = this;
    var hostsInfo = this.get("hosts");
    var servicesInfo = this.get("servicesInfo");
    var isBackgroundOperations = this.get('isBackgroundOperations');
    var categoryObject = Em.Object.extend({
      value: '',
      count: 0,
      labelPath: '',
      label: function(){
        return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
      }.property('count')
    });
    self.set('isPopup', App.ModalPopup.show({
      /**
       * no need to track is it loaded when popup contain only list of hosts
       * @type {bool}
       */
      isLoaded: !isBackgroundOperations,

      /**
       * is BG-popup opened
       * @type {bool}
       */
      isOpen: false,

      didInsertElement: function(){
        this._super();
        this.set('isOpen', true);
      },

      /**
       * @type {Em.View}
       */
      headerClass: Em.View.extend({
        controller: this,
        template: Ember.Handlebars.compile('{{popupHeaderName}}')
      }),

      /**
       * @type {String[]}
       */
      classNames: ['sixty-percent-width-modal', 'host-progress-popup'],

      /**
       * for the checkbox: do not show this dialog again
       * @type {bool}
       */
      hasFooterCheckbox: true,

      /**
       * Auto-display BG-popup
       * @type {bool}
       */
      isNotShowBgChecked : null,

      /**
       * Save user pref about auto-display BG-popup
       */
      updateNotShowBgChecked: function () {
        var curVal = !this.get('isNotShowBgChecked');
        var key = App.router.get('applicationController').persistKey();
        if (!App.testMode) {
          App.router.get('applicationController').postUserPref(key, curVal);
        }
      }.observes('isNotShowBgChecked'),

      autoHeight: false,
      closeModelPopup: function () {
        this.set('isOpen', false);
        if(isBackgroundOperations){
          $(this.get('element')).detach();
          App.router.get('backgroundOperationsController').set('levelInfo.name', 'REQUESTS_LIST');
        } else {
          this.hide();
          self.set('isPopup', null);
        }
      },
      onPrimary: function () {
        this.closeModelPopup();
      },
      onClose: function () {
        this.closeModelPopup();
      },
      secondary: null,

      bodyClass: Em.View.extend({
        templateName: require('templates/common/host_progress_popup'),
        isLogWrapHidden: true,
        isTaskListHidden: true,
        isHostListHidden: true,
        isServiceListHidden: false,
        showTextArea: false,
        isServiceEmptyList: true,
        isHostEmptyList: true,
        isTasksEmptyList: true,
        controller: this,
        sourceRequestScheduleId: -1,
        sourceRequestScheduleRunning: false,
        sourceRequestScheduleAborted: false,
        sourceRequestScheduleCommand: null,
        hosts: self.get('hosts'),
        services: self.get('servicesInfo'),

        currentHost: function () {
          return this.get('hosts') && this.get('hosts').findProperty('name', this.get('controller.currentHostName'));
        }.property('controller.currentHostName'),

        tasks: function () {
          var currentHost = this.get('currentHost');
          if (currentHost) {
            return currentHost.get('tasks');
          }
          return [];
        }.property('currentHost.tasks', 'currentHost.tasks.@each.status'),

        /**
         * Preset values on init
         */
        setOnStart: function () {
          if (this.get("controller.isBackgroundOperations")) {
            this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
            this.updateHostInfo();
          } else {
            this.set("isHostListHidden", false);
            this.set("isServiceListHidden", true);
          }
        },

        /**
         * force popup to show list of operations
         */
        resetState: function(){
          if(this.get('parentView.isOpen')){
            this.set('isLogWrapHidden', true);
            this.set('isTaskListHidden', true);
            this.set('isHostListHidden', true);
            this.set('isServiceListHidden', false);
            this.get("controller").setBackgroundOperationHeader(false);
            this.setOnStart();
          }
        }.observes('parentView.isOpen'),

        /**
         * When popup is opened, and data after polling has changed, update this data in component
         */
        updateHostInfo: function () {
          if(!this.get('parentView.isOpen')) return;
          this.set('parentView.isLoaded', false);
          this.get("controller").set("inputData", this.get("controller.dataSourceController.services"));
          this.get("controller").onServiceUpdate(this.get('isServiceListHidden'));
          this.get("controller").onHostUpdate();
          this.set('parentView.isLoaded', true);
          //push hosts into view when none or all hosts are loaded
          if(this.get('hosts') == null || this.get('hosts').length === this.get("controller.hosts").length){
            this.set("hosts", this.get("controller.hosts"));
          }
          this.set("services", this.get("controller.servicesInfo"));
        }.observes("controller.dataSourceController.serviceTimestamp"),

        /**
         * Depending on service filter, set which services should be shown
         */
        visibleServices: function () {
          if (this.get("services")) {
            this.set("isServiceEmptyList", true);
            if (this.get('serviceCategory.value')) {
              var filter = this.get('serviceCategory.value');
              var services = this.get('services');
              this.set("isServiceEmptyList", this.setVisibility(filter, services));
            }
          }
        }.observes('serviceCategory', 'services', 'services.@each.status'),

        /**
         * Depending on hosts filter, set which hosts should be shown
         */
        visibleHosts: function () {
          this.set("isHostEmptyList", true);
          if (this.get('hostCategory.value') && this.get('hosts')) {
            var filter = this.get('hostCategory.value');
            var hosts = this.get('hosts');
            this.set("isHostEmptyList", this.setVisibility(filter, hosts));
          }
        }.observes('hostCategory', 'hosts', 'hosts.@each.status'),

        /**
         * Depending on tasks filter, set which tasks should be shown
         */
        visibleTasks: function () {
          this.set("isTasksEmptyList", true);
          if (this.get('taskCategory.value') && this.get('tasks')) {
            var filter = this.get('taskCategory.value');
            var tasks = this.get('tasks');
            this.set("isTasksEmptyList", this.setVisibility(filter, tasks));
          }
        }.observes('taskCategory', 'tasks', 'tasks.@each.status'),

        /**
         * Depending on selected filter type, set object visibility value
         * @param filter
         * @param obj
         * @return {bool} isEmptyList
         */
        setVisibility: function (filter, obj) {
          var isEmptyList = true;
          if (filter == "all") {
            obj.setEach("isVisible", true);
            isEmptyList = !(obj.length > 0);
          } else {
            obj.forEach(function(item){
              if (filter == "pending") {
                item.set('isVisible', ["pending", "queued"].contains(item.status));
              } else if (filter == "in_progress") {
                item.set('isVisible', ["in_progress", "upgrading"].contains(item.status));
              } else if (filter == "failed") {
                item.set('isVisible', (item.status === "failed"));
              } else if (filter == "completed") {
                item.set('isVisible', ["completed", "success"].contains(item.status));
              } else if (filter == "aborted") {
                item.set('isVisible', (item.status === "aborted"));
              } else if (filter == "timedout") {
                item.set('isVisible', (item.status === "timedout"));
              }
              isEmptyList = (isEmptyList) ? !item.get('isVisible') : false;
            })
          }
          return isEmptyList;
        },

        /**
         * Select box, display names and values
         */
        categories: [
          categoryObject.create({value: 'all', labelPath: 'hostPopup.status.category.all'}),
          categoryObject.create({value: 'pending', labelPath: 'hostPopup.status.category.pending'}),
          categoryObject.create({value: 'in_progress', labelPath: 'hostPopup.status.category.inProgress'}),
          categoryObject.create({value: 'failed', labelPath: 'hostPopup.status.category.failed'}),
          categoryObject.create({value: 'completed', labelPath: 'hostPopup.status.category.success'}),
          categoryObject.create({value: 'aborted', labelPath: 'hostPopup.status.category.aborted'}),
          categoryObject.create({value: 'timedout', labelPath: 'hostPopup.status.category.timedout'})
        ],

        /**
         * Selected option is binded to this values
         */
        serviceCategory: null,
        hostCategory: null,
        taskCategory: null,

        /**
         * Depending on currently viewed tab, call setSelectCount function
         */
        updateSelectView: function () {
          if (!this.get('isHostListHidden')) {
            //since lazy loading used for hosts, we need to get hosts info directly from controller, that always contains entire array of data
            this.get('controller').setSelectCount(this.get("controller.hosts"), this.get('categories'));
          } else if (!this.get('isTaskListHidden')) {
            this.get('controller').setSelectCount(this.get("tasks"), this.get('categories'));
          } else if (!this.get('isServiceListHidden')) {
            this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
          }
        }.observes('tasks.@each.status', 'hosts.@each.status', 'isTaskListHidden', 'isHostListHidden', 'services.length', 'services.@each.status'),

        /**
         * control data uploading, depending on which display level is showed
         * @param levelName
         */
        switchLevel: function (levelName) {
          if (this.get("controller.isBackgroundOperations")) {
            var BGController = App.router.get('backgroundOperationsController');
            var levelInfo = BGController.get('levelInfo');
            levelInfo.set('taskId', this.get('openedTaskId'));
            levelInfo.set('requestId', this.get('controller.currentServiceId'));
            levelInfo.set('name', levelName);
            if (levelName === 'HOSTS_LIST') {
              levelInfo.set('sync', (this.get('controller.hosts').length === 0));
              BGController.requestMostRecent();
            } else if (levelName === 'TASK_DETAILS') {
              levelInfo.set('sync', true);
              BGController.requestMostRecent();
            } else if (levelName === 'REQUESTS_LIST') {
              this.get('controller.hosts').clear();
              BGController.requestMostRecent();
            }
          }
        },
        /**
         * Onclick handler for button <-Tasks
         */
        backToTaskList: function () {
          this.destroyClipBoard();
          this.set("openedTaskId", 0);
          this.set("isLogWrapHidden", true);
          this.set("isTaskListHidden", false);
          this.switchLevel("TASKS_LIST");
        },

        /**
         * Onclick handler for button <-Hosts
         */
        backToHostList: function () {
          this.set("isHostListHidden", false);
          this.set("isTaskListHidden", true);
          this.get("controller").set("popupHeaderName", this.get("controller.serviceName"));
          this.switchLevel("HOSTS_LIST");
        },

        /**
         * Onclick handler for button <-Services
         */
        backToServiceList: function () {
          this.get("controller").set("serviceName", "");
          this.set("isHostListHidden", true);
          this.set("isServiceListHidden", false);
          this.set("isTaskListHidden", true);
          this.set("hosts", null);
          this.get("controller").setBackgroundOperationHeader(false);
          this.switchLevel("REQUESTS_LIST");
        },

        /**
         * Onclick handler for selected Service
         * @param {Object} event
         */
        gotoHosts: function (event) {
          this.get("controller").set("serviceName", event.context.get("name"));
          this.get("controller").set("currentServiceId", event.context.get("id"));
          this.get("controller").set("currentHostName", null);
          this.get("controller").onHostUpdate();
          this.switchLevel("HOSTS_LIST");
          var servicesInfo = this.get("controller.hosts");
          if (servicesInfo.length) {
            this.get("controller").set("popupHeaderName", event.context.get("name"));
          }
          //apply lazy loading on cluster with more than 100 nodes
          if (servicesInfo.length > 100) {
            this.set('hosts', servicesInfo.slice(0, 50));
          } else {
            this.set('hosts', servicesInfo);
          }
          this.set("isServiceListHidden", true);
          this.set("isHostListHidden", false);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
          if (servicesInfo.length > 100) {
            Em.run.next(this, function(){
              this.set('hosts', this.get('hosts').concat(servicesInfo.slice(50, servicesInfo.length)));
            });
          }
          // Determine if source request schedule is present
          this.set('sourceRequestScheduleId', event.context.get("sourceRequestScheduleId"));
          this.set('sourceRequestScheduleCommand', event.context.get('contextCommand'));
          this.refreshRequestScheduleInfo();
        },

        isRequestSchedule : function() {
          var id = this.get('sourceRequestScheduleId');
          return id != null && !isNaN(id) && id > -1;
        }.property('sourceRequestScheduleId'),

        refreshRequestScheduleInfo : function() {
          var self = this;
          var id = this.get('sourceRequestScheduleId');
          batchUtils.getRequestSchedule(id, function(data) {
            if (data != null && data.RequestSchedule != null &&
                data.RequestSchedule.status != null) {
              switch (data.RequestSchedule.status) {
              case 'DISABLED':
                self.set('sourceRequestScheduleRunning', false);
                self.set('sourceRequestScheduleAborted', true);
                break;
              case 'COMPLETED':
                self.set('sourceRequestScheduleRunning', false);
                self.set('sourceRequestScheduleAborted', false);
                break;
              case 'SCHEDULED':
                self.set('sourceRequestScheduleRunning', true);
                self.set('sourceRequestScheduleAborted', false);
                break;
              }
            } else {
              self.set('sourceRequestScheduleRunning', false);
              self.set('sourceRequestScheduleAborted', false);
            }
          }, function(xhr, textStatus, error, opt) {
            console.log("Error getting request schedule information: ", textStatus, error, opt);
            self.set('sourceRequestScheduleRunning', false);
            self.set('sourceRequestScheduleAborted', false);
          });
        }.observes('sourceRequestScheduleId'),

        /**
         * Attempts to abort the current request schedule
         */
        doAbortRequestSchedule: function(event){
          var self = this;
          var id = event.context;
          console.log("Aborting request schedule: ", id);
          batchUtils.doAbortRequestSchedule(id, function(){
            self.refreshRequestScheduleInfo();
          });
        },

        requestScheduleAbortLabel : function() {
          var label = Em.I18n.t("common.abort");
          var command = this.get('sourceRequestScheduleCommand');
          if (command != null && "ROLLING-RESTART" == command) {
            label = Em.I18n.t("hostPopup.bgop.abort.rollingRestart");
          }
          return label;
        }.property('sourceRequestScheduleCommand'),

        /**
         * Onclick handler for selected Host
         * @param {Object} event
         */
        gotoTasks: function (event) {
          var tasksInfo = [];
          event.context.logTasks.forEach(function (_task) {
            tasksInfo.pushObject(this.get("controller").createTask(_task));
          }, this);
          if (tasksInfo.length) {
            this.get("controller").set("popupHeaderName", event.context.publicName);
            this.get("controller").set("currentHostName", event.context.publicName);
          }
          this.switchLevel("TASKS_LIST");
          this.set('currentHost.tasks', tasksInfo);
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", false);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        /**
         * Onclick handler for selected Task
         */
        openTaskLogInDialog: function () {
          if ($(".task-detail-log-clipboard").length > 0) {
            this.destroyClipBoard();
          }
          var newWindow = window.open();
          var newDocument = newWindow.document;
          newDocument.write($(".task-detail-log-info").html());
          newDocument.close();
        },

        openedTaskId: 0,

        /**
         * Return task detail info of opened task
         */
        openedTask: function () {
          if (!(this.get('openedTaskId') && this.get('tasks'))) {
            return Ember.Object.create();
          }
          return this.get('tasks').findProperty('id', this.get('openedTaskId'));
        }.property('tasks', 'tasks.@each.stderr', 'tasks.@each.stdout', 'openedTaskId'),

        /**
         * Onclick event for show task detail info
         * @param {Object} event
         */
        toggleTaskLog: function (event) {
          var taskInfo = event.context;
          this.set("isLogWrapHidden", false);
          if ($(".task-detail-log-clipboard").length > 0) {
            this.destroyClipBoard();
          }
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", true);
          this.set('openedTaskId', taskInfo.id);
          this.switchLevel("TASK_DETAILS");
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        /**
         * Onclick event for copy to clipboard button
         */
        textTrigger: function () {
          $(".task-detail-log-clipboard").length > 0 ? this.destroyClipBoard() : this.createClipBoard();
        },

        /**
         * Create Clip Board
         */
        createClipBoard: function () {
          var logElement = $(".task-detail-log-maintext");
          $(".task-detail-log-clipboard-wrap").html('<textarea class="task-detail-log-clipboard"></textarea>');
          $(".task-detail-log-clipboard")
            .html("stderr: \n" + $(".stderr").html() + "\n stdout:\n" + $(".stdout").html())
            .css("display", "block")
            .width(logElement.width())
            .height(logElement.height())
            .select();
          logElement.css("display", "none")
        },

        /**
         * Destroy Clip Board
         */
        destroyClipBoard: function () {
          $(".task-detail-log-clipboard").remove();
          $(".task-detail-log-maintext").css("display", "block");
        }

      })
    }));
    return self.get('isPopup');
  }

});