/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

function AssignMasters() {

  var managerHostName;
  var allHosts;
  var masterServices;
  var hostsToMasterServices;
  
  function getNodeInfo(nodeName) {
    // Y.log("nodename: " + nodeName);
    if (nodeName == null) {
      return null;
    }
    for (var host in allHosts) {
      if (allHosts[host].hostName == nodeName) {
        // Y.log("Get node info: " + allHosts[host].hostName);
        return allHosts[host];
      }
    }
  
    return null;
  }
  
  function addCommasToInt(num) {
  	return num.replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
  }
  
  function getTotalMemForDisplay(totalMem) {
  	return Math.round(totalMem / 102.4)/10 + "GB";
  }
  
  function renderHostsToMasterServices(hostsToMasterServices) {
  	var markup = '';
  	for (var host in hostsToMasterServices) {
  	  var hostInfo = getNodeInfo(host);
  	  markup += '<div class="hostToMasterServices"><h3>' + host + '<span class="hostInfo">' + getTotalMemForDisplay(hostInfo.totalMem) + ', ' + hostInfo.cpuCount + ' cores</span></h3><ul>';
  	  for (var j in hostsToMasterServices[host]) {
        if (j === "KERBEROS_SERVER" && Y.one("#useExistingKerberos").get('checked') === true) {
          continue;
        }
  	    markup += '<li>' + hostsToMasterServices[host][j] + '</li>';
  	  }
      markup += '</ul><div style="clear:both"></div></div>';
  	}
  	$('#hostsToMasterServices').html(markup);
  }
  
  function addMasterServiceToHost(masterName, hostName, hostsToMasterServices, masterServices) {
    // enforce constraints on what services can be co-hosted (unless those suggestions were made by the server initially)
    // we currently disallow:
    // 1. namenode and secondary namenode to be on the same host
    // 2. more than one zookeeper server to be on the same host
  
    if (hostsToMasterServices[hostName] != null) {
      for (var service in hostsToMasterServices[hostName]) {
        if (masterName == 'NAMENODE' && service == 'SNAMENODE' || masterName == 'SNAMENODE' && service == 'NAMENODE') {
          alert('NameNode and Secondary NameNode cannot be hosted on the same host.');
          return false;
        }
        if (masterName.indexOf('ZOOKEEPER') == 0 && service.indexOf('ZOOKEEPER') == 0) {
          alert('You cannot put more than one ZooKeeper Server on the same host.');
          return false;
        }
      }
    }
  	if (hostsToMasterServices[hostName] == null) {
  		hostsToMasterServices[hostName] = {};
  	}
  	hostsToMasterServices[hostName][masterName] = masterServices[masterName].displayName;
  	return true;
  }
  
  function removeMasterServiceFromHost(masterName, hostName, hostsToMasterServices) {
  	for (var i in hostsToMasterServices[hostName]) {
  		if (i == masterName) {
  			delete hostsToMasterServices[hostName][i];
  			//alert(Object.keys(hostsToMasterServices[hostName]).length);
  			if (Object.keys(hostsToMasterServices[hostName]).length == 0) {
  				//alert('remove');
  				delete hostsToMasterServices[hostName];
  			}
  			return;
  		}
  	}
  }
  
  function getHostInfoForDisplay(host) {
    return host.hostName + ' - ' + getTotalMemForDisplay(host.totalMem) + ', ' + host.cpuCount + ' cores';
  }
  
  function getMasterHostSelect(masterName, chosenHostName) {
  	var chosenHost = getNodeInfo(chosenHostName);
  	var markup = '<select name="' + masterName + '">'; 
  	markup += '<option selected="selected" value="' + chosenHost.hostName + '">' + getHostInfoForDisplay(chosenHost) + '</option>';
  	for (var i in allHosts) {
        var host = allHosts[i];
  	  if (host.hostName != chosenHost.hostName) {
  	    markup += '<option value="' + host.hostName + '">' + host.hostName + ' - ' + getTotalMemForDisplay(host.totalMem) + ', ' + host.cpuCount + ' cores</option>';
  	  }
  	}
  	markup += '</select><div style="clear:both"></div><input type="hidden" style="display:none" id="' + masterName + 'ChosenHost" value="' + chosenHost.hostName + '">';
  	return markup;
  }

  function attachEventHandlers() {
    var submitButton = Y.one('#selectServiceMastersSubmitButtonId');
    submitButton.detach();
    submitButton.on('click', function (e) {
      Y.log('clicked select service submit');
      e.target.set('disabled', true);
      var assignHostsRequestData = {};
      for (var masterName in masterServices) {
        var hostName = $('select[name=' + masterName + ']').val();
        if (masterName.indexOf("ZOOKEEPER_SERVER") == 0) {
          if (assignHostsRequestData['ZOOKEEPER_SERVER'] == null) {
            assignHostsRequestData['ZOOKEEPER_SERVER'] = [];
          }
          assignHostsRequestData['ZOOKEEPER_SERVER'].push(hostName);
        } else {
          if (assignHostsRequestData[masterName] == null) {
            assignHostsRequestData[masterName] = [];
          }
          if (masterName === 'KERBEROS_SERVER') {

            var checked = Y.one("#useExistingKerberos").get('checked');
            if (checked) {
              App.props.securityType = "USER_SET_KERBEROS";
              hostName = Y.one("#existingKerberosHostname").get('value');
            } else {
              App.props.securityType = "AMBARI_SET_KERBEROS";
            }
          }
          assignHostsRequestData[masterName].push(hostName);
        }
        // Y.log("Assignment for " + masterName + " is " + assignHostsRequestData[masterName]);
      };

      Y.io("../php/frontend/assignMasters.php?clusterName=" + App.props.clusterName, {
        method: 'POST',
        data: Y.JSON.stringify(assignHostsRequestData),
        timeout : App.io.DEFAULT_AJAX_TIMEOUT_MS,
        on: {
          start: function(x, o) {
            App.ui.showLoadingOverlay();
          },
          complete: function(x, o) {
            e.target.set('disabled', false);
            App.ui.hideLoadingOverlay();
          },

          success: function (x,o) {
            var clusterConfigJson;
            e.target.set('disabled', false);
            Y.log("RAW JSON DATA: " + o.responseText);

            // Process the JSON data returned from the server
            try {
              clusterConfigJson = Y.JSON.parse(o.responseText);
            }
            catch (e) {
              alert("JSON Parse failed!");
              return;
            }

            //Y.log("PARSED DATA: " + Y.Lang.dump(clusterConfigJson));

            if (clusterConfigJson.result != 0) {
              // Error!
              alert("Got error!" + clusterConfigJson.error);
              return;
            }

            clusterConfigJson = clusterConfigJson.response;

            /* Done with this stage, transition to the next. */
            App.transition.transitionToNextStage( "#assignHostsCoreDivId", assignHostsRequestData,
              "#configureClusterCoreDivId", clusterConfigJson, renderConfigureCluster );
          },
          failure: function (x,o) {
            e.target.set('disabled', false);
            alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
          }
        }
      });
    });

    // prevValue is used to undo user selection in case we prevent the user from assigning a service
    var prevValue = '';

    $('#masterServicesToHosts select').off('click');
    $('#masterServicesToHosts select').click(function() {
      prevValue = $(this).val();
    }).change(function(event) {
        var masterName = $(this).attr('name');
        // masterServices[masterName] = $(this).val();
        var prevChosenHost = $('#' + masterName + 'ChosenHost').val();
        var newChosenHost = $(this).val();
        if (addMasterServiceToHost(masterName, newChosenHost, hostsToMasterServices, masterServices)) {
          removeMasterServiceFromHost(masterName, prevChosenHost, hostsToMasterServices);
          renderHostsToMasterServices(hostsToMasterServices);
          $('#' + masterName + 'ChosenHost').val(newChosenHost);
        } else {
          $(this).val(prevValue);
        }
      });

  }

  this.render = function (clusterInfo) {
  
    App.ui.hideLoadingOverlay();
    Y.log("Render assign hosts data " + Y.Lang.dump(clusterInfo));
    Y.one('#assignHostsCoreDivId').setStyle("display", "block");
    App.props.clusterName = clusterInfo.clusterName;

    allHosts = clusterInfo.allHosts;
    managerHostName = clusterInfo.managerHostName;
    
    var servicesInfo = Y.Array( clusterInfo.services );
    masterServices = {};
  
    Y.Array.each(servicesInfo, function(serviceInfo) {
      if( serviceInfo.enabled == true ) {
        var zkIndex = 1;
        Y.Array.each(serviceInfo.masters, function(masterInfo) {
                  
          for (var i in masterInfo.hostNames) {
            var masterHostInfo = {
                'name' : masterInfo.name,
                'displayName' : masterInfo.displayName,
                'host' : masterInfo.hostNames[i]
            };
            // there could be multiple zookeepers
            if (masterInfo.name == 'ZOOKEEPER_SERVER') {
              masterHostInfo.name = 'ZOOKEEPER_SERVER_' + zkIndex;
              masterHostInfo.displayName = masterHostInfo.displayName + ' ' + zkIndex;
              zkIndex++;
            }
            masterServices[masterHostInfo.name] = masterHostInfo;
          }
        });
      }
    });
    
    hostsToMasterServices = {};
    var markup = '';
    for (var i in masterServices) {
  	  markup += '<div class="masterServiceSelect" id=' + masterServices[i].name + 'Id' + '><label><b>'
  	  	+ masterServices[i].displayName
  	    + '</b></label>' + getMasterHostSelect(masterServices[i].name, masterServices[i].host)
  		  + '</div>';
  	  if (hostsToMasterServices[masterServices[i].host] == null) {
  		  hostsToMasterServices[masterServices[i].host] = {};
  	  } 
  	  hostsToMasterServices[masterServices[i].host][masterServices[i].name] = masterServices[i].displayName;
    }
    // add manager server
    if (hostsToMasterServices[managerHostName] == null) {
      hostsToMasterServices[managerHostName] = {};
    }
    hostsToMasterServices[managerHostName].MANAGER_SERVER = App.props.managerServiceName + ' Server';

    Y.one('#masterServicesToHosts').setContent(markup);

    // BEGIN KERBEROS HANDLING
    // At the time of load of the page if Kerberos selection div is already
    // present then remove it
    var kerbPresent = Y.one("#existingKerberosServerId");
    if (kerbPresent) {
      Y.one("#masterServicesToHostsContainer").removeChild(kerbPresent);
    }

    kerbPresent = Y.one("#KERBEROS_SERVERId");
    if (kerbPresent) {
      var markup = '<div name="existingKerberosServer" id="existingKerberosServerId" style="clear:both;float:none;margin-bottom:12px">'
        + '<label class="checkbox" for="useExistingKerberos" style="padding-left:70px; display:block;">'
        + '<em>'
        + 'Use self-configured Kerberos Server'
        + '</em>'
        + '<input type="checkbox" name="useExistingKerberos" id="useExistingKerberos" style="position:relative;">'
        + '</label>'
        + '</div>';
      Y.one("#masterServicesToHostsContainer").prepend(markup);

      markup = '<input class type="text" name="existingKerberosHostname" id="existingKerberosHostname" placeholder="Enter host name" value="" style="display:none;">';
      kerbPresent.one('label').insert(markup, 'after');

      Y.one('#useExistingKerberos').on('click', function(e) {
        var selectKerbNode = Y.one('#KERBEROS_SERVERId select');
        var existingKerberosHostname =  Y.one('#existingKerberosHostname');

        if (this.get('checked')) {
          selectKerbNode.hide();
          existingKerberosHostname.show();
        } else {
          selectKerbNode.show();
          existingKerberosHostname.hide();
        }
        renderHostsToMasterServices(hostsToMasterServices);
      });
    }
    // END KERBEROS HANDLING

    renderHostsToMasterServices(hostsToMasterServices);

    attachEventHandlers();
  };  // end render

};
