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
require('views/main/admin/highAvailability/journalNode/progress_view');

describe('App.ManageJournalNodeProgressPageView', function () {
  var view = App.ManageJournalNodeProgressPageView.create({
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

  describe("#headerTitle", function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(1);
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it("get header title", function () {
      view.propertyDidChange('headerTitle');
      expect(view.get('headerTitle')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step1.header'));
    });

  });

  describe("#noticeInProgress", function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(1);
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it("get in progress notice", function () {
      view.propertyDidChange('noticeInProgress');
      expect(view.get('noticeInProgress')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step1.notice.inProgress'));
    });

  });

});
