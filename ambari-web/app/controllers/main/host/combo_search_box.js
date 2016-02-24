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
  nameColumnMap: {
    'Host Name': 'hostName',
    'IP': 'ip',
    'Health Status': 'hostName',
    'Host Name': 'healthClass',
    'Rack': 'rack',
    'Cores': 'cpu',
    'RAM': 'memoryFormatted',
    'Service': 'service',
    'Has Component': 'hostComponents',
    'State': 'state'
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