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
  page_size: 10,

  VSCallbacks : {

    facetMatches: function (callback) {
      callback([
        {label: 'host_name', category: 'Host'},
        {label: 'ip', category: 'Host'},
        {label: 'version', category: 'Host'},
        {label: 'health', category: 'Host'},
        {label: 'rack', category: 'Host'},
        {label: 'service', category: 'Service'},
        {label: 'component', category: 'Service'},
        {label: 'state', category: 'Service'}
      ]);
    },

    valueMatches: function (facet, searchTerm, callback) {
      var controller = App.router.get('mainHostComboSearchBoxController');
      var category_mocks = require('data/host/categories');
      switch (facet) {
        case 'host_name':
        case 'ip':
          controller.getPropertySuggestions(facet, searchTerm).done(function() {
            callback(controller.get('currentSuggestion'), {preserveMatches: true});
          });
          break;
        case 'rack':
          callback(App.Host.find().toArray().mapProperty('rack').uniq());
          break;
        case 'version':
          callback(App.StackVersion.find().toArray().mapProperty('name'));
          break;
        case 'health':
          callback(category_mocks.slice(1).mapProperty('healthStatus'), {preserveOrder: true});
          break;
        case 'service':
          callback(App.Service.find().toArray().mapProperty('serviceName'), {preserveOrder: true});
          break;
        case 'component':
          callback(App.HostComponent.find().toArray().mapProperty('componentName').uniq(), {preserveOrder: true});
          break;
        case 'state':
          callback(App.HostComponentStatus.getStatusesList(), {preserveOrder: true});
          break;
      }
    }
  },

  getPropertySuggestions: function(facet, searchTerm) {
    return App.ajax.send({
      name: 'hosts.with_searchTerm',
      sender: this,
      data: {
        facet: facet,
        searchTerm: searchTerm,
        page_size: this.get('page_size')
      },
      success: 'getPropertySuggestionsSuccess',
      error: 'commonSuggestionErrorCallback'
    });
  },

  getPropertySuggestionsSuccess: function(data, opt, params) {
    this.updateSuggestion(data.items.map(function(item) {
      return item.Hosts[params.facet];
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