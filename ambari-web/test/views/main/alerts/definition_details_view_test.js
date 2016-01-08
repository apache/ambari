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

var view, instanceTableRow;

describe('App.MainAlertDefinitionDetailsView', function () {

  beforeEach(function () {

    view = App.MainAlertDefinitionDetailsView.create();

    instanceTableRow = view.get('instanceTableRow').create();

  });

  describe("#goToHostAlerts()", function () {
    beforeEach(function () {
      sinon.stub(App.get('router'), 'transitionTo', Em.K);
    });
    afterEach(function () {
      App.get('router').transitionTo.restore();
    });
    it("not route to host - no event", function () {
      instanceTableRow.goToHostAlerts(null);
      expect(App.get('router').transitionTo.notCalled).to.be.true;
    });
    it("not route to host - no event context", function () {
      instanceTableRow.goToHostAlerts({});
      expect(App.get('router').transitionTo.notCalled).to.be.true;
    });
    it("routes to host", function () {
      instanceTableRow.goToHostAlerts({"context": "hostname"});
      expect(App.get('router').transitionTo.calledOnce).to.be.true;
    });
  });

  describe("#openFullResponse()", function() {

    beforeEach(function() {
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function() {
      App.ModalPopup.show.restore();
    });

    it("App.ModalPopup.show should be called", function() {
      instanceTableRow.openFullResponse({context: Em.Object.create({text: 'text1'})});
      expect(App.ModalPopup.show.calledWith({
        header: Em.I18n.t('alerts.instance.fullLogPopup.header'),
        secondary: null,
        content: 'text1',
        bodyClass: App.SelectablePopupBodyView
      })).to.be.true;
    });
  });

});
