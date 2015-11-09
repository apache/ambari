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

require('utils/ember_reopen');

describe('Ember functionality extension', function () {

  describe('#Em.View', function () {

    var view,
      cases = [
        {
          result: {
            p0: 'v3',
            p1: 'v4',
            p2: 'v5'
          },
          title: 'active view'
        },
        {
          result: {
            p0: 'v0',
            p1: 'v1',
            p2: 'v2'
          },
          propertyToSet: 'isDestroyed',
          title: 'destroyed view'
        },
        {
          result: {
            p0: 'v0',
            p1: 'v1',
            p2: 'v2'
          },
          propertyToSet: 'isDestroying',
          title: 'view being destroyed'
        }
      ];

    beforeEach(function () {
      view = Em.View.create({
        isDestroyed: false,
        isDestroying: false,
        p0: 'v0',
        p1: 'v1',
        p2: 'v2'
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        if (item.propertyToSet) {
          view.set(item.propertyToSet, true);
        }
        view.set('p0', 'v3');
        view.setProperties({
          p1: 'v4',
          p2: 'v5'
        });
        expect(view.getProperties(['p0', 'p1', 'p2'])).to.eql(item.result);
      });
    });

  });

});
