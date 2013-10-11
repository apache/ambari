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

App.WizardStep9View = Em.View.extend({

  templateName:require('templates/wizard/step9'),
  barColor:'',
  resultMsg:'',
  resultMsgColor:'',

  didInsertElement:function () {
    var controller = this.get('controller');
    this.get('controller.hosts').setEach('status', 'info');
    this.onStatus();
    controller.navigateStep();
  },

  barWidth:function () {
    var controller = this.get('controller');
    var barWidth = 'width: ' + controller.get('progress') + '%;';
    return barWidth;
  }.property('controller.progress'),

  progressMessage: function() {
    return Em.I18n.t('installer.step9.overallProgress').format(this.get('controller.progress'));
  }.property('controller.progress'),

  onStatus:function () {
    if (this.get('controller.status') === 'info') {
      this.set('resultMsg', '');
      this.set('barColor', 'progress-info');
    } else if (this.get('controller.status') === 'warning') {
      this.set('barColor', 'progress-warning');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.warning'));
      this.set('resultMsgColor', 'alert-warning');
    } else if (this.get('controller.status') === 'failed') {
      this.set('barColor', 'progress-danger');
      console.log('TRACE: Inside error view step9');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.failed'));
      this.set('resultMsgColor', 'alert-error');
    } else if (this.get('controller.status') === 'success') {
      console.log('TRACE: Inside success view step9');
      this.set('barColor', 'progress-success');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.success'));
      this.set('resultMsgColor', 'alert-success');
    }
  }.observes('controller.status')
});

