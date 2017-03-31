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
import ApplicationAdapter from './application';
import FileUploader from './file-uploader';

export default ApplicationAdapter.extend({
  tableOperations: Ember.inject.service(),

  buildURL: function(){
    return this._super(...arguments);
  },

  buildUploadURL: function (path) {
    return  this.buildURL() + "/upload/" + path;
  },

  uploadFiles: function (path, files, extras) {
    var uploadUrl = this.buildUploadURL(path);

    console.log("uplaoder : uploadURL : ", uploadUrl, " extras : ", extras , "files : ", files);

    var hdrs = Ember.$.extend(true, {},this.get('headers'));
    delete hdrs['Content-Type'];
    var uploader = FileUploader.create({
      headers: hdrs,
      url: uploadUrl
    });

    if (!Ember.isEmpty(files)) {
      var promise = uploader.upload(files[0], extras);
      return promise;
    }
  },

  createTable: function (tableData) {
    console.log("creating table with data :", tableData);
    return this.doPost("createTable",tableData);
  },

  insertIntoTable: function(insertData){
    console.log("inserting into table with data : ", insertData);
    return this.doPost("insertIntoTable",insertData);
  },

  deleteTable: function(deleteData){
    console.log("delete table with info : ", deleteData);
    return this.get('tableOperations').deleteTable(deleteData.database, deleteData.table);
  },

  doPost : function(path,inputData){
    var self = this;
    return new Ember.RSVP.Promise(function(resolve,reject){
                 Ember.$.ajax({
                     url :  self.buildUploadURL(path),
                     type : 'post',
                     data: JSON.stringify(inputData),
                     headers: self.get('headers'),
                     dataType : 'json'
                 }).done(function(data) {
                     resolve(data);
                 }).fail(function(error) {
                     reject(error);
                 });
              });
  },

  previewFromHDFS : function(previewFromHdfsData){
    console.log("preview from hdfs with info : ", previewFromHdfsData);
    return this.doPost("previewFromHdfs",previewFromHdfsData);
  },

  uploadFromHDFS : function(uploadFromHdfsData){
    console.log("upload from hdfs with info : ", uploadFromHdfsData);
    return this.doPost("uploadFromHDFS",uploadFromHdfsData);
  }
});
