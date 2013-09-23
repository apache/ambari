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

App.WizardStep13View = App.HighAvailabilityProgressPageView.extend({

  headerTitle: Em.I18n.t('installer.step13.header'),

  hostComponents: function () {
    var result = '';
    this.get('controller.hostComponents').forEach(function (comp, index) {
      result += index ? ', ' : '';
      result += App.format.role(comp);
    }, this);
    return result;
  }.property('controller.hostComponents'),

  noticeInProgress: function () {
    return Em.I18n.t('installer.step13.status.info').format(this.get('hostComponents'));
  }.property('hostComponents'),

  noticeFailed: function () {
    return Em.I18n.t('installer.step13.status.failed').format(this.get('hostComponents'));
  }.property('hostComponents'),

  noticeCompleted: function () {
    return Em.I18n.t('installer.step13.status.success').format(this.get('hostComponents'));
  }.property('hostComponents'),

  submitButtonText: Em.I18n.t('common.complete'),

  templateName: require('templates/wizard/step13')
});
