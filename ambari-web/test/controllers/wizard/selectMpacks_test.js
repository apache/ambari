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
require('controllers/installer');
require('controllers/wizard/selectMpacks_controller');

var wizardSelectMpacksController;
var registry = {
  "href" : "http://localhost:8080/api/v1/registries?fields=mpacks/*,mpacks/versions/RegistryMpackVersionInfo/*,scenarios/*",
  "items" : [
    {
      "href" : "http://localhost:8080/api/v1/registries/1",
      "RegistryInfo" : {
        "registry_id" : 1
      },
      "mpacks" : [
        {
          "href" : "http://localhost:8080/api/v1/registries/1/mpacks/HDPCORE",
          "RegistryMpackInfo" : {
            "mpack_description" : "Hortonworks Data Platform Core",
            "mpack_id" : "hdpcore",
            "mpack_logo_uri" : "https://public-repo-1.hortonworks.com/logos/hdpcore-logo.png",
            "mpack_name" : "HDPCORE",
            "registry_id" : 1
          },
          "versions" : [
            {
              "href" : "http://localhost:8080/api/v1/registries/1/mpacks/HDPCORE/versions/1.0.0-b85",
              "RegistryMpackVersionInfo" : {
                "modules" : [
                  {
                    "id" : "zookeeper_clients",
                    "displayName" : "ZooKeeper Clients",
                    "description" : "Clients for ZooKeeper",
                    "category" : "CLIENT",
                    "name" : "ZOOKEEPER_CLIENTS",
                    "version" : "3.4.0.0-b29",
                    "definition" : "zookeeper_clients-3.4.0.0-b29-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "zookeeper",
                        "name" : "ZOOKEEPER",
                        "dependencyType" : "RUNTIME"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "zookeeper_client",
                        "name" : "ZOOKEEPER_CLIENT",
                        "category" : "CLIENT",
                        "isExternal" : false,
                        "version" : "3.4.0.0-b29"
                      }
                    ]
                  },
                  {
                    "id" : "zookeeper",
                    "displayName" : "ZooKeeper",
                    "description" : "Centralized service which provides highly reliable distributed coordination",
                    "category" : "SERVER",
                    "name" : "ZOOKEEPER",
                    "version" : "3.4.0.0-b29",
                    "definition" : "zookeeper-3.4.0.0-b29-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "zookeeper_clients",
                        "name" : "ZOOKEEPER_CLIENTS",
                        "dependencyType" : "INSTALL"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "zookeeper_server",
                        "name" : "ZOOKEEPER_SERVER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.4.0.0-b29"
                      }
                    ]
                  },
                  {
                    "id" : "hdfs",
                    "displayName" : "HDFS",
                    "description" : "Apache Hadoop Distributed File System",
                    "category" : "SERVER",
                    "name" : "HDFS",
                    "version" : "3.0.0.0-b54",
                    "definition" : "hdfs-3.0.0.0-b54-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "hadoop_clients",
                        "name" : "HADOOP_CLIENTS",
                        "dependencyType" : "INSTALL"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "namenode",
                        "name" : "NAMENODE",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "datanode",
                        "name" : "DATANODE",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "journalnode",
                        "name" : "JOURNALNODE",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "secondary_namenode",
                        "name" : "SECONDARY_NAMENODE",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "zkfc",
                        "name" : "ZKFC",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "nfs_gateway",
                        "name" : "NFS_GATEWAY",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      }
                    ]
                  },
                  {
                    "id" : "yarn",
                    "displayName" : "YARN",
                    "description" : "Apache Hadoop Yarn",
                    "category" : "SERVER",
                    "name" : "YARN",
                    "version" : "3.0.0.0-b54",
                    "definition" : "yarn-3.0.0.0-b54-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "hadoop_clients",
                        "name" : "HADOOP_CLIENTS",
                        "dependencyType" : "INSTALL"
                      },
                      {
                        "id" : "hdfs",
                        "name" : "HDFS",
                        "dependencyType" : "RUNTIME"
                      },
                      {
                        "id" : "mapreduce2",
                        "name" : "MAPREDUCE2",
                        "dependencyType" : "RUNTIME"
                      },
                      {
                        "id" : "zookeeper",
                        "name" : "ZOOKEEPER",
                        "dependencyType" : "RUNTIME"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "resourcemanager",
                        "name" : "RESOURCEMANAGER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "nodemanager",
                        "name" : "NODEMANAGER",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "app_timeline_server",
                        "name" : "APP_TIMELINE_SERVER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "timeline_reader",
                        "name" : "TIMELINE_READER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      },
                      {
                        "id" : "yarn_registry_dns",
                        "name" : "YARN_REGISTRY_DNS",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      }
                    ]
                  },
                  {
                    "id" : "mapreduce2",
                    "displayName" : "MAPREDUCE2",
                    "description" : "Apache Hadoop Mapreduce2",
                    "category" : "SERVER",
                    "name" : "MAPREDUCE2",
                    "version" : "3.0.0.0-b54",
                    "definition" : "mapreduce2-3.0.0.0-b54-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "hadoop_clients",
                        "name" : "HADOOP_CLIENTS",
                        "dependencyType" : "INSTALL"
                      },
                      {
                        "id" : "yarn",
                        "name" : "YARN",
                        "dependencyType" : "RUNTIME"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "historyserver",
                        "name" : "HISTORYSERVER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      }
                    ]
                  },
                  {
                    "id" : "hadoop_clients",
                    "displayName" : "Hadoop client",
                    "description" : "Clients for HDFS, YARN and MAPREDUCE services",
                    "category" : "CLIENT",
                    "name" : "HADOOP_CLIENTS",
                    "version" : "3.0.0.0-b54",
                    "definition" : "hadoop_clients-3.0.0.0-b54-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "hdfs",
                        "name" : "HDFS",
                        "dependencyType" : "RUNTIME"
                      },
                      {
                        "id" : "yarn",
                        "name" : "YARN",
                        "dependencyType" : "RUNTIME"
                      },
                      {
                        "id" : "mapreduce2",
                        "name" : "MAPREDUCE2",
                        "dependencyType" : "RUNTIME"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "hadoop_client",
                        "name" : "HADOOP_CLIENT",
                        "category" : "CLIENT",
                        "isExternal" : false,
                        "version" : "3.0.0.0-b54"
                      }
                    ]
                  }
                ],
                "mpack_dependencies" : null,
                "mpack_description" : "Hortonworks Data Platform Core",
                "mpack_doc_uri" : "http://docs.hortonworks.com/HDPDocuments/HDPCORE1/HDPCORE-1.0.0/index.html",
                "mpack_id" : "hdpcore",
                "mpack_logo_uri" : "https://public-repo-1.hortonworks.com/logos/hdpcore-logo.png",
                "mpack_name" : "HDPCORE",
                "mpack_uri" : "http://dev.hortonworks.com.s3.amazonaws.com/HDPCORE/centos7/1.x/BUILDS/1.0.0-b85/mpack.json",
                "mpack_version" : "1.0.0-b85",
                "registry_id" : 1
              }
            }
          ]
        },
        {
          "href" : "http://localhost:8080/api/v1/registries/1/mpacks/ODS",
          "RegistryMpackInfo" : {
            "mpack_description" : "Hortonworks Operational Data Store",
            "mpack_id" : "ods",
            "mpack_logo_uri" : "https://public-repo-1.hortonworks.com/logos/ods-logo.png",
            "mpack_name" : "ODS",
            "registry_id" : 1
          },
          "versions" : [
            {
              "href" : "http://localhost:8080/api/v1/registries/1/mpacks/ODS/versions/1.0.0-b27",
              "RegistryMpackVersionInfo" : {
                "modules" : [
                  {
                    "id" : "hbase",
                    "displayName" : "HBase",
                    "description" : "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
                    "category" : "SERVER",
                    "name" : "HBASE",
                    "version" : "2.0.0.0-b23",
                    "definition" : "hbase-2.0.0.0-b23-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "zookeeper_clients",
                        "name" : "ZOOKEEPER_CLIENTS",
                        "dependencyType" : "INSTALL"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "hbase_master",
                        "name" : "HBASE_MASTER",
                        "category" : "MASTER",
                        "isExternal" : false,
                        "version" : "2.0.0.0-b23"
                      },
                      {
                        "id" : "hbase_regionserver",
                        "name" : "HBASE_REGIONSERVER",
                        "category" : "SLAVE",
                        "isExternal" : false,
                        "version" : "2.0.0.0-b23"
                      }
                    ]
                  },
                  {
                    "id" : "hbase_clients",
                    "displayName" : "HBase",
                    "description" : "Non-relational distributed database and centralized service for configuration management &\n        synchronization\n      ",
                    "category" : "CLIENT",
                    "name" : "HBASE_CLIENTS",
                    "version" : "2.0.0.0-b23",
                    "definition" : "hbase_clients-2.0.0.0-b23-definition.tar.gz",
                    "dependencies" : [
                      {
                        "id" : "zookeeper_clients",
                        "name" : "ZOOKEEPER_CLIENTS",
                        "dependencyType" : "INSTALL"
                      }
                    ],
                    "components" : [
                      {
                        "id" : "hbase-client",
                        "name" : null,
                        "category" : null,
                        "isExternal" : null,
                        "version" : "2.0.0.0-b23"
                      }
                    ]
                  }
                ],
                "mpack_dependencies" : [
                  {
                    "id" : "hdpcore",
                    "name" : "HDPCORE",
                    "minVersion" : "1.0.0",
                    "maxVersion" : "1.0.99"
                  }
                ],
                "mpack_description" : "Hortonworks Operational Data Store",
                "mpack_doc_uri" : "http://docs.hortonworks.com/HDPDocuments/ODS1/ODS-1.0.0/index.html",
                "mpack_id" : "ods",
                "mpack_logo_uri" : "https://public-repo-1.hortonworks.com/logos/ods-logo.png",
                "mpack_name" : "ODS",
                "mpack_uri" : "http://dev.hortonworks.com.s3.amazonaws.com/ODS/centos7/1.x/BUILDS/1.0.0-b27/mpack.json",
                "mpack_version" : "1.0.0-b27",
                "registry_id" : 1
              }
            }
          ]
        }
      ],
      "scenarios" : [
        {
          "href" : "http://localhost:8080/api/v1/registries/1/scenarios/DataStore",
          "RegistryScenarioInfo" : {
            "registry_id" : 1,
            "scenario_description" : "Operational Data Store",
            "scenario_mpacks" : [
              {
                "name" : "HDPCORE"
              },
              {
                "name" : "ODS"
              }
            ],
            "scenario_name" : "DataStore"
          }
        },
        {
          "href" : "http://localhost:8080/api/v1/registries/1/scenarios/Hadoop",
          "RegistryScenarioInfo" : {
            "registry_id" : 1,
            "scenario_description" : "Hadoop Core Platform",
            "scenario_mpacks" : [
              {
                "name" : "HDPCORE"
              }
            ],
            "scenario_name" : "Hadoop"
          }
        }
      ]
    }
  ]
};

