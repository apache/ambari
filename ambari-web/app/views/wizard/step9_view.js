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
    } else if (this.get('obj.status') === 'success') {
      this.set('barColor', 'progress-success');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.success'));
      }
    }
  }.observes('obj.status', 'obj.progress'),

  isFailed:function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'failed') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted', 'controller.status'),

  isSuccess:function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'success') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted', 'controller.status'),

  isWarning:function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'warning') {
      return true;
    } else {
      return false;
    }
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

        startedTasks:[], // initialized in didInsertElement

        task: null, // set in showTaskLog; contains task info including stdout and stderr
        /**
         * sort task array by request Id
         * @param tasks
         * @return {Array}
         */

        sortTasksByRequest: function(tasks){
          var result = [];
          var requestId = 1;
          for(var i = 0; i < tasks.length; i++){
            requestId = (tasks[i].Tasks.request_id > requestId) ? tasks[i].Tasks.request_id : requestId;
          }
          while(requestId >= 1){
            for(var j = 0; j < tasks.length; j++){
              if(requestId == tasks[j].Tasks.request_id){
                result.push(tasks[j]);
              }
            }
            requestId--;
          }
          result.reverse();
          return result;
        },

        visibleTasks: function () {
          var self=this;
          self.set("isEmptyList", true);
          if (this.get('category.value')) {
            var filter = this.get('category.value');
            $.each(this.get("roles"),function(a,e){

              e.taskInfos.setEach("isVisible", false);

              if(filter == "all")
              {
                e.taskInfos.setEach("isVisible", true);
              }
              else if(filter == "pending")
              {
                e.taskInfos.filterProperty("status", "pending").setEach("isVisible", true);
                e.taskInfos.filterProperty("status", "queued").setEach("isVisible", true);
              }
              else if(filter == "in_progress")
              {
                e.taskInfos.filterProperty("status", "in_progress").setEach("isVisible", true);
              }
              else if(filter == "failed")
              {
                e.taskInfos.filterProperty("status", "failed").setEach("isVisible", true);
              }
              else if(filter == "completed")
              {
                e.taskInfos.filterProperty("status", "completed").setEach("isVisible", true);
              }
              else if(filter == "aborted")
              {
                e.taskInfos.filterProperty("status", "aborted").setEach("isVisible", true);
              }
              else if(filter == "timedout")
              {
                e.taskInfos.filterProperty("status", "timedout").setEach("isVisible", true);
              }

              if(e.taskInfos.filterProperty("isVisible", true).length >0){
                self.set("isEmptyList", false);
              }
            })
          }
        }.observes('category'),

        categories: [
            Ember.Object.create({value: 'all', label: 'All' }),
            Ember.Object.create({value: 'pending', label: 'Queued / Pending'}),
            Ember.Object.create({value: 'in_progress', label: 'In Progress'}),
            Ember.Object.create({value: 'failed', label: 'Failed' }),
            Ember.Object.create({value: 'completed', label: 'Success' }),
            Ember.Object.create({value: 'aborted', label: 'Cancelled' }),
            Ember.Object.create({value: 'timedout', label: 'Timed Out' })
        ],

        category: null,

        roles:function () {
          var roleArr = [];
          var tasks = this.getStartedTasks(host);
          tasks = this.sortTasksByRequest(tasks);
          if (tasks.length) {
            var _roles = tasks.mapProperty('Tasks.role').uniq();
            _roles.forEach(function (_role) {
              var taskInfos = [];
              var roleObj = {};
              roleObj.roleName = App.format.role(_role);
              tasks.filterProperty('Tasks.role', _role).forEach(function (_task) {
                var taskInfo = Ember.Object.create({});
                taskInfo.set('requestId', _task.Tasks.request_id);
                taskInfo.set('command', _task.Tasks.command.toLowerCase());
                taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
                taskInfo.set('url', _task.href);
                taskInfo.set('roleName', roleObj.roleName);
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
                  taskInfo.set('icon', 'icon-remove');
                } else if (taskInfo.get('status') == 'timedout') {
                  taskInfo.set('icon', 'icon-time');
                }
                taskInfos.pushObject(taskInfo);
              }, this);
              roleObj.taskInfos = taskInfos;
              roleArr.pushObject(roleObj);
            }, this);
          }
          return roleArr;
        }.property('startedTasks.@each'),

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

        toggleTaskLog:function (event, context) {
          if(this.isLogWrapHidden){

            var taskInfo = event.context;
            this.set("isLogWrapHidden",false);

            $(".task-detail-log-rolename")
                .html(taskInfo.roleName + " " + taskInfo.command)

            $(".task-detail-status-ico")
                .removeClass()
                .addClass(taskInfo.status + " task-detail-status-ico " + taskInfo.icon);

            var url = (App.testMode) ? '/data/wizard/deploy/task_log.json' : taskInfo.url;
            $.ajax({
              url:url,
              dataType:'text',
              timeout:App.timeout,
              success:function (data) {
                var task = $.parseJSON(data);
                $(".stderr").html(task.Tasks.stderr);
                $(".stdout").html(task.Tasks.stdout);
                $(".modal").scrollTop(0);
                $(".modal-body").scrollTop(0);
              },
              error:function () {
                alert('Failed to retrieve task log');
              }
            });
          }else{
            this.set("isLogWrapHidden",true);
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