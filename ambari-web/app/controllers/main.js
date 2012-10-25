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
require('models/background_operation');

App.MainController = Em.Controller.extend({
  name: 'mainController',
  backgroundOperations: null,
  intervalId: false,
  updateOperationsInterval: 8000,

  startLoadOperationsPeriodically: function() {
    this.intervalId = setInterval(this.loadBackgroundOperations, this.get('updateOperationsInterval'));
  },
  stopLoadOperationsPeriodically:function () {
    if(this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.intervalId = false;
  },
  loadBackgroundOperations: function(){
    var self = App.router.get('mainController');
    jQuery.getJSON('data/hosts/background_operations/bg_operations.json',
      function (data) {
        var backgroundOperations = self.get('backgroundOperations');
        if(!backgroundOperations || self.get('backgroundOperationsCount') >= 6)
          self.set('backgroundOperations', data);
        else backgroundOperations.tasks.pushObjects(data['tasks'])
      }
    )
  },

  backgroundOperationsCount: function() {
    return this.get('backgroundOperations.tasks.length');
  }.property('backgroundOperations.tasks.length'),

  showBackgroundOperationsPopup: function(){
    App.ModalPopup.show({
      headerClass: Ember.View.extend({
        controllerBinding: 'App.router.mainController',
        template:Ember.Handlebars.compile('{{backgroundOperationsCount}} Background Operations Running')
      }),
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.mainController',
        templateName: require('templates/main/background_operations_popup')
      }),
      onPrimary: function() {
        this.hide();
      }
    });
  }
})