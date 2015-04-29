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
require('utils/helper');
require('controllers/wizard/slave_component_groups_controller');

var configPropertyHelper = require('utils/configs/config_property_helper');
var controller;
describe('App.SlaveComponentGroupsController', function () {

  beforeEach(function () {
    controller = App.SlaveComponentGroupsController.create({
      content: {},
      selectedSlaveComponent: null
    });
  });

  describe('#getHostsByGroup', function () {
    it('should return empty array', function () {
      controller.set('hosts', undefined);
      expect(controller.getHostsByGroup()).to.eql([]);
    });
    it('should return g1 hosts', function () {
      var hosts = Em.A([
        Em.Object.create({
          group: 'g1',
          name: 'h1'
        }),
        Em.Object.create({
          group: 'g2',
          name: 'h2'
        }),
        Em.Object.create({
          group: 'g1',
          name: 'h3'
        })
      ]);
      var group = {
        name: 'g1'
      };
      var selectedSlaveComponent = Em.Object.create({
        hosts: hosts
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      var expected = [
        {
          "group": "g1",
          "name": "h1"
        },
        {
          "group": "g1",
          "name": "h3"
        }
      ];
 
      expect(JSON.parse(JSON.stringify(controller.getHostsByGroup(group)))).to.eql(expected);
    });
  });

  describe('#changeSlaveGroupName', function () {
    it('should return true if group is exist', function () {
      var selectedSlaveComponent = Em.Object.create({
        groups: Em.A([
          Em.Object.create({
            name: 'g1'
          })
        ])
      });
      var group = {};
      controller.set('selectedSlaveComponent',selectedSlaveComponent);
      expect(controller.changeSlaveGroupName(group, 'g1')).to.be.true;
    });
    it('should return false if group is not exist', function () {
      var hosts = Em.A([
        Em.Object.create({
          group: 'g1',
          name: 'h1'
        }),
        Em.Object.create({
          group: 'g2',
          name: 'h2'
        }),
        Em.Object.create({
          group: 'g1',
          name: 'h3'
        })
      ]);
      var selectedSlaveComponent = Em.Object.create({
        hosts: hosts,
        groups: Em.A([
          Em.Object.create({
            name: 'g1'
          })
        ])
      });
      var group = {
        name: 'g2'
      };
      controller.set('selectedSlaveComponent',selectedSlaveComponent);
      expect(controller.changeSlaveGroupName(group, 'g2')).to.be.false;
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(App.SlaveConfigs, 'create').returns(Em.Object.create({
        groups: false,
        componentName: '',
        displayName: ''
      }));
      sinon.stub(App.config, 'get').returns(Em.A([
        Em.Object.create({
          category: 'HBASE',
          serviceName: 'HBASE',
          configs: Em.A([Em.Object.create({})])
        })
      ]));
      sinon.stub(App.ServiceConfigProperty, 'create').returns(Em.Object.create({
          name: 'dfs_data_dir',
          initialValue: Em.K
        })
      );
    });
    afterEach(function () {
      App.SlaveConfigs.create.restore();
      App.ServiceConfigProperty.create.restore();
      App.config.get.restore();
    });
    it('should add groups to stepConfgig', function () {
      var stepConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HBASE',
          configCategories: Em.A([
            Em.Object.create({
              isForSlaveComponent: true,
              primaryName: 'sl1',
              slaveConfigs: Em.A([])
            })
          ])
        })
      ]);
      var content = Em.A([
        Em.Object.create({
          componentName: 'sl1',
          displayName: 'd1'
        })
      ]);
      controller.set('content',content);
      controller.set('stepConfigs',stepConfigs);
      controller.loadStep();
      var expected = [
        {
          "serviceName": "HBASE",
          "configCategories": [
            {
              "isForSlaveComponent": true,
              "primaryName": "sl1",
              "slaveConfigs": {
                "groups": [
                  {
                   "name": "Default",
                   "index":
                   "default",
                   "type": "default",
                   "active": true,
                   "properties": []
                  }
                ],

                "componentName": "sl1",
                "displayName": "d1"
              }
            }
          ]
        }
      ];
      var result = JSON.parse(JSON.stringify(controller.get('stepConfigs')));
      expect(result).to.be.eql(expected);
    });
  });

  describe('#componentProperties', function () {
    beforeEach(function () {
      sinon.stub(App.config, 'get').returns(Em.A([
        Em.Object.create({
          category: 'RegionServer',
          serviceName: 'HBASE',
          configs: Em.A([
            Em.Object.create({
              category: 'RegionServer'
            }),
            Em.Object.create({
              next: true,
              category: 'RegionServer'
            })
          ])
        })
      ]));
      sinon.stub(App.ServiceConfigProperty, 'create', function(value) {
        if (value && value.next) {
          return Em.Object.create({
            name: 'dfs_data_dir',
            initialValue: Em.K,
            validate: Em.K
          });
        } else {
          return Em.Object.create({
            name: 'mapred_local_dir',
            initialValue: Em.K,
            validate: Em.K
          });
        }
      });
      sinon.stub(configPropertyHelper, 'initialValue', Em.K);
    });
    afterEach(function () {
      App.ServiceConfigProperty.create.restore();
      App.config.get.restore();
      configPropertyHelper.initialValue.restore();
    });
    it('should return created config', function () {
      var res = JSON.parse(JSON.stringify(controller.componentProperties('HBASE')));
      var expected = [
        {
          "name": "mapred_local_dir"
        },
        {
          "name": "dfs_data_dir"
        }
      ];
      expect(res).to.be.eql(expected);
    });
  });

  describe('#selectedComponentName', function () {
    afterEach(function () {
      App.router.get.restore();
    });
    it('should return selected component name HDFS', function () {
      sinon.stub(App.router, 'get').returns('HDFS');
      expect(controller.get('selectedComponentName')).to.be.eql({
        name: 'DATANODE',
        displayName: 'DataNode'
      });
    });
    it('should return selected component name HBASE', function () {
      sinon.stub(App.router, 'get').returns('HBASE');
      expect(controller.get('selectedComponentName')).to.be.eql({
        name: 'HBASE_REGIONSERVER',
        displayName: 'RegionServer'
      });
    });
    it('should return null', function () {
      sinon.stub(App.router, 'get').returns('undefined');
      expect(controller.get('selectedComponentName')).to.be.null;
    });
  });

  describe('#selectedSlaveComponent', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configCategories: Em.A([
            Em.Object.create({
              isForSlaveComponent: true,
              primaryName: 'sl1',
              slaveConfigs: Em.A([]),
              name: 'name'
            })
          ])
        })
      ]));
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it('should return selected component name', function () {
      var controller = App.SlaveComponentGroupsController.create({
        content: {}
      });
      controller.set('selectedComponentName', Em.Object.create({
        displayName: 'name'
      }));
      controller.set('selectedComponentName', 'value')
      expect(controller.get('selectedSlaveComponent')).to.be.null;
    });
  });

  describe('#removeSlaveComponentGroup', function () {
    beforeEach(function() {
      sinon.stub(controller, 'componentProperties').returns(Em.A([]));
    });
    afterEach(function() {
      controller.componentProperties.restore();
    });
    it('should return empty selected component', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([]),
        groups: Em.A([
          Em.Object.create({
            active: true,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      var ev = Em.Object.create({
        context: selectedSlaveComponent.groups[0]
      });
      controller.removeSlaveComponentGroup(ev);
      var expected = {
        "newGroupIndex": 0,
        "hosts": [],
        "groups": []
      };

      var res = JSON.parse(JSON.stringify(controller.get('selectedSlaveComponent')));
      expect(res).to.be.eql(expected);
    });
  });

  describe('#showSlaveComponentGroup', function () {
    it('should make all groups active', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([]),
        groups: Em.A([
          Em.Object.create({
            active: false,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      var ev = Em.Object.create({
        context: selectedSlaveComponent.groups[0]
      });
      controller.showSlaveComponentGroup(ev);
      var expected = {
        "newGroupIndex": 0,
        "hosts": [],
        "groups": [
          {
            "active": true,
            "name": "New Group",
            "type": "new"
          }
        ]
      };

      var res = JSON.parse(JSON.stringify(controller.get('selectedSlaveComponent')));
      expect(res).to.be.eql(expected);
    });
  });

  describe('#selectedComponentDisplayName', function () {
    beforeEach(function () {
      sinon.stub(App.format, 'role').returns('name')
    });
    afterEach(function () {
      App.format.role.restore();
    });
    it('should return selected component name', function () {
      expect(controller.get('selectedComponentDisplayName')).to.be.equal('name');
    });
  });

  describe('#hosts', function () {
    it('should return hosts', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([{name: 'h1'}]),
        groups: Em.A([
          Em.Object.create({
            active: false,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      expect(controller.get('hosts')).to.be.eql(Em.A([{name: 'h1'}]));
    });
  });

  describe('#componentGroups', function () {
    it('should return groups', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([{name: 'h1', group: 'one'}, {name: 'h2', group: 'one'}]),
        groups: Em.A([
          Em.Object.create({
            active: false,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      expect(controller.get('componentGroups')).to.be.eql(Em.A([
        Em.Object.create({
          active: false,
          name: 'New Group',
          type: 'new'
        })
      ]));
    });
  });

  describe('#activeGroup', function () {
    it('should return active group', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([{name: 'h1', group: 'one'}, {name: 'h2', group: 'one'}]),
        groups: Em.A([
          Em.Object.create({
            active: false,
            name: 'New Group',
            type: 'new'
          }),
          Em.Object.create({
            active: true,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      expect(controller.get('activeGroup')).to.be.eql(Em.Object.create({
        active: true,
        name: 'New Group',
        type: 'new'
      }));
    });
  });

  describe('#groups', function () {
    it('should return uniq groups names', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        hosts: Em.A([{name: 'h1', group: 'one'}, {name: 'h2', group: 'one'}]),
        groups: Em.A([
          Em.Object.create({
            active: false,
            name: 'New Group',
            type: 'new'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      expect(controller.get('groups')).to.be.eql(['one']);
    });
  });

  describe('#addSlaveComponentGroup', function () {
    beforeEach(function() {
      sinon.stub(controller, 'componentProperties').returns(Em.A([]));
    });
    afterEach(function() {
      controller.componentProperties.restore();
    });
    it('should return selected component name', function () {
      var selectedSlaveComponent = Em.Object.create({
        newGroupIndex: 0,
        groups: Em.A([
          Em.Object.create({
            active: true,
            name: 'New Group'
          })
        ])
      });
      controller.set('selectedSlaveComponent', selectedSlaveComponent);
      controller.addSlaveComponentGroup();
      var expected = {
        "newGroupIndex": 1,
        "groups": [
          {
            "active": false,
            "name": "New Group"
          },
          {
            "name": "New Group 1",
            "index": 1,
            "type": "new",
            "active": true,
            "properties": []
          }
        ]
      };

      var res = JSON.parse(JSON.stringify(controller.get('selectedSlaveComponent')));
      expect(res).to.be.eql(expected);
    });
  });

  describe('#checkGroupName', function () {
    it('should make equal to 2', function () {
      var selectedSlaveComponent = Em.Object.create({
        groups: Em.A([
          Em.Object.create({
            name: 'New Group 1'
          })
        ]),
        newGroupIndex: 0
      });
      var group = {};
      controller.set('selectedSlaveComponent',selectedSlaveComponent);
      controller.checkGroupName();
      expect(controller.get('selectedSlaveComponent').newGroupIndex).to.be.equal(2);
    });
  });

  describe('#changeHostGroup', function () {
    it('should push 1 host group', function () {
      var selectedSlaveComponent = Em.Object.create({
        tempSelectedGroups: undefined
      });
      var host = Em.Object.create({
        hostName: 'h1'
      });
      controller.set('selectedSlaveComponent',selectedSlaveComponent);
      controller.changeHostGroup(host, 'g1');
      var expected = [
        {
          "hostName": "h1",
          "groupName": "g1"
        }
      ];
      var result = JSON.parse(JSON.stringify(controller.get('selectedSlaveComponent').tempSelectedGroups));
      expect(result).to.be.eql(expected);
    });
    it('should push change host group name', function () {
      var selectedSlaveComponent = Em.Object.create({
        tempSelectedGroups: [
          Em.Object.create({
            hostName: 'h1',
            groupName: ''
          })
        ]
      });
      var host = Em.Object.create({
        hostName: 'h1'
      });
      controller.set('selectedSlaveComponent',selectedSlaveComponent);
      controller.changeHostGroup(host, 'g1');
      var expected = [
        Em.Object.create({
          "hostName": "h1",
          "groupName": "g1"
        })
      ]
      expect(controller.get('selectedSlaveComponent').tempSelectedGroups).to.be.eql(expected);
    });
  });

  describe('#loadGroups', function () {
    beforeEach(function () {
      sinon.stub(App.SlaveConfigs, 'create').returns(Em.Object.create({
        groups: false,
        componentName: '',
        displayName: ''
      }));
      sinon.stub(App.config, 'get').returns(Em.A([
        Em.Object.create({
          category: 'HDFS',
          serviceName: 'HDFS',
          configs: Em.A([])
        })
      ]));
    });
    afterEach(function () {
      App.SlaveConfigs.create.restore();
      App.config.get.restore();
    });
    it('should modefie step confgigs', function () {
      var stepConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configCategories: Em.A([
            Em.Object.create({
              isForSlaveComponent: true,
              primaryName: 'sl1',
              slaveConfigs: Em.A([])
            })
          ])
        })
      ]);
      var content = Em.A([
        Em.Object.create({
          componentName: 'sl1',
          displayName: 'd1',
          groups: Em.A([
            Em.Object.create({
              name: 'g1'
            })
          ])
        })
      ]);
      controller.set('content',content);
      controller.set('stepConfigs',stepConfigs);
      controller.loadGroups();
      var expected = [
        {
          "serviceName": "HDFS",
          "configCategories": [
            {
              "isForSlaveComponent": true,
              "primaryName": "sl1",
              "slaveConfigs": {
                "groups": [
                  {
                    "name": "g1"
                  }
                ],
                "componentName": "sl1",
                "displayName": "d1"
              }
            }
          ]
        }
      ];
      var result = JSON.parse(JSON.stringify(controller.get('stepConfigs')));
      expect(result).to.be.eql(expected);
    });
    it('should add groups to stepConfgig', function () {
      var stepConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configCategories: Em.A([
            Em.Object.create({
              isForSlaveComponent: true,
              primaryName: 'sl1',
              slaveConfigs: Em.A([])
            })
          ])
        })
      ]);
      var content = Em.A([
        Em.Object.create({
          componentName: 'sl1',
          displayName: 'd1'
        })
      ]);
      controller.set('content',content);
      controller.set('stepConfigs',stepConfigs);
      controller.loadGroups();
      var expected = [
        {
          "serviceName": "HDFS",
          "configCategories": [
            {
              "isForSlaveComponent": true,
              "primaryName": "sl1",
              "slaveConfigs": {
                "groups": [
                  {
                   "name": "Default",
                   "index":
                   "default",
                   "type": "default",
                   "active": true,
                   "properties": []
                  }
                ],

                "componentName": "sl1",
                "displayName": "d1"
              }
            }
          ]
        }
      ];
      var result = JSON.parse(JSON.stringify(controller.get('stepConfigs')))
      expect(result).to.be.eql(expected);
    });
  });

});