App.HostStatusView = Em.View.extend({
  tagName:'tr',
  obj:'null',
  barColor:'',

  didInsertElement:function () {
    var controller = this.get('controller');
    this.onStatus();
  },

  barWidth:function () {
    var barWidth = 'width: ' + this.get('obj.progress') + '%;';
    return barWidth;
  }.property('obj.progress'),

  onStatus:function () {
    if (this.get('obj.status') === 'info') {
      this.set('barColor', 'progress-info');
    } else if (this.get('obj.status') === 'warning') {
      this.set('barColor', 'progress-warning');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.warning'));
      }
    } else if (this.get('obj.status') === 'failed') {
      this.set('barColor', 'progress-danger');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.failed'));
      }
    } else {
      if (this.get('obj.status') === 'success' && this.get('isHostCompleted') && parseInt(this.get('controller.progress')) > 34) {
        this.set('barColor', 'progress-success');
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.success'));
      }
    }
  }.observes('obj.status', 'obj.progress', 'controller.progress'),

  isFailed:function () {
    return (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'failed');
  }.property('controller.isStepCompleted', 'controller.status'),

  isSuccess:function () {
    return (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'success');
  }.property('controller.isStepCompleted', 'controller.status'),

  isWarning:function () {
    return(this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'warning');
  }.property('controller.isStepCompleted', 'controller.status'),

  isHostCompleted:function () {
    return this.get('obj.progress') == 100 || this.get('controller.isStepCompleted');
  }.property('controller.isStepCompleted', 'obj.progress'),

  hostLogPopup:function (event, context) {
    var self = this;
    var host = event.context;
    App.ModalPopup.show({
      header: event.context.get('name'),
      classNames: ['sixty-percent-width-modal'],
      autoHeight: false,
      onPrimary:function () {
        this.hide();
      },
      secondary:null,

      bodyClass:Ember.View.extend({
        templateName:require('templates/wizard/step9HostTasksLogPopup'),
        isLogWrapHidden: true,
        showTextArea: false,
        isEmptyList:true,
        controllerBinding:context,
        hostObj:function () {
          return this.get('parentView.obj');
        }.property('parentView.obj'),

        task: null, // set in showTaskLog; contains task info including stdout and stderr
        /**
         * sort task array by Id
         * @param tasks
         * @return {Array}
         */

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

        visibleTasks: function () {
          var self = this;
          self.set("isEmptyList", true);
          if (this.get('category.value')) {
            var filter = this.get('category.value');
            var tasks = this.get('tasks');
            tasks.setEach("isVisible", false);

            if (filter == "all") {
              tasks.setEach("isVisible", true);
            }
            else if (filter == "pending") {
              tasks.filterProperty("status", "pending").setEach("isVisible", true);
              tasks.filterProperty("status", "queued").setEach("isVisible", true);
            }
            else if (filter == "in_progress") {
              tasks.filterProperty("status", "in_progress").setEach("isVisible", true);
            }
            else if (filter == "failed") {
              tasks.filterProperty("status", "failed").setEach("isVisible", true);
            }
            else if (filter == "completed") {
              tasks.filterProperty("status", "completed").setEach("isVisible", true);
            }
            else if (filter == "aborted") {
              tasks.filterProperty("status", "aborted").setEach("isVisible", true);
            }
            else if (filter == "timedout") {
              tasks.filterProperty("status", "timedout").setEach("isVisible", true);
            }

            if (tasks.filterProperty("isVisible", true).length > 0) {
              self.set("isEmptyList", false);
            }
          }
        }.observes('category', 'tasks'),

        categories: [
            Ember.Object.create({value: 'all', label: Em.I18n.t('installer.step9.hostLog.popup.categories.all') }),
            Ember.Object.create({value: 'pending', label: Em.I18n.t('installer.step9.hostLog.popup.categories.pending')}),
            Ember.Object.create({value: 'in_progress', label: Em.I18n.t('installer.step9.hostLog.popup.categories.in_progress')}),
            Ember.Object.create({value: 'failed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.failed') }),
            Ember.Object.create({value: 'completed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.completed') }),
            Ember.Object.create({value: 'aborted', label: Em.I18n.t('installer.step9.hostLog.popup.categories.aborted') }),
            Ember.Object.create({value: 'timedout', label: Em.I18n.t('installer.step9.hostLog.popup.categories.timedout') })
        ],

        category: null,

        tasks: function () {
          var tasksArr = [];
          var tasks = this.getStartedTasks(host);
          //tasks = this.sortTasksById(tasks);
          tasks = tasks.sort(function(a,b){
            return a.Tasks.id - b.Tasks.id;
          });
          //tasks = this.groupTasksByRole(tasks);
          if (tasks.length) {
            tasks.forEach(function (_task) {
              var taskInfo = Ember.Object.create({});
              taskInfo.set('id', _task.Tasks.id);
              taskInfo.set('command', _task.Tasks.command.toLowerCase());
              taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
              taskInfo.set('role', App.format.role(_task.Tasks.role));
              taskInfo.set('stderr', _task.Tasks.stderr);
              taskInfo.set('stdout', _task.Tasks.stdout);
              taskInfo.set('isVisible', true);
              taskInfo.set('icon', '');
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
          return tasksArr;
        }.property('App.router.wizardStep9Controller.logTasksChangesCounter'),

        backToTaskList: function(event, context) {
          this.destroyClipBoard();
          this.set("isLogWrapHidden",true);
        },

        getStartedTasks:function (host) {
          var startedTasks = host.logTasks.filter(function (task) {
            return task.Tasks.status;
            //return task.Tasks.status != 'PENDING' && task.Tasks.status != 'QUEUED';
          });
          return startedTasks;
        },

        openTaskLogInDialog: function(){
          newwindow=window.open();
          newdocument=newwindow.document;
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
          if (this.isLogWrapHidden) {
            var taskInfo = event.context;
            this.set("isLogWrapHidden", false);
            this.set('openedTaskId', taskInfo.id);
            $(".modal").scrollTop(0);
            $(".modal-body").scrollTop(0);
          } else {
            this.set("isLogWrapHidden", true);
            this.set('openedTaskId', 0);
          }
        },

        textTrigger:function (event) {
          if($(".task-detail-log-clipboard").length > 0)
          {
            this.destroyClipBoard();
          }else
          {
            this.createClipBoard();
          };
        },
        createClipBoard:function(){
          $(".task-detail-log-clipboard-wrap").html('<textarea class="task-detail-log-clipboard"></textarea>');
          $(".task-detail-log-clipboard")
              .html("stderr: \n"+$(".stderr").html()+"\n stdout:\n"+$(".stdout").html())
              .css("display","block")
              .width($(".task-detail-log-maintext").width())
              .height($(".task-detail-log-maintext").height())
              .select();
          $(".task-detail-log-maintext").css("display","none")
        },
        destroyClipBoard:function(){
          $(".task-detail-log-clipboard").remove();
          $(".task-detail-log-maintext").css("display","block");
        }
      })
    });
  }

});