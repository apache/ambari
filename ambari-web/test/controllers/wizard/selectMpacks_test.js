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
      content: Em.Object.create({
        selectedServices: null,
        selectedServiceNames: null,
        selectedMpacks: null
      }),
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
      // wizardSelectMpacksController.set('content.selectedServices', [
      //   { id: "HDPCORE1.0.0-b85ZOOKEEPER" },
      //   { id: "HDPCORE1.0.0-b85HDFS" }
      // ]);

      // wizardSelectMpacksController.loadStep();
      
      // var service = wizardSelectMpacksController.getServiceVersionById("HDPCORE1.0.0-b85ZOOKEEPER");
      // expect(service.get('selected')).to.be.true;
      // expect(service.get('mpackVersion.selected')).to.be.true;

      // var service = wizardSelectMpacksController.getServiceVersionById("HDPCORE1.0.0-b85HDFS");
      // expect(service.get('selected')).to.be.true;
      // expect(service.get('mpackVersion.selected')).to.be.true;
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

  describe('#getUseCaseById', function () {
    it('should return the correct use case', function () {
      var useCases = [
        Em.Object.create({ id: 0 }),
        Em.Object.create({ id: 1 }),
        Em.Object.create({ id: 2 }),
      ];
      wizardSelectMpacksController.set('content.mpackUseCases', useCases);

      var actual = wizardSelectMpacksController.getUseCaseById(1);
      expect(actual).to.equal(useCases[1]);
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
    var actual = Em.Object.create({
      mpack: {
        versions: [
          Em.Object.create({ id: "1", displayed: true }),
          Em.Object.create({ id: "2", displayed: false }),
          Em.Object.create({ id: "3", displayed: false }),
          Em.Object.create({ id: "4", displayed: false }),
        ]
      }
    });

    before(function () {
      sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(actual);
    });

    it('should set chosen mpack version to displayed and set others to not displayed', function () {
      var expected = Em.Object.create({
        mpack: {
          versions: [
            Em.Object.create({ id: "1", displayed: false }),
            Em.Object.create({ id: "2", displayed: false }),
            Em.Object.create({ id: "3", displayed: true }),
            Em.Object.create({ id: "4", displayed: false }),
          ]
        }
      });
      
      wizardSelectMpacksController.displayMpackVersion("3");
      expect(actual).to.deep.equal(expected);
    });

    after(function () {
      wizardSelectMpacksController.getMpackVersionById.restore();
    });
  });

  describe('#displayServiceVersion', function () {
    var actual = Em.Object.create({
      service: {
        versions: [
          Em.Object.create({ id: "1", displayed: true }),
          Em.Object.create({ id: "2", displayed: false }),
          Em.Object.create({ id: "3", displayed: false }),
          Em.Object.create({ id: "4", displayed: false }),
        ]
      }
    });

    before(function () {
      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);
    });

    it('should set chosen service version to displayed and set others to not displayed', function () {
      var expected = Em.Object.create({
        service: {
          versions: [
            Em.Object.create({ id: "1", displayed: false }),
            Em.Object.create({ id: "2", displayed: false }),
            Em.Object.create({ id: "3", displayed: true }),
            Em.Object.create({ id: "4", displayed: false }),
          ]
        }
      });

      wizardSelectMpacksController.displayServiceVersion("3");
      expect(actual).to.deep.equal(expected);
    });

    after(function () {
      wizardSelectMpacksController.getServiceVersionById.restore();
    });
  });

  describe('#addMpackVersion', function () {
    it('should set mpack version as selected', function () {
      var actual = Em.Object.create({
        selected: false
      });

      var expected = Em.Object.create({
        selected: true
      });

      sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(actual);
      wizardSelectMpacksController.addMpackVersion("anyId");
      wizardSelectMpacksController.getMpackVersionById.restore();

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#removeMpackVersion', function () {
    it('should set mpack version as not selected and deselect all of its services', function () {
      var actual = Em.Object.create({
        selected: true,
        services: [
          Em.Object.create({ selected: true }),
          Em.Object.create({ selected: true })
        ]
      });

      var expected = Em.Object.create({
        selected: false,
        services: [
          Em.Object.create({ selected: false }),
          Em.Object.create({ selected: false })
        ]
      });

      wizardSelectMpacksController.removeMpackVersion(actual);

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#addServiceVersion', function () {
    it('should set the service version as selected', function () {
      var actual = Em.Object.create({
        selected: false
      });

      var expected = Em.Object.create({
        selected: true
      });

      sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(actual);
      wizardSelectMpacksController.addServiceVersion();
      wizardSelectMpacksController.getServiceVersionById.restore();

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#removeServiceVersion', function () {
    it('should set the service version as selected', function () {
      var actual = Em.Object.create({
        selected: true
      });

      var expected = Em.Object.create({
        selected: false
      });

      wizardSelectMpacksController.removeServiceVersion(actual);

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#createServiceGroup', function () {
    it('should construct a service group object', function () {
      var name = 'name';
      var mpackVersion = 'mpackVersion';
      var canRemove = true;

      var expected = Em.Object.create({
        name: name,
        mpackVersion: mpackVersion,
        canRemove: canRemove
      });

      var actual = wizardSelectMpacksController.createServiceGroup(name, mpackVersion, canRemove);
      delete actual.serviceInstances; //this property is a function with complicated dependencies so we can't really mock it up for the comparison

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#addServiceGroup', function () {
    it('should add a service group to addedServiceGroups', function () {
      var mpackVersion = Em.Object.create({
        name: 'service',
        selected: true
      });
      sinon.stub(wizardSelectMpacksController, 'addMpackVersion').returns(mpackVersion);
      
      var serviceGroupName = 'serviceGroup';
      var serviceGroup = Em.Object.create({
        name: serviceGroupName,
        mpackVersion: mpackVersion,
        canRemove: true
      })
      sinon.stub(wizardSelectMpacksController, 'createServiceGroup').returns(serviceGroup);

      var addedServiceGroups = wizardSelectMpacksController.get('addedServiceGroups');
      var countBefore = addedServiceGroups.length;

      wizardSelectMpacksController.addServiceGroup('mpackVersionId', serviceGroupName);

      wizardSelectMpacksController.addMpackVersion.restore();
      wizardSelectMpacksController.createServiceGroup.restore();

      expect(addedServiceGroups.length).to.equal(countBefore + 1);
      expect(addedServiceGroups[countBefore]).to.equal(serviceGroup) //last object pushed should be serviceGroup
    });

    it('should not add a duplicate service group to addedServiceGroups', function () {
      var mpackVersion = Em.Object.create({
        name: 'service',
        selected: true
      });
      sinon.stub(wizardSelectMpacksController, 'addMpackVersion').returns(mpackVersion);
      
      var serviceGroupName = 'serviceGroupDupe';
      var expected = Em.Object.create({
        name: serviceGroupName,
        mpackVersion: mpackVersion,
        canRemove: true
      })
      sinon.stub(wizardSelectMpacksController, 'createServiceGroup').returns(expected);
      //prime with the duplicate
      wizardSelectMpacksController.addServiceGroup('mpackVersionId', serviceGroupName);

      var addedServiceGroups = wizardSelectMpacksController.get('addedServiceGroups');
      var countBefore = addedServiceGroups.length;

      //now try adding the same one again
      wizardSelectMpacksController.addServiceGroup('mpackVersionId', serviceGroupName);

      wizardSelectMpacksController.addMpackVersion.restore();
      wizardSelectMpacksController.createServiceGroup.restore();

      expect(addedServiceGroups.length).to.equal(countBefore); //count should not change
    });
  });

  describe('#removeServiceGroup', function () {
    it('should remove the service group from addedServiceGroups', function () {
      var serviceGroup = Em.Object.create({
        name: 'sg1',
        canRemove: true,
        serviceInstances: []
      });

      wizardSelectMpacksController.set('addedServiceGroups', [
        serviceGroup,  
        Em.Object.create({
          name: 'sg2'
        })
      ]);

      wizardSelectMpacksController.removeServiceGroup('sg1');
      expect(wizardSelectMpacksController.get('addedServiceGroups').length).to.equal(1);
    });
  });

  describe('#createServiceInstance', function () {
    it('should construct a service instance object', function () {
      var name = 'name';
      var serviceGroup = Em.Object.create({ name: 'sg1' });
      var service = 'service';
      var canRemove = true;

      var expected = Em.Object.create({
        id: serviceGroup.get('name') + name,
        name: name,
        service: service,
        serviceGroup: serviceGroup,
        canRemove: canRemove
      });

      var actual = wizardSelectMpacksController.createServiceInstance(name, service, serviceGroup, canRemove);

      expect(actual).to.deep.equal(expected);
    });
  });

  describe('#addServiceInstance', function () {
    it('should add a service instance to addedServiceInstances', function () {
      var serviceName = 'service';
      var serviceGroup = Em.Object.create({
        name: 'group',
        mpackVersion: Em.Object.create({
          name: 'mpack',
          selected: true,
          services: [
            Em.Object.create({
              name: serviceName
            })
          ]
        }),
        serviceInstances: []
      });

      var serviceInstanceName = 'instance';
      var serviceInstance = Em.Object.create({
        id: serviceGroup.get('name') + serviceInstanceName,
        name: serviceInstanceName,
        service: Em.Object.create({
          name: serviceName
        }),
        serviceGroup: serviceGroup,
        canRemove: true
      });
      sinon.stub(wizardSelectMpacksController, 'createServiceInstance').returns(serviceInstance);

      var addedServiceInstances = wizardSelectMpacksController.get('addedServiceInstances');
      var countBefore = addedServiceInstances.length;

      wizardSelectMpacksController.addServiceInstance(serviceName, serviceInstanceName, serviceGroup);

      wizardSelectMpacksController.createServiceInstance.restore();

      expect(addedServiceInstances.length).to.equal(countBefore + 1);
      expect(addedServiceInstances[countBefore]).to.equal(serviceInstance) //last object pushed should be serviceInstance
    });

    it('should not add a duplicate service instance to addedServiceInstances', function () {
      var serviceName = 'service';
      var serviceInstanceName = 'instance';
      
      var serviceGroup = Em.Object.create({
        name: 'group',
        mpackVersion: Em.Object.create({
          name: 'mpack',
          selected: true,
          services: [
            Em.Object.create({
              name: serviceName
            })
          ]
        }),
        //note this is not really how a serviceGroup object works in the real code
        //serviceInstances is a FUNCTION that filters addedServiceInstances rather than an array
        //but we need to mock this up this way for test purposes
        serviceInstances: [
          Em.Object.create({ name: serviceInstanceName })
        ]
      });

      var serviceInstance = Em.Object.create({
        id: serviceGroup.get('name') + serviceInstanceName,
        name: serviceInstanceName,
        service: Em.Object.create({
          name: serviceName
        }),
        serviceGroup: serviceGroup,
        canRemove: true
      });
      sinon.stub(wizardSelectMpacksController, 'createServiceInstance').returns(serviceInstance);

      //prime with the duplicate
      wizardSelectMpacksController.addServiceInstance(serviceName, serviceInstanceName, serviceGroup);
      var addedServiceInstances = wizardSelectMpacksController.get('addedServiceInstances');
      var countBefore = addedServiceInstances.length;

      //now try adding the same one again
      wizardSelectMpacksController.addServiceInstance(serviceName, serviceInstanceName, serviceGroup);

      wizardSelectMpacksController.createServiceInstance.restore();

      expect(addedServiceInstances.length).to.equal(countBefore); //count should not change
    });
  });

  describe('#removeServiceInstance', function () {
    it('should remove a service instance from addedServiceInstances', function () {
      var serviceInstance = Em.Object.create({
        id: 'si1',
        name: 'si1',
        canRemove: true
      });
      
      var serviceGroup = Em.Object.create({
        //note this is not really how a serviceGroup object works in the real code
        //serviceInstances is a FUNCTION that filters addedServiceInstances rather than an array
        //but we need to mock this up this way for test purposes
        serviceInstances: [
          serviceInstance
        ]
      });
      
      wizardSelectMpacksController.set('addedServiceInstances', [
        serviceInstance,  
        Em.Object.create({
          id: 'si2',
          name: 'si2'
        })
      ]);

      wizardSelectMpacksController.removeServiceInstance('si1', serviceGroup);
      expect(wizardSelectMpacksController.get('addedServiceInstances').length).to.equal(1);
    });
  });

  describe('#addMpackHandler', function () {
    it('should make all calls required to add an mpack', function () {
      var stub1 = sinon.stub(wizardSelectMpacksController, 'addMpackVersion').returns(Em.Object.create({
        services: [
          Em.Object.create({}),
          Em.Object.create({}),
          Em.Object.create({})
        ]
      }));
      var stub2 = sinon.stub(wizardSelectMpacksController, 'addServiceGroup').returns(true);
      var stub3 = sinon.stub(wizardSelectMpacksController, 'addServiceInstance');
      var stub4 = sinon.stub(wizardController, 'setStepUnsaved');

      wizardSelectMpacksController.addMpackHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.calledThrice).to.be.true;
      expect(stub4.called).to.be.true;

      wizardSelectMpacksController.addMpackVersion.restore();
      wizardSelectMpacksController.addServiceGroup.restore();
      wizardSelectMpacksController.addServiceInstance.restore();
      wizardController.setStepUnsaved.restore();
    });
  });

  describe('#removeMpackHandler', function () {
    it('should make all calls required to remove an mpack', function () {
      var stub1 = sinon.stub(wizardSelectMpacksController, 'getMpackVersionById').returns(Em.Object.create({}));
      var stub2 = sinon.stub(wizardSelectMpacksController, 'removeServiceGroup').returns(true);
      var stub3 = sinon.stub(wizardSelectMpacksController, 'removeMpackVersion');
      var stub4 = sinon.stub(wizardController, 'setStepUnsaved');

      wizardSelectMpacksController.removeMpackHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.called).to.be.true;
      expect(stub4.called).to.be.true;

      wizardSelectMpacksController.getMpackVersionById.restore();
      wizardSelectMpacksController.removeServiceGroup.restore();
      wizardSelectMpacksController.removeMpackVersion.restore();
      wizardController.setStepUnsaved.restore();
    });
  });

  describe('#addServiceHandler', function () {
    var stub1, stub2, stub4, stub5;
    
    beforeEach(function () {
      stub1 = sinon.stub(wizardSelectMpacksController, 'addServiceVersion').returns(Em.Object.create({
        mpackVersion: Em.Object.create({})
      }));
      stub2 = sinon.stub(wizardSelectMpacksController, 'addMpackVersion').returns(Em.Object.create({}));
      stub4 = sinon.stub(wizardSelectMpacksController, 'addServiceInstance');
      stub5 = sinon.stub(wizardController, 'setStepUnsaved');
    });

    afterEach(function () {
      wizardSelectMpacksController.addServiceVersion.restore();
      wizardSelectMpacksController.addMpackVersion.restore();
      wizardSelectMpacksController.addServiceInstance.restore();
      wizardSelectMpacksController.getServiceGroup.restore();
      wizardSelectMpacksController.addServiceGroup.restore();
      wizardController.setStepUnsaved.restore();
    });

    it('should make all calls required to add a service to an existing service group', function () {
      var stub3 = sinon.stub(wizardSelectMpacksController, 'getServiceGroup').returns(true);
      var stub3a = sinon.stub(wizardSelectMpacksController, 'addServiceGroup').returns(true);

      wizardSelectMpacksController.addServiceHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.called).to.be.true;
      expect(stub3a.called).to.be.false;
      expect(stub4.called).to.be.true;
      expect(stub5.called).to.be.true;
    });

    it('should make all calls required to add a service to a new service group', function () {
      var stub3 = sinon.stub(wizardSelectMpacksController, 'getServiceGroup').returns(false);
      var stub3a = sinon.stub(wizardSelectMpacksController, 'addServiceGroup').returns(true);

      wizardSelectMpacksController.addServiceHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.called).to.be.true;
      expect(stub3a.called).to.be.true;
      expect(stub4.called).to.be.true;
      expect(stub5.called).to.be.true;
    });
  });

  describe('#removeServiceHandler', function () {
    var stub1, stub3, stub4, stub5, stub6;

    beforeEach(function () {
      stub1 = sinon.stub(wizardSelectMpacksController, 'getServiceVersionById').returns(Em.Object.create({}));
      stub3 = sinon.stub(wizardSelectMpacksController, 'removeServiceInstance').returns(true);
      stub4 = sinon.stub(wizardSelectMpacksController, 'removeServiceVersion');
      stub5 = sinon.stub(wizardSelectMpacksController, 'removeMpackHandler');
      stub6 = sinon.stub(wizardController, 'setStepUnsaved');
    });

    afterEach(function () {
      wizardSelectMpacksController.getServiceVersionById.restore();
      wizardSelectMpacksController.removeServiceInstance.restore();
      wizardSelectMpacksController.removeServiceVersion.restore();
      wizardSelectMpacksController.removeMpackHandler.restore();
      wizardController.setStepUnsaved.restore();
    });

    it('should make all calls required to remove a service instance', function () {
      var stub2 = sinon.stub(wizardSelectMpacksController, 'getServiceGroup').returns(Em.Object.create({
        //note this is not really how a serviceGroup object works in the real code
        //serviceInstances is a FUNCTION that filters addedServiceInstances rather than an array
        //but we need to mock this up this way for test purposes
        serviceInstances: [{}]
      }));

      wizardSelectMpacksController.removeServiceHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.called).to.be.true;
      expect(stub4.called).to.be.true;
      expect(stub5.called).to.be.false;
      expect(stub6.called).to.be.true;

      wizardSelectMpacksController.getServiceGroup.restore();
    });

    it('when removing the last service instance, it should also make the call to remove the service group', function () {
      var stub2 = sinon.stub(wizardSelectMpacksController, 'getServiceGroup').returns(Em.Object.create({
        //note this is not really how a serviceGroup object works in the real code
        //serviceInstances is a FUNCTION that filters addedServiceInstances rather than an array
        //but we need to mock this up this way for test purposes
        serviceInstances: []
      }));

      wizardSelectMpacksController.removeServiceHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
      expect(stub3.called).to.be.true;
      expect(stub4.called).to.be.true;
      expect(stub5.called).to.be.true;
      expect(stub6.called).to.be.true;

      wizardSelectMpacksController.getServiceGroup.restore();
    });
  });

  describe('#removeServiceGroupHandler', function () {
    it('should make all calls required to remove a service group', function () {
      var stub1 = sinon.stub(wizardSelectMpacksController, 'getServiceGroup').returns(Em.Object.create({}));
      var stub2 = sinon.stub(wizardSelectMpacksController, 'removeMpackHandler');

      wizardSelectMpacksController.removeServiceGroupHandler();

      expect(stub1.called).to.be.true;
      expect(stub2.called).to.be.true;
    });
  });

  describe('#removeServiceInstanceHandler', function () {
    it('should make all calls required to remove a service instance', function () {
      sinon.stub(wizardSelectMpacksController, 'get').returns([
        Em.Object.create({
          serviceInstances: [
            Em.Object.create({
              id: '0'
            })
          ]
        })
      ]);
      var stub1 = sinon.stub(wizardSelectMpacksController, 'removeServiceHandler');
      
      wizardSelectMpacksController.removeServiceInstanceHandler('0');

      expect(stub1.called).to.be.true;

      wizardSelectMpacksController.get.restore();
      wizardSelectMpacksController.removeServiceHandler.restore();
    });
  });

  describe('#clearSelection', function () {
    var initial, servicesActual, versionsActual, useCasesActual, expected, final;

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

      useCasesActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackUseCases', useCasesActual);

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
      expect(useCasesActual).to.deep.equal([
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ]);

      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });

    it('should set all useCases to be unselected in advanced mode', function () {
      wizardSelectMpacksController.set('content.advancedMode', true);
      wizardSelectMpacksController.clearSelection();
      expect(servicesActual).to.deep.equal(expected);
      expect(versionsActual).to.deep.equal(expected);
      expect(useCasesActual).to.deep.equal(expected);

      var savedState = wizardSelectMpacksController.get('wizardController.content.stepsSavedState');
      expect(savedState).to.deep.equal(final);
    });
  });

  describe('#submit', function () {
    it('should populate content.addedServiceGroups, content.addedServiceInstances, and content.selectedMpacks', function () {
      wizardSelectMpacksController.set('addedServiceGroups', [
        Em.Object.create({
          name: 'name',
          mpackVersion: Em.Object.create({
            mpack: Em.Object.create({
              name: 'mpackName'
            }),
            version: 'version'
          })
        })
      ]);

      wizardSelectMpacksController.set('addedServiceInstances', [
        Em.Object.create({
          name: 'instanceName',
          service: Em.Object.create({
            id: '0',
            name: 'name',
            mpackVersion: Em.Object.create({
              mpack: Em.Object.create({
                name: 'mpackName'
              }),
              version: 'version'
            })
          }),
          serviceGroup: Em.Object.create({
            name: 'groupName'
          })
        })
      ]);
      
      wizardSelectMpacksController.set('selectedMpackVersions', [
        Em.Object.create({
          mpack: {
            name: "mpackName1",
            displayName: "displayName1",
            registryId: 1
          },
          mpackUrl: "http://someurl.com/mpack1",
          version: "1.0.0.0"
        }),
        Em.Object.create({
          mpack: {
            name: "mpackName2",
            displayName: "displayName2",
            registryId: 1
          },
          mpackUrl: "http://someurl.com/mpack2",
          version: "1.0.0.0"
        })
      ]);
      
      var expectedAddedServiceGroups = [
        {
          name: 'name',
          mpackName: 'mpackName',
          mpackVersion: 'version'
        }
      ];

      var expectedAddedServiceInstances = [
        {
          id: '0',
          name: 'instanceName',
          serviceGroupName: 'groupName',
          serviceName: 'name',
          mpackName: 'mpackName',
          mpackVersion: 'version'
        }
      ];

      var expectedSelectedMpacks = [
        {
          id: "mpackName1-1.0.0.0",
          name: "mpackName1",
          displayName: "displayName1",
          publicUrl: "http://someurl.com/mpack1",
          downloadUrl: "http://someurl.com/mpack1",
          version: "1.0.0.0",
          registryId: 1
        },
        {
          id: "mpackName2-1.0.0.0",
          name: "mpackName2",
          displayName: "displayName2",
          publicUrl: "http://someurl.com/mpack2",
          downloadUrl: "http://someurl.com/mpack2",
          version: "1.0.0.0",
          registryId: 1
        }
      ];

      sinon.stub(App.router, 'send');
      wizardSelectMpacksController.submit();
      App.router.send.restore();
      
      expect(wizardSelectMpacksController.get('content.addedServiceGroups')).to.deep.equal(expectedAddedServiceGroups);
      expect(wizardSelectMpacksController.get('content.addedServiceInstances')).to.deep.equal(expectedAddedServiceInstances);
      expect(wizardSelectMpacksController.get('content.selectedMpacks')).to.deep.equal(expectedSelectedMpacks);
    });
  });

  describe('#toggleUseCaseHandler', function () {
    beforeEach(function () {
      var initial = Em.Object.create({
        "0": true,
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      wizardSelectMpacksController.set('wizardController.content.stepsSavedState', initial);

      sinon.stub(wizardSelectMpacksController, 'getUseCaseRecommendation').returns({
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

      wizardSelectMpacksController.getUseCaseById.restore();
      wizardSelectMpacksController.getUseCaseRecommendation.restore();
    });

    it('should select useCase when it is not already selected', function () {
      var actual = Em.Object.create({
        selected: false
      });

      wizardSelectMpacksController.set('content.mpackUseCases', [
        Em.Object.create({
          selected: false
        }),
        actual
      ]);

      sinon.stub(wizardSelectMpacksController, 'getUseCaseById').returns(actual);

      wizardSelectMpacksController.toggleUseCaseHandler("DataScience");
      expect(actual.get('selected')).to.be.true;
      expect(wizardSelectMpacksController.getUseCaseRecommendation).to.have.been.called;
    });

    it('should deselect use case when it is already selected', function () {
      var actual = Em.Object.create({
        selected: true
      });
      
      wizardSelectMpacksController.set('content.mpackUseCases', [
        Em.Object.create({
          selected: true
        }),
        actual
      ]);
      
      sinon.stub(wizardSelectMpacksController, 'getUseCaseById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'clearSelection');

      wizardSelectMpacksController.toggleUseCaseHandler("DataScience");
      expect(actual.get('selected')).to.be.false;
      expect(wizardSelectMpacksController.getUseCaseRecommendation).to.have.been.called;
      
      wizardSelectMpacksController.clearSelection.restore();
    });

    it('should not call getUseCaseRecommendation when no use cases are selected', function () {
      var actual = Em.Object.create({
        selected: true
      });

      wizardSelectMpacksController.set('content.mpackUseCases', [
        Em.Object.create({
          selected: false
        }),
        actual
      ]);

      sinon.stub(wizardSelectMpacksController, 'getUseCaseById').returns(actual);
      sinon.stub(wizardSelectMpacksController, 'clearSelection');

      wizardSelectMpacksController.toggleUseCaseHandler("DataScience");
      expect(actual.get('selected')).to.be.false;
      expect(wizardSelectMpacksController.getUseCaseRecommendation).to.not.have.been.called;

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
