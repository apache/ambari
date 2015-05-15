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
require('views/main/service/info/configs');

describe('App.MainServiceInfoConfigsView', function() {

  var view = App.MainServiceInfoConfigsView.create({
    controller: Em.Object.create()
  });

  describe('#updateComponentInformation', function() {

    var testCases = [
      {
        title: 'if components absent then counters should be 0',
        content: {
          restartRequiredHostsAndComponents: {}
        },
        result: {
          componentsCount: 0,
          hostsCount: 0
        }
      },
      {
        title: 'if host doesn\'t have components then hostsCount should be 1 and componentsCount should be 0',
        content: {
          restartRequiredHostsAndComponents: {
            host1: []
          }
        },
        result: {
          componentsCount: 0,
          hostsCount: 1
        }
      },
      {
        title: 'if host has 1 component then hostsCount should be 1 and componentsCount should be 1',
        content: {
          restartRequiredHostsAndComponents: {
            host1: [{}]
          }
        },
        result: {
          componentsCount: 1,
          hostsCount: 1
        }
      }
    ];
    testCases.forEach(function(test) {
      it(test.title, function() {
        view.set('controller.content', test.content);
        view.updateComponentInformation();
        expect(view.get('componentsCount')).to.equal(test.result.componentsCount);
        expect(view.get('hostsCount')).to.equal(test.result.hostsCount);
      });
    });
  });

});
