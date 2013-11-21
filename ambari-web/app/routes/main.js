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
var stringUtils = require('utils/string_utils');

module.exports = Em.Route.extend({
  route: '/main',
  enter: function (router) {
    App.db.updateStorage();
    console.log('in /main:enter');
    if (router.getAuthenticated()) {
      App.router.get('clusterController').loadClusterName(false);
      router.get('mainController').initialize();
      // TODO: redirect to last known state
    } else {
      Ember.run.next(function () {
        router.transitionTo('login');
      });
    }
  },
  /*
   routePath: function(router,event) {
   if (router.getAuthenticated()) {
   App.router.get('clusterController').loadClusterName(false);
   router.get('mainController').initialize();
   // TODO: redirect to last known state
   } else {
   Ember.run.next(function () {
   router.transitionTo('login');
   });
   }
   }, */

  index: Ember.Route.extend({
    route: '/',
    redirectsTo: 'dashboard'
  }),

  test: Em.Route.extend({
    route: '/test',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainTest');
    }
  }),

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('main');
  },

  charts: Em.Route.extend({
    route: '/charts',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainCharts');
    },
    enter: function (router) {
      Em.run.next(function () {
        router.transitionTo('heatmap');
      });
    },
    index: Ember.Route.extend({
      route: '/',
      redirectsTo: 'heatmap'
    }),
    heatmap: Em.Route.extend({
      route: '/heatmap',
      connectOutlets: function (router, context) {
        router.get('mainChartsController').connectOutlet('mainChartsHeatmap');
      }
    }),
    horizon_chart: Em.Route.extend({
      route: '/horizon_chart',
      connectOutlets: function (router, context) {
        router.get('mainChartsController').connectOutlet('mainChartsHorizon');
      }
    }),
    showChart: function (router, event) {
      var parent = event.view._parentView;
      parent.deactivateChildViews();
      event.view.set('active', "active");
      router.transitionTo(event.context);
    }
  }),
    apps: Em.Route.extend({
      route: '/apps',
      connectOutlets: function (router) {
        if (App.get('isHadoop2Stack')) {
          Em.run.next(function () {
            router.transitionTo('main.dashboard');
          });
        } else {
          router.get('mainAppsController').loadRuns();
          router.get('mainController').connectOutlet('mainApps');
        }
      }
    }),

  mirroring: Em.Route.extend({
    route: '/mirroring',
    index: Ember.Route.extend({
      route: '/',
      enter: function () {
        this.setupController()
      },
      setupController: function () {
        var controller = App.router.get('mainMirroringController');
        var datasets = App.Dataset.find();
        controller.set('datasets', datasets);
      },
      connectOutlets: function (router, context) {
        router.get('mainController').connectOutlet('mainMirroring');
      }
    }),

    gotoMirroringHome: function (router) {
      router.transitionTo('mirroring/index');
    },
    addNewDataset: function (router) {
      router.transitionTo('addNewDatasetRoute');
    },

    addTargetCluster: function (router, event) {
      router.transitionTo('addTargetClusterRoute');
    },

    addNewDatasetRoute: Em.Route.extend({
      route: '/dataset/add',

      setupController: function (controller) {
        controller.createNewDataSet();
      },
      enter: function (router) {
        var controller = router.get('mainMirroringDataSetController');
        // if we are coming from closing AddCluster popup
        if (controller.isReturning) {
          controller.isReturning = false;
          return;
        }

        controller.set('isPopupForEdit', false);
        this.setupController(controller);

        var self = this;
        controller.set('isSubmitted', false);
        App.ModalPopup.show({
          classNames: ['sixty-percent-width-modal', 'hideCloseLink'],
          header: Em.I18n.t('mirroring.dataset.newDataset'),
          primary: Em.I18n.t('mirroring.dataset.save'),
          secondary: Em.I18n.t('common.cancel'),
          onPrimary: function () {
            controller.set('isSubmitted', true);
            var isValid = controller.validate();

            if (!isValid) {
              return;
            }
            newDataSet = controller.getNewDataSet();
            var schedule = newDataSet.get('schedule');
            var targetCluster = newDataSet.get('targetCluster');
            var scheduleRecord = App.Dataset.Schedule.createRecord(schedule);
            var dataSetRecord = App.Dataset.createRecord(newDataSet);
            scheduleRecord.set('dataset', dataSetRecord);
            dataSetRecord.set('schedule', scheduleRecord);

            this.hide();
            router.transitionTo('main.mirroring.index');
          },
          onSecondary: function () {
            this.hide();
            router.transitionTo('main.mirroring.index');
          },
          bodyClass: App.MainMirroringDataSetView.extend({
            controller: router.get('mainMirroringDataSetController')
          })
        });
      }
    }),

    gotoShowJobs: function (router, event) {
      router.transitionTo('showDatasetJobs', event.context);
    },

    showDatasetJobs: Em.Route.extend({
      route: '/dataset/:dataset_id',
      connectOutlets: function (router, dataset) {
        router.get('mainController').connectOutlet('mainJobs', dataset);
      }
    }),

    editDataset: Em.Route.extend({
      route: '/dataset/:dataset_id/edit',
      setupController: function (controller, dataset) {
        controller.setOriginalDataSetRecord(dataset);
        controller.setDataSet(dataset);
      },


      connectOutlets: function (router, dataset) {
        var controller = router.get('mainMirroringDataSetController');
        // if we are coming from closing AddCluster popup
        if (controller.isReturning) {
          controller.isReturning = false;
          return;
        }
        // for showing delete button
        controller.set('isPopupForEdit', true);
        this.setupController(controller, dataset);

        var self = this;
        controller.set('isSubmitted', false);
        controller.set('popup', App.ModalPopup.show({
          classNames: ['sixty-percent-width-modal'],
          header: Em.I18n.t('mirroring.dataset.editDataset'),
          primary: Em.I18n.t('mirroring.dataset.save'),
          secondary: Em.I18n.t('common.cancel'),
          onPrimary: function () {
            controller.set('isSubmitted', true);
            var isValid = controller.validate();

            if (!isValid) {
              return;
            }
            newDataSet = controller.getNewDataSet();

            var originalRecord = controller.get('model.originalRecord');

            originalRecord.set('name', newDataSet.get('name'));
            originalRecord.set('sourceDir', newDataSet.get('sourceDir'));
            originalRecord.set('targetCluster', newDataSet.get('targetCluster'));
            originalRecord.set('targetDir', newDataSet.get('targetDir'));
            originalRecord.set('schedule', newDataSet.get('schedule'));
            this.hide();
            router.transitionTo('main.mirroring.index');
          },
          onSecondary: function () {
            this.hide();
            router.transitionTo('main.mirroring.index');
          },
          bodyClass: App.MainMirroringDataSetView.extend({
            controller: router.get('mainMirroringDataSetController')
          })
        })
        );
      }
    }),

    gotoEditDataset: function (router, event) {
      router.transitionTo('editDataset', event.context);
    },

    addTargetClusterRoute: Ember.Route.extend({
      route: '/targetCluster/add',
      initialState: 'testConnectionRoute',
      testConnectionRoute: Ember.Route.extend({
        setupController: function (controller) {
          controller.createTargetCluster();
          controller.set('model.isPopupForEdit', false);

        },

        enter: function (router, context) {

          var self = this;
          var controller = App.router.get('mainMirroringTargetClusterController');
          this.setupController(controller);

          controller.set('isSubmitted1', false);
          controller.set('isSubmitted2', false);
          controller.set('popup', App.ModalPopup.show({
            classNames: ['sixty-percent-width-modal', 'hideCloseLink'],
            header: Em.I18n.t('mirroring.targetcluster.addCluster'),
            primary: Em.I18n.t('mirroring.targetcluster.testConnection'),
            onPrimary: function () {
              controller.set('isSubmitted1', true);
              var isValid = controller.validate1();

              if (!isValid) {
                return;
              }

              App.router.transitionTo('testConnectionResultsRoute');
            },
            onSecondary: function () {
              this.hide();

              var dscontroller = App.router.get('mainMirroringDataSetController');
              var tccontroller = App.router.get('mainMirroringTargetClusterController');
              var returnRoute = tccontroller.get('returnRoute');
              // if we have come from addNewDatasetRoute
              if (returnRoute) {
                dscontroller.isReturning = true;
                App.router.transitionTo(returnRoute);
              }
              else
                App.router.transitionTo('main.mirroring.index');
            },
            bodyClass: App.MainMirroringAddTargetClusterView.extend({
              controller: App.router.get('mainMirroringTargetClusterController')
            })
          }));
        },

        connectOutlets: function (router, context) {
          console.log("entering the connectOutlets method of testConnectionRoute.")
          var parentController = router.get('mainMirroringTargetClusterController');
          parentController.connectOutlet('testConnection', parentController.get('model'));
        },

        exit: function (stateManager) {
          console.log("exiting the testConnectionRoute state")
        }
      }),
      testConnectionResultsRoute: Ember.Route.extend({
        enter: function (stateManager) {
          console.log("entering the testConnectionResultsRoute state.")
          // lets change the primary button
          var controller = App.router.get('mainMirroringTargetClusterController');
          var popup = controller.get('popup');
          popup.set('primary', Em.I18n.t('common.save'));
          popup.set('onPrimary',
            function () {
              var controller = App.router.get('mainMirroringTargetClusterController');
              controller.set('isSubmitted2', true);
              var isValid = controller.validate2();

              if (!isValid) {
                return;
              }

              var controller = App.router.get('testConnectionResultsController');
              controller.saveClusterName();
            }
          );

        },

        connectOutlets: function (router, context) {
          console.log("entering the connectOutlets method of testConnectionResultsRoute.")
          var parentController = router.get('mainMirroringTargetClusterController');
          parentController.connectOutlet('testConnectionResults', parentController.get('model'));
        },

        exit: function (stateManager) {
          console.log("exiting the connectionSuccessRoute state")
        }

      })
    }),
    editTargetClusterRoute: Em.Route.extend({
      route: '/targetCluster/:targetCluster_id/edit',

      initialState: 'testConnectionRoute',

      setupController: function (controller, targetCluster) {
        controller.setOriginalRecord(targetCluster);
        controller.setTargetCluster(targetCluster);
      },

      connectOutlets: function (router, targetCluster) {
        // this connectOutlets is mainly to receive the 'targetCluster' argument
        var controller = router.get('mainMirroringTargetClusterController');
        // for showing delete button
        controller.set('model.isPopupForEdit', true);
        this.setupController(controller, targetCluster);
      },

      testConnectionRoute: Em.Route.extend({
        connectOutlets: function (router, targetCluster) {
          var controller = router.get('mainMirroringTargetClusterController');
          controller.set('isSubmitted1', false);
          controller.set('isSubmitted2', false);

          controller.set('popup', App.ModalPopup.show({
            classNames: ['sixty-percent-width-modal'],
            header: Em.I18n.t('mirroring.dataset.editDataset'),
            primary: Em.I18n.t('mirroring.targetcluster.testConnection'),
            onPrimary: function () {
              var controller = App.router.get('mainMirroringTargetClusterController');
              controller.set('isSubmitted1', true);
              var isValid = controller.validate1();

              if (!isValid) {
                return;
              }

              App.router.transitionTo('testConnectionResultsRoute');
            },
            secondary: Em.I18n.t('common.cancel'),
            onSecondary: function () {
              this.hide();
              router.transitionTo('main.mirroring.index');
            },
            bodyClass: App.MainMirroringAddTargetClusterView.extend({
              controller: App.router.get('mainMirroringTargetClusterController')
            })
          }));

          console.log("entering the connectOutlets method of testConnectionRoute.")
          var parentController = router.get('mainMirroringTargetClusterController');
          parentController.connectOutlet('testConnection', parentController.get('model'));

        }

      }),
      testConnection: function () {
        App.router.transitionTo('testConnectionResultsRoute');
      },
      testConnectionResultsRoute: Ember.Route.extend({
        enter: function (stateManager) {
          console.log("entering the testConnectionResultsRoute state.")
          // lets change the primary button
          var controller = App.router.get('mainMirroringTargetClusterController');
          var popup = controller.get('popup');
          popup.set('primary', Em.I18n.t('common.save'));
          popup.set('onPrimary',
            function () {
              var controller = App.router.get('mainMirroringTargetClusterController');
              controller.set('isSubmitted2', true);
              var isValid = controller.validate1();

              if (!isValid) {
                return;
              }
              var controller2 = App.router.get('testConnectionResultsController');
              controller2.saveClusterName();
            }
          );

        },

        connectOutlets: function (router, context) {
          console.log("entering the connectOutlets method of testConnectionResultsRoute.");
          var parentController = router.get('mainMirroringTargetClusterController');
          parentController.connectOutlet('testConnectionResults', parentController.get('model'));
        },

        exit: function (stateManager) {
          console.log("exiting the connectionSuccessRoute state")
        }

      })

    }),

    editTargetCluster: function (router, event) {
      router.transitionTo('editTargetClusterRoute', event.context);
    }


  }),


  hosts: Em.Route.extend({
    route: '/hosts',
    index: Ember.Route.extend({
      route: '/',
      connectOutlets: function (router, context) {
        router.get('mainController').connectOutlet('mainHost');
      }
    }),

    hostDetails: Em.Route.extend({
      route: '/:host_id',
      connectOutlets: function (router, host) {
        router.get('mainController').connectOutlet('mainHostDetails', host);
      },

      index: Ember.Route.extend({
        route: '/',
        redirectsTo: 'summary'
      }),

      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostSummary');
        }
      }),

      configs: Em.Route.extend({
        route: '/configs',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostConfigs');
        }
      }),

      metrics: Em.Route.extend({
        route: '/metrics',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostMetrics');
        }
      }),

      audit: Em.Route.extend({
        route: '/audit',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostAudit');
        }
      }),

      hostNavigate: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),

    back: function (router, event) {
      window.history.back();
    },

    showDetails: function (router, event) {
      router.get('mainHostDetailsController').setBack(true);
      router.transitionTo('hostDetails.summary', event.context)
    },

    addHost: function (router) {
      router.transitionTo('hostAdd');
    }

  }),

  hostAdd: require('routes/add_host_routes'),

  admin: Em.Route.extend({
    route: '/admin',
    enter: function (router, transition) {
      if (!App.isAdmin) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard');
        });
      }
    },

    routePath: function (router, event) {
      if (!App.isAdmin) {
        Em.run.next(function () {
          App.router.transitionTo('main.dashboard');
        });
      } else {
        var controller = router.get('mainAdminController');
        router.transitionTo('admin' + controller.get('category').capitalize());
      }
    },
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainAdmin');
    },

    index: Em.Route.extend({
      /* enter: function(router, transition){
       var controller = router.get('mainAdminController');
       router.transitionTo('admin' + controller.get('category').capitalize());
       }, */
      route: '/',
      redirectsTo: 'adminUser'
    }),


    adminUser: Em.Route.extend({
      route: '/user',
      index: Em.Route.extend({
        route: '/',
        redirectsTo: 'allUsers'
      }),
      enter: function (router) {
        router.set('mainAdminController.category', "user");
        Em.run.next(function () {
          router.transitionTo('allUsers');
        });
      },
      routePath: function (router, event) {
        router.set('mainAdminController.category', "user");
        router.transitionTo('allUsers');
        Em.run.next(function () {
          router.transitionTo('allUsers');
        });
      },
      // events
      gotoUsers: Em.Router.transitionTo("allUsers"),
      gotoCreateUser: Em.Router.transitionTo("createUser"),
      gotoEditUser: function (router, event) {
        router.transitionTo("editUser", event.context)
      },

      // states
      allUsers: Em.Route.extend({
        route: '/allUsers',
        // index: Ember.Route.extend({
        //route: '/',
        connectOutlets: function (router) {
          router.get('mainAdminController').connectOutlet('mainAdminUser');
        }
        //})
      }),

      createUser: Em.Route.extend({
        route: '/create',
        connectOutlets: function (router) {
          router.get('mainAdminController').connectOutlet('mainAdminUserCreate', {});
        }
      }),

      editUser: Em.Route.extend({
        route: '/edit/:user_id',
        connectOutlets: function (router, user) {
          router.get('mainAdminController').connectOutlet('mainAdminUserEdit', user);
        }
      })
    }),


    adminAuthentication: Em.Route.extend({
      route: '/authentication',
      connectOutlets: function (router, context) {
        router.set('mainAdminController.category', "authentication");
        router.get('mainAdminController').connectOutlet('mainAdminAuthentication');
      }
    }),

    adminHighAvailability: Em.Route.extend({
      route: '/highAvailability',
      enter: function (router) {
        Em.run.next(function () {
          router.transitionTo('adminHighAvailability.index');
        });
      },
      index: Ember.Route.extend({
        route: '/',
        connectOutlets: function (router, context) {
          router.set('mainAdminController.category', "highAvailability");
          router.get('mainAdminController').connectOutlet('mainAdminHighAvailability');
        }
      })
    }),

    enableHighAvailability: require('routes/high_availability_routes'),

    rollbackHighAvailability: require('routes/rollbackHA_routes'),



    adminSecurity: Em.Route.extend({
      route: '/security',
      enter: function (router) {
        router.set('mainAdminController.category', "security");
        var controller = router.get('mainAdminSecurityController');
        if (!(controller.getAddSecurityWizardStatus() === 'RUNNING') && !(controller.getDisableSecurityStatus() === 'RUNNING')) {
          Em.run.next(function () {
            router.transitionTo('adminSecurity.index');
          });
        } else if (controller.getAddSecurityWizardStatus() === 'RUNNING') {
          Em.run.next(function () {
            router.transitionTo('adminAddSecurity');
          });
        } else if (controller.getDisableSecurityStatus() === 'RUNNING') {
          Em.run.next(function () {
            router.transitionTo('disableSecurity');
          });
        }
      },

      index: Ember.Route.extend({
        route: '/',
        connectOutlets: function (router, context) {
          var controller = router.get('mainAdminController');
          controller.set('category', "security");
          controller.connectOutlet('mainAdminSecurity');
        }
      }),

      addSecurity: function (router, object) {
        router.get('mainAdminSecurityController').setAddSecurityWizardStatus('RUNNING');
        router.transitionTo('adminAddSecurity');
      },

      disableSecurity: Ember.Route.extend({
        route: '/disableSecurity',
        enter: function (router) {
          //after refresh check if the wizard is open then restore it
          if (router.get('mainAdminSecurityController').getDisableSecurityStatus() === 'RUNNING') {
            Ember.run.next(function () {
              App.router.get('updateController').set('isWorking', false);
              App.ModalPopup.show({
                classNames: ['full-width-modal'],
                header: Em.I18n.t('admin.removeSecurity.header'),
                bodyClass: App.MainAdminSecurityDisableView.extend({
                  controllerBinding: 'App.router.mainAdminSecurityDisableController'
                }),
                primary: Em.I18n.t('form.cancel'),
                secondary: null,
                showFooter: false,

                onClose: function () {
                  var self = this;
                  var controller = router.get('mainAdminSecurityDisableController');
                  if (!controller.get('isSubmitDisabled')) {
                    self.proceedOnClose();
                    return;
                  }
                  var applyingConfigStage = controller.get('stages').findProperty('stage', 'stage3');
                  if (applyingConfigStage && !applyingConfigStage.get('isCompleted')) {
                    if (applyingConfigStage.get('isStarted')) {
                      App.showAlertPopup(Em.I18n.t('admin.security.applying.config.header'), Em.I18n.t('admin.security.applying.config.body'));
                    } else {
                      App.showConfirmationPopup(function () {
                        self.proceedOnClose();
                      }, Em.I18n.t('admin.addSecurity.disable.onClose'));
                    }
                  } else {
                    self.proceedOnClose();
                  }
                },
                proceedOnClose: function () {
                  router.get('mainAdminSecurityDisableController').clearStep();
                  App.db.setSecurityDeployStages(undefined);
                  App.router.get('updateController').set('isWorking', true);
                  router.get('mainAdminSecurityController').setDisableSecurityStatus(undefined);
                  App.clusterStatus.setClusterStatus({
                    clusterName: router.get('content.cluster.name'),
                    clusterState: 'DEFAULT'
                  });
                  this.hide();
                  router.transitionTo('adminSecurity.index');
                },
                didInsertElement: function () {
                  this.fitHeight();
                }
              });
            });
          } else {
            router.transitionTo('adminSecurity.index');
          }
        },

        unroutePath: function () {
          return false;
        },

        done: function (router, context) {
          var controller = router.get('mainAdminSecurityDisableController');
          if (!controller.get('isSubmitDisabled')) {
            $(context.currentTarget).parents("#modal").find(".close").trigger('click');
          }
        }
      }),

      adminAddSecurity: require('routes/add_security')
    }),

    adminCluster: Em.Route.extend({
      route: '/cluster',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "cluster");
        router.get('mainAdminController').connectOutlet('mainAdminCluster');
      }
    }),
    adminAdvanced: Em.Route.extend({
      route: '/advanced',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "advanced");
        router.get('mainAdminController').connectOutlet('mainAdminAdvanced');
      }
    }),
    adminMisc: Em.Route.extend({
      route: '/misc',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "misc");
        router.get('mainAdminController').connectOutlet('mainAdminMisc');
      }
    }),

    adminAudit: Em.Route.extend({
      route: '/audit',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "audit");
        router.get('mainAdminController').connectOutlet('mainAdminAudit');
      }
    }),
    upgradeStack: function (router, event) {
      if (!$(event.currentTarget).hasClass('inactive')) {
        router.transitionTo('stackUpgrade');
      }
    },


    adminNavigate: function (router, object) {
      router.transitionTo('admin' + object.context.capitalize());
    },

//events
    goToAdmin: function (router, event) {
      router.transitionTo(event.context);
    }

  }),
  stackUpgrade: require('routes/stack_upgrade'),

  dashboard: Em.Route.extend({
    route: '/dashboard',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainDashboard');
    },
    showDetails: function (router, event) {
      router.get('mainHostDetailsController').setBack(true);
      router.transitionTo('hosts.hostDetails.summary', event.context);
    }
  }),

  services: Em.Route.extend({
    route: '/services',
    index: Ember.Route.extend({
      route: '/',
      enter: function (router) {
        Ember.run.next(function () {
          var controller = router.get('mainController');
          controller.dataLoading().done(function () {
            var service = router.get('mainServiceItemController.content');
            if (!service) {
              service = App.Service.find().objectAt(0); // getting the first service to display
            }
            router.transitionTo('service.summary', service);
          });
        });
      }
    }),
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainService');
    },
    service: Em.Route.extend({
      route: '/:service_id',
      connectOutlets: function (router, service) {
        router.get('mainServiceController').connectOutlet('mainServiceItem', service);
        router.transitionTo('summary');
      },
      index: Ember.Route.extend({
        route: '/'
      }),
      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          var viewName = 'mainServiceInfoSummary';
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoSummary', item);
        }
      }),
      metrics: Em.Route.extend({
        route: '/metrics',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoMetrics', item);
        }
      }),
      configs: Em.Route.extend({
        route: '/configs',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
        }
      }),
      audit: Em.Route.extend({
        route: '/audit',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('mainServiceItemController').connectOutlet('mainServiceInfoAudit', item);
        }
      }),
      showInfo: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      },
      showDetails: function (router, event) {
        router.get('mainHostDetailsController').setBack(true);
        router.transitionTo('hosts.hostDetails.summary', event.context);
      }
    }),
    showService: Em.Router.transitionTo('service'),
    addService: Em.Router.transitionTo('serviceAdd'),
    reassign: require('routes/reassign_master_routes')
  }),


  serviceAdd: require('routes/add_service_routes'),

  selectService: Em.Route.transitionTo('services.service.summary'),
  selectHost: function (router, event) {
    router.get('mainHostDetailsController').setBack(false);
    router.transitionTo('hosts.hostDetails.index', event.context);
  },
  filterHosts: function (router, component) {
    if(!component.context)
      return;
    router.get('mainHostController').filterByComponent(component.context);
    router.transitionTo('hosts.index');
  }
});
