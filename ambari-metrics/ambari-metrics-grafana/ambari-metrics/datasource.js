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
define([
      'angular',
      'lodash',
      'jquery',
      './directives',
      './queryCtrl'
    ],
    function (angular, _) {
      'use strict';

      var module = angular.module('grafana.services');

      module.factory('AmbariMetricsDatasource', function ($q, backendSrv, templateSrv) {
        /**
         * AMS Datasource Constructor
         */
        function AmbariMetricsDatasource(datasource) {
          this.name = datasource.name;
          this.url = datasource.url;
          this.initMetricAppidMapping();
        }
        var allMetrics = {};
        var appIds = [];

        //We get a list of components and their associated metrics.
        AmbariMetricsDatasource.prototype.initMetricAppidMapping = function () {
          backendSrv.get(this.url + '/ws/v1/timeline/metrics/metadata')
            .then(function (items) {
              allMetrics = {};
              appIds = [];
              _.forEach(items, function (metric,app) {
                metric.forEach(function (component) {
                  if (!allMetrics[app]) {
                    allMetrics[app] = [];
                  }
                  allMetrics[app].push(component.metricname);
                });
              });
              //We remove a couple of components from the list that do not contain any
              //pertinent metrics.
              delete allMetrics["timeline_metric_store_watcher"];
              delete allMetrics["amssmoketestfake"];
              appIds = Object.keys(allMetrics);
            });
        };

        /**
         * AMS Datasource  Authentication
         */
        AmbariMetricsDatasource.prototype.doAmbariRequest = function (options) {
          if (this.basicAuth || this.withCredentials) {
            options.withCredentials = true;
          }
          if (this.basicAuth) {
            options.headers = options.headers || {};
            options.headers.Authorization = this.basicAuth;
          }

          options.url = this.url + options.url;
          options.inspect = {type: 'ambarimetrics'};

          return backendSrv.datasourceRequest(options);
        };

        /**
         * AMS Datasource  Query
         */
        AmbariMetricsDatasource.prototype.query = function (options) {

          var emptyData = function (metric) {
            return {
              data: {
                target: metric,
                datapoints: []
              }
            };
          };
          var self = this;
          var getMetricsData = function (target) {
            var alias = target.alias ? target.alias : target.metric;
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "yarnqueues") {
              alias = alias + ' on ' + target.qmetric; }
            return function (res) {
              console.log('processing metric ' + target.metric);
              if (!res.metrics[0] || target.hide) {
                return $q.when(emptyData(target.metric));
              }
              var series = [];
              var metricData = res.metrics[0].metrics;
              // Added hostname to legend for templated dashboards.
              var hostLegend = res.metrics[0].hostname ? ' on ' + res.metrics[0].hostname : '';
              var timeSeries = {};
              timeSeries = {
                target: alias + hostLegend,
                datapoints: []
              };
              for (var k in metricData){
                if (metricData.hasOwnProperty(k)) {
                  timeSeries.datapoints.push([metricData[k], (k - k % 1000)]);
                }
              }
              series.push(timeSeries);
              return $q.when({data: series});
            };
          };
          // To speed up querying on templatized dashboards.
          var allHostMetricsData = function (target) {
            var alias = target.alias ? target.alias : target.metric;
            return function (res) {
              console.log('processing metric ' + target.metric);
              if (!res.metrics[0] || target.hide) {
                return $q.when(emptyData(target.metric));
              }
              var series = [];
              var timeSeries = {};
              var metricData = res.metrics;
              _.map(metricData, function (data) {
                timeSeries = {
                  target: alias + ' on ' + data.hostname,
                  datapoints: []
                };
                for (var k in data.metrics){
                  if (data.metrics.hasOwnProperty(k)) {
                    timeSeries.datapoints.push([data.metrics[k], (k - k % 1000)]);
                  }
                }
                series.push(timeSeries);
              });
              return $q.when({data: series});
            };
          };
          var getHostAppIdData = function(target) {
            var precision = target.shouldAddPrecision && target.precision !== '' ? '&precision=' + target.precision : '';
            var rate = target.shouldComputeRate ? '._rate._' : '._';
            target.aggregator = target.precision !== "seconds" ? target.aggregator : '';
            return backendSrv.get(self.url + '/ws/v1/timeline/metrics?metricNames=' + target.metric + rate +
                target.aggregator + "&hostname=" + target.hosts + '&appId=' + target.app + '&startTime=' + from +
                '&endTime=' + to + precision).then(
                getMetricsData(target)
            );
          };
          //Check if it's a templated dashboard.
          var templatedHosts = templateSrv.variables.filter(function(o) { return o.name === "hosts"});
          var templatedHost = (_.isEmpty(templatedHosts)) ? '' : templatedHosts[0].options.filter(function(host)
            { return host.selected; }).map(function(hostName) { return hostName.value; });

          var tComponents = _.isEmpty(templateSrv.variables) ? '' : templateSrv.variables.filter(function(variable)
            { return variable.name === "components"});
          var tComponent = _.isEmpty(tComponents) ? '' : tComponents[0].current.value;

          var getServiceAppIdData = function(target) {
            var tHost = (_.isEmpty(templateSrv.variables)) ? templatedHost : target.templatedHost;
            var precision = target.shouldAddPrecision && target.precision !== '' ? '&precision=' + target.precision : '';
            var rate = target.shouldComputeRate ? '._rate._' : '._';
            return backendSrv.get(self.url + '/ws/v1/timeline/metrics?metricNames=' + target.metric + rate
              + target.aggregator + '&hostname=' + tHost + '&appId=' + target.app + '&startTime=' + from +
              '&endTime=' + to + precision).then(
              getMetricsData(target)
            );
          };
          // To speed up querying on templatized dashboards.
          var getAllHostData = function(target) {
            var precision = target.shouldAddPrecision && target.precision !== '' ? '&precision=' + target.precision : '';
            var rate = target.shouldComputeRate ? '._rate._' : '._';
            var templatedComponent = (_.isEmpty(tComponent)) ? target.app : tComponent;
            return backendSrv.get(self.url + '/ws/v1/timeline/metrics?metricNames=' + target.metric + rate
              + target.aggregator + '&hostname=' + target.templatedHost + '&appId=' + templatedComponent + '&startTime=' + from +
              '&endTime=' + to + precision).then(
              allHostMetricsData(target)
            );
          };
          var getYarnAppIdData = function(target) {
            var precision = target.shouldAddPrecision && target.precision !== '' ? '&precision=' + target.precision : '';
            var rate = target.shouldComputeRate ? '._rate._' : '._';
            return backendSrv.get(self.url + '/ws/v1/timeline/metrics?metricNames=' + target.queue + rate
              + target.aggregator + '&appId=resourcemanager&startTime=' + from +
              '&endTime=' + to + precision).then(
              getMetricsData(target)
            );
          };

          // Time Ranges
          var from = Math.floor(options.range.from.valueOf() / 1000);
          var to = Math.floor(options.range.to.valueOf() / 1000);

          var metricsPromises = [];
          if (!_.isEmpty(templateSrv.variables)) {
            // YARN Queues Dashboard
            if (templateSrv.variables[0].query === "yarnqueues") {
              var allQueues = templateSrv.variables.filter(function(variable) { return variable.query === "yarnqueues";});
              var selectedQs = (_.isEmpty(allQueues)) ? "" : allQueues[0].options.filter(function(q)
              { return q.selected; }).map(function(qName) { return qName.value; });
              // All Queues
              if (!_.isEmpty(_.find(selectedQs, function (wildcard) { return wildcard === "*"; })))  {
                var allQueue = allQueues[0].options.filter(function(q) {
                  return q.text !== "All"; }).map(function(queue) { return queue.value; });
                _.forEach(allQueue, function(processQueue) {
                  metricsPromises.push(_.map(options.targets, function(target) {
                    target.qmetric = processQueue;
                    target.queue = target.metric.replace('root', target.qmetric);
                    return getYarnAppIdData(target);
                  }));
                });
              } else {
                // All selected queues.
                _.forEach(selectedQs, function(processQueue) {
                  metricsPromises.push(_.map(options.targets, function(target) {
                    target.qmetric = processQueue;
                    target.queue = target.metric.replace('root', target.qmetric);
                    return getYarnAppIdData(target);
                  }));
                });
              }
            }
            // To speed up querying on templatized dashboards.
            if (templateSrv.variables[1] && templateSrv.variables[1].name === "hosts") {
              var splitHosts = [];
              // Remove curly braces that Grafana adds. {host1,host2}
              var allHosts = templateSrv._values.hosts.lastIndexOf('}') > 0 ? templateSrv._values.hosts.slice(1,-1) :
                templateSrv._values.hosts;
              var allHost = allHosts.split(',');
              while (allHost.length > 0) {
                splitHosts.push(allHost.splice(0,50));
              }
              _.forEach(splitHosts, function(splitHost) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.templatedHost = _.flatten(splitHost).join(',');
                  return getAllHostData(target);
                }));
              });
            }
            metricsPromises = _.flatten(metricsPromises);
          } else {
            // Non Templatized Dashboards
            metricsPromises = _.map(options.targets, function(target) {
              console.debug('target app=' + target.app + ',' +
                'target metric=' + target.metric + ' on host=' + target.tempHost);
              if (!!target.hosts) {
                return getHostAppIdData(target);
              } else {
                return getServiceAppIdData(target);
              }
            });
          }

          return $q.all(metricsPromises).then(function(metricsDataArray) {
            var data = _.map(metricsDataArray, function(metricsData) {
              return metricsData.data;
            });
            var metricsDataResult = {data: _.flatten(data)};
            return $q.when(metricsDataResult);
          });
        };

        /**
         * AMS Datasource  List Series.
         */
        AmbariMetricsDatasource.prototype.listSeries = function (query) {
          // wrap in regex
          if (query && query.length > 0 && query[0] !== '/') {
            query = '/' + query + '/';
          }
          return $q.when([]);
        };

        /**
         * AMS Datasource Templating Variables.
         */
        AmbariMetricsDatasource.prototype.metricFindQuery = function (query) {
          var interpolated;
          try {
            interpolated = templateSrv.replace(query);
          } catch (err) {
            return $q.reject(err);
          }
          var tComponents = _.isEmpty(templateSrv.variables) ? '' : templateSrv.variables.filter(function(variable) 
            { return variable.name === "components"});
          var tComponent = _.isEmpty(tComponents) ? '' : tComponents[0].current.value;
          // Templated Variable for YARN Queues.
          // It will search the cluster and populate the queues.
          if(interpolated === "yarnqueues") {
            return backendSrv.get(this.url + '/ws/v1/timeline/metrics/metadata')
              .then(function (results) {
                var allM = {};
                _.forEach(results, function (metric,app) {
                  metric.forEach(function (component) {
                    if (!allM[app]) {
                      allM[app] = [];
                    }
                    allM[app].push(component.metricname);
                  });
                });
                var yarnqueues = allM["resourcemanager"];
                var extractQueues = yarnqueues.filter(/./.test.bind(new RegExp(".=root", 'g')));
                var queues = _.map(extractQueues, function(metric) {
                  return metric.substring("yarn.QueueMetrics.Queue=".length);
                });
                queues = _.map(queues, function(metricName) {
                  return metricName.substring(metricName.lastIndexOf("."), 0);
                });
                queues = _.sortBy(_.uniq(queues));
                return _.map(queues, function (queues) {
                  return {
                    text: queues
                  };
                });
              });
          }
          // Templated Variable that will populate all hosts on the cluster.
          // The variable needs to be set to "hosts".
          if (!tComponent){
                  return this.doAmbariRequest({
                        method: 'GET',
                        url: '/ws/v1/timeline/metrics/' + interpolated
                      })
                      .then(function (results) {
                        //Remove fakehostname from the list of hosts on the cluster.
                        var fake = "fakehostname"; delete results.data[fake];
                        return _.map(_.keys(results.data), function (hostName) {
                          return {
                                text: hostName,
                                expandable: hostName.expandable ? true : false
                              };
                        });
                      });
          } else {
            // Create a dropdown in templated dashboards for single components.
            // This will check for the component set and show hosts only for the
            // selected component.
            return this.doAmbariRequest({
                method: 'GET',
                url: '/ws/v1/timeline/metrics/hosts'
              })
              .then(function(results) {
                var compToHostMap = {};
                //Remove fakehostname from the list of hosts on the cluster.
                var fake = "fakehostname";
                delete results.data[fake];
                //Query hosts based on component name
                _.forEach(results.data, function(comp, hostName) {
                  comp.forEach(function(component) {
                    if (!compToHostMap[component]) {
                      compToHostMap[component] = [];
                    }
                    compToHostMap[component].push(hostName);
                  });
                });
                var compHosts = compToHostMap[tComponent];
                compHosts = _.map(compHosts, function(host) {
                  return {
                    text: host,
                    expandable: host.expandable ? true : false
                  };
                });
                compHosts = _.sortBy(compHosts, function(i) {
                  return i.text.toLowerCase();
                });
                return $q.when(compHosts);
              });
           }
        };

        /**
         * AMS Datasource  - Test Data Source Connection.
         *
         * Added Check to see if Datasource is working. Throws up an error in the
         * Datasources page if incorrect info is passed on.
         */
        AmbariMetricsDatasource.prototype.testDatasource = function () {
          return backendSrv.datasourceRequest({
            url: this.url + '/ws/v1/timeline/metrics/metadata',
            method: 'GET'
          }).then(function(response) {
            console.log(response);
            if (response.status === 200) {
              return { status: "success", message: "Data source is working", title: "Success" };
            }
          });
        };

        /**
         * AMS Datasource - Suggest AppId.
         *
         * Read AppIds from cache.
         */
        AmbariMetricsDatasource.prototype.suggestApps = function (query) {
          console.log(query);

          appIds = appIds.sort();
          var appId = _.map(appIds, function (k) {
            return {text: k};
          });
          return $q.when(appId);
        };

        /**
         * AMS Datasource - Suggest Metrics.
         *
         * Read Metrics based on AppId chosen.
         */
        AmbariMetricsDatasource.prototype.suggestMetrics = function (query, app) {
          if (!app) {
            return $q.when([]);
          }
          var keys = [];
          keys = _.map(allMetrics[app],function(m) {
            return {text: m};
          });
          keys = _.sortBy(keys, function (i) { return i.text.toLowerCase(); });
          return $q.when(keys);
        };

        /**
         * AMS Datasource - Suggest Hosts.
         *
         * Query Hosts on the cluster.
         */
        AmbariMetricsDatasource.prototype.suggestHosts = function (query, app) {
          console.log(query);
          return this.doAmbariRequest({method: 'GET', url: '/ws/v1/timeline/metrics/hosts'})
            .then(function (results) {
              var compToHostMap = {};
              //Remove fakehostname from the list of hosts on the cluster.
              var fake = "fakehostname"; delete results.data[fake];
              //Query hosts based on component name
              _.forEach(results.data, function (comp, hostName) {
                comp.forEach(function (component) {
                  if (!compToHostMap[component]){
                    compToHostMap[component] = [];
                  }
                  compToHostMap[component].push(hostName);
                });
              });
              var compHosts = compToHostMap[app];
              compHosts = _.map(compHosts, function (h) {
                return {text: h};
              });
              compHosts = _.sortBy(compHosts, function (i) { return i.text.toLowerCase(); });
              return $q.when(compHosts);
            });
        };

        /**
         * AMS Datasource Aggregators.
         */
        var aggregatorsPromise = null;
        AmbariMetricsDatasource.prototype.getAggregators = function () {
          if (aggregatorsPromise) {
            return aggregatorsPromise;
          }
          aggregatorsPromise = $q.when([
            'avg', 'sum', 'min', 'max'
          ]);
          return aggregatorsPromise;
        };
        return AmbariMetricsDatasource;
      });
    }
);
