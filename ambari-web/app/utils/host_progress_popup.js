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
 * App.HostPopup is for the popup that shows up upon clicking already-performed or currently-in-progress operations
 */
App.HostPopup = Em.Object.create({

  servicesInfo: null,
  hosts: null,
  inputData: null,
  serviceName: "",
  popupHeaderName: "",
  serviceController: null,
  showServices: false,

  /**
   * Sort object array
   * @param array
   * @param p
   * @return {*}
   */
  sortArray: function (array, p) {
    return array.sort(function (a, b) {
      return (a[p] > b[p]) ? 1 : (a[p] < b[p]) ? -1 : 0;
    });
  },

  /**
   * Entering point of this component
   * @param serviceName
   * @param controller
   * @param showServices
   */
  initPopup: function (serviceName, controller, showServices) {
    this.set("serviceName", serviceName);
    this.set("serviceController", controller);
    if (showServices) {
      this.set("showServices", true);
    } else {
      this.set("showServices", false);
      this.set("popupHeaderName", serviceName);
    }
    this.set("hosts", null);
    this.set("servicesInfo", null);
    this.set("inputData", null);
    this.set("inputData", this.get("serviceController.services"));
    this.createPopup();
  },

  /**
   * Depending on tasks status
   * @param tasks
   * @return {Array} [Status, Icon type, Progressbar color, is IN_PROGRESS]
   */
  getStatus: function (tasks) {
    if (tasks.everyProperty('Tasks.status', 'COMPLETED')) {
      return ['SUCCESS', 'icon-ok', 'progress-success', false];
    }
    if (tasks.someProperty('Tasks.status', 'FAILED')) {
      return ['FAILED', 'icon-exclamation-sign', 'progress-danger', false];
    }
    if (tasks.someProperty('Tasks.status', 'ABORTED')) {
      return ['CANCELLED', 'icon-minus', 'progress-warning', false];
    }
    if (tasks.someProperty('Tasks.status', 'TIMEDOUT')) {
      return ['TIMEDOUT', 'icon-time', 'progress-warning', false];
    }
    if (tasks.someProperty('Tasks.status', 'IN_PROGRESS')) {
      return ['IN_PROGRESS', 'icon-cogs', 'progress-info', true];
    }
    return ['PENDING', 'icon-cog', 'progress-info', true];
  },

  /**
   * Progress of host or service depending on tasks status
   * @param tasks
   * @return {Number} percent of completion
   */
  getProgress: function (tasks) {
    var progress = 0;
    var actionsNumber = tasks.length;
    var completedActions = tasks.filterProperty('Tasks.status', 'COMPLETED').length
        + tasks.filterProperty('Tasks.status', 'FAILED').length
        + tasks.filterProperty('Tasks.status', 'ABORTED').length
        + tasks.filterProperty('Tasks.status', 'TIMEDOUT').length;
    var queuedActions = tasks.filterProperty('Tasks.status', 'QUEUED').length;
    var inProgressActions = tasks.filterProperty('Tasks.status', 'IN_PROGRESS').length;
    progress = Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsNumber * 100);
    return progress;
  },

  /**
   * For Background operation popup calculate number of running Operations, and set popup header
   */
  setBackgroundOperationHeader: function () {
    var allServices = this.get("servicesInfo");
    var numRunning = 0;
    numRunning = allServices.filterProperty("status", App.format.taskStatus("IN_PROGRESS")).length;
    numRunning += allServices.filterProperty("status", App.format.taskStatus("QUEUED")).length;
    numRunning += allServices.filterProperty("status", App.format.taskStatus("PENDING")).length;
    this.set("popupHeaderName", numRunning + " Background operations Running");
  },

  /**
   * Create services obj data structure for popup
   * Set data for services
   */
  onServiceUpdate: function () {
    if (this.showServices && this.get("inputData")) {
      var self = this;
      var allNewServices = [];
      this.set("servicesInfo", null);
      this.get("inputData").forEach(function (service) {
        var newService = Ember.Object.create({
          displayName: service.displayName,
          detailMessage: service.detailMessage,
          message: service.message,
          progress: 0,
          status: App.format.taskStatus("PENDING"),
          name: service.name,
          isVisible: true,
          icon: 'icon-cog',
          barColor: 'progress-info',
          barWidth: 'width:0%;'
        });
        var allTasks = []
        service.hosts.forEach(function (tasks) {
          tasks.logTasks.forEach(function (task) {
            allTasks.push(task);
          });
        });
        if (allTasks.length > 0) {
          var status = self.getStatus(allTasks);
          var progress = self.getProgress(allTasks);
          newService.set('status', App.format.taskStatus(status[0]));
          newService.set('icon', status[1]);
          newService.set('barColor', status[2]);
          newService.set('isInProgress', status[3]);
          newService.set('progress', progress);
          newService.set('barWidth', "width:" + progress + "%;");
        }
        allNewServices.push(newService);
      })
      self.set('servicesInfo', allNewServices);
      if (this.get("serviceName") == "")
        this.setBackgroundOperationHeader();
    }
  }.observes("this.inputData"),

  /**
   * Create hosts and tasks data structure for popup
   * Set data for hosts and tasks
   */
  onHostUpdate: function () {
    var self = this;
    if (this.get("inputData")) {
      var hostsArr = [];
      var hostsData = this.get("inputData")
      var hosts = [];
      if (this.get("showServices") && this.get("serviceName") == "") {
        hostsData.forEach(function (service) {
          var host = service.hosts;
          host.setEach("serviceName", service.name);
          hosts.push.apply(hosts, host);
        });
      } else {
        hostsData = hostsData.filterProperty("name", this.get("serviceName")).objectAt(0);
        hosts = hostsData.hosts;
        hosts.setEach("serviceName", this.get("serviceName"));
      }
    }

    if (hosts) {
      /**
       * sort host names by name value
       */
      this.sortArray(hosts, "name");

      hosts.forEach(function (_host) {
        var tasks = _host.logTasks;
        var hostInfo = Ember.Object.create({});
        hostInfo.set('name', _host.name);
        hostInfo.set('publicName', _host.publicName);
        hostInfo.set('progress', 0);
        hostInfo.set('status', App.format.taskStatus("PENDING"));
        hostInfo.set('serviceName', _host.serviceName);
        hostInfo.set('isVisible', true);
        hostInfo.set('icon', "icon-cog");
        hostInfo.set('barColor', "progress-info");
        hostInfo.set('barWidth', "width:0%;");

        tasks = self.sortTasksById(tasks);
        var tasksArr = [];

        if (tasks.length) {

          var hostStatus = self.getStatus(tasks);
          var hostProgress = self.getProgress(tasks);
          hostInfo.set('status', App.format.taskStatus(hostStatus[0]));
          hostInfo.set('icon', hostStatus[1]);
          hostInfo.set('barColor', hostStatus[2]);
          hostInfo.set('isInProgress', hostStatus[3]);
          hostInfo.set('progress', hostProgress);
          hostInfo.set('barWidth', "width:" + hostProgress + "%;");

          tasks.forEach(function (_task) {
            var taskInfo = Ember.Object.create({});
            taskInfo.set('id', _task.Tasks.id);
            taskInfo.set('hostName', _host.publicName);
            taskInfo.set('command', _task.Tasks.command.toLowerCase());
            taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
            taskInfo.set('role', App.format.role(_task.Tasks.role));
            taskInfo.set('stderr', _task.Tasks.stderr);
            taskInfo.set('stdout', _task.Tasks.stdout);
            taskInfo.set('isVisible', true);
            taskInfo.set('icon', 'icon-cogs');
            if (taskInfo.get('status') == 'pending' || taskInfo.get('status') == 'queued') {
              taskInfo.set('icon', 'icon-cog');
            } else if (taskInfo.get('status') == 'in_progress') {
              taskInfo.set('icon', 'icon-cogs');
            } else if (taskInfo.get('status') == 'completed') {
              taskInfo.set('icon', ' icon-ok');
            } else if (taskInfo.get('status') == 'failed') {
              taskInfo.set('icon', 'icon-exclamation-sign');
            } else if (taskInfo.get('status') == 'aborted') {
              taskInfo.set('icon', 'icon-minus');
            } else if (taskInfo.get('status') == 'timedout') {
              taskInfo.set('icon', 'icon-time');
            }
            tasksArr.push(taskInfo);
          }, this);
        }

        hostInfo.set('tasks', tasksArr);
        hostsArr.push(hostInfo);
      }, this);
    }

    self.set("hosts", hostsArr);
  }.observes("this.inputData"),

  /**
   * Sort tasks by it`s id
   * @param tasks
   * @return {Array}
   */
  sortTasksById: function (tasks) {
    var result = [];
    var id = 1;
    for (var i = 0; i < tasks.length; i++) {
      id = (tasks[i].Tasks.id > id) ? tasks[i].Tasks.id : id;
    }
    while (id >= 1) {
      for (var j = 0; j < tasks.length; j++) {
        if (id == tasks[j].Tasks.id) {
          result.push(tasks[j]);
        }
      }
      id--;
    }
    result.reverse();
    return result;
  },

  /**
   * Show popup
   * @return PopupObject For testing purposes
   */
  createPopup: function () {
    var self = this;
    var hostsInfo = this.get("hosts");
    var servicesInfo = this.get("servicesInfo");
    return App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controller: this,
        template: Ember.Handlebars.compile('{{popupHeaderName}}')
      }),
      classNames: ['sixty-percent-width-modal'],
      autoHeight: false,
      onPrimary: function () {
        this.hide();
      },
      secondary: null,

      bodyClass: Ember.View.extend({
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
        hosts: hostsInfo,
        services: servicesInfo,

        tasks: null,

        didInsertElement: function () {
          this.setOnStart();
        },

        /**
         * Preset values on init
         */
        setOnStart: function () {
          if (this.get("controller.showServices")) {
            this.setSelectCount(this.get("services"));
          } else {
            this.set("isHostListHidden", false);
            this.set("isServiceListHidden", true);
          }
        },

        /**
         * When popup is opened, and data after polling has changed, update this data in component
         */
        updateHostInfo: function () {
          //debugger;
          this.get("controller").set("inputData", null);
          this.get("controller").set("inputData", this.get("controller.serviceController.services"));
          this.set("hosts", this.get("controller.hosts"));
          this.set("services", this.get("controller.servicesInfo"));
        }.observes("this.controller.serviceController.serviceTimestamp"),

        /**
         * Depending on service filter, set which services should be shown
         */
        visibleServices: function () {
          if (this.get("services")) {
            this.set("isServiceEmptyList", true);
            if (this.get('serviceCategory.value')) {
              var filter = this.get('serviceCategory.value');
              var services = this.get('services');
              services.setEach("isVisible", false);
              this.setVisability(filter, services);
              if (services.filterProperty("isVisible", true).length > 0) {
                this.set("isServiceEmptyList", false);
              }
            }
          }
        }.observes('serviceCategory', 'services'),

        /**
         * Depending on hosts filter, set which hosts should be shown
         */
        visibleHosts: function () {
          this.set("isHostEmptyList", true);
          if (this.get('hostCategory.value') && this.get('hosts')) {
            var filter = this.get('hostCategory.value');
            var hosts = this.get('hosts');
            hosts.setEach("isVisible", false);
            this.setVisability(filter, hosts);

            if (hosts.filterProperty("isVisible", true).length > 0) {
              this.set("isHostEmptyList", false);
            }
          }
        }.observes('hostCategory', 'hosts'),

        /**
         * Depending on tasks filter, set which tasks should be shown
         */
        visibleTasks: function () {
          this.set("isTasksEmptyList", true);
          if (this.get('taskCategory.value') && this.get('tasks')) {
            var filter = this.get('taskCategory.value');
            var tasks = this.get('tasks');
            this.setVisability(filter, tasks);
            if (tasks.filterProperty("isVisible", true).length > 0) {
              this.set("isTasksEmptyList", false);
            }
          }
        }.observes('taskCategory', 'tasks'),

        /**
         * Depending on selected filter type, set object visibility value
         * @param filter
         * @param obj
         */
        setVisability: function (filter, obj) {
          obj.setEach("isVisible", false);
          if (filter == "all") {
            obj.setEach("isVisible", true);
          }
          else if (filter == "pending") {
            obj.filterProperty("status", "pending").setEach("isVisible", true);
            obj.filterProperty("status", "queued").setEach("isVisible", true);
          }
          else if (filter == "in_progress") {
            obj.filterProperty("status", "in_progress").setEach("isVisible", true);
            obj.filterProperty("status", "upgrading").setEach("isVisible", true);
          }
          else if (filter == "failed") {
            obj.filterProperty("status", "failed").setEach("isVisible", true);
          }
          else if (filter == "completed") {
            obj.filterProperty("status", "completed").setEach("isVisible", true);
            obj.filterProperty("status", "success").setEach("isVisible", true);
          }
          else if (filter == "aborted") {
            obj.filterProperty("status", "aborted").setEach("isVisible", true);
          }
          else if (filter == "timedout") {
            obj.filterProperty("status", "timedout").setEach("isVisible", true);
          }
        },

        /**
         * Select box, display names and values
         */
        categories: [
          Ember.Object.create({value: 'all', label: Em.I18n.t('installer.step9.hostLog.popup.categories.all') }),
          Ember.Object.create({value: 'pending', label: Em.I18n.t('installer.step9.hostLog.popup.categories.pending')}),
          Ember.Object.create({value: 'in_progress', label: Em.I18n.t('installer.step9.hostLog.popup.categories.in_progress')}),
          Ember.Object.create({value: 'failed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.failed') }),
          Ember.Object.create({value: 'completed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.completed') }),
          Ember.Object.create({value: 'aborted', label: Em.I18n.t('installer.step9.hostLog.popup.categories.aborted') }),
          Ember.Object.create({value: 'timedout', label: Em.I18n.t('installer.step9.hostLog.popup.categories.timedout') })
        ],

        /**
         * Selected option is binded to this values
         */
        serviceCategory: null,
        hostCategory: null,
        taskCategory: null,

        /**
         * Count number of operations for select box options
         * @param obj
         */
        setSelectCount: function (obj) {
          if (!obj) return;
          var countAll = obj.length;
          var countPending = obj.filterProperty("status", 'pending').length + obj.filterProperty("status", 'queued').length;
          var countInProgress = obj.filterProperty("status", 'in_progress').length;
          var countFailed = obj.filterProperty("status", 'failed').length;
          var countCompleted = obj.filterProperty("status", 'success').length + obj.filterProperty("status", 'completed').length;
          var countAborted = obj.filterProperty("status", 'aborted').length;
          var countTimedout = obj.filterProperty("status", 'timedout').length;

          this.categories.filterProperty("value", 'all').objectAt(0).set("label", "All (" + countAll + ")");
          this.categories.filterProperty("value", 'pending').objectAt(0).set("label", "Pending (" + countPending + ")");
          this.categories.filterProperty("value", 'in_progress').objectAt(0).set("label", "In Progress (" + countInProgress + ")");
          this.categories.filterProperty("value", 'failed').objectAt(0).set("label", "Failed (" + countFailed + ")");
          this.categories.filterProperty("value", 'completed').objectAt(0).set("label", "Success (" + countCompleted + ")");
          this.categories.filterProperty("value", 'aborted').objectAt(0).set("label", "Aborted (" + countAborted + ")");
          this.categories.filterProperty("value", 'timedout').objectAt(0).set("label", "Timedout (" + countTimedout + ")");
        },

        /**
         * Depending on currently viewed tab, call setSelectCount function
         */
        updateSelectView: function () {
          if (!this.get('isHostListHidden')) {
            this.setSelectCount(this.get("hosts"))
          } else if (!this.get('isTaskListHidden')) {
            this.setSelectCount(this.get("tasks"))
          } else if (!this.get('isServiceListHidden')) {
            this.setSelectCount(this.get("services"))
          }
        }.observes('hosts', 'isTaskListHidden', 'isHostListHidden'),

        /**
         * Onclick handler for button <-Tasks
         * @param event
         * @param context
         */
        backToTaskList: function (event, context) {
          this.destroyClipBoard();
          this.set("openedTaskId", 0);
          this.set("isLogWrapHidden", true);
          this.set("isTaskListHidden", false);
        },

        /**
         * Onclick handler for button <-Hosts
         * @param event
         * @param context
         */
        backToHostList: function (event, context) {
          this.set("isHostListHidden", false);
          this.set("isTaskListHidden", true);
          this.set("tasks", null);
          this.get("controller").set("popupHeaderName", this.get("controller.serviceName"));
        },

        /**
         * Onclick handler for button <-Services
         * @param event
         * @param context
         */
        backToServiceList: function (event, context) {
          this.get("controller").set("serviceName", "");
          this.set("isHostListHidden", true);
          this.set("isServiceListHidden", false);
          this.set("isTaskListHidden", true);
          this.set("tasks", null);
          this.set("hosts", null);
          this.get("controller").setBackgroundOperationHeader();
        },

        /**
         * Onclick handler for selected Service
         * @param event
         * @param context
         */
        gotoHosts: function (event, context) {
          this.get("controller").set("serviceName", event.context.get("name"));
          this.get("controller").onHostUpdate();
          var servicesInfo = this.get("controller.hosts");
          if (servicesInfo.length) {
            this.get("controller").set("popupHeaderName", event.context.get("name"));
          }
          this.set('hosts', servicesInfo);
          this.set("isServiceListHidden", true);
          this.set("isHostListHidden", false);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        /**
         * Onclick handler for selected Host
         * @param event
         * @param context
         */
        gotoTasks: function (event, context) {
          var taskInfo = event.context.tasks;
          if (taskInfo.length) {
            this.get("controller").set("popupHeaderName", taskInfo.objectAt(0).hostName);
          }
          this.set('tasks', taskInfo);
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", false);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        /**
         * Onclick handler for selected Task
         */
        openTaskLogInDialog: function () {
          newwindow = window.open();
          newdocument = newwindow.document;
          newdocument.write($(".task-detail-log-info").html());
          newdocument.close();
        },

        openedTaskId: 0,

        /**
         * Return task detail info of opened task
         */
        openedTask: function () {
          if (!this.get('openedTaskId')) {
            return Ember.Object.create();
          }
          return this.get('tasks').findProperty('id', this.get('openedTaskId'));
        }.property('tasks', 'openedTaskId'),

        /**
         * Onclick event for show task detail info
         * @param event
         * @param context
         */
        toggleTaskLog: function (event, context) {
          var taskInfo = event.context;
          this.set("isLogWrapHidden", false);
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", true);
          this.set('openedTaskId', taskInfo.id);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        /**
         * Onclick event for copy to clipboard button
         * @param event
         */
        textTrigger: function (event) {
          if ($(".task-detail-log-clipboard").length > 0) {
            this.destroyClipBoard();
          } else {
            this.createClipBoard();
          }
        },

        /**
         * Create Clip Board
         */
        createClipBoard: function () {
          $(".task-detail-log-clipboard-wrap").html('<textarea class="task-detail-log-clipboard"></textarea>');
          $(".task-detail-log-clipboard")
              .html("stderr: \n" + $(".stderr").html() + "\n stdout:\n" + $(".stdout").html())
              .css("display", "block")
              .width($(".task-detail-log-maintext").width())
              .height($(".task-detail-log-maintext").height())
              .select();
          $(".task-detail-log-maintext").css("display", "none")
        },

        /**
         * Destroy Clip Board
         */
        destroyClipBoard: function () {
          $(".task-detail-log-clipboard").remove();
          $(".task-detail-log-maintext").css("display", "block");
        }
      })
    });
  }

});

