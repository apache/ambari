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

App.WizardStep3View = Em.View.extend({

  templateName: require('templates/wizard/step3'),

  didInsertElement: function () {
    var controller = this.get('controller');
    controller.set('content', controller.get('data')); // workaround, to make select workable
    controller.navigateStep();
  }
});

App.HostView = Em.View.extend({

  isVisible: true,
  category: 'Hosts',

  /**
   * Onclick handler for remove host button(trash icon)
   */
  removeItem: function () {
    var hostInfo = this.get('hostInfo');
    this.get('controller').removeHosts([hostInfo]);
  },

  /**
   * Show/hide hosts on the page according to correct <code>category</code> status
   */
  hideItem: function () {
    var controller = this.get('controller');
    var hostInfo = this.get('hostInfo');
    var category = this.get('category');
    if (category === "Hosts") {
      this.set('isVisible', true);
    } else if (category === "Succeeded" && hostInfo.get('bootStatus') == "success") {
      this.set('isVisible', true);
    } else if (category === "Failed" && hostInfo.get('bootStatus') == "error") {
      this.set('isVisible', true);
    } else {
      this.set('isVisible', false);
    }
  }.observes('category')
});
