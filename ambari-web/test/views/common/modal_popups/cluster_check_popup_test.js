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
require('views/common/modal_popups/cluster_check_popup');

describe('App.showClusterCheckPopup', function () {

  var isCallbackExecuted,
    callback = function () {
      isCallbackExecuted = true;
    },
    cases = [
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'p0',
                  status: 'PASS'
                }
              },
              {
                UpgradeChecks: {
                  id: 'p1',
                  status: 'PASS'
                }
              }
            ]
          }
        },
        result: {
          primary: Em.I18n.t('common.proceedAnyway'),
          secondary: Em.I18n.t('common.cancel'),
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [],
          warnings: [],
          hasConfigsMergeConflicts: false,
          isAllPassed: true
        },
        isCallbackExecuted: false,
        title: 'no fails, no warnings, no popup customization'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'w0',
                  status: 'WARNING'
                }
              },
              {
                UpgradeChecks: {
                  id: 'w1',
                  status: 'WARNING'
                }
              }
            ]
          },
          popup: {
            header: 'checks',
            failTitle: 'fail',
            failAlert: 'something has failed',
            warningTitle: 'warning',
            warningAlert: 'something is not good',
            callback: callback
          }
        },
        result: {
          primary: Em.I18n.t('common.proceedAnyway'),
          secondary: Em.I18n.t('common.cancel'),
          header: 'checks'
        },
        bodyResult: {
          failTitle: 'fail',
          failAlert: 'something has failed',
          warningTitle: 'warning',
          warningAlert: 'something is not good',
          fails: [],
          warnings: [
            {
              UpgradeChecks: {
                id: 'w0',
                status: 'WARNING'
              }
            },
            {
              UpgradeChecks: {
                id: 'w1',
                status: 'WARNING'
              }
            }
          ],
          hasConfigsMergeConflicts: false,
          isAllPassed: false
        },
        isCallbackExecuted: true,
        title: 'no fails, default buttons, callback executed'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'f0',
                  status: 'FAIL'
                }
              },
              {
                UpgradeChecks: {
                  id: 'f1',
                  status: 'FAIL'
                }
              }
            ]
          },
          popup: {
            callback: callback,
            noCallbackCondition: true
          }
        },
        result: {
          primary: Em.I18n.t('common.dismiss'),
          secondary: false,
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [
            {
              UpgradeChecks: {
                id: 'f0',
                status: 'FAIL'
              }
            },
            {
              UpgradeChecks: {
                id: 'f1',
                status: 'FAIL'
              }
            }
          ],
          warnings: [],
          hasConfigsMergeConflicts: false,
          isAllPassed: false
        },
        isCallbackExecuted: false,
        title: 'fails detected, default buttons, callback not executed'
      },
      {
        inputData: {
          data: {
            items: [
              {
                UpgradeChecks: {
                  id: 'p0',
                  status: 'PASS'
                }
              },
              {
                UpgradeChecks: {
                  id: 'p1',
                  status: 'PASS'
                }
              }
            ]
          },
          popup: {
            primary: 'ok',
            secondary: 'cancel'
          },
          configs: [
            {
              name: 'c0'
            },
            {
              name: 'c1'
            }
          ],
          upgradeVersion: 'HDP-2.3.0.0'
        },
        result: {
          primary: 'ok',
          secondary: 'cancel',
          header: '&nbsp;'
        },
        bodyResult: {
          failTitle: undefined,
          failAlert: undefined,
          warningTitle: undefined,
          warningAlert: undefined,
          fails: [],
          warnings: [],
          hasConfigsMergeConflicts: true,
          isAllPassed: false
        },
        configsResult: [
          {
            name: 'c0'
          },
          {
            name: 'c1'
          }
        ],
        isCallbackExecuted: false,
        title: 'configs merge conflicts detected, custom buttons'
      }
    ];

  beforeEach(function () {
    isCallbackExecuted = false;
    sinon.stub(App, 'tooltip', Em.K);
  });

  afterEach(function () {
    App.tooltip.restore();
  });

  cases.forEach(function (item) {
    it(item.title, function () {
      var popup = App.showClusterCheckPopup(item.inputData.data, item.inputData.popup, item.inputData.configs, item.inputData.upgradeVersion),
        popupBody = popup.bodyClass.create();
      popup.onPrimary();
      Em.keys(item.result).forEach(function (key) {
        expect(popup[key]).to.equal(item.result[key]);
      });
      Em.keys(item.bodyResult).forEach(function (key) {
        expect(popupBody[key]).to.eql(item.bodyResult[key]);
      });
      expect(isCallbackExecuted).to.equal(item.isCallbackExecuted);
      if (item.bodyResult.hasConfigsMergeConflicts) {
        var configsMergeTable = popupBody.configsMergeTable.create();
        configsMergeTable.didInsertElement();
        expect(configsMergeTable.configs).to.eql(item.configsResult);
        expect(App.tooltip.calledOnce).to.be.true;
        expect(App.tooltip.firstCall.args[1].title).to.equal(item.inputData.upgradeVersion);
      } else {
        expect(App.tooltip.calledOnce).to.be.false;
      }
    });
  });

});
