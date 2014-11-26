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

describe('App.ManageAlertNotificationsView', function () {

  beforeEach(function () {

    view = App.ManageAlertNotificationsView.create({
      controller: Em.Object.create()
    });

  });

  describe('#buttonObserver', function () {

    Em.A([
      {
        selectedAlertNotification: {id: 1},
        m: 'some alert notification is selected',
        p: {
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        },
        e:  {
          isEditButtonDisabled: false,
          isRemoveButtonDisabled: false,
          isDuplicateButtonDisabled: false
        }
      },
      {
        selectedAlertNotification: null,
        m: 'some alert notification is not selected',
        p: {
          isEditButtonDisabled: false,
          isRemoveButtonDisabled: false,
          isDuplicateButtonDisabled: false
        },
        e:  {
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        }
      }
    ]).forEach(function (test) {
        it(test.m, function () {
          Em.keys(test.p).forEach(function (k) {
            view.set(k, test.p[k]);
          });
          view.set('controller.selectedAlertNotification', test.selectedAlertNotification);
          view.buttonObserver();
          Em.keys(test.e).forEach(function (k) {
            expect(view.get(k)).to.equal(test.e[k]);
          });
        });
      });

  });

  describe('#showEmailDetails', function () {

    Em.A([
      {
        selectedAlertNotification: {type: 'SNMP'},
        e: false
      },
      {
        selectedAlertNotification: {type: 'EMAIL'},
        e: true
      }
    ]).forEach(function (test, i) {
        it('test ' + (i + 1), function () {
          view.set('controller.selectedAlertNotification', test.selectedAlertNotification);
          expect(view.get('showEmailDetails')).to.equal(test.e);
        });
      });

  });

  describe('#showSNMPDetails', function () {

    Em.A([
        {
          selectedAlertNotification: {type: 'SNMP'},
          e: true
        },
        {
          selectedAlertNotification: {type: 'EMAIL'},
          e: false
        }
      ]).forEach(function (test, i) {
        it('test ' + (i + 1), function () {
          view.set('controller.selectedAlertNotification', test.selectedAlertNotification);
          expect(view.get('showSNMPDetails')).to.equal(test.e);
        });
      });

  });

});
