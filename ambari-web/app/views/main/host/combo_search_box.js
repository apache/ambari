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
    this.restoreComboFilterQuery();
  },

  restoreComboFilterQuery: function() {
    var query = App.db.getComboSearchQuery(this.get('parentView.parentView.controller.name'));
    if (query) {
      visualSearch.searchBox.setQuery(query);
    }
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
          App.db.setComboSearchQuery(tableView.get('controller.name'), query);
          tableView.updateComboFilter(searchCollection);
        },

        facetMatches: function (callback) {
          var list = [
            {label: 'Host Name', value: 'hostName', category: 'Host'},
            {label: 'IP', value: 'ip', category: 'Host'},
            {label: 'Heath Status', value: 'healthClass', category: 'Host'},
            {label: 'Cores', value: 'cpu', category: 'Host'},
            {label: 'RAM', value: 'memoryFormatted', category: 'Host'},
            {label: 'Stack Version', value: 'version', category: 'Host'},
            {label: 'Version State', value: 'versionState', category: 'Host'},
            {label: 'Rack', value: 'rack', category: 'Host'},
            {label: 'Service', value: 'services', category: 'Service'},
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
              callback(App.HostStackVersion.find().toArray().filterProperty('isVisible', true).mapProperty('displayName').uniq());
              break;
            case 'versionState':
              callback(App.HostStackVersion.statusDefinition.map(function (status) {
                return {label: App.HostStackVersion.formatStatus(status), value: status};
              }));
              break;
            case 'healthClass':
              var category_mocks = require('data/host/categories');
              callback(category_mocks.slice(1).map(function (category) {
                return {label: category.value, value: category.healthStatus}
              }), {preserveOrder: true});
              break;
            case 'services':
              callback(App.Service.find().toArray().map(function (service) {
                return {label: App.format.role(service.get('serviceName')), value: service.get('serviceName')};
              }), {preserveOrder: true});
              break;
            case 'componentState':
              callback([{label: "All", value: "ALL"}]
                  .concat(App.HostComponentStatus.getStatusesList().map(function (status) {
                    return {label: App.HostComponentStatus.getTextStatus(status), value: status};
                  }))
                  .concat([
                    {label: "Inservice", value: "INSERVICE"},
                    {label: "Decommissioned", value: "DECOMMISSIONED"},
                    {label: "Decommissioning", value: "DECOMMISSIONING"},
                    {label: "RS Decommissioned", value: "RS_DECOMMISSIONED"},
                    {label: "Maintenance Mode On", value: "ON"},
                    {label: "Maintenance Mode Off", value: "OFF"}
                  ]), {preserveOrder: true});
              break;
          }
        }
      }
    });
  }
});