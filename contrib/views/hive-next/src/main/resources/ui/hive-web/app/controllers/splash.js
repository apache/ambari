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

export default Ember.Controller.extend({

  databaseService: Ember.inject.service(constants.namingConventions.database),
  ldapService: Ember.inject.service(constants.namingConventions.ldap),
  isExpanded: false,
  errors: "",
  stackTrace: "",
  requestLdapPassword:function(callback) {
    var ldap = this.get('ldapService');
    ldap.requestLdapPassword(this,callback);
  },

checkConnection: function() {
    var model = this.get('model');
    var url = this.container.lookup('adapter:application').buildURL() + '/resources/connection/';
    var finalurl =  url + 'connect' ;
    var self = this;

    return Ember.$.getJSON( finalurl )
        .then(
            function(data) {
              console.log("fulfil");
              model.set('ldapFailure',false);
            },
            function(reason) {
              console.log("fail");
              if(reason.status === 401){
                model.set('ldapFailure',true);
              } else {

                  var data = reason.responseJSON;
                  var checkFailedMessage = "Service Hive check failed";
                  var errors = self.get("errors");
                  errors += checkFailedMessage;
                  errors += (data.message) ? (': <i>' + data.message + '</i><br>') : '<br>';
                  self.set("errors", errors);

                if (data.trace != null) {
                  var stackTrace = self.get("stackTrace");
                  stackTrace += checkFailedMessage + ':\n' + data.trace;
                  self.set("stackTrace", stackTrace);
                }
              }
            }
        );

  },
  startTests: function() {

    var model = this.get('model');
    var url = this.container.lookup('adapter:application').buildURL() + '/resources/hive/';
    var self = this;

    var processResponse = function(name, data) {

      if( data != undefined ){
        if(data.databases){
          data = Ember.Object.create( {trace: null, message: "OK", status: "200"});
        } else {
          data = data;
        }
      } else {
        data = Ember.Object.create( {trace: null, message: "Server Error", status: "500"});
      }

      model.set(name + 'Test', data.status == 200);

      if (data.status != 200) {
        var checkFailedMessage = "Service '" + name + "' check failed";
        var errors = self.get("errors");
        errors += checkFailedMessage;
        errors += (data.message)?(': <i>' + data.message + '</i><br>'):'<br>';
        self.set("errors", errors);
      }

      if (data.trace != null) {
        var stackTrace = self.get("stackTrace");
        stackTrace += checkFailedMessage + ':\n' + data.trace;
        self.set("stackTrace", stackTrace);
      }

      model.set(name + 'TestDone', true);
      var percent = model.get('percent');
      model.set('percent', percent + (100/model.get("numberOfChecks")));
    };


    var checks = [];
    if(model.get("serviceCheckPolicy").checkHdfs){
      checks.push("hdfs");
    }else{
      model.set("hdfs" + 'TestDone', true);
      model.set("hdfs" + 'Test', true);
    }
    if(model.get("serviceCheckPolicy").checkATS){
      checks.push("ats");
    }else{
      model.set("ats" + 'TestDone', true);
      model.set("ats" + 'Test', true);
    }
    if(model.get("serviceCheckPolicy").checkHomeDirectory){
      checks.push("userhome");
    }else{
      model.set("userhome" + 'TestDone', true);
      model.set("userhome" + 'Test', true);
    }

    var promises = checks.map(function(name) {

      var finalurl =  url + name + 'Status' ;

      return Ember.$.getJSON( finalurl )
        .then(
          function(data) {
            processResponse(name, data);
          },
          function(reason) {
              processResponse(name, reason.responseJSON);
          }
        );
    });

    return Ember.RSVP.all(promises);
  },

  progressBarStyle: function() {
    return 'width: ' + this.get("model").get("percent") +  '%;';
  }.property("model.percent"),

  allTestsCompleted: function(){
    return this.get('modelhdfsTestDone') && this.get('modelhiveserverTestDone') && this.get('modelatsTestDone') && this.get('modeluserhomeTestDone');
  }.property('modelhdfsTestDone', 'modelhiveserverTestDone', 'modelatsTestDone', 'modeluserhomeTestDone'),

  modelhdfsTestDone: function() {
    return this.get('model.hdfsTestDone');
  }.property('model.hdfsTestDone' ),

  modeluserhomeTestDone: function() {
    return this.get('model.userhomeTestDone');
  }.property('model.userhomeTestDone' ),

  modelhiveserverTestDone: function() {
    return this.get('model.hiveserverTestDone');
  }.property('model.hiveserverTestDone' ),

  modelatsTestDone: function() {
    return this.get('model.atsTestDone');
  }.property('model.atsTestDone' ),

  modelhdfsTest: function() {
    return this.get('model.hdfsTest');
  }.property('model.hdfsTest' ),

  modeluserhomeTest: function() {
    return this.get('model.userhomeTest');
  }.property('model.userhomeTest' ),

  modelhiveserverTest: function() {
    return this.get('model.hiveserverTest');
  }.property('model.hiveserverTest' ),

  modelatsTest: function() {
    return this.get('model.atsTest');
  }.property('model.atsTest' ),

  actions: {
    toggleStackTrace:function () {
      var value = this.get('isExpanded');
      this.set('isExpanded', !value);
    }
  }
});
