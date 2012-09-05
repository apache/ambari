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



module.exports = Em.Route.extend({

  route: '/main',

  enter: function (router) {

    console.log('in /main:enter');

    if (router.getAuthenticated()) {
      // TODO: redirect to last known state
      /*
      Ember.run.next(function () {
        router.transitionTo('step' + router.getInstallerCurrentStep());
      });
      */
    } else {
      Ember.run.next(function () {
        router.transitionTo('login');
      });
    }
  },

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('main');
  }

  // TODO: create new routes here
  // dashboard
  // charts
  // hosts
  // hosts/:hostname
  // admin
  // etc...


});