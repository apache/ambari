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
require('views/main/admin/highAvailability/nameNode/rollbackHA/step2_view');

describe('App.RollbackHighAvailabilityWizardStep2View', function () {
  var view = App.RollbackHighAvailabilityWizardStep2View.create({
    controller: Em.Object.create({
      content: Em.Object.create({})
    })
  });

  describe("#step2BodyText", function() {

    beforeEach(function () {
      this.stub = sinon.stub(App.HostComponent, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("is activeNN true", function() {
      this.stub.returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Active NameNode',
          hostName: 'host1'
        }),
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Standby NameNode',
          hostName: 'host2'
        })
      ]);
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step2BodyText');
      expect(view.get('step2BodyText')).to.equal(Em.I18n.t('admin.highAvailability.rollback.step2.body').format('user', 'host1'));
    });

    it("is activeNN false", function() {
      this.stub.returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Standby NameNode',
          hostName: 'host1'
        })
      ]);
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step2BodyText');
      expect(view.get('step2BodyText')).to.equal(Em.I18n.t('admin.highAvailability.rollback.step2.body').format('user', 'host1'));
    });

  });
});
