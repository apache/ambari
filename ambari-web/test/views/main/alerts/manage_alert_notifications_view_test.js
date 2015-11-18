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
	 isOperator: false,
        selectedAlertNotification: {id: 1},
        m: 'some alert notification is selected and user is an admin',
        p: {
          isAddButtonDisabled: true,
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        },
        e:  {
          isAddButtonDisabled: false,
          isEditButtonDisabled: false,
          isRemoveButtonDisabled: false,
          isDuplicateButtonDisabled: false
        }
      },
      {
        isOperator: true,
        selectedAlertNotification: {id: 1},
        m: 'some alert notification is selected and user is a non-admin operator',
        p: {
          isAddButtonDisabled: true,
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        },
        e:  {
          isAddButtonDisabled: true,
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        }
      },
      {
        isOperator: false,
        selectedAlertNotification: null,
        m: 'some alert notification is not selected and user is an admin',
        p: {
          isAddButtonDisabled: true,
          isEditButtonDisabled: false,
          isRemoveButtonDisabled: false,
          isDuplicateButtonDisabled: false
        },
        e:  {
          isAddButtonDisabled: true,
          isEditButtonDisabled: true,
          isRemoveButtonDisabled: true,
          isDuplicateButtonDisabled: true
        }
      },
      {
        isOperator: true,
        selectedAlertNotification: null,        
        m: 'some alert notification is not selected and user is a non-admin operator',
        p: {
	   isAddButtonDisabled: true,
          isEditButtonDisabled: false,
          isRemoveButtonDisabled: false,
          isDuplicateButtonDisabled: false
        },
        e:  {
          isAddButtonDisabled: true,
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
          App.isOperator=test.isOperator;
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
