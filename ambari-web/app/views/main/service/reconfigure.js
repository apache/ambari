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

App.MainServiceReconfigureView = Em.View.extend({

  templateName: require('templates/main/service/reconfigure')

});

App.StageLabelView = Em.View.extend({
  tagName: 'a',
  classNameBindings: ['removeLink'],
  attributeBindings: ['href'],
  href: '#',
  removeLink: null,
  didInsertElement: function() {
   this.onLink();
  },
  onLink: function() {
   if (this.get('showLink') === true) {
     this.set('removeLink',null);
   } else {
     this.set('removeLink','remove-link');
   }
  }.observes('showLink'),
  stage: null,
  click: function () {
    if (this.get('stage') && this.get('showLink')) {
      this.showHostPopup(this.get('stage.label'));
    }
  },

  showHostPopup: function (label) {
    var serviceName = label;
    var controller = this.get("controller");
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      var popupView = App.HostPopup.initPopup(serviceName, controller);
      popupView.set ('isNotShowBgChecked', !initValue);
    })
  },

  isStarted: function () {
    return  (this.get('stage') && this.get('stage.isStarted'));
  }.property('stage.isStarted'),

  showLink: function () {
    return (this.get('stage') && this.get('stage.showLink'));
  }.property('stage.showLink')
});

App.StageSuccessView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="icon-ok icon-large"></i> {{t common.done}}')
});

App.StageFailureView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="icon-remove icon-large"></i> {{t common.failed}}')
});

App.StageInProgressView = Em.View.extend({
  stage: null,
  classNames: ['progress-striped', 'active', 'progress'],
  template: Ember.Handlebars.compile('<div class="bar" {{bindAttr style="stage.barWidth"}}></div>'),

  isStageCompleted: function () {
    return this.get('obj.progress') == 100 || this.get('controller.isStepCompleted');
  }.property('controller.isStepCompleted', 'obj.progress')

});