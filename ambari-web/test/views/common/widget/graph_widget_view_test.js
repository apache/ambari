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
var fileUtils = require('utils/file_utils');

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

  describe('#exportGraphData', function () {

    var cases = [
      {
        data: null,
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'no data'
      },
      {
        data: {},
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'invalid data'
      },
      {
        data: [
          {
            data: null
          }
        ],
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'empty data'
      },
      {
        data: [
          {
            data: {}
          }
        ],
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        showAlertPopupCallCount: 1,
        title: 'malformed data'
      },
      {
        data: [
          {
            data: [
              {
                key: 'value'
              }
            ]
          }
        ],
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 1,
        downloadTextFileCallCount: 1,
        showAlertPopupCallCount: 0,
        title: 'JSON export'
      },
      {
        data: [
          {
            data: [
              {
                key: 'value'
              }
            ]
          }
        ],
        event: {
          context: true
        },
        prepareCSVCallCount: 1,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 1,
        showAlertPopupCallCount: 0,
        title: 'CSV export'
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'prepareCSV').returns([]);
      sinon.stub(view, 'prepareJSON').returns([]);
      sinon.stub(fileUtils, 'downloadTextFile', Em.K);
      sinon.stub(App, 'showAlertPopup', Em.K);
    });

    afterEach(function () {
      view.prepareCSV.restore();
      view.prepareJSON.restore();
      fileUtils.downloadTextFile.restore();
      App.showAlertPopup.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('data', item.data);
        view.exportGraphData(item.event || {});
        expect(view.prepareCSV.callCount).to.equal(item.prepareCSVCallCount);
        expect(view.prepareJSON.callCount).to.equal(item.prepareJSONCallCount);
        expect(fileUtils.downloadTextFile.callCount).to.equal(item.downloadTextFileCallCount);
        expect(App.showAlertPopup.callCount).to.equal(item.showAlertPopupCallCount);
        if (item.downloadTextFileCallCount) {
          var fileType = item.event && item.event.context ? 'csv' : 'json';
          expect(fileUtils.downloadTextFile.calledWith([], fileType, 'data.' + fileType)).to.be.true;
        }
      });
    });

  });

});