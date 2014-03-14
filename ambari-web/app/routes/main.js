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
      App.router.get('mainAdminAccessController').loadShowJobsForUsers().done(function() {
        App.router.get('clusterController').loadClusterName(false);
        if(App.testMode) {
          router.get('mainController').initialize();
        }else {
          App.router.get('clusterController').loadClientServerClockDistance().done(function() {
            router.get('mainController').initialize();
          });
        }
      });
      // TODO: redirect to last known state
    } else {
      Em.run.next(function () {
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

  jobs : Em.Route.extend({
    route : '/jobs',
    enter: function (router) {
      if(!App.router.get('mainAdminAccessController.showJobs') && !App.get('isAdmin')){
        Em.run.next(function () {
          router.transitionTo('main.dashboard');
        });
      }
    },
    exit: function(router) {
      clearInterval(router.get('mainJobsController').jobsUpdate);
    },
    index: Ember.Route.extend({
      route: '/',
      connectOutlets : function(router) {
        if (!App.get('isHadoop2Stack')) {
          Em.run.next(function() {
            router.transitionTo('main.dashboard');
          });
        } else {
          router.get('mainJobsController').loadJobs();
          router.get('mainJobsController').updateJobs('mainJobsController', 'refreshLoadedJobs');
          router.get('mainController').connectOutlet('mainJobs');
        }
      }
    }),
    jobDetails : Em.Route.extend({
      route : '/:job_id',
      connectOutlets : function(router, job) {
        if (job) {
          router.get('mainHiveJobDetailsController').set('loaded', false);
          router.get('mainController').connectOutlet('mainHiveJobDetails', job);
          router.get('mainHiveJobDetailsController').loadJobDetails();
          router.get('mainJobsController').updateJobs('mainHiveJobDetailsController', 'loadJobDetails');
        }
      },
      exit: function(router) {
        router.get('mainHiveJobDetailsController').set('loaded', false);
      }
    }),
    showJobDetails : function(router, event) {
      if (event.context && event.context.get('hasTezDag')) {
        router.transitionTo('jobDetails', event.context);
      }
    }
  }),

  mirroring: Em.Route.extend({
    route: '/mirroring',
    index: Ember.Route.extend({
      route: '/'
    }),

    connectOutlets: function (router) {
      router.get('mainController').connectOutlet('mainMirroring');
    },

    gotoShowJobs: function (router, context) {
      var dataset = context || router.get('mainMirroringController.selectedDataset') || App.Dataset.find().objectAt(0);
      if (dataset) {
        router.transitionTo('showDatasetJobs', dataset);
      } else {
        router.transitionTo('index');
      }
    },

    showDatasetJobs: Em.Route.extend({
      route: '/:dataset_id',
      connectOutlets: function (router, dataset) {
        router.get('mainDatasetJobsController').set('content', dataset);
        router.get('mainMirroringController').set('selectedDataset', dataset);
      }
    }),

    editDatasetRoute: Em.Route.extend({
      route: '/edit/:dataset_id',
      connectOutlets: function (router, dataset) {
        router.get('mainMirroringEditDataSetController').showEditPopup(dataset);
      }
    }),

    editDataset: function (router, event) {
      router.transitionTo('editDatasetRoute', event.view.get('dataset'));
    },

    addNewDataset: function (router) {
      router.transitionTo('addNewDatasetRoute');
    },

    addNewDatasetRoute: Em.Route.extend({
      route: '/dataset/add',
      enter: function (router) {
        var controller = router.get('mainMirroringEditDataSetController');
        controller.showAddPopup();
      }
    }),

    manageClustersRoute: Em.Route.extend({
      route: '/dataset/clusters/edit',
      enter: function (router) {
        var controller = router.get('mainMirroringController');
        controller.manageClusters();
      }
    }),

    manageClusters: function (router) {
      router.transitionTo('manageClustersRoute');
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
      var referer = router.get('mainHostDetailsController.referer');
      if (referer) {
        router.route(referer);
      }
      else {
        window.history.back();
      }
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
                  var applyingConfigCommand = controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
                  if (applyingConfigCommand && !applyingConfigCommand.get('isCompleted')) {
                    if (applyingConfigCommand.get('isStarted')) {
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
                  App.db.setSecurityDeployCommands(undefined);
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
    adminAccess: Em.Route.extend({
      enter: function(router) {
        router.get('mainController').dataLoading().done(function() {
          if (!router.get('mainAdminController.isAccessAvailable')) router.transitionTo('adminUser.allUsers');
        });
      },
      route: '/access',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "access");
        router.get('mainAdminController').connectOutlet('mainAdminAccess');
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
          if (item.get('isConfigurable')) {
            router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
          }
          else {
            // if service doesn't have configs redirect to summary
            router.transitionTo('summary');
          }
        },
        unroutePath: function (router, context) {
          var controller = router.get('mainServiceInfoConfigsController');
          if (!controller.get('forceTransition') && controller.hasUnsavedChanges()) {
            controller.showSavePopup(context);
          } else {
            this._super(router, context);
          }
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
        var mainServiceInfoConfigsController = App.router.get('mainServiceInfoConfigsController');
        if (event.context === 'summary' && mainServiceInfoConfigsController.hasUnsavedChanges()) {
          mainServiceInfoConfigsController.showSavePopup(router.get('location.lastSetURL').replace('configs', 'summary'));
          return false;
        }
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),
    showService: Em.Router.transitionTo('service'),
    addService: Em.Router.transitionTo('serviceAdd'),
    reassign: Em.Router.transitionTo('reassign')
  }),

  reassign: require('routes/reassign_master_routes'),

  serviceAdd: require('routes/add_service_routes'),

  selectService: Em.Route.transitionTo('services.service.summary'),
  selectHost: function (router, event) {
    router.get('mainHostDetailsController').set('isFromHosts', false);
    router.transitionTo('hosts.hostDetails.index', event.context);
  },
  filterHosts: function (router, component) {
    if(!component.context)
      return;
    router.get('mainHostController').filterByComponent(component.context);
    router.transitionTo('hosts.index');
  },
  showDetails: function (router, event) {
    router.get('mainHostDetailsController').set('referer', router.location.lastSetURL);
    router.get('mainHostDetailsController').set('isFromHosts', true);
    router.transitionTo('hosts.hostDetails.summary', event.context);
  }
});
