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
require('utils/db');

App.InstallerStep1Controller = Em.Controller.extend({
  name: 'installerStep1Controller',
  content: [],
  clusterName: '',
  clusterNameError: '',
  invalidClusterName: true,

  /**
   * Returns true if the cluster name is valid and stores it in localStorage.
   * Returns false otherwise, and sets appropriate field error message.
   */

  clearStep: function() {
    this.set('clusterName','');
  },

  loadStep: function () {
    var clusterName;
    console.log('The value of the cluster name is: ' + App.db.getClusterName());
    if (App.db.getClusterName() !== undefined && App.db.getClusterName() !== true ) {
      this.set('clusterName', App.db.getClusterName());
    } else {
      this.set('clusterNameError','');
      this.set('invalidClusterName',true);
    }
  },

  validateStep1: function () {
    console.log('TRACE: Entering controller:InstallerStep1:validateStep1 function');
    if (this.get('clusterName') == '') {
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.required'));
      this.set('invalidClusterName', true);
      return false;
    } else if (/\s/.test(this.get('clusterName'))) {
      console.log('White spaces not allowed for cluster name');
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.whitespaces'));
      this.set('invalidClusterName', true);
      return false;
    } else if (/[^\w\s]/gi.test(this.get('clusterName'))) {
      console.log('Special characters are not allowed for the cluster name');
      this.set('clusterNameError', Em.I18n.t('installer.step1.clusterName.error.specialChar'));
      this.set('invalidClusterName', true);
      return false;
    } else {
      console.log('value of clusterName is: ' + this.get('clusterName'));
      this.set('clusterNameError', '');
      this.set('invalidClusterName', false);
      return true;
    }
  }.observes('clusterName'),

  submit: function () {
    this.validateStep1();
    if (this.get('clusterNameError') === '') {
      App.db.setClusterName(this.get('clusterName'));
      App.router.send('next');
    }
  }

})