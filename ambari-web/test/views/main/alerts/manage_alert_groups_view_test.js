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

var view;

describe('App.MainAlertsManageAlertGroupView', function () {

  beforeEach(function () {

    view = App.MainAlertsManageAlertGroupView.create({
      controller: Em.Object.create()
    });

  });

  it('#buttonObserver', function () {

    Em.A([
      {
        p: {
          isRemoveButtonDisabled: false,
          isRenameButtonDisabled: false,
          isDuplicateButtonDisabled: true
        },
        selectedAlertGroup: {default: true},
        m: 'selected alert group is default',
        e: {
          isRemoveButtonDisabled: true,
          isRenameButtonDisabled: true,
          isDuplicateButtonDisabled: false
        }
      },
      {
        p: {
          isRemoveButtonDisabled: true,
          isRenameButtonDisabled: true,
          isDuplicateButtonDisabled: true
        },
        selectedAlertGroup: {default: false},
        m: 'selected alert group is not default',
        e: {
          isRemoveButtonDisabled: false,
          isRenameButtonDisabled: false,
          isDuplicateButtonDisabled: false
        }
      },
      {
        p: {
          isRemoveButtonDisabled: true,
          isRenameButtonDisabled: true,
          isDuplicateButtonDisabled: true
        },
        selectedAlertGroup: null,
        m: 'not one alert group is selected',
        e: {
          isRemoveButtonDisabled: false,
          isRenameButtonDisabled: false,
          isDuplicateButtonDisabled: false
        }
      }
    ]).forEach(function (test) {
        it(test.m, function () {
          Em.keys(test.p).forEach(function (k) {
            view.set(k, test.p[k]);
          });
          view.set('controller.selectedAlertGroup', test.selectedAlertGroup);
          Em.keys(test.e).forEach(function (k) {
            expect(view.get(k)).to.equal(test.e[k]);
          });
        });
      });

  });

});
