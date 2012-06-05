var registeredAssignHostsEventHandlers = false;

function getNodeInfo (allHosts, nodeName) {
  // globalYui.log("nodename: " + nodeName);
  if (nodeName == null) {
    return null;
  }
  for (host in allHosts) {
    if (allHosts[host].hostName == nodeName) {
      // globalYui.log("Get node info: " + allHosts[host].hostName);
      return allHosts[host];
    }
  }

  return null;
}

function addCommasToInt(num) {
	return num.replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
}

function renderHostsToMasterServices(allHosts, hostsToMasterServices) {
	var markup = '';
	for (var host in hostsToMasterServices) {
		  var hostInfo = getNodeInfo(allHosts, host);
		  markup += '<div class="hostToMasterServices"><h3>' + host + '<span class="hostInfo">' + addCommasToInt(hostInfo.totalMem) + 'MB RAM, ' + hostInfo.cpuCount + ' cores</span></h3><ul>';
		  for (var j in hostsToMasterServices[host]) {
			  markup += '<li>' + hostsToMasterServices[host][j] + '</li>';
		  }
		  markup += '</ul><div style="clear:both"></div></div>';
	}
	$('#hostsToMasterServices').html(markup);
}

function addMasterServiceToHost(masterName, hostName, hostsToMasterServices, masterServices) {
	if (hostsToMasterServices[hostName] == null) {
		hostsToMasterServices[hostName] = {};
	}
	hostsToMasterServices[hostName][masterName] = masterServices[masterName].displayName;
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

function getMasterHostSelect(masterName, allHosts, chosenHostName) {
	var chosenHost = getNodeInfo(allHosts, chosenHostName);
	var markup = '<select name="' + masterName + '">'; 
	markup += '<option selected="selected" value="' + chosenHost.hostName + '">' + chosenHost.hostName + ' - ' + addCommasToInt(chosenHost.totalMem) + 'MB RAM, ' + chosenHost.cpuCount + ' cores</option>';
	for (var i in allHosts) {
      var host = allHosts[i];
	  if (host.hostName != chosenHost.hostName) {
	    markup += '<option value="' + host.hostName + '">' + host.hostName + ' - ' + addCommasToInt(host.totalMem) + 'MB RAM, ' + host.cpuCount + ' cores</option>';
	  }
	}
	markup += '</select><input type="hidden" style="display:none" id="' + masterName + 'ChosenHost" value="' + chosenHost.hostName + '">';
	return markup;
}

function renderAssignHosts(clusterInfo) {

  hideLoadingImg();
  globalYui.one('#assignHostsCoreDivId').setStyle("display", "block");

  if( !registeredAssignHostsEventHandlers ) {

    globalYui.one('#selectServiceMastersSubmitButtonId').on('click', function (e) {
      e.target.set('disabled', true);

      var assignHostsRequestData = {};
      for (var masterName in masterServices) {
        assignHostsRequestData[masterName] = $('select[name=' + masterName + ']').val();
        // globalYui.log("Assignment for " + masterName + " is " + assignHostsRequestData[masterName]);
      };

      globalYui.io("../php/frontend/assignMasters.php?clusterName="+clusterInfo.clusterName, {

        method: 'POST',
        data: globalYui.JSON.stringify(assignHostsRequestData),
        timeout : 10000,
        on: {
          start: function(x, o) {
            showLoadingImg();
          },
          complete: function(x, o) {
            e.target.set('disabled', false);
            hideLoadingImg();
          },

          success: function (x,o) {
            e.target.set('disabled', false);
            globalYui.log("RAW JSON DATA: " + o.responseText);

            // Process the JSON data returned from the server
            try {
              clusterConfigJson = globalYui.JSON.parse(o.responseText);
            }
            catch (e) {
              alert("JSON Parse failed!");
              return;
            }

            //globalYui.log("PARSED DATA: " + globalYui.Lang.dump(clusterConfigJson));

            if (clusterConfigJson.result != 0) {
              // Error!
              alert("Got error!" + clusterConfigJson.error);
              return;
            }

            clusterConfigJson = clusterConfigJson.response;

            /* Done with this stage, transition to the next. */
            transitionToNextStage( "#assignHostsCoreDivId", assignHostsRequestData,
            "#configureClusterCoreDivId", clusterConfigJson, renderConfigureCluster );
          },
          failure: function (x,o) {
            e.target.set('disabled', false);
            alert("Async call failed!");
          }
        }
      });
    });

    registeredAssignHostsEventHandlers = true;
  }

  var allHosts = clusterInfo.allHosts;
  var servicesInfo = globalYui.Array( clusterInfo.services );
  var masterServices = {};

  globalYui.Array.each(servicesInfo, function(serviceInfo) {
    if( serviceInfo.enabled == true ) {
      globalYui.Array.each(serviceInfo.masters, function(masterInfo) {
        var masterHostInfo = {
          'name' : masterInfo.name,
          'displayName' : masterInfo.displayName,
          'host' : masterInfo.hostName
        };

        masterServices[masterInfo.name] = masterHostInfo;

      });
    }
  });
  
  var hostsToMasterServices = {};
  var markup = '';
  for (var i in masterServices) {
	  markup += '<div class="masterServiceSelect"><label><b>'
	  	+ masterServices[i].displayName
	    + '</b> assigned to</label>' + getMasterHostSelect(masterServices[i].name, allHosts, masterServices[i].host)
		+ '</div>';
	  if (hostsToMasterServices[masterServices[i].host] == null) {
		  hostsToMasterServices[masterServices[i].host] = {};
	  } 
	  hostsToMasterServices[masterServices[i].host][masterServices[i].name] = masterServices[i].displayName;
  }
  
  $('#masterServicesToHosts').html(markup);
  
  renderHostsToMasterServices(allHosts, hostsToMasterServices);
  
  $('select').change(function() {
	  var masterName = $(this).attr('name');
	  // masterServices[masterName] = $(this).val();
	  var prevChosenHost = $('#' + masterName + 'ChosenHost').val();
	  var newChosenHost = $(this).val();
	  removeMasterServiceFromHost(masterName, prevChosenHost, hostsToMasterServices);
	  addMasterServiceToHost(masterName, newChosenHost, hostsToMasterServices, masterServices);
	  renderHostsToMasterServices(allHosts, hostsToMasterServices);
	  $('#' + masterName + 'ChosenHost').val(newChosenHost);
  });
  
}
