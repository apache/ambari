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

  isStepDisabled: [],

  totalSteps: 10,

  init: function () {
    this.clusters = App.Cluster.find();
    this.isStepDisabled.pushObject(Ember.Object.create({
      step: 1,
      value: false
    }));
    for (var i = 2; i <= this.totalSteps; i++) {
      this.isStepDisabled.pushObject(Ember.Object.create({
        step: i,
        value: true
      }));
    }
  },

  setStepsEnable: function () {
    for (var i = 2; i <= this.totalSteps; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep')) {
        step.set('value', false);
      } else {
        step.set('value', true);
      }
    }
  }.observes('currentStep'),

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

  isStep1: function () {
    return this.get('currentStep') == 1;
  }.property('currentStep'),

  isStep2: function () {
    return this.get('currentStep') == 2;
  }.property('currentStep'),

  isStep3: function () {
    return this.get('currentStep') == 3;
  }.property('currentStep'),

  isStep4: function () {
    return this.get('currentStep') == 4;
  }.property('currentStep'),

  isStep5: function () {
    return this.get('currentStep') == 5;
  }.property('currentStep'),

  isStep6: function () {
    return this.get('currentStep') == 6;
  }.property('currentStep'),

  isStep7: function () {
    return this.get('currentStep') == 7;
  }.property('currentStep'),

  isStep8: function () {
    return this.get('currentStep') == 8;
  }.property('currentStep'),

  isStep9: function () {
    return this.get('currentStep') == 9;
  }.property('currentStep'),

  isStep10: function () {
    return this.get('currentStep') == 10;
  }.property('currentStep'),

  gotoStep1: function () {
    if (this.get('isStepDisabled').findProperty('step', 1).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep1');
    }

  },

  gotoStep2: function () {
    if (this.get('isStepDisabled').findProperty('step', 2).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep2');
    }

  },

  gotoStep3: function () {
    if (this.get('isStepDisabled').findProperty('step', 3).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep3');
    }

  },

  gotoStep4: function () {

    if (this.get('isStepDisabled').findProperty('step', 4).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep4');
    }
  },

  gotoStep5: function () {
    if (this.get('isStepDisabled').findProperty('step', 5).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep5');
    }
  },

  gotoStep6: function () {
    if (this.get('isStepDisabled').findProperty('step', 6).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep6');
    }

  },

  gotoStep7: function () {
    if (this.get('isStepDisabled').findProperty('step', 7).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep7');
    }
  },

  gotoStep8: function () {
    if (this.get('isStepDisabled').findProperty('step', 8).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep8');
    }
  },

  gotoStep9: function () {
    if (this.get('isStepDisabled').findProperty('step', 9).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep9');
    }
  },

  gotoStep10: function () {
    if (this.get('isStepDisabled').findProperty('step', 10).get('value') === true) {
      return;
    } else {
      App.router.send('gotoStep10');
    }
  },

  /**
   *
   * @param cluster ClusterModel
   */
  createCluster: function (cluster) {
    alert('created cluster ' + cluster.name);
  }

});