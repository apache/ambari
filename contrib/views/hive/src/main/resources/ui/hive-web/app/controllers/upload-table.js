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
import Uploader from 'hive/adapters/upload-table'
import constants from 'hive/utils/constants';


export default Ember.Controller.extend({
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  needs : ['databases'],
  showErrors : false,
  uploader: Uploader.create(),
  baseUrl: "/resources/upload",
  isFirstRowHeader: null, // is first row  header
  header: null,  // header received from server
  files: null, // files that need to be uploaded only file[0] is relevant
  firstRow: [], // the actual first row of the table.
  rows: null,  // preview rows received from server
  databaseName:null,
  selectedDatabase : null,
  filePath : null,
  tableName: null,
  dataTypes : [
     "TINYINT", //
     "SMALLINT", //
     "INT", //
     "BIGINT", //
     "BOOLEAN", //
     "FLOAT", //
     "DOUBLE", //
     "STRING", //
     "BINARY", // -- (Note: Available in Hive 0.8.0 and later)
     "TIMESTAMP", // -- (Note: Available in Hive 0.8.0 and later)
     "DECIMAL", // -- (Note: Available in Hive 0.11.0 and later)
     "DATE", // -- (Note: Available in Hive 0.12.0 and later)
     "VARCHAR", // -- (Note: Available in Hive 0.12.0 and later)
     "CHAR" // -- (Note: Available in Hive 0.13.0 and later)
  ],
  isFirstRowHeaderDidChange: function () {
    console.log("inside onFirstRowHeader : isFirstRowHeader : " + this.get('isFirstRowHeader'));
    if (this.get('isFirstRowHeader') != null && typeof this.get('isFirstRowHeader') !== 'undefined') {
      if (this.get('isFirstRowHeader') == false) {
        if (this.get('rows')) {
          this.get('rows').unshiftObject({row: this.get('firstRow')});
        }
      } else {
        // take first row of
        this.get('header').forEach(function (item, index) {
          console.log("item : ", item);
          console.log("this.get('firstRow').objectAt(index)  : ", this.get('firstRow').objectAt(index));
          Ember.set(item, 'name', this.get('firstRow')[index]);
        }, this);

        this.get('rows').removeAt(0);
      }

      this.printValues();
    }
  }.observes('isFirstRowHeader'),

  uploadForPreview: function (files) {
    console.log("uploaderForPreview called.");
    return this.get('uploader').uploadFiles('preview', files);
  },

  clearFields: function () {
    this.set("header");
    this.set("rows");
    this.set("error");
    this.set('isFirstRowHeader');
    this.set('files');
    this.set("firstRow");
    this.set("selectedDatabase");
    this.set("databaseName");
    this.set("filePath");
    this.set('tableName');

    this.printValues();
  },

  printValues: function () {
    console.log("printing all values : ");
    console.log("header : ", this.get('header'));
    console.log("rows : ", this.get('rows'));
    console.log("error : ", this.get('error'));
    console.log("isFirstRowHeader : ", this.get('isFirstRowHeader'));
    console.log("files : ", this.get('files'));
    console.log("firstRow : ", this.get('firstRow'));
  },
  previewTable: function (data) {
    console.log('inside previewTable');
    this.set("header", data.header);
    this.set("rows", data.rows);
    this.set("firstRow", data.rows[0].row);
    console.log("firstRow : ", this.get('firstRow'));
    this.set('isFirstRowHeader', data.isFirstRowHeader);
  },

  fetchCreateTableStatus: function (jobId, resolve, reject) {
    var self = this;
    this.get('uploader').getCreateTableResult(jobId).then(function (data) {
      console.log("fetchCreateTableStatus : data : ", data);
      var status = data.status;
      if (status == "Succeeded") {
        console.log("resolving fetchCreateTableStatus with : " + data);
        resolve(status);
      } else if (status == "Canceled" || status == "Closed" || status == "Error") {
        console.log("rejecting fetchCreateTableStatus with : " + status);
        reject(new Error(status));
      } else {
        console.log("retrying fetchCreateTableStatus : ");
        self.fetchCreateTableStatus(jobId, resolve, reject);
      }
    }, function (error) {
      console.log("rejecting fetchCreateTableStatus with : " + error);
      reject(error);
    })
  },

  waitForResult: function (jobId) {
    var self = this;
    return new Ember.RSVP.Promise(function (resolve, reject) {
      self.fetchCreateTableStatus(jobId,resolve,reject);
    });
  },

  createTable: function () {
    var headers = JSON.stringify(this.get('header'));

    var selectedDatabase = this.get('selectedDatabase');
    if( null == selectedDatabase || typeof selectedDatabase === 'undefined'){
      throw new Error(constants.hive.errors.emptyDatabase);
    }

    this.set('databaseName',this.get('selectedDatabase').get('name'));
    var databaseName = this.get('databaseName');
    var tableName = this.get('tableName');
    var isFirstRowHeader = this.get('isFirstRowHeader');
    console.log("databaseName : " , databaseName, ", tableName : ", tableName, ", isFirstRowHeader : " , isFirstRowHeader , ", headers : ", headers);

    if( null == databaseName || typeof databaseName === 'undefined'){
      throw new Error(constants.hive.errors.emptyDatabase);
    }
    if( null == tableName || typeof tableName === 'undefined'){
      throw new Error(constants.hive.errors.emptyTableName);
    }
    if( null == isFirstRowHeader || typeof isFirstRowHeader === 'undefined'){
      throw new Error(constants.hive.errors.emptyIsFirstRow);
    }

    this.validateColumns();

    return this.get('uploader').createTable({
      "isFirstRowHeader": isFirstRowHeader,
      "header": headers,
      "tableName": tableName,
      "databaseName": databaseName
    });
  },

  validateColumns: function(){
    // TODO :check validation of columnames.
    // throw exception if invalid.
  },
  setError: function(error){
    this.set('error',JSON.stringify(error));
    console.log("upload table error : ",error);
    this.get('notifyService').error(error);
  },

  previewError: function (error) {
    this.setError(error);
  },

  uploadTable: function () {
    this.printValues();
    return this.get('uploader').uploadFiles('upload', this.get('files'), {
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "filePath": this.get('filePath')
    });
  },

  onUploadSuccessfull: function (data) {
    console.log("onUploadSuccessfull : ", data);
    this.get('notifyService').success( "Uploaded Successfully", "Table " + this.get('tableName') + " created in database " + this.get("databaseName"));
    this.clearFields();
  },

  onUploadError: function (error) {
    console.log("onUploadError : ", error);
    this.setError(error);
  },

  actions: {
    toggleErrors : function(){
      this.toggleProperty('showErrors');
    },
    filesUploaded: function (files) {
      console.log("upload-table.js : uploaded new files : ", files);

      this.clearFields();

      this.set('files', files);
      var name = files[0].name;
      var i = name.indexOf(".");
      var tableName = name.substr(0, i);
      this.set('tableName', tableName);
      var self = this;
      return this.uploadForPreview(files).then(function (data) {
        self.previewTable(data);
      }, function (error) {
        self.previewError(error);
      });
    },

    createTableAndUploadFile: function () {
      var self = this;

      try {
        this.createTable()
          .then(function (jobData) {
            console.log("jobData : ", jobData);
            self.set('filePath', jobData.filePath);
            self.waitForResult(jobData.jobId)
              .then(function (successStatus) {
                console.log("successStatus : ", successStatus);
                self.uploadTable().then(function (operationData) {
                  console.log("operation successfull operationData : ", operationData);
                  self.onUploadSuccessfull(operationData);
                }, function (error) {
                  self.onUploadError(error);
                });
              }, function (error) {
                self.onUploadError(error);
              })
          }, function (error) {
            self.onUploadError(error);
          })
      }catch(e){
        self.onUploadError(e);
      }
    }

  }
});
