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
var validator = require('utils/validator');
var componentHelper = require('utils/component');

App.MainHostController = Em.ArrayController.extend({
  name:'mainHostController',
  content: App.Host.find(),
  comeWithFilter: false,

  alerts: function () {
    return App.router.get('clusterController.alerts').filterProperty('isOk', false).filterProperty('ignoredForHosts', false);
  }.property('App.router.clusterController.alerts.length'),

  /**
   * Components which will be shown in component filter
   */
  componentsForFilter:function() {
    var installedComponents = componentHelper.getInstalledComponents();
    installedComponents.setEach('checkedForHostFilter', false);
    return installedComponents;
  }.property('App.router.clusterController.isLoaded'),

  masterComponents:function () {
    return this.get('componentsForFilter').filterProperty('isMaster', true);
  }.property('componentsForFilter'),

  slaveComponents:function () {
    return this.get('componentsForFilter').filterProperty('isSlave', true);
  }.property('componentsForFilter'),

  clientComponents: function() {
    return this.get('componentsForFilter').filterProperty('isClient', true);
  }.property('componentsForFilter'),

  /**
   * Is true if alets filter is active
   */
  filteredByAlerts:false,

  /**
   * Is true if Hosts page was opened by clicking on alerts count badge
   */
  comeWithAlertsFilter: false,

  /**
   * Enable or disable filtering by alets
   */
  filterByAlerts: function () {
    if (App.router.get('currentState.name') == 'index') {
      this.set('filteredByAlerts', !this.get('filteredByAlerts'));
    } else {
      App.router.transitionTo('hosts.index');
      this.set('comeWithAlertsFilter', true);
    }
  },

  /**
   * Filter hosts by componentName of <code>component</code>
   * @param component App.HostComponent
   */
  filterByComponent:function (component) {
    var id = component.get('componentName');

    this.get('componentsForFilter').setEach('checkedForHostFilter', false);
    this.get('componentsForFilter').filterProperty('id', id).setEach('checkedForHostFilter', true);

    this.set('comeWithFilter', true);
  },

  /**
   * On click callback for decommission button
   * @param event
   */
  decommissionButtonPopup:function () {
    var self = this;
    App.ModalPopup.show({
      header:Em.I18n.t('hosts.decommission.popup.header'),
      body:Em.I18n.t('hosts.decommission.popup.body'),
      primary:Em.I18n.t('yes'),
      secondary:Em.I18n.t('no'),
      onPrimary:function () {
        alert('do');
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },

  /**
   * On click callback for delete button
   * @param event
   */
  deleteButtonPopup:function () {
    var self = this;
    App.ModalPopup.show({
      header:Em.I18n.t('hosts.delete.popup.header'),
      body:Em.I18n.t('hosts.delete.popup.body'),
      primary:Em.I18n.t('yes'),
      secondary:Em.I18n.t('no'),
      onPrimary:function () {
        self.removeHosts();
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },

  showAlertsPopup: function (event) {
    var host = event.context;
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        hostAlerts: function () {
          var allAlerts = App.router.get('clusterController.alerts').filterProperty('ignoredForHosts', false);
          if (host) {
            return allAlerts.filterProperty('hostName', host.get('hostName'));
          }
          return 0;
        }.property('App.router.clusterController.alerts'),

        closePopup: function () {
          this.get('parentView').hide();
        },

        templateName: require('templates/main/host/alerts_popup')
      }),
      primary: Em.I18n.t('common.close'),
      onPrimary: function() {
        this.hide();
      },
      secondary : null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
        this.$().children('.modal').css({'margin-top': '-350px'});
      }
    });
    event.stopPropagation();
  },

  /**
   * remove selected hosts
   */
  removeHosts:function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.hostName);
    });
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * remove hosts with id equal host_id
   * @param host_id
   */
  checkRemoved:function (host_id) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('id', host_id);
    this.get('fullContent').removeObjects(selectedHosts);
  }

});