/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import Ember from 'ember';

export default Ember.Route.extend({
    afterModel(){
      let ooziePromise = this.checkOozie();
      let hdfsPromise = this.checkHdfs();
      let homeDirPromise = this.checkUserHome();
      let serviceChecks = this.controllerFor('index').get('serviceChecks');
      this.controllerFor('index').get('issues').clear();
      this.processServiceCheckPromise(ooziePromise, serviceChecks.findBy('name', 'oozie'));
      this.processServiceCheckPromise(hdfsPromise, serviceChecks.findBy('name', 'hdfs'));
      this.processServiceCheckPromise(homeDirPromise, serviceChecks.findBy('name', 'homeDir'));
      Ember.RSVP.Promise.all([ooziePromise, hdfsPromise, homeDirPromise]).then(()=>{
        this.controllerFor('index').set('serviceChecksComplete', true);
        Ember.run.later(()=>{
          this.transitionTo('design');
      }, 2000);
      }).catch((errors)=>{
        this.controllerFor('index').set('serviceChecksComplete', true);
        this.controllerFor('index').set('errors', errors);
      });
    },
    processServiceCheckPromise(promise, serviceCheck){
      promise.then(()=>{
        Ember.set(serviceCheck, 'isAvailable', true);
      }).catch((e)=>{
        console.error(e);
        Ember.set(serviceCheck, 'isAvailable', false);
      }).finally(()=>{
        Ember.set(serviceCheck, 'checkCompleted', true);
      });
    },
    checkOozie(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/v1/admin/configuration";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    checkHdfs(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/hdfsCheck";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    },
    checkUserHome(){
      return new Ember.RSVP.Promise((resolve, reject) => {
        var url = Ember.ENV.API_URL + "/homeDirCheck";
          Ember.$.ajax({
          url: url,
          method: "GET",
          dataType: "text",
          contentType: "text/plain;charset=utf-8",
          beforeSend: function(request) {
            request.setRequestHeader("X-Requested-By", "workflow-designer");
          },
          success : function(response){
            resolve(true);
          },
          error : function(response){
            reject(response);
          }
        });
      });
    }
});
