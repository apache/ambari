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

String.prototype.trim = function () {
  return this.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
};

String.prototype.capitalize = function () {
  return this.charAt(0).toUpperCase() + this.slice(1);
}

Em.CoreObject.reopen({
  t: function (key, attrs) {
    return Em.I18n.t(key, attrs)
  }
});

Handlebars.registerHelper('log', function (variable) {
  console.log(variable);
});

Handlebars.registerHelper('warn', function (variable) {
  console.warn(variable);
});

String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};

/**
 * Convert byte size to other metrics.
 * @param {Number} precision  Number to adjust precision of return value. Default is 0.
 * @param {String} parseType  JS method name for parse string to number. Default is "parseInt".
 * @remarks The parseType argument can be "parseInt" or "parseFloat".
 * @return {String) Returns converted value with abbreviation.
 */
Number.prototype.bytesToSize = function (precision, parseType/* = 'parseInt' */) {
  if (arguments[1] === undefined) {
    parseType = 'parseInt';
  }

  var value = this;
  var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  var posttxt = 0;
  if (this == 0) return 'n/a';
  while (value >= 1024) {
    posttxt++;
    value = value / 1024;
  }
  var parsedValue = window[parseType](value);

  return parsedValue.toFixed(precision) + " " + sizes[posttxt];
}

Number.prototype.toDaysHoursMinutes = function () {
  var formatted = {},
    dateDiff = this,
    minK = 60, // sec
    hourK = 60 * minK, // sec
    dayK = 24 * hourK;

  dateDiff = parseInt(dateDiff / 1000);
  formatted.d = Math.floor(dateDiff / dayK);
  dateDiff -= formatted.d * dayK;
  formatted.h = Math.floor(dateDiff / hourK);
  dateDiff -= formatted.h * hourK;
  formatted.m = Math.floor(dateDiff / minK);
  dateDiff -= formatted.m * minK;

  return formatted;
}

Number.prototype.countPercentageRatio = function (maxValue) {
  var usedValue = this;
  return Math.round((usedValue / maxValue) * 100) + "%";
}

/**
 * Formats the given URL template by replacing keys in 'substitutes'
 * with their values. If not in App.testMode, the testUrl is used.
 *
 * The substitution points in urlTemplate should be of format "...{key}..."
 * For example "http://apache.org/{projectName}".
 * The substitutes can then be{projectName: "Ambari"}.
 *
 * Keys which will be automatically taken care of are:
 * {
 *  hostName: App.test_hostname,
 *  fromSeconds: ..., // 1 hour back from now
 *  toSeconds: ..., // now
 *  stepSeconds: ..., // 15 seconds by default
 * }
 *
 * @param {String} urlTemplate  URL template on which substitutions are to be made
 * @param substitutes Object containing keys to be replaced with respective values
 * @param {String} testUrl  URL to be used if app is not in test mode (!App.testMode)
 * @return {String} Formatted URL
 */
App.formatUrl = function (urlTemplate, substitutes, testUrl) {
  var formatted = urlTemplate;
  if (urlTemplate) {
    if (!App.testMode) {
      var toSeconds = Math.round(new Date().getTime() / 1000);
      var allSubstitutes = {
        toSeconds: toSeconds,
        fromSeconds: toSeconds - 3600, // 1 hour back
        stepSeconds: 15, // 15 seconds
        hostName: App.test_hostname
      };
      jQuery.extend(allSubstitutes, substitutes);
      for (key in allSubstitutes) {
        var useKey = '{' + key + '}';
        formatted = formatted.replace(new RegExp(useKey, 'g'), allSubstitutes[key]);
      }
    } else {
      formatted = testUrl;
    }
  }
  return formatted;
}

/**
 * Certain variables can have JSON in string
 * format, or in JSON format itself.
 */
App.parseJSON = function(value){
  if(typeof value == "string"){
    return jQuery.parseJSON(value);
  }
  return value;
};

