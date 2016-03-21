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
  isLocalUpload : Ember.computed.equal("uploadSource","local"),
  uploadSource : "local",
  hdfsPath : "",
  jobService: Ember.inject.service(constants.namingConventions.job),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  needs: ['databases'],
  showErrors: false,
  uploader: Uploader.create(),
  baseUrl: "/resources/upload",
  isFirstRowHeader: true, // is first row  header
  header: null,  // header received from server
  files: null, // files that need to be uploaded only file[0] is relevant
  firstRow: [], // the actual first row of the table.
  rows: null,  // preview rows received from server
  databaseName: null,
  selectedDatabase: null,
  filePath: null,
  tableName: null,
  uploadProgressInfos : [],
  onChangeUploadSource : function(){
    this.clearFields();
  }.observes("uploadSource"),
  uploadProgressInfo : Ember.computed("uploadProgressInfos.[]",function(){
    var info = "";
    for( var i = 0 ; i < this.get('uploadProgressInfos').length ; i++)
        info += this.get('uploadProgressInfos').objectAt(i);

    return new Ember.Handlebars.SafeString(info);
  }),
  inputFileTypes :[
    {id : "CSV", name : "CSV"},
    {id : "JSON", name : "JSON"},
    {id : "XML", name : "XML"}
  ],
  inputFileType : {id : "CSV", name : "CSV"},
  inputFileTypeCSV : Ember.computed.equal('inputFileType.id',"CSV"),
  fileTypes:[
    "SEQUENCEFILE",
    "TEXTFILE"    ,
    "RCFILE"      ,
    "ORC"         ,
    "PARQUET"     ,
    "AVRO"        ,
    "INPUTFORMAT"
  ],
  selectedFileType: "ORC",
  dataTypes: [
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
      } else if( this.get('header') ) { // headers are available
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

  popUploadProgressInfos : function(){
    var msg = this.get('uploadProgressInfos').popObject();
   // console.log("popedup message : " + msg);
  },

  pushUploadProgressInfos : function(info){
    this.get('uploadProgressInfos').pushObject(info);
   // console.log("pushed message : " + info);
  },

  clearUploadProgressModal : function(){
  //  console.log("inside clearUploadProgressModal this.get('uploadProgressInfos') : " + this.get('uploadProgressInfos'));
    var len = this.get('uploadProgressInfos').length;
    for( var i = 0 ; i < len ; i++){
      this.popUploadProgressInfos();
    }
  },

  hideUploadModal : function(){
    console.log("hiding the modal ....");
    this.clearUploadProgressModal();
    Ember.$("#uploadProgressModal").modal("hide");
  },

  showUploadModal : function(){
    Ember.$("#uploadProgressModal").modal("show");
  },

  clearFields: function () {
    this.set("hdfsPath");
    this.set("header");
    this.set("rows");
    this.set("error");
    this.set('isFirstRowHeader',true);
    this.set('files');
    this.set("firstRow");
    this.set("selectedDatabase",null);
    this.set("databaseName");
    this.set("filePath");
    this.set('tableName');
    this.clearUploadProgressModal();
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

  generateTempTableName : function(){
    var text = "";
    var possible = "abcdefghijklmnopqrstuvwxyz";

    for( var i=0; i < 30; i++ )
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  },

  waitForJobStatus: function (jobId, resolve, reject) {
    console.log("finding status of job: ", jobId);
    var self = this;
    var fetchJobPromise = this.get('jobService').fetchJobStatus(jobId);
      fetchJobPromise.then(function (data) {
        console.log("waitForJobStatus : data : ", data);
        var status = data.jobStatus;
        if (status == "Succeeded") {
          console.log("resolving waitForJobStatus with : " , status);
          resolve(status);
        } else if (status == "Canceled" || status == "Closed" || status == "Error") {
          console.log("rejecting waitForJobStatus with : " + status);
          reject(new Error(status));
        } else {
          console.log("retrying waitForJobStatus : ");
          self.waitForJobStatus(jobId, resolve, reject);
        }
      }, function (error) {
        console.log("rejecting waitForJobStatus with : " + error);
        reject(error);
    })
  },

  uploadForPreview: function (files) {
    console.log("uploaderForPreview called.");
    var self = this;
    return this.get('uploader').uploadFiles('preview', files, {"isFirstRowHeader" : self.get("isFirstRowHeader"), "inputFileType" : self.get("inputFileType").id});
  },

  uploadForPreviewFromHDFS : function(){
    console.log("uploadForPreviewFromHDFS called.");
    return this.get('uploader').previewFromHDFS({"isFirstRowHeader" : this.get("isFirstRowHeader"),"inputFileType" : this.get("inputFileType").id , "hdfsPath" : this.get("hdfsPath") });
  },

  generatePreview : function(files){
    var self = this;
    var promise = null;
    this.waitForGeneratingPreview();
    if(this.get('isLocalUpload')){
      promise = this.uploadForPreview(files);
    }else{
      promise = this.uploadForPreviewFromHDFS();
    }

    return promise.then(function (data) {
        self.onGeneratePreviewSuccess(data);
    }, function (error) {
        self.onGeneratePreviewFailure(error);
    });
  },

  waitForGeneratingPreview: function () {
    console.log("waitForGeneratingPreview");
    this.showUploadModal();
    this.pushUploadProgressInfos("<li> Generating Preview .... </li>")
  },

  previewTable: function (data) {
    console.log('inside previewTable');
    this.set("header", data.header);
    this.set("firstRow", data.rows[0].row);
    console.log("firstRow : ", this.get('firstRow'));
    this.set('isFirstRowHeader', data.isFirstRowHeader);
    this.set('tableName',data.tableName);
    if(data.isFirstRowHeader == true){
        data.rows = data.rows.slice(1);
    }
    this.set("rows", data.rows);
  },

  onGeneratePreviewSuccess: function (data) {
    console.log("onGeneratePreviewSuccess");
    this.hideUploadModal();
    this.previewTable(data);
  },

  onGeneratePreviewFailure: function (error) {
    console.log("onGeneratePreviewFailure");
    this.hideUploadModal();
    this.setError(error);
  },

  createTable: function () {
    console.log("table headers : ", this.get('header'));
    var headers = this.get('header');

    var selectedDatabase = this.get('selectedDatabase');
    if (null == selectedDatabase || typeof selectedDatabase === 'undefined') {
      throw new Error(Ember.I18n.t('hive.errors.emptyDatabase'));
    }

    this.set('databaseName', this.get('selectedDatabase').get('name'));
    var databaseName = this.get('databaseName');
    var tableName = this.get('tableName');
    var isFirstRowHeader = this.get('isFirstRowHeader');
    var filetype = this.get("selectedFileType");

    if (null == databaseName || typeof databaseName === 'undefined' || databaseName == '') {
      throw new Error(Ember.I18n.t('hive.errors.emptyDatabase'));
    }
    if (null == tableName || typeof tableName === 'undefined' || tableName == '') {
      throw new Error(Ember.I18n.t('hive.errors.emptyTableName'));
    }
    if (null == isFirstRowHeader || typeof isFirstRowHeader === 'undefined') {
      throw new Error(Ember.I18n.t('hive.errors.emptyIsFirstRow'));
    }

    this.validateColumns();

    return this.get('uploader').createTable({
      "isFirstRowHeader": isFirstRowHeader,
      "header": headers,
      "tableName": tableName,
      "databaseName": databaseName,
      "fileType":filetype
    });
  },

  createActualTable : function(){
    console.log("createActualTable");
    this.pushUploadProgressInfos("<li> Starting to create Actual table.... </li>");
    return this.createTable();
  },

  waitForCreateActualTable: function (jobId) {
    console.log("waitForCreateActualTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Waiting for creation of Actual table.... </li>");
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onCreateActualTableSuccess : function(){
    console.log("onCreateTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Successfully created Actual table. </li>");
  },

  onCreateActualTableFailure : function(error){
    console.log("onCreateActualTableFailure");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Failed to create Actual table. </li>");
    this.setError(error);
  },

  createTempTable : function(){
    console.log("createTempTable");
    this.pushUploadProgressInfos("<li> Starting to create Temporary table.... </li>");
    var tempTableName = this.generateTempTableName();
    this.set('tempTableName',tempTableName);
    return this.get('uploader').createTable({
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "header": this.get("header"),
      "tableName": tempTableName,
      "databaseName": this.get('databaseName'),
      "fileType":"TEXTFILE"
    });
  },

  waitForCreateTempTable: function (jobId) {
    console.log("waitForCreateTempTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Waiting for creation of Temporary table.... </li>");
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onCreateTempTableSuccess : function(){
    console.log("onCreateTempTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Successfully created Temporary table. </li>");
  },

  deleteTable : function(databaseName, tableName){
    console.log("deleting table " + databaseName + "." + tableName);

    return this.get('uploader').deleteTable({
      "database":  databaseName,
      "table": tableName
    });
  },

  deleteTableOnError : function(databaseName,tableName, tableLabel){
      //delete table and wait for delete job
    var self = this;
    this.pushUploadProgressInfos("<li> Deleting " + tableLabel + " table...  </li>");

    return this.deleteTable(databaseName,tableName).then(function(data){
      return new Ember.RSVP.Promise(function(resolve,reject){
        self.waitForJobStatus(data.jobId,resolve,reject);
      });
    }).then(function(){
      self.popUploadProgressInfos();
      self.pushUploadProgressInfos("<li> Successfully deleted " + tableLabel + " table. </li>");
      return Ember.RSVP.Promise.resolve();
    },function(err){
      self.popUploadProgressInfos();
      self.pushUploadProgressInfos("<li> Failed to delete " + tableLabel + " table. </li>");
      self.setError(err);
      return Ember.RSVP.Promise.reject();
    });
  },

  rollBackActualTableCreation : function(){
    return this.deleteTableOnError(this.get("databaseName"),this.get("tableName"),"Actual");
  },


  onCreateTempTableFailure : function(error){
    console.log("onCreateTempTableFailure");
    this.setError(error);
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Failed to create temporary table. </li>");
    return this.rollBackActualTableCreation().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  uploadFile : function(){
    console.log("uploadFile");
    this.pushUploadProgressInfos("<li> Starting to upload the file .... </li>");
    if( this.get("isLocalUpload")){
      return this.uploadTable();
    }else{
      return this.uploadTableFromHdfs();
    }
  },

  waitForUploadingFile: function (data) {
    console.log("waitForUploadingFile");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Waiting for uploading file .... </li>");
    if( data.jobId ){
      var self = this;
          var p = new Ember.RSVP.Promise(function (resolve, reject) {
            self.waitForJobStatus(data.jobId, resolve, reject);
          });
      return p;
    }else{
      return  Ember.RSVP.Promise.resolve(data);
    }
  },

  onUploadingFileSuccess: function () {
    console.log("onUploadingFileSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Successfully uploaded file. </li>");
  },

  rollBackTempTableCreation : function(){
    var self = this;
    return this.deleteTableOnError(this.get("databaseName"),this.get("tempTableName"),"Temporary").then(function(data){
      return self.rollBackActualTableCreation();
    },function(err){
      return self.rollBackActualTableCreation();
    })
  },

  onUploadingFileFailure: function (error) {
    console.log("onUploadingFileFailure");
    this.setError(error);
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Failed to upload file. </li>");
    return this.rollBackTempTableCreation().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  rollBackUploadFile : function(){
    return this.rollBackTempTableCreation();
  },

  insertIntoTable : function(){
    console.log("insertIntoTable");
    this.pushUploadProgressInfos("<li> Starting to Insert rows from temporary table to actual table .... </li>");

    return this.get('uploader').insertIntoTable({
      "fromDatabase":  this.get("databaseName"),
      "fromTable": this.get("tempTableName"),
      "toDatabase": this.get("databaseName"),
      "toTable": this.get("tableName")
    });
  },

  waitForInsertIntoTable: function (jobId) {
    console.log("waitForInsertIntoTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Waiting for Insertion of rows from temporary table to actual table .... </li>");
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onInsertIntoTableSuccess : function(){
    console.log("onInsertIntoTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Successfully inserted rows from temporary table to actual table. </li>");
  },

  onInsertIntoTableFailure : function(error){
    console.log("onInsertIntoTableFailure");
    this.setError(error);
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Failed to insert rows from temporary table to actual table. </li>");
    return this.rollBackUploadFile().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  deleteTempTable : function(){
    console.log("deleteTempTable");
    this.pushUploadProgressInfos("<li> Starting to delete temporary table .... </li>");

    return this.deleteTable(
      this.get("databaseName"),
      this.get("tempTableName")
    );
  },

  waitForDeleteTempTable: function (jobId) {
    console.log("waitForDeleteTempTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li> Waiting for deletion of temporary table .... </li>");
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onDeleteTempTableSuccess : function(){
    console.log("onDeleteTempTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos("<li>Successfully inserted row. </li>");
    this.onUploadSuccessfull();
  },

  onDeleteTempTableFailure : function(error){
    console.log("onDeleteTempTableFailure");
    this.setError(error);
    this.setError("You will have to manually delete the table " + this.get("databaseName") + "." + this.get("tempTableName"));
  },

  createTableAndUploadFile : function(){
    var self = this;
    self.setError();
    self.showUploadModal();
    self.createActualTable()
      .then(function(data){
        console.log("1. received data : ", data);
        return self.waitForCreateActualTable(data.jobId);
      },function(error){
        self.onCreateActualTableFailure(error);
        console.log("Error occurred: ", error);
        throw error;
      })
      .then(function(data){
        console.log("2. received data : ", data);
        self.onCreateActualTableSuccess(data);
        return self.createTempTable(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onCreateActualTableFailure(new Error("Server job for creation of actual table failed."));
        }
        throw error;
      })
      .then(function(data){
        console.log("3. received data : ", data);
        return self.waitForCreateTempTable(data.jobId);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onCreateTempTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        console.log("4. received data : ", data);
        self.onCreateTempTableSuccess(data);
        return self.uploadFile(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onCreateTempTableFailure(new Error("Server job for creation of temporary table failed."));
        }
        throw error;
      }).then(function(data){
        console.log("4.5 received data : ", data);
        return self.waitForUploadingFile(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onUploadingFileFailure(error);
        }
        throw error;
      })
      .then(function(data){
        console.log("5. received data : ", data);
        self.onUploadingFileSuccess(data);
        return self.insertIntoTable(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onUploadingFileFailure(new Error("Server job for upload of file failed."));
        }
        throw error;
      })
      .then(function(data){
        console.log("6. received data : ", data);
        return self.waitForInsertIntoTable(data.jobId);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onInsertIntoTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        console.log("7. received data : ", data);
        self.onInsertIntoTableSuccess(data);
        return self.deleteTempTable(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onInsertIntoTableFailure(new Error("Server job for insert from temporary to actual table failed."));
        }
        throw error;
      })
      .then(function(data){
        console.log("8. received data : ", data);
        return self.waitForDeleteTempTable(data.jobId);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onDeleteTempTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        console.log("9. received data : ", data);
        self.onDeleteTempTableSuccess(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onDeleteTempTableFailure(new Error("Server job for deleting temporary table failed."));
        }
        throw error;
      }).catch(function(error){
        console.log("inside catch : ", error);
      }).finally(function(){
        console.log("finally hide the modal always");
        self.hideUploadModal();
      });
  },

  validateColumns: function () {
    // throw exception if invalid.
  },
  setError: function (error) {
    if(error){
      console.log("upload table error : ", error);
      this.set('error', JSON.stringify(error));
      this.get('notifyService').error(error);
    }else{
      this.set("error");
    }
  },

  previewError: function (error) {
    this.setError(error);
  },

  uploadTableFromHdfs : function(){
    console.log("uploadTableFromHdfs called.");
    if(!(this.get("inputFileTypeCSV") == true && this.get("isFirstRowHeader") == false) ){
      this.pushUploadProgressInfos("<li>Uploading file .... </li>");
    }
    return  this.get('uploader').uploadFromHDFS({
        "isFirstRowHeader": this.get("isFirstRowHeader"),
        "databaseName" :  this.get('databaseName'),
        "tableName" : this.get("tempTableName"),
        "inputFileType" : this.get("inputFileType").id,
        "hdfsPath" : this.get("hdfsPath")
      });
  },
  uploadTable: function () {
    this.printValues();
    return this.get('uploader').uploadFiles('upload', this.get('files'), {
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "databaseName" :  this.get('databaseName'),
      "tableName" : this.get("tempTableName"),
      "inputFileType" : this.get("inputFileType").id
    });
  },

  onUploadSuccessfull: function (data) {
    console.log("onUploadSuccessfull : ", data);
    this.get('notifyService').success("Uploaded Successfully", "Table " + this.get('tableName') + " created in database " + this.get("databaseName"));
    this.clearFields();
  },

  onUploadError: function (error) {
    console.log("onUploadError : ", error);
    this.setError(error);
  },
  showOrHide: function () {
    if (this.get('show') == false) {
      this.set("displayOption", "display:none");
      this.set("showMoreOrLess", "Show More");
    } else {
      this.set("displayOption", "display:table-row");
      this.set("showMoreOrLess", "Show Less");
    }
  },
  displayOption: "display:none",
  actions: {
    toggleErrors: function () {
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
      return this.generatePreview(files)
    },
    previewFromHdfs : function(){
      return this.generatePreview();
    },
    uploadTable : function(){
      try{
        this.createTableAndUploadFile();
      }catch(e){
        console.log("exception occured : ", e);
        this.setError(e);
        this.hideUploadModal();
      }
    },
    uploadFromHDFS : function(){
      this.set("isLocalUpload",false);
    }
  }
});
