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
require('views/main/jobs/hive_job_details_view');

describe('App.MainHiveJobDetailsView', function () {
  var mainHiveJobDetailsView = App.MainHiveJobDetailsView.create({
    controller: App.MainHiveJobDetailsController.create({
      content: Ember.Object.create({
        id: 'id',
        queryText: 'show tables',
        stages: [{
          id: 'Stage-0',
          description: '. Fetch Operator'
        }, {
          id: 'Stage-1',
          description: '. Tez'
        }],
        name: 'id',
        startTime: 1394569191001,
        endTime: null,
        tezDagId: 'id:1',
        tezDag: {
          id: 'id:1',
          instanceId: 'dag_1394502141829_0425_1',
          name: 'id:1',
          yarnApplicationId: 'application_1395263571423_0014',
          stage: 'Stage-1',
          vertices: [
            Ember.Object.create({
              id: 'id:1/Map 1',
              name: 'Map 1',
              type: 'MAP',
              operations: ['TableScan', 'Filter Operator', 'Reduce Output Operator'],
              operationPlan: '[\n  {\n    "TableScan": {\n      "filterExpr:": "((hd_dep_count = 4) or (hd_vehicle_count = 3)) (type: boolean)",\n      "alias:": "household_demographics",\n      "children": {\n        "Filter Operator": {\n          "predicate:": "((hd_dep_count = 4) or (hd_vehicle_count = 3)) (type: boolean)",\n          "children": {\n            "Reduce Output Operator": {\n              "Map-reduce partition columns:": "hd_demo_sk (type: int)",\n              "sort order:": "+",\n              "value expressions:": "hd_demo_sk (type: int), hd_dep_count (type: int), hd_vehicle_count (type: int)",\n              "Statistics:": "Num rows: 7200 Data size: 770400 Basic stats: COMPLETE Column stats: NONE",\n              "key expressions:": "hd_demo_sk (type: int)"\n            }\n          },\n          "Statistics:": "Num rows: 7200 Data size: 770400 Basic stats: COMPLETE Column stats: NONE"\n        }\n      },\n      "Statistics:": "Num rows: 7200 Data size: 770400 Basic stats: COMPLETE Column stats: NONE"\n    }\n  }\n]'
            }), Ember.Object.create({
              id: 'id:1/Map 2',
              name: 'Map 2',
              state: 'KILLED',
              type: 'MAP',
              startTime: 1394569231819,
              endTime: 1394569303554,
              operations: ['TableScan', 'Map Join Operator', 'Map Join Operator', 'Map Join Operator', 'Map Join Operator', 'Filter Operator', 'Select Operator', 'Group By Operator', 'Reduce Output Operator'],
              operationPlan: '[\n  {\n    "TableScan": {\n      "alias:": "store_sales",\n      "children": {\n        "Map Join Operator": {\n          "keys:": {\n            "0": "ss_sold_date_sk (type: int)",\n            "1": "d_date_sk (type: int)"\n          },\n          "outputColumnNames:": [\n            "_col0",\n            "_col3",\n            "_col5",\n            "_col6",\n            "_col8",\n            "_col18",\n            "_col21",\n            "_col22",\n            "_col25",\n            "_col31",\n            "_col32"\n          ],\n          "children": {\n            "Map Join Operator": {\n              "keys:": {\n                "0": "UDFToDouble(_col22) (type: double)",\n                "1": "UDFToDouble(s_store_sk) (type: double)"\n              },\n              "outputColumnNames:": [\n                "_col0",\n                "_col3",\n                "_col5",\n                "_col6",\n                "_col8",\n                "_col18",\n                "_col21",\n                "_col22",\n                "_col25",\n                "_col31",\n                "_col32",\n                "_col55",\n                "_col77"\n              ],\n              "children": {\n                "Map Join Operator": {\n                  "keys:": {\n                    "0": "_col5 (type: int)",\n                    "1": "hd_demo_sk (type: int)"\n                  },\n                  "outputColumnNames:": [\n                    "_col0",\n                    "_col22",\n                    "_col31",\n                    "_col34",\n                    "_col36",\n                    "_col37",\n                    "_col39",\n                    "_col49",\n                    "_col52",\n                    "_col53",\n                    "_col56",\n                    "_col62",\n                    "_col63",\n                    "_col86",\n                    "_col89",\n                    "_col90"\n                  ],\n                  "children": {\n                    "Map Join Operator": {\n                      "keys:": {\n                        "0": "_col37 (type: int)",\n                        "1": "ca_address_sk (type: int)"\n                      },\n                      "outputColumnNames:": [\n                        "_col0",\n                        "_col3",\n                        "_col4",\n                        "_col7",\n                        "_col29",\n                        "_col38",\n                        "_col41",\n                        "_col43",\n                        "_col44",\n                        "_col46",\n                        "_col56",\n                        "_col59",\n                        "_col60",\n                        "_col63",\n                        "_col69",\n                        "_col70",\n                        "_col93",\n                        "_col99"\n                      ],\n                      "children": {\n                        "Filter Operator": {\n                          "predicate:": "((((((((_col38 = _col63) and (_col60 = _col7)) and (_col43 = _col0)) and (_col44 = _col93)) and ((_col3 = 4) or (_col4 = 3))) and (_col70) IN (6, 0)) and (_col69) IN (1999, (1999 + 1), (1999 + 2))) and (_col29) IN (\'Fairview\', \'Fairview\', \'Fairview\', \'Midway\', \'Fairview\')) (type: boolean)",\n                          "children": {\n                            "Select Operator": {\n                              "expressions:": "_col46 (type: int), _col41 (type: int), _col44 (type: int), _col99 (type: string), _col56 (type: decimal(7,2)), _col59 (type: decimal(7,2))",\n                              "outputColumnNames:": [\n                                "_col46",\n                                "_col41",\n                                "_col44",\n                                "_col99",\n                                "_col56",\n                                "_col59"\n                              ],\n                              "children": {\n                                "Group By Operator": {\n                                  "mode:": "hash",\n                                  "aggregations:": [\n                                    "sum(_col56)",\n                                    "sum(_col59)"\n                                  ],\n                                  "keys:": "_col46 (type: int), _col41 (type: int), _col44 (type: int), _col99 (type: string)",\n                                  "outputColumnNames:": [\n                                    "_col0",\n                                    "_col1",\n                                    "_col2",\n                                    "_col3",\n                                    "_col4",\n                                    "_col5"\n                                  ],\n                                  "children": {\n                                    "Reduce Output Operator": {\n                                      "Map-reduce partition columns:": "_col0 (type: int), _col1 (type: int), _col2 (type: int), _col3 (type: string)",\n                                      "sort order:": "++++",\n                                      "value expressions:": "_col4 (type: decimal(17,2)), _col5 (type: decimal(17,2))",\n                                      "Statistics:": "Num rows: 32946 Data size: 43551613 Basic stats: COMPLETE Column stats: NONE",\n                                      "key expressions:": "_col0 (type: int), _col1 (type: int), _col2 (type: int), _col3 (type: string)"\n                                    }\n                                  },\n                                  "Statistics:": "Num rows: 32946 Data size: 43551613 Basic stats: COMPLETE Column stats: NONE"\n                                }\n                              },\n                              "Statistics:": "Num rows: 32946 Data size: 43551613 Basic stats: COMPLETE Column stats: NONE"\n                            }\n                          },\n                          "Statistics:": "Num rows: 32946 Data size: 43551613 Basic stats: COMPLETE Column stats: NONE"\n                        }\n                      },\n                      "Statistics:": "Num rows: 4217199 Data size: 5574753280 Basic stats: COMPLETE Column stats: NONE",\n                      "condition map:": [\n                        {\n                          "": "Inner Join 0 to 1"\n                        }\n                      ],\n                      "condition expressions:": {\n                        "0": "{_col86} {_col89} {_col90} {_col0} {_col22} {_col31} {_col34} {_col36} {_col37} {_col39} {_col49} {_col52} {_col53} {_col56} {_col62} {_col63}",\n                        "1": "{ca_address_sk} {ca_city}"\n                      }\n                    }\n                  },\n                  "Statistics:": "Num rows: 3833817 Data size: 5067957248 Basic stats: COMPLETE Column stats: NONE",\n                  "condition map:": [\n                    {\n                      "": "Inner Join 0 to 1"\n                    }\n                  ],\n                  "condition expressions:": {\n                    "0": "{_col55} {_col77} {_col0} {_col3} {_col5} {_col6} {_col8} {_col18} {_col21} {_col22} {_col25} {_col31} {_col32}",\n                    "1": "{hd_demo_sk} {hd_dep_count} {hd_vehicle_count}"\n                  }\n                }\n              },\n              "Statistics:": "Num rows: 3485288 Data size: 4607233536 Basic stats: COMPLETE Column stats: NONE",\n              "condition map:": [\n                {\n                  "": "Inner Join 0 to 1"\n                }\n              ],\n              "condition expressions:": {\n                "0": "{_col0} {_col3} {_col5} {_col6} {_col8} {_col18} {_col21} {_col22} {_col25} {_col31} {_col32}",\n                "1": "{s_store_sk} {s_city}"\n              }\n            }\n          },\n          "Statistics:": "Num rows: 3168444 Data size: 4188394240 Basic stats: COMPLETE Column stats: NONE",\n          "condition map:": [\n            {\n              "": "Inner Join 0 to 1"\n            }\n          ],\n          "condition expressions:": {\n            "0": "{ss_sold_date_sk} {ss_customer_sk} {ss_hdemo_sk} {ss_addr_sk} {ss_ticket_number} {ss_coupon_amt} {ss_net_profit} {ss_store_sk}",\n            "1": "{d_date_sk} {d_year} {d_dow}"\n          }\n        }\n      },\n      "Statistics:": "Num rows: 2880404 Data size: 3807631184 Basic stats: COMPLETE Column stats: NONE"\n    }\n  }\n]',
              tasksCount: 6,
              fileReadBytes: 0,
              fileWriteBytes: 337559,
              fileReadOps: 0,
              fileWriteOps: 0,
              spilledRecords: 7194,
              hdfsReadBytes: 12854749,
              hdfsWriteBytes: 0,
              hdfsReadOps: 12,
              hdfsWriteOps: 0
            })
          ]
        }
      })
    })
  });
  describe('#zoomStep', function () {
    it('should be calculated according to difference between zoomScaleTo and zoomScaleFrom', function () {
      mainHiveJobDetailsView.set('zoomScaleFrom', 0);
      mainHiveJobDetailsView.set('zoomScaleTo', 1);
      expect(mainHiveJobDetailsView.get('zoomStep')).to.equal(0.2);
      mainHiveJobDetailsView.set('zoomScaleFrom', 2);
      expect(mainHiveJobDetailsView.get('zoomStep')).to.equal(0.01);
    });
  });
  describe('#canGraphZoomIn', function () {
    it('should be calculated according to comparison between zoomScale and zoomScaleTo', function () {
      mainHiveJobDetailsView.set('zoomScale', 2);
      expect(mainHiveJobDetailsView.get('canGraphZoomIn')).to.equal(false);
      mainHiveJobDetailsView.set('zoomScaleTo', 3);
      expect(mainHiveJobDetailsView.get('canGraphZoomIn')).to.equal(true);
    });
  });
  describe('#canGraphZoomOut', function () {
    it('should be calculated according to comparison between zoomScale and zoomScaleFrom', function () {
      mainHiveJobDetailsView.set('zoomScale', 3);
      expect(mainHiveJobDetailsView.get('canGraphZoomOut')).to.equal(true);
      mainHiveJobDetailsView.set('zoomScaleFrom', 4);
      expect(mainHiveJobDetailsView.get('canGraphZoomOut')).to.equal(false);
    });
  });
  describe('#doGraphZoomIn', function () {
    it('should be calculated according to zoomScale, zoomScaleTo and zoomScaleFrom', function () {
      mainHiveJobDetailsView.set('zoomScale', 0);
      mainHiveJobDetailsView.set('zoomScaleTo', 1);
      mainHiveJobDetailsView.set('zoomScaleFrom', 0);
      mainHiveJobDetailsView.doGraphZoomIn();
      expect(mainHiveJobDetailsView.get('zoomScale')).to.equal(0.2);
      mainHiveJobDetailsView.set('zoomScale', 0);
      mainHiveJobDetailsView.set('zoomScaleTo', 10);
      mainHiveJobDetailsView.set('zoomScaleFrom', 0);
      mainHiveJobDetailsView.doGraphZoomIn();
      expect(mainHiveJobDetailsView.get('zoomScale')).to.equal(2);
    });
  });
  describe('#doGraphZoomOut', function () {
    it('should be calculated according to zoomScale, zoomScaleTo and zoomScaleFrom', function () {
      mainHiveJobDetailsView.set('zoomScale', 3);
      mainHiveJobDetailsView.set('zoomScaleTo', 2);
      mainHiveJobDetailsView.set('zoomScaleFrom', 1);
      mainHiveJobDetailsView.doGraphZoomOut();
      expect(mainHiveJobDetailsView.get('zoomScale')).to.equal(2.8);
      mainHiveJobDetailsView.set('zoomScale', 1);
      mainHiveJobDetailsView.set('zoomScaleTo', 10);
      mainHiveJobDetailsView.set('zoomScaleFrom', 0);
      mainHiveJobDetailsView.doGraphZoomOut();
      expect(mainHiveJobDetailsView.get('zoomScale')).to.equal(0);
    });
  });
  describe('#toggleShowQueryText', function () {
    it('should be toggled according to showQuery', function () {
      mainHiveJobDetailsView.set('showQuery', true);
      expect(mainHiveJobDetailsView.get('toggleShowQueryText')).to.equal(Em.I18n.t('jobs.hive.less'));
      mainHiveJobDetailsView.toggleProperty('showQuery');
      expect(mainHiveJobDetailsView.get('toggleShowQueryText')).to.equal(Em.I18n.t('jobs.hive.more'));
    });
  });
  describe('#summaryMetricTypeDisplay', function () {
    it('should return the correct message for output', function () {
      mainHiveJobDetailsView.set('summaryMetricType', 'output');
      expect(mainHiveJobDetailsView.get('summaryMetricTypeDisplay')).to.equal(Em.I18n.t('jobs.hive.tez.metric.output'));
    });
  });
  describe('#initialDataLoaded', function () {
    it('controller.content should be loaded to view.content', function () {
      mainHiveJobDetailsView.set('controller.loaded', true);
      expect(mainHiveJobDetailsView.get('content.id')).to.equal(mainHiveJobDetailsView.get('controller.content.id'));
    });
  });
  describe('#jobObserver', function () {
    it('should set selection of the first vertex as default', function () {
      expect(mainHiveJobDetailsView.get('selectedVertex.id')).to.equal(mainHiveJobDetailsView.get('controller.content.tezDag.vertices').objectAt(0).get('id'));
    });
  });
});
