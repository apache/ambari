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
require('views/main/admin/highAvailability/journalNode/step2_view');

describe('App.ManageJournalNodeWizardStep2View', function () {
  var view = App.ManageJournalNodeWizardStep2View.create({
    controller: Em.Object.create({
      content: {},
      loadStep: Em.K
    })
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.spy(view.get('controller'), 'loadStep');
    });
    after(function () {
      view.get('controller').loadStep.restore();
    });
    it("call loadStep", function () {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });

  describe("#journalNodesToAdd", function() {

    var wizardController = {
      getJournalNodesToAdd: Em.K
    };

    beforeEach(function() {
      this.mock = sinon.stub(wizardController, 'getJournalNodesToAdd');
      sinon.stub(App.router, 'get').returns(wizardController);
    });

    afterEach(function() {
      App.router.get.restore();
      this.mock.restore();
    });

    it("JournalNodes is get from `manageJournalNodeWizardController`", function() {
      this.mock.returns(['host1']);
      view.propertyDidChange('journalNodesToAdd');
      expect(view.get('journalNodesToAdd')).to.be.eql(['host1']);
    });
  });

  describe("#journalNodesToDelete", function() {

    var wizardController = {
      getJournalNodesToDelete: Em.K
    };

    beforeEach(function() {
      this.mock = sinon.stub(wizardController, 'getJournalNodesToDelete');
      sinon.stub(App.router, 'get').returns(wizardController);
    });

    afterEach(function() {
      App.router.get.restore();
      this.mock.restore();
    });

    it("JournalNodes is get from `manageJournalNodeWizardController`", function() {
      this.mock.returns(['host1']);
      view.propertyDidChange('journalNodesToDelete');
      expect(view.get('journalNodesToDelete')).to.be.eql(['host1']);
    });
  });

});
