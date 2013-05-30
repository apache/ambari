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

App.WizardStep12Controller = App.MainServiceInfoConfigsController.extend({

  modifiedConfigs: [],

  afterLoad: function () {
    if (this.get('dataIsLoaded')) {
      this.get('stepConfigs').objectAt(0).get('configs').filterProperty('isEditable', false).setEach('isEditable', true);
      this.get('stepConfigs').objectAt(0).get('configs').filterProperty('displayType', 'masterHost').setEach('isVisible', false);
    }
  }.observes('dataIsLoaded'),

  addHostNamesToGlobalConfig: function () {
    var hostComponents = [];
    this.get('content.masterComponentHosts').forEach(function (component) {
      hostComponents.push(Ember.Object.create({
        componentName: component.component,
        host: {hostName: component.hostName}
      }))
    });
    this.set('content.hostComponents', hostComponents);
    this._super();
  },

  submit: function () {
    if (this.get('isSubmitDisabled')) {
      return false;
    }
    var self = this;
    this.get('modifiedConfigs').clear();
    this.get('stepConfigs').objectAt(0).get('configs').forEach(function (config) {
      if (config.get('defaultValue') !== config.get('value')) {
        self.get('modifiedConfigs').push({
          name: config.get('displayName'),
          oldValue: config.get('defaultValue'),
          value: config.get('value'),
          unit: config.get('unit') || false
        });
      }
    });
    App.router.send('next');
  }
});
