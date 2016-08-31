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
require('messages');
require('views/main/alert_definitions_view');

var view;

function getView() {
  return App.MainAlertDefinitionsView.create({
    controller: Em.Object.create()
  });
}

describe('App.MainAlertDefinitionsView', function () {

  beforeEach(function () {
    view = getView();
    sinon.stub(App.db, 'setFilterConditions', Em.K);
    sinon.stub(App.db, 'getFilterConditions').returns([]);
    sinon.stub(App.db, 'getDisplayLength', Em.K);
    sinon.stub(App.db, 'setStartIndex', Em.K);
    sinon.stub(view, 'initFilters', Em.K);
  });

  afterEach(function () {
    App.db.setFilterConditions.restore();
    App.db.getFilterConditions.restore();
    App.db.getDisplayLength.restore();
    App.db.setStartIndex.restore();
    view.initFilters.restore();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'totalCount', 'content.length', 'number');

  describe('#serviceFilterView', function () {
    it('Add Ambari service to filters', function () {
      var serviceFilterClass = view.serviceFilterView;
      var content = serviceFilterClass.create({}).get('content');
      expect(content[0].label).to.be.equal(Em.I18n.t('common.all'));
      expect(content[content.length - 1].label).to.be.equal(Em.I18n.t('app.name'));
    });
  });

  describe('#willInsertElement', function () {

    beforeEach(function(){
      sinon.stub(view, 'clearFilterConditionsFromLocalStorage', Em.K);
      sinon.stub(App.db, 'getSortingStatuses').returns([
        {
          name: "summary",
          status: "sorting"
        }
      ]);
      sinon.stub(App.db, 'setSortingStatuses');
    });

    afterEach(function(){
      view.clearFilterConditionsFromLocalStorage.restore();
      App.db.getSortingStatuses.restore();
      App.db.setSortingStatuses.restore();
    });

    it('should call clearFilterCondition if controller.showFilterConditionsFirstLoad is false', function () {
      view.set('controller', {showFilterConditionsFirstLoad: false, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.calledOnce).to.be.true;
    });

    it('should not call clearFilterCondition if controller.showFilterConditionsFirstLoad is true', function () {
      view.set('controller', {showFilterConditionsFirstLoad: true, content: []});
      view.willInsertElement();
      expect(view.clearFilterConditionsFromLocalStorage.calledOnce).to.be.false;
    });

    it('showFilterConditionsFirstLoad is true', function () {
      view.set('controller', {showFilterConditionsFirstLoad: true, name: 'ctrl1'});
      view.willInsertElement();
      expect(App.db.setSortingStatuses.calledWith('ctrl1',
        [
          {
            name: "summary",
            status: "sorting"
          },
          {
            name: "summary",
            status: "sorting_asc"
          }
        ])).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view, 'contentObsOnce');
      sinon.stub(view, 'tooltipsUpdater');
      view.didInsertElement();
    });

    afterEach(function() {
      Em.run.next.restore();
      view.contentObsOnce.restore();
      view.tooltipsUpdater.restore();
    });

    it("isInitialRendering should be false", function() {
      expect(view.get('isInitialRendering')).to.be.false;
    });

    it("contentObsOnce should be called", function() {
      expect(view.contentObsOnce.calledOnce).to.be.true;
    });

    it("tooltipsUpdater should be called", function() {
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    var container = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(container);
      sinon.stub(view, 'removeObserver');
      sinon.stub(container, 'tooltip');
      view.willDestroyElement();
    });

    afterEach(function() {
      window.$.restore();
      view.removeObserver.restore();
      container.tooltip.restore();
    });

    it("tooltip should be destroyed", function() {
      expect(container.tooltip.calledWith('destroy')).to.be.true;
    });

    it("removeObserver should be called", function() {
      expect(view.removeObserver.calledWith('pageContent.length', view, 'tooltipsUpdater')).to.be.true;
    });
  });

  describe("#saveStartIndex()", function () {

    it("App.db.setStartIndex should be called", function() {
      view.set('controller.name', 'ctrl1');
      view.set('startIndex', 1);
      view.saveStartIndex();
      expect(App.db.setStartIndex.calledWith('ctrl1', 1)).to.be.true;
    });
  });

  describe("#clearStartIndex()", function () {

    it("App.db.setStartIndex should be called", function() {
      view.set('controller.name', 'ctrl1');
      view.clearStartIndex();
      expect(App.db.setStartIndex.calledWith('ctrl1', null)).to.be.true;
    });
  });

  describe("#alertGroupFilterView", function () {
    var alertGroupFilterView;

    beforeEach(function() {
      alertGroupFilterView = view.get('alertGroupFilterView').create({
        parentView: Em.Object.create({
          updateFilter: Em.K
        })
      });
    });

    describe("#didInsertElement()", function () {

      beforeEach(function() {
        sinon.stub(alertGroupFilterView, '$').returns({parent: function() {
          return {
            addClass: Em.K
          }
        }});
        sinon.stub(alertGroupFilterView, 'showClearFilter');
        sinon.stub(alertGroupFilterView, 'updateContent');
        alertGroupFilterView.didInsertElement();
      });

      afterEach(function() {
        alertGroupFilterView.$.restore();
        alertGroupFilterView.updateContent.restore();
        alertGroupFilterView.showClearFilter.restore();
      });

      it("updateContent should be called", function() {
        expect(alertGroupFilterView.updateContent.calledOnce).to.be.true;
      });

      it("value should be empty", function() {
        expect(alertGroupFilterView.get('value')).to.be.empty;
      });
    });

    describe("#updateContent()", function () {

      beforeEach(function() {
        sinon.stub(App.AlertGroup, 'find').returns([
          Em.Object.create({
            id: 'g1',
            displayNameDefinitions: 'def1',
            default: true
          }),
          Em.Object.create({
            id: 'g2',
            displayNameDefinitions: 'def2',
            default: false
          })
        ]);
        sinon.stub(alertGroupFilterView, 'onValueChange');
        alertGroupFilterView.set('parentView.controller', {content: [{}]});
        alertGroupFilterView.updateContent();
      });

      afterEach(function() {
        App.AlertGroup.find.restore();
        alertGroupFilterView.onValueChange.restore();
      });

      it("onValueChange should be called", function() {
        expect(alertGroupFilterView.onValueChange.calledOnce).to.be.true;
      });

      it("content should be set", function() {
        expect(alertGroupFilterView.get('content')).to.be.eql([
          Em.Object.create({
            value: '',
            label: Em.I18n.t('common.all') + ' (1)'
          }),
          Em.Object.create({
            value: 'g1',
            label: 'def1'
          }),
          Em.Object.create({
            value: 'g2',
            label: 'def2'
          })
        ]);
      });
    });

    describe("#selectCategory()", function () {

      beforeEach(function() {
        sinon.stub(alertGroupFilterView.get('parentView'), 'updateFilter');
        alertGroupFilterView.selectCategory({context: {value: 'val1'}});
      });

      afterEach(function() {
        alertGroupFilterView.get('parentView').updateFilter.restore();
      });

      it("value should be set", function() {
        expect(alertGroupFilterView.get('value')).to.be.equal('val1');
      });

      it("updateFilter should be called", function() {
        expect(alertGroupFilterView.get('parentView').updateFilter.calledWith(
          7, 'val1', 'alert_group'
        )).to.be.true;
      });
    });

    describe("#onValueChange()", function () {

      beforeEach(function() {
        sinon.stub(alertGroupFilterView.get('parentView'), 'updateFilter');
      });

      afterEach(function() {
        alertGroupFilterView.get('parentView').updateFilter.restore();
      });

      it("value is undefined", function() {
        alertGroupFilterView.set('value', undefined);
        alertGroupFilterView.onValueChange();
        expect(alertGroupFilterView.get('value')).to.be.empty;
        expect(alertGroupFilterView.get('parentView').updateFilter.calledWith(
          7, '', 'alert_group'
        )).to.be.true;
      });

      it("value is not undefined", function() {
        var option = Em.Object.create({
          selected: true,
          value: 'val1'
        });
        alertGroupFilterView.set('content', [ option ]);
        alertGroupFilterView.set('value', 'val1');
        alertGroupFilterView.onValueChange();
        expect(option.get('selected')).to.be.true;
        expect(alertGroupFilterView.get('parentView').updateFilter.calledWith(
          7, 'val1', 'alert_group'
        )).to.be.true;
      });
    });
  });

  describe("#paginationLeftClass", function() {

    it("startIndex is 2", function() {
      view.set('startIndex', 2);
      expect(view.get('paginationLeftClass')).to.equal('paginate_previous');
    });

    it("startIndex is 1", function() {
      view.set('startIndex', 1);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });

    it("startIndex is 0", function() {
      view.set('startIndex', 0);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });
  });

  describe("#paginationRightClass", function() {

    it("endIndex more than filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 3
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex equal to filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 4
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex less than filteredCount", function() {
      view.reopen({
        endIndex: 3,
        filteredCount: 4
      });
      view.propertyDidChange('paginationRightClass');
      expect(view.get('paginationRightClass')).to.equal('paginate_next');
    });
  });

  describe("#previousPage()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.previousPage();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#nextPage()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.nextPage();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#tooltipsUpdater", function () {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', function(context, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
    });

    afterEach(function() {
      Em.run.next.restore();
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.tooltipsUpdater();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#updateFilter()", function () {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
    });

    it("tooltipsUpdater should be called", function() {
      view.updateFilter(1, 'val1', 'type1');
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });
});
