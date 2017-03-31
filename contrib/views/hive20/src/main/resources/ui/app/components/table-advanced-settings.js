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
import fileFormats from '../configs/file-format';
import Helpers from '../configs/helpers';


export default Ember.Component.extend({

  classNames: ['create-table-advanced-wrap'],
  showLocationInput: false,
  showFileFormatInput: false,
  showRowFormatInput: false,
  shouldAddBuckets: false,
  errors: [],
  editMode: false,
  disableTransactionInput: false,
  disableNumBucketsInput: false,

  settings: {},

  errorsObserver: Ember.observer('errors.@each', function() {
    let numBucketsError = this.get('errors').findBy('type', 'numBuckets');
    if(!Ember.isEmpty(numBucketsError)) {
      this.set('hasNumBucketError', true);
      this.set('numBucketErrorText', numBucketsError.error);
    }
  }).on('init'),


  fileFormats: Ember.copy(fileFormats),
  terminationChars: Ember.computed(function () {
    return Helpers.getAllTerminationCharacters();
  }),

  didReceiveAttrs() {
    if (!Ember.isEmpty(this.get('settings.location'))) {
      this.set('showLocationInput', true);
    }
    if (!Ember.isEmpty(this.get('settings.fileFormat'))) {
      this.set('showFileFormatInput', true);
      let currentFileFormat = this.get('fileFormats').findBy('name', this.get('settings.fileFormat.type'));
      this.set('selectedFileFormat', currentFileFormat);
      this.set('customFileFormat', currentFileFormat.custom);
    } else {
      let defaultFileFormat = this.get('fileFormats').findBy('default', true);
      this.set('settings.fileFormat', {});
      this.set('settings.fileFormat.type', defaultFileFormat.name);
    }
    if (!Ember.isEmpty(this.get('settings.rowFormat'))) {
      this.set('showRowFormatInput', true);
      this.set('selectedFieldTerminator', this.get('settings.rowFormat.fieldTerminatedBy'));
      this.set('selectedLinesTerminator', this.get('settings.rowFormat.linesTerminatedBy'));
      this.set('selectedNullDefinition', this.get('settings.rowFormat.nullDefinedAs'));
      this.set('selectedEscapeDefinition', this.get('settings.rowFormat.escapeDefinedAs'));
    }
    if(!Ember.isEmpty(this.get('settings.transactional')) && this.get('settings.transactional') && this.get('editMode')) {
      this.set('disableTransactionInput', true);
    }

    if(!Ember.isEmpty(this.get('settings.numBuckets')) && this.get('settings.numBuckets') && this.get('editMode')) {
      this.set('disableNumBucketsInput', true);
    }
  },

  locationInputObserver: Ember.observer('showLocationInput', function () {
    if (!this.get('showLocationInput')) {
      this.set('settings.location');
    }
  }),

  fileFormatInputObserver: Ember.observer('showFileFormatInput', function () {
    if (!this.get('showFileFormatInput')) {
      this.set('settings.fileFormat');
    } else {
      this.set('selectedFileFormat', this.get('fileFormats').findBy('default', true));
    }
  }),

  rowFormatInputObserver: Ember.observer('showRowFormatInput', function () {
    if (!this.get('showRowFormatInput')) {
      this.send('clearFieldTerminator');
      this.send('clearLinesTerminator');
      this.send('clearNullDefinition');
      this.send('clearEscapeDefinition');
      this.set('settings.rowFormat');
    } else {
      this.set('settings.rowFormat', {});
    }
  }),

  actions: {

    closeHdfsModal() {
      this.set('showDirectoryViewer', false);
    },

    hdfsPathSelected(path) {
      this.set('settings.location', path);
      this.set('showDirectoryViewer', false);
    },

    toggleDirectoryViewer() {
      this.set('showDirectoryViewer', true);
    },

    toggleLocation() {
      this.toggleProperty('showLocationInput');
    },

    toggleFileFormat() {
      this.toggleProperty('showFileFormatInput');
    },

    toggleRowFormat() {
      this.toggleProperty('showRowFormatInput');
    },

    fileFormatSelected(format) {
      this.set('settings.fileFormat.type', format.name);
      this.set('selectedFileFormat', format);
      this.set('customFileFormat', format.custom);
    },

    fieldTerminatorSelected(terminator) {
      this.set('settings.rowFormat.fieldTerminatedBy', terminator);
      this.set('selectedFieldTerminator', terminator);
    },
    clearFieldTerminator() {
      this.set('settings.rowFormat.fieldTerminatedBy');
      this.set('selectedFieldTerminator');
    },

    linesTerminatorSelected(terminator) {
      this.set('settings.rowFormat.linesTerminatedBy', terminator);
      this.set('selectedLinesTerminator', terminator);
    },
    clearLinesTerminator() {
      this.set('settings.rowFormat.linesTerminatedBy');
      this.set('selectedLinesTerminator');
    },

    nullDefinedAsSelected(terminator) {
      this.set('settings.rowFormat.nullDefinedAs', terminator);
      this.set('selectedNullDefinition', terminator);
    },
    clearNullDefinition() {
      this.set('settings.rowFormat.nullDefinedAs');
      this.set('selectedNullDefinition');
    },

    escapeDefinedAsSelected(terminator) {
      this.set('settings.rowFormat.escapeDefinedAs', terminator);
      this.set('selectedEscapeDefinition', terminator);
    },
    clearEscapeDefinition() {
      this.set('settings.rowFormat.escapeDefinedAs');
      this.set('selectedEscapeDefinition');
    },
  }
});
