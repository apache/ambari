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

App.InstallerController = Em.Controller.extend({

  name: 'installerController',

  evaluateStep4: function () {
    // TODO: evaluation at the end of step4

  },
  evaluateStep5: function () {
    // TODO: evaluation at the end of step5

  },
  evaluateStep6: function () {
    // TODO: evaluation at the end of step6

  },
  evaluateStep7: function () {
    // TODO: evaluation at the end of step7

  },
  evaluateStep8: function () {
    // TODO: evaluation at the end of step8

  },

  prevInstallStatus: function () {
    console.log('Inside the prevInstallStep function: The name is ' + App.router.get('loginController.loginName'));
    var result = App.db.isCompleted()
    if (result == '1') {
      return true;
    }
  }.property('App.router.loginController.loginName'),

  currentStep: function () {
    return App.get('router').getInstallerCurrentStep();
  }.property(),

  clusters: null,

  init: function () {
    this.clusters = App.Cluster.find();
  },

  isStep1: function () {
    return this.get('currentStep') == '1';
  }.property('currentStep'),

  isStep2: function () {
    return this.get('currentStep') == '2';
  }.property('currentStep'),

  isStep3: function () {
    return this.get('currentStep') == '3';
  }.property('currentStep'),

  isStep4: function () {
    return this.get('currentStep') == '4';
  }.property('currentStep'),

  isStep5: function () {
    return this.get('currentStep') == '5';
  }.property('currentStep'),

  isStep6: function () {
    return this.get('currentStep') == '6';
  }.property('currentStep'),

  isStep7: function () {
    return this.get('currentStep') == '7';
  }.property('currentStep'),

  isStep8: function () {
    return this.get('currentStep') == '8';
  }.property('currentStep'),

  isStep9: function () {
    return this.get('currentStep') == '9';
  }.property('currentStep'),

  isStep10: function () {
    return this.get('currentStep') == '10';
  }.property('currentStep'),

  /**
   *
   * @param cluster ClusterModel
   */
  createCluster: function (cluster) {
    alert('created cluster ' + cluster.name);
  }

});