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
  isExpanded: false,
  errors: "",
  stackTrace: "",
  startTests: function() {

    var model = this.get('model');
    var url = this.container.lookup('adapter:application').buildURL() + '/resources/hive/'
    var self = this;

    var processResponse = function(name, data) {

      if(data.databases){
        data = Ember.Object.create( {trace: null, message: "OK", status: "200"});
      } else {
        data = data;
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
      model.set('percent', percent + 33.33);
    };

    var promises = ['hdfs', 'hiveserver', 'ats'].map(function(name) {

      var finalurl = ((name == 'hiveserver') ? self.get('databaseService.baseUrl') : (url + name + 'Status')) || '' ;

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

  modelhdfsTestDone: function() {
    return this.get('model.hdfsTestDone');
  }.property('model.hdfsTestDone' ),

  modelhiveserverTestDone: function() {
    return this.get('model.hiveserverTestDone');
  }.property('model.hiveserverTestDone' ),

  modelatsTestDone: function() {
    return this.get('model.atsTestDone');
  }.property('model.atsTestDone' ),

  modelhdfsTest: function() {
    return this.get('model.hdfsTest');
  }.property('model.hdfsTest' ),

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


