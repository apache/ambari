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
require('controllers/main/admin/serviceAccounts_controller');


describe('App.MainAdminServiceAccountsController', function () {

  var controller = App.MainAdminServiceAccountsController.create();

  describe('#setContentProperty()', function () {
    var testCases = [
      {
        title: 'key is null',
        content: {
          key: null,
          configName: 'cc',
          miscConfigs: []
        },
        result: {
          output: false,
          configValue: 'test'
        }
      },
      {
        title: 'configName is null',
        content: {
          key: 'key',
          configName: null,
          miscConfigs: []
        },
        result: {
          output: false,
          configValue: 'test'
        }
      },
      {
        title: 'misc configs array doesn\'t contain such a config',
        content: {
          key: 'key',
          configName: 'config1',
          miscConfigs: []
        },
        result: {
          output: false,
          configValue: 'test'
        }
      },
      {
        title: 'content doesn\'t contain such a key',
        content: {
          key: 'key',
          configName: 'config1',
          miscConfigs: [
            Em.Object.create({
              name: 'test_key'
            })
          ]
        },
        result: {
          output: false,
          configValue: 'test'
        }
      },
      {
        title: 'content property match config',
        content: {
          key: 'testKey',
          configName: 'test_key',
          miscConfigs: [
            Em.Object.create({
              name: 'test_key',
              value: 'testValue'
            })
          ]
        },
        result: {
          output: true,
          configValue: 'testValue'
        }
      }
    ];
    controller.set('content', Em.Object.create({testKey: 'test'}));
    testCases.forEach(function (test) {
      it(test.title, function () {
        var content = controller.get('content');
        expect(controller.setContentProperty(test.content.key, test.content.configName, test.content.miscConfigs)).to.equal(test.result.output);
        expect(content.get('testKey')).to.equal(test.result.configValue);
      });
    });
  });

  describe('#sortByOrder()', function () {
    var testCases = [
      {
        title: 'sortOrder is null',
        content: {
          sortOrder: null,
          arrayToSort: [
            {
              name: 'one',
              displayName: 'one'
            }
          ]
        },
        result: ['one']
      },
      {
        title: 'sortOrder is empty',
        content: {
          sortOrder: [],
          arrayToSort: [
            {
              name: 'one',
              displayName: 'one'
            }
          ]
        },
        result: ['one']
      },
      {
        title: 'sortOrder items don\'t match items of array',
        content: {
          sortOrder: ['one'],
          arrayToSort: [
            {name: 'two'}
          ]
        },
        result: []
      },
      {
        title: 'sort items in reverse order',
        content: {
          sortOrder: ['two', 'one'],
          arrayToSort: [
            Em.Object.create({
              name: 'one',
              displayName: 'one'
            }),
            Em.Object.create({
              name: 'two',
              displayName: 'two'
            })
          ]
        },
        result: ['two', 'one']
      },
      {
        title: 'sort items in correct order',
        content: {
          sortOrder: ['one', 'two'],
          arrayToSort: [
            Em.Object.create({
              name: 'one',
              displayName: 'one'
            }),
            Em.Object.create({
              name: 'two',
              displayName: 'two'
            })
          ]
        },
        result: ['one', 'two']
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.sortByOrder(test.content.sortOrder, test.content.arrayToSort).mapProperty('displayName')).to.eql(test.result);
      });
    });
  });
});
