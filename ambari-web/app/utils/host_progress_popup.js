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

  hosts: null,
  inputData:null,
  serviceName:"",
  popupHeaderName:"",
  serviceController:null,
  updateTimeOut:100,

  initPopup: function (serviceName,controller) {
    this.set("serviceName", serviceName);
    this.set("serviceController", controller);
    this.set("inputData", null);
    this.set("inputData", this.get("serviceController.services"));
    this.set("popupHeaderName",serviceName);
    this.createPopup();
  },

  getHostStatus:function(tasks){
    if (tasks.everyProperty('Tasks.status', 'COMPLETED')) {
      return ['SUCCESS','icon-ok','progress-info'];
    }
    if (tasks.someProperty('Tasks.status', 'FAILED')) {
      return ['FAILED','icon-exclamation-sign','progress-danger'];
    }
    if (tasks.someProperty('Tasks.status', 'ABORTED')) {
      return ['CANCELLED','icon-minus','progress-warning'];
    }
    if (tasks.someProperty('Tasks.status', 'TIMEDOUT')) {
      return ['TIMEDOUT','icon-time','progress-warning'];
    }
    if (tasks.someProperty('Tasks.status', 'IN_PROGRESS') || tasks.someProperty('Tasks.status', 'UPGRADING')) {
      return ['IN_PROGRESS','icon-cogs','progress-info'];
    }
    return ['PENDING','icon-cog','progress-info'];
  },

  getHostProgress:function(tasks){

    var progress = 0;
    var actionsNumber = tasks.length;
    var completedActions = tasks.filterProperty('Tasks.status', 'COMPLETED').length
        + tasks.filterProperty('Tasks.status', 'FAILED').length
        + tasks.filterProperty('Tasks.status', 'ABORTED').length
        + tasks.filterProperty('Tasks.status', 'TIMEDOUT').length;
    var queuedActions = tasks.filterProperty('Tasks.status', 'QUEUED').length;
    var inProgressActions = tasks.filterProperty('Tasks.status', 'UPGRADING').length;
    progress = Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsNumber * 100);
    console.log('--------INFO: progressPerHost is: ' + progress);
    return progress;
  },

  onHostUpdate: function () {
    var self=this;
    if(this.get("inputData")){
    var hostsArr = [];
    var hostsData = this.get("inputData").filterProperty("name" , this.get("serviceName")).objectAt(0);
    var hosts = hostsData.hosts;
    }
    if (hosts) {
      hosts.forEach(function (_host) {
        var tasks = _host.logTasks;
        var hostInfo = Ember.Object.create({});
        hostInfo.set('name', _host.name);
        hostInfo.set('publicName', _host.publicName);
        hostInfo.set('progress', 0);
        hostInfo.set('status', App.format.taskStatus("PENDING"));
        hostInfo.set('serviceName', hostsData.name);
        hostInfo.set('isVisible', true);
        hostInfo.set('icon', "icon-cog");
        hostInfo.set('barColor', "progress-info");
        hostInfo.set('barWidth', "width:0%;");

        tasks = self.sortTasksById(tasks);
        tasks = self.groupTasksByRole(tasks);
        var tasksArr = [];

        if (tasks.length) {

          var hostStatus = self.getHostStatus(tasks);
          var hostProgress= self.getHostProgress(tasks);
          hostInfo.set('status', App.format.taskStatus(hostStatus[0]));
          hostInfo.set('icon', hostStatus[1]);
          hostInfo.set('barColor', hostStatus[2]);
          hostInfo.set('progress', hostProgress);
          hostInfo.set('barWidth', "width:"+hostProgress+"%;");

          tasks.forEach(function (_task) {
            var taskInfo = Ember.Object.create({});
            taskInfo.set('id', _task.Tasks.id);
            taskInfo.set('hostName', _host.name);
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

    self.set("hosts",hostsArr);
  }.observes("this.inputData"),

  sortTasksById: function(tasks){
    var result = [];
    var id = 1;
    for(var i = 0; i < tasks.length; i++){
      id = (tasks[i].Tasks.id > id) ? tasks[i].Tasks.id : id;
    }
    while(id >= 1){
      for(var j = 0; j < tasks.length; j++){
        if(id == tasks[j].Tasks.id){
          result.push(tasks[j]);
        }
      }
      id--;
    }
    result.reverse();
    return result;
  },

  groupTasksByRole: function (tasks) {
    var sortedTasks = [];
    var taskRoles = tasks.mapProperty('Tasks.role').uniq();
    for (var i = 0; i < taskRoles.length; i++) {
      sortedTasks = sortedTasks.concat(tasks.filterProperty('Tasks.role', taskRoles[i]))
    }
    return sortedTasks;
  },


  createPopup: function () {
    var self = this;
    var hostsInfo = this.get("hosts");
    return App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controller: this,
        template:Ember.Handlebars.compile('{{popupHeaderName}}')
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
        isHostListHidden: false,
        showTextArea: false,
        isHostEmptyList: true,
        isTasksEmptyList: true,
        controller:this,
        hosts:hostsInfo,

        tasks:null,

        didInsertElement:function(){
          this.setSelectCount(this.get("hosts"));
        },

        updateHostInfo:function(){
          this.get("controller").set("inputData", null);
          this.get("controller").set("inputData", this.get("controller.serviceController.services"));
          this.set("hosts", this.get("controller.hosts"));
        }.observes("this.controller.serviceController.serviceTimestamp"),

        visibleHosts: function () {
          this.set("isHostEmptyList", true);
          if (this.get('hostCategory.value')) {
            var filter = this.get('hostCategory.value');
            var hosts = this.get('hosts');
            hosts.setEach("isVisible", false);
            this.setVisability(filter,hosts);
            if (hosts.filterProperty("isVisible", true).length > 0) {
              this.set("isHostEmptyList", false);
            }
          }
        }.observes('hostCategory', 'hosts'),

        visibleTasks: function () {
          this.set("isTasksEmptyList", true);
          if (this.get('taskCategory.value')&&this.get('tasks')) {
            var filter = this.get('taskCategory.value');
            var tasks = this.get('tasks');
            this.setVisability(filter,tasks);
            if (tasks.filterProperty("isVisible", true).length > 0) {
              this.set("isTasksEmptyList", false);
            }
          }
        }.observes('taskCategory', 'tasks'),

        setVisability: function(filter,obj){
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

        categories: [
          Ember.Object.create({value: 'all', label: Em.I18n.t('installer.step9.hostLog.popup.categories.all') }),
          Ember.Object.create({value: 'pending', label: Em.I18n.t('installer.step9.hostLog.popup.categories.pending')}),
          Ember.Object.create({value: 'in_progress', label: Em.I18n.t('installer.step9.hostLog.popup.categories.in_progress')}),
          Ember.Object.create({value: 'failed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.failed') }),
          Ember.Object.create({value: 'completed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.completed') }),
          Ember.Object.create({value: 'aborted', label: Em.I18n.t('installer.step9.hostLog.popup.categories.aborted') }),
          Ember.Object.create({value: 'timedout', label: Em.I18n.t('installer.step9.hostLog.popup.categories.timedout') })
        ],

        hostCategory: null,
        taskCategory: null,

        setSelectCount:function(obj){
          if(!obj) return;
          var countAll = obj.length;
          var countPending = obj.filterProperty("status",'pending').length;
          var countInProgress = obj.filterProperty("status",'in_progress').length;
          var countFailed = obj.filterProperty("status",'failed').length;
          var countCompleted = obj.filterProperty("status",'success').length + obj.filterProperty("status",'completed').length;
          var countAborted = obj.filterProperty("status",'aborted').length;
          var countTimedout = obj.filterProperty("status",'timedout').length;

          this.categories.filterProperty("value",'all').objectAt(0).set("label","All ("+countAll+")");
          this.categories.filterProperty("value",'pending').objectAt(0).set("label","Pending ("+countPending+")");
          this.categories.filterProperty("value",'in_progress').objectAt(0).set("label","In Progress ("+countInProgress+")");
          this.categories.filterProperty("value",'failed').objectAt(0).set("label","Failed ("+countFailed+")");
          this.categories.filterProperty("value",'completed').objectAt(0).set("label","Success ("+countCompleted+")");
          this.categories.filterProperty("value",'aborted').objectAt(0).set("label","Aborted ("+countAborted+")");
          this.categories.filterProperty("value",'timedout').objectAt(0).set("label","Timedout ("+countTimedout+")");
          },

        updateSelectView:function(){
          if(!this.get('isHostListHidden')){
            this.setSelectCount(this.get("hosts"))
          }else if(!this.get('isTaskListHidden')){
            this.setSelectCount(this.get("tasks"))
          }
        }.observes('hosts','isTaskListHidden','isHostListHidden'),

        backToTaskList: function (event, context) {
          this.destroyClipBoard();
          this.set("openedTaskId",0);
          this.set("isLogWrapHidden", true);
          this.set("isTaskListHidden", false);
        },

        backToHostList: function (event, context) {
          this.set("isHostListHidden", false);
          this.set("isTaskListHidden", true);
          this.set("tasks", null);
          this.get("controller").set("popupHeaderName",this.get("controller.serviceName"));
        },

        gotoTasks: function(event, context){
          var taskInfo = event.context.tasks;
          if(taskInfo.length){
            this.get("controller").set("popupHeaderName", taskInfo.objectAt(0).hostName);
          }
          this.set('tasks', taskInfo);
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", false);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        openTaskLogInDialog: function () {
          newwindow = window.open();
          newdocument = newwindow.document;
          newdocument.write($(".task-detail-log-info").html());
          newdocument.close();
        },

        openedTaskId: 0,

        openedTask: function () {
          if (!this.get('openedTaskId')) {
            return Ember.Object.create();
          }
          return this.get('tasks').findProperty('id', this.get('openedTaskId'));
        }.property('tasks', 'openedTaskId'),

        toggleTaskLog: function (event, context) {
          var taskInfo = event.context;
          this.set("isLogWrapHidden", false);
          this.set("isHostListHidden", true);
          this.set("isTaskListHidden", true);
          this.set('openedTaskId', taskInfo.id);
          $(".modal").scrollTop(0);
          $(".modal-body").scrollTop(0);
        },

        textTrigger: function (event) {
          if ($(".task-detail-log-clipboard").length > 0) {
            this.destroyClipBoard();
          } else {
            this.createClipBoard();
          }
        },
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
        destroyClipBoard: function () {
          $(".task-detail-log-clipboard").remove();
          $(".task-detail-log-maintext").css("display", "block");
        }
      })
    });
  }

});

