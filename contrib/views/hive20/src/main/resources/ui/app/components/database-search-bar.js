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

export default Ember.Component.extend({
  classNames: ['database-search', 'clearfix'],
  databases: [],

  heading: 'database',
  subHeading: 'Select or search database/schema',
  enableSecondaryAction: true,
  secondaryActionText: 'Browse',
  secondaryActionFaIcon: 'folder',

  extendDrawer: false,
  filterText: '',

  selectedDatabase: Ember.computed('databases.@each.selected', function() {
    return this.get('databases').findBy('selected', true);
  }),

  filteredDatabases: Ember.computed('filterText', 'databases.@each', function() {
    return this.get('databases').filter((item) => {
      return item.get('name').indexOf(this.get('filterText')) !== -1;
    });
  }),

  resetDatabaseSelection() {
    this.get('databases').forEach(x => {
        if (x.get('selected')) {
          x.set('selected', false);
        }
    });
  },

  didRender() {
    this._super(...arguments);
    this.$('input.display').on('focusin', () => {
      this.set('extendDrawer', true);
      Ember.run.later(() => {
        this.$('input.search').focus();
      });
    });
  },

  actions: {
    secondaryActionClicked: function() {
      this.toggleProperty('extendDrawer');
      Ember.run.later(() => {
        this.$('input.search').focus();
      });
    },

    databaseClicked: function(database) {
      this.resetDatabaseSelection();
      database.set('selected', true);
      this.set('extendDrawer', false);
      this.set('filterText', '');
      this.sendAction('selected', database);
    }
  }
});
