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
      name: 'repositories',
      url: 'adminRepositories',
      label: Em.I18n.t('common.repositories')
    });
    items.push({
      name: 'serviceAccounts',
      url: 'adminServiceAccounts',
      label: Em.I18n.t('common.serviceAccounts')
    });
    if (App.supports.secureCluster) {
      items.push({
        name: 'security',
        url: 'adminSecurity.index',
        label: Em.I18n.t('common.security')
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
  })
});

