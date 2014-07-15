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

App.StackUpgradeController = App.WizardController.extend({
  name: 'stackUpgradeController',

  totalSteps: 3,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * upgradeOptions - upgrade options
   */
  content: Em.Object.create({
    cluster: null,
    upgradeOptions: null,
    servicesInfo: function(){
      return App.router.get('mainAdminClusterController.services');
    }.property('App.router.mainAdminClusterController.services'),
    upgradeVersion: function(){
      return App.router.get('mainAdminClusterController.upgradeVersion');
    }.property('App.router.mainAdminClusterController.upgradeVersion'),
    controllerName: 'stackUpgradeController'
  }),

  /**
   * return new object extended from upgradeOptionsTemplate
   * @return Object
   */
  getUpgradeOptions: function(){
    return jQuery.extend({}, this.get('upgradeOptionsTemplate'));
  },
  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    this.load('cluster');
    this.load('upgradeOptions');
  },
  upgradeOptionsTemplate:{
    localRepo: false
  },
  clear: function () {
    this.set('content', Ember.Object.create({
      servicesInfo: function(){
        return App.router.get('mainAdminClusterController.services');
      }.property('App.router.mainAdminClusterController.services'),
      upgradeVersion: function(){
        return App.router.get('mainAdminClusterController.upgradeVersion');
      }.property('App.router.mainAdminClusterController.upgradeVersion'),
      'controllerName': 'stackUpgradeController'
    }));
    this.set('currentStep', 0);
    this.clearStorageData();
  },
  clearStorageData: function(){
    App.db.setCluster(undefined);
    App.db.setUpgradeOptions(undefined);
  },
  /**
   * Finish upgrade
   */
  finish: function () {
    this.clear();
    this.setCurrentStep('1');
  }
});
