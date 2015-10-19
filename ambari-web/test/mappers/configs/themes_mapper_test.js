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
require('mappers/configs/themes_mapper');
require('models/configs/theme/tab');
require('models/configs/theme/section');
require('models/configs/theme/sub_section');
require('models/configs/stack_config_property');

describe('App.themeMapper', function () {

  beforeEach(function () {
    App.resetDsStoreTypeMap(App.Tab);
    App.resetDsStoreTypeMap(App.Section);
    App.resetDsStoreTypeMap(App.SubSection);
    App.resetDsStoreTypeMap(App.StackConfigProperty);
    sinon.stub(App.store, 'commit', Em.K);
  });

  afterEach(function () {
    App.store.commit.restore();
  });

  describe("#map", function () {

    var json = {
      items: [
        {
          ThemeInfo: {
            service_name: "HDFS",
            theme_data: {
              "Theme": {
                "name": "default",
                "description": "Default theme for HDFS service",
                "configuration": {
                  "layouts": [
                    {
                      "name": "default",
                      "tabs": [
                        {
                          "name": "settings",
                          "display-name": "Settings",
                          "layout": {
                            "tab-columns": "2",
                            "tab-rows": "1",
                            "sections": [
                              {
                                "name": "Section-1",
                                "display-name": "Section One",
                                "row-index": "0",
                                "column-index": "0",
                                "row-span": "1",
                                "column-span": "1",
                                "section-columns": "2",
                                "section-rows": "3",
                                "subsections": [
                                  {
                                    "name": "subsection1",
                                    "display-name": "Storage",
                                    "border": "false",
                                    "row-index": "0",
                                    "column-index": "0",
                                    "column-span": "1",
                                    "row-span": "1"
                                  }
                                ]
                              },
                              {
                                "name": "Section-2",
                                "display-name": "Section Two",
                                "row-index": "0",
                                "column-index": "1"
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ],
                  "widgets": [
                              {
                                "config": "c1/p1",
                                "widget": {
                                  "type": "slider",
                                  "units": [
                                            {
                                              "unit-name": "MB"
                                            },
                                            {
                                              "unit-name": "GB"
                                            }
                                            ]
                                }
                              },
                              {
                                "config": "c1/p2",
                                "widget": {
                                  "type": "slider",
                                  "units": [
                                            {
                                              "unit-name": "percent"
                                            }
                                            ]
                                }
                              }
                              ],
                              "placement": {
                                "configuration-layout": "default",
                                "configs": [
                                            {
                                              "config": "c1/p1",
                                              "subsection-name": "subsection1"
                                            },
                                            {
                                              "config": "c1/p2",
                                              "subsection-name": "subsection1"
                                            }
                                            ]
                              }
                }
              }
            }
          }
        }
      ]
    };

    it('should map theme data', function () {

      App.StackConfigProperty.createRecord({id: 'p1_c1'});
      App.StackConfigProperty.createRecord({id: 'p2_c1'});

      App.themesMapper.map(json);

      expect(App.Tab.find().get('length')).to.equal(1);
      expect(App.Section.find().get('length')).to.equal(2);
      expect(App.SubSection.find().get('length')).to.equal(1);

      //checking tab
      expect(App.Tab.find('HDFS_settings').toJSON()).to.eql({
        id: 'HDFS_settings',
        name: 'settings',
        display_name: 'Settings',
        columns: "2",
        rows: "1",
        is_advanced: false,
        service_name: 'HDFS',
        is_advanced_hidden: false,
        is_rendered: false,
        is_configs_prepared: false
      });

      //checking section
      expect(App.Tab.find('HDFS_settings').get('sections').objectAt(0).toJSON()).to.eql({
        "id": "Section-1",
        "name": "Section-1",
        "display_name": "Section One",
        "row_index": "0",
        "row_span": "1",
        "column_index": "0",
        "column_span": "1",
        "section_columns": "2",
        "section_rows": "3",
        "tab_id": "HDFS_settings"
      });

      //checking subsection
      expect(App.Tab.find('HDFS_settings').get('sections').objectAt(0).get('subSections').objectAt(0).toJSON()).to.eql({
        "id": "subsection1",
        "name": "subsection1",
        "display_name": "Storage",
        "border": "false",
        "row_index": "0",
        "row_span": "1",
        "column_index": "0",
        "depends_on": [],
        "left_vertical_splitter": true,
        "column_span": "1",
        "section_id": "Section-1"
      });

      //checking stack config object
      var config = App.Tab.find('HDFS_settings').get('sections').objectAt(0).get('subSections').objectAt(0).get('configProperties').objectAt(0);
      expect(config.get('id')).to.eql("p1_c1");
      expect(config.get('subSection.id')).to.eql("subsection1");
      expect(config.get('widget')).to.eql({
        "type": "slider",
        "units": [
          {
            "unit-name": "MB"
          },
          {
            "unit-name": "GB"
          }
        ]
      });
    });
  });

  describe('#generateAdvancedTabs', function () {
    it('generates advanced tabs', function () {
      App.themesMapper.generateAdvancedTabs(['HDFS']);
      expect(App.Tab.find('HDFS_advanced').toJSON()).to.eql({
        "id": "HDFS_advanced",
        "name": "advanced",
        "display_name": "Advanced",
        "columns": 1,
        "rows": 1,
        "is_advanced": true,
        "service_name": "HDFS",
        "is_advanced_hidden": false,
        is_rendered: false,
        is_configs_prepared: false
      });
    });
  });

  describe('#getConfigId', function () {
    it('gets configs id from json', function () {
      expect(App.themesMapper.getConfigId({config: "c1/p1"})).to.equal("p1_c1");
    });
    it('returns null as data is invalid', function () {
      expect(App.themesMapper.getConfigId({configs: "c1/p1"})).to.equal(null);
    });
  });
});
