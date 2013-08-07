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

App.HighAvailabilityProgressPageView = Em.View.extend({

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  notice: Em.I18n.t('admin.highAvailability.wizard.progressPage.notice'),

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

    showProgressBar: function () {
      return this.get('content.status') === "IN_PROGRESS";
    }.property('content.status')
  })
});
