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
  serviceCheck: Ember.inject.service(),
  beforeModel() {
    if (this.get('serviceCheck.checkCompleted')) {
      this.transitionTo('application');
    }
  },

  model(){
    let promise =  this.get("serviceCheck").fetchServiceCheckPolicy();
    promise.then((data) => {
      console.log("data : ", data);
      this.set("serviceCheckPolicy", data.serviceCheckPolicy)
    });

    return promise;
  },

  afterModel(){
    let controller = this.controllerFor("service-check");
    controller.reset();
    controller.set("serviceCheckPolicy", this.get("serviceCheckPolicy"));
    this.get('serviceCheck').check(this.get("serviceCheckPolicy")).then((data) => {
      if(data.userHomePromise.state === 'rejected') {
        controller.set('userHomeError', data.userHomePromise.reason.errors);
      }

      if(data.hdfsPromise.state === 'rejected') {
        controller.set('hdfsError', data.hdfsPromise.reason.errors);
      }

      if(data.atsPromise.state === 'rejected') {
        controller.set('atsError', data.atsPromise.reason.errors);
      }

      if(data.hivePromise.state === 'rejected') {
        controller.set('hiveError', data.hivePromise.reason.errors);
      }
    });

  }
});
