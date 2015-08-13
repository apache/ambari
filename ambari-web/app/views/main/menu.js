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

/**
 * this menu extended by other with modifying content and itemViewClass.template
 * @type {*}
 */
App.MainMenuView = Em.CollectionView.extend({
  tagName: 'ul',
  classNames: ['nav', 'top-nav-menu'],

  views: function () {
    return App.router.get('mainViewsController.ambariViews');
  }.property('App.router.mainViewsController.ambariViews'),

  content: function () {
    var result = [];
    if (App.router.get('loggedIn')) {

      if (App.router.get('clusterController.isLoaded') && App.get('router.clusterInstallCompleted')) {

        result.push(
          { label: Em.I18n.t('menu.item.dashboard'), routing: 'dashboard', active: 'active'},
          { label: Em.I18n.t('menu.item.services'), routing: 'services'},
          { label: Em.I18n.t('menu.item.hosts'), routing: 'hosts', hasAlertsLabel: true},
          { label: Em.I18n.t('menu.item.alerts'), routing: 'alerts'}
        );
        if (App.isAccessible('upgrade_ADMIN')) {
          result.push({ label: Em.I18n.t('menu.item.admin'), routing: 'admin'});
        }
      }
      result.push({ label: Em.I18n.t('menu.item.views'), routing: 'views.index', isView: true, views: this.get('views').filterProperty('visible')});
    }
    return result;
  }.property('App.router.loggedIn', 'views.length',
    'App.router.clusterController.isLoaded', 'App.router.clusterInstallCompleted'),

  itemViewClass: Em.View.extend({

    classNameBindings: ['active', ':top-nav-dropdown'],

    active: function () {
      if (App.get('clusterName') && App.router.get('clusterController.isLoaded')) {
        var last_url = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
        if (last_url.substr(1, 4) !== 'main' || !this._childViews) {
          return;
        }
        var reg = /^\/main\/([a-z]+)/g;
        var sub_url = reg.exec(last_url);
        var chunk = (null != sub_url) ? sub_url[1] : 'dashboard';
        return this.get('content.routing').indexOf(chunk) === 0 ? "active" : "";
      }
      return "";
    }.property('App.router.location.lastSetURL', 'App.router.clusterController.isLoaded'),

    alertsCount: function () {
      return App.router.get('mainHostController.hostsCountMap.health-status-WITH-ALERTS');
    }.property('App.router.mainHostController.hostsCountMap'),

    hasCriticalAlerts: function () {
      return App.router.get('mainHostController.hostsCountMap.health-status-CRITICAL') > 0;
    }.property('content.hasAlertsLabel', 'alertsCount'),

    hasAlertsLabel: function () {
      return this.get('content.hasAlertsLabel') && this.get('alertsCount') > 0;
    }.property('content.hasAlertsLabel', 'alertsCount'),

    templateName: require('templates/main/menu_item'),

    dropdownMenu: function () {
      var item = this.get('content').routing;
      var itemsWithDropdown = ['services', 'admin', 'views'];
      return itemsWithDropdown.contains(item);
    }.property(''),
    isAdminItem: function () {
      return this.get('content').routing == 'admin';
    }.property(''),
    isServicesItem: function () {
      return this.get('content').routing == 'services';
    }.property(''),
    isViewsItem: function () {
      return this.get('content').routing.contains('views');
    }.property(''),
    goToSection: function (event) {
      if (event.context === 'hosts') {
        App.router.set('mainHostController.showFilterConditionsFirstLoad', false);
      } else if (event.context === 'views') {
        App.router.route('views');
        return;
      } else if (event.context === 'alerts') {
        App.router.set('mainAlertDefinitionsController.showFilterConditionsFirstLoad', false);
      }
      App.router.route('main/' + event.context);
    },

    selectedAdminItemBinding: 'App.router.mainAdminController.category',

    dropdownCategories: function () {
      var itemName = this.get('content').routing;
      var categories = [];
      // create dropdown categories for each menu item
      if (itemName == 'admin') {
        categories = [];
        categories.push({
          name: 'stackAndUpgrade',
          url: 'stack',
          label: Em.I18n.t('admin.stackUpgrade.title')
        });
        categories.push({
          name: 'adminServiceAccounts',
          url: 'serviceAccounts',
          label: Em.I18n.t('common.serviceAccounts')
        });
        if (!App.get('isHadoopWindowsStack')) {
          categories.push({
            name: 'kerberos',
            url: 'kerberos/',
            label: Em.I18n.t('common.kerberos')
          });
        }
      }
      return categories;
    }.property(''),

    AdminDropdownItemView: Ember.View.extend({
      tagName: 'li',
      classNameBindings: 'isActive:active'.w(),
      isActive: function () {
        return this.get('item') === this.get('parentView.selectedAdminItem');
      }.property('item', 'parentView.selectedAdminItem'),

      goToCategory: function (event) {
        var itemName = this.get('parentView').get('content').routing;
        // route to correct category of current menu item
        // skip routing to already selected category
        if (itemName === 'admin' && !this.get('isActive')) {
          App.router.route('main/admin/' + event.context);
        }
      }
    })
  })
});
