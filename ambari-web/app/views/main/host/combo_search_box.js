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

App.MainHostComboSearchBoxView = Em.View.extend({
  templateName: require('templates/main/host/combo_search_box'),
  didInsertElement: function () {
    this.initVS();
  },

  initVS: function() {
    var self = this;
    var controller = App.router.get('mainHostComboSearchBoxController');
    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      delay: 1000,
      unquotable: [
        'text'
      ],
      callbacks: {
        search: function (query, searchCollection) {
          var tableView = self.get('parentView').get('parentView');
          tableView.updateComboFilter(searchCollection);
        },

        facetMatches: function (callback) {
          var list = [
            {label: 'Host Name', value: 'hostName', category: 'Host'},
            {label: 'IP', value: 'ip', category: 'Host'},
            {label: 'Heath Status', value: 'healthClass', category: 'Host'},
            {label: 'Rack', value: 'rack', category: 'Host'},
            {label: 'Service', value: 'services', category: 'Service'},
            {label: 'Has Component', value: 'hostComponents', category: 'Service'},
          ];
          var hostComponentHash = {};
          App.HostComponent.find().toArray().forEach(function(component) {
            hostComponentHash[component.get('displayName')] = component;
          });
          for (key in hostComponentHash) {
            var name = hostComponentHash[key].get('componentName');
            var displayName = hostComponentHash[key].get('displayName');
            if (displayName != null && !controller.isClientComponent(name)) {
              list.push({label: displayName, value: name, category: 'Component'});
            }
          }
          // Append host components
          callback(list, {preserveOrder: true});
        },

        valueMatches: function (facet, searchTerm, callback) {
          var category_mocks = require('data/host/categories');
          if (controller.isComponentStateFacet(facet)) {
            facet = 'componentState'
          }
          switch (facet) {
            case 'hostName':
            case 'ip':
              facet = (facet == 'hostName')? 'host_name' : facet;
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
            case 'healthClass':
              callback(category_mocks.slice(1).mapProperty('healthStatus'), {preserveOrder: true});
              break;
            case 'services':
              callback(App.Service.find().toArray().mapProperty('serviceName'), {preserveOrder: true});
              break;
            case 'hostComponents':
              callback(App.HostComponent.find().toArray().mapProperty('componentName').uniq(), {preserveOrder: true});
              break;
            case 'state':
              callback(App.HostComponentStatus.getStatusesList(), {preserveOrder: true});
              break;
            case 'componentState':
              callback(['STARTED', 'STOPPED'], {preserveOrder: true});
          }
        }
      }
    });
  }
});