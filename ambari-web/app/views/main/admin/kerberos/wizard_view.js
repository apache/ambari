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

App.KerberosWizardView = Em.View.extend({

  templateName: require('templates/main/admin/kerberos/wizard'),

  isStep1Disabled: function () {
    return this.get('controller.isStepDisabled').findProperty('step',1).get('value');
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep2Disabled: function () {
    return this.get('controller.isStepDisabled').findProperty('step',2).get('value');
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep3Disabled: function () {
    return this.get('controller.isStepDisabled').findProperty('step',3).get('value');
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep4Disabled: function () {
    return this.get('controller.isStepDisabled').findProperty('step',4).get('value');
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep5Disabled: true,

  isStep6Disabled: true,

  isLoaded: false,

  willInsertElement: function () {
    if (this.get('controller').getDBProperty('hosts')) {
      this.set('isLoaded', true);
    } else {
      this.loadHosts();
    }
  },

  loadHosts: function () {
    App.ajax.send({
      name: 'hosts.confirmed',
      sender: this,
      data: {},
      success: 'loadHostsSuccessCallback',
      error: 'loadHostsErrorCallback'
    });
  },

  loadHostsSuccessCallback: function (response) {
    var installedHostNames = [];

    response.items.forEach(function (item) {
      installedHostNames.push(item.Hosts.host_name);
    });
    this.get('controller').setDBProperty('hosts', installedHostNames);
    this.set('controller.content.hosts', installedHostNames);
    this.set('isLoaded', true);
  },

  loadHostsErrorCallback: function(){
    this.set('isLoaded', true);
  }
});


