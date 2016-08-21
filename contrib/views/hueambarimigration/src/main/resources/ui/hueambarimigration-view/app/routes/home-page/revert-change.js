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

  model: function() {
    var store = this.store;
    return Ember.RSVP.hash({
      allinstancedetail: store.findAll('allinstancedetail')
    });
  },

  actions: {
    submitResult: function() {

    if(this.controller.get('instancename') ===undefined){
      alert("Mandatory fields can not left blank");
    }
    else{
      this.controller.set('jobstatus', null);
      this.controller.set('progressBar', null);
      this.controller.set('completionStatus', null);
      var migration = this.store.queryRecord('returnjobidforrevertchange', {
        instance: this.controller.get('instancename'),
        revertdate: this.controller.get('revertdate')
      });
      var control = this.controller;
      var store = this.store;
      var repeat = this;
      migration.then(function() {
        var jobid = migration.get('idforJob');
        var hivehistoryqueryjobstart = store.queryRecord('startrevertchange', {
          instance: control.get('instancename'),
          revertdate: control.get('revertdate'),
          jobid: jobid
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

        var progressPercentage = progress.get('progressPercentage');
        var numberOfQueryTransfered = progress.get('numberOfQueryTransfered');
        var totalNoQuery = progress.get('totalNoQuery');
        var intanceName = progress.get('intanceName');
        var userNameofhue = progress.get('userNameofhue');
        var totalTimeTaken = progress.get('totalTimeTaken');
        //var jobtype=progress.get('jobtype');
        var isNoQuerySelected = progress.get('isNoQuerySelected');
        if (progressPercentage !== '100' && isNoQuerySelected === 'no') {
          control.set('progressBar', progressPercentage);
          repeat.progresscheck(jobid);
        }
        if (progressPercentage === '100' || isNoQuerySelected === 'yes') {
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
        }
      });
    }, 500);
  }

});
