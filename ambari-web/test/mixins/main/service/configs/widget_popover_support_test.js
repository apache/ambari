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

describe('App.WidgetPopoverSupport', function () {

  beforeEach(function () {
    view = Em.View.create(App.WidgetPopoverSupport, {});
  });

  describe.skip('#isWidgetInTheRightColumn', function () {

    Em.A([
        {
          v: {
            section: {
              columnIndex: 1,
              columnSpan: 2,
              sectionColumns: 3
            },
            tab: {
              columns: 3
            },
            subSection: {
              columnSpan: 1,
              columnIndex: 2
            }
          },
          e: true
        },
        {
          v: {
            section: {
              columnIndex: 1,
              columnSpan: 2,
              sectionColumns: 4
            },
            tab: {
              columns: 4
            },
            subSection: {
              columnSpan: 1,
              columnIndex: 2
            }
          },
          e: false
        },
        {
          v: {
            section: {
              columnIndex: 1,
              columnSpan: 2,
              sectionColumns: 3
            },
            tab: {
              columns: 4
            },
            subSection: {
              columnSpan: 1,
              columnIndex: 3
            }
          },
          e: false
        }
    ]).forEach(function (test, index) {
        it('test #' + index, function () {
          view.setProperties(test.v);
          expect(view.get('isWidgetInTheRightColumn')).to.equal(test.e);
        });
      });

  });

});