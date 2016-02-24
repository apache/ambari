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
  fileSelectionService: Ember.inject.service('files-selection'),
  modalEventBus: Ember.inject.service('modal-event-bus'),
  alertMessages: Ember.inject.service('alert-messages'),
  filesDownloadService: Ember.inject.service('files-download'),

  classNames: ['row', 'context-menu-row'],
  selectedFilesCount: Ember.computed.oneWay('fileSelectionService.filesCount'),
  selectedFolderCount: Ember.computed.oneWay('fileSelectionService.folderCount'),
  isMultiSelected: Ember.computed('selectedFilesCount', 'selectedFolderCount', function() {
    return this.get('selectedFilesCount') + this.get('selectedFolderCount') > 1;
  }),
  isSingleSelected: Ember.computed('selectedFilesCount', 'selectedFolderCount', function() {
    return this.get('selectedFilesCount') + this.get('selectedFolderCount') === 1;
  }),
  isSelected: Ember.computed('selectedFilesCount', 'selectedFolderCount', function() {
    return (this.get('selectedFilesCount') + this.get('selectedFolderCount')) !== 0;
  }),
  isOnlyMultiFilesSelected: Ember.computed('selectedFilesCount', 'selectedFolderCount', function() {
    return this.get('selectedFolderCount') === 0 && this.get('selectedFilesCount') > 1;
  }),

  didInitAttrs: function() {
    // Register different modal so that they can be controlled from outside
    this.get('modalEventBus').registerModal('ctx-open');
    this.get('modalEventBus').registerModal('ctx-rename');
    this.get('modalEventBus').registerModal('ctx-permission');
    this.get('modalEventBus').registerModal('ctx-delete');
    this.get('modalEventBus').registerModal('ctx-copy');
    this.get('modalEventBus').registerModal('ctx-move');
    this.get('modalEventBus').registerModal('ctx-download');
    this.get('modalEventBus').registerModal('ctx-concatenate');
  },

  willDestroyElement() {
    this.get('modalEventBus').resetModal('ctx-open');
    this.get('modalEventBus').resetModal('ctx-rename');
    this.get('modalEventBus').resetModal('ctx-permission');
    this.get('modalEventBus').resetModal('ctx-delete');
    this.get('modalEventBus').resetModal('ctx-copy');
    this.get('modalEventBus').resetModal('ctx-move');
    this.get('modalEventBus').resetModal('ctx-download');
    this.get('modalEventBus').resetModal('ctx-concatenate');
  },

  actions: {
    open: function(event) {
      if (this.get('isSingleSelected')) {
        var file = this.get('fileSelectionService.files').objectAt(0);
        if (file.get('isDirectory')) {
          this.sendAction('openFolderAction', file.get('path'));
        } else {
          this.get('modalEventBus').showModal('ctx-open');
        }
      }

    },

    delete: function(event) {
      if (!this.get('isSelected')) {
        return false;
      }
      this.get('modalEventBus').showModal('ctx-delete');
    },

    copy: function(event) {
      if (!this.get('isSelected')) {
        return false;
      }
      this.get('modalEventBus').showModal('ctx-copy');
    },

    move: function(event) {
      if (!this.get('isSelected')) {
        return false;
      }
      this.get('modalEventBus').showModal('ctx-move');
    },

    download: function(event) {
      if (!this.get('isSelected')) {
        return false;
      }
      this.get('filesDownloadService').download();
    },

    concatenate: function(event) {
      if (!this.get('isOnlyMultiFilesSelected')) {
        return false;
      }
      this.get('filesDownloadService').concatenate();
    },

    rename: function(event) {
      if (!this.get('isSingleSelected')) {
        return false;
      }
      this.get('modalEventBus').showModal('ctx-rename');
    },
    permission: function(event) {
      if (!this.get('isSingleSelected')) {
        return false;
      }
      this.get('modalEventBus').showModal('ctx-permission');
    },

    modalClosed: function(modalName) {
      this.get('modalEventBus').resetModal(modalName);
    },

    refreshCurrentRoute: function() {
      this.get('fileSelectionService').reset();
      this.sendAction('refreshCurrentRouteAction');
    }
  }

});
