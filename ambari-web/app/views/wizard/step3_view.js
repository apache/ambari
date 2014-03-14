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
  category: '',

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  message:'',
  linkText: '',
  status: '',

  registeredHostsMessage: '',

  setRegisteredHosts: function(){
    this.set('registeredHostsMessage',Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('controller.registeredHosts').length));
  }.observes('controller.registeredHosts'),

  monitorStatuses: function() {
    var hosts = this.get('controller.bootHosts');
    var failedHosts = hosts.filterProperty('bootStatus', 'FAILED').length;

    if (hosts.length === 0) {
      this.set('status', 'alert-warn');
      this.set('linkText', '');
      this.set('message', Em.I18n.t('installer.step3.warnings.missingHosts'));
    } else if (!this.get('controller.isWarningsLoaded')) {
      this.set('status', 'alert-info');
      this.set('linkText', '');
      this.set('message', Em.I18n.t('installer.step3.warning.loading'));
    } else if (this.get('controller.isHostHaveWarnings') || this.get('controller.repoCategoryWarnings.length') || this.get('controller.diskCategoryWarnings.length')) {
      this.set('status', 'alert-warn');
      this.set('linkText', Em.I18n.t('installer.step3.warnings.linkText'));
      this.set('message', Em.I18n.t('installer.step3.warnings.fails').format(hosts.length - failedHosts));
    } else {
      this.set('status', 'alert-success');
      this.set('linkText', Em.I18n.t('installer.step3.noWarnings.linkText'));
      if (failedHosts == 0) {
        // all are ok
        this.set('message', Em.I18n.t('installer.step3.warnings.noWarnings').format(hosts.length));
      } else if (failedHosts == hosts.length) {
        // all failed
        this.set('status', 'alert-warn');
        this.set('linkText', '');
        this.set('message', Em.I18n.t('installer.step3.warnings.allFailed').format(failedHosts));
      } else {
        // some failed
        this.set('message', Em.I18n.t('installer.step3.warnings.someWarnings').format((hosts.length - failedHosts), failedHosts));
      }
    }
  }.observes('controller.isWarningsLoaded', 'controller.isHostHaveWarnings', 'controller.bootHosts.@each.bootStatus', 'controller.repoCategoryWarnings', 'controller.diskCategoryWarnings')
});

//todo: move it inside WizardStep3View
App.WizardHostView = Em.View.extend({

  tagName: 'tr',
  classNameBindings: ['hostInfo.bootStatus', 'hostInfo.isVisible::hidden'],
  hostInfo: null,

  remove: function () {
    this.get('controller').removeHost(this.get('hostInfo'));
  },

  retry: function() {
    this.get('controller').retryHost(this.get('hostInfo'));
  },

  isRemovable: function () {
    return true;
  }.property(),

  isRetryable: function() {
    // return ['FAILED'].contains(this.get('hostInfo.bootStatus'));
    return false;
  }.property('hostInfo.bootStatus')

});


