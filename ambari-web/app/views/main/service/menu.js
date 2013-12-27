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
var misc = require('utils/misc');

App.MainServiceMenuView = Em.CollectionView.extend({
  content:function () {
    var items = App.router.get('mainServiceController.content').filter(function(item){
      if(['PIG', 'SQOOP', 'HCATALOG', 'TEZ'].contains(item.get('id'))){
        return false;
      }
      return true;
    });
    return misc.sortByOrder(App.Service.servicesSortOrder, items);
  }.property('App.router.mainServiceController.content', 'App.router.mainServiceController.content.length'),

  didInsertElement:function () {
    App.router.location.addObserver('lastSetURL', this, 'renderOnRoute');
    this.renderOnRoute();
    App.tooltip($(".restart-required-service"), {html:true, placement:"right"});
  },

  activeServiceId:null,

  /**
   *    Syncs navigation menu with requested URL
   */
  renderOnRoute:function () {
    var last_url = App.router.location.lastSetURL || location.href.replace(/^[^#]*#/, '');
    if (last_url.substr(1, 4) !== 'main' || !this._childViews) {
      return;
    }
    var reg = /^\/main\/services\/(\S+)\//g;
    var sub_url = reg.exec(last_url);
    var service_id = (null != sub_url) ? sub_url[1] : 1;
    this.set('activeServiceId', service_id);

  },

  tagName:'ul',
  classNames:["nav", "nav-list", "nav-services"],

  itemViewClass:Em.View.extend({

    classNameBindings:["active", "clients"],
    templateName:require('templates/main/service/menu_item'),
    restartRequiredMessage: null,

    shouldBeRestarted: function() {
      return this.get('content.hostComponents').someProperty('staleConfigs', true);
    }.property('content.hostComponents.@each.staleConfigs'),

    active:function () {
      return this.get('content.id') == this.get('parentView.activeServiceId') ? 'active' : '';
    }.property('parentView.activeServiceId'),

    alertsCount: function () {
      var allAlerts = App.router.get('clusterController.alerts');
      var serviceId = this.get('content.serviceName');
      if (serviceId) {
        return allAlerts.filterProperty('serviceType', serviceId).filterProperty('isOk', false).filterProperty('ignoredForServices', false).length;
      }
      return 0;
    }.property('App.router.clusterController.alerts'),

    refreshRestartRequiredMessage: function() {
      var restarted, componentsCount, hostsCount, message, tHosts, tComponents;
      restarted = this.get('content.restartRequiredHostsAndComponents');
      componentsCount = 0;
      hostsCount = 0;
      message = "";
      for (var host in restarted) {
        hostsCount++;
        componentsCount += restarted[host].length;
      }
      if (hostsCount > 1) {
        tHosts = Em.I18n.t('common.hosts');
      } else {
        tHosts = Em.I18n.t('common.host');
      }
      if (componentsCount > 1) {
        tComponents = Em.I18n.t('common.components');
      } else {
        tComponents = Em.I18n.t('common.component');
      }
      message += componentsCount + ' ' + tComponents + ' ' + Em.I18n.t('on') + ' ' +
        hostsCount + ' ' + tHosts + ' ' + Em.I18n.t('services.service.config.restartService.needToRestartEnd');
      this.set('restartRequiredMessage', message);
    }.observes('content.restartRequiredHostsAndComponents')
  })

});
