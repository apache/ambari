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

App.InstallerStep5View = Em.View.extend({

  templateName: require('templates/installer/step5'),

  submit: function (e) {
    App.router.transitionTo('step6');
  }

});

App.SelectHostView = Em.Select.extend({
  content: [],
  zId: null,
  selectedHost: null,
  serviceName: null,

  change: function () {
    App.router.get('installerStep5Controller').assignHostToMaster(this.get("serviceName"), this.get("value"), this.get("zId"));
  },

  didInsertElement: function () {
    this.set("value", this.get("selectedHost"));
  }
});

App.AddControlView = Em.View.extend({
  componentName: null,
  tagName: "span",
  classNames: ["badge", "badge-important"],
  template: Ember.Handlebars.compile('+'),

  click: function (event) {
    App.router.get('installerStep5Controller').addZookeepers();
  }
});

App.RemoveControlView = Em.View.extend({
  zId: null,
  tagName: "span",
  classNames: ["badge", "badge-important"],
  template: Ember.Handlebars.compile('-'),

  click: function (event) {
    App.router.get('installerStep5Controller').removeZookeepers(this.get("zId"));
  }
});
