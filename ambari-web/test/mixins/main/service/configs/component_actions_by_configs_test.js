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

require('mixins/main/service/configs/component_actions_by_configs');

var mixin;

describe('App.ComponentActionsByConfigs', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.ComponentActionsByConfigs, {
      content: Em.Object.create()
    });
  });

  describe('#showPopup', function () {

    var testCases = [
        {
          configActions: [],
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no config actions'
        },
        {
          configActions: [
            {
              actionType: 'none'
            },
            {
              actionType: null
            },
            {}
          ],
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no popup config actions'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'f0'
            })
          ],
          mixinProperties: {
            allConfigs: [
              Em.Object.create({
                filename: 'f1',
                value: 0,
                initialValue: 1
              })
            ]
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no associated configs'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'f2'
            })
          ],
          mixinProperties: {
            allConfigs: [
              Em.Object.create({
                filename: 'f2',
                value: 0,
                initialValue: 0
              })
            ]
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no changes in associated configs'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'f3'
            })
          ],
          mixinProperties: {
            allConfigs: [
              Em.Object.create({
                filename: 'f3',
                value: 0,
                initialValue: 1
              })
            ]
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no capacity-scheduler actions defined'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'capacity-scheduler.xml'
            })
          ],
          mixinProperties: {
            allConfigs: [
              Em.Object.create({
                filename: 'capacity-scheduler.xml',
                value: 0,
                initialValue: 1
              })
            ],
            isYarnQueueRefreshed: true
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'YARN queue refreshed'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'capacity-scheduler.xml'
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'RESOURCEMANAGER'
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: false
            }),
            Em.Object.create({
              componentName: 'COMPONENT',
              isRunning: true
            })
          ],
          mixinProperties: {
            allConfigs: [
              Em.Object.create({
                filename: 'capacity-scheduler.xml',
                value: 0,
                initialValue: 1
              })
            ],
            isYarnQueueRefreshed: false
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'no ResourceManagers running'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'capacity-scheduler.xml'
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'RESOURCEMANAGER'
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: false
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: true
            }),
            Em.Object.create({
              componentName: 'HIVE_SERVER_INTERACTIVE'
            })
          ],
          mixinProperties: {
            'allConfigs': [
              Em.Object.create({
                filename: 'capacity-scheduler.xml',
                value: 0,
                initialValue: 1
              })
            ],
            'isYarnQueueRefreshed': false,
            'content.serviceName': 'HIVE'
          },
          popupPrimaryButtonCallbackCallCount: 1,
          showHsiRestartPopupCallCount: 1,
          showConfirmationPopupCallCount: 0,
          title: 'change from Hive page, Hive Server Interactive present'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'capacity-scheduler.xml'
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'RESOURCEMANAGER'
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: false
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: true
            })
          ],
          mixinProperties: {
            'allConfigs': [
              Em.Object.create({
                filename: 'capacity-scheduler.xml',
                value: 0,
                initialValue: 1
              })
            ],
            'isYarnQueueRefreshed': false,
            'content.serviceName': 'HIVE'
          },
          popupPrimaryButtonCallbackCallCount: 1,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 0,
          title: 'change from Hive page, no Hive Server Interactive'
        },
        {
          configActions: [
            Em.Object.create({
              actionType: 'showPopup',
              fileName: 'capacity-scheduler.xml',
              popupProperties: {
                primaryButton: {}
              }
            })
          ],
          hostComponents: [
            Em.Object.create({
              componentName: 'RESOURCEMANAGER'
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: false
            }),
            Em.Object.create({
              componentName: 'RESOURCEMANAGER',
              isRunning: true
            })
          ],
          mixinProperties: {
            'allConfigs': [
              Em.Object.create({
                filename: 'capacity-scheduler.xml',
                value: 0,
                initialValue: 1
              })
            ],
            'isYarnQueueRefreshed': false,
            'content.serviceName': 'YARN'
          },
          popupPrimaryButtonCallbackCallCount: 0,
          showHsiRestartPopupCallCount: 0,
          showConfirmationPopupCallCount: 1,
          title: 'change from YARN page'
        }
      ];

    testCases.forEach(function (test) {

      describe(test.title, function () {

        beforeEach(function () {
          sinon.stub(App.ConfigAction, 'find').returns(test.configActions);
          sinon.stub(App.HostComponent, 'find').returns(test.hostComponents || []);
          sinon.stub(mixin, 'popupPrimaryButtonCallback', Em.K);
          sinon.stub(mixin, 'showHsiRestartPopup', Em.K);
          sinon.stub(App, 'showConfirmationPopup', Em.K);
          mixin.setProperties(test.mixinProperties);
          mixin.showPopup();
        });

        afterEach(function () {
          App.ConfigAction.find.restore();
          App.HostComponent.find.restore();
          mixin.popupPrimaryButtonCallback.restore();
          mixin.showHsiRestartPopup.restore();
          App.showConfirmationPopup.restore();
        });

        it('popup callback', function () {
          expect(mixin.popupPrimaryButtonCallback.callCount).to.eql(test.popupPrimaryButtonCallbackCallCount);
        });

        it('HSI restart popup', function () {
          expect(mixin.showHsiRestartPopup.callCount).to.eql(test.showHsiRestartPopupCallCount);
        });

        it('confirmation popup', function () {
          expect(App.showConfirmationPopup.callCount).to.eql(test.showConfirmationPopupCallCount);
        });

      });

    });

  });

});