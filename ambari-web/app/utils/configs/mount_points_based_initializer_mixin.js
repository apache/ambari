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

/**
 * Regexp used to determine if mount point is windows-like
 *
 * @type {RegExp}
 */
var winRegex = /^([a-z]):\\?$/;

App.MountPointsBasedInitializerMixin = Em.Mixin.create({

  /**
   * Map for methods used as value-modifiers for configProperties with values as mount point(s)
   * Used if mount point is win-like (@see winRegex)
   * Key: id
   * Value: method-name
   *
   * @type {{default: string, file: string, slashes: string}}
   */
  winReplacersMap: {
    default: '_defaultWinReplace',
    file: '_winReplaceWithFile',
    slashes: '_defaultWinReplaceWithAdditionalSlashes'
  },

  /**
   * Initializer for configs with value as one of the possible mount points
   * Only hosts that contains on the components from <code>initializer.components</code> are processed
   * Hosts with Windows needs additional processing (@see winReplacersMap)
   * Value example: '/', '/some/cool/dir'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   */
  _initAsSingleMountPoint: function (configProperty, localDB, dependencies, initializer) {
    var hostsInfo = this._updateHostInfo(localDB.hosts);
    var setOfHostNames = this._getSetOfHostNames(localDB, initializer);
    var winReplacersMap = this.get('winReplacersMap');
    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (!setOfHostNames.length) {
      return configProperty;
    }
    var allMountPoints = this._getAllMountPoints(setOfHostNames, hostsInfo);

    var mPoint = allMountPoints[0].mountpoint;
    if (mPoint === "/") {
      mPoint = Em.get(configProperty, 'recommendedValue');
    }
    else {
      var mp = mPoint.toLowerCase();
      if (winRegex.test(mp)) {
        var methodName = winReplacersMap[initializer.winReplacer];
        mPoint = this[methodName].call(this, configProperty, mp);
      }
      else {
        mPoint = mPoint + Em.get(configProperty, 'recommendedValue');
      }
    }
    Em.setProperties(configProperty, {
      value: mPoint,
      recommendedValue: mPoint
    });

    return configProperty;
  },

  /**
   * Initializer for configs with value as all of the possible mount points
   * Only hosts that contains on the components from <code>initializer.components</code> are processed
   * Hosts with Windows needs additional processing (@see winReplacersMap)
   * Value example: '/\n/some/cool/dir' (`\n` - is divider)
   *
   * @param {Object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   */
  _initAsMultipleMountPoints: function (configProperty, localDB, dependencies, initializer) {
    var hostsInfo = this._updateHostInfo(localDB.hosts);
    var self = this;
    var setOfHostNames = this._getSetOfHostNames(localDB, initializer);
    var winReplacersMap = this.get('winReplacersMap');
    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (!setOfHostNames.length) {
      return configProperty;
    }

    var allMountPoints = this._getAllMountPoints(setOfHostNames, hostsInfo);
    var mPoint = '';

    allMountPoints.forEach(function (eachDrive) {
      if (eachDrive.mountpoint === '/') {
        mPoint += Em.get(configProperty, 'recommendedValue') + "\n";
      }
      else {
        var mp = eachDrive.mountpoint.toLowerCase();
        if (winRegex.test(mp)) {
          var methodName = winReplacersMap[initializer.winReplacer];
          mPoint += self[methodName].call(this, configProperty, mp);
        }
        else {
          mPoint += eachDrive.mountpoint + Em.get(configProperty, 'recommendedValue') + "\n";
        }
      }
    }, this);

    Em.setProperties(configProperty, {
      value: mPoint,
      recommendedValue: mPoint
    });

    return configProperty;
  },

  /**
   * Replace drive-based windows-path with 'file:///'
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _winReplaceWithFile: function (configProperty, mountPoint) {
    var winDriveUrl = mountPoint.toLowerCase().replace(winRegex, 'file:///$1:');
    return winDriveUrl + Em.get(configProperty, 'recommendedValue') + '\n';
  },

  /**
   * Replace drive-based windows-path
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _defaultWinReplace: function (configProperty, mountPoint) {
    var winDrive = mountPoint.toLowerCase().replace(winRegex, '$1:');
    var winDir = Em.get(configProperty, 'recommendedValue').replace(/\//g, '\\');
    return winDrive + winDir + '\n';
  },

  /**
   * Same to <code>_defaultWinReplace</code>, but with extra-slash in the end
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _defaultWinReplaceWithAdditionalSlashes: function (configProperty, mountPoint) {
    var winDrive = mountPoint.toLowerCase().replace(winRegex, '$1:');
    var winDir = Em.get(configProperty, 'recommendedValue').replace(/\//g, '\\\\');
    return winDrive + winDir + '\n';
  },

  /**
   * Update information from localDB using <code>App.Host</code>-model
   *
   * @param {object} hostsInfo
   * @returns {object}
   * @private
   */
  _updateHostInfo: function (hostsInfo) {
    App.Host.find().forEach(function (item) {
      if (!hostsInfo[item.get('id')]) {
        hostsInfo[item.get('id')] = {
          name: item.get('id'),
          cpu: item.get('cpu'),
          memory: item.get('memory'),
          disk_info: item.get('diskInfo'),
          bootStatus: "REGISTERED",
          isInstalled: true
        };
      }
    });
    return hostsInfo;
  },

  /**
   * Determines if mount point is valid
   * Criterias:
   * <ul>
   *   <li>Should has available space</li>
   *   <li>Should not be home-dir</li>
   *   <li>Should not be docker-dir</li>
   *   <li>Should not be boot-dir</li>
   *   <li>Should not be dev-dir</li>
   * </ul>
   *
   * @param {{mountpoint: string, available: number}} mPoint
   * @returns {boolean} true - valid, false - invalid
   * @private
   */
  _filterMountPoint: function (mPoint) {
    var isAvailable = mPoint.available !== 0;
    if (!isAvailable) {
      return false;
    }

    var notHome = !['/', '/home'].contains(mPoint.mountpoint);
    var notDocker = !['/etc/resolv.conf', '/etc/hostname', '/etc/hosts'].contains(mPoint.mountpoint);
    var notBoot = mPoint.mountpoint && !(mPoint.mountpoint.startsWith('/boot') || mPoint.mountpoint.startsWith('/mnt'));
    var notDev = !(['devtmpfs', 'tmpfs', 'vboxsf', 'CDFS'].contains(mPoint.type));

    return notHome && notDocker && notBoot && notDev;
  },

  /**
   * Get list of hostNames from localDB which contains needed components
   *
   * @param {topologyLocalDB} localDB
   * @param {object} initializer
   * @returns {string[]}
   * @private
   */
  _getSetOfHostNames: function (localDB, initializer) {
    var masterComponentHostsInDB = Em.getWithDefault(localDB, 'masterComponentHosts', []);
    var slaveComponentHostsInDB = Em.getWithDefault(localDB, 'slaveComponentHosts', []);
    var hosts = masterComponentHostsInDB.filter(function (master) {
      return initializer.components.contains(master.component);
    }).mapProperty('hostName');

    var sHosts = slaveComponentHostsInDB.find(function (slave) {
      return initializer.components.contains(slave.componentName);
    });
    if (sHosts) {
      hosts = hosts.concat(sHosts.hosts.mapProperty('hostName'));
    }
    return hosts;
  },

  /**
   * Get list of all unique valid mount points for hosts
   *
   * @param {string[]} setOfHostNames
   * @param {object} hostsInfo
   * @returns {string[]}
   * @private
   */
  _getAllMountPoints: function (setOfHostNames, hostsInfo) {
    var allMountPoints = [];
    for (var i = 0; i < setOfHostNames.length; i++) {
      var hostname = setOfHostNames[i];
      var mountPointsPerHost = hostsInfo[hostname].disk_info;
      var mountPointAsRoot = mountPointsPerHost.findProperty('mountpoint', '/');

      // If Server does not send any host details information then atleast one mountpoint should be presumed as root
      // This happens in a single container Linux Docker environment.
      if (!mountPointAsRoot) {
        mountPointAsRoot = {
          mountpoint: '/'
        };
      }

      mountPointsPerHost.filter(this._filterMountPoint).forEach(function (mPoint) {
        if( !allMountPoints.findProperty("mountpoint", mPoint.mountpoint)) {
          allMountPoints.push(mPoint);
        }
      }, this);
    }

    if (!allMountPoints.length) {
      allMountPoints.push(mountPointAsRoot);
    }
    return allMountPoints;
  },

  /**
   * Settings for <code>single_mountpoint</code>-initializer
   * Used for configs with value as one of the possible mount points
   *
   * @see _initAsSingleMountPoint
   * @param {string|string[]} components
   * @param {string} winReplacer
   * @returns {{components: string[], winReplacer: string, type: string}}
   */
  getSingleMountPointConfig: function (components, winReplacer) {
    winReplacer = winReplacer || 'default';
    return {
      components: Em.makeArray(components),
      winReplacer: winReplacer,
      type: 'single_mountpoint'
    };
  },

  /**
   * Settings for <code>multiple_mountpoints</code>-initializer
   * Used for configs with value as all of the possible mount points
   *
   * @see _initAsMultipleMountPoints
   * @param {string|string[]} components
   * @param {string} winReplacer
   * @returns {{components: string[], winReplacer: string, type: string}}
   */
  getMultipleMountPointsConfig: function (components, winReplacer) {
    winReplacer = winReplacer || 'default';
    return {
      components: Em.makeArray(components),
      winReplacer: winReplacer,
      type: 'multiple_mountpoints'
    };
  }

});
