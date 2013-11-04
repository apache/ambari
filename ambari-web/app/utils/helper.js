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

String.prototype.endsWith = function(suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
String.prototype.startsWith = function (prefix){
  return this.indexOf(prefix) == 0;
};

/**
 * convert ip address string to long int
 * @return {*}
 */
String.prototype.ip2long = function () {
  // *     example 1: ip2long('192.0.34.166');
  // *     returns 1: 3221234342
  // *     example 2: ip2long('0.0xABCDEF');
  // *     returns 2: 11259375
  // *     example 3: ip2long('255.255.255.256');
  // *     returns 3: false
  var i = 0;
  // PHP allows decimal, octal, and hexadecimal IP components.
  // PHP allows between 1 (e.g. 127) to 4 (e.g 127.0.0.1) components.
  var IP = this.match(/^([1-9]\d*|0[0-7]*|0x[\da-f]+)(?:\.([1-9]\d*|0[0-7]*|0x[\da-f]+))?(?:\.([1-9]\d*|0[0-7]*|0x[\da-f]+))?(?:\.([1-9]\d*|0[0-7]*|0x[\da-f]+))?$/i); // Verify IP format.
  if (!IP) {
    return false; // Invalid format.
  }
  // Reuse IP variable for component counter.
  IP[0] = 0;
  for (i = 1; i < 5; i += 1) {
    IP[0] += !!((IP[i] || '').length);
    IP[i] = parseInt(IP[i]) || 0;
  }
  // Continue to use IP for overflow values.
  // PHP does not allow any component to overflow.
  IP.push(256, 256, 256, 256);
  // Recalculate overflow of last component supplied to make up for missing components.
  IP[4 + IP[0]] *= Math.pow(256, 4 - IP[0]);
  if (IP[1] >= IP[5] || IP[2] >= IP[6] || IP[3] >= IP[7] || IP[4] >= IP[8]) {
    return false;
  }
  return IP[1] * (IP[0] === 1 || 16777216) + IP[2] * (IP[0] <= 2 || 65536) + IP[3] * (IP[0] <= 3 || 256) + IP[4] * 1;
};

String.prototype.capitalize = function () {
  return this.charAt(0).toUpperCase() + this.slice(1);
}

Em.CoreObject.reopen({
  t:function (key, attrs) {
    return Em.I18n.t(key, attrs)
  }
});

Em.Handlebars.registerHelper('log', function (variable) {
  console.log(variable);
});

Em.Handlebars.registerHelper('warn', function (variable) {
  console.warn(variable);
});

Em.Handlebars.registerHelper('highlight', function (property, words, fn) {
  var context = (fn.contexts && fn.contexts[0]) || this;
  property = Em.Handlebars.getPath(context, property, fn);

  words = words.split(";");

//  if (highlightTemplate == undefined) {
  var highlightTemplate = "<b>{0}</b>";
//  }

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    property = property.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return new Em.Handlebars.SafeString(property);
})
/**
 * Replace {i} with argument. where i is number of argument to replace with
 * @return {String}
 */
String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};

String.prototype.highlight = function (words, highlightTemplate) {
  var self = this;
  if (highlightTemplate == undefined) {
    var highlightTemplate = "<b>{0}</b>";
  }

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    self = self.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return self;
};

Number.prototype.toDaysHoursMinutes = function () {
  var formatted = {},
    dateDiff = this,
    secK = 1000, //ms
    minK = 60 * secK, // sec
    hourK = 60 * minK, // sec
    dayK = 24 * hourK;

  dateDiff = parseInt(dateDiff);
  formatted.d = Math.floor(dateDiff / dayK);
  dateDiff -= formatted.d * dayK;
  formatted.h = Math.floor(dateDiff / hourK);
  dateDiff -= formatted.h * hourK;
  formatted.m = (dateDiff / minK).toFixed(2);

  return formatted;
}

Number.prototype.countPercentageRatio = function (maxValue) {
  var usedValue = this;
  return Math.round((usedValue / maxValue) * 100) + "%";
}

