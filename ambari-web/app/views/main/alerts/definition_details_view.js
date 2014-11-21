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

App.MainAlertDefinitionDetailsView = App.TableView.extend({

  templateName: require('templates/main/alerts/definition_details'),

  /**
   * Determines if <code>controller.content</code> is loaded
   * @type {bool}
   */
  isLoaded: false,

  willInsertElement: function () {
    var self = this,
      updater = App.router.get('updateController');
    if (self.get('controller.content.isLoaded')) {
      self.set('isLoaded', true);
      self.get('controller').loadAlertInstances();
    }
    else {
      updater.updateAlertGroups(function () {
        updater.updateAlertDefinitions(function () {
          updater.updateAlertDefinitionSummary(function () {
            self.set('isLoaded', true);
            // App.AlertDefinition doesn't represents real models
            // Real model (see AlertDefinition types) should be used
            self.set('controller.content', App.AlertDefinition.getAllDefinitions().findProperty('id', parseInt(self.get('controller.content.id'))));
            self.get('controller').loadAlertInstances();
          });
        });
      });
    }
  },

  didInsertElement: function () {
    this.filter();
  },

  content: function () {
    return this.get('controller.alerts');
  }.property('controller.alerts.@each')
});
