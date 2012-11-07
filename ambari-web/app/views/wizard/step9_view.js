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
      this.set('obj.message', Em.I18n.t('installer.step9.host.status.warning'));
    } else if (this.get('obj.status') === 'failed') {
      this.set('barColor', 'progress-danger');
      this.set('obj.message', Em.I18n.t('installer.step9.host.status.failed'));
    } else if (this.get('obj.status') === 'success') {
      this.set('barColor', 'progress-success');
      this.set('obj.message', Em.I18n.t('installer.step9.host.status.success'));
    }
  }.observes('obj.status'),

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

  hostLogPopup: function (event, context) {
    var self = this;
    var host = event.context;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step9.hostLog.popup.header') + event.context.get('name'),
      onPrimary: function () {
        this.hide();
      },
      controllerBinding: context,
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step9HostTasksLogPopup'),

        hostObj: function () {
          return this.get('parentView.obj');
        }.property('parentView.obj'),
        tasks: [],

        roles: function () {
          var roleArr = [];
          var tasks = this.get('tasks');
          if (tasks.length) {
            var role = this.get('tasks').mapProperty('Tasks.role').uniq();
            role.forEach(function (_role) {
              var statusArr = [];
              var roleObj = {};
              roleObj.roleName = _role;
              tasks.filterProperty('Tasks.role', _role).forEach(function (_task) {
                var statusObj = {};
                statusObj.status = _task.Tasks.command;
                statusObj.url = _task.href;
                statusArr.pushObject(statusObj);
              }, this);
              roleObj.statusArr = statusArr;
              roleArr.pushObject(roleObj);
            }, this);
          }
          return roleArr;
        }.property('tasks.@each'),

        didInsertElement: function () {
          console.log('The value of event context is: ' + host.name);
          this.set('tasks', self.get('controller').getCompletedTasksForHost(event.context));
        }
      })
    });
  }
});

