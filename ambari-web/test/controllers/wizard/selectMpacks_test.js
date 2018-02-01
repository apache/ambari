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
  "href": "http://localhost:8080/api/v1/registries?fields=mpacks/*,mpacks/versions/RegistryMpackVersionInfo/*",
  "items": [
    {
      "href": "http://localhost:8080/api/v1/registries/1",
      "RegistryInfo": {
        "registry_id": 1
      },
      "mpacks": [
        {
          "href": "http://localhost:8080/api/v1/registries/1/mpacks/EDW",
          "RegistryMpackInfo": {
            "mpack_description": "Buzz word buzz word, buzz word buzz word buzz word buzz wording buzzy buzz word.",
            "mpack_display_name": null,
            "mpack_logo_url": "https://public-repo-1.hortonworks.com/logos/edw-logo.png",
            "mpack_name": "EDW",
            "registry_id": 1
          },
          "versions": [
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/EDW/versions/1.0.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": [
                  {
                    "name": "HDPCore",
                    "minVersion": "3.2.0",
                    "maxVersion": "3.2.99"
                  }
                ],
                "mpack_buildnum": "1.0.0.0-111",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/EDW1/EDW-1.0.0/index.html",
                "mpack_name": "EDW",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/EDW/1.0.0.0-111/edw-ambari-mpack-1.0.0.0-111.tar.gz",
                "mpack_version": "1.0.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HBASE",
                    "version": "2.0.0"
                  },
                  {
                    "name": "HIVE",
                    "version": "2.0.0"
                  }
                ]
              }
            }
          ]
        },
        {
          "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore",
          "RegistryMpackInfo": {
            "mpack_description": "The latest Hortonworks release for Hortonworks Data Platlform Core (HDFS, ZooKeeper, YARN, MapReduce 2) and Hortonworks SmartSense™.",
            "mpack_display_name": null,
            "mpack_logo_url": "https://public-repo-1.hortonworks.com/logos/hdpcore-logo.png",
            "mpack_name": "HDPCore",
            "registry_id": 1
          },
          "versions": [
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.0.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.0.0.0-247",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.0.0/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "http://localhost:8080/resources/mpack-repo/hdp-ambari-mpack-3.0.0.0-247.tar.gz",
                "mpack_version": "3.0.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.1.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.1.0.0-234",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.1.0/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.1.0.0-234/hdpcore-ambari-mpack-3.1.0.0-234.tar.gz",
                "mpack_version": "3.1.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.1.1",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.1.1.0-111",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.1.1/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.1.1.0-111/hdpcore-ambari-mpack-3.1.1.0-111.tar.gz",
                "mpack_version": "3.1.1",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.2.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.2.0.0-345",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.2.0/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.2.0.0-345/hdpcore-ambari-mpack-3.2.0.0-345.tar.gz",
                "mpack_version": "3.2.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.2.1",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.2.1.0-333",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.2.1/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.2.1.0-333/hdpcore-ambari-mpack-3.2.1.0-333.tar.gz",
                "mpack_version": "3.2.1",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.3.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.3.0.0-456",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.3.0/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.3.0.0-456/hdpcore-ambari-mpack-3.3.0.0-456.tar.gz",
                "mpack_version": "3.3.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDPCore/versions/3.4.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": null,
                "mpack_buildnum": "3.4.0.0-567",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDPCore3/HDPCore-3.4.0/index.html",
                "mpack_name": "HDPCore",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDPCore/3.4.0.0-567/hdpcore-ambari-mpack-3.4.0.0-567.tar.gz",
                "mpack_version": "3.4.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "HDFS",
                    "version": "3.0.0"
                  },
                  {
                    "name": "ZOOKEEPER",
                    "version": "3.0.0"
                  }
                ]
              }
            }
          ]
        },
        {
          "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDS",
          "RegistryMpackInfo": {
            "mpack_description": "Buzz word buzz word, buzz word buzz word buzz word buzz wording buzzy buzz word.",
            "mpack_display_name": null,
            "mpack_logo_url": "https://public-repo-1.hortonworks.com/logos/hds-logo.png",
            "mpack_name": "HDS",
            "registry_id": 1
          },
          "versions": [
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDS/versions/3.0.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": [
                  {
                    "name": "HDPCore",
                    "minVersion": "3.1.0",
                    "maxVersion": "3.2.99"
                  }
                ],
                "mpack_buildnum": "3.0.0.0-247",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDS4/HDS-4.3.0/index.html",
                "mpack_name": "HDS",
                "mpack_url": "http://localhost:8080/resources/mpack-repo/hds-ambari-mpack-3.0.0.0-247.tar.gz",
                "mpack_version": "3.0.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "SPARK",
                    "version": "2.0.0"
                  },
                  {
                    "name": "ZEPPELIN",
                    "version": "1.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDS/versions/4.3.0",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": [
                  {
                    "name": "HDPCore",
                    "minVersion": "3.1.0",
                    "maxVersion": "3.2.99"
                  }
                ],
                "mpack_buildnum": "4.3.0.0-444",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDS4/HDS-4.3.0/index.html",
                "mpack_name": "HDS",
                "mpack_url": "file:///var/lib/ambari-server/resources/mpack-repo/hds-ambari-mpack-4.3.0.0-444.tar.gz",
                "mpack_version": "4.3.0",
                "registry_id": 1,
                "services": [
                  {
                    "name": "SPARK",
                    "version": "2.0.0"
                  },
                  {
                    "name": "ZEPPELIN",
                    "version": "1.0.0"
                  }
                ]
              }
            },
            {
              "href": "http://localhost:8080/api/v1/registries/1/mpacks/HDS/versions/4.3.1",
              "RegistryMpackVersionInfo": {
                "compatible_mpacks": [
                  {
                    "name": "HDPCore",
                    "minVersion": "3.2.0",
                    "maxVersion": "3.3.99"
                  }
                ],
                "mpack_buildnum": "4.3.1.0-555",
                "mpack_doc_url": "http://docs.hortonworks.com/HDPDocuments/HDS4/HDS-4.3.1/index.html",
                "mpack_name": "HDS",
                "mpack_url": "https://public-repo-1.hortonworks.com/mpacks/HDS/4.3.1.0-555/hds-ambari-mpack-4.3.1.0-555.tar.gz",
                "mpack_version": "4.3.1",
                "registry_id": 1,
                "services": [
                  {
                    "name": "SPARK",
                    "version": "2.0.0"
                  },
                  {
                    "name": "ZEPPELIN",
                    "version": "1.0.0"
                  }
                ]
              }
            }
          ]
        }
      ],
      "scenarios": [
        {
          "href": "http://localhost:8080/api/v1/registries/1/scenarios/DataScience",
          "RegistryScenarioInfo": {
            "registry_id": 1,
            "scenario_description": "Data Science and Machine Learning",
            "scenario_mpacks": [
              {
                "name": "HDP"
              },
              {
                "name": "HDS"
              }
            ],
            "scenario_name": "DataScience"
          }
        },
        {
          "href": "http://localhost:8080/api/v1/registries/1/scenarios/EDW",
          "RegistryScenarioInfo": {
            "registry_id": 1,
            "scenario_description": "EDW or SQL Analytics",
            "scenario_mpacks": [
              {
                "name": "HDP"
              },
              {
                "name": "EDW"
              }
            ],
            "scenario_name": "EDW"
          }
        },
        {
          "href": "http://localhost:8080/api/v1/registries/1/scenarios/Hadoop",
          "RegistryScenarioInfo": {
            "registry_id": 1,
            "scenario_description": "Hadoop Core",
            "scenario_mpacks": [
              {
                "name": "HDP"
              }
            ],
            "scenario_name": "Hadoop"
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
        { id: "HDPCore3.0.0ZOOKEEPER" },
        { id: "HDPCore3.0.0HDFS" }
      ]);

      wizardSelectMpacksController.loadStep();
      
      var service = wizardSelectMpacksController.getServiceVersionById("HDPCore3.0.0ZOOKEEPER");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;

      var service = wizardSelectMpacksController.getServiceVersionById("HDPCore3.0.0HDFS");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;
    });
  });

  describe('#getMpacksByName', function () {
    it('should return an array of mpacks matching the given names', function () {
      //this test assumes that mpackNames contains the names of all mpacks in the test registry data at the top of this file
      var mpackNames = [
        'EDW',
        'HDPCore',
        'HDS'
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
            name: "mpackName1",
            version: "1.0.0.0",
            stackName: "stack1",
            stackVersion: "1.0.0"
          }
        },
        {
          id: "id2",
          name: "name2",
          mpackVersion: {
            name: "mpackName2",
            version: "1.0.0.0",
            stackName: "stack1",
            stackVersion: "1.0.0"
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
          mpackVersion: "1.0.0.0",
          stackName: "stack1",
          stackVersion: "1.0.0"
        },
        {
          id: "id2",
          name: "name2",
          mpackName: "mpackName2",
          mpackVersion: "1.0.0.0",
          stackName: "stack1",
          stackVersion: "1.0.0"
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
