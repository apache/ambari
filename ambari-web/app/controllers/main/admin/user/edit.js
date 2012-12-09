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

var App = require('app');

App.MainAdminUserEditController = Em.Controller.extend({
  name:'mainAdminUserEditController',
  sendCommandToServer : function(url, postData, callback){
    var url =  (App.testMode) ?
        '/data/wizard/deploy/poll_1.json' : //content is the same as ours
        '/api/clusters/' + App.router.getClusterName() + url;

    var method = App.testMode ? 'GET' : 'PUT';

    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(postData),
      dataType: 'json',
      timeout: 5000,
      success: function(data){
        if(data && data.Requests){
          callback(data.Requests.id);
        } else{
          callback(null);
          console.log('cannot get request id from ', data);
        }
      },

      error: function (request, ajaxOptions, error) {
        //do something
        callback(null);
        console.log('error on change component host status')
      },

      statusCode: require('data/statusCodes')
    });
  },
  content:false
})