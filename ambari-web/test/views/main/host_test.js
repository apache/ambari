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

    describe('#displayComponents', function () {

      var cases = [
        {
          hostComponents: [
            {
              displayName: 'c0'
            },
            {
              displayName: 'c1'
            }
          ],
          showHostsTableListPopupCallCount: 1,
          title: 'should display host components in modal popup'
        },
        {
          hostComponents: [],
          showHostsTableListPopupCallCount: 0,
          title: 'should not display modal popup'
        }
      ];

      beforeEach(function () {
        sinon.stub(App, 'showHostsTableListPopup', Em.K);
      });

      afterEach(function () {
        App.showHostsTableListPopup.restore();
      });

      cases.forEach(function (item) {
        it(item.title, function () {
          hostView.set('content', {
            hostName: 'h',
            hostComponents: item.hostComponents
          });
          hostView.displayComponents();
          expect(App.showHostsTableListPopup.callCount).to.equal(item.showHostsTableListPopupCallCount);
          if (item.showHostsTableListPopupCallCount) {
            expect(App.showHostsTableListPopup.calledWith(Em.I18n.t('common.components'), 'h', ['c0', 'c1'])).to.be.true;
          }
        });
      });

    });

    describe('#displayVersions', function () {

      var cases = [
        {
          stackVersions: [
            Em.Object.create({
              displayName: 'v0',
              status: 'CURRENT',
              isVisible: true
            }),
            Em.Object.create({
              displayName: 'v1',
              status: 'OUT_OF_SYNC',
              isVisible: true
            }),
            Em.Object.create({
              displayName: 'v2',
              status: 'INSTALL_FAILED',
              isVisible: false
            })
          ],
          showHostsTableListPopupCallCount: 1,
          title: 'should display stack versions in modal popup'
        },
        {
          stackVersions: [
            Em.Object.create({
              displayName: 'v0',
              status: 'CURRENT',
              isVisible: true
            }),
            Em.Object.create({
              displayName: 'v2',
              status: 'INSTALL_FAILED',
              isVisible: false
            })
          ],
          showHostsTableListPopupCallCount: 0,
          title: 'should not display modal popup if there\'s only one visible stack version'
        },
        {
          stackVersions: [
            Em.Object.create({
              displayName: 'v2',
              status: 'INSTALL_FAILED',
              isVisible: false
            })
          ],
          showHostsTableListPopupCallCount: 0,
          title: 'should not display modal popup if there are no visible stack versions available'
        },
        {
          stackVersions: [],
          showHostsTableListPopupCallCount: 0,
          title: 'should not display modal popup if there are no stack versions available'
        }
      ];

      beforeEach(function () {
        sinon.stub(App, 'showHostsTableListPopup', Em.K);
      });

      afterEach(function () {
        App.showHostsTableListPopup.restore();
      });

      cases.forEach(function (item) {
        it('should display stack versions in modal popup', function () {
          hostView.set('content', {
            hostName: 'h',
            stackVersions: item.stackVersions
          });
          hostView.displayVersions();
          expect(App.showHostsTableListPopup.callCount).to.equal(item.showHostsTableListPopupCallCount);
          if (item.showHostsTableListPopupCallCount) {
            expect(App.showHostsTableListPopup.calledWith(Em.I18n.t('common.versions'), 'h', [
              {
                name: 'v0',
                status: 'Current'
              },
              {
                name: 'v1',
                status: 'Out Of Sync'
              }
            ])).to.be.true;
          }
        });
      });

    });

    describe('#currentVersion', function () {

      var cases = [
        {
          stackVersions: [
            Em.Object.create({
              displayName: 'HDP-2.2.0.0-2000'
            }),
            Em.Object.create({
              displayName: 'HDP-2.2.0.0-1000',
              isCurrent: true
            })
          ],
          currentVersion: 'HDP-2.2.0.0-1000',
          title: 'current version specified explicitly'
        },
        {
          stackVersions: [
            Em.Object.create({
              displayName: 'HDP-2.2.0.0-2000'
            }),
            Em.Object.create({
              displayName: 'HDP-2.3.0.0-1000'
            })
          ],
          currentVersion: 'HDP-2.2.0.0-2000',
          title: 'current version not specified explicitly'
        },
        {
          stackVersions: [Em.Object.create()],
          currentVersion: undefined,
          title: 'version display name isn\'t defined'
        },
        {
          stackVersions: [null],
          currentVersion: '',
          title: 'no version data available'
        },
        {
          stackVersions: [],
          currentVersion: '',
          title: 'no versions available'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          hostView.set('content.stackVersions', item.stackVersions);
          expect(hostView.get('currentVersion')).to.equal(item.currentVersion);
        });
      });

    });

  });

});
