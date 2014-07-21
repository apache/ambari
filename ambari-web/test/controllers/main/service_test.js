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
require('controllers/main/service');

var mainServiceController;

describe('App.MainServiceController', function () {

  var tests = Em.A([
    {
      isStartStopAllClicked: true,
      content: Em.A([
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HIVE',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HDFS',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'TEZ',
          isClientsOnly: true
        })
      ]),
      eStart: true,
      eStop: true,
      mStart: 'mainServiceController StartAll is Disabled 2',
      mStop: 'mainServiceController StopAll is Disabled 2'
    },
    {
      isStartStopAllClicked: false,
      content: Em.A([
        Em.Object.create({
          healthStatus: 'green',
          serviceName: 'HIVE',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HDFS',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'TEZ',
          isClientsOnly: true
        })
      ]),
      eStart: false,
      eStop: false,
      mStart: 'mainServiceController StartAll is Enabled 3',
      mStop: 'mainServiceController StopAll is Enabled 3'
    }

  ]);
  describe('#isStartAllDisabled', function () {
    tests.forEach(function (test) {
      it(test.mStart, function () {
        mainServiceController = App.MainServiceController.create({
          content: test.content,
          isStartStopAllClicked: test.isStartStopAllClicked
        });
        expect(mainServiceController.get('isStartAllDisabled')).to.equals(test.eStart);
      });
    });
  });

  describe('#isStopAllDisabled', function () {
    tests.forEach(function (test) {
      it(test.mStop, function () {
        mainServiceController = App.MainServiceController.create({
          content: test.content,
          isStartStopAllClicked: test.isStartStopAllClicked
        });
        expect(mainServiceController.get('isStopAllDisabled')).to.equals(test.eStop);
      });
    });
  });
});
