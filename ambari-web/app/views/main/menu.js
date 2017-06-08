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
        if (!App.get('isOnlyViewUser')) {
          result.push(
              {label: Em.I18n.t('menu.item.dashboard'), routing: 'dashboard', active: 'active'},
              {label: Em.I18n.t('menu.item.services'), routing: 'services'},
              {label: Em.I18n.t('menu.item.hosts'), routing: 'hosts'},
              {label: Em.I18n.t('menu.item.alerts'), routing: 'alerts'}
          );
        }
        if (App.isAuthorized('CLUSTER.TOGGLE_KERBEROS, CLUSTER.MODIFY_CONFIGS, SERVICE.START_STOP, SERVICE.SET_SERVICE_USERS_GROUPS, CLUSTER.UPGRADE_DOWNGRADE_STACK, CLUSTER.VIEW_STACK_DETAILS')
          || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          result.push({ label: Em.I18n.t('menu.item.admin'), routing: 'admin'});
        }
      }
      result.push({ label: Em.I18n.t('menu.item.views'), routing: 'views.index', isView: true, views: this.get('views').filterProperty('visible')});
    }
    return result;
  }.property(
    'App.router.loggedIn',
    'views.length',
    'App.router.clusterController.isLoaded',
    'App.router.clusterInstallCompleted',
    'App.router.wizardWatcherController.isWizardRunning'
  ),

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

    templateName: require('templates/main/menu_item'),

    dropdownMenu: Em.computed.existsIn('content.routing', ['services', 'admin', 'views']),
    isAdminItem: Em.computed.equal('content.routing', 'admin'),
    isServicesItem: Em.computed.equal('content.routing', 'services'),
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
        if(App.isAuthorized('CLUSTER.VIEW_STACK_DETAILS, CLUSTER.UPGRADE_DOWNGRADE_STACK') || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          categories.push({
            name: 'stackAndUpgrade',
            url: 'stack',
            label: Em.I18n.t('admin.stackUpgrade.title')
          });
        }
        if(App.isAuthorized('SERVICE.SET_SERVICE_USERS_GROUPS') ||  (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          categories.push({
            name: 'adminServiceAccounts',
            url: 'serviceAccounts',
            label: Em.I18n.t('common.serviceAccounts'),
            disabled: App.get('upgradeInProgress') || App.get('upgradeHolding')
          });
        }
        if (!App.get('isHadoopWindowsStack') && App.isAuthorized('CLUSTER.TOGGLE_KERBEROS') || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          if (App.supports.enableToggleKerberos) {
            categories.push({
              name: 'kerberos',
              url: 'kerberos/',
              label: Em.I18n.t('common.kerberos'),
              disabled: App.get('upgradeInProgress') || App.get('upgradeHolding')
            });
          }
        }
        if ((App.isAuthorized('SERVICE.START_STOP, CLUSTER.MODIFY_CONFIGS') && App.isAuthorized('SERVICE.MANAGE_AUTO_START, CLUSTER.MANAGE_AUTO_START')) || (App.get('upgradeInProgress') || App.get('upgradeHolding'))) {
          if (App.supports.serviceAutoStart) {
            categories.push({
              name: 'serviceAutoStart',
              url: 'serviceAutoStart',
              label: Em.I18n.t('admin.serviceAutoStart.title')
            });
          }
        }
      }
      return categories;
    }.property(''),

    AdminDropdownItemView: Ember.View.extend({
      tagName: 'li',
      classNameBindings: 'isActive:active isDisabled:disabled'.w(),
      isActive: Em.computed.equalProperties('item', 'parentView.selectedAdminItem'),
      isDisabled: function () {
        return !!this.get('parentView.dropdownCategories').findProperty('name', this.get('item'))['disabled'];
      }.property('item', 'parentView.dropdownCategories.@each.disabled'),
      goToCategory: function (event) {
        var itemName = this.get('parentView').get('content').routing;
        // route to correct category of current menu item
        // skip routing to already selected category
        if (itemName === 'admin' && !this.get('isActive') && !this.get('isDisabled')) {
          App.router.route('main/admin/' + event.context);
        }
      }
    })
  })
});
