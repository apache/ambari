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

App.MainAdminSecurityAddStep2View = Em.View.extend({

  templateName: require('templates/main/admin/security/add/step2'),

  configProperties: function () {
    var configProperties = [];
    var stepConfigs = this.get('controller.stepConfigs');
    if (stepConfigs) {
      this.get('controller.stepConfigs').mapProperty('configs').forEach(function (_stepProperties) {
        _stepProperties.forEach(function (_stepConfigProperty) {
          configProperties.pushObject(_stepConfigProperty);
        }, this);
      }, this);
    }
    return configProperties;
  }.property('controller.stepConfigs.@each.configs'),

  realmName: function () {
    return this.get('configProperties').findProperty('name', 'kerberos_domain');
  }.property('configProperties'),

  onRealmNameChange: function () {
    this.get('configProperties').forEach(function (_property) {
      if (/principal_name?$/.test(_property.get('name')) || _property.get('name') == 'namenode_principal_name_falcon') {
        _property.set('unit', '@' + this.get('realmName.value'));
      }
    }, this);
  }.observes('realmName.value')

});
