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
  fileResources: [],

  sortAscending: true,
  sortProperties: [],

  columns: [
    Ember.Object.create({
      caption: 'placeholders.udfs.name',
      property: 'name'
    }),
    Ember.Object.create({
      caption: 'placeholders.udfs.className',
      property: 'classname'
    })
  ],

  model: function () {
    return this.filter(this.get('udfs'));
  }.property('udfs', 'filters.@each'),

  actions: {
    handleAddFileResource: function (udf) {
      var file = this.store.createRecord(constants.namingConventions.fileResource);
      udf.set('fileResource', file);
      udf.set('isEditingResource', true);
    },

    handleDeleteFileResource: function (file) {
      var defer = Ember.RSVP.defer();

      this.send('openModal',
                'modal-delete',
                 {
                    heading: 'modals.delete.heading',
                    text: 'modals.delete.message',
                    defer: defer
                 });

      defer.promise.then(function () {
        file.destroyRecord();
      });
    },

    handleSaveUdf: function (udf) {
      var self = this,
          saveUdf = function () {
            udf.save().then(function () {
              udf.set('isEditing', false);
              udf.set('isEditingResource', false);
            });
          };

      //replace with a validation system if needed.
      if (!udf.get('name') || !udf.get('classname')) {
        return;
      }

      udf.get('fileResource').then(function (file) {
        if (file) {
          if (!file.get('name') || !file.get('path')) {
            return;
          }

          file.save().then(function () {
            saveUdf();
          });
        } else {
          saveUdf();
        }
      });
    },

    handleDeleteUdf: function (udf) {
      var defer = Ember.RSVP.defer();

      this.send('openModal',
                'modal-delete',
                 {
                    heading: 'modals.delete.heading',
                    text: 'modals.delete.message',
                    defer: defer
                 });

      defer.promise.then(function () {
        udf.destroyRecord();
      });
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

    add: function () {
      this.store.createRecord(constants.namingConventions.udf);
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
