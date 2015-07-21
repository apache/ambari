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
require('mappers/configs/stack_config_properties_mapper');

describe('App.stackConfigPropertiesMapper', function () {

  describe("#map", function() {

    var json = { items: [
      {
        "StackServices" : {
          "service_name" : "HBASE",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "config_types" : {
            "site1" : {
              "supports" : {
                "adding_forbidden" : "false",
                "do_not_extend" : "false",
                "final" : "true"
              }
            }
          }
        },
        "configurations" : [
          {
            "StackConfigurations" : {
              "final" : "false",
              "property_description" : "desc1",
              "property_name" : "p1",
              "property_display_name" : "P1",
              "property_type" : [ ],
              "property_value" : "v1",
              "service_name" : "s1",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "type" : "site1.xml",
              "property_depends_on": [
                {
                  "name": "p5",
                  "type": "site5"
                }
              ],
              "property_value_attributes": {
                "type": "int",
                "minimum": "512",
                "maximum": "10240",
                "unit": "MB"
              }
            },
            "dependencies": [
              {
                "StackConfigurationDependency" : {
                  "dependency_name" : "p4",
                  "dependency_type" : "site4"
                }
              }
            ]
          }
        ]
      },
      {
        "StackServices" : {
          "service_name" : "HDFS",
          "stack_name" : "HDP",
          "stack_version" : "2.2",
          "config_types" : {
            "site2" : {
              "supports" : {
                "adding_forbidden" : "false",
                "do_not_extend" : "false",
                "final" : "true"
              }
            },
            "site3" : {
              "supports" : {
                "adding_forbidden" : "false",
                "do_not_extend" : "false",
                "final" : "true"
              }
            }
          }
        },
        "configurations" : [
          {
            "StackConfigurations" : {
              "final" : "false",
              "property_description" : "desc3",
              "property_name" : "p2",
              "property_display_name" : "P2",
              "property_type" : [ ],
              "property_value" : "v2",
              "service_name" : "s2",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "type" : "site2.xml"
            }
          },
          {
            "StackConfigurations" : {
              "final" : "false",
              "property_description" : "desc3",
              "property_name" : "p3",
              "property_display_name" : "P3",
              "property_type" : [ ],
              "property_value" : "v3",
              "service_name" : "s2",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "type" : "site3.xml"
            }
          },
          {
            "StackConfigurations" : {
              "final" : "false",
              "property_description" : "desc4",
              "property_name" : "p4",
              "property_display_name" : "P4",
              "property_type" : [ "PASSWORD" ],
              "property_value" : "v4",
              "service_name" : "s2",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "type" : "site3.xml"
            }
          },
          {
            "StackConfigurations" : {
              "final" : "false",
              "property_description" : "desc5",
              "property_name" : "p5",
              "property_display_name" : "P5",
              "property_type" : [ "USER" ],
              "property_value" : "v4",
              "service_name" : "s2",
              "stack_name" : "HDP",
              "stack_version" : "2.2",
              "type" : "site3.xml"
            }
          }
        ]
      }
    ]};

    beforeEach(function () {
      App.resetDsStoreTypeMap(App.StackConfigProperty);
      sinon.stub(App.store, 'commit', Em.K);
      sinon.stub(App.StackService, 'find', function() { return Em.A()});
    });
    afterEach(function(){
      App.store.commit.restore();
      App.StackService.find.restore();
    });

    it('should not do anything as there is no json', function() {
      App.stackConfigPropertiesMapper.map(null);
      expect(App.StackConfigProperty.find().get('length')).to.equal(0);
    });

    it('should load data to model', function() {
      App.stackConfigPropertiesMapper.map(json);
      expect(App.StackConfigProperty.find().get('length')).to.equal(5);
      expect(App.StackConfigProperty.find().mapProperty('id')).to.eql(['p1_site1','p2_site2','p3_site3', 'p4_site3', 'p5_site3']);

      expect(App.StackConfigProperty.find('p1_site1').get('name')).to.eql('p1');
      expect(App.StackConfigProperty.find('p1_site1').get('displayName')).to.eql('P1');
      expect(App.StackConfigProperty.find('p1_site1').get('description')).to.eql('desc1');
      expect(App.StackConfigProperty.find('p1_site1').get('recommendedValue')).to.eql('v1');
      expect(App.StackConfigProperty.find('p1_site1').get('recommendedIsFinal')).to.be.false;
      expect(App.StackConfigProperty.find('p1_site1').get('serviceName')).to.eql('s1');
      expect(App.StackConfigProperty.find('p1_site1').get('stackName')).to.eql('HDP');
      expect(App.StackConfigProperty.find('p1_site1').get('stackVersion')).to.eql('2.2');
      expect(App.StackConfigProperty.find('p1_site1').get('type').toArray()).to.eql([]);
      expect(App.StackConfigProperty.find('p1_site1').get('fileName')).to.eql('site1.xml');
      expect(App.StackConfigProperty.find('p1_site1').get('propertyDependedBy')).to.eql([
        {
          "type": "site4",
          "name": "p4"
        }
      ]);
      expect(App.StackConfigProperty.find('p1_site1').get('propertyDependsOn')).to.eql([
        {
          "type": "site5",
          "name": "p5"
        }
      ]);
      expect(App.StackConfigProperty.find('p1_site1').get('valueAttributes')).to.eql({
        "type": "int",
        "minimum": "512",
        "maximum": "10240",
        "unit": "MB"
      });
      expect(App.StackConfigProperty.find('p1_site1').get('supportsFinal')).to.be.true;
    });

    it('should set "displayType" by "property_type" attribute', function() {
      App.stackConfigPropertiesMapper.map(json);
      var prop = App.StackConfigProperty.find().findProperty('name', 'p4');
      var prop2 = App.StackConfigProperty.find().findProperty('name', 'p5');
      expect(prop).to.be.ok;
      expect(prop.get('displayType')).to.be.eql('password');
      expect(prop2.get('displayType')).to.be.eql('user');
    });
  });

});
