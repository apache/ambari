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
require('views/common/widget/graph_widget_view');

describe('App.GraphWidgetView', function () {
  var view = App.GraphWidgetView.create();

  describe("#adjustData()", function() {
    var testCases = [
      {
        title: 'empty data',
        data: {
          dataLinks: {},
          dataLength: 0
        },
        result: {}
      },
      {
        title: 'correct data',
        data: {
          dataLinks: {
            s1: [[0, 0]]
          },
          dataLength: 1
        },
        result:  {
          s1: [[0, 0]]
        }
      },
      {
        title: 'second series empty',
        data: {
          dataLinks: {
            s1: [[1, 0]],
            s2: []
          },
          dataLength: 1
        },
        result:  {
          s1: [[1, 0]],
          s2: [[null, 0]]
        }
      },
      {
        title: 'second series missing data at the end',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 0]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[1, 0], [null, 1], [null, 2]]
        }
      },
      {
        title: 'second series missing data at the beginning',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[3, 2]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [null, 1], [3, 2]]
        }
      },
      {
        title: 'second series missing data in the middle',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 1]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [1, 1], [null, 2]]
        }
      },
      {
        title: 'second and third series missing data',
        data: {
          dataLinks: {
            s1: [[1, 0], [2, 1], [3, 2]],
            s2: [[1, 1]],
            s3: [[1, 2]]
          },
          dataLength: 3
        },
        result:  {
          s1: [[1, 0], [2, 1], [3, 2]],
          s2: [[null, 0], [1, 1], [null, 2]],
          s3: [[null, 0], [null, 1], [1, 2]]
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        view.adjustData(test.data.dataLinks, test.data.dataLength);
        expect(test.data.dataLinks).to.eql(test.result);
      });
    });
  });

});