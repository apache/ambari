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
      ]
    }
  ]
};

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

  describe('#loadStep', function () {
    it('adds previously selected services to selection', function () {
      wizardSelectMpacksController.set('content.selectedServices', [
        { id: "HDPCore3.0.0ZOOKEEPER" },
        { id: "HDPCore3.0.0HDFS" }
      ]);

      wizardSelectMpacksController.loadStep();
      
      var service = wizardSelectMpacksController.getServiceById("HDPCore3.0.0ZOOKEEPER");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;

      var service = wizardSelectMpacksController.getServiceById("HDPCore3.0.0HDFS");
      expect(service.get('selected')).to.be.true;
      expect(service.get('mpackVersion.selected')).to.be.true;
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

      sinon.stub(wizardSelectMpacksController, 'getServiceById').returns(actual);
    });
    
    after(function () {
      wizardSelectMpacksController.getServiceById.restore();
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

      wizardSelectMpacksController.getServiceById.restore();
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

      sinon.stub(wizardSelectMpacksController, 'getServiceById').returns(actual);

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

      sinon.stub(wizardSelectMpacksController, 'getServiceById').returns(actual);

      wizardSelectMpacksController.removeServiceHandler("HDPCore3.0.0ZOOKEEPER");
      expect(actual.get('selected')).to.be.false;
      expect(actual.get('mpackVersion.selected')).to.be.false;
    });
  });

  describe('#clearSelection', function () {
    it('should set all services and mpacks to be unselected', function () {
      var servicesActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackServices', servicesActual);

      var versionsActual = [
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true }),
        Em.Object.create({ selected: true })
      ];
      wizardSelectMpacksController.set('content.mpackVersions', versionsActual);

      var expected = [
        Em.Object.create({ selected: false }),
        Em.Object.create({ selected: false }),
        Em.Object.create({ selected: false })
      ];

      wizardSelectMpacksController.clearSelection();
      expect(servicesActual).to.deep.equal(expected);
      expect(versionsActual).to.deep.equal(expected);
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
          name: "mpackName1",
          displayName: "displayName1",
          url: "http://someurl.com/mpack1",
          version: "1.0.0.0"
        },
        {
          name: "mpackName2",
          displayName: "displayName2",
          url: "http://someurl.com/mpack2",
          version: "1.0.0.0"
        }
      ];

      sinon.stub(App.router, 'send');
      wizardSelectMpacksController.submit();
      App.router.send.restore();
      
      expect(wizardSelectMpacksController.get('content.selectedServices')).to.deep.equal(expectedSelectedServices);
      expect(wizardSelectMpacksController.get('content.selectedServiceNames')).to.deep.equal(expectedSelectedServiceNames);
      expect(wizardSelectMpacksController.get('content.selectedMpacks')).to.deep.equal(expectedSelectedMpacks);
    })
  })
});
