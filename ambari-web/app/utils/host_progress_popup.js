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
  currentServiceId: null,
  popupHeaderName: "",
  serviceController: null,
  showServices: false,
  currentHostName: null,

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
    if (!showServices) {
      this.set("popupHeaderName", serviceName);
    }
    this.set("showServices", showServices);
    this.set("inputData", this.get("serviceController.services"));
    if(this.get('showServices')){
      this.onServiceUpdate();
    } else {
      this.onHostUpdate();
    }
    return this.createPopup();
  },

  /**
   * Depending on tasks status
   * @param tasks
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
   * @param tasks
   * @return {Number} percent of completion
   */
  getProgress: function (tasks) {
    var completedActions = 0;
    var queuedActions = 0;
    var inProgressActions = 0;
    tasks.forEach(function(task){
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
   * @param obj
   * @param categories
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
    }, this);

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
   */
  setBackgroundOperationHeader: function () {
    if (this.get("showServices")) {
      var numRunning = this.get('categories').findProperty("value", 'pending').get("count");
      numRunning += this.get('categories').findProperty("value", 'in_progress').get("count");
      this.set("popupHeaderName", numRunning + Em.I18n.t('hostPopup.header.postFix'));
    } else {
      this.set("popupHeaderName", this.get("serviceName"));
    }
  },

  /**
   * Create services obj data structure for popup
   * Set data for services
   */
  onServiceUpdate: function () {
    if (this.get('showServices') && this.get("inputData")) {
      var self = this;
      var allNewServices = [];
      this.set("servicesInfo", null);
      this.get("inputData").forEach(function (service) {
        var newService = Ember.Object.create({
          id: service.id,
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
        var allTasks = [];
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
      });
      self.set('servicesInfo', allNewServices);
      if (this.get("serviceName") == "") this.setBackgroundOperationHeader();
    }
  },

  /**
   * update icon of task depending on its status
   * @param taskInfo
   */
  updateTaskIcon: function(taskInfo){
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
  },
  /**
   * Create hosts and tasks data structure for popup
   * Set data for hosts and tasks
   */
  onHostUpdate: function () {
    var self = this;
    if (this.get("inputData")) {
      var hostsArr = [];
      var hostsData = this.get("inputData");
      var hosts = [];
      if (this.get("showServices") && this.get("serviceName") == "") {
        hostsData.forEach(function (service) {
          var host = service.hosts;
          host.setEach("serviceName", service.name);
          hosts.push.apply(hosts, host);
        });
      } else {
        if (this.get("currentServiceId") != null) {
          hostsData = hostsData.findProperty("id", this.get("currentServiceId"));
        }
        else {
          hostsData = hostsData.findProperty("name", this.get("serviceName"));
        }

        if (hostsData && hostsData.hosts) {
          hosts = hostsData.hosts;
        }

        hosts.setEach("serviceName", this.get("serviceName"));
      }

      var existedHosts = self.get('hosts');

      if (hosts) {
        if (existedHosts && existedHosts.length === hosts.length) {
          existedHosts.forEach(function (host) {
            var newHostInfo = hosts.findProperty('name', host.get('name'));
            if (newHostInfo) {
              var hostStatus = self.getStatus(newHostInfo.logTasks);
              var hostProgress = self.getProgress(newHostInfo.logTasks);
              host.set('status', App.format.taskStatus(hostStatus[0]));
              host.set('icon', hostStatus[1]);
              host.set('barColor', hostStatus[2]);
              host.set('isInProgress', hostStatus[3]);
              host.set('progress', hostProgress);
              host.set('barWidth', "width:" + hostProgress + "%;");
              var existTasks = host.get('tasks');
              var newTasks = newHostInfo.logTasks;
              if (existTasks && newTasks && existTasks.length == newTasks.length) {
                // Same number of source and destinations
                var existTaskMap = {};
                var newTaskMap = {};
                host.get('tasks').forEach(function (taskInfo) {
                  var id = taskInfo.get('id');
                  existTaskMap[id] = taskInfo;
                });
                var newTasksArray = [];
                newTasks.forEach(function (newTask) {
                  var existTask = existTaskMap[newTask.Tasks.id];
                  if (existTask) {
                    // reuse
                    existTask.set('status', App.format.taskStatus(newTask.Tasks.status));
                    existTask.set('stderr', newTask.Tasks.stderr);
                    existTask.set('stdout', newTask.Tasks.stdout);
                    self.updateTaskIcon(existTask);
                    delete existTaskMap[newTask.Tasks.id];
                  } else {
                    // create new
                    var taskInfo = Ember.Object.create({
                      id: newTask.Tasks.id,
                      hostName: newHostInfo.publicName,
                      command: newTask.Tasks.command.toLowerCase(),
                      status: App.format.taskStatus(newTask.Tasks.status),
                      role: App.format.role(newTask.Tasks.role),
                      stderr: newTask.Tasks.stderr,
                      stdout: newTask.Tasks.stdout,
                      isVisible: true,
                      icon: 'icon-cogs'
                    });
                    self.updateTaskIcon(taskInfo);
                    newTasksArray.push(taskInfo);
                  }
                });
                for (var id in existTaskMap) {
                  host.get('tasks').removeObject(existTaskMap[id]);
                }
                if (newTasksArray.length) {
                  host.get('tasks').pushObjects(newTasksArray);
                }
              } else {
                // Tasks have changed
                var tasksArr = [];
                newTasks.forEach(function (newTask) {
                  var taskInfo = Ember.Object.create({
                    id: newTask.Tasks.id,
                    hostName: newHostInfo.publicName,
                    command: newTask.Tasks.command.toLowerCase(),
                    status: App.format.taskStatus(newTask.Tasks.status),
                    role: App.format.role(newTask.Tasks.role),
                    stderr: newTask.Tasks.stderr,
                    stdout: newTask.Tasks.stdout,
                    isVisible: true,
                    icon: 'icon-cogs'
                  });
                  self.updateTaskIcon(taskInfo);
                  tasksArr.push(taskInfo);
                });
                host.set('tasks', tasksArr);
              }
            }
          }, this);
        } else {

          //sort hosts by name
          this.sortArray(hosts, "name");

          hosts.forEach(function (_host) {
            var tasks = _host.logTasks;
            var hostInfo = Ember.Object.create({
              name: _host.name,
              publicName: _host.publicName,
              progress: 0,
              status: App.format.taskStatus("PENDING"),
              serviceName: _host.serviceName,
              isVisible: true,
              icon: "icon-cog",
              barColor: "progress-info",
              barWidth: "width:0%;"
            });

            var tasksArr = [];

            if (tasks.length) {
              tasks = self.sortTasksById(tasks);
              var hostStatus = self.getStatus(tasks);
              var hostProgress = self.getProgress(tasks);
              hostInfo.set('status', App.format.taskStatus(hostStatus[0]));
              hostInfo.set('icon', hostStatus[1]);
              hostInfo.set('barColor', hostStatus[2]);
              hostInfo.set('isInProgress', hostStatus[3]);
              hostInfo.set('progress', hostProgress);
              hostInfo.set('barWidth', "width:" + hostProgress + "%;");

              tasks.forEach(function (_task) {
                var taskInfo = Ember.Object.create({
                  id: _task.Tasks.id,
                  hostName: _host.publicName,
                  command: _task.Tasks.command.toLowerCase(),
                  status: App.format.taskStatus(_task.Tasks.status),
                  role: App.format.role(_task.Tasks.role),
                  stderr: _task.Tasks.stderr,
                  stdout: _task.Tasks.stdout,
                  isVisible: true,
                  icon: 'icon-cogs'
                });
                this.updateTaskIcon(taskInfo);
                tasksArr.push(taskInfo);
              }, this);
            }

            hostInfo.set('tasks', tasksArr);
            hostsArr.push(hostInfo);
          }, this);
          self.set("hosts", hostsArr);
        }
      }
    }
  },

  /**
   * Sort tasks by it`s id
   * @param tasks
   * @return {Array}
   */
  sortTasksById: function (tasks) {
    return tasks.sort(function (a, b) {
      return (a.Tasks.id > b.Tasks.id) ? 1 : (a.Tasks.id < b.Tasks.id) ? -1 : 0;
    });
  },

  /**
   * Show popup
   * @return PopupObject For testing purposes
   */
  createPopup: function () {
    var self = this;
    var hostsInfo = this.get("hosts");
    var servicesInfo = this.get("servicesInfo");
    var showServices = this.get('showServices');
    var categoryObject = Em.Object.extend({
      value: '',
      count: 0,
      labelPath: '',
      label: function(){
        return Em.I18n.t(this.get('labelPath')).format(this.get('count'));
      }.property('count')
    });
    return App.ModalPopup.show({
      //no need to track is it loaded when popup contain only list of hosts
      isLoaded: !showServices,
      isOpen: false,
      didInsertElement: function(){
        this.set('isOpen', true);
      },
      headerClass: Ember.View.extend({
        controller: this,
        template: Ember.Handlebars.compile('{{popupHeaderName}}')
      }),
      classNames: ['sixty-percent-width-modal'],
      autoHeight: false,
      closeModelPopup: function () {
        this.set('isOpen', false);
        if(showServices){
          $(this.get('element')).detach();
        } else {
          this.hide();
        }
      },
      onPrimary: function () {
        this.closeModelPopup();
      },
      onClose: function () {
        this.closeModelPopup();
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
        hosts: self.get("hosts"),
        services: self.get('servicesInfo'),

        tasks: function () {
          if (!this.get('controller.currentHostName')) return [];
          if (this.get('hosts') && this.get('hosts').length) {
            var currentHost = this.get('hosts').findProperty('name', this.get('controller.currentHostName'));
            if (currentHost) {
              return currentHost.get('tasks');
            }
          }
          return [];
        }.property('hosts.@each.tasks', 'hosts.@each.tasks.@each.status'),

        didInsertElement: function () {
          this.setOnStart();
        },

        /**
         * Preset values on init
         */
        setOnStart: function () {
          if (this.get("controller.showServices")) {
            this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
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
            this.get("controller").setBackgroundOperationHeader();
            this.setOnStart();
          }
        }.observes('parentView.isOpen'),

        /**
         * When popup is opened, and data after polling has changed, update this data in component
         */
        updateHostInfo: function () {
          if(!this.get('parentView.isOpen')) return;
          this.set('parentView.isLoaded', false);
          this.get("controller").set("inputData", this.get("controller.serviceController.services"));
          this.get("controller").onServiceUpdate();
          this.get("controller").onHostUpdate();
          this.set('parentView.isLoaded', true);
          this.set("hosts", this.get("controller.hosts"));
          this.set("services", this.get("controller.servicesInfo"));
        }.observes("controller.serviceController.serviceTimestamp"),

        /**
         * Depending on service filter, set which services should be shown
         */
        visibleServices: function () {
          if (this.get("services")) {
            this.set("isServiceEmptyList", true);
            if (this.get('serviceCategory.value')) {
              var filter = this.get('serviceCategory.value');
              var services = this.get('services');
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
            this.get('controller').setSelectCount(this.get("hosts"), this.get('categories'));
          } else if (!this.get('isTaskListHidden')) {
            this.get('controller').setSelectCount(this.get("tasks"), this.get('categories'));
          } else if (!this.get('isServiceListHidden')) {
            this.get('controller').setSelectCount(this.get("services"), this.get('categories'));
          }
        }.observes('hosts', 'isTaskListHidden', 'isHostListHidden', 'services.length', 'services.@each.status'),

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
          this.get("controller").set("currentServiceId", event.context.get("id"));
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
            this.get("controller").set("currentHostName", taskInfo.objectAt(0).hostName);
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
          if (!this.get('openedTaskId')) {
            return Ember.Object.create();
          }
          return this.get('tasks').findProperty('id', this.get('openedTaskId'));
        }.property('tasks', 'tasks.@each.stderr', 'tasks.@each.stdout', 'openedTaskId'),

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

