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

App.InstallerStep1Controller = Em.Controller.extend({
  name: 'installerStep1Controller',
  content: [],
  clusterName: '',
  invalidClusterName: false,
  clusterNameError: '',
  validateStep1: function () {
    //TODO: Done
    //task1 =  checks on valid cluster name
    //task2 (prereq(task1 says it's a valid cluster name)) =  storing cluster name in localstorage
    var result;
    console.log('TRACE: Entering controller:InstallerStep1:evaluateStep1 function');
    if (this.get('clusterName') == '') {
      this.set('clusterNameError', App.messages.step1_clusterName_error_required);
      this.set('invalidClusterName', true);
      result = false;
    } else if (/\s/.test(this.get('clusterName'))) {
      console.log('White spaces not allowed for cluster name');
      this.set('clusterNameError', App.messages.step1_clusterName_error_whitespaces);
      this.set('invalidClusterName', true);
      result = false;
    } else if (/[^\w\s]/gi.test(this.get('clusterName'))) {
      console.log('Special characters are not allowed for the cluster name');
      this.set('clusterNameError', App.messages.step1_clusterName_error_specialChar);
      this.set('invalidClusterName', true);
      result = false;
    } else {
      console.log('value of clusterNmae is: ' + this.get('clusterName'));
      this.set('clusterNameError', '');
      this.set('invalidClusterName', false);
      result = true;
    }
    if (result === true) {
      App.db.setClusterName(this.get('clusterName'));
    }
    console.log('Exiting the evaluatestep1 function');
    return result;
  }.observes('clusterName')

})