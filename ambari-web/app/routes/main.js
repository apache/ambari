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

module.exports = Em.Route.extend(App.RouterRedirections, {
  route: '/main',
  enter: function (router) {
    App.db.updateStorage();
    console.log('in /main:enter');
    var self = this;
    var location = router.location.location.hash;
    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = App.router.get('applicationController');
        App.router.get('experimentalController').loadSupports().complete(function () {
          applicationController.startKeepAlivePoller();
          App.router.get('mainController').checkServerClientVersion().done(function () {
            App.router.get('mainViewsController').loadAmbariViews();
            App.router.get('clusterController').loadClusterName(false).done(function () {
              if (App.get('testMode')) {
                router.get('mainController').initialize();
              } else {
                if (router.get('clusterInstallCompleted')) {
                  App.router.get('clusterController').loadClientServerClockDistance().done(function () {
                    App.router.get('clusterController').checkDetailedRepoVersion().done(function () {
                      router.get('mainController').initialize();
                    });
                  });
                }
                else {
                  Em.run.next(function () {
                    App.clusterStatus.updateFromServer().complete(function () {
                      var currentClusterStatus = App.clusterStatus.get('value');
                      if (router.get('currentState.parentState.name') !== 'views'
                          && currentClusterStatus && self.get('installerStatuses').contains(currentClusterStatus.clusterState)) {
                        if (App.isAccessible('ADMIN')) {
                          self.redirectToInstaller(router, currentClusterStatus, false);
                        } else {
                          Em.run.next(function () {
                            App.router.transitionTo('main.views.index');
                          });
                        }
                      }
                    });
                  });
                  App.router.get('clusterController').set('isLoaded', true);
                }
              }
            });
          });
          // TODO: redirect to last known state
        });
      } else {
        router.savePreferedPath(location);
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
        App.loadTimer.start('Dashboard Metrics Page');
        router.set('mainDashboardController.selectedCategory', 'widgets');
        router.get('mainDashboardController').connectOutlet('mainDashboardWidgets');
      }
    }),
    charts: Em.Route.extend({
      route: '/charts',
      connectOutlets: function (router, context) {
        App.loadTimer.start('Heatmaps Page');
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
          router.get('mainController').dataLoading().done(function () {
            router.get('mainChartsController').connectOutlet('mainChartsHeatmap');
          });
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
        App.loadTimer.start('Config History Page');
        router.set('mainDashboardController.selectedCategory', 'configHistory');
        router.get('mainDashboardController').connectOutlet('mainConfigHistory');
      }
    }),
    goToServiceConfigs: function (router, event) {
      router.get('mainServiceItemController').set('routeToConfigs', true);
      router.get('mainServiceInfoConfigsController').set('preSelectedConfigVersion', event.context);
      router.transitionTo('main.services.service.configs', App.Service.find(event.context.get('serviceName')));
      router.get('mainServiceItemController').set('routeToConfigs', false);
    }
  }),

  views: require('routes/views'),

  hosts: Em.Route.extend({
    route: '/hosts',
    index: Ember.Route.extend({
      route: '/',
      connectOutlets: function (router, context) {
        App.loadTimer.start('Hosts Page');
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
          router.get('mainController').dataLoading().done(function() {
            var controller = router.get('mainHostDetailsController');
            if ( App.Service.find().mapProperty('serviceName').contains('OOZIE')) {
              controller.loadConfigs('loadOozieConfigs');
              controller.isOozieConfigLoaded.always(function () {
                controller.connectOutlet('mainHostSummary');
              });
            } else {
              controller.connectOutlet('mainHostSummary');
            }
          });
        }
      }),

      configs: Em.Route.extend({
        route: '/configs',
        connectOutlets: function (router, context) {
          router.get('mainController').isLoading.call(router.get('clusterController'), 'isConfigsPropertiesLoaded').done(function () {
            router.get('mainHostDetailsController').connectOutlet('mainHostConfigs');
          });
        }
      }),

      alerts: Em.Route.extend({
        route: '/alerts',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostAlerts');
        },
        exit: function (router) {
          router.set('mainAlertInstancesController.isUpdating', false);
        }
      }),

      metrics: Em.Route.extend({
        route: '/metrics',
        connectOutlets: function (router, context) {
          router.get('mainHostDetailsController').connectOutlet('mainHostMetrics');
        }
      }),

      stackVersions: Em.Route.extend({
        route: '/stackVersions',
        connectOutlets: function (router, context) {
          if (App.get('stackVersionsAvailable')) {
            router.get('mainHostDetailsController').connectOutlet('mainHostStackVersions');
          }
          else {
            router.transitionTo('summary');
          }
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

  alerts: Em.Route.extend({
    route: '/alerts',
    index: Em.Route.extend({
      route: '/',
      connectOutlets: function (router, context) {
        router.get('mainController').connectOutlet('mainAlertDefinitions');
      }
    }),

    alertDetails: Em.Route.extend({

      route: '/:alert_definition_id',

      connectOutlets: function (router, alertDefinition) {
        App.router.set('mainAlertDefinitionsController.showFilterConditionsFirstLoad', true);
        router.get('mainController').connectOutlet('mainAlertDefinitionDetails', alertDefinition);
      },

      exit: function (router) {
        router.set('mainAlertInstancesController.isUpdating', false);
      },

      unroutePath: function (router, context) {
        var controller = router.get('mainAlertDefinitionDetailsController');
        if (!controller.get('forceTransition') && controller.get('isEditing')) {
          controller.showSavePopup(context);
        } else {
          controller.set('forceTransition', false);
          this._super(router, context);
        }
      }
    }),

    back: function (router, event) {
      window.history.back();
    }
  }),

  alertAdd: require('routes/add_alert_definition_routes'),

  admin: Em.Route.extend({
    route: '/admin',
    enter: function (router, transition) {
      if (router.get('loggedIn') && !App.isAccessible('upgrade_ADMIN')) {
        Em.run.next(function () {
          router.transitionTo('main.dashboard.index');
        });
      }
    },

    routePath: function (router, event) {
      if (!App.isAccessible('upgrade_ADMIN')) {
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
      route: '/',
      redirectsTo: 'stackAndUpgrade.index'
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

      index: Em.Route.extend({
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

      adminAddSecurity: require('routes/add_security')
    }),

    adminKerberos: Em.Route.extend({
      route: '/kerberos',
      index: Em.Route.extend({
        route: '/',
        connectOutlets: function (router, context) {
          router.set('mainAdminController.category', "kerberos");
          router.get('mainAdminController').connectOutlet('mainAdminKerberos');
        }
      }),
      adminAddKerberos: require('routes/add_kerberos_routes'),

      disableSecurity: Em.Route.extend({
        route: '/disableSecurity',
        enter: function (router) {
          App.router.get('updateController').set('isWorking', false);
          router.get('mainController').dataLoading().done(function () {
            App.ModalPopup.show({
              classNames: ['full-width-modal'],
              header: Em.I18n.t('admin.removeSecurity.header'),
              bodyClass: App.KerberosDisableView.extend({
                controllerBinding: 'App.router.kerberosDisableController'
              }),
              primary: Em.I18n.t('form.cancel'),
              secondary: null,
              showFooter: false,

              onClose: function () {
                var self = this;
                var controller = router.get('kerberosDisableController');
                if (!controller.get('isSubmitDisabled')) {
                  self.proceedOnClose();
                  return;
                }
                // warn user if disable kerberos command in progress
                var unkerberizeCommand = controller.get('tasks').findProperty('command', 'unkerberize');
                if (unkerberizeCommand && !unkerberizeCommand.get('isCompleted')) {
                  // user cannot exit wizard during removing kerberos
                  if (unkerberizeCommand.get('status') == 'IN_PROGRESS') {
                    App.showAlertPopup(Em.I18n.t('admin.kerberos.disable.unkerberize.header'), Em.I18n.t('admin.kerberos.disable.unkerberize.message'));
                  } else {
                    // otherwise show confirmation window
                    App.showConfirmationPopup(function () {
                      self.proceedOnClose();
                    }, Em.I18n.t('admin.addSecurity.disable.onClose'));
                  }
                } else {
                  self.proceedOnClose();
                }
              },
              proceedOnClose: function () {
                var self = this;
                var disableController = router.get('kerberosDisableController');
                disableController.clearStep();
                disableController.resetDbNamespace();
                App.db.setSecurityDeployCommands(undefined);
                App.router.get('updateController').set('isWorking', true);
                router.get('mainAdminKerberosController').setDisableSecurityStatus(undefined);
                router.get('addServiceController').finish();
                App.clusterStatus.setClusterStatus({
                  clusterName: router.get('content.cluster.name'),
                  clusterState: 'DEFAULT',
                  localdb: App.db.data
                }, {
                  alwaysCallback: function () {
                    self.hide();
                    router.transitionTo('adminKerberos.index');
                    Em.run.next(function() {
                      location.reload();
                    });
                  }
                });
              },
              didInsertElement: function () {
                this.fitHeight();
              }
            });
          });
        },

        unroutePath: function () {
          return false;
        },
        next: function (router, context) {
          $("#modal").find(".close").trigger('click');
        },
        done: function (router, context) {
          var controller = router.get('kerberosDisableController');
          if (!controller.get('isSubmitDisabled')) {
            $(context.currentTarget).parents("#modal").find(".close").trigger('click');
          }
        }
      })
    }),

    stackAndUpgrade: Em.Route.extend({
      route: '/stack',
      connectOutlets: function (router) {
        router.set('mainAdminController.category', "stackAndUpgrade");
        router.get('mainAdminController').connectOutlet('mainAdminStackAndUpgrade');
      },

      index: Em.Route.extend({
        route: '/',
        redirectsTo: 'services'
      }),

      services: Em.Route.extend({
        route: '/services',
        connectOutlets: function (router, context) {
          router.get('mainAdminStackAndUpgradeController').connectOutlet('mainAdminStackServices');
        }
      }),

      versions: Em.Route.extend({
        route: '/versions',
        connectOutlets: function (router, context) {
          router.get('mainAdminStackAndUpgradeController').connectOutlet('MainAdminStackVersions');
        }
      }),

      stackNavigate: function (router, event) {
        var parent = event.view._parentView;
        parent.deactivateChildViews();
        event.view.set('active', "active");
        router.transitionTo(event.context);
      }
    }),
    stackUpgrade: require('routes/stack_upgrade_routes'),

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
        router.set('mainAdminController.category', "adminServiceAccounts");
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

  createServiceWidget: function (router, context) {
    if (context) {
      var widgetController = router.get('widgetWizardController');
      widgetController.save('widgetService', context.get('serviceName'));
      var layout = JSON.parse(JSON.stringify(context.get('layout')));
      layout.widgets = context.get('layout.widgets').mapProperty('id');
      widgetController.save('layout', layout);
    }
    router.transitionTo('createWidget');
  },

  createWidget: require('routes/create_widget'),

  editServiceWidget: function (router, context) {
    if (context) {
      var widgetController = router.get('widgetEditController');
      widgetController.save('widgetService', context.get('serviceName'));
      widgetController.save('widgetType', context.get('widgetType'));
      widgetController.save('widgetProperties', context.get('properties'));
      widgetController.save('widgetMetrics', context.get('metrics'));
      widgetController.save('widgetValues', context.get('values'));
      widgetController.save('widgetName', context.get('widgetName'));
      widgetController.save('widgetDescription', context.get('description'));
      widgetController.save('widgetScope', context.get('scope'));
      widgetController.save('widgetAuthor', context.get('author'));
      widgetController.save('widgetId', context.get('id'));
      widgetController.save('allMetrics', []);
    }
    router.transitionTo('editWidget');
  },

  editWidget: require('routes/edit_widget'),

  services: Em.Route.extend({
    route: '/services',
    index: Em.Route.extend({
      route: '/',
      enter: function (router) {
        Em.run.next(function () {
          var controller = router.get('mainController');
          controller.dataLoading().done(function () {
            if (router.currentState.parentState.name === 'services' && router.currentState.name === 'index') {
              var service = router.get('mainServiceItemController.content');
              if (!service || !service.get('isLoaded')) {
                service = App.Service.find().objectAt(0); // getting the first service to display
              }
              if (router.get('mainServiceItemController').get('routeToConfigs')) {
                router.transitionTo('service.configs', service);
              } else if (router.get('mainServiceItemController.routeToHeatmaps')) {
                router.transitionTo('service.heatmaps', service);
              } else {
                router.transitionTo('service.summary', service);
              }
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
        if (service.get('isLoaded')) {
          if (router.get('mainServiceItemController').get('routeToConfigs')) {
            router.transitionTo('configs');
          } else if (router.get('mainServiceItemController.routeToHeatmaps')) {
            router.transitionTo('heatmaps');
          } else {
            router.transitionTo('summary');
          }
        } else {
          router.transitionTo('index');
        }
      },
      index: Ember.Route.extend({
        route: '/'
      }),
      summary: Em.Route.extend({
        route: '/summary',
        connectOutlets: function (router, context) {
          App.loadTimer.start('Service Summary Page');
          var item = router.get('mainServiceItemController.content');
          if (router.get('clusterController.isServiceMetricsLoaded')) router.get('updateController').updateServiceMetric(Em.K);
          //if service is not existed then route to default service
          if (item.get('isLoaded')) {
            router.get('mainServiceItemController').connectOutlet('mainServiceInfoSummary', item);
          } else {
            router.transitionTo('services.index');
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
          App.loadTimer.start('Service Configs Page');
          router.get('mainController').dataLoading().done(function () {
            var item = router.get('mainServiceItemController.content');
            //if service is not existed then route to default service
            if (item.get('isLoaded')) {
              if (router.get('mainServiceItemController.isConfigurable')) {
                // HDFS service config page requires service metrics information to determine NameNode HA state and hide SNameNode category
                if (item.get('serviceName') === 'HDFS') {
                  router.get('mainController').isLoading.call(router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
                    router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
                  });
                } else {
                  router.get('mainServiceItemController').connectOutlet('mainServiceInfoConfigs', item);
                }
              }
              else {
                // if service doesn't have configs redirect to summary
                router.transitionTo('summary');
              }
            } else {
              item.set('routeToConfigs', true);
              router.transitionTo('services.index');
            }
          });
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
      heatmaps: Em.Route.extend({
        route: '/heatmaps',
        connectOutlets: function (router, context) {
          App.loadTimer.start('Service Heatmaps Page');
          router.get('mainController').dataLoading().done(function () {
            var item = router.get('mainServiceItemController.content');
            if (item.get('isLoaded')) {
              router.get('mainServiceItemController').connectOutlet('mainServiceInfoHeatmap', item);
            } else {
              item.set('routeToHeatmaps', true);
              router.transitionTo('services.index');
            }
          });
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
        if (event.context !== 'configs' && mainServiceInfoConfigsController.hasUnsavedChanges()) {
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

    enableRAHighAvailability: require('routes/ra_high_availability_routes'),

    addHawqStandby: require('routes/add_hawq_standby_routes'),

    removeHawqStandby: require('routes/remove_hawq_standby_routes'),

    activateHawqStandby: require('routes/activate_hawq_standby_routes'),

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
    router.get('mainHostController').set('filterChangeHappened', true);
    router.transitionTo('hosts.index');
  },
  showDetails: function (router, event) {
    router.get('mainHostDetailsController').set('referer', router.location.lastSetURL);
    router.get('mainHostDetailsController').set('isFromHosts', true);
    router.transitionTo('hosts.hostDetails.summary', event.context);
  },
  gotoAlertDetails: function (router, event) {
    router.transitionTo('alerts.alertDetails', event.context);
  },

  /**
   * Open summary page of the selected service
   * @param {object} event
   * @method routeToService
   */
  routeToService: function (router, event) {
    var service = event.context;
    router.transitionTo('main.services.service.summary', service);
  }
});
