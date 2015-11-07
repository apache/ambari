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

import EmberUploader from 'ember-uploader';
import Ember from 'ember';
import application from './application';
import FileUploader from './file-upload';

export default application.extend({
  hdrs : function(){
    console.log("inside hdrs : headers : ",this.get('headers'));
    var h = Ember.$.extend(true, {},this.get('headers'));
    delete h['Content-Type'];
    return h;
  }.property('headers'),

  buildUploadURL: function (path) {
    return this.buildURL() + "/resources/upload/" + path;
  },

  uploadFiles: function (path, files, extras) {
    var uploadUrl = this.buildUploadURL(path);

    console.log("uplaoder : uploadURL : ", uploadUrl);
    console.log("uploader : extras : ", extras);
    console.log("uploader : files : ", files);

    console.log("hdrs : ", this.get('hdrs'));
    var uploader = FileUploader.create({
      headers: this.get('hdrs'),
      url: uploadUrl
    });

    if (!Ember.isEmpty(files)) {
      var promise = uploader.upload(files[0], extras);
      return promise;
    }
  },

  createTable: function (tableData) {
    var _this = this;
    var postHeader = JSON.parse(JSON.stringify(this.get('headers')));
    console.log("headers postHeadesfsfdfsfsfss : : " , postHeader);
    return Ember.$.ajax(      {
        url :  this.buildUploadURL("createTable"),
        type : 'post',
        data: JSON.stringify(tableData),
        headers: this.get('headers'),
        dataType : 'json'
      }
    );
  },

  getCreateTableResult : function(jobId){
    return Ember.$.ajax(this.buildUploadURL("createTable/status"),{
      data : {"jobId":jobId},
      type: "get",
      headers: this.get('headers')
    });
  }
});