describe('App.WizardSelectMpacksController', function () {
  describe('#registryLoaded - before loading registry', function () {  
    it('should not have a loaded registry', function () {
      wizardController = App.InstallerController.create();
      wizardController.set('steps', [
        "step0",
        "selectMpacks",
        "step2",
        "step3",
        "step4"
      ]);

      wizardSelectMpacksController = App.WizardSelectMpacksController.create({
        isSubmitDisabled: false,
        selectedServices: null,
        selectedMpackVersions: null,
        content: {
          selectedServices: null,
          selectedServiceNames: null,
          selectedMpacks: null
        },
        wizardController: wizardController
      });

      expect(wizardSelectMpacksController.registryLoaded()).to.be.false;
    });
  });  
});

describe('App.WizardSelectMpacksController', function () {

  before(function () {
    wizardController = App.InstallerController.create();
    wizardController.set('steps', [
      "step0",
      "selectMpacks",
      "step2",
      "step3",
      "step4"
    ]);

    wizardSelectMpacksController = App.WizardSelectMpacksController.create({
      isSubmitDisabled: false,
      selectedServices: null,
      selectedMpackVersions: null,
      content: {
        selectedServices: null,
        selectedServiceNames: null,
        selectedMpacks: null
      },
      wizardController: wizardController      
    });

    wizardSelectMpacksController.loadRegistrySucceeded(registry);
  });

  describe('#registryLoaded - after loading registry', function () { 
    it('should have a loaded registry', function () {
      expect(wizardSelectMpacksController.registryLoaded()).to.be.true;
    })
  });

  describe('#loadStep', function () {
    it('adds previously selected services to selection', function () {
      wizardSelectMpacksController.set('content.selectedServices', [
        { id: "HDPCORE1.0.0-b85ZOOKEEPER" },
        { id: "HDPCORE1.0.0-b85HDFS" }
      ]);

      wizardSelectMpacksController.loadStep();
      
      var service = wizardSelectMpacksController.getServiceVersionById("HDPCORE1.0.0-b85ZOOKEEPER");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;

      var service = wizardSelectMpacksController.getServiceVersionById("HDPCORE1.0.0-b85HDFS");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;
    });
  });

  describe('#getMpacksByName', function () {
    it('should return an array of mpacks matching the given names', function () {
      //this test assumes that mpackNames contains the names of all mpacks in the test registry data at the top of this file
      var mpackNames = [
        'HDPCORE',
        'ODS'
      ]

      var expected = wizardSelectMpacksController.get('content.mpacks');
      var actual = wizardSelectMpacksController.getMpacksByName(mpackNames);
      
      expect(actual.length).to.equal(expected.length);
      for (var i = 0, length = actual.length; i < length; i++) {
        expect(actual[i].get('mpack.name')).to.equal(expected[i].get('name'));
      }
    });
  });

  describe('#getServiceVersionById', function () {
    it('should return the correct service', function () {
      var serviceVersions = [
        Em.Object.create({ id: 0 }),
        Em.Object.create({ id: 1 }),
        Em.Object.create({ id: 2 }),
      ];     
      wizardSelectMpacksController.set('content.mpackServiceVersions', serviceVersions);
      
      var actual = wizardSelectMpacksController.getServiceVersionById(1);
      expect(actual).to.equal(serviceVersions[1]);
    });
  });

  describe('#getUsecaseById', function () {
    it('should return the correct use case', function () {
      var usecases = [
        Em.Object.create({ id: 0 }),
        Em.Object.create({ id: 1 }),
        Em.Object.create({ id: 2 }),
      ];
      wizardSelectMpacksController.set('content.mpackUsecases', usecases);

      var actual = wizardSelectMpacksController.getUsecaseById(1);
      expect(actual).to.equal(usecases[1]);
    });
  });

  describe('#getMpackVersionById', function () {
    it('should return the correct mpack', function () {
      var mpackVersions = [
        Em.Object.create({ id: 0 }),
        Em.Object.create({ id: 1 }),
        Em.Object.create({ id: 2 }),
      ];
      wizardSelectMpacksController.set('content.mpackVersions', mpackVersions);

      var actual = wizardSelectMpacksController.getMpackVersionById(1);
      expect(actual).to.equal(mpackVersions[1]);
    });
  });

  describe('#displayMpackVersion', function () {
    var actual = {
      mpack: {
        versions: [
          Em.Object.create({ id: "1", displayed: true }),
          Em.Object.create({ id: "2", displayed: false }),
          Em.Object.create({ id: "3", displayed: false }),
          Em.Object.create({ id: "4", displayed: false }),
        ]
      }
    };

    before(function () {
      sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(actual);
    });

    it('should set chosen mpack version to displayed and set others to not displayed', function () {
      var expected = {
        mpack: {
          versions: [
            Em.Object.create({ id: "1", displayed: false }),
            Em.Object.create({ id: "2", displayed: false }),
            Em.Object.create({ id: "3", displayed: true }),
            Em.Object.create({ id: "4", displayed: false }),
          ]
        }
      };
      
      wizardSelectMpacksController.displayMpackVersion("3");
      expect(actual).to.deep.equal(expected);
    });

    after(function () {
      wizardSelectMpacksController.getMpackVersionById.restore();
    });
  });

  describe('#displayServiceVersion', function () {
    var actual = {
      service: {
        versions: [
          Em.Object.create({ id: "1", displayed: true }),
          Em.Object.create({ id: "2", displayed: false }),
          Em.Object.create({ id: "3", displayed: false }),
          Em.Object.create({ id: "4", displayed: false }),
        ]
      }
    };

    before(function () {
      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);
    });

    it('should set chosen service version to displayed and set others to not displayed', function () {
      var expected = {
        service: {
          versions: [
            Em.Object.create({ id: "1", displayed: false }),
            Em.Object.create({ id: "2", displayed: false }),
            Em.Object.create({ id: "3", displayed: true }),
            Em.Object.create({ id: "4", displayed: false }),
          ]
        }
      };

      wizardSelectMpacksController.displayServiceVersion("3");
      expect(actual).to.deep.equal(expected);
    });

    after(function () {
      wizardSelectMpacksController.getServiceVersionById.restore();
    });
  });

  describe('#addServiceHandler', function () {
    var actual;

    before(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);
      
      actual = Em.Object.create({
        selected: false,
        mpackVersion: {
          selected: false,
          services: [
            actual,
            Em.Object.create({ selected: false })
          ]
        }
      });

      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);
    });
    
    after(function () {
      wizardSelectMpacksController.getServiceVersionById.restore();
    });

    it('should set the service and its mpack to selected and set the step to unsaved', function () {
      var expected = Em.Object.create({
        selected: true,
        mpackVersion: {
          selected: true,
          services: [
            expected,
            Em.Object.create({ selected: false })
          ]
        }
      });
      
      wizardSelectMpacksController.addServiceHandler("HDPCore3.0.0ZOOKEEPER");
      expect(actual).to.deep.equal(expected);
      
      var final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });
      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });
  });

  describe('#removeServiceHandler', function () {
    beforeEach(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);
    });

    afterEach(function () {
      var final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });
      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);

      wizardSelectMpacksController.getServiceVersionById.restore();
    });

    it('should set only the service to not selected and set the step to unsaved', function () {
      var actual = Em.Object.create({
        selected: true,
        mpackVersion: {
          selected: true,
          services: []
        }
      });
      actual.set('mpackVersion.services', [
        actual,
        Em.Object.create({ selected: true })
      ]);

      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);

      wizardSelectMpacksController.removeServiceHandler("HDPCore3.0.0ZOOKEEPER");
      expect(actual.get('selected')).to.be.false;
      expect(actual.get('mpackVersion.selected')).to.be.true;
    });

    it('when removing the last service, it should set the service and its mpack to not selected and set the step to unsaved', function () {
      var actual = Em.Object.create({
        selected: true,
        mpackVersion: {
          selected: true,
          services: []
        }
      });
      actual.set('mpackVersion.services', [
        actual,
        Em.Object.create({ selected: false })
      ]);

      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);

      wizardSelectMpacksController.removeServiceHandler("HDPCore3.0.0ZOOKEEPER");
      expect(actual.get('selected')).to.be.false;
      expect(actual.get('mpackVersion.selected')).to.be.false;
    });
  });

  describe('#addMpackHandler', function () {
    var actual;

    before(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);

      var service0 = Em.Object.create({
        id: 0,
        selected: false
      });

      var service1 = Em.Object.create({
        id: 1,
        selected: false
      });

      actual = Em.Object.create({
        selected: false,
        services: [
          service0,
          service1
        ]
      });

      actual.get('services')[0].set('mpackVersion', actual);
      actual.get('services')[1].set('mpackVersion', actual);

      sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById')
        .withArgs(0).returns(service0)
        .withArgs(1).returns(service1);
    });

    after(function () {
      wizardSelectMpacksController.getMpackVersionById.restore();
      wizardSelectMpacksController.getServiceVersionById.restore();
    });

    it('should set the mpack and all of its services to selected and set the step to unsaved', function () {
      var expected = Em.Object.create({
        selected: true,
        services: [
          Em.Object.create({
            id: 0,
            selected: true
          }),
          Em.Object.create({
            id: 1,
            selected: true
          })
        ]
      });
      expected.get('services')[0].set('mpackVersion', expected);
      expected.get('services')[1].set('mpackVersion', expected);

      wizardSelectMpacksController.addMpackHandler("HDPCore3.0.0");
      expect(actual).to.deep.equal(expected);

      var final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });
      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });
  });

  describe('#removeMpackHandler', function () {
    before(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);

      var service0 = Em.Object.create({
        id: 0,
        selected: true
      });

      var service1 = Em.Object.create({
        id: 1,
        selected: true
      });

      actual = Em.Object.create({
        selected: true,
        services: [
          service0,
          service1
        ]
      });

      actual.get('services')[0].set('mpackVersion', actual);
      actual.get('services')[1].set('mpackVersion', actual);

      sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById')
        .withArgs(0).returns(service0)
        .withArgs(1).returns(service1);
    });

    after(function () {
      wizardSelectMpacksController.getMpackVersionById.restore();
      wizardSelectMpacksController.getServiceVersionById.restore();
    });

    it('should set the mpack and all its services to not selected and set the step to unsaved', function () {
      var expected = Em.Object.create({
        selected: false,
        services: [
          Em.Object.create({
            id: 0,
            selected: false
          }),
          Em.Object.create({
            id: 1,
            selected: false
          })
        ]
      });
      expected.get('services')[0].set('mpackVersion', expected);
      expected.get('services')[1].set('mpackVersion', expected);

      wizardSelectMpacksController.removeMpackHandler("HDPCore3.0.0");
      expect(actual).to.deep.equal(expected);
      
      var final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });
      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });
  });

  describe('#clearSelection', function () {
    var initial, servicesActual, versionsActual, usecasesActual, expected, final;

    beforeEach(function () {
      initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      });
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);

      final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });

      servicesActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackServiceVersions', servicesActual);

      versionsActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackVersions', versionsActual);

      usecasesActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackUsecases', usecasesActual);

      expected = [
        Em.Object.create({ selected: false }),
        Em.Object.create({ selected: false }),
        Em.Object.create({ selected: false })
      ];
    });
    
    it('should set all services and mpacks to be unselected and set the step to unsaved', function () {
      wizardSelectMpacksController.set('content.advancedMode', false);
      wizardSelectMpacksController.clearSelection();
      expect(servicesActual).to.deep.equal(expected);
      expect(versionsActual).to.deep.equal(expected);
       
      //should not be changed
      expect(usecasesActual).to.deep.equal([
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ]);

      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });

    it('should set all usecases to be unselected in advanced mode', function () {
      wizardSelectMpacksController.set('content.advancedMode', true);
      wizardSelectMpacksController.clearSelection();
      expect(servicesActual).to.deep.equal(expected);
      expect(versionsActual).to.deep.equal(expected);
      expect(usecasesActual).to.deep.equal(expected);

      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });
  });

  describe('#submit', function () {
    it('should populate content.selectedServices, content.selectedServiceNames, and content.selectedMpacks', function () {
      wizardSelectMpacksController.set('selectedServices', [
        {
          id: "id1",
          name: "name1",
          mpackVersion: {
            mpack: { name: "mpackName1" },
            name: "mpackName1",
            version: "1.0.0.0"
          }
        },
        {
          id: "id2",
          name: "name2",
          mpackVersion: {
            mpack: { name: "mpackName2" },
            name: "mpackName2",
            version: "1.0.0.0"
          }
        }
      ]);

      wizardSelectMpacksController.set('selectedMpackVersions', [
        {
          mpack: {
            name: "mpackName1",
            displayName: "displayName1"
          },
          mpackUrl: "http://someurl.com/mpack1",
          version: "1.0.0.0"
        },
        {
          mpack: {
            name: "mpackName2",
            displayName: "displayName2"
          },
          mpackUrl: "http://someurl.com/mpack2",
          version: "1.0.0.0"
        }
      ]);
      
      var expectedSelectedServices = [
        {
          id: "id1",
          name: "name1",
          mpackName: "mpackName1",
          mpackVersion: "1.0.0.0"
        },
        {
          id: "id2",
          name: "name2",
          mpackName: "mpackName2",
          mpackVersion: "1.0.0.0"
        }
      ];

      var expectedSelectedServiceNames = [
        "name1",
        "name2"
      ];

      var expectedSelectedMpacks = [
        {
          id: "mpackName1-1.0.0.0",
          name: "mpackName1",
          displayName: "displayName1",
          publicUrl: "http://someurl.com/mpack1",
          downloadUrl: "http://someurl.com/mpack1",
          version: "1.0.0.0"
        },
        {
          id: "mpackName2-1.0.0.0",
          name: "mpackName2",
          displayName: "displayName2",
          publicUrl: "http://someurl.com/mpack2",
          downloadUrl: "http://someurl.com/mpack2",
          version: "1.0.0.0"
        }
      ];

      sinon.stub(App.router, 'send');
      wizardSelectMpacksController.submit();
      App.router.send.restore();
      
      expect(wizardSelectMpacksController.get('content.selectedServices')).to.deep.equal(expectedSelectedServices);
      expect(wizardSelectMpacksController.get('content.selectedServiceNames')).to.deep.equal(expectedSelectedServiceNames);
      expect(wizardSelectMpacksController.get('content.selectedMpacks')).to.deep.equal(expectedSelectedMpacks);
    });
  });

  describe('#toggleUsecaseHandler', function () {
    beforeEach(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);

      sinon.stub(wizardSelectMpacksController, 'getUsecaseRecommendation').returns({
        done: sinon.stub().returns({ fail: sinon.stub() })
      });
    });

    afterEach(function () {
      var final = Em.Object.create({
        "0": true,
        "1": false,
        "2": true,
        "3": true,
        "4": true
      });
      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);

      wizardSelectMpacksController.getUsecaseById.restore();
      wizardSelectMpacksController.getUsecaseRecommendation.restore();
    });

    it('should select usecase when it is not already selected', function () {
      var actual = Em.Object.create({
        selected: false
      });

      wizardSelectMpacksController.set('content.mpackUsecases', [
        Em.Object.create({
          selected: false
        }),
        actual
      ]);

      sinon.stub(wizardSelectMpacksController, 'getUsecaseById').returns(actual);

      wizardSelectMpacksController.toggleUsecaseHandler("DataScience");
      expect(actual.get('selected')).to.be.true;
      expect(wizardSelectMpacksController.getUsecaseRecommendation).to.have.been.called;
    });

    it('should deselect use case when it is already selected', function () {
      var actual = Em.Object.create({
        selected: true
      });
      
      wizardSelectMpacksController.set('content.mpackUsecases', [
        Em.Object.create({
          selected: true
        }),
        actual
      ]);
      
      sinon.stub(wizardSelectMpacksController, 'getUsecaseById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'clearSelection');

      wizardSelectMpacksController.toggleUsecaseHandler("DataScience");
      expect(actual.get('selected')).to.be.false;
      expect(wizardSelectMpacksController.getUsecaseRecommendation).to.have.been.called;
      
      wizardSelectMpacksController.clearSelection.restore();
    });

    it('should not call getUsecaseRecommendation when no use cases are selected', function () {
      var actual = Em.Object.create({
        selected: true
      });

      wizardSelectMpacksController.set('content.mpackUsecases', [
        Em.Object.create({
          selected: false
        }),
        actual
      ]);

      sinon.stub(wizardSelectMpacksController, 'getUsecaseById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'clearSelection');

      wizardSelectMpacksController.toggleUsecaseHandler("DataScience");
      expect(actual.get('selected')).to.be.false;
      expect(wizardSelectMpacksController.getUsecaseRecommendation).to.not.have.been.called;

      wizardSelectMpacksController.clearSelection.restore();
    });
  });

  describe('#toggleMode', function () {
    it('should set mode to advanced', function () {
      wizardSelectMpacksController.set('content.advancedMode', false);
      wizardSelectMpacksController.toggleMode();
      expect(wizardSelectMpacksController.get('content.advancedMode')).to.be.true;
    });

    it('should set mode to basic', function () {
      wizardSelectMpacksController.set('content.advancedMode', true);
      sinon.stub(wizardSelectMpacksController, 'clearSelection');

      wizardSelectMpacksController.toggleMode();

      expect(wizardSelectMpacksController.get('content.advancedMode')).to.be.false;
      expect(wizardSelectMpacksController.clearSelection).to.have.been.called

      wizardSelectMpacksController.clearSelection.restore();
    });
  });

  describe('#filteredMpacks', function () {
    var mpacks = [
      Em.Object.create({
        filterOn: "alphabravo"
      }),
      Em.Object.create({
        filterOn: "alphacharlie"
      }),
      Em.Object.create({
        filterOn: "bravodelta"
      })
    ];
    
    beforeEach(function () {
      wizardSelectMpacksController.set('content.mpacks', mpacks);
      wizardSelectMpacksController.set('filterMpacksText', 'alpha');
    })

    it('should return the correctly filtered mpacks', function () {
      var expected = [
        Em.Object.create({
          filterOn: "alphabravo"
        }),
        Em.Object.create({
          filterOn: "alphacharlie"
        })
      ];

      var actual = wizardSelectMpacksController.get('filteredMpacks');

      expect(actual).to.deep.equal(expected);
    });

    it('should return all mpacks when there is no filter text', function () {
      wizardSelectMpacksController.clearFilterMpacks();

      var actual = wizardSelectMpacksController.get('filteredMpacks');

      expect(actual).to.deep.equal(mpacks);
    });
  });

  describe('#filteredServices', function () {
    var services = [
      Em.Object.create({
        filterOn: "alphabravo"
      }),
      Em.Object.create({
        filterOn: "alphacharlie"
      }),
      Em.Object.create({
        filterOn: "bravodelta"
      })
    ];

    beforeEach(function () {
      wizardSelectMpacksController.set('content.mpackServices', services);
      wizardSelectMpacksController.set('filterServicesText', 'alpha');
    })

    it('should return the correctly filtered services', function () {
      var expected = [
        Em.Object.create({
          filterOn: "alphabravo"
        }),
        Em.Object.create({
          filterOn: "alphacharlie"
        })
      ];

      var actual = wizardSelectMpacksController.get('filteredServices');

      expect(actual).to.deep.equal(expected);
    });

    it('should return all services when there is no filter text', function () {
      wizardSelectMpacksController.clearFilterServices();

      var actual = wizardSelectMpacksController.get('filteredServices');

      expect(actual).to.deep.equal(services);
    });
  });
});
