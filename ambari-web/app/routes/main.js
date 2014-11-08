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
    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = App.router.get('applicationController');
        applicationController.startKeepAlivePoller();
        App.router.get('mainController').checkServerClientVersion().done(function () {
          App.router.get('mainViewsController').loadAmbariViews();
          App.router.get('clusterController').loadClusterName(false).done(function () {
            if (App.get('testMode')) {
              router.get('mainController').initialize();
            } else {
              if (router.get('clusterInstallCompleted')) {
                App.router.get('clusterController').loadClientServerClockDistance().done(function () {
                  router.get('mainController').initialize();
                });
              }
              else {
                App.router.get('clusterController').set('isLoaded', true);
              }
            }
          });
        });
        // TODO: redirect to last known state
      } else {
        router.set('preferedPath', router.location.location.hash);
        Em.run.next(function () {
          router.transitionTo('login');
        });
      }
    });
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
    redirectsTo: 'dashboard.index'
  }),

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('main');
  },

  test: Em.Route.extend({
    route: '/test',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainTest');
    }
  }),

  dashboard: Em.Route.extend({
    route: '/dashboard',
    connectOutlets: function (router, context) {
      router.get('mainController').connectOutlet('mainDashboard');
    },
    index: Em.Route.extend({
      route: '/',
      enter: function (router) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.widgets');
        });
      }
    }),
    goToDashboardView: function (router, event) {
      router.transitionTo(event.context);
    },
    widgets: Em.Route.extend({
      route: '/metrics',
      connectOutlets: function (router, context) {
        router.set('mainDashboardController.selectedCategory', 'widgets');
        router.get('mainDashboardController').connectOutlet('mainDashboardWidgets');
      }
    }),
    charts: Em.Route.extend({
      route: '/charts',
      connectOutlets: function (router, context) {
        router.set('mainDashboardController.selectedCategory', 'charts');
        router.get('mainDashboardController').connectOutlet('mainCharts');
      },
      index: Ember.Route.extend({
        route: '/',
        enter: function (router) {
          Em.run.next(function () {
            router.transitionTo('heatmap');
          });
        }
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
    configHistory: Em.Route.extend({
      route: '/config_history',
      connectOutlets: function (router, context) {
        if (App.get('supports.configHistory')) {
          router.set('mainDashboardController.selectedCategory', 'configHistory');
          router.get('mainDashboardController').connectOutlet('mainConfigHistory');
        } else {
          router.transitionTo('main.dashboard.widgets');
        }
      }
    }),
    goToServiceConfigs: function (router, event) {
      router.get('mainServiceItemController').set('routeToConfigs', true);
      router.get('mainServiceInfoConfigsController').set('preSelectedConfigVersion', event.context);
      router.transitionTo('main.services.service.configs', App.Service.find(event.context.get('serviceName')));
      router.get('mainServiceItemController').set('routeToConfigs', false);
    }
  }),

  apps: Em.Route.extend({
    route: '/apps',
    connectOutlets: function (router) {
      if (App.get('isHadoop2Stack')) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.index');
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

  views: require('routes/views'),

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
        router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
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
      if (router.get('loggedIn') && !App.get('isAdmin')) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.index');
        });
      }
    },

    routePath: function (router, event) {
      if (!App.isAdmin) {
        Em.run.next(function () {
          App.router.transitionTo('main.dashboard.index');
        });
      } else {
        this._super(router, event);
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
      redirectsTo: 'adminRepositories'
    }),

    adminAuthentication: Em.Route.extend({
      route: '/authentication',
      connectOutlets: function (router, context) {
        router.set('mainAdminController.category', "authentication");
        router.get('mainAdminController').connectOutlet('mainAdminAuthentication');
      }
    }),

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

      disableSecurity: Em.Route.extend({
        route: '/disableSecurity',
        enter: function (router) {
          //after refresh check if the wizard is open then restore it
          if (router.get('mainAdminSecurityController').getDisableSecurityStatus() === 'RUNNING') {
            var controller = router.get('addSecurityController');
            // App.MainAdminSecurityDisableController uses App.Service DS model whose data needs to be loaded first
            controller.dataLoading().done(Em.run.next(function () {
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
                  router.get('addServiceController').finish();
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
            }));
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

    adminRepositories: Em.Route.extend({
      route: '/repositories',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "repositories");
        router.get('mainAdminController').connectOutlet('mainAdminRepositories');
      }
    }),
    adminAdvanced: Em.Route.extend({
      route: '/advanced',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "advanced");
        router.get('mainAdminController').connectOutlet('mainAdminAdvanced');
      }
    }),
    adminServiceAccounts: Em.Route.extend({
      route: '/serviceAccounts',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "serviceAccounts");
        router.get('mainAdminController').connectOutlet('mainAdminServiceAccounts');
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

  services: Em.Route.extend({
    route: '/services',
    index: Em.Route.extend({
      route: '/',
      enter: function (router) {
        Em.run.next(function () {
          var controller = router.get('mainController');
          controller.dataLoading().done(function () {
            var service = router.get('mainServiceItemController.content');
            if (!service || !service.get('isLoaded')) {
              service = App.Service.find().objectAt(0); // getting the first service to display
            }
            if (router.get('mainServiceItemController').get('routeToConfigs')) {
              router.transitionTo('service.configs', service);
            }
            else {
              router.transitionTo('service.summary', service);
            }
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
        if (service && router.get('mainServiceItemController').get('routeToConfigs')) {
          router.transitionTo('configs');
        } else {
          router.transitionTo('summary');
        }
      },
      index: Ember.Route.extend({
        route: '/'
      }),
      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          var item = router.get('mainServiceItemController.content');
          router.get('updateController').updateServiceMetric(Em.K);
          //if service is not existed then route to default service
          if (item.get('isLoaded')) {
            router.get('mainServiceItemController').connectOutlet('mainServiceInfoSummary', item);
          } else {
            router.transitionTo('services.index');
          }
        },
        exit: function(router) {
          var request = router.get('mainAlertsController.servicesRequest');
          if (request) {
            request.abort();
          }
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
          //if service is not existed then route to default service
          if (item.get('isLoaded')) {
            if (router.get('mainServiceItemController.isConfigurable')) {
              router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
            }
            else {
              // if service doesn't have configs redirect to summary
              router.transitionTo('summary');
            }
          }
          else {
            item.set('routeToConfigs', true);
            router.transitionTo('services.index');
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
        var parent = event.view.get('_parentView');
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),
    showService: Em.Router.transitionTo('service'),
    addService: Em.Router.transitionTo('serviceAdd'),
    reassign: Em.Router.transitionTo('reassign'),

    enableHighAvailability: require('routes/high_availability_routes'),

    enableRMHighAvailability: require('routes/rm_high_availability_routes'),

    rollbackHighAvailability: require('routes/rollbackHA_routes')
  }),

  reassign: require('routes/reassign_master_routes'),

  serviceAdd: require('routes/add_service_routes'),

  selectService: Em.Route.transitionTo('services.service.summary'),
  selectHost: function (router, event) {
    router.get('mainHostDetailsController').set('isFromHosts', false);
    router.transitionTo('hosts.hostDetails.index', event.context);
  },
  filterHosts: function (router, component) {
    if (!component.context)
      return;
    router.get('mainHostController').filterByComponent(component.context);
    router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    router.transitionTo('hosts.index');
  },
  showDetails: function (router, event) {
    router.get('mainHostDetailsController').set('referer', router.location.lastSetURL);
    router.get('mainHostDetailsController').set('isFromHosts', true);
    router.transitionTo('hosts.hostDetails.summary', event.context);
  }
});
