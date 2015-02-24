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

export default Ember.ObjectController.extend({
  needs: [ constants.namingConventions.udfs, constants.namingConventions.fileResources ],

  columns: Ember.computed.alias('controllers.' + constants.namingConventions.udfs + '.columns'),
  links: Ember.computed.alias('controllers.' + constants.namingConventions.udfs + '.links'),
  fileResources: Ember.computed.alias('controllers.' + constants.namingConventions.fileResources),

  init: function () {
    this._super();

    if (this.get('model.isNew')) {
      this.set('isEditing', true);
    }

    // we need this because model.rollback doesnt roll back secondary relations
    this.set('fileBackup', this.get('model.fileResource'));
  },

  actions: {
    executeAction: function (action) {
      switch (action) {
        case 'buttons.edit':
          this.set('isEditing', true);
          break;
        case 'buttons.delete':
          var defer = Ember.RSVP.defer(),
              self = this;

          this.send('openModal',
                    'modal-delete',
                     {
                        heading: Ember.I18n.translations.modals.delete.heading,
                        text: Ember.I18n.translations.modals.delete.message,
                        defer: defer
                     });

          defer.promise.then(function () {
            self.get('model').destroyRecord();
          });
          break;
      }
    },

    save: function () {
      var self = this,
          saveUdf = function () {
            self.get('model').save().then(function (updatedModel) {
              self.set('isEditing', false);
              self.set('isEditingResource', false);
              self.set('fileBackup', updatedModel.get('fileResource'));
            });
          };

      //replace with a validation system if needed.
      if (!this.get('model.name') || !this.get('model.classname')) {
        return;
      }

      this.get('model.fileResource').then(function (file) {
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

    cancel: function () {
      var self = this;

      this.set('isEditing', false);
      this.set('isEditingResource', false);

      this.model.get('fileResource').then(function (file) {
        if (file) {
          file.rollback();
        }

        self.model.rollback();
        self.model.set('fileResource', self.get('fileBackup'));
      });
    },

    addFileResource: function () {
      var file = this.store.createRecord(constants.namingConventions.fileResource);
      this.set('isEditingResource', true);
      this.model.set('fileResource', file);
    },

    editFileResource: function (file) {
      this.set('isEditingResource', true);
      this.model.set('fileResource', file);
    },

    removeFileResource: function (file) {
      var defer = Ember.RSVP.defer();

      this.send('openModal',
                'modal-delete',
                 {
                    heading: Ember.I18n.translations.modals.delete.heading,
                    text: Ember.I18n.translations.modals.delete.message,
                    defer: defer
                 });

      defer.promise.then(function () {
        file.destroyRecord();
      });
    }
  }
});
