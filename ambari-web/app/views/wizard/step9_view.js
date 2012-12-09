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

  templateName: require('templates/wizard/step9'),
  barColor: '',
  resultMsg: '',
  resultMsgColor: '',

  didInsertElement: function () {
    var controller = this.get('controller');
    this.get('controller.hosts').setEach('status', 'info');
    this.onStatus();
    controller.navigateStep();
  },

  barWidth: function () {
    var controller = this.get('controller');
    var barWidth = 'width: ' + controller.get('progress') + '%;';
    return barWidth;
  }.property('controller.progress'),

  onStatus: function () {
    if (this.get('controller.status') === 'info') {
      this.set('resultMsg', '');
      this.set('barColor', 'progress-info');
    } else if (this.get('controller.status') === 'warning') {
      this.set('barColor', 'progress-warning');
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
  tagName: 'tr',
  obj: 'null',
  barColor: '',

  didInsertElement: function () {
    var controller = this.get('controller');
    this.onStatus();
  },

  barWidth: function () {
    var barWidth = 'width: ' + this.get('obj.progress') + '%;';
    return barWidth;
  }.property('obj.progress'),

  onStatus: function () {
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

  isFailed: function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'failed') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted', 'controller.status'),

  isSuccess: function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'success') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted', 'controller.status'),

  isWarning: function () {
    if (this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'warning') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted', 'controller.status'),

  isHostCompleted: function () {
    return this.get('obj.progress') == 100 || this.get('controller.isStepCompleted');
  }.property('controller.isStepCompleted', 'obj.progress'),

  hostLogPopup: function (event, context) {
    var self = this;
    var host = event.context;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step9.hostLog.popup.header') + event.context.get('name'),
      onPrimary: function () {
        this.hide();
      },
      secondary: null,
      controllerBinding: context,
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step9HostTasksLogPopup'),

        hostObj: function () {
          return this.get('parentView.obj');
        }.property('parentView.obj'),

        startedTasks: [], // initialized in didInsertElement

        task: null, // set in showTaskLog; contains task info including stdout and stderr

        roles: function () {
          var roleArr = [];
          var tasks = this.getStartedTasks(host);
          if (tasks.length) {
            var _roles = tasks.mapProperty('Tasks.role').uniq();
            _roles.forEach(function (_role) {
              var taskInfos = [];
              var roleObj = {};
              roleObj.roleName = App.format.role(_role);
              tasks.filterProperty('Tasks.role', _role).forEach(function (_task) {
                var taskInfo = Ember.Object.create({
                  isTextArea: false,
                  buttonLabel: function(){
                    return (this.get('isTextArea')) ? 'Press CTRL+C': 'Click to highlight';
                  }.property('isTextArea')
                });
                taskInfo.set('command', _task.Tasks.command.toLowerCase());
                taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
                taskInfo.set('url', _task.href);
                taskInfo.set('isLogHidden', false);
                taskInfos.pushObject(taskInfo);
              }, this);
              roleObj.taskInfos = taskInfos;
              roleArr.pushObject(roleObj);
            }, this);
          }
          return roleArr;
        }.property('startedTasks.@each'),

        didInsertElement: function () {
          console.log('The value of event context is: ' + host.name);
          this.get('roles').forEach(function(role){
            role.taskInfos.forEach(function(task){
              task.set('isLogHidden', true);
            });
          });
          $(this.get('element')).find('.content-area').each(function(index, value){
            var button = $(value).find('.textTrigger');
            $(value).mouseenter(
              function () {
                var element = $(this);
                element.css('border', '1px solid #dcdcdc');
                button.css('visibility', 'visible');
              }).mouseleave(
              function () {
                var element = $(this);
                element.css('border', 'none');
                button.css('visibility', 'hidden');
              })
          });

        },

        getStartedTasks: function (host) {
          var startedTasks = host.logTasks.filter(function (task) {
            return task.Tasks.status != 'PENDING' && task.Tasks.status != 'QUEUED';
          });
          return startedTasks;
        },

        toggleTaskLog: function (event, context) {
          var taskInfo = event.context;
          if (taskInfo.get('isLogHidden')) {
            var url = (App.testMode) ? '/data/wizard/deploy/task_log.json' : taskInfo.url;
            $.ajax({
              url: url,
              dataType: 'text',
              timeout: App.timeout,
              success: function (data) {
                var task = $.parseJSON(data);
                taskInfo.set('stdout', task.Tasks.stdout);
                taskInfo.set('stderr', task.Tasks.stderr);
                taskInfo.set('isLogHidden', false);
                taskInfo.set('isTextArea', false);
              },
              error: function () {
                alert('Failed to retrieve task log');
              }
            });
          } else {
            taskInfo.set('isLogHidden', true);
          }
        },
        textTrigger: function(event){
          var task = event.context;
          task.set('isTextArea', !task.get('isTextArea'));
        },
        textArea: Em.TextArea.extend({
          didInsertElement: function(){
            var element = $(this.get('element'));
            element.width($(this.get('parentView').get('element')).width() - 10);
            element.height($(this.get('parentView').get('element')).height());
            element.select();
            element.css('resize', 'none');
          },
          readOnly: true,
          value: function(){
            var taskInfo = this.get('content');
            var content = "";
            content += this.get('role').role  + " " + taskInfo.command + " log " + taskInfo.status + "\n";
            content += "stderr: " + taskInfo.stderr + "\n";
            content += "stdout: " + taskInfo.stdout + "\n";
            return content;
          }.property('content')
        })
      })
    });
  }

});

