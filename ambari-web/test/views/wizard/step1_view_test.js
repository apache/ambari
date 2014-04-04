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
require('views/wizard/step1_view');

describe('App.WizardStep1View', function () {
  var view = App.WizardStep1View.create({
    stacks: [],
    updateByCheckbox: function () {
    },
    editGroupLocalRepository: function () {
    },
    controller: Em.Object.create(),
    allRepoUnchecked: false
  });

  describe('#emptyRepoExist', function () {
    it('none repos', function () {
      view.set('allRepositoriesGroup', []);
      expect(view.get('emptyRepoExist')).to.equal(false);
    });
    it('one not empty repo', function () {
      view.set('allRepositoriesGroup', [
        {
          'empty-error': false
        }
      ]);
      expect(view.get('emptyRepoExist')).to.equal(false);
    });
    it('one empty repo', function () {
      view.set('allRepositoriesGroup', [
        {
          'empty-error': true
        }
      ]);
      expect(view.get('emptyRepoExist')).to.equal(true);
    });
  });

  describe('#invalidUrlExist', function () {
    var invalidUrlExistTestCases = [
      {
        title: 'if invalid count more than 0 and validation failed then invalid URL should exist',
        stacks: [
          Em.Object.create({
            isSelected: true,
            invalidCnt: 1
          })
        ],
        allRepositoriesGroup: [
          {
            'validation': 'icon-exclamation-sign'
          }
        ],
        result: true
      },
      {
        title: 'if invalid count equal 0 and validation failed then invalid URL shouldn\'t exist',
        stacks: [
          Em.Object.create({
            isSelected: true,
            invalidCnt: 0
          })
        ],
        allRepositoriesGroup: [
          {
            'validation': 'icon-exclamation-sign'
          }
        ],
        result: false
      },
      {
        title: 'if invalid count more than 0 and validation passed then invalid URL shouldn\'t exist',
        stacks: [
          Em.Object.create({
            isSelected: true,
            invalidCnt: 1
          })
        ],
        allRepositoriesGroup: [
          {
            'validation': 'icon-success'
          }
        ],
        result: false
      },
      {
        title: 'if invalid count equal 0 and validation passed then invalid URL shouldn\'t exist',
        stacks: [
          Em.Object.create({
            isSelected: true,
            invalidCnt: 0
          })
        ],
        allRepositoriesGroup: [
          {
            'validation': 'icon-success'
          }
        ],
        result: false
      }
    ];

    invalidUrlExistTestCases.forEach(function (test) {
      it(test.title, function () {
        view.get('controller').set('content', {stacks: test.stacks});
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('invalidUrlExist')).to.equal(test.result);
      });
    });
  });
  describe('#totalErrorCnt', function () {
    var totalErrorCntTestCases = [
      {
        title: 'if allRepoUnchecked is true then totalErrorCnt should be 1',
        allRepoUnchecked: true,
        allRepositoriesGroup: [
          {
            'empty-error': true,
            'validation': 'icon-exclamation-sign'
          }
        ],
        result: 1
      },
      {
        title: 'if validation passed successfully then totalErrorCnt should be 0',
        allRepoUnchecked: false,
        allRepositoriesGroup: [
          {
            'empty-error': false,
            'validation': 'icon-success'
          }
        ],
        result: 0
      },
      {
        title: 'if empty-error is true then totalErrorCnt should be 1',
        allRepoUnchecked: false,
        allRepositoriesGroup: [
          {
            'empty-error': true,
            'validation': 'icon-success'
          }
        ],
        result: 1
      },
      {
        title: 'if validation failed then totalErrorCnt should be 1',
        allRepoUnchecked: false,
        allRepositoriesGroup: [
          {
            'empty-error': false,
            'validation': 'icon-exclamation-sign'
          }
        ],
        result: 1
      },
      {
        title: 'if validation failed and empty-error is true then totalErrorCnt should be 2',
        allRepoUnchecked: false,
        allRepositoriesGroup: [
          {
            'empty-error': true,
            'validation': 'icon-exclamation-sign'
          }
        ],
        result: 2
      }
    ];

    totalErrorCntTestCases.forEach(function (test) {
      it(test.title, function () {
        view.set('allRepoUnchecked', test.allRepoUnchecked);
        view.set('allRepositoriesGroup', test.allRepositoriesGroup);
        expect(view.get('totalErrorCnt')).to.equal(test.result);
      });
    });
  });
});
