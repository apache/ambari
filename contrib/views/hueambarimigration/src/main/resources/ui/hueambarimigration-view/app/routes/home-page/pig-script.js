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

  resetController: function(controller, isExiting, transition) {
    if (isExiting) {
      this.usernames = [];
      this.controller.set('username', null);
      this.controller.set('instancename', null);
      this.controller.set('startdate', null);
      this.controller.set('enddate', null);
      this.controller.set('jobstatus', null);
      this.controller.set('progressBar', null);
      this.controller.set('completionStatus', null);
      this.controller.set('error', null);
    }
  },

  usernames: [],

  model: function() {
    var store = this.store;
    return Ember.RSVP.hash({
      usersdetail: store.findAll('usersdetail'),
      piginstancedetail: store.findAll('piginstancedetail'),
      selections: []
    });

  },

  actions: {

    addSelection: function(value) {
      this.usernames.push(value);
    },

    removeSelection: function(value) {
      var index = this.usernames.indexOf(value);
      if(index > -1) {
        this.usernames.splice(index,1);
      }
    },

    submitResult: function() {
    if(this.usernames.length === 0 || this.controller.get('instancename') ===undefined){
      alert("Mandatory fields can not left blank");
    }
    else
    {
      this.controller.set('jobstatus', null);
      this.controller.set('progressBar', null);
      this.controller.set('completionStatus', null);
      var migration = this.store.queryRecord('returnjobid', {
        username: this.usernames.toString(),
        instance: this.controller.get('instancename'),
        startdate: this.controller.get('startdate'),
        enddate: this.controller.get('enddate'),
        jobtype: "pigsavedscriptmigration"
      });
      var control = this.controller;
      var store = this.store;
      var repeat = this;

      migration.then(function() {
        var jobid = migration.get('idforJob');
        var hivehistoryqueryjobstart = store.queryRecord('startmigration', {
          username: repeat.usernames.toString(),
          instance: control.get('instancename'),
          startdate: control.get('startdate'),
          enddate: control.get('enddate'),
          jobid: jobid,
          jobtype: "pigsavedscriptmigration"
        });
        hivehistoryqueryjobstart.then(function() {
          control.set('jobstatus', "0");
          repeat.progresscheck(jobid);
        });
      });
    }
    }
  },
  progresscheck: function(jobid) {
    var repeat = this;
    var control = this.controller;
    Ember.run.later(this, function() {
      var progress = this.store.queryRecord('checkprogress', {
        jobid: jobid
      });
      progress.then(function() {
        // control.set('jobstatus',null);
        var progressPercentage = progress.get('progressPercentage');
        var numberOfQueryTransfered = progress.get('numberOfQueryTransfered');
        var flagForCompletion = parseInt(progress.get('flag'));
        var error = progress.get('error');
        console.log("the progress percentage is="+progressPercentage);
        console.log("flag status is "+flagForCompletion);
        console.log("error is "+error);
        if(error) {
          control.set('error', error);
          control.set('jobstatus', null);

        } else if (flagForCompletion === 1) {
          var totalNoQuery = progress.get('totalNoQuery');
          var intanceName = progress.get('intanceName');
          var userNameofhue = progress.get('userNameofhue');
          var totalTimeTaken = progress.get('totalTimeTaken');
          control.set('jobstatus', null);
          control.set('completionStatus', progressPercentage);
          control.set('progressBar', progressPercentage);
          if (numberOfQueryTransfered === "0") {
            control.set('numberOfQueryTransfered', "No Queries selected according to your criteria");
          } else {
            control.set('numberOfQueryTransfered', numberOfQueryTransfered);
          }
          control.set('totalNoQuery', totalNoQuery);
          control.set('instanceName', intanceName);
          control.set('Username', userNameofhue);
          control.set('totalTimeTaken', totalTimeTaken);

        } else {
          control.set('progressBar', progressPercentage);
          repeat.progresscheck(jobid);
        }
      });
    }, 500);
  }
});
