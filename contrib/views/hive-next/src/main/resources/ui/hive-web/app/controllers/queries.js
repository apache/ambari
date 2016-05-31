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

import Ember from 'ember';
import FilterableMixin from 'hive/mixins/filterable';
import constants from 'hive/utils/constants';

export default Ember.ArrayController.extend(FilterableMixin, {
  needs: [ constants.namingConventions.routes.history,
           constants.namingConventions.openQueries ],

  history: Ember.computed.alias('controllers.' + constants.namingConventions.routes.history),
  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),

  sortAscending: true,
  sortProperties: [],

  init: function () {
    this._super();

    this.set('columns', Ember.ArrayProxy.create({ content: Ember.A([
       Ember.Object.create({
        caption: "columns.shortQuery",
        property: 'shortQuery',
        link: constants.namingConventions.subroutes.savedQuery
      }),
      Ember.Object.create({
        caption: "columns.title",
        property: 'title',
        link: constants.namingConventions.subroutes.savedQuery
      }),
      Ember.Object.create({
        caption: "columns.database",
        property: 'dataBase',
        link: constants.namingConventions.subroutes.savedQuery
      }),
      Ember.Object.create({
        caption: "columns.owner",
        property: 'owner',
        link: constants.namingConventions.subroutes.savedQuery
      })
    ])}));
  },

  //row buttons
  links: [
    "buttons.history",
    "buttons.delete"
  ],

  model: function () {
    return this.filter(this.get('queries'));
  }.property('queries', 'filters.@each'),

  actions: {
    executeAction: function (action, savedQuery) {
      var self = this;

      switch (action) {
        case "buttons.history":
          this.get('history').filterBy('queryId', savedQuery.get('id'), true);
          this.transitionToRoute(constants.namingConventions.routes.history);
          break;
        case "buttons.delete":
          var defer = Ember.RSVP.defer();
          this.send('openModal',
                    'modal-delete',
                     {
                        heading: "modals.delete.heading",
                        text: "modals.delete.message",
                        defer: defer
                     });

          defer.promise.then(function () {
            savedQuery.destroyRecord();
            self.get('openQueries').updatedDeletedQueryTab(savedQuery);
          });

          break;
      }
    },

    sort: function (property) {
      //if same column has been selected, toggle flag, else default it to true
      if (this.get('sortProperties').objectAt(0) === property) {
        this.set('sortAscending', !this.get('sortAscending'));
      } else {
        this.set('sortAscending', true);
        this.set('sortProperties', [ property ]);
      }
    },

    clearFilters: function () {
      var columns = this.get('columns');

      if (columns) {
        columns.forEach(function (column) {
          var filterValue = column.get('filterValue');

          if (filterValue && typeof filterValue === 'string') {
            column.set('filterValue');
          }
        });
      }

      //call clear filters from Filterable mixin
      this.clearFilters();
    }
  }
});
