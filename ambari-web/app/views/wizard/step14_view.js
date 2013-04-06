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

App.WizardStep14View = Em.View.extend({

  templateName: require('templates/wizard/step14'),

  statusMessage: null,
  statusClass: 'alert-info',

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  tasks: function () {
    var tasks = this.get('controller.tasks');
    if (this.get('controller.service.serviceName') == 'GANGLIA') {
      tasks = tasks.slice(0,2).concat(tasks.slice(4));
    }
    return tasks;
  }.property('controller.tasks', 'controller.service'),

  onStatus: function () {
    var master = (this.get('controller.isCohosted')) ? Em.I18n.t('installer.step5.hiveGroup') : this.get('controller.content.reassign.display_name');
    switch (this.get('controller.status')) {
      case 'COMPLETED':
        this.set('statusMessage', Em.I18n.t('installer.step14.status.success').format(master));
        this.set('statusClass', 'alert-success');
        break;
      case 'FAILED':
        this.set('statusMessage', Em.I18n.t('installer.step14.status.failed').format(master));
        this.set('statusClass', 'alert-error');
        break;
      case 'IN_PROGRESS':
      default:
        this.set('statusMessage', Em.I18n.t('installer.step14.status.info').format(master));
        this.set('statusClass', 'alert-info');
    }
  }.observes('controller.status'),

  taskView: Em.View.extend({
    icon: '',
    iconColor: '',

    didInsertElement: function () {
      this.onStatus();
    },

    barWidth: function () {
      return 'width: ' + this.get('content.progress') + '%;';
    }.property('content.progress'),

    onStatus: function () {
      if (this.get('content.status') === 'IN_PROGRESS') {
        this.set('icon', 'icon-cog');
        this.set('iconColor', 'text-info');
      } else if (this.get('content.status') === 'WARNING') {
        this.set('icon', 'icon-warning-sign');
        this.set('iconColor', 'text-warning');
      } else if (this.get('content.status') === 'FAILED') {
        this.set('icon', 'icon-exclamation-sign');
        this.set('iconColor', 'text-error');
      } else if (this.get('content.status') === 'COMPLETED') {
        this.set('icon', 'icon-ok');
        this.set('iconColor', 'text-success');
      } else {
        this.set('icon', 'icon-cog');
        this.set('iconColor', '');
      }
    }.observes('content.status'),

    inProgress: function () {
      return this.get('content.status') === "IN_PROGRESS";
    }.property('content.status')

  })
});
