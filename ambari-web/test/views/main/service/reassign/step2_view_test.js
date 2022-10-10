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
require('/views/main/service/reassign/step2_view');


describe('App.ReassignMasterWizardStep2View', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep2View.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create()
        })
      })
    });
  });

  describe("#alertMessage", function() {

    beforeEach(function () {
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function () {
      App.get.restore();
    });

    it("should return message", function() {
      view.set('controller.content.reassign.component_name', 'OOZIE_SERVER');
      view.set('controller.content.reassign.display_name', 'OOZIE_SERVER');
      view.propertyDidChange('alertMessage');
      expect(view.get('alertMessage')).to.equal(Em.I18n.t('services.reassign.step2.body').format('OOZIE_SERVER'));
    });

    it("should return message for NAMENODE component with HA enabled", function() {
      view.set('controller.content.reassign.component_name', 'NAMENODE');
      view.set('controller.content.reassign.display_name', 'NAMENODE');
      view.propertyDidChange('alertMessage');
      expect(view.get('alertMessage')).to.equal(Em.I18n.t('services.reassign.step2.body.namenodeHA').format('NAMENODE'));
    });
  });

});
