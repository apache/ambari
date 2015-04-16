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
    items.push({
      name: 'stackAndUpgrade',
      url: 'stackAndUpgrade.index',
      label: Em.I18n.t('admin.stackUpgrade.title')
    });
    items.push({
      name: 'adminServiceAccounts',
      url: 'adminServiceAccounts',
      label: Em.I18n.t('common.serviceAccounts')
    });
    if (!App.get('isHadoopWindowsStack')) {
      items.push({
        name: 'kerberos',
        url: 'adminKerberos.index',
        label: Em.I18n.t('common.kerberos')
      });
    }
    return items;
  }.property(''),

  NavItemView: Ember.View.extend({
    tagName: 'li',
    classNameBindings: 'isActive:active'.w(),
    isActive: function () {
      return this.get('item') === this.get('parentView.selected');
    }.property('item', 'parentView.selected')
  }),

  willDestroyElement: function () {
    //reset selected category in Admin sub-menu after leaving admin section
    this.set('controller.category', null);
  }
});

