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

var Ember = require('ember');
var App = require('app');
require('mappers/server_data_mapper');
require('mappers/stack_mapper');
require('models/stack');
require('models/operating_system');
require('models/repository');

describe('App.stackMapper', function () {
	describe("#map", function() {
    
    var test_data = {
        items: [{
          "Versions" : {
            "active" : true,
            "min_upgrade_version" : null,
            "parent_stack_version" : "1.3.3",
            "stack_name" : "HDP",
            "stack_version" : "1.3"
          },
          "operating_systems" : [
            {
              "OperatingSystems" : {
                "os_type" : "redhat5",
                "stack_name" : "HDP",
                "stack_version" : "1.3"
              },
              "repositories" : [
                {
                   "Repositories" : {
                    "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/1.x/updates/1.3.7.0",
                    "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/1.x/updates/1.3.7.0",
                    "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/1.x/updates/1.3.8.0",
                    "mirrors_list" : null,
                    "os_type" : "redhat5",
                    "repo_id" : "HDP-1.3",
                    "repo_name" : "HDP",
                    "stack_name" : "HDP",
                    "stack_version" : "1.3"
                  }
                },{
                  "Repositories" : {
                    "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos5",
                    "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos5",
                    "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos5",
                    "mirrors_list" : null,
                    "os_type" : "redhat5",
                    "repo_id" : "HDP-UTILS-1.1.0.16",
                    "repo_name" : "HDP-UTILS",
                    "stack_name" : "HDP",
                    "stack_version" : "1.3"
                  }
                }]
            },{
              "OperatingSystems" : {
                "os_type" : "redhat6",
                "stack_name" : "HDP",
                "stack_version" : "1.3"
              }, "repositories" : [
                  {
                    "Repositories" : {
                      "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/1.x/updates/1.3.7.0",
                      "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/1.x/updates/1.3.7.0",
                      "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/1.x/updates/1.3.8.0",
                      "mirrors_list" : null,
                      "os_type" : "redhat6",
                      "repo_id" : "HDP-1.3",
                      "repo_name" : "HDP",
                      "stack_name" : "HDP",
                      "stack_version" : "1.3"
                    }
                  },
                  {
                    "Repositories" : {
                      "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos6",
                      "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos6",
                      "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.16/repos/centos6",
                      "mirrors_list" : null,
                      "os_type" : "redhat6",
                      "repo_id" : "HDP-UTILS-1.1.0.16",
                      "repo_name" : "HDP-UTILS",
                      "stack_name" : "HDP",
                      "stack_version" : "1.3"
                    }
                  }
                ]
            }]
      },{
        "Versions" : {
          "active" : false,
          "min_upgrade_version" : null,
          "parent_stack_version" : null,
          "stack_name" : "HDP",
          "stack_version" : "2.0.6"
        },
        "operating_systems" : [
          {
            "OperatingSystems" : {
              "os_type" : "redhat5",
              "stack_name" : "HDP",
              "stack_version" : "2.0.6"
            },
            "repositories" : [
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "mirrors_list" : null,
                  "os_type" : "redhat5",
                  "repo_id" : "HDP-2.0.6",
                  "repo_name" : "HDP",
                  "stack_name" : "HDP",
                  "stack_version" : "2.0.6"
                }
              },
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "mirrors_list" : null,
                  "os_type" : "redhat5",
                  "repo_id" : "HDP-UTILS-1.1.0.17",
                  "repo_name" : "HDP-UTILS",
                  "stack_name" : "HDP",
                  "stack_version" : "2.0.6"
                }
              }]
          }, {
            "OperatingSystems" : {
              "os_type" : "redhat6",
              "stack_name" : "HDP",
              "stack_version" : "2.0.6"
            },
            "repositories" : [
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "mirrors_list" : null,
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-2.0.6",
                  "repo_name" : "HDP",
                  "stack_name" : "HDP",
                  "stack_version" : "2.0.6"
                }
              }, {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "mirrors_list" : null,
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-UTILS-1.1.0.17",
                  "repo_name" : "HDP-UTILS",
                  "stack_name" : "HDP",
                  "stack_version" : "2.0.6"
                }
              }]
          }]
      },{
        "Versions" : {
          "active" : true,
          "min_upgrade_version" : null,
          "parent_stack_version" : null,
          "stack_name" : "HDP",
          "stack_version" : "2.1"
        },
        "operating_systems" : [
          {
            "OperatingSystems" : {
              "os_type" : "redhat5",
              "stack_name" : "HDP",
              "stack_version" : "2.1"
            },
            "repositories" : [
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos5/2.x/updates/2.0.6.1",
                  "mirrors_list" : null,
                  "os_type" : "redhat5",
                  "repo_id" : "HDP-2.1",
                  "repo_name" : "HDP",
                  "stack_name" : "HDP",
                  "stack_version" : "2.1"
                }
              },
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos5",
                  "mirrors_list" : null,
                  "os_type" : "redhat5",
                  "repo_id" : "HDP-UTILS-1.1.0.17",
                  "repo_name" : "HDP-UTILS",
                  "stack_name" : "HDP",
                  "stack_version" : "2.1"
                }
              }]
          }, {
            "OperatingSystems" : {
              "os_type" : "redhat6",
              "stack_name" : "HDP",
              "stack_version" : "2.1"
            },
            "repositories" : [
              {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.1",
                  "mirrors_list" : null,
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-2.1",
                  "repo_name" : "HDP",
                  "stack_name" : "HDP",
                  "stack_version" : "2.1"
                }
              }, {
                "Repositories" : {
                  "base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "default_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "latest_base_url" : "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.17/repos/centos6",
                  "mirrors_list" : null,
                  "os_type" : "redhat6",
                  "repo_id" : "HDP-UTILS-1.1.0.17",
                  "repo_name" : "HDP-UTILS",
                  "stack_name" : "HDP",
                  "stack_version" : "2.1"
                }
              }]
          }]
      }] 
    };

    beforeEach(function () {
      App.resetDsStoreTypeMap(App.Repository);
      App.resetDsStoreTypeMap(App.OperatingSystem);
      App.resetDsStoreTypeMap(App.Stack);
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function(){
      App.store.commit.restore();
    });

		
    it ('should map active Stack data', function() {
      App.stackMapper.map(test_data);
      expect(App.Stack.find().get('length')).to.equal(2);
      expect(App.Stack.find().everyProperty('active')).to.equal(true);
      expect(App.Stack.find().everyProperty('isSelected')).to.equal(false);
      expect(App.Stack.find().mapProperty('id')).to.eql(['HDP-2.1','HDP-1.3']);
    });

    it ('should map Operating System data', function() {
      App.stackMapper.map(test_data);
      expect(App.OperatingSystem.find().get('length')).to.equal(4);
      expect(App.OperatingSystem.find().mapProperty('id')).to.eql(['HDP-2.1-redhat5', 'HDP-2.1-redhat6', 'HDP-1.3-redhat5', 'HDP-1.3-redhat6']);
    });
    
    it ('should map Repository data', function() {
      App.stackMapper.map(test_data);
      expect(App.Repository.find().get('length')).to.equal(8);
      expect(App.Repository.find().mapProperty('id')).to.eql(["HDP-2.1-redhat5-HDP-2.1", "HDP-2.1-redhat5-HDP-UTILS-1.1.0.17", "HDP-2.1-redhat6-HDP-2.1", "HDP-2.1-redhat6-HDP-UTILS-1.1.0.17", "HDP-1.3-redhat5-HDP-1.3", "HDP-1.3-redhat5-HDP-UTILS-1.1.0.16", "HDP-1.3-redhat6-HDP-1.3", "HDP-1.3-redhat6-HDP-UTILS-1.1.0.16"]);
    });
  });
});
