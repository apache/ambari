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

App.MainAdminSecurityDisableView = Em.View.extend({

  templateName: require('templates/main/admin/security/disable'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  },
  msgColor: 'alert-info',
  message: Em.I18n.t('admin.security.disable.body.header'),
  onResult: function () {
    var stopServiceCommand = this.get('controller.commands').findProperty('name', 'STOP_SERVICES');
    var applyConfigCommand = this.get('controller.commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    if (applyConfigCommand && applyConfigCommand.get('isSuccess') === true ) {
      this.set('message', Em.I18n.t('admin.security.disable.body.success.header'));
      this.set('msgColor', 'alert-success');
    } else if ((stopServiceCommand && stopServiceCommand.get('isError') === true) || (applyConfigCommand && applyConfigCommand.get('isError') === true)) {
      this.set('message', Em.I18n.t('admin.security.disable.body.failure.header'));
      this.set('msgColor', 'alert-error');
    } else {
      this.set('message', Em.I18n.t('admin.security.disable.body.header'));
      this.set('msgColor', 'alert-info');
    }
  }.observes('controller.commands.@each.isCompleted')
});
