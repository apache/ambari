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

  clearFilters: null,

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
   * Filter hosts by componentName of <code>component</code>
   * @param component App.HostComponent
   */
  filterByComponent:function (component) {
    if(!component)
      return;
    var id = component.get('componentName');
    var column = 6;
    this.get('componentsForFilter').setEach('checkedForHostFilter', false);

    var filterForComponent = {
      iColumn: column,
      value: id,
      type: 'multiple'
    };
    App.db.setFilterConditions(this.get('name'), [filterForComponent]);
  },
  /**
   * On click callback for delete button
   */
  deleteButtonPopup:function () {
    var self = this;
    App.showConfirmationPopup(function(){
      self.removeHosts();
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
