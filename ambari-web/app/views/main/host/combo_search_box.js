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

    var callbacks = this.get('controller').VSCallbacks;
    callbacks.search = function (query, searchCollection) {

      searchCollection.models.forEach(function (data) {
        var query = data.attributes;

        switch (query.category) {
          case 'health':
            self.get('parentView').get('parentView').updateFilter(0, query.value, 'string');
            break;
          case 'host_name':
            self.get('parentView').get('parentView').updateFilter(1, query.value, 'string');
            break;
          case 'ip':
            self.get('parentView').get('parentView').updateFilter(2, query.value, 'string');
            break;
          case 'rack':
            self.get('parentView').get('parentView').updateFilter(12, query.value, 'string');
            break;
          case 'version':
            self.get('parentView').get('parentView').updateFilter(11, query.value, 'string');
            break;
          case 'component':
            self.get('parentView').get('parentView').updateFilter(15, query.value, 'string');
            break;
          case 'service':
            self.get('parentView').get('parentView').updateFilter(13, query.value, 'string');
            break;
          case 'state':
            self.get('parentView').get('parentView').updateFilter(14, query.value, 'string');
            break;
        }
      });

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
    };

    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      delay: 1000,
      unquotable: [
        'text'
      ],
      callbacks: callbacks
    });
  }
});