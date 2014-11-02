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

describe('App.InstallerController', function () {

  describe('#getServerVersionSuccessCallback', function () {

    var controller = App.MainController.create(),
      cases = [
        {
          osType: 'redhat5',
          expected: false
        },
        {
          osType: 'redhat6',
          expected: true
        },
        {
          osType: 'sles11',
          expected: false
        }
      ],
      title = 'App.isManagedMySQLForHiveEnabled should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osType), function () {
        controller.getServerVersionSuccessCallback({
          'RootServiceComponents': {
            'component_version': '',
            'properties': {
              'server.os_type': item.osType
            }
          }
        });
        expect(App.get('isManagedMySQLForHiveEnabled')).to.equal(item.expected);
      });
    });

  });

});
