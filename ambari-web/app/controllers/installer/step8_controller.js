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

App.InstallerStep8Controller = Em.ArrayController.extend({
  name: 'installerStep8Controller',
  contentBinding: Ember.Binding.oneWay('App.router.installerStep7Controller.content'),


  clearStep: function () {
    this.clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step8: Review Page")
    App.router.get('installerStep7Controller').loadStep();
    this.doConfigsUneditable();
  },

  doConfigsUneditable: function () {
    this.content.forEach(function (_service) {
      _service.get('configs').forEach(function (_serviceConfig) {
        console.log('value of isEditable before for: '+ _serviceConfig.name);
        console.log('value of isEditable before: '+ _serviceConfig.isEditable);
        console.log('value of displayType before: '+ _serviceConfig.displayType);
      _serviceConfig.set('isEditable',false);
        _serviceConfig.set('displayType','string');
        console.log('value of isEditable after for: '+ _serviceConfig.name);
        console.log('value of isEditable after: '+ _serviceConfig.isEditable);
        console.log('value of displayType after: '+ _serviceConfig.displayType);
      }, this);
    }, this);
  },

  navigateStep: function () {
    if (App.router.get('isFwdNavigation') === true) {
      this.loadStep();
    }
  }

});


