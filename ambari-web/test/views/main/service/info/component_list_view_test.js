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
require('views/main/service/info/menu');


describe('App.SummaryMasterComponentsView', function () {
  var view;

  beforeEach(function () {
    view = App.SummaryMasterComponentsView.create({
      controller: Em.Object.create()
    });
  });

  describe("#mastersCompWillChange", function() {

    beforeEach(function() {
      sinon.stub(view, 'removeTooltips');
    });
    afterEach(function() {
      view.removeTooltips.restore();
    });

    it("removeTooltips should be called", function() {
      view.mastersCompWillChange();
      expect(view.removeTooltips.calledOnce).to.be.true;
    });
  });

  describe("#mastersCompDidChange", function() {

    beforeEach(function() {
      sinon.stub(view, 'attachTooltip');
    });
    afterEach(function() {
      view.attachTooltip.restore();
    });

    it("attachTooltip should be called", function() {
      view.mastersCompDidChange();
      expect(view.attachTooltip.calledOnce).to.be.true;
    });
  });

  describe("#didInsertElement", function() {

    beforeEach(function() {
      sinon.stub(view, 'attachTooltip');
    });
    afterEach(function() {
      view.attachTooltip.restore();
    });

    it("attachTooltip should be called", function() {
      view.didInsertElement();
      expect(view.attachTooltip.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement", function() {
    var mock = {tooltip: Em.K};

    beforeEach(function() {
      sinon.spy(mock, 'tooltip');
      sinon.stub(window, '$').returns(mock);
    });
    afterEach(function() {
      mock.tooltip.restore();
      window.$.restore();
    });

    it("tooltip should be called", function() {
      view.willDestroyElement();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

  describe("#removeTooltips", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.spy(mock, 'tooltip');
      sinon.stub(window, '$').returns(mock);
    });
    afterEach(function() {
      mock.tooltip.restore();
      window.$.restore();
    });

    it("tooltip should be called", function() {
      mock.length = 1;
      view.removeTooltips();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });

    it("tooltip should not be called", function() {
      mock.length = 0;
      view.removeTooltips();
      expect(mock.tooltip.called).to.be.false;
    });
  });

  describe("#attachTooltip", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
      sinon.stub(window, '$').returns(mock);
    });
    afterEach(function() {
      App.tooltip.restore();
      window.$.restore();
    });

    it("tooltip should be called", function() {
      mock.length = 1;
      view.attachTooltip();
      expect(App.tooltip.calledOnce).to.be.true;
    });

    it("tooltip should not be called", function() {
      mock.length = 0;
      view.attachTooltip();
      expect(App.tooltip.called).to.be.false;
    });
  });




});
