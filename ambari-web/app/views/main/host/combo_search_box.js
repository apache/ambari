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
    this.showHideClearButton();
  },

  showHideClearButton: function() {
    if(visualSearch.searchQuery.toJSON().length > 0) {
      $('.VS-cancel-search-box').removeClass('hide');
    } else {
      $('.VS-cancel-search-box').addClass('hide');
    }
  },

  restoreComboFilterQuery: function() {
    var query = App.db.getComboSearchQuery(this.get('parentView.parentView.controller.name'));
    if (query) {
      visualSearch.searchBox.setQuery(query);
    }
  },

  getHostComponentList: function() {
    var hostComponentList = [];
    App.MasterComponent.find().rejectProperty('totalCount', 0).toArray()
        .concat(App.SlaveComponent.find().rejectProperty('totalCount', 0).toArray())
        .forEach(function(component) {
      var displayName = component.get('displayName');
      if (displayName) {
        hostComponentList.push({label: displayName, category: 'Component'});
        App.router.get('mainHostController.labelValueMap')[displayName] = component.get('componentName');
      }
    });
    return hostComponentList;
  },

  getComponentStateFacets: function(hostComponentList, includeAllValue) {
    if (!hostComponentList) {
      hostComponentList = this.getHostComponentList();
    }
    var currentComponentFacets = visualSearch.searchQuery.toJSON().filter(function (facet) {
      var result = !!(hostComponentList.findProperty('label', facet.category) && facet.value);
      if (!includeAllValue) {
        result &= (facet.value != 'All');
      }
      return result;
    });
    return currentComponentFacets;
  },

  initVS: function() {
    var self = this;
    var controller = App.router.get('mainHostComboSearchBoxController');
    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      delay: 1000,
      placeholder: Em.I18n.t('hosts.combo.search.placebolder'),
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
          self.showHideClearButton();
          var list = [
            {label: 'Host Name', category: 'Host'},
            {label: 'IP', category: 'Host'},
            {label: 'Heath Status', category: 'Host'},
            {label: 'Cores', category: 'Host'},
            {label: 'RAM', category: 'Host'},
            {label: 'Stack Version', category: 'Host'},
            {label: 'Version State', category: 'Host'},
            {label: 'Rack', category: 'Host'},
            {label: 'Service', category: 'Service'}
          ];
          var map = App.router.get('mainHostController.labelValueMap');
          map['Host Name'] = 'hostName';
          map['IP'] = 'ip';
          map['Heath Status'] = 'healthClass';
          map['Cores'] = 'cpu';
          map['RAM'] = 'memoryFormatted';
          map['Stack Version'] = 'version';
          map['Version State'] = 'versionState';
          map['Rack'] = 'rack';
          map['Service'] = 'services';

          var hostComponentList = self.getHostComponentList();
          // Add host component facets only when there isn't any component filter
          // with value other than ALL yet
          var currentComponentFacets = self.getComponentStateFacets(hostComponentList, false);
          if (currentComponentFacets.length == 0) {
            list = list.concat(hostComponentList);
          }
          // Append host components
          callback(list, {preserveOrder: true});
        },

        valueMatches: function (facet, searchTerm, callback) {
          self.showHideClearButton();
          var map = App.router.get('mainHostController.labelValueMap');
          var facetValue = map[facet] || facet;
          if (controller.isComponentStateFacet(facetValue)) {
            facetValue = 'componentState'
          }
          switch (facetValue) {
            case 'hostName':
            case 'ip':
              controller.getPropertySuggestions(facetValue, searchTerm).done(function() {
                callback(controller.get('currentSuggestion').reject(function (item) {
                  return visualSearch.searchQuery.values(facet).indexOf(item) >= 0; // reject the ones already in search
                }), {preserveMatches: true});
              });
              break;
            case 'rack':
              callback(App.Host.find().toArray().mapProperty('rack').uniq().reject(function (item) {
                return visualSearch.searchQuery.values(facet).indexOf(item) >= 0;
              }));
              break;
            case 'version':
              callback(App.HostStackVersion.find().toArray()
                .filterProperty('isVisible', true).mapProperty('displayName').uniq().reject(function (item) {
                  return visualSearch.searchQuery.values(facet).indexOf(item) >= 0;
                }));
              break;
            case 'versionState':
              callback(App.HostStackVersion.statusDefinition.map(function (status) {
                map[App.HostStackVersion.formatStatus(status)] = status;
                return App.HostStackVersion.formatStatus(status);
              }).reject(function (item) {
                return visualSearch.searchQuery.values(facet).indexOf(item.value) >= 0;
              }));
              break;
            case 'healthClass':
              var category_mocks = require('data/host/categories');
              callback(category_mocks.slice(1).map(function (category) {
                map[category.value] = category.healthStatus;
                return category.value;
              }).reject(function (item) {
                return visualSearch.searchQuery.values(facet).indexOf(item.value) >= 0;
              }), {preserveOrder: true});
              break;
            case 'services':
              callback(App.Service.find().toArray().map(function (service) {
                map[App.format.role(service.get('serviceName'), true)] = service.get('serviceName');
                return App.format.role(service.get('serviceName'), true);
              }).reject(function (item) {
                return visualSearch.searchQuery.values(facet).indexOf(item.value) >= 0;
              }), {preserveOrder: true});
              break;
            case 'componentState':
              var list = [ "All" ];
              map['All'] = 'ALL';
              var currentComponentFacets = self.getComponentStateFacets(null, true);
              if (currentComponentFacets.length == 0) {
                list = list.concat(App.HostComponentStatus.getStatusesList().map(function (status) {
                  map[App.HostComponentStatus.getTextStatus(status)] = status;
                  return App.HostComponentStatus.getTextStatus(status);
                })).concat([
                    "Inservice",
                    "Decommissioned",
                    "Decommissioning",
                    "RS Decommissioned",
                    "Maintenance Mode On",
                    "Maintenance Mode Off"
                ]);
                map['Inservice'] = 'INSERVICE';
                map['Decommissioned'] = 'DECOMMISSIONED';
                map['Decommissioning'] = 'DECOMMISSIONING';
                map['RS Decommissioned'] = 'RS_DECOMMISSIONED';
                map['Maintenance Mode On'] = 'ON';
                map['Maintenance Mode Off'] = 'OFF';
              }
              callback(list, {preserveOrder: true});
              break;
          }
        }
      }
    });
  }
});