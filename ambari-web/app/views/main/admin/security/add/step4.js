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

App.MainAdminSecurityAddStep4View = Em.View.extend({

  templateName: require('templates/main/admin/security/add/step4'),
  didInsertElement: function () {
    this.get('controller').loadStep();
  },
  msgColor: 'alert-info',
  message: Em.I18n.t('admin.security.step4.body.header'),
  onResult: function () {
    var stage1 = this.get('controller.stages').findProperty('stage', 'stage2');
    var stage2 = this.get('controller.stages').findProperty('stage', 'stage3');
    var stage3 = this.get('controller.stages').findProperty('stage', 'stage4');
      if (stage2 && stage2.get('isSuccess') === true ) {
        this.set('message', Em.I18n.t('admin.security.step4.body.success.header'));
        this.set('msgColor','alert-success');
      } else if ((stage1 && stage1.get('isError') === true) || (stage2 && stage2.get('isError') === true)) {
        this.set('message', Em.I18n.t('admin.security.step4.body.failure.header'));
        this.set('msgColor','alert-error');
      } else {
        this.set('message', Em.I18n.t('admin.security.step4.body.header'));
        this.set('msgColor','alert-info');
      }
  }.observes('controller.stages.@each.isCompleted')

});

App.StageStatusView = Em.View.extend({
  tagName: 'tr',
  hasStarted: null,
  classNameBindings: ['faintText']
});

App.StageSuccessView = Em.View.extend({
  template: Ember.Handlebars.compile('<i class="icon-ok icon-large"></i> Done')
});

App.StageFailureView = Em.View.extend({
  template: Ember.Handlebars.compile('<i class="icon-remove icon-large"></i> Failed')
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

