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
require('/views/main/service/reassign/step4_view');


describe('App.ReassignMasterWizardStep4View', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep4View.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          reassignHosts: Em.Object.create(),
          reassign: Em.Object.create()
        }),
      })
    });
  });

  describe("#noticeCompleted", function() {

    it("should return message", function() {
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.propertyDidChange('noticeCompleted');
      expect(view.get('noticeCompleted')).to.equal(Em.I18n.t('services.reassign.step4.status.success').format('NameNode','host1','host2'));
    });

    it("should return proper message if has manual steps", function() {
      view.set('controller.content.reassignHosts.source', 'host1');
      view.set('controller.content.reassignHosts.target', 'host2');
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.hasManualSteps', true);
      view.propertyDidChange('noticeCompleted');
      expect(view.get('noticeCompleted')).to.equal(Em.I18n.t('services.reassign.step4.status.success.withManualSteps').format('NameNode'));
    });
  });
});
