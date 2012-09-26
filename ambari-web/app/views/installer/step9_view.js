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

App.InstallerStep9View = Em.View.extend({

  templateName: require('templates/installer/step9'),
  barColor: 'progress-info',
  resultMsg: '',
  resultMsgColor: '',

  didInsertElement: function () {
    var controller = this.get('controller');
    controller.clear();
    var hosts = controller.loadHosts();
    controller.renderHosts(hosts);
    //TODO: uncomment following line after the hook up with the API call
    //controller.startPolling();
  },

  barWidth: function () {
    var controller = this.get('controller');
    var barWidth = 'width: ' + controller.get('progress') + '%;';
    return barWidth;
  }.property('controller.progress'),

  onStatus: function () {
    if (this.get('controller.status') === 'info') {
      console.log('TRACE: Inside info view step9');
      this.set('barColor', 'progress-info');
    } else if (this.get('controller.status') === 'warning') {
      console.log('TRACE: Inside warning view step9');
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
  barColor: 'progress-info',


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

  isFailed: function() {
    if(this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'failed') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted','controller.status'),

  isSuccess: function() {
    if(this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'success') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted','controller.status'),

  isWarning: function() {
    if(this.get('controller.isStepCompleted') === true && this.get('obj.status') === 'warning') {
      return true;
    } else {
      return false;
    }
  }.property('controller.isStepCompleted','controller.status')

});