App.format = {
  role: function (role) {
    switch (role) {
      case 'ZOOKEEPER_SERVER':
        return 'ZooKeeper Server';
      case 'ZOOKEEPER_CLIENT':
        return 'ZooKeeper Client';
      case 'NAMENODE':
        return 'NameNode';
      case 'NAMENODE_SERVICE_CHECK':
        return 'NameNode Service Check';
      case 'DATANODE':
        return 'DataNode';
      case 'HDFS_SERVICE_CHECK':
        return 'HDFS Service Check';
      case 'SECONDARY_NAMENODE':
        return 'SNameNode';
      case 'HDFS_CLIENT':
        return 'HDFS Client';
      case 'HBASE_MASTER':
        return 'HBase Master';
      case 'HBASE_REGIONSERVER':
        return 'HBase RegionServer';
      case 'HBASE_CLIENT':
        return 'HBase Client';
      case 'JOBTRACKER':
        return 'JobTracker';
      case 'TASKTRACKER':
        return 'TaskTracker';
      case 'MAPREDUCE_CLIENT':
        return 'MapReduce Client';
      case 'JAVA_JCE':
        return 'Java JCE';
      case 'KERBEROS_SERVER':
        return 'Kerberos Server';
      case 'KERBEROS_CLIENT':
        return 'Kerberos Client';
      case 'KERBEROS_ADMIN_CLIENT':
        return 'Kerberos Admin Client';
      case 'HADOOP_CLIENT':
        return 'Hadoop Client';
      case 'JOBTRACKER_SERVICE_CHECK':
        return 'JobTracker Service Check';
      case 'MAPREDUCE_SERVICE_CHECK':
        return 'MapReduce Service Check';
      case 'ZOOKEEPER_SERVICE_CHECK':
        return 'ZooKeeper Service Check';
      case 'ZOOKEEPER_QUORUM_SERVICE_CHECK':
        return 'ZooKeeper Quorum Service Check';
      case  'HBASE_SERVICE_CHECK':
        return 'HBase Service Check';
      case 'MYSQL_SERVER':
        return 'MySQL Server';
      case 'HIVE_SERVER':
        return 'Hive Server';
      case 'HIVE_CLIENT':
        return 'Hive Client';
      case 'HIVE_SERVICE_CHECK':
        return 'Hive Service Check';
      case 'HCAT':
        return 'HCat';
      case 'HCAT_SERVICE_CHECK':
        return 'HCat Service Check';
      case 'OOZIE_CLIENT':
        return 'Oozie Client';
      case 'OOZIE_SERVER':
        return 'Oozie Server';
      case 'OOZIE_SERVICE_CHECK':
        return 'Oozie Service Check';
      case 'PIG':
        return 'Pig';
      case 'PIG_SERVICE_CHECK':
        return 'Pig Service Check';
      case 'SQOOP':
        return 'Sqoop';
      case 'SQOOP_SERVICE_CHECK':
        return 'Sqoop Service Check';
      case 'TEMPLETON_CLIENT':
        return 'Templeton Client';
      case 'TEMPLETON_SERVER':
        return 'Templeton Server';
      case 'TEMPLETON_SERVICE_CHECK':
        return 'Templeton Service Check';
      case 'NAGIOS_SERVER':
        return 'Nagios Server';
      case 'GANGLIA_SERVER':
        return 'Ganglia Server';
      case 'GANGLIA_MONITOR':
        return 'Ganglia Monitor';
      case 'GMOND_SERVICE_CHECK':
        return 'Gmond Service Check'
      case 'GMETAD_SERVICE_CHECK':
        return 'Gmetad Service Check';
      case 'DECOMMISSION_DATANODE':
        return 'Decommission DataNode';
    }
  },

  /**
   * PENDING - Not queued yet for a host
   * QUEUED - Queued for a host
   * IN_PROGRESS - Host reported it is working
   * COMPLETED - Host reported success
   * FAILED - Failed
   * TIMEDOUT - Host did not respond in time
   * ABORTED - Operation was abandoned
   */
  taskStatus: function (_taskStatus) {
    return _taskStatus.replace('_', ' ').toLowerCase();
  }
};