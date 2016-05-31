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
import constants from 'hive/utils/constants';

export default Ember.Component.extend({
  tagName: 'tr',

  didInsertElement: function () {
    this._super();

    if (this.get('udf.isNew')) {
      this.set('udf.isEditing', true);
    }
  },

  setfileBackup: function () {
    if (!this.get('udf.isDirty')) {
      this.set('fileBackup', this.get('udf.fileResource'));
    }
  }.observes('udf.isDirty').on('didInsertElement'),

  actions: {
    editUdf: function () {
      this.set('udf.isEditing', true);
    },

    deleteUdf: function () {
      this.sendAction('onDeleteUdf', this.get('udf'));
    },

    addFileResource: function () {
      this.sendAction('onAddFileResource', this.get('udf'));
    },

    editFileResource: function (file) {
      this.set('udf.fileResource', file);
      this.set('udf.isEditingResource', true);
    },

    deleteFileResource: function (file) {
      this.sendAction('onDeleteFileResource', file);
    },

    save: function () {
      this.sendAction('onSaveUdf', this.get('udf'));
    },

    cancel: function () {
      var self = this;

      this.set('udf.isEditing', false);
      this.set('udf.isEditingResource', false);

      this.udf.get('fileResource').then(function (file) {
        if (file) {
          file.rollback();
        }

        self.udf.rollback();
        self.udf.set('fileResource', self.get('fileBackup'));
      });
    }
  }
});
