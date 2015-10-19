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

require('mixins/common/widgets/export_metrics_mixin');
var fileUtils = require('utils/file_utils');

describe('App.ExportMetricsMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.ExportMetricsMixin);
  });

  describe('#toggleFormatsList', function () {

    var cases = [
      {
        isExportMenuHidden: true,
        title: 'menu should be visible'
      },
      {
        isExportMenuHidden: false,
        title: 'menu should be hidden'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.set('isExportMenuHidden', !item.isExportMenuHidden);
        obj.toggleFormatsList();
        expect(obj.get('isExportMenuHidden')).to.equal(item.isExportMenuHidden);
      });
    });

  });

  describe('#exportGraphData', function () {

    var cases = [
      {
        isExportMenuHidden: true,
        title: 'menu should remain hidden'
      },
      {
        isExportMenuHidden: false,
        title: 'menu should become hidden'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.set('isExportMenuHidden', item.isExportMenuHidden);
        obj.exportGraphData();
        expect(obj.get('isExportMenuHidden')).to.be.true;
      });
    });

  });

  describe('#exportGraphDataSuccessCallback', function () {

    var cases = [
      {
        response: null,
        showAlertPopupCallCount: 1,
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        title: 'no response'
      },
      {
        response: {
          metrics: null
        },
        showAlertPopupCallCount: 1,
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        title: 'no metrics object in response'
      },
      {
        response: {
          metrics: {}
        },
        showAlertPopupCallCount: 1,
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 0,
        title: 'empty metrics object'
      },
      {
        response: {
          metrics: {
            m0: [0, 1]
          }
        },
        params: {
          isCSV: true
        },
        showAlertPopupCallCount: 0,
        prepareCSVCallCount: 1,
        prepareJSONCallCount: 0,
        downloadTextFileCallCount: 1,
        fileType: 'csv',
        fileName: 'data.csv',
        title: 'export to CSV'
      },
      {
        response: {
          metrics: {
            m0: [0, 1]
          }
        },
        params: {
          isCSV: false
        },
        showAlertPopupCallCount: 0,
        prepareCSVCallCount: 0,
        prepareJSONCallCount: 1,
        downloadTextFileCallCount: 1,
        fileType: 'json',
        fileName: 'data.json',
        title: 'export to JSON'
      }
    ];

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup', Em.K);
      sinon.stub(fileUtils, 'downloadTextFile', Em.K);
      sinon.stub(obj, 'prepareCSV', Em.K);
      sinon.stub(obj, 'prepareJSON', Em.K);
    });

    afterEach(function () {
      App.showAlertPopup.restore();
      fileUtils.downloadTextFile.restore();
      obj.prepareCSV.restore();
      obj.prepareJSON.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.exportGraphDataSuccessCallback(item.response, null, item.params);
        expect(obj.prepareCSV.callCount).to.equal(item.prepareCSVCallCount);
        expect(obj.prepareJSON.callCount).to.equal(item.prepareJSONCallCount);
        expect(fileUtils.downloadTextFile.callCount).to.equal(item.downloadTextFileCallCount);
        if (item.downloadTextFileCallCount) {
          expect(fileUtils.downloadTextFile.firstCall.args[1]).to.equal(item.fileType);
          expect(fileUtils.downloadTextFile.firstCall.args[2]).to.equal(item.fileName);
        }
      });
    });

  });

  describe('#exportGraphDataErrorCallback', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
    });

    afterEach(function () {
      App.ajax.defaultErrorHandler.restore();
    });

    it('should display error popup', function () {
      obj.exportGraphDataErrorCallback({
          status: 404
        }, null, '', {
          url: 'url',
          method: 'GET'
        });
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
      expect(App.ajax.defaultErrorHandler.calledWith({
          status: 404
        }, 'url', 'GET', 404)).to.be.true;
    });

  });

  describe('#setMetricsArrays', function () {

    var metrics = [],
      titles = [],
      data = {
        key0: {
          key1: {
            key2: [[0, 1], [2, 3]],
            key3: [[4, 5], [6, 7]]
          }
        }
      };

    it('should construct arrays with metrics info', function () {
      obj.setMetricsArrays(data, metrics, titles);
      expect(metrics).to.eql([[[0, 1], [2, 3]], [[4, 5], [6, 7]]]);
      expect(titles).to.eql(['key2', 'key3']);
    })

  });

  describe('#prepareCSV', function () {

    var cases = [
      {
        data: {
          metrics: {
            key0: [[0, 1], [2, 3]],
            key1: [[4, 1], [5, 3]]
          }
        },
        result: 'Timestamp,key0,key1\n1,0,4\n3,2,5\n',
        title: 'old style widget metrics'
      },
      {
        data: [
          {
            data: [[6, 7], [8, 9]]
          },
          {
            data: [[10, 7], [11, 9]]
          }
        ],
        result: 'Timestamp,,\n7,6,10\n9,8,11\n',
        title: 'enhanced widget metrics'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(obj.prepareCSV(item.data)).to.equal(item.result);
      });
    });

  });

  describe('#prepareJSON', function () {

    var cases = [
      {
        data: {
          metrics: {
            key0: [[0, 1], [2, 3]],
            key1: [[4, 1], [5, 3]]
          }
        },
        result: "{\"key0\":[[0,1],[2,3]],\"key1\":[[4,1],[5,3]]}",
        title: 'old style widget metrics'
      },
      {
        data: [
          {
            name: 'n0',
            data: [[6, 7], [8, 9]]
          },
          {
            name: 'n1',
            data: [[10, 7], [11, 9]]
          }
        ],
        result: "[{\"name\":\"n0\",\"data\":[[6,7],[8,9]]},{\"name\":\"n1\",\"data\":[[10,7],[11,9]]}]",
        title: 'enhanced widget metrics'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(obj.prepareJSON(item.data).replace(/\s/g, '')).to.equal(item.result);
      });
    });

  });

  describe('#hideMenuForNoData', function () {

    var cases = [
      {
        isExportButtonHidden: true,
        isExportMenuHidden: true,
        title: 'menu should be hidden'
      },
      {
        isExportButtonHidden: false,
        isExportMenuHidden: false,
        title: 'menu should be visible'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.setProperties({
          isExportButtonHidden: item.isExportButtonHidden,
          isExportMenuHidden: false
        });
        expect(obj.get('isExportMenuHidden')).to.equal(item.isExportMenuHidden);
      });
    });

  });

});
