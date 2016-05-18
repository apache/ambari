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
require('views/main/dashboard/config_history_view');
require('utils/load_timer');

describe('App.MainConfigHistoryView', function() {
  var view = App.MainConfigHistoryView.create({
    totalCount: 0,
    filteredCount: 0
  });
  view.reopen({
    controller: Em.Object.create({
      name: 'mainConfigHistoryController11',
      paginationProps: [
        {
          name: 'displayLength'
        },
        {
          name: 'startIndex'
        }
      ],
      doPolling: Em.K,
      load: function () {
        return {done: Em.K};
      },
      colPropAssoc: []
    })
  });
  view.removeObserver('controller.resetStartIndex', view, 'resetStartIndex');

  describe("#filteredContentInfo", function () {
    it("is formatted with filteredCount and totalCount", function () {
      view.set('filteredCount', 1);
      view.set('totalCount', 2);
      view.propertyDidChange('filteredContentInfo');
      expect(view.get('filteredContentInfo')).to.eql(Em.I18n.t('tableView.filters.filteredConfigVersionInfo').format(1, 2));
    });
  });

  describe("#serviceFilterView", function () {
    var subView = view.get('serviceFilterView').create({
      parentView: view
    });

    before(function () {
      sinon.stub(App.StackService, 'find').returns([Em.Object.create({
        serviceName: 'S1',
        displayName: 's1'
      })])
    });
    after(function () {
      App.StackService.find.restore();
    });
    it("content", function () {
      expect(subView.get('content')).to.eql([
        {
          "value": "",
          "label": Em.I18n.t('common.all')
        },
        {
          "value": "S1",
          "label": "s1"
        }
      ]);
    });

    before(function () {
      sinon.stub(view, 'updateFilter', Em.K);
    });
    after(function () {
      view.updateFilter.restore();
    });
    it("call onChangeValue()", function () {
      subView.set('column', 1);
      subView.set('value', 'value');
      subView.onChangeValue();
      expect(view.updateFilter.calledWith(1, 'value', 'select')).to.be.true;
    });
  });

  describe("#configGroupFilterView", function () {
    var subView = view.get('configGroupFilterView').create({
      parentView: view
    });

    before(function () {
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        Em.Object.create({groupName: 'G1'}),
        Em.Object.create({groupName: 'G1'}),
        Em.Object.create({groupName: null})
      ]);
    });
    after(function () {
      App.ServiceConfigVersion.find.restore();
    });
    it("content", function () {
      expect(subView.get('content')).to.eql([
        {
          "value": "",
          "label": Em.I18n.t('common.all')
        },
        {
          "value": "G1",
          "label": "G1"
        }
      ]);
    });

    before(function () {
      sinon.stub(view, 'updateFilter', Em.K);
    });
    after(function () {
      view.updateFilter.restore();
    });
    it("call onChangeValue()", function () {
      subView.set('column', 1);
      subView.set('value', 'value');
      subView.onChangeValue();
      expect(view.updateFilter.calledWith(1, 'value', 'select')).to.be.true;
    });
  });

  /**
   * for now we don't use this method
  describe("#modifiedFilterView", function () {
    var subView = view.get('modifiedFilterView').create({
      parentView: view,
      controller: {
        modifiedFilter: {
          actualValues: {
            startTime: 0,
            endTime: 1
          }
        }
      }
    });

    before(function () {
      sinon.stub(view, 'updateFilter', Em.K);
    });
    after(function () {
      view.updateFilter.restore();
    });
    it("call onTimeChange()", function () {
      subView.set('column', 1);
      subView.onTimeChange();
      expect(view.updateFilter.calledWith(1, [0, 1], 'range')).to.be.true;
    });
  });*/

  describe("#authorFilterView", function () {
    var subView = view.get('authorFilterView').create({
      parentView: view
    });

    before(function () {
      sinon.stub(view, 'updateFilter', Em.K);
    });
    after(function () {
      view.updateFilter.restore();
    });
    it("call onChangeValue()", function () {
      subView.set('column', 1);
      subView.set('value', 'value');
      subView.onChangeValue();
      expect(view.updateFilter.calledWith(1, 'value', 'string')).to.be.true;
    });
  });

  describe("#notesFilterView", function () {
    var subView = view.get('notesFilterView').create({
      parentView: view
    });

    before(function () {
      sinon.stub(view, 'updateFilter', Em.K);
    });
    after(function () {
      view.updateFilter.restore();
    });
    it("call onChangeValue()", function () {
      subView.set('column', 1);
      subView.set('value', 'value');
      subView.onChangeValue();
      expect(view.updateFilter.calledWith(1, 'value', 'string')).to.be.true;
    });
  });

  describe("#ConfigVersionView", function () {
    var subView = view.get('ConfigVersionView').create({
      parentView: view
    });

    before(function () {
      sinon.stub(App, 'tooltip', Em.K);
    });
    after(function () {
      App.tooltip.restore();
    });
    it("call didInsertElement()", function () {
      subView.didInsertElement();
      expect(App.tooltip.calledOnce).to.be.true;
    });
    it("call toggleShowLessStatus()", function () {
      subView.set('showLessNotes', true);
      subView.toggleShowLessStatus();
      expect(subView.get('showLessNotes')).to.be.false;
    });
  });

  describe('#didInsertElement()', function() {

    beforeEach(function () {
      sinon.stub(view, 'addObserver', Em.K);
      sinon.spy(view.get('controller'), 'doPolling');
      view.didInsertElement();
    });

    afterEach(function () {
      view.addObserver.restore();
      view.get('controller').doPolling.restore();
    });

    it('addObserver is called twice', function() {
      expect(view.addObserver.calledTwice).to.be.true;
    });

    it('isInitialRendering is true', function() {
      expect(view.get('isInitialRendering')).to.be.true;
    });

    it('controller.isPolling is true', function() {
      expect(view.get('controller.isPolling')).to.be.true;
    });

    it('controller.doPolling is true', function() {
      expect(view.get('controller').doPolling.calledOnce).to.be.true;
    });
  });

  describe('#updateFilter()', function () {
    var cases = [
      {
        isInitialRendering: false,
        updateFilterCalled: true,
        title: 'updateFilter should be called'
      },
      {
        isInitialRendering: true,
        updateFilterCalled: false,
        title: 'updateFilter should not be called'
      }
    ];
    beforeEach(function () {
      sinon.stub(view, 'saveFilterConditions', Em.K);
      view.set('filteringComplete', true);
    });
    afterEach(function () {
      view.saveFilterConditions.restore();
    });
    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('isInitialRendering', item.isInitialRendering);
        view.updateFilter(1, 'value', 'string');
        expect(view.get('saveFilterConditions').calledWith(1, 'value', 'string')).to.equal(item.updateFilterCalled);
      });
    });
  });

  describe('#willDestroyElement()', function() {
    it('controller.isPolling is false', function() {
      view.willDestroyElement();
      expect(view.get('controller.isPolling')).to.be.false;
    });
  });

  describe('#refresh()', function() {

    beforeEach(function () {
      sinon.spy(view.get('controller'), 'load');
      view.refresh();
    });

    afterEach(function () {
      view.get('controller').load.restore();
    });

    it('filteringComplete is false', function() {
      expect(view.get('filteringComplete')).to.be.false;
    });

    it('controller.load is called once', function() {
      expect(view.get('controller').load.calledOnce).to.be.true;
    });
  });

  describe("#refreshDone()", function () {
    beforeEach(function () {
      sinon.stub(view, 'propertyDidChange', Em.K);
      view.set('filteringComplete', false);
      view.set('controller.resetStartIndex', true);
      view.refreshDone();
    });
    afterEach(function () {
      view.propertyDidChange.restore();
    });
    it("filteringComplete is true", function () {
      expect(view.get('filteringComplete')).to.be.true;
    });
    it("controller.resetStartIndex is false", function () {
      expect(view.get('controller.resetStartIndex')).to.be.false;
    });
  });

  App.TestAliases.testAsComputedAlias(view, 'colPropAssoc', 'controller.colPropAssoc', 'array');

});
