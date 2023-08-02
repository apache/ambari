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
require('views/wizard/step7/directories_tab_view');
var view;

function getView() {
  return App.DirectoriesTabOnStep7View.create({
    controller: Em.Object.create({
      selectedServiceObserver: Em.K,
      tabs: Em.A([
        Em.Object.create({
          themeName: 'directories',
          serviceName: 'S1',
          selectedServiceName: 'S1',
          isActive: true
        })
      ])
    })
  });
}

var tab = Em.Object.create({
  themeName: 'directories',
  serviceName: 'S1',
  selectedServiceName: 'S1',
  isActive: true
});

var stepConfigs = Em.A([
  Em.Object.create({
    serviceName: 'S1',
  })
]);

describe('App.DirectoriesTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });

  describe("#didInsertElement", function () {

    beforeEach(function () {
      sinon.stub(App.Tab, 'find').returns([tab]);
      this.runNextStub = sinon.stub(Em.run, 'next');
      this.setServicesStub = sinon.stub(view, 'setServices', Em.K);
    });

    afterEach(function () {
      App.Tab.find.restore();
      this.runNextStub.restore();
      this.setServicesStub.restore();
    });

    it("set services and enable right arrow", function () {
      view.set('controller.tabs', [tab]);
      view.set('controller.stepConfigs', stepConfigs);
      view.didInsertElement();
      expect(this.setServicesStub.called).to.be.true;
      expect(this.runNextStub.calledOnce).to.be.true;
    });
  });

  describe("#enableRightArrow", function () {

    beforeEach(function () {
      this.mock = sinon.stub($, 'find');
    });

    afterEach(function () {
      $.find.restore();
    });

    it("container.width >= content.width", function () {
      view.enableRightArrow();
      expect(view.isRightArrowDisabled).to.be.true;
    });
  });

  describe("#getScrollInterval", function () {

    beforeEach(function () {
      this.mock = sinon.stub($, 'find');
    });

    afterEach(function () {
      $.find.restore();
    });

    it("returns scroll interval", function () {
      expect(view.getScrollInterval()).to.equal(301);
    });
  });

  describe("#scrollTabsLeft", function () {

    beforeEach(function () {
      sinon.spy(view, 'scrollTabs');
    });

    afterEach(function () {
      view.scrollTabs.restore();
    });

    it("scrollTabsLeft if left arrow is disabled", function () {
      view.set('isLeftArrowDisabled', true);
      view.scrollTabsLeft();
      expect(view.scrollTabs.calledWith('left')).to.be.false;
    });

    it("scrollTabsLeft if left arrow is enabled", function () {
      view.set('isLeftArrowDisabled', false);
      view.scrollTabsLeft();
      expect(view.scrollTabs.calledWith('left')).to.be.true;
    });
  });

  describe("#scrollTabsRight", function () {

    beforeEach(function () {
      sinon.spy(view, 'scrollTabs');
    });

    afterEach(function () {
      view.scrollTabs.restore();
    });

    it("scrollTabsRight if right arrow is disabled", function () {
      view.set('isRightArrowDisabled', true);
      view.scrollTabsRight();
      expect(view.scrollTabs.calledWith('right')).to.be.false;
    });

    it("scrollTabsRight if right arrow is enabled", function () {
      view.set('isRightArrowDisabled', false);
      view.scrollTabsRight();
      expect(view.scrollTabs.calledWith('right')).to.be.true;
    });
  });

  describe("#scrollTabs", function () {

    beforeEach(function () {
      this.mock = sinon.stub($, 'find');
      this.spy = sinon.spy($.prototype, 'animate');
    });

    afterEach(function () {
      $.find.restore();
      this.spy.restore();
    });

    it("scroll tabs right px", function () {
      view.scrollTabs('right');
      expect(view.get('isLeftArrowDisabled')).to.be.false;
      expect(view.get('isRightArrowDisabled')).to.be.true;
      expect(this.spy.calledWith({scrollLeft: '+=301px'})).to.be.true;
    });

    it("scroll tabs left px", function () {
      view.scrollTabs('left');
      expect(view.get('isLeftArrowDisabled')).to.be.true;
      expect(view.get('isRightArrowDisabled')).to.be.false;
      expect(this.spy.calledWith({scrollLeft: '-=301px'})).to.be.true;
    });
  });

  describe("#setServices", function () {

    beforeEach(function () {
      this.mock = sinon.stub($, 'find');
      sinon.stub(App.Tab, 'find').returns([tab]);
      sinon.stub(view.get('controller'), 'selectedServiceObserver');
    });

    afterEach(function () {
      $.find.restore();
      App.Tab.find.restore();
      view.get('controller').selectedServiceObserver.restore();
    });

    it("should set services", function () {
      view.set('controller.stepConfigs', stepConfigs);
      view.setServices();
      expect(view.get('services')).to.eql([tab]);
      expect(view.get('controller.selectedService')).to.eql(stepConfigs[0]);
      expect(view.get('controller').selectedServiceObserver.called).to.be.true;
    });
  });

});