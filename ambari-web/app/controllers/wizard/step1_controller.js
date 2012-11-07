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

App.WizardStep1Controller = Em.Controller.extend({
  name: 'wizardStep1Controller',

  clusterName: function(){
    return this.get('content.cluster.name');
  }.property('content.cluster.name'),


  hasSubmitted : false,

  clearStep: function() {
     this.set('content.cluster.name','');
  },

  loadStep: function () {
    var clusterName;
    this.set('hasSubmitted',false);
    console.log('The value of the cluster name is: ' + App.db.getClusterName());
    if (App.db.getClusterName() !== undefined) {
      this.set('clusterName', App.db.getClusterName());
    } else {
      this.set('clusterNameError','');
      this.set('invalidClusterName',true);
    }
  },

  invalidClusterName : function(){
    if(!this.get('hasSubmitted')){
      return false;
    }

    var clusterName = this.get('content.cluster.name');
    if (clusterName == '') {
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.required'));
      return true;
    } else if (/\s/.test(clusterName)) {
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.whitespaces'));
      return true;
    } else if (/[^\w\s]/gi.test(clusterName)) {
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.specialChar'));
      return true;
    } else {
      this.set('clusterNameError', '');
      return false;
    }
  }.property('hasSubmitted', 'content.cluster.name').cacheable(),

  /**
   * calculates by <code>invalidClusterName</code> property
   */
  clusterNameError: '',

  /**
   * Onclick handler for <code>next</code> button
   */
  submit: function () {
    this.set('hasSubmitted', true);
    if (!this.get('invalidClusterName')) {
      this.set('content.cluster',{name: this.get('clusterName'), status: 'PENDING', isCompleted: false});
     // App.router.get('installerController').saveClusterStatus({status: 'PENDING', isCompleted: false});
      App.router.send('next');
    }
  }

});
