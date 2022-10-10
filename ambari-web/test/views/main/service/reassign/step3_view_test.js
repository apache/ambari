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
require('/views/main/service/reassign/step3_view');


describe('App.ReassignMasterWizardStep3View', function () {
  var view;

  beforeEach(function() {
    view = App.ReassignMasterWizardStep3View.create({
      controller: Em.Object.create({
        content: Em.Object.create({
          reassign: Em.Object.create()
        }),
        loadStep: Em.K
      })
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadStep');
    });
    afterEach(function () {
      view.get('controller').loadStep.restore();
    });

    it("loadStep should be called", function() {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });

  describe("#jdbcSetupMessage", function() {

    it("should return false(1)", function() {
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.be.false;
    });

    it("should return false(2)", function() {
      view.set('controller.content.reassign.component_name', 'OOZIE_SERVER');
      view.set('controller.content.databaseType', 'derby');
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.be.false;
    });

    it("should return false(2)", function() {
      view.set('controller.content.reassign.component_name', 'OOZIE_SERVER');
      view.set('controller.content.databaseType', 'type');
      view.propertyDidChange('jdbcSetupMessage');
      expect(view.get('jdbcSetupMessage')).to.equal(Em.I18n.t('services.service.config.database.msg.jdbcSetup').format('type', 'type'));
    });
  });

});
