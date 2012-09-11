/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

App.InstallerStep2View = Em.View.extend({

  templateName: require('templates/installer/step2'),

  doManualInstall: function (router, event) {
    if (typeof jQuery != 'undefined') {

      console.log('jQuery library is loaded!');

    }
    console.log('value is: ' + $('#hostConnectId h2').text());
    console.log('over here');
    //alert('value is:' +  $('hostConnectId').('connect-opt').value);
  }

});

App.Step2_parentView = Em.View.extend({
  isVisible: true,
  click: function () {
    console.log('parent of step2');
  }
});

App.Step2_parent_TargetHostView = Em.View.extend({
  ///

  ///
  isVisible: true,
  click: function () {
    console.log('target hosts: child of step2');
  }
});

App.Step2_child_HostManageView = Em.View.extend({
  isVisible: true,
  click: function () {
    console.log('host management: child of step2');
  }

});


App.Step2_child_AdvOpt = Em.View.extend({
  isVisible: true,
  click: function () {
    console.log('Soft Repo: parent of step2');
  }
});
