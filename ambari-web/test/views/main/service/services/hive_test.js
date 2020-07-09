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
require('/views/main/service/services/hive');

describe('App.MainDashboardServiceHiveView', function () {

  var view;

  beforeEach(function() {
    view = App.MainDashboardServiceHiveView.create({
      controller: Em.Object.create({
        hiveServerEndPoints: Em.Object.create({
          clear: Em.K
        }),
        setHiveEndPointsValue: Em.K
      })
    });
  });

  describe("#willDestroyElement", function() {

    beforeEach(function () {
      sinon.stub(view.get('controller.hiveServerEndPoints'), 'clear', Em.K);
    });

    afterEach(function () {
      view.get('controller.hiveServerEndPoints').clear.restore();
    });

    it("should execute clear function", function() {
      view.willDestroyElement();
      expect(view.get('controller.hiveServerEndPoints').clear.calledOnce).to.be.true;
    });
  });

  describe("#viewLinks", function() {

    beforeEach(function() {
      sinon.stub(App.router, 'get').withArgs('mainViewsController.ambariViews')
        .returns([
          Em.Object.create({label: 'view1', instanceName: 'view1'}),
          Em.Object.create({label: 'view2', instanceName: 'view2', overwriteLabel: true}),
          Em.Object.create({label: 'view3', instanceName: 'view3'})
        ]);
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("should return links array", function() {
      view.set('viewsToShow', {
        view1: {},
        view2: {
          overwriteLabel: 'common.label'
        }
      });
      view.propertyDidChange('viewLinks');
      expect(view.get('viewLinks')).to.eql([
        {
          viewInstance: Em.Object.create({label: 'view1', instanceName: 'view1'}),
          label: 'view1'
        },
        {
          viewInstance: Em.Object.create({label: 'view2', instanceName: 'view2', overwriteLabel: true}),
          label: 'Label'
        }
      ]);
    });
  });

  describe("#didInsertElement", function() {
    var dfd;

    beforeEach(function () {
      dfd = $.Deferred();
      sinon.stub(App.get('router.mainController'), 'isLoading').returns(dfd);
      sinon.stub(view.get('controller'), 'setHiveEndPointsValue', Em.K);
      view.didInsertElement();
      dfd.resolve();
    });

    afterEach(function () {
      App.get('router.mainController.isLoading').restore();
    });

    it("should execute setHiveEndPointsValue function", function() {
      expect(view.get('controller').setHiveEndPointsValue.called).to.be.true;
    });
  });

  describe("#clipBoardView", function () {
    var clipBoardView;

    beforeEach(function () {
      clipBoardView = view.get('clipBoardView').create();
    });

    describe("#didInsertElement()", function () {
      var mock = {
        tooltip: Em.K
      };
      var mock2 = {
        attr: Em.K
      };

      beforeEach(function() {
        sinon.stub(mock, 'tooltip');
        sinon.stub(mock2, 'attr');
        sinon.stub(clipBoardView, '$').returns(mock2);
        sinon.stub(window, '$').withArgs("[rel=clipboard-tooltip]").returns(mock);
        this.runNextStub = sinon.stub(Em.run, 'next', Em.clb);
      });
      afterEach(function() {
        mock.tooltip.restore();
        mock2.attr.restore();
        clipBoardView.$.restore();
        window.$.restore();
        this.runNextStub.restore();
      });

      it("tooltip destroy should be called", function () {
        clipBoardView.didInsertElement();
        expect(mock.tooltip.calledWith({trigger: 'click'})).to.be.true;
      });
    });

    describe("#mouseLeave()", function () {
      var mock = {
        tooltip: Em.K
      };

      beforeEach(function() {
        sinon.stub(mock, 'tooltip');
        sinon.stub(window, '$').withArgs("[rel=clipboard-tooltip]").returns(mock);
        this.runNextStub = sinon.stub(Em.run, 'next', Em.clb);
      });
      afterEach(function() {
        mock.tooltip.restore();
        window.$.restore();
        this.runNextStub.restore();
      });

      it("tooltip hide should be called", function () {
        clipBoardView.mouseLeave();
        expect(mock.tooltip.calledWith('hide')).to.be.true;
      });
    });
  });

});