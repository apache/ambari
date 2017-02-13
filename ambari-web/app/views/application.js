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

App.ApplicationView = Em.View.extend({
  templateName: require('templates/application'),

  views: function () { 
    if (App.router.get('loggedIn')) { 
      return App.router.get('mainViewsController.ambariViews').filterProperty('visible'); 
    } else { 
      return []; 
    } }.property('App.router.mainViewsController.ambariViews.length', 'App.router.loggedIn'),

  /**
   * Create the breadcrums showing on ambari top bar
   * Eg, Home / Alerts / Metrics Monitor Status
   * @returns {array}
   */
  breadcrumbs: function () {
    var breadcrumbs = [];
    if (App.router.get('loggedIn')) {
      var home = {
        label: '<span class="glyphicon glyphicon-home"></span>',
        route: 'dashboard',
        disabled: false
      };
      if (App.router.get('currentState.parentState.name') == 'dashboard') {
        breadcrumbs.pushObject({
          label: '<span class="glyphicon glyphicon-home"></span>&nbsp;&nbsp;' +  Em.I18n.t('menu.item.dashboard'),
          disabled: true,
          lastItem: true
        });
      } else if (App.router.get('currentState.parentState.name') == 'hosts') {
        breadcrumbs.pushObject(home);
        breadcrumbs.pushObject({
          label: Em.I18n.t('menu.item.hosts'),
          disabled: true,
          lastItem: true
        });
      } else if (App.router.get('currentState.parentState.name') == 'hostDetails') {
        var hostName = App.router.get('mainHostDetailsController.content.hostName');
        breadcrumbs.pushObject(home);
        breadcrumbs.pushObject({
          label: Em.I18n.t('menu.item.hosts'),
          route: 'hosts',
          disabled: false
        });
        breadcrumbs.pushObject({
          label: hostName,
          disabled: true,
          lastItem: true
        });
      } else if (App.router.get('currentState.parentState.name') == 'alerts') {
        breadcrumbs.pushObject(home);
        if (App.router.get('currentState.name') == 'alertDetails') {
          breadcrumbs.pushObject({
            label: Em.I18n.t('menu.item.alerts'),
            route: 'alerts',
            disabled: false
          });
          breadcrumbs.pushObject({
            label: App.router.get('mainAlertDefinitionDetailsController.content.label'),
            disabled: true,
            lastItem: true
          });
        } else {
          breadcrumbs.pushObject({
            label: Em.I18n.t('menu.item.alerts'),
            disabled: true,
            lastItem: true
          });
        }
      } else if (App.router.get('currentState.parentState.name') == 'service') {
        breadcrumbs.pushObject(home);
        var serviceName = App.router.get('mainServiceItemController.content.displayName');
        breadcrumbs.pushObject({
          label: 'Service - ' + serviceName,
          disabled: true,
          lastItem: true
        });
      } else if (App.router.get('currentState.parentState.name') == 'admin'|| App.router.get('currentState.parentState.parentState.name') == 'admin') {
        breadcrumbs.pushObject(home);
        breadcrumbs.pushObject({
          label: 'Admin - ' + App.router.get('mainAdminController.categoryLabel'),
          disabled: true,
          lastItem: true
        });
      } else if (App.router.get('currentState.parentState.name') == 'views') {
        breadcrumbs.pushObject(home);
        breadcrumbs.pushObject({
          label: App.router.get('mainViewsDetailsController.content.label'),
          disabled: true,
          lastItem: true
        });
      }
    }
    return breadcrumbs;

  }.property('App.router.loggedIn', 'App.router.currentState.parentState.name',
      'App.router.mainHostDetailsController.content.hostName', 'App.router.mainAlertDefinitionDetailsController.content.label',
      'App.router.mainServiceItemController.content.displayName', 'App.router.mainAdminController.categoryLabel', 'App.router.mainViewsDetailsController.content.label'),

  goToSection: function (event) {
    if (!event.context) return;
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

  didInsertElement: function () {
    // on 'Enter' pressed, trigger modal window primary button if primary button is enabled(green)
    // on 'Esc' pressed, close the modal
    $(document).keydown(function (event) {
      if (event.which === 13 || event.keyCode === 13) {
        $('.modal:last').trigger('enter-key-pressed');
      }
      return true;
    });
    $(document).keyup(function (event) {
      if (event.which === 27 || event.keyCode === 27) {
        $('.modal:last').trigger('escape-key-pressed');
      }
      return true;
    });
  },

  /**
   * Navigation Bar should be initialized after cluster data is loaded
   */
  initNavigationBar: function () {
    if (App.get('router.mainController.isClusterDataLoaded')) {
      Em.run.next(() => $('.navigation-bar').navigationBar({
        fitHeight: true,
        collapseNavBarClass: 'icon-double-angle-left',
        expandNavBarClass: 'icon-double-angle-right'
      }));
    }
  }.observes('App.router.mainController.isClusterDataLoaded')

});
