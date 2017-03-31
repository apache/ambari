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
  healthStatusCategories: require('data/host/categories'),
  errMsg: '',
  serviceMap : {},

  didInsertElement: function () {
    this.initVS();
    this.restoreComboFilterQuery();
    this.showHideClearButton();
  },

  initVS: function() {
    this.setupLabelMap();
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
        search: this.search.bind(this),
        facetMatches: this.facetMatches.bind(this),
        valueMatches: this.valueMatches.bind(this)
      }
    });
  },

  /**
   * 'search' call back for visualsearch.js
   * @param query
   * @param searchCollection
   */
  search: function (query, searchCollection) {
    this.clearErrMsg();
    var invalidFacet = this.findInvalidFacet(searchCollection);
    if (invalidFacet) {
      this.showErrMsg(invalidFacet);
    }
    var tableView = this.get('parentView').get('parentView');
    App.db.setComboSearchQuery(tableView.get('controller.name'), query);
    var filterConditions = this.createFilterConditions(searchCollection);
    tableView.updateComboFilter(filterConditions);
  },

  /**
   * 'facetMatches' callback for visualsearch.js
   * @param callback
   */
  facetMatches: function (callback) {
    this.showHideClearButton();
    var list = [
      {label: 'Host Name', category: 'Host'},
      {label: 'IP', category: 'Host'},
      {label: 'Host Status', category: 'Host'},
      {label: 'Cores', category: 'Host'},
      {label: 'RAM', category: 'Host'},
      {label: 'Stack Version', category: 'Host'},
      {label: 'Version State', category: 'Host'},
      {label: 'Rack', category: 'Host'},
      {label: 'Service', category: 'Service'}
    ];
    var hostComponentList = this.getHostComponentList();
    // Add host component facets only when there isn't any component filter
    // with value other than ALL yet
    var currentComponentFacets = this.getComponentStateFacets(hostComponentList, false);
    if (currentComponentFacets.length == 0) {
      list = list.concat(hostComponentList);
    }
    list = this.filterOutOneFacetOnlyOptions(list);
    // Append host components
    callback(list, {preserveOrder: true});
  },

  /**
   * 'valueMatches' callback for visualsearch.js
   * @param facet
   * @param searchTerm
   * @param callback
   */
  valueMatches: function (facet, searchTerm, callback) {
    var controller = App.router.get('mainHostComboSearchBoxController');
    this.showHideClearButton();
    var map = App.router.get('mainHostController.labelValueMap');
    var serviceMap = this.get('serviceMap')
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
          return visualSearch.searchQuery.values(facet).indexOf(item) >= 0;
        }));
        break;
      case 'healthClass':
        var category_mocks = this.healthStatusCategories;
        callback(category_mocks.slice(1).map(function (category) {
          map[category.value] = category.healthStatus;
          return category.value;
        }).reject(function (item) {
          return visualSearch.searchQuery.values(facet).indexOf(item) >= 0;
        }), {preserveOrder: true});
        break;
      case 'services':
        callback(App.Service.find().toArray().map(function (service) {
          serviceMap[App.format.role(service.get('serviceName'), true)] = service.get('serviceName');
          return App.format.role(service.get('serviceName'), true);
        }).reject(function (item) {
          return visualSearch.searchQuery.values(facet).indexOf(item) >= 0;
        }), {preserveOrder: true});
        break;
      case 'componentState':
        var list = [ "All" ];
        // client only have "ALL" state
        if (facet.toLowerCase().indexOf("client") == -1)
        {
          var currentComponentFacets = this.getComponentStateFacets(null, true);
          if (currentComponentFacets.length == 0) {
            list = list.concat(App.HostComponentStatus.getStatusesList()
              .reject(function(status){return status == "UPGRADE_FAILED"}) // take out 'UPGRADE_FAILED'
              .map(function (status) {
                map[App.HostComponentStatus.getTextStatus(status)] = status;
                return App.HostComponentStatus.getTextStatus(status);
              }))
              .concat([
                "Inservice",
                "Decommissioned",
                "Decommissioning",
                "RS Decommissioned",
                "Maintenance Mode On",
                "Maintenance Mode Off"
              ]);
          }
        }
        callback(list, {preserveOrder: true});
        break;
    }
  },

  findInvalidFacet: function(searchCollection) {
    var result = null;
    var map = App.router.get('mainHostController.labelValueMap');
    for (var i = 0; i < searchCollection.models.length; i++) {
      var facet = searchCollection.models[i];
      if (!map[facet.attributes.category]) {
        result = facet;
        break;
      }
    }
    return result;
  },

  showErrMsg: function(category) {
    this.set('errMsg', category.attributes.value + " " + Em.I18n.t('hosts.combo.search.invalidCategory'));
  },

  clearErrMsg: function() {
    this.set('errMsg', '')
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
        .concat(App.SlaveComponent.find().rejectProperty('totalCount', 0).toArray()
        .concat(App.ClientComponent.find().rejectProperty('totalCount', 0).toArray()))
        .forEach(function(component) {
      var displayName = component.get('displayName');
      if (displayName) {
        hostComponentList.push({label: displayName, category: 'Component'});
        App.router.get('mainHostController.labelValueMap')[displayName] = component.get('componentName');
      }
    });
    hostComponentList = hostComponentList.sortProperty('label');
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

  getFacetsByName: function(name) {
    var facets = visualSearch.searchQuery.toJSON().filter(function(facet) {
      return facet.category === name;
    });
    return facets;
  },

  filterOutOneFacetOnlyOptions: function(options) {
    var self = this;
    var oneFacetOnlyLables = ['Cores', 'RAM'];
    oneFacetOnlyLables.forEach(function(label) {
      var facets = self.getFacetsByName(label);
      if (facets.length > 0) {
        options = options.rejectProperty('label', label);
      }
    });
    return options;
  },

  setupLabelMap: function() {
    var map = App.router.get('mainHostController.labelValueMap');
    map['All'] = 'ALL';
    map['Host Name'] = 'hostName';
    map['IP'] = 'ip';
    map['Host Status'] = 'healthClass';
    map['Cores'] = 'cpu';
    map['RAM'] = 'memoryFormatted';
    map['Stack Version'] = 'version';
    map['Version State'] = 'versionState';
    map['Rack'] = 'rack';
    map['Service'] = 'services';

    map['Inservice'] = 'INSERVICE';
    map['Decommissioned'] = 'DECOMMISSIONED';
    map['Decommissioning'] = 'DECOMMISSIONING';
    map['RS Decommissioned'] = 'RS_DECOMMISSIONED';
    map['Maintenance Mode On'] = 'ON';
    map['Maintenance Mode Off'] = 'OFF';
  },

  createFilterConditions: function(searchCollection) {
    var self = this;
    var mainHostController = App.router.get('mainHostController');
    var map = mainHostController.get('labelValueMap');
    var serviceMap = this.get('serviceMap');
    var filterConditions = Em.A();
    searchCollection.models.forEach(function (model) {
      var tag = model.attributes;
      var category = map[tag.category] || tag.category;
      var value = (category == 'services')? (serviceMap[tag.value] || tag.value) : (map[tag.value] || tag.value);

      var iColumn = self.getFilterColumn(category, value);
      var filterValue = self.getFilterValue(category, value);
      var condition = filterConditions.findProperty('iColumn', iColumn);
      if (condition) {
        // handle multiple facets with same category
        if (typeof condition.value == 'string') {
          condition.value = [condition.value, filterValue];
        } else if (Em.isArray(condition.value) && condition.value.indexOf(filterValue) == -1) {
          condition.value.push(filterValue);
        }
      } else {
        var type = 'string';
        if (category === 'cpu') {
          type = 'number';
        }
        if (category === 'memoryFormatted') {
          type = 'ambari-bandwidth';
        }
        condition = {
          skipFilter: false,
          iColumn: iColumn,
          value: filterValue,
          type: type
        };
        filterConditions.push(condition);
      }
    });
    return filterConditions;
  },

  getFilterColumn: function(category, value) {
    var iColumn = -1;
    if (this.get('controller').isComponentStateFacet(category)) {
      iColumn = App.router.get('mainHostController').get('colPropAssoc').indexOf('componentState');
    } else if (this.get('controller').isComplexHealthStatusFacet(value)) {
      iColumn = this.get('healthStatusCategories').findProperty('healthStatus', value).column;
    } else {
      iColumn = App.router.get('mainHostController').get('colPropAssoc').indexOf(category);
    }
    return iColumn;
  },

  getFilterValue: function(category, value) {
    var filterValue = value;
    if (this.get('controller').isComponentStateFacet(category)) {
      filterValue = category + ':' + value;
    } else if (this.get('controller').isComplexHealthStatusFacet(value)) {
      filterValue = this.get('healthStatusCategories').findProperty('healthStatus', value).filterValue;
    }
    return filterValue;
  }
});
