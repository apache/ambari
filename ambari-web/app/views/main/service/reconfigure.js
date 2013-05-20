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

App.StageStatusView = Em.View.extend({
  tagName: 'tr',
  hasStarted: null,
  classNameBindings: ['faintText'],
  showHostPopup:function(event){
    var serviceName = event.contexts[0];
    var controller = this.get("controller");
    App.HostPopup.initPopup(serviceName, controller);
  }
});

App.StageSuccessView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="icon-ok icon-large"></i> Done')
});

App.StageFailureView = Em.View.extend({
  layout: Ember.Handlebars.compile('<i class="icon-remove icon-large"></i> Failed')
});

App.StageInProgressView = Em.View.extend({
  stage: null,
  classNames: ['progress-striped', 'active', 'progress'],
  template: Ember.Handlebars.compile([
    '<div class="bar" {{bindAttr style="stage.barWidth"}}>',
    '</div>'
  ].join('\n')),

  isStageCompleted: function () {
    return this.get('obj.progress') == 100 || this.get('controller.isStepCompleted');
  }.property('controller.isStepCompleted', 'obj.progress')

});



