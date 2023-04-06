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
      './query_ctrl'
    ],
    function (angular, _) {
      'use strict';

      /**
       * AMS Datasource Constructor
       */
      function AmbariMetricsDatasource(instanceSettings, $q, backendSrv, templateSrv) {
        this.type = 'ambari-metrics';
        this.name = instanceSettings.name;
        this.url = instanceSettings.url;
        this.withCredentials = instanceSettings.withCredentials;
        this.basicAuth = instanceSettings.basicAuth;

        var allMetrics = {};
        var appIds = [];

        /**
         * AMS Datasource  Authentication
         */
        this.doAmbariRequest = function (options) {
          if (this.basicAuth || this.withCredentials) {
            options.withCredentials = true;
          }
          if (this.basicAuth) {
            options.headers = options.headers || {};
            options.headers.Authorization = this.basicAuth;
          }

          options.url = this.url + options.url;
          options.inspect = {type: 'ambari-metrics'};

          return backendSrv.datasourceRequest(options);
        };

        //We get a list of components and their associated metrics.
        this.initMetricAppidMapping = function () {
          return this.doAmbariRequest({ url: '/ws/v1/timeline/metrics/metadata' })
            .then(function (items) {
              items = items.data;
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
        this.initMetricAppidMapping();

        /**
         * AMS Datasource  Query
         */
        this.query = function (options) {
          var emptyData = function (metric) {
            var legend = metric.alias ? metric.alias : metric.metric;
            return {
              data: {
                target: legend,
                datapoints: []
              }
            };
          };
          var self = this;
          var getMetricsData = function (target) {
            var alias = target.alias ? target.alias : target.metric;
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "yarnqueues") {
              alias = alias + ' on ' + target.qmetric; }
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "kafka-topics") {
              alias = alias + ' on ' + target.kbTopic; }
            return function (res) {
              res = res.data;
              console.log('processing metric ' + target.metric);
              if (!res.metrics[0] || target.hide) {
                return $q.when(emptyData(target));
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
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "hbase-users") {
            alias = alias + ' for ' + target.hbUser; }
            // Aliases for Storm Topologies and components under a topology.
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "topologies" &&
            !templateSrv.variables[1]) {
              alias = alias + ' on ' + target.sTopology;
            }
            if(!_.isEmpty(templateSrv.variables[1]) && templateSrv.variables[1].name === "component") {
              alias = alias + ' on ' + target.sTopology + ' for ' + target.sComponent;
            }

            // Aliases for Druid Datasources.
            if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "druidDataSources" &&
                        !templateSrv.variables[1]) {
              alias = alias.replace('$druidDataSource', target.sDataSource);
            }
            return function (res) {
              res = res.data;
              console.log('processing metric ' + target.metric);
              if (!res.metrics[0] || target.hide) {
                return $q.when(emptyData(target));
              }
              var series = [];
              var timeSeries = {};
              var metricData = res.metrics;
              _.map(metricData, function (data) {
                var totalCountFlag = false;
                var aliasSuffix = data.hostname ? ' on ' + data.hostname : '';
                var op = '';
                var user = '';
                if(!_.isEmpty(templateSrv.variables) && templateSrv.variables[0].query === "hbase-tables") {
                  var tableName = "Tables.";
                  var tableSuffix = data.metricname.substring(data.metricname.indexOf(tableName) + tableName.length,
                  data.metricname.lastIndexOf("_metric"));
                  aliasSuffix = ' on ' + tableSuffix;
                }
                if(templateSrv.variables[0].query === "callers") {
                  alias = data.metricname.substring(data.metricname.indexOf('(')+1, data.metricname.indexOf(')'));
                }
                // Set legend and alias for HDFS - TopN dashboard
                if(data.metricname.indexOf('dfs.NNTopUserOpCounts') === 0) {
                  var metricname_arr = data.metricname.split(".");
                  _.map(metricname_arr, function (segment) {
                    if(segment.indexOf('op=') === 0) {
                      var opKey = 'op=';
                      op = segment.substring(segment.indexOf(opKey) + opKey.length);
                    } else if(segment.indexOf('user=') === 0) {
                      var userKey = 'user=';
                      user = segment.substring(segment.indexOf(userKey) + userKey.length);
                    }
                  });
                  // Check if metric is TotalCount
                  if(data.metricname.indexOf('TotalCount') > 0) {
                    totalCountFlag = true;
                    if (op !== '*') {
                      alias = op;
                    } else {
                      alias = 'Total Count';
                    }
                  } else if (op !== '*') {
                    alias = op + ' by ' + user;
                  } else {
                    alias = user;
                  }
                  aliasSuffix = '';
                }
                if (data.appid.indexOf('ambari_server') === 0) {
                  alias = data.metricname;
                  aliasSuffix = '';
                }
                timeSeries = {
                  target: alias + aliasSuffix,
                  datapoints: []
                };
                for (var k in data.metrics){
                  if (data.metrics.hasOwnProperty(k)) {
                    timeSeries.datapoints.push([data.metrics[k], (k - k % 1000)]);
                  }
                }
                if( (user !== '*') || (totalCountFlag) ) {
                  series.push(timeSeries);
                }
              });
              return $q.when({data: series});
            };
          };
          var getHostAppIdData = function(target) {
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var instanceId = typeof target.cluster == 'undefined'  ? '' : '&instanceId=' + target.cluster;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.metric + metricTransform +
            metricAggregator + "&hostname=" + target.hosts + '&appId=' + target.app + instanceId + '&startTime=' + from +
            '&endTime=' + to + precision + seriesAggregator }).then(
              getMetricsData(target)
            );
          };
          //Check if it's a templated dashboard.
          var templatedClusters = templateSrv.variables.filter(function(o) { return o.name === "cluster"});
          var templatedCluster = (_.isEmpty(templatedClusters)) ? '' : templatedClusters[0].options.filter(function(cluster)
            { return cluster.selected; }).map(function(clusterName) { return clusterName.value; });

          var templatedHosts = templateSrv.variables.filter(function(o) { return o.name === "hosts"});
          var templatedHost = (_.isEmpty(templatedHosts)) ? '' : templatedHosts[0].options.filter(function(host)
            { return host.selected; }).map(function(hostName) { return hostName.value; });

          var tComponents = _.isEmpty(templateSrv.variables) ? '' : templateSrv.variables.filter(function(variable)
            { return variable.name === "components"});
          var tComponent = _.isEmpty(tComponents) ? '' : tComponents[0].current.value;

          var getServiceAppIdData = function(target) {
            var tCluster = (_.isEmpty(templateSrv.variables))? templatedCluster : '';
            var instanceId = typeof tCluster == 'undefined'  ? '' : '&instanceId=' + tCluster;
            var tHost = (_.isEmpty(templateSrv.variables)) ? templatedHost : target.templatedHost;
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.metric + metricTransform
              + metricAggregator + '&hostname=' + tHost + '&appId=' + target.app + instanceId + '&startTime=' + from +
              '&endTime=' + to + precision + seriesAggregator }).then(
              getMetricsData(target)
            );
          };
          // To speed up querying on templatized dashboards.
          var getAllHostData = function(target) {
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var appId = target.app;
            if ((appId === 'nifi' || appId === 'druid') && (!instanceId || instanceId === '&instanceId=')) {
                instanceId = "&instanceId=%"
            }
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var topN = ""; var isBottomN = "";
            if (!_.isEmpty(templateSrv.variables.filter(function(o) { return o.name === "instances";}))) {
              var metricTopN = _.filter(templateSrv.variables, function (o) { return o.name === "instances"; });
              var metricTopAgg = _.filter(templateSrv.variables, function (o) { return o.name === "topagg"; });
              isBottomN = templateSrv.variables.filter(function(o) { return o.name === "orientation";})[0].current.value
              === "bottom" ? true : false;
              topN = '&topN=' + metricTopN[0].current.value  +'&topNFunction=' + metricTopAgg[0].current.value  + '&isBottomN='+ isBottomN;
            }
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            var templatedComponent = (_.isEmpty(tComponent)) ? target.app : tComponent;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.metric + metricTransform
              + metricAggregator + '&hostname=' + target.templatedHost + '&appId=' + templatedComponent + instanceId
              + '&startTime=' + from + '&endTime=' + to + precision + topN + seriesAggregator }).then(
              allHostMetricsData(target)
            );
          };
          var getYarnAppIdData = function(target) {
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + encodeURIComponent(target.queue) + metricTransform
              + metricAggregator + '&appId=resourcemanager' + instanceId + '&startTime=' + from +
              '&endTime=' + to + precision + seriesAggregator }).then(
              getMetricsData(target)
            );
          };
          var getHbaseAppIdData = function(target) {
              var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
              var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.hbMetric + instanceId + '&appId=hbase&startTime='
            + from + '&endTime=' + to + precision + seriesAggregator }).then(
              allHostMetricsData(target)
            );
          };

          var getKafkaAppIdData = function(target) {
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.kbMetric + metricTransform + instanceId
              + metricAggregator + '&appId=kafka_broker&startTime=' + from +
              '&endTime=' + to + precision + seriesAggregator }).then(
              getMetricsData(target)
            );
          };
          var getNnAppIdData = function(target) {

            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.nnMetric + metricTransform + instanceId
            + metricAggregator + '&appId=namenode&startTime=' + from + '&endTime=' + to + precision + seriesAggregator }).then(
              allHostMetricsData(target)
            );
          };

          // Storm Topology calls.
          var getStormData = function(target) {
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.sTopoMetric + metricTransform + instanceId
                + metricAggregator + '&appId=nimbus&startTime=' + from + '&endTime=' + to + precision + seriesAggregator }).then(
                allHostMetricsData(target)
            );
          };

            // Infra Solr Calls
          var getSolrCoreData = function(target) {
              var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
              var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
                  + target.precision;
              var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
              var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
              var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
              return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.sCoreMetric + metricTransform + instanceId
              + metricAggregator + '&appId=ambari-infra-solr&startTime=' + from + '&endTime=' + to + precision + seriesAggregator }).then(
                  allHostMetricsData(target)
              );
          };

          var getSolrCollectionData = function(target) {
              var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
              var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
                  + target.precision;
              var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
              var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
              var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
              return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.sCollectionMetric + metricTransform + instanceId
              + metricAggregator + '&appId=ambari-infra-solr&startTime=' + from + '&endTime=' + to + precision + seriesAggregator }).then(
                  allHostMetricsData(target)
              );
          };

          // Druid calls.
          var getDruidData = function(target) {
            var instanceId = typeof target.templatedCluster == 'undefined'  ? '' : '&instanceId=' + target.templatedCluster;
            var precision = target.precision === 'default' || typeof target.precision == 'undefined'  ? '' : '&precision='
            + target.precision;
            var metricAggregator = target.aggregator === "none" ? '' : '._' + target.aggregator;
            var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
            var seriesAggregator = !target.seriesAggregator || target.seriesAggregator === "none" ? '' : '&seriesAggregateFunction=' + target.seriesAggregator;
            return self.doAmbariRequest({ url: '/ws/v1/timeline/metrics?metricNames=' + target.sDataSourceMetric + metricTransform + instanceId
                          + metricAggregator + '&appId=druid&startTime=' + from + '&endTime=' + to + precision + seriesAggregator }).then(
                          allHostMetricsData(target)
            );
          };

          // Time Ranges
          var from = Math.floor(options.range.from.valueOf() / 1000);
          var to = Math.floor(options.range.to.valueOf() / 1000);

          var metricsPromises = [];
          if (!_.isEmpty(templateSrv.variables)) {
            // YARN Queues Dashboard
            if (templateSrv.variables[0].query === "yarnqueues") {
              var allQueues = templateSrv.variables.filter(function(variable) {
                return variable.query === "yarnqueues";
              });
              var selectedQs = (_.isEmpty(allQueues)) ? "" : allQueues[0].options
                .filter(getSelectedItems)
                .map(function(qName) {
                  return qName.value;
                });

              _.forEach(selectedQs, function(processQueue) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.qmetric = processQueue;
                  target.queue = target.metric.replace('root', target.qmetric);
                  return getYarnAppIdData(target);
                }));
              });
            }

            // Templatized Dashboard for per-user metrics in HBase.
            if (templateSrv.variables[0].query === "hbase-users") {
              var allUsers = templateSrv.variables.filter(function(variable) {
                return variable.query === "hbase-users";
              });
              var selectedUsers = (_.isEmpty(allUsers)) ? "" : allUsers[0].options
                .filter(getSelectedItems)
                .map(function(uName) {
                  return uName.value;
                });

              _.forEach(selectedUsers, function(processUser) {
                  metricsPromises.push(_.map(options.targets, function(target) {
                    target.hbUser = processUser;
                    var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
                    target.hbMetric = target.metric.replace('*', target.hbUser) + metricTransform +'._' +  target.aggregator;
                    return getHbaseAppIdData(target);
                  }));
                });
            }
            // Templatized Dashboard for per-table metrics in HBase.
            if (templateSrv.variables[0].query === "hbase-tables") {
              var splitTables = [];
              let allTables = [];
              const tables = templateSrv.index.Tables.options;
              for (let table of tables) {
                if (table.text.toLowerCase() === "all" && table.selected) {
                  allTables = "";
                  break;
                } else if (table.selected) {
                  allTables.push(table.value);
                }
              }

              while (allTables.length > 0) {
                splitTables.push(allTables.splice(0, 20));
              }
              _.forEach(splitTables, function(table) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  var hbMetric = [];
                  _.map(table, function(tableMetric) { hbMetric.push(target.metric.replace('*', tableMetric)); });
                  var metricTransform = !target.transform || target.transform === "none" ? '' : '._' + target.transform;
                  hbMetric = _.map(hbMetric, function(tbl) { return tbl + metricTransform +'._' +  target.aggregator; });
                  target.hbMetric = _.flatten(hbMetric).join(',');
                  return getHbaseAppIdData(target);
                }));
              });
            }
            // Templatized Dashboard for per-topic metrics in Kafka.
            if (templateSrv.variables[0].query === "kafka-topics") {
              var allTopics = templateSrv.variables.filter(function(variable) {
                return variable.query === "kafka-topics";
              });
              var selectedTopics = (_.isEmpty(allTopics)) ? "" : allTopics[0].options
                .filter(getSelectedItems)
                .map(function(topicName) {
                  return topicName.value;
                });

              _.forEach(selectedTopics, function(processTopic) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.kbTopic = processTopic;
                  target.kbMetric = target.metric.replace('*', target.kbTopic);
                  return getKafkaAppIdData(target);
                }));
              });
            }
            //Templatized Dashboard for Call Queues
            if (templateSrv.variables[0].query === "callers") {
              var allCallers = templateSrv.variables.filter(function(variable) { return variable.query === "callers";});
              let selectedCallers = [];
              const callers = templateSrv.index.Callers.options;
              for (let caller of callers) {
                if (caller.text.toLowerCase() === "all" && caller.selected) {
                  selectedCallers = "";
                  break;
                } else if (caller.selected) {
                  selectedCallers.push(caller.text);
                }
              }
              _.forEach(selectedCallers, function(processCaller) {
                  metricsPromises.push(_.map(options.targets, function(target) {
                    target.nnCaller = processCaller;
                    target.nnMetric = target.metric.replace('*', target.nnCaller);
                    return getNnAppIdData(target);
                  }));
              });
            }

            //Templatized Dashboard for Infra Solr Cores
            if (templateSrv.variables[0].query === "infra_solr_core") {
              var allCores = templateSrv.variables.filter(function(variable) {
                return variable.query === "infra_solr_core";
              });
              var selectedCores = (_.isEmpty(allCores)) ? "" : allCores[0].options
                .filter(getSelectedItems)
                .map(function(coreName) {
                  return coreName.value;
                });

              _.forEach(selectedCores, function(processCore) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sCore = processCore;
                  target.sCoreMetric = target.metric.replace('*', target.sCore);
                  return getSolrCoreData(target);
                }));
              });
            }

            //Templatized Dashboard for Infra Solr Collections
            if (templateSrv.variables[0].query === "infra_solr_collection") {
              var allCollections = templateSrv.variables.filter(function(variable) {
                return variable.query === "infra_solr_collection";
              });
              var selectedCollections = (_.isEmpty(allCollections)) ? "" : allCollections[0].options
                .filter(getSelectedItems)
                .map(function(collectionsName) {
                  return collectionsName.value;
                });

              _.forEach(selectedCollections, function(processCollection) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sCollection = processCollection;
                  target.sCollectionMetric = target.metric.replace('*', target.sCollection);
                  return getSolrCollectionData(target);
                }));
              });
            }

            //Templatized Dashboard for Storm Topologies
            if (templateSrv.variables[0].query === "topologies" && !templateSrv.variables[1]) {
              var allTopologies = templateSrv.variables.filter(function(variable) {
                return variable.query === "topologies";
              });
              var selectedTopologies = (_.isEmpty(allTopologies)) ? "" : allTopologies[0].options
                .filter(getSelectedItems)
                .map(function(topoName) {
                  return topoName.value;
                });

              _.forEach(selectedTopologies, function(processTopology) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sTopology = processTopology;
                  target.sTopoMetric = target.metric.replace('*', target.sTopology);
                  return getStormData(target);
                }));
              });
            }

            //Templatized Dashboards for Storm Components
            if (templateSrv.variables[0].query === "topologies" && templateSrv.variables[1] &&
                templateSrv.variables[1].name === "component") {
              if (templateSrv._values) {
                var selectedTopology = templateSrv._values.topologies;
                var selectedComponent = templateSrv._values.component;

                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sTopology = selectedTopology;
                  target.sComponent = selectedComponent;
                  target.sTopoMetric = target.metric.replace('*', target.sTopology).replace('*', target.sComponent);
                    return getStormData(target);
                }));
              }
            }

            //Templatized Dashboard for Storm Kafka Offset
            if (templateSrv.variables[0].query === "topologies" && templateSrv.variables[1] &&
                templateSrv.variables[1].name === "topic") {
              if (templateSrv._values) {
                var selectedTopology = templateSrv._values.topologies;
                var selectedTopic = templateSrv._values.topic;

                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sTopology = selectedTopology;
                  target.sTopic = selectedTopic;
                  target.sPartition = options.scopedVars.partition.value;
                  target.sTopoMetric = target.metric.replace('*', target.sTopology).replace('*', target.sTopic)
                      .replace('*', target.sPartition);
                  return getStormData(target);
                }));
              }
            }

            //Templatized Dashboards for Druid
            if (templateSrv.variables[0].query === "druidDataSources" && !templateSrv.variables[1]) {
              var allDataSources = templateSrv.variables.filter(function(variable) {
                return variable.query === "druidDataSources";
              });
              var selectedDataSources = (_.isEmpty(allDataSources)) ? "" : allDataSources[0].options
                .filter(getSelectedItems)
                .map(function(dataSourceName) {
                  return dataSourceName.value;
                });

              _.forEach(selectedDataSources, function(processDataSource) {
                metricsPromises.push(_.map(options.targets, function(target) {
                  target.sDataSource = processDataSource;
                  target.sDataSourceMetric = target.metric.replace('*', target.sDataSource);
                  return getDruidData(target);
                }));
              });
            }
            // To speed up querying on templatized dashboards.
            var indexOfHosts = -1;
            for (var i = 0; i < templateSrv.variables.length; i++) {
              if (templateSrv.variables[i].name == 'hosts' && templateSrv.index.hosts) {
                indexOfHosts = i;
              }
            }
            if (indexOfHosts >= 0) {
              let allHosts = [];
              const hosts = templateSrv.index.hosts.options
                for (let host of hosts) {
                  if (host.text.toLowerCase() === "all" && host.selected) {
                    allHosts = '%';
                    break;
                  } else if (host.selected) {
                    allHosts.push(host.text);
                  }
                };

              /* The Producer & Consumer Requests graphs on the Kafka Hosts dashboard should display metrics that are
               * versioned, thus the value of different versions should be aggregated and grouped by hosts.
               * In order to have a 'grouped by hosts' like view the metric results are queried for each hosts separately.
               */
              if (!_.isEmpty(options.targets.filter(function(target) {
                    return target.metric.endsWith(".%.count"); }))) {
                _.forEach(allHosts, function(host) {
                    metricsPromises.push(_.map(options.targets, function(target) {
                        target.templatedHost = host;
                        target.templatedCluster = templatedCluster;
                        return getAllHostData(target);
                    }));
                });
              } else {
                  metricsPromises.push(_.map(options.targets, function(target) {
                      target.templatedHost = allHosts? allHosts : '';
                      target.templatedCluster = templatedCluster;
                      return getAllHostData(target);
                }));
              }
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
         * AMS Datasource List Series.
         */
        this.listSeries = function (query) {
          // wrap in regex
          if (query && query.length > 0 && query[0] !== '/') {
            query = '/' + query + '/';
          }
          return $q.when([]);
        };

        /**
         * AMS Datasource Templating Variables.
         */
        this.metricFindQuery = function (query) {
          var interpolated;
          try {
            interpolated = query.split('.')[0];
          } catch (err) {
            return $q.reject(err);
          }
          var templatedClusters = templateSrv.variables.filter(function(o) { return o.name === "cluster"});
          var templatedCluster = (_.isEmpty(templatedClusters)) ? '' : templatedClusters[0].options.filter(function(cluster)
          { return cluster.selected; }).map(function(clusterName) { return clusterName.value; });

          var tComponents = _.isEmpty(templateSrv.variables) ? '' : templateSrv.variables.filter(function(variable)
            { return variable.name === "components"});
          var tComponent = _.isEmpty(tComponents) ? '' : tComponents[0].current.value;


          // Templated Variable for HBase Users
          // It will search the cluster and populate the HBase Users.
          if(interpolated === "hbase-users") {
            return this.initMetricAppidMapping()
              .then(function () {
                var hbaseUsers = getMetrics(allMetrics, "hbase");
                var extractUsers = hbaseUsers.filter(/./.test.bind(new RegExp("regionserver.Users.", 'g')));
                var removeUser = "regionserver.Users.numUsers";
                var i = extractUsers.indexOf(removeUser);
                if(i !== -1) { extractUsers.splice(i, 1);}
                var userPrefix = "regionserver.Users.";
                var users = _.map(extractUsers, function(user) {
                  return user.substring(userPrefix.length);
                });
                users = _.map(users, function(userName) {
                  return userName.substring(0,userName.lastIndexOf("_metric"));
                });
                users = _.sortBy(_.uniq(users));
                return _.map(users, function (users) {
                  return {
                    text: users
                  };
                });
              });
          }
          // Templated Variable for HBase Tables.
          // It will search the cluster and populate the hbase-tables.
          if(interpolated === "hbase-tables") {
            return this.initMetricAppidMapping()
              .then(function () {
                var hbaseTables = getMetrics(allMetrics, "hbase");
                var extractTables = hbaseTables.filter(/./.test.bind(new RegExp("regionserver.Tables.", 'g')));
                var removeTable = "regionserver.Tables.numTables";
                var i = extractTables.indexOf(removeTable);
                if(i != -1) { extractTables.splice(i, 1);}
                var tablePrefix = "regionserver.Tables.";
                var tables = _.map(extractTables, function(user) {
                  return user.substring(tablePrefix.length);
                });
                tables = _.map(tables, function(userName) {
                  return userName.substring(0,userName.lastIndexOf("_metric"));
                });
                tables = _.sortBy(_.uniq(tables));
                return _.map(tables, function (tables) {
                  return {
                    text: tables
                  };
                });
              });
          }
          // Templated Variable for Kafka Topics.
          // It will search the cluster and populate the topics.
          if(interpolated === "kafka-topics") {
            return this.initMetricAppidMapping()
              .then(function () {
                var kafkaTopics = getMetrics(allMetrics, "kafka_broker");
                var extractTopics = kafkaTopics.filter(/./.test.bind(new RegExp("\\b.log.Log.\\b", 'g')));
                var topics =_.map(extractTopics, function (topic) {
                  var topicPrefix = "topic.";
                  return topic.substring(topic.lastIndexOf(topicPrefix)+topicPrefix.length, topic.length);
                });
                topics = _.sortBy(_.uniq(topics));
                var i = topics.indexOf("ambari_kafka_service_check");
                if(i != -1) { topics.splice(i, 1);}
                return _.map(topics, function (topics) {
                  return {
                    text: topics
                  };
                });
              });
          }

          //Templated Variables for Call Queue Metrics
          if(interpolated === "callers") {
            return this.initMetricAppidMapping()
              .then(function () {
                var nnCallers = getMetrics(allMetrics, "namenode");
                var extractCallers = nnCallers.filter(/./.test.bind(new
                  RegExp("ipc.client.org.apache.hadoop.ipc.DecayRpcScheduler.Caller", 'g')));
                var callers = _.sortBy(_.uniq(_.map(extractCallers, function(caller) {
                  return caller.substring(caller.indexOf('(')+1, caller.indexOf(')')) })));
                return _.map(callers, function (callers) {
                  return {
                    text: callers
                  };
                });
              });
          }

          var cores = [];
          //Templated Variables for Infra Solr Cores
          if (interpolated === "infra_solr_core") {
            return this.initMetricAppidMapping()
              .then(function () {
                var solrMetrics = getMetrics(allMetrics, "ambari-infra-solr");
                var extractCores = solrMetrics.filter(/./.test.bind(new
                RegExp("^infra.solr.core.", 'g')));
                _.map(extractCores, function (core) {
                  // Core naming convention is infra.solr.core.<collection_name>.<shard>.<replica>.<metric_name>
                  // coreName should be <collection_name>.<shard>.<replica>
                  core = core.split('.');
                  var coreName = core.slice(3,6).join(".");
                  if (cores.indexOf(coreName) < 0) {
                    cores.push(coreName);
                  }
                });
                return _.map(cores, function (cores) {
                        return {
                          text: cores
                        };
                      });
              });
          }

          var collections = [];
          //Templated Variables for Infra Solr Collections
          if (interpolated === "infra_solr_collection") {
            return this.initMetricAppidMapping()
              .then(function () {
                var solrMetrics = getMetrics(allMetrics, "ambari-infra-solr");
                var extractCollections = solrMetrics.filter(/./.test.bind(new
                RegExp("^infra.solr.core.", 'g')));
                _.map(extractCollections, function (core) {
                  // Core naming convention is infra.solr.core.<collection_name>.<shard>.<replica>.<metric_name>
                  core = core.split('.');
                  var collection = core[3];
                  if (collections.indexOf(collection) < 0) {
                    collections.push(collection);
                  }
                });
                return _.map(collections, function (collections) {
                        return {
                          text: collections
                        };
                      });
              });
          }

          var topologies = {};
          //Templated Variables for Storm Topologies
          if(interpolated === "topologies") {
            return this.initMetricAppidMapping()
                .then(function () {
                  var storm = getMetrics(allMetrics, "nimbus");
                  var extractTopologies = storm.filter(/./.test.bind(new
                      RegExp("^topology.", 'g')));
                  _.map(extractTopologies, function(topology){
                    // Topology naming convention is topology.<topology-name>.component.
                    topology = topology.split('.').slice(0,3);
                    if (topologies[topology[1]]){
                      topologies[topology[1]].push(topology[2]);
                    } else {
                      topologies[topology[1]] = [topology[2]];
                    }
                  });
                  return _.map(Object.keys(topologies), function(topologyNames){
                    return {
                      text: topologyNames
                    };
                  });
                });
          }
          //Templated Variables for Storm Components per Topology
          if (interpolated.indexOf("stormComponent") >= 0) {
            var componentName = interpolated.substring(0,interpolated.indexOf('.'));
            return this.initMetricAppidMapping()
                .then(function () {
                  var storm = getMetrics(allMetrics, "nimbus");
                  var extractTopologies = storm.filter(/./.test.bind(new
                      RegExp("^topology.", 'g')));
                  _.map(extractTopologies, function(topology){
                    topology = topology.split('.').slice(0,3);
                    if (topologies[topology[1]]){
                      topologies[topology[1]].push(topology[2]);
                    } else {
                      topologies[topology[1]] = [topology[2]];
                    }
                  });
                  // Retrieve unique component names from the list.
                  var compName = _.uniq(topologies[componentName]);
                  // Remove "kafka-topic" from the list of components.
                  var remove = compName.indexOf('kafka-topic');
                  if (remove > -1) { compName.splice(remove, 1);}
                  return _.map(compName, function(components){
                    return {
                      text: components
                    };
                  });
                });
          }
          var stormEntities = {};
          this.getStormEntities = function () {
            return this.initMetricAppidMapping()
                .then(function () {
                  var storm = getMetrics(allMetrics, "nimbus");
                  var extractTopologies = storm.filter(/./.test.bind(new
                      RegExp("partition", 'g')));
                  _.map(extractTopologies, function(topology){
                    topology = topology.split('.').slice(0,5);
                    var topologyName = topologyN = topology[1]; // Topology
                    var topologyTopicName = topicN = topology[3]; // Topic
                    var topologyTopicPartitionName = topology[4]; // Partition
                    if (stormEntities[topologyName]) {
                      if (stormEntities[topologyName][topologyTopicName]) {
                        stormEntities[topologyName][topologyTopicName].push(topologyTopicPartitionName);
                      } else {
                        stormEntities[topologyName][topologyTopicName] = [topologyTopicPartitionName];
                      }
                    } else {
                      stormEntities[topologyName] = {};
                      stormEntities[topologyName][topologyTopicName] = [topologyTopicPartitionName];
                    }
                  });
                });
          };
          //Templated Variables for Storm Topics per Topology
          if (interpolated.indexOf("stormTopic") >= 0) {
            var topicName = interpolated.substring(0,interpolated.indexOf('.'));
            return this.getStormEntities().then(function () {
              var topicNames = Object.keys(stormEntities[topicName]);
              return _.map(topicNames, function(names){
                return {
                  text: names
                };
              });
            });
          }
          //Templated Variables for Storm Partitions per Topic
          if (interpolated.indexOf("stormPartition") >= 0) {
            var topicN, topologyN;
            return this.getStormEntities().then(function () {
              var partitionNames = _.uniq(stormEntities[topologyN][topicN]);
              return _.map(partitionNames, function(names){
                return {
                  text: names
                };
              });
            });
          }
          // Templated Variable for YARN Queues.
          // It will search the cluster and populate the queues.
          if(interpolated === "yarnqueues") {
            return this.initMetricAppidMapping()
              .then(function () {
                var yarnqueues = getMetrics(allMetrics, "resourcemanager");
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

          // Templated Variable for DruidServices.
          // It will search the cluster and populate the druid service names.
          if(interpolated === "druidServices") {
            return this.initMetricAppidMapping()
              .then(function () {
                var druidMetrics = getMetrics(allMetrics, "druid");
                // Assumption: each node always emits jvm metrics
                var extractNodeTypes = druidMetrics.filter(/./.test.bind(new RegExp("jvm/gc/time", 'g')));
                var nodeTypes = _.map(extractNodeTypes, function(metricName) {
                  return metricName.substring(0, metricName.indexOf("."));
                });
                nodeTypes = _.sortBy(_.uniq(nodeTypes));
                return _.map(nodeTypes, function (nodeType) {
                  return {
                    text: nodeType
                  };
                });
              });
          }

          // Templated Variable for Druid datasources.
          // It will search the cluster and populate the druid datasources.
          if(interpolated === "druidDataSources") {
            return this.initMetricAppidMapping()
              .then(function () {
                var druidMetrics = getMetrics(allMetrics, "druid");
                // Assumption: query/time is emitted for each datasource
                var extractDataSources = druidMetrics.filter(/./.test.bind(new RegExp("query/time", 'g')));
                var dataSources = _.map(extractDataSources, function(metricName) {
                  return metricName.split('.')[1]
                });
                dataSources = _.sortBy(_.uniq(dataSources));
                return _.map(dataSources, function (dataSource) {
                  return {
                    text: dataSource
                  };
                });
              });
          }

          // Templated Variable for Druid query type.
          // It will search the cluster and populate the druid query types.
          if(interpolated === "druidQueryTypes") {
            return this.initMetricAppidMapping()
              .then(function () {
                var druidMetrics = getMetrics(allMetrics, "druid");
                // Assumption: query/time is emitted for each query type.
                var extractQueryTypes = druidMetrics.filter(/./.test.bind(new RegExp("query/time", 'g')));
                var queryTypes = _.map(extractQueryTypes, function(metricName) {
                  return metricName.split('.')[2]
                });
                queryTypes = _.sortBy(_.uniq(queryTypes));
                return _.map(queryTypes, function (queryType) {
                  return {
                    text: queryType
                  };
                });
              });
          }

          if (interpolated == 'hosts') {
            return this.suggestHosts(tComponent, templatedCluster);
          } else if (interpolated == 'cluster') {
            return this.suggestClusters(tComponent)
          }
        };

        /**
         * AMS Datasource  - Test Data Source Connection.
         *
         * Added Check to see if Datasource is working. Throws up an error in the
         * Datasources page if incorrect info is passed on.
         */
        this.testDatasource = function () {
          return this.doAmbariRequest({
            url: '/ws/v1/timeline/metrics/metadata',
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
        this.suggestApps = function (query) {
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
        this.suggestMetrics = function (query, app) {
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

        this.suggestClusters = function(app) {
          if (!app) { app = ''; }
          return this.doAmbariRequest({
            method: 'GET',
            url: '/ws/v1/timeline/metrics/instances?' + 'appId=' + app
          }).then(function(response) {
              var clusters = [];
              var data = response.data;
              for (var cluster in data) {
                if (data[cluster].hasOwnProperty(app)) {
                  clusters.push({text: cluster});
                }
              }
              return $q.when(clusters);
          });
        };

        /**
         * AMS Datasource - Suggest Hosts.
         *
         * Query Hosts on the cluster.
         */
        this.suggestHosts = function (app, cluster) {
          if (!app) { app = ''; }
          if (!cluster) { cluster = ''; }
          return this.doAmbariRequest({
            method: 'GET',
            url: '/ws/v1/timeline/metrics/instances?' + 'appId=' + app + '&instanceId=' + cluster
          }).then(function (response) {
            var hosts = [];
            var data = response.data;
            for (var cluster in data) {
              var appHosts = data[cluster][app];
              if (appHosts) {
                for (var index in appHosts) {
                  hosts.push({text: appHosts[index]});
                }
              }
            }
            return $q.when(hosts);
          });
        };

        /**
         * AMS Datasource Aggregators.
         */
        var aggregatorsPromise = null;
        this.getAggregators = function () {
          if (aggregatorsPromise) {
            return aggregatorsPromise;
          }
          aggregatorsPromise = $q.when([
            'none','avg', 'sum', 'min', 'max'
          ]);
          return aggregatorsPromise;
        };
      };

      return {
        AmbariMetricsDatasource: AmbariMetricsDatasource
      };

      function getSelectedItems(item, index, options) {
        // When 'All' is selected return every items except that, otherwise return what is selected.
        return index > 0 && (options[0].selected || item.selected);
      }

      function getMetrics(allmetrics, appname) {
        var metrics = allmetrics[appname];
        if(!metrics) {
            metrics = [];
        }
        return metrics;
      }
    });