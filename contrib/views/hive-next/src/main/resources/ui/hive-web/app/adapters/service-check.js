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
import application from './application';

export default application.extend({

  buildServiceURL: function (path) {
    return this.buildURL() + "/resources/hive/" + path;
  },

  fetchServiceCheckPolicy: function(){
    return this.doGet("service-check-policy");
  },

  doGet : function(path,inputData){
    var self = this;
    return new Ember.RSVP.Promise(function(resolve,reject){
      Ember.$.ajax({
        url :  self.buildServiceURL(path),
        type : 'get',
        headers: self.get('headers'),
        dataType : 'json'
      }).done(function(data) {
        resolve(data);
      }).fail(function(error) {
        reject(error);
      });
    });
  },
});