Number.prototype.long2ip = function () {
  // http://kevin.vanzonneveld.net
  // +   original by: Waldo Malqui Silva
  // *     example 1: long2ip( 3221234342 );
  // *     returns 1: '192.0.34.166'
  if (!isFinite(this))
    return false;

  return [this >>> 24, this >>> 16 & 0xFF, this >>> 8 & 0xFF, this & 0xFF].join('.');
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
App = require('app');

App.formatUrl = function (urlTemplate, substitutes, testUrl) {
  var formatted = urlTemplate;
  if (urlTemplate) {
    if (!App.testMode) {
      var toSeconds = Math.round(new Date().getTime() / 1000);
      var allSubstitutes = {
        toSeconds:toSeconds,
        fromSeconds:toSeconds - 3600, // 1 hour back
        stepSeconds:15, // 15 seconds
        hostName:App.test_hostname
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
App.parseJSON = function (value) {
  if (typeof value == "string") {
    return jQuery.parseJSON(value);
  }
  return value;
};

App.format = {
  role:function (role) {
    switch (role) {
      case 'ZOOKEEPER_SERVER':
        return 'ZooKeeper Server';
      case 'ZOOKEEPER_CLIENT':
        return 'ZooKeeper Client';
      case 'NAMENODE':
        return 'NameNode';
      case 'NAMENODE_SERVICE_CHECK':
        return 'NameNode Check';
      case 'DATANODE':
        return 'DataNode';
      case 'JOURNALNODE':
        return 'JournalNode';
      case 'HDFS_SERVICE_CHECK':
        return 'HDFS Check';
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
      case 'HISTORYSERVER':
        return 'History Server';
      case 'NODEMANAGER':
        return 'NodeManager';
      case 'RESOURCEMANAGER':
        return 'ResourceManager';
      case 'TEZ_CLIENT':
        return 'Tez Client';
      case 'MAPREDUCE2_CLIENT':
        return 'MapReduce2 Client';
      case 'YARN_CLIENT':
        return 'YARN Client';
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
        return 'JobTracker Check';
      case 'MAPREDUCE_SERVICE_CHECK':
        return 'MapReduce Check';
      case 'ZOOKEEPER_SERVICE_CHECK':
        return 'ZooKeeper Check';
      case 'ZOOKEEPER_QUORUM_SERVICE_CHECK':
        return 'ZK Quorum Check';
      case  'HBASE_SERVICE_CHECK':
        return 'HBase Check';
      case 'MYSQL_SERVER':
        return 'MySQL Server';
      case 'HIVE_SERVER':
        return 'HiveServer2';
      case 'HIVE_METASTORE':
        return 'Hive Metastore';
      case 'HIVE_CLIENT':
        return 'Hive Client';
      case 'HIVE_SERVICE_CHECK':
        return 'Hive Check';
      case 'HCAT':
        return 'HCat';
      case 'HCAT_SERVICE_CHECK':
        return 'HCat Check';
      case 'OOZIE_CLIENT':
        return 'Oozie Client';
      case 'OOZIE_SERVER':
        return 'Oozie Server';
      case 'OOZIE_SERVICE_CHECK':
        return 'Oozie Check';
      case 'PIG':
        return 'Pig';
      case 'PIG_SERVICE_CHECK':
        return 'Pig Check';
      case 'MAPREDUCE2_SERVICE_CHECK':
        return 'MapReduce2 Check';
      case 'YARN_SERVICE_CHECK':
        return 'YARN Check';
      case 'SQOOP':
        return 'Sqoop';
      case 'SQOOP_SERVICE_CHECK':
        return 'Sqoop Check';
      case 'WEBHCAT_SERVER':
        return 'WebHCat Server';
      case 'WEBHCAT_SERVICE_CHECK':
        return 'WebHCat Check';
      case 'NAGIOS_SERVER':
        return 'Nagios Server';
      case 'GANGLIA_SERVER':
        return 'Ganglia Server';
      case 'GANGLIA_MONITOR':
        return 'Ganglia Monitor';
      case 'GMOND_SERVICE_CHECK':
        return 'Gmond Check';
      case 'GMETAD_SERVICE_CHECK':
        return 'Gmetad Check';
      case 'DECOMMISSION_DATANODE':
        return 'Update Exclude File';
      case 'HUE_SERVER':
        return 'Hue Server';
      case 'HCFS_CLIENT':
        return 'HCFS Client';
      case 'HCFS_SERVICE_CHECK':
        return 'HCFS Service Check';
      case 'FLUME_SERVER':
        return 'Flume Agent';
      case 'ZKFC':
        return 'ZKFailoverController';
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
  taskStatus:function (_taskStatus) {
    return _taskStatus.toLowerCase();
  }
};

/**
 * wrapper to bootstrap popover
 * fix issue when popover stuck on view routing
 * @param self
 * @param options
 */
App.popover = function(self, options) {
  self.popover(options);
  self.on("remove", function () {
    $(this).trigger('mouseleave');
  });
}