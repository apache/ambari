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

App.MainHostComboSearchBoxController = Em.Controller.extend({
  name: 'mainHostComboSearchBoxController',
  currentSuggestion: [],

  VSCallbacks : {
    search: function (query, searchCollection) {
      var $query = $('#search_query');
      var count = searchCollection.size();
      $query.stop().animate({opacity: 1}, {duration: 300, queue: false});
      $query.html('<span class="raquo">&raquo;</span> You searched for: ' +
      '<b>' + (query || '<i>nothing</i>') + '</b>. ' +
      '(' + count + ' facet' + (count == 1 ? '' : 's') + ')');
      clearTimeout(window.queryHideDelay);
      window.queryHideDelay = setTimeout(function () {
        $query.animate({
          opacity: 0
        }, {
          duration: 1000,
          queue: false
        });
      }, 2000);
    },

    facetMatches: function (callback) {
      console.log('called');
      callback([
        {label: 'name', category: 'Host'},
        {label: 'ip', category: 'Host'},
        {label: 'version', category: 'Host'},
        {label: 'health', category: 'Host'},
        {label: 'service', category: 'Service'},
        {label: 'component', category: 'Service'},
        {label: 'state', category: 'Service'}
      ]);
    },

    valueMatches: function (facet, searchTerm, callback) {
      var controller = App.router.get('mainHostComboSearchBoxController');
      switch (facet) {
        case 'name':
          controller.getHostPropertySuggestions('name', searchTerm).done(function() {
            callback(controller.get('currentSuggestion'));
          });
          break;
        case 'ip':
          callback(App.Host.find().toArray().mapProperty('ip'));
          break;
        case 'rack':
          callback(App.Host.find().toArray().mapProperty('rack').uniq());
          break;
        case 'version':
          callback(App.StackVersion.find().toArray().mapProperty('name'));
          break;
        case 'health':
          callback([
            Em.I18n.t('hosts.host.healthStatusCategory.green'),
            Em.I18n.t('hosts.host.healthStatusCategory.red'),
            Em.I18n.t('hosts.host.healthStatusCategory.orange'),
            Em.I18n.t('hosts.host.healthStatusCategory.yellow'),
            Em.I18n.t('hosts.host.alerts.label'),
            Em.I18n.t('common.restart'),
            Em.I18n.t('common.passive_state')
          ]);
          break;
        case 'service':
          callback(App.Service.find().toArray().mapProperty('serviceName'));
          break;
        case 'component':
          callback(App.MasterComponent.find().toArray().mapProperty('componentName')
              .concat(App.SlaveComponent.find().toArray().mapProperty('componentName'))
              .concat(App.ClientComponent.find().toArray().mapProperty('componentName'))
            ,{preserveOrder: true});
          break;
        case 'state':
          callback([
            Em.I18n.t('common.started'),
            Em.I18n.t('common.stopped'),
            Em.I18n.t('hosts.host.stackVersions.status.install_failed'),
            Em.I18n.t('hosts.host.decommissioning'),
            Em.I18n.t('hosts.host.decommissioned')
          ], {preserveOrder: true});
          break;
      }
    }
  },

  getHostPropertySuggestions: function(facet, searchTerm) {
    return App.ajax.send({
      name: 'hosts.all.install',
      sender: this,
      success: 'updateHostNameSuggestion',
      error: 'commonSuggestionErrorCallback'
    });
  },

  updateHostNameSuggestion: function(data) {
    this.updateSuggestion(data.items.map(function(item) {
      return item.Hosts.host_name;
    }));
  },

  updateSuggestion: function(data) {
    var controller = App.router.get('mainHostComboSearchBoxController');
    controller.set('currentSuggestion', data);
  },

  commonSuggestionErrorCallback:function() {
    // handle suggestion error
  }
});