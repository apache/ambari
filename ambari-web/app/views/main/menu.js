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
  tagName:'ul',
  classNames:['nav', 'top-nav-menu'],

  views: function() {
    return App.router.get('clusterController.ambariViews');
  }.property('App.router.clusterController.ambariViews'),

  content:function(){
    var result = [];
    if (App.router.get('loggedIn')) {

      if (App.router.get('clusterController.isLoaded')) {

        result.push(
          { label:Em.I18n.t('menu.item.dashboard'), routing:'dashboard', active:'active'},
          { label:Em.I18n.t('menu.item.services'), routing:'services'},
          { label:Em.I18n.t('menu.item.hosts'), routing:'hosts'}
        );

        if (App.supports.mirroring && App.Service.find().findProperty('serviceName', 'FALCON')) {
          result.push({ label:Em.I18n.t('menu.item.mirroring'), routing:'mirroring'});
        }

        if (!App.get('isHadoop2Stack')) {
          result.push({ label:Em.I18n.t('menu.item.jobs'), routing:'apps'});
        }

        if (App.get('isAdmin')) {
          result.push({ label:Em.I18n.t('menu.item.admin'), routing:'admin'});
        }

      }

      if (App.supports.views) {
        result.push({ label:Em.I18n.t('menu.item.views'), routing:'views', isView:true, views: this.get('views')});
      }

    }
    return result;
  }.property('App.router.loggedIn', 'App.router.clusterController.isLoaded', 'App.supports.views', 'App.supports.mirroring', 'App.supports.secureCluster', 'App.supports.highAvailability'),

    /**
     *    Adds observer on lastSetURL and calls navigation sync procedure
     */
  didInsertElement:function () {
    this.renderOnRoute();
  },

  /**
   *    Syncs navigation menu with requested URL
   */
  renderOnRoute: function () {
    if (App.router.get('clusterController.isLoaded')) {
      var last_url = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
      if (last_url.substr(1, 4) !== 'main' || !this._childViews) {
        return;
      }
      var reg = /^\/main\/([a-z]+)/g;
      var sub_url = reg.exec(last_url);
      var chunk = (null != sub_url) ? sub_url[1] : 'dashboard';
      $.each(this._childViews, function () {
        this.set('active', this.get('content.routing') == chunk ? "active" : "");
      });
    }
  }.observes('App.router.location.lastSetURL', 'App.router.clusterController.isLoaded'),

  itemViewClass:Em.View.extend({

    classNameBindings:['active', ':top-nav-dropdown'],
    active:'',

    alertsCount:function () {
      if (this.get('content').routing == 'hosts') {
        return App.router.get('mainHostController.content').mapProperty('criticalAlertsCount')
          .reduce(function(pv, cv) { return pv + parseInt(cv); }, 0);
      }
    }.property('App.router.mainHostController.content.@each.criticalAlertsCount'),

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
      return this.get('content').routing == 'views';
    }.property(''),
    goToSection: function (event) {
      if (event.context === 'hosts') {
        App.router.set('mainHostController.showFilterConditionsFirstLoad', false);
      }
      App.router.route('main/' + event.context);
    },
    goToCategory: function (event) {
      var itemName = this.get('content').routing;
      // route to correct category of current menu item
      if (itemName == 'admin') {
        App.router.transitionTo('admin.' + event.context);
      }
    },
    dropdownCategories: function () {
      var itemName = this.get('content').routing;
      var categories = [];
      // create dropdown categories for each menu item
      if (itemName == 'admin') {
        categories = [{
          name: 'user',
          url: 'adminUser',
          label: Em.I18n.t('common.users')
        }];
        if (App.get('isHadoop2Stack') && App.supports.highAvailability) {
          categories.push({
            name: 'highAvailability',
            url: 'adminHighAvailability',
            label: Em.I18n.t('admin.highAvailability')
          });
        }
        if (App.supports.secureCluster) {
          categories.push({
            name: 'security',
            url: 'adminSecurity.index',
            label: Em.I18n.t('common.security')
          });
        }
        categories.push({
          name: 'cluster',
          url: 'adminCluster',
          label: Em.I18n.t('common.cluster')
        });
        categories.push({
          name: 'misc',
          url: 'adminMisc',
          label: Em.I18n.t('common.misc')
        });
        if (App.router.get('mainAdminController.isAccessAvailable')) {
          categories.push({
            name: 'access',
            url: 'adminAccess',
            label: Em.I18n.t('common.access')
          });
        }
      }
      return categories;

    }.property('')
  })
});
