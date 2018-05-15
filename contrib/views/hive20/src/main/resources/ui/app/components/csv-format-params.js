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
import Helpers from '../configs/helpers';

export default Ember.Component.extend({
  showCSVFormatInput: true,
  DEFAULT_CSV_DELIMITER: ',',
  DEFAULT_CSV_QUOTE: '"',
  DEFAULT_CSV_ESCAPE: '\\',
  DEFAULT_FILE_TYPE: 'CSV',
  csvParams: Ember.Object.create(),
  inputFileTypes: Ember.computed(function () {
    return Helpers.getUploadFileTypes();
  }),
  inputFileTypeCSV : Ember.computed.equal('fileFormatInfo.inputFileType.id',"CSV"),

  terminationChars: Ember.computed(function () {
    return Helpers.getAllTerminationCharacters();
  }),

  init: function(){
    this._super(...arguments);
    this.set('fileFormatInfo.csvParams.csvDelimiter',  this.get("terminationChars").findBy( "name", this.get('DEFAULT_CSV_DELIMITER') ));
    this.set('fileFormatInfo.csvParams.csvQuote', this.get("terminationChars").findBy( "name",  this.get('DEFAULT_CSV_QUOTE')));
    this.set('fileFormatInfo.csvParams.csvEscape', this.get("terminationChars").findBy( "name",  this.get('DEFAULT_CSV_ESCAPE')));
    this.set("fileFormatInfo.inputFileType", this.get("inputFileTypes").findBy("name"),  this.get('DEFAULT_FILE_TYPE'));
  },

  actions: {
    toggleCSVFormat: function () {
      console.log("inside toggleCSVFormat");
      this.toggleProperty('showCSVFormatInput');
    },
    clearColumnDelimter: function(){
      this.set('fileFormatInfo.csvParams.csvDelimiter');
    },
    csvDelimiterSelected: function(terminator){
      this.set('fileFormatInfo.csvParams.csvDelimiter', terminator);
    },
    csvEscapeSelected: function(terminator){
      this.set('fileFormatInfo.csvParams.csvEscape', terminator);
    },
    clearEscapeCharacter: function(){
      this.set('fileFormatInfo.csvParams.csvEscape');
    },
    csvQuoteSelected: function(terminator){
      this.set('fileFormatInfo.csvParams.csvQuote', terminator);
    },
    clearCsvQuote: function(){
      this.set('fileFormatInfo.csvParams.csvQuote');
    },
    inputFileTypeSelected: function(fileType){
      this.set("fileFormatInfo.inputFileType", fileType);
    },
    clearInputFileType: function(){
      this.set("fileFormatInfo.inputFileType");
    },
  }
});
