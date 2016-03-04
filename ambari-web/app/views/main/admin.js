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

App.MainAdminView = Em.View.extend({
  templateName: require('templates/main/admin'),
  selectedBinding: 'controller.category',
  categories: function() {
    var items = [];
    if(App.isAuthorized('CLUSTER.VIEW_STACK_DETAILS, CLUSTER.UPGRADE_DOWNGRADE_STACK') || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
      items.push({
        name: 'stackAndUpgrade',
        url: 'stackAndUpgrade.index',
        label: Em.I18n.t('admin.stackUpgrade.title')
      });
    }
    if(App.isAuthorized('AMBARI.SET_SERVICE_USERS_GROUPS') || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
      items.push({
        name: 'adminServiceAccounts',
        url: 'adminServiceAccounts',
        label: Em.I18n.t('common.serviceAccounts')
      });
    }
    if (!App.get('isHadoopWindowsStack') && App.isAuthorized('CLUSTER.TOGGLE_KERBEROS') || (App.get('upgradeInProgress') || App.get('upgradeHolding')) ) {
      items.push({
        name: 'kerberos',
        url: 'adminKerberos.index',
        label: Em.I18n.t('common.kerberos')
      });
    }
    if (App.isAuthorized('SERVICE.START_STOP, CLUSTER.MODIFY_CONFIGS') || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
      if (App.supports.serviceAutoStart) {
        items.push({
          name: 'serviceAutoStart',
          url: 'adminServiceAutoStart',
          label: Em.I18n.t('admin.serviceAutoStart.title')
        });
      }
    }
    return items;
  }.property(''),

  NavItemView: Ember.View.extend({
    tagName: 'li',
    classNameBindings: 'isActive:active'.w(),
    isActive: Em.computed.equalProperties('item', 'parentView.selected')
  }),

  willDestroyElement: function () {
    //reset selected category in Admin sub-menu after leaving admin section
    this.set('controller.category', null);
  }
});

