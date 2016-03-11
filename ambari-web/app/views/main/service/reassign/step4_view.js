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

App.ReassignMasterWizardStep4View = App.HighAvailabilityProgressPageView.extend({

  headerTitle: Em.I18n.t('services.reassign.step4.header'),

  noticeInProgress: function () {
    return Em.I18n.t('services.reassign.step4.status.info').format(App.format.role(this.get('controller.content.reassign.component_name'), false))
  }.property('controller.content.reassign.component_name'),

  noticeFailed: function () {
    return Em.I18n.t('services.reassign.step4.status.failed').format(App.format.role(this.get('controller.content.reassign.component_name'), false),this.get('controller.content.reassignHosts.source'),this.get('controller.content.reassignHosts.target'));
  }.property('controller.content.reassign.component_name','controller.content.reassignHosts.source','controller.content.reassignHosts.target'),

  noticeCompleted: function () {
    if (this.get('controller.content.hasManualSteps')) {
      return Em.I18n.t('services.reassign.step4.status.success.withManualSteps').format(App.format.role(this.get('controller.content.reassign.component_name'), false));
    } else {
      return Em.I18n.t('services.reassign.step4.status.success').format(App.format.role(this.get('controller.content.reassign.component_name'), false),this.get('controller.content.reassignHosts.source'),this.get('controller.content.reassignHosts.target'));
    }
  }.property('controller.content.reassign.component_name','controller.content.reassignHosts.source','controller.content.reassignHosts.target'),

  submitButtonText: function () {
    if (this.get('controller.content.hasManualSteps')) {
      return Em.I18n.t('common.next') + ' &rarr;';
    } else {
      return Em.I18n.t('common.complete');
    }
  }.property('controller.content.hasManualSteps'),

  templateName: require('templates/main/service/reassign/step4'),

  labelWidth: 'span5'
});
