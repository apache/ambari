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
require('views/main/host');

describe('App.MainHostView', function () {

  var view;

  beforeEach(function () {
    view = App.MainHostView.create({
      controller: App.MainHostController.create()
    });
  });

  describe('#didInsertElement', function () {

    var cases = [
        {
          methodName: 'clearFiltersObs',
          propertyToChange: 'controller.clearFilters',
          callCount: 2
        },
        {
          methodName: 'toggleAllHosts',
          propertyToChange: 'selectAllHosts',
          callCount: 1
        },
        {
          methodName: 'overlayObserver',
          propertyToChange: 'filteringComplete',
          callCount: 2
        },
        {
          methodName: 'updateHostsPagination',
          propertyToChange: 'startIndex',
          callCount: 1
        },
        {
          methodName: 'updatePagination',
          propertyToChange: 'displayLength',
          callCount: 1
        },
        {
          methodName: 'updatePaging',
          propertyToChange: 'filteredCount',
          callCount: 2
        }
      ],
      title = '{0} changed';

    beforeEach(function () {
      cases.forEach(function (item) {
        sinon.stub(view, item.methodName, Em.K);
      });
    });

    afterEach(function () {
      cases.forEach(function (item) {
        view[item.methodName].restore();
      });
    });

    cases.forEach(function (item) {
      it(title.format(item.propertyToChange), function () {
        view.didInsertElement();
        view.propertyDidChange(item.propertyToChange);
        expect(view[item.methodName].callCount).to.equal(item.callCount);
      });
    });

  });

  describe('#updateHostsPagination', function () {

    beforeEach(function () {
      sinon.stub(view, 'clearExpandedSections', Em.K);
      sinon.stub(view, 'updatePagination', Em.K);
    });

    afterEach(function () {
      view.clearExpandedSections.restore();
      view.updatePagination.restore();
    });

    it('should execute clearExpandedSections and updatePagination', function () {
      view.updateHostsPagination();
      expect(view.clearExpandedSections.calledOnce).to.be.true;
      expect(view.updatePagination.calledOnce).to.be.true;
    });

  });

  describe('#willDestroyElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'clearExpandedSections', Em.K);
    });

    afterEach(function () {
      view.clearExpandedSections.restore();
    });

    it('should execute clearExpandedSections', function () {
      view.willDestroyElement();
      expect(view.clearExpandedSections.calledOnce).to.be.true;
    });

  });

  describe('#clearExpandedSections', function () {

    it('should clear expandedComponentsSections and expandedVersionsSections from controller', function () {
      view.get('controller').setProperties({
        expandedComponentsSections: [''],
        expandedVersionsSections: ['']
      });
      view.clearExpandedSections();
      expect(view.get('controller.expandedComponentsSections')).to.have.length(0);
      expect(view.get('controller.expandedVersionsSections')).to.have.length(0);
    });

  });

  describe('#HostView', function () {

    var hostView;

    beforeEach(function () {
      hostView = view.HostView.create({
        content: {
          hostName: null
        },
        controller: App.MainHostController.create()
      });
    });

    describe('#didInsertElement', function () {

      var cases = [
          {
            expandedSections: ['h0'],
            isCollapsed: false,
            title: '{0} section should be expanded'
          },
          {
            expandedSections: ['h1'],
            isCollapsed: true,
            title: '{0} section should be collapsed'
          }
        ],
        testMethod = function (item, elementsName, arrayName, propertyName) {
          it(item.title.format(elementsName), function () {
            hostView.set('content.hostName', 'h0');
            hostView.set('controller.' + arrayName, item.expandedSections);
            hostView.didInsertElement();
            expect(App.tooltip.calledOnce).to.be.true;
            expect(hostView.get(propertyName)).to.equal(item.isCollapsed);
          });
        };

      beforeEach(function () {
        sinon.stub(App, 'tooltip', Em.K);
      });

      afterEach(function () {
        App.tooltip.restore();
      });

      cases.forEach(function (item) {
        testMethod(item, 'components', 'expandedComponentsSections', 'isComponentsCollapsed');
        testMethod(item, 'versions', 'expandedVersionsSections', 'isVersionsCollapsed');
      });

    });

    describe('#toggleList', function () {

      var cases = [
        {
          isCollapsed: false,
          isCollapsedAfter: true,
          expandedSections: ['h0'],
          expandedSectionsAfter: [],
          title: 'section becomes collapsed'
        },
        {
          isCollapsed: true,
          isCollapsedAfter: false,
          expandedSections: [],
          expandedSectionsAfter: ['h0'],
          title: 'section becomes expanded'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          hostView.set('content.hostName', 'h0');
          hostView.set('isComponentsCollapsed', item.isCollapsed);
          hostView.set('controller.expandedComponentsSections', item.expandedSections);
          hostView.toggleList('isComponentsCollapsed', 'expandedComponentsSections');
          expect(hostView.get('isComponentsCollapsed')).to.equal(item.isCollapsedAfter);
          expect(hostView.get('controller.expandedComponentsSections')).to.eql(item.expandedSectionsAfter);
        });
      });

    });

    describe('#toggleComponents', function () {

      beforeEach(function () {
        sinon.stub(hostView, 'toggleList', Em.K);
      });

      afterEach(function () {
        hostView.toggleList.restore();
      });

      it('should toggle components list', function () {
        hostView.toggleComponents();
        expect(hostView.toggleList.calledOnce).to.be.true;
        expect(hostView.toggleList.calledWith('isComponentsCollapsed', 'expandedComponentsSections')).to.be.true;
      });

    });

    describe('#toggleVersions', function () {

      beforeEach(function () {
        sinon.stub(hostView, 'toggleList', Em.K);
      });

      afterEach(function () {
        hostView.toggleList.restore();
      });

      it('should toggle components list', function () {
        hostView.toggleVersions();
        expect(hostView.toggleList.calledOnce).to.be.true;
        expect(hostView.toggleList.calledWith('isVersionsCollapsed', 'expandedVersionsSections')).to.be.true;
      });

    });

  });

});
