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
require('views/main/admin/stack_upgrade/upgrade_group_view');

describe('App.upgradeGroupView', function () {
  var view = App.upgradeGroupView.create({
    content: Em.Object.create({})
  });

  describe.skip("#isFailed", function () {
    var testCases = [
      {
        data: {
          failedItem: undefined,
          status: 'COMPLETED'
        },
        result: false
      },
      {
        data: {
          failedItem: true,
          status: 'COMPLETED'
        },
        result: false
      },
      {
        data: {
          failedItem: undefined,
          status: 'FAILED'
        },
        result: false
      },
      {
        data: {
          failedItem: true,
          status: 'FAILED'
        },
        result: true
      }
    ];
    beforeEach(function () {
      this.mock = sinon.stub(view, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });
    testCases.forEach(function (test) {
      it('failedItem - ' + test.data.failedItem + ', status - ' + test.data.status, function () {
        view.get.withArgs('content.status').returns(test.data.status);
        view.get.withArgs('failedItem').returns(test.data.failedItem);
        view.propertyDidChange('isFailed');
        expect(view.get('isFailed')).to.equal(test.result);
      });
    });
  });
});