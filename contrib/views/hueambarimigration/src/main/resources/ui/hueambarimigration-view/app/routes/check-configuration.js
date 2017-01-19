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


export default Ember.Route.extend({

  actions: {
    toggleStackTrace: function() {
      var value = this.controller.get('isExpanded');
      this.controller.set('isExpanded', !value);
    }
  },

  isExpanded: false,

  setupController: function() {
    var progresBar = 0;
    var passtonext = 0;
    var self = this;
    var control = this.controller;
    this.controller.set('progresBar', progresBar);
    var huehttpurl = this.store.queryRecord('huehttpurl', {});
    var huewebhdfsurl = this.store.queryRecord('huewebhdfsurl', {});
    var ambariwebhdfs = this.store.queryRecord('ambariwebhdfsurl', {});
    var huedatabases = this.store.queryRecord('huedatabase', {});
    var ambaridatabases = this.store.queryRecord('ambaridatabase', {});

    huehttpurl.then(function() {
        passtonext = passtonext + 20;
        progresBar = progresBar + 20;
        control.set('progresBar', progresBar);
        control.set('huehttpurlTestDone', "set");

        if (huehttpurl.get('configStatus') === "Success") {
          control.set('huehttpurlTest', "Success");
          control.set('huehttpurlTestresult', "Hue Http URl test Successful");
        }

        if (passtonext === 100) {
          Ember.run.later(this, function() {
            self.transitionTo('homePage');
          }, 4000);
        }
      },
      function(error) {
        progresBar = progresBar + 20;
        var checkFailedMessage;
        control.set('progresBar', progresBar);
        control.set('huehttpurlTestresult', "Hue Http URl test Failed");
        if (error.status !== 200) {
          checkFailedMessage = "Hue Http URL test Failed";
          var errors;
          errors = checkFailedMessage;
          errors += (error.message) ? (': <i>' + error.message + '</i><br>') : '<br>';

          control.set("errors", errors);
        }
        if (error.trace != null) {
          var stackTrace;
          stackTrace = checkFailedMessage + ':\n' + error.trace;
          control.set("stackTrace", stackTrace);
        }
      });
    huewebhdfsurl.then(function() {
        passtonext = passtonext + 20;
        progresBar = progresBar + 20;
        control.set('huewebhdfsurlTestDone', progresBar);

        if (huewebhdfsurl.get('configStatus') === "Success") {
          control.set('huewebhdfsurlTest', "Success");
          control.set('huewebhdfsurlTestresult', "Hue Webhdfs url test Successful");
        }
        if (passtonext === 100) {
          Ember.run.later(this, function() {
            self.transitionTo('homePage');
          }, 4000);
        }
      },
      function(error) {
        progresBar = progresBar + 20;
        var checkFailedMessage;
        control.set('progresBar', progresBar);
        if (error.status !== 200) {
          checkFailedMessage = "Hue Web HDFS URL test Failed";
          var errors;
          errors = checkFailedMessage;
          errors += (error.message) ? (': <i>' + error.message + '</i><br>') : '<br>';
          control.set("errors", errors);
        }
        if (error.trace != null) {
          var stackTrace;
          stackTrace = checkFailedMessage + ':\n' + error.trace;
          control.set("stackTrace", stackTrace);
        }
      });
    ambariwebhdfs.then(function() {
        progresBar = progresBar + 20;
        passtonext = passtonext + 20;
        control.set('progresBar', progresBar);
        control.set('ambariwebhdfsTestDone', progresBar);

        if (ambariwebhdfs.get('configStatus') === "Success") {
          control.set('ambariwebhdfsTest', "Success");
          control.set('ambariwebhdfsTestresult', "Ambari Webhdfs url test Successful");
        }
        if (passtonext === 100) {
          Ember.run.later(this, function() {
            self.transitionTo('homePage');
          }, 4000);
        }
      },
      function(error) {
        progresBar = progresBar + 20;
        var checkFailedMessage;
        control.set('progresBar', progresBar);
        if (error.status !== 200) {
          checkFailedMessage = "Ambari Web HDFS URL test Failed";
          var errors;
          errors = checkFailedMessage;
          errors += (error.message) ? (': <i>' + error.message + '</i><br>') : '<br>';
          control.set("errors", errors);
        }
        if (error.trace != null) {
          var stackTrace;
          stackTrace = checkFailedMessage + ':\n' + error.trace;
          control.set("stackTrace", stackTrace);
        }
      });
    huedatabases.then(function() {
        passtonext = passtonext + 20;
        progresBar = progresBar + 20;
        control.set('progresBar', progresBar);
        control.set('huedatabasesTestDone', progresBar);

        if (huedatabases.get('configStatus') === "Success") {
          control.set('huedatabasesTest', "Success");
          control.set('huedatabasesTestresult', "Hue database Connection test Successful");
        }
        if (passtonext === 100) {
          Ember.run.later(this, function() {
            self.transitionTo('homePage');
          }, 4000);
        }
      },
      function(error) {
        progresBar = progresBar + 20;
        var checkFailedMessage;
        control.set('progresBar', progresBar);
        control.set('huedatabasesTestresult', "Hue database Connection test Failed");
        if (error.status !== 200) {
          checkFailedMessage = "Service Hue Database check failed";
          var errors;
          errors = checkFailedMessage;
          errors += (error.message) ? (': <i>' + error.message + '</i><br>') : '<br>';
          control.set("errors", errors);
        }
        if (error.trace !== null) {
          var stackTrace;
          stackTrace = checkFailedMessage + ':\n' + error.trace;
          control.set("stackTrace", stackTrace);
        }
      });
    ambaridatabases.then(function() {
        passtonext = passtonext + 20;
        progresBar = progresBar + 20;
        control.set('progresBar', progresBar);
        control.set('ambaridatabasesTestDone', progresBar);
        if (ambaridatabases.get('configStatus') === "Success") {
          control.set('ambaridatabasesTest', "Success");
          control.set('ambaridatabasesTestresult', "Ambari database Connection test Successful");
        }
        if (passtonext === 100) {
          Ember.run.later(this, function() {
            self.transitionTo('homePage');
          }, 4000);
        }
      },
      function(error) {
        progresBar = progresBar + 20;
        var checkFailedMessage;
        control.set('progresBar', progresBar);
        if (error.status !== 200) {
          checkFailedMessage = "Ambari Database Connection Failed";
          var errors;
          errors = checkFailedMessage;
          errors += (error.message) ? (': <i>' + error.message + '</i><br>') : '<br>';
          control.set("errors", errors);
        }

        if (error.trace !== null) {
          var stackTrace;
          stackTrace = checkFailedMessage + ':\n' + error.trace;
          control.set("stackTrace", stackTrace);
        }
      });
  }
});
