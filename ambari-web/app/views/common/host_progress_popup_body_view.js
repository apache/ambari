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
var date = require('utils/date/date');

/**
 * @typedef {object} TaskRelationObject
 * @property {string} type relation type 'service', 'component'
 * @property {string} [value] optional value of relation e.g. name of component or service
 */

/**
 * Option for "filter by state" dropdown
 * @typedef {object} progressPopupCategoryObject
 * @property {string} value "all|pending|in progress|failed|completed|aborted|timedout"
 * @property {number} count number of items with <code>state</code> equal to <code>this.value</code>
 * @property {string} labelPath key in the messages.js
 * @property {string} label localized label
 */
var categoryObject = Em.Object.extend({
  value: '',
  count: 0,
  labelPath: '',
  label: function () {
    return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
  }.property('count', 'labelPath')
});

/**
 * @class HostProgressPopupBodyView
 * @type {Em.View}
 */
App.HostProgressPopupBodyView = App.TableView.extend({

  templateName: require('templates/common/host_progress_popup'),

  /**
   * @type {boolean}
   */
  showTextArea: false,

  /**
   * @type {boolean}
   */
  isServiceEmptyList: true,

  /**
   * @type {boolean}
   */
  isTasksEmptyList: true,

  /**
   * @type {number}
   */
  sourceRequestScheduleId: -1,

  /**
   * @type {boolean}
   */
  sourceRequestScheduleRunning: false,

  /**
   * @type {boolean}
   */
  sourceRequestScheduleAborted: false,

  /**
   * @type {?string}
   */
  sourceRequestScheduleCommand: null,

  /**
   * Alias for <code>controller.hosts</code>
   *
   * @type {wrappedHost[]}
   */
  hosts: function () {
    return this.get('controller.hosts')
  }.property('controller.hosts.[]'),

  /**
   * Alias for <code>controller.servicesInfo</code>
   *
   * @type {wrappedService[]}
   */
  services: function () {
    return this.get('controller.servicesInfo');
  }.property('controller.servicesInfo.[]'),

  /**
   * @type {number}
   */
  openedTaskId: 0,

  /**
   * Return task detail info of opened task
   *
   * @type {wrappedTask}
   */
  openedTask: function () {
    if (!(this.get('openedTaskId') && this.get('tasks'))) {
      return Em.Object.create();
    }
    return this.get('tasks').findProperty('id', this.get('openedTaskId'));
  }.property('tasks', 'tasks.@each.stderr', 'tasks.@each.stdout', 'openedTaskId'),

  /**
   * @type {object}
   */
  filterMap: {
    pending: ["pending", "queued"],
    in_progress: ["in_progress", "upgrading"],
    failed: ["failed"],
    completed: ["completed", "success"],
    aborted: ["aborted"],
    timedout: ["timedout"]
  },

  /**
   * Determines if "Show More ..."-link should be shown
   * @type {boolean}
   */
  isShowMore: true,

  /**
   * @type {boolean}
   */
  pagination: true,

  /**
   * @type {boolean}
   */
  isPaginate: false,

  /**
   * Select box, display names and values
   *
   * @type {progressPopupCategoryObject[]}
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
   * Selected option is bound to this values
   * @type {?progressPopupCategoryObject}
   */
  serviceCategory: null,

  /**
   * @type {?progressPopupCategoryObject}
   */
  hostCategory: null,

  /**
   * @type {?progressPopupCategoryObject}
   */
  taskCategory: null,

  /**
   * flag to indicate whether level data has already been loaded
   * applied only to HOSTS_LIST and TASK_DETAILS levels, whereas async query used to obtain data
   *
   * @type {boolean}
   */
  isLevelLoaded: true,

  /**
   * <code>switchLevel</code> for some <code>controller.dataSourceController</code> should be customized
   * So, this map contains information about customize-methods
   * Format: key - <code>dataSourceController.name</code>, value - method name in this view
   * Method is called with same arguments as <code>switchLevel</code> is
   *
   * @type {object}
   */
  customControllersSwitchLevelMap: {
    highAvailabilityProgressPopupController: '_switchLevelForHAProgressPopupController'
  },

  /**
   * @type {boolean}
   */
  isHostEmptyList: Em.computed.empty('pageContent'),

  /**
   * @type {wrappedHost}
   */
  currentHost: function () {
    return this.get('hosts') && this.get('hosts').findProperty('name', this.get('controller.currentHostName'));
  }.property('controller.currentHostName'),

  /**
   * Tasks for current shown host (<code>currentHost</code>)
   *
   * @type {wrappedTask[]}
   */
  tasks: function () {
    var currentHost = this.get('currentHost');
    return currentHost ? currentHost.get('tasks') : [];
  }.property('currentHost.tasks', 'currentHost.tasks.@each.status'),


  /**
   * Message about aborting operation
   * Custom for Rolling Restart
   *
   * @type {string}
   */
  requestScheduleAbortLabel: function () {
    return 'ROLLING-RESTART' == this.get('sourceRequestScheduleCommand') ?
      Em.I18n.t("hostPopup.bgop.abort.rollingRestart"):
      Em.I18n.t("common.abort");
  }.property('sourceRequestScheduleCommand'),


  willDestroyElement: function () {
    if (this.get('controller.dataSourceController.name') == 'highAvailabilityProgressPopupController') {
      this.set('controller.dataSourceController.isTaskPolling', false);
    }
  },

  /**
   * Preset values on init
   *
   * @method setOnStart
   */
  setOnStart: function () {
    this.set('serviceCategory', this.get('categories').findProperty('value', 'all'));
    if (this.get("controller.isBackgroundOperations")) {
      this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
      this.updateHostInfo();
    }
    else {
      this.set("parentView.isHostListHidden", false);
      this.set("parentView.isServiceListHidden", true);
    }
  },

  /**
   * force popup to show list of operations
   *
   * @method resetState
   */
  resetState: function () {
    if (this.get('parentView.isOpen')) {
      this.get('parentView').setProperties({
        isLogWrapHidden: true,
        isTaskListHidden: true,
        isHostListHidden: true,
        isServiceListHidden: false
      });
      this.get("controller").setBackgroundOperationHeader(false);
      this.setOnStart();
    }
  }.observes('parentView.isOpen'),

  /**
   * When popup is opened, and data after polling has changed, update this data in component
   *
   * @method updateHostInfo
   */
  updateHostInfo: function () {
    if (!this.get('parentView.isOpen')) {
      return;
    }
    this.set('parentView.isLoaded', false);
    this.get("controller").set("inputData", this.get("controller.dataSourceController.services"));
    this.get("controller").onServiceUpdate(this.get('parentView.isServiceListHidden'));
    this.get("controller").onHostUpdate();
    this.set('parentView.isLoaded', true);
    this.set("hosts", this.get("controller.hosts"));
    this.set("services", this.get("controller.servicesInfo"));
    this.set('isLevelLoaded', true);
  }.observes("controller.dataSourceController.serviceTimestamp"),

  /**
   * Depending on service filter, set which services should be shown
   *
   * @method visibleServices
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
   *
   * @method filter
   */
  filter: function () {
    var filter = this.get('hostCategory.value');
    var hosts = this.get('hosts') || [];
    var filterMap = this.get('filterMap');
    if (!filter || !hosts.length) {
      return;
    }
    if (filter === 'all') {
      this.set('filteredContent', hosts);
    }
    else {
      this.set('filteredContent', hosts.filter(function (item) {
        return filterMap[filter].contains(item.status);
      }));
    }
  }.observes('hosts.length', 'hostCategory.value'),

  /**
   * Reset startIndex property back to 1 when filter type has been changed.
   *
   * @method resetIndex
   */
  resetIndex: function () {
    if (this.get('hostCategory.value')) {
      this.set('startIndex', 1);
    }
  }.observes('hostCategory.value'),

  /**
   * Depending on tasks filter, set which tasks should be shown
   *
   * @method visibleTasks
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
   *
   * @param filter
   * @param obj
   * @return {bool} isEmptyList
   * @method setVisibility
   */
  setVisibility: function (filter, obj) {
    var isEmptyList = true;
    var filterMap = this.get('filterMap');
    if (filter == "all") {
      obj.setEach("isVisible", true);
      isEmptyList = !obj.length;
    }
    else {
      obj.forEach(function (item) {
        item.set('isVisible', filterMap[filter].contains(item.status));
        isEmptyList = (isEmptyList) ? !item.get('isVisible') : false;
      }, this)
    }
    return isEmptyList;
  },

  /**
   * Depending on currently viewed tab, call setSelectCount function
   *
   * @method updateSelectView
   */
  updateSelectView: function () {
    var isPaginate;
    if (this.get('parentView.isHostListHidden')) {
      if (this.get('parentView.isTaskListHidden')) {
        if (!this.get('parentView.isServiceListHidden')) {
          this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
        }
      }
      else {
        this.get('controller').setSelectCount(this.get("tasks"), this.get('categories'));
      }
    }
    else {
      //since lazy loading used for hosts, we need to get hosts info directly from controller, that always contains entire array of data
      this.get('controller').setSelectCount(this.get("controller.hosts"), this.get('categories'));
      isPaginate = true;

    }
    this.set('isPaginate', !!isPaginate);
  }.observes('tasks.@each.status', 'hosts.@each.status', 'parentView.isTaskListHidden', 'parentView.isHostListHidden', 'services.length', 'services.@each.status'),

  /**
   * control data uploading, depending on which display level is showed
   *
   * @param {string} levelName
   * @method switchLevel
   */
  switchLevel: function (levelName) {
    var dataSourceController = this.get('controller.dataSourceController');
    var args = [].slice.call(arguments);
    if (this.get("controller.isBackgroundOperations")) {
      var levelInfo = dataSourceController.get('levelInfo');
      levelInfo.set('taskId', this.get('openedTaskId'));
      levelInfo.set('requestId', this.get('controller.currentServiceId'));
      levelInfo.set('name', levelName);
      if (levelName === 'HOSTS_LIST') {
        this.set('isLevelLoaded', dataSourceController.requestMostRecent());
        this.set('hostCategory', this.get('categories').findProperty('value', 'all'));
      }
      else {
        if (levelName === 'TASK_DETAILS') {
          dataSourceController.requestMostRecent();
          this.set('isLevelLoaded', false);
        }
        else {
          if (levelName === 'REQUESTS_LIST') {
            this.set('serviceCategory', this.get('categories').findProperty('value', 'all'));
            this.get('controller.hosts').clear();
            dataSourceController.requestMostRecent();
          }
          else {
            this.set('taskCategory', this.get('categories').findProperty('value', 'all'));
          }
        }
      }
    }
    else {
      var customControllersSwitchLevelMap = this.get('customControllersSwitchLevelMap');
      Em.tryInvoke(this, customControllersSwitchLevelMap[dataSourceController.get('name')], args);
    }
  },

  /**
   * Switch-level custom method for <code>highAvailabilityProgressPopupController</code>
   *
   * @param {string} levelName
   * @private
   */
  _switchLevelForHAProgressPopupController: function (levelName) {
    var dataSourceController = this.get('controller.dataSourceController');
    if (levelName === 'TASK_DETAILS') {
      this.set('isLevelLoaded', false);
      dataSourceController.startTaskPolling(this.get('openedTask.request_id'), this.get('openedTask.id'));
      Em.keys(this.get('parentView.detailedProperties')).forEach(function (key) {
        dataSourceController.addObserver('taskInfo.' + this.get('parentView.detailedProperties')[key], this, 'updateTaskInfo');
      }, this);
    }
    else {
      dataSourceController.stopTaskPolling();
    }
  },

  /**
   * @method updateTaskInfo
   */
  updateTaskInfo: function () {
    var dataSourceController = this.get('controller.dataSourceController');
    var openedTask = this.get('openedTask');
    if (openedTask && openedTask.get('id') == dataSourceController.get('taskInfo.id')) {
      this.set('isLevelLoaded', true);
      Em.keys(this.get('parentView.detailedProperties')).forEach(function (key) {
        openedTask.set(key, dataSourceController.get('taskInfo.' + key));
      }, this);
    }
  },

  /**
   * Onclick handler for button <-Tasks
   *
   * @method backToTaskList
   */
  backToTaskList: function () {
    this.destroyClipBoard();
    this.set("openedTaskId", 0);
    this.set("parentView.isLogWrapHidden", true);
    this.set("parentView.isTaskListHidden", false);
    this.switchLevel("TASKS_LIST");
  },

  /**
   * Onclick handler for button <-Hosts
   *
   * @method backToHostList
   */
  backToHostList: function () {
    this.set("parentView.isHostListHidden", false);
    this.set("parentView.isTaskListHidden", true);
    this.get("controller").set("popupHeaderName", this.get("controller.serviceName"));
    this.get("controller").set("operationInfo", this.get('controller.servicesInfo').findProperty('name', this.get('controller.serviceName')));
    this.switchLevel("HOSTS_LIST");
  },

  /**
   * Onclick handler for button <-Services
   *
   * @method backToServiceList
   */
  backToServiceList: function () {
    this.get("controller").set("serviceName", "");
    this.set("parentView.isHostListHidden", true);
    this.set("parentView.isServiceListHidden", false);
    this.set("parentView.isTaskListHidden", true);
    this.set("parentView.isLogWrapHidden", true);
    this.set("hosts", null);
    this.get("controller").setBackgroundOperationHeader(false);
    this.switchLevel("REQUESTS_LIST");
  },

  /**
   * Onclick handler for Show more ..
   *
   * @method requestMoreOperations
   */
  requestMoreOperations: function () {
    var BGOController = App.router.get('backgroundOperationsController');
    var count = BGOController.get('operationsCount');
    BGOController.set('operationsCount', (count + 10));
    BGOController.requestMostRecent();
  },

  /**
   * @method setShowMoreAvailable
   */
  setShowMoreAvailable: function () {
    if (this.get('parentView.isOpen')) {
      this.set('isShowMore', App.router.get("backgroundOperationsController.isShowMoreAvailable"));
    }
  }.observes('parentView.isOpen', 'App.router.backgroundOperationsController.isShowMoreAvailable'),

  /**
   * Onclick handler for selected Service
   *
   * @param {{context: wrappedService}} event
   * @method gotoHosts
   */
  gotoHosts: function (event) {
    this.get("controller").set("serviceName", event.context.get("name"));
    this.get("controller").set("currentServiceId", event.context.get("id"));
    this.get("controller").set("currentHostName", null);
    this.get("controller").onHostUpdate();
    this.switchLevel("HOSTS_LIST");
    var servicesInfo = this.get("controller.hosts");
    this.set("controller.popupHeaderName", event.context.get("name"));
    this.set("controller.operationInfo", event.context);

    //apply lazy loading on cluster with more than 100 nodes
    this.set('hosts', servicesInfo.length > 100 ? servicesInfo.slice(0, 50) : servicesInfo);
    this.set("parentView.isServiceListHidden", true);
    this.set("parentView.isHostListHidden", false);
    this.set("parentView.isTaskListHidden", true);
    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);
    if (servicesInfo.length > 100) {
      Em.run.next(this, function () {
        this.set('hosts', this.get('hosts').concat(servicesInfo.slice(50, servicesInfo.length)));
      });
    }
    // Determine if source request schedule is present
    this.set('sourceRequestScheduleId', event.context.get("sourceRequestScheduleId"));
    this.set('sourceRequestScheduleCommand', event.context.get('contextCommand'));
    this.refreshRequestScheduleInfo();
  },

  /**
   * Navigate to host details logs tab with preset filter.
   */
  navigateToHostLogs: function() {
    var relationType = this._determineRoleRelation(this.get('openedTask')),
        hostModel = App.Host.find().findProperty('id', this.get('currentHost.name')),
        queryParams = [],
        model;

    if (relationType.type === 'component') {
      model = App.StackServiceComponent.find().findProperty('componentName', relationType.value);
      queryParams.push('service_name=' + model.get('serviceName'));
      queryParams.push('component_name=' + relationType.value);
    }
    if (relationType.type === 'service') {
      queryParams.push('service_name=' + relationType.value);
    }
    App.router.transitionTo('main.hosts.hostDetails.logs', hostModel, { query: '?' + queryParams.join('&') });
    if (this.get('parentView') && typeof this.get('parentView').onClose === 'function') this.get('parentView').onClose();
  },

  /**
  /**
  * Determines if opened task related to service or component.
  *
  * @return {boolean} <code>true</code> when relates to service or component
  */
  isLogsLinkVisible: function() {
    if (!this.get('openedTask') || !this.get('openedTask.id')) return false;
    return !!this._determineRoleRelation(this.get('openedTask'));
  }.property('openedTask'),

  /**
   * @param  {wrappedTask} taskInfo
   * @return {boolean|TaskRelationObject}
   */
  _determineRoleRelation: function(taskInfo) {
    var foundComponentName,
        foundServiceName,
        componentNames = App.StackServiceComponent.find().mapProperty('componentName'),
        serviceNames = App.StackService.find().mapProperty('serviceName'),
        taskLog = this.get('currentHost.logTasks').findProperty('Tasks.id', Em.get(taskInfo, 'id')) || {},
        role = Em.getWithDefault(taskLog, 'Tasks.role', false),
        eqlFn = function(compare) {
          return function(item) {
            return item === compare;
          };
        };

    if (!role) {
      return false;
    }
    // component service check
    if (role.endsWith('_SERVICE_CHECK')) {
      role = role.replace('_SERVICE_CHECK', '');
    }
    foundComponentName = componentNames.filter(eqlFn(role))[0];
    foundServiceName = serviceNames.filter(eqlFn(role))[0];
    if (foundComponentName || foundServiceName) {
      return {
        type: foundComponentName ? 'component' : 'service',
        value: foundComponentName || foundServiceName
      }
    }
    return false;
  },

  /**
   * @type {boolean}
   */
  isRequestSchedule: function () {
    var id = this.get('sourceRequestScheduleId');
    return id != null && !isNaN(id) && id > -1;
  }.property('sourceRequestScheduleId'),

  /**
   * @method refreshRequestScheduleInfo
   */
  refreshRequestScheduleInfo: function () {
    var self = this;
    var id = this.get('sourceRequestScheduleId');
    batchUtils.getRequestSchedule(id, function (data) {
      var status = Em.get(data || {}, 'RequestSchedule.status');
      if (status) {
        switch (status) {
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
      }
      else {
        self.set('sourceRequestScheduleRunning', false);
        self.set('sourceRequestScheduleAborted', false);
      }
    }, function () {
      self.set('sourceRequestScheduleRunning', false);
      self.set('sourceRequestScheduleAborted', false);
    });
  }.observes('sourceRequestScheduleId'),

  /**
   * Attempts to abort the current request schedule
   *
   * @param {{context: number}} event
   * @method doAbortRequestSchedule
   */
  doAbortRequestSchedule: function (event) {
    var self = this;
    var id = event.context;
    batchUtils.doAbortRequestSchedule(id, function () {
      self.refreshRequestScheduleInfo();
    });
  },

  /**
   * Onclick handler for selected Host
   *
   * @param {{context: wrappedHost}} event
   * @method gotoTasks
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
    this.set("parentView.isHostListHidden", true);
    this.set("parentView.isTaskListHidden", false);
    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);
  },

  /**
   * @method stopRebalanceHDFS
   * @returns {App.ModalPopup}
   */
  stopRebalanceHDFS: function () {
    var hostPopup = this;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'cancel.background.operation',
        sender: hostPopup,
        data: {
          requestId: hostPopup.get('controller.currentServiceId')
        }
      });
      hostPopup.backToServiceList();
    });
  },

  /**
   * Onclick handler for selected Task
   *
   * @method openTaskLogInDialog
   */
  openTaskLogInDialog: function () {
    if ($(".task-detail-log-clipboard").length) {
      this.destroyClipBoard();
    }
    var newWindow = window.open();
    var newDocument = newWindow.document;
    newDocument.write($(".task-detail-log-info").html());
    newDocument.close();
  },

  /**
   * Onclick event for show task detail info
   *
   * @param {{context: wrappedTask}} event
   * @method toggleTaskLog
   */
  toggleTaskLog: function (event) {
    var taskInfo = event.context;
    this.set("parentView.isLogWrapHidden", false);
    if ($(".task-detail-log-clipboard").length) {
      this.destroyClipBoard();
    }
    this.set("parentView.isHostListHidden", true);
    this.set("parentView.isTaskListHidden", true);
    this.set('openedTaskId', taskInfo.id);
    this.switchLevel("TASK_DETAILS");
    $(".modal").scrollTop(0);
    $(".modal-body").scrollTop(0);
  },

  /**
   * Onclick event for copy to clipboard button
   *
   * @method textTrigger
   */
  textTrigger: function () {
    $(".task-detail-log-clipboard").length ? this.destroyClipBoard() : this.createClipBoard();
  },

  /**
   * Create Clip Board
   *
   * @method createClipBoard
   */
  createClipBoard: function () {
    var logElement = $(".task-detail-log-maintext"),
      logElementRect = logElement[0].getBoundingClientRect();
    $(".task-detail-log-clipboard-wrap").html('<textarea class="task-detail-log-clipboard"></textarea>');
    $(".task-detail-log-clipboard")
      .html("stderr: \n" + $(".stderr").html() + "\n stdout:\n" + $(".stdout").html())
      .css('display', 'block')
      .width(logElementRect.width)
      .height(logElementRect.height)
      .select();

    logElement.css("display", "none");
  },

  /**
   * Destroy Clip Board
   *
   * @method destroyClipBoard
   */
  destroyClipBoard: function () {
    $(".task-detail-log-clipboard").remove();
    $(".task-detail-log-maintext").css("display", "block");
  }

});
