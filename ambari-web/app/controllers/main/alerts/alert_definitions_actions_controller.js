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

App.MainAlertDefinitionActionsController = Em.ArrayController.extend({

  name: 'mainAlertDefinitionActionsController',

  /**
   * List of available actions for alert definitions
   * @type {{title: string, icon: string, action: string, showDivider: boolean}[]}
   */
  content: [
    {
      title: Em.I18n.t('alerts.actions.create'),
      icon: 'icon-plus',
      action: 'createNewAlertDefinition',
      showDivider: true
    },
    {
      title: Em.I18n.t('alerts.actions.manageGroups'),
      icon: 'icon-th-large',
      action: 'manageAlertGroups',
      showDivider: false
    },
    {
      title: Em.I18n.t('alerts.actions.manageNotifications'),
      icon: 'icon-envelope-alt',
      action: 'manageNotifications',
      showDivider: false
    }
  ],

  /**
   * Common handler for menu item click
   * Call proper controller's method described in <code>action</code>-field (see <code>content</code>)
   * @param {object} event
   * @method actionHandler
   */
  actionHandler: function(event) {
    var menuElement = event.context,
      action = menuElement.action;
    if ('function' === Em.typeOf(Em.get(this, action))) {
      this[action]();
    }
    else {
      console.error('Invalid action provided - ', action);
    }
  },

  createNewAlertDefinition: Em.K,

  manageAlertGroups: Em.K,

  manageNotifications: Em.K

});