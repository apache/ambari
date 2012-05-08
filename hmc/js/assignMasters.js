var registeredAssignHostsEventHandlers = false;

var nodeAssignments;
// TODO: Limit the number of groups
var staticNodeGroups;
var nodeGroups;
var possibleGroups;
var numSlotsPerGroup;

function getAssociativeArrayLength( associateArray ) {
  var element_count = 0;
  for(var e in associateArray) {
    if(associateArray.hasOwnProperty(e)) {
      element_count++; 
    }
  }
  return element_count;
}

var changeNodeLocked = false; // only once change node at a time
function changeNode(nodeGroupName, slotNumber) {
  if (changeNodeLocked) {
    setFormStatus("You are already in the process of changing node, let's finish that first!", true);
    return;
  }
  changeNodeLocked = true;
  var slotId = nodeGroupName + slotNumber;
  var hostListMarkup = "";
  var moreHostsLeft = false;
  for (hostIndex in possibleGroups[nodeGroupName].hosts) {
    var hostName = possibleGroups[nodeGroupName].hosts[hostIndex];
    if (!isNodeAssigned(nodeGroupName, hostName)) { 
      moreHostsLeft = true;
      hostListMarkup += '<a href=\'javascript:setNodeAssignment("' + slotId + '", "'  + hostName + '")\'>' + hostName + '</a><br/>';
    }
  }
  if (!moreHostsLeft) {
   setFormStatus("All hosts are already assigned in Node-Group " + nodeGroupName, true);
   changeNodeLocked = false;
  } else {
    hoverOnANode(slotId);
    globalYui.Node.create('<div id="nodeListDynamicId"></div>').appendTo(globalYui.one("#assignHostsCoreDivId"));
    globalYui.one("#nodeGroupsCoreDivId").setStyle("width", "80%");;
    globalYui.one("#nodeListDynamicId").setContent(hostListMarkup);
    var heightOfNodeTable = globalYui.one("#nodeGroupsCoreDivId").getComputedStyle("height");
    globalYui.one("#nodeListDynamicId").setStyle("margin-top", "-" + heightOfNodeTable );
    globalYui.one("#nodeListDynamicId").setStyle("height", heightOfNodeTable);
  }
}

function hoverOnANode(slotId) {
  globalYui.one("#nodeAssignmentWrapper" + slotId + "Id").replaceClass("nodeAssignmentWrapper", "nodeAssignmentWrapperHover");
  globalYui.one("#slotForGroup" + slotId).replaceClass("slot", "slotHover");
}

function hoverSlotForGroup(slotId) {
  globalYui.log("hovered on " + "#SlotForGroup" + slotId + "Id");
  hoverOnANode(slotId);
}

function hoverNodeAssignment(slotId) {
  globalYui.log("hovered on " + "#nodeAssignment" + slotId + "Id");
  hoverOnANode(slotId);
}

function mouseOutNode(slotId) {
  globalYui.one("#nodeAssignmentWrapper" + slotId + "Id").replaceClass("nodeAssignmentWrapperHover", "nodeAssignmentWrapper");
  globalYui.one("#slotForGroup" + slotId).replaceClass("slotHover", "slot");
}

function mouseOutSlotForGroup(slotId) {
  globalYui.log("mouseOut on " + "#slotForGroup" + slotId + "Id");
  mouseOutNode(slotId);
}

function mouseOutNodeAssignment(slotId) {
  globalYui.log("mouseOut on " + "#nodeAssignment" + slotId + "Id");
  mouseOutNode(slotId);
}

function isNodeAssigned(nodeGroupName, hostName) {
  for (slotId in nodeAssignments) {
    if (nodeAssignments[slotId] == hostName) {
      globalYui.log(hostName + " is already assigned");
      return true;
    }
  }
  globalYui.log(hostName + " is not assigned");
  return false;
}

function setNodeAssignment(slotId, hostName) {
  nodeAssignments[slotId] = hostName;
  globalYui.one("#nodeAssignment" + slotId + 'Id').setContent(nodeAssignments[slotId]);
  globalYui.one("#nodeListDynamicId").setContent("");
  globalYui.one("#nodeListDynamicId").setStyle("display", "none");
  globalYui.one("#nodeListDynamicId").remove();
  setFormStatus("Changed Node # " + slotId + " to " + hostName, false);
  globalYui.one("#nodeGroupsCoreDivId").setStyle("width", "100%");;
  mouseOutNode(slotId);
  // Release the lock
  changeNodeLocked = false;
}

function renderNodeAssignments() {
  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      var nodeAssignmentDivId = "#nodeAssignment" + nodeGroupName + i + 'Id';
      var nodeAssignmentDiv = globalYui.one(nodeAssignmentDivId);
      nodeAssignmentDiv.setContent(nodeAssignments[nodeGroupName + i]);
    }
  }
}

function getNodeInfo (allHosts, nodeName) {

  globalYui.log("nodename: " + nodeName);
  if (nodeName == null) {
    return null;
  }
  for (host in allHosts) {
    if (allHosts[host].hostName == nodeName) {
      globalYui.log("Get node info: " + allHosts[host].hostName);
      return allHosts[host];
    }
  }

  return null;
}

function findNodeGroup (nodeName, nodeGroups, possibleGroups) {
  var position = 0;
  var retval = {};

  for (nodeGroup in nodeGroups) {
    globalYui.log("Node group: " + globalYui.Lang.dump(nodeGroups));
    nodeGroupId = nodeGroups[nodeGroup];
    
    globalYui.log("Hosts for group: " + globalYui.Lang.dump(possibleGroups[nodeGroupId].hosts));
    for (host in possibleGroups[nodeGroupId].hosts) {
      globalYui.log("Node: " + possibleGroups[nodeGroupId].hosts[host]);
      if (possibleGroups[nodeGroupId].hosts[host] == nodeName) {
        retval = {
          'nodeGroupId'   : nodeGroupId,
          'nodePosition'  : position
        };
        return retval;
      } else {
        position++;
      }
    }
  }
}

function getNodeGroupId (nodeName, nodeGroups, possibleGroups) {

  var nodeGroupIndex;
  var nodeId = 0;
  if (nodeName == null) {
    nodeGroupIndex = nodeGroups[0];
    globalYui.log("Default group index: " + nodeGroupIndex);
  } else {
    globalYui.log("Node name: " + globalYui.Lang.dump(nodeName));
    globalYui.log("node grps: " + globalYui.Lang.dump(nodeGroups));
    globalYui.log("possible grps: " + globalYui.Lang.dump(possibleGroups));
    nodeGroupInfo = findNodeGroup(nodeName, nodeGroups, possibleGroups);
    nodeGroupIndex = nodeGroupInfo.nodeGroupId;
    nodeId = nodeGroupInfo.nodePosition;
  }
  var id = 'slotForGroup' + nodeGroupIndex + nodeId;

  globalYui.log("Node group index: " + nodeGroupIndex);

  return id;
}

function hasHost (list, hostName) {
  var host
  for (host in list) {
    if (list[host].hostName == hostName) {
      globalYui.log("Host name found: " + hostName);
      return true;
    }
  }

  globalYui.log("Host name not found: " + hostName);
  return false;
}

// excludes the elements of the prunelist from allHosts
function pruneHostList (allHosts, pruneList) {
  var retval = {};

  var node;
  for (node in allHosts) {
    if (hasHost(pruneList, allHosts[node].hostName) == false) {
      retval[allHosts[node].hostName] = allHosts[node];
    }
  }

  return retval;
}

// returns a tuple of possible groups and node groups
function constructNodeGroups (hostList, currPossibleGroups, currNodeGroups) {
  globalYui.log("current possible: " + globalYui.Lang.dump(currPossibleGroups));
  globalYui.log("current nodes: " + globalYui.Lang.dump(currNodeGroups));
  globalYui.log("hostlist: " + globalYui.Lang.dump(hostList));
  possibleGroups = currPossibleGroups;
  nodeGroups = currNodeGroups;
  // Construct node-groups now
  for (hostInfo in hostList) {
    host = hostList[hostInfo];
//  globalYui.Array.each( hostList, function(host) {
    var totalMem = host.totalMem;
    var numCpus = host.cpuCount;
    var found = false;
    globalYui.log("Looking at host " + host.hostName);
    for (groupIndex in possibleGroups) {
      var thisGroup = possibleGroups[groupIndex];
      if (thisGroup.totalMem == totalMem && thisGroup.numCpus == numCpus) {
        found = true;
        globalYui.log("Using an existing nodeGroup " + groupIndex );
        thisGroup.hosts.push(host.hostName);
        break;
      }
    }

    if (!found) {
      // Not found create a new group
      var newGroup = { "totalMem": totalMem, "numCpus": numCpus, "hosts" : [ host.hostName ] };
      var newGroupName = staticNodeGroups[getAssociativeArrayLength(possibleGroups)];
      possibleGroups[newGroupName] = newGroup;
      nodeGroups.push(newGroupName);
      globalYui.log("Added a new group " + newGroupName);
    }
  }
//  });

  retval = {
    'possibleGroups' : possibleGroups,
    'nodeGroups' : nodeGroups
  };

  return retval;
}

function renderAssignHosts (clusterInfo) {

  hideLoadingImg();
  globalYui.one('#assignHostsCoreDivId').setStyle("display", "block");

  // Mapping from slotIDs to hostNames
  nodeAssignments = {};
  // TODO: Limit the number of groups
  staticNodeGroups = [ "A", "B", "C", "D"];
  // Array of names of groups
  nodeGroups = [];
  // Mapping of group-name to hosts
  possibleGroups = { };
  // Mapping of group-name to number of slots in that group
  numSlotsPerGroup = {};
  // Mapping of slot-id to Array of masters
  var slotContents = {};
  // Mapping of master-Name to slotId
  var serviceLocations = {};

  if( !registeredAssignHostsEventHandlers ) {

    globalYui.one('#selectServiceMastersSubmitButtonId').on('click', function (e) {
      e.target.set('disabled', true);

      var assignHostsRequestData = {};
      for (masterName in serviceLocations) {
        var pollutedSlotId = serviceLocations[masterName];
        var slotId = pollutedSlotId.replace(/slotForGroup/, "");
        assignHostsRequestData[ masterName ] = nodeAssignments[slotId];
        globalYui.log("Assignment for " + masterName + " is " + nodeAssignments[slotId]);
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
  var serviceMasters = [ ];
  var serviceMastersDisplayNames = {};
  var suggestNodes = {};
  globalYui.Array.each(servicesInfo, function(serviceInfo) {
    if( serviceInfo.enabled == true ) {
      globalYui.Array.each(serviceInfo.masters, function(masterInfo) {
        var masterHostInfo = {
          'name' : masterInfo.name,
          'host' : masterInfo.hostName
        };

        serviceMasters.push(masterHostInfo);
        serviceMastersDisplayNames[masterInfo.name] = masterInfo.displayName;

        if ((retval = getNodeInfo(allHosts, masterInfo.hostName)) != null) {
          suggestNodes[masterInfo.hostName] = retval;
        }
      });
    }
  });

  // construct possible and node groups with the suggested nodes first
  // before calling the same with allHosts (pruned) 
  globalYui.log("Suggest nodes: " + globalYui.Lang.dump(suggestNodes));
  retval = constructNodeGroups(suggestNodes, possibleGroups, nodeGroups);

  possibleGroups = retval.possibleGroups;
  nodeGroups = retval.nodeGroups;

  retval = pruneHostList(allHosts, suggestNodes);

  globalYui.log("Pruned list: " + globalYui.Lang.dump(retval));
  retval = constructNodeGroups(retval, possibleGroups, nodeGroups);
  possibleGroups = retval.possibleGroups;
  nodeGroups = retval.nodeGroups;

  globalYui.log("Possible groups: " + globalYui.Lang.dump(possibleGroups));
  globalYui.log("Groups: " + globalYui.Lang.dump(nodeGroups));

  /*
  // For testing
  possibleGroups["C"] = {"totalMem": 1234, "numCpus":5, "hosts" : [ "dummy1.hortonworks.com", "dummy2.hortonworks.com", "dummy3.hortonworks.com", "dummy4.hortonworks.com", "dummy5.hortonworks.com", "dummy6.hortonworks.com", "dummy7.hortonworks.com", "dummy8.hortonworks.com" ] };
  nodeGroups.push("C");

  possibleGroups["D"] = {"totalMem": 12345, "numCpus":6, "hosts" : [ "dumm101.hortonworks.com", "dummy102.hortonworks.com", "dummy103.hortonworks.com", "dummy104.hortonworks.com" ] };
  nodeGroups.push("D");
  */

  /////////// Figure out the numSlots per group
  var numServices = serviceMasters.length;
  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    var numNodes = possibleGroups[nodeGroupName].hosts.length;
    numSlotsPerGroup[nodeGroupName] = numServices < numNodes ? numServices : numNodes ;
  }

  ////////////////// Render node groups ////////
  var nodeGroupsMarkup = "";
  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    nodeGroupsMarkup += '<div id="nodeGroupsTable">';
    globalYui.log("Rendering nodeGroup for " + nodeGroupName);
    var nodeGroupConfig = possibleGroups[nodeGroupName];
    nodeGroupsMarkup += '<div class="nodeGroupTitle">Node group ' + nodeGroupName + ' has ' + nodeGroupConfig.hosts.length + ' nodes each with ' + nodeGroupConfig.totalMem + 'MB memory and ' + nodeGroupConfig.numCpus + ' cores.</div>';
    nodeGroupsMarkup += '<div id="' + nodeGroupName + '" class="aNodeGroup">';

    // Number of boxes equal to number of service-masters
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      nodeGroupsMarkup +=       '<div class="slotBucket">' + 
        '<div class="slotTitle"> Node # ' + ( i + 1) + '</div>' +
        '<div id="slotForGroup' + nodeGroupName + i + '" class="slot" onmouseover=\'javascript:hoverSlotForGroup("' + nodeGroupName + i + '");\' onmouseout=\'javascript:mouseOutSlotForGroup("' + nodeGroupName + i + '");\'></div>' +
        '</div>';
  } 
  nodeGroupsMarkup +=     '</div>'; 

  ////////////////// Create node list per group ////////////////
  //var nodeListContent = '<div class="nodeList">Nodes assignment for group ' + nodeGroupName + ':<br/>';
  var nodeListContent = '<div class="nodeList">';
  //var nodeListContent = '';
  for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
    var slotId = nodeGroupName + i;
    nodeListContent += '<div class="nodeAssignmentWrapper" id="nodeAssignmentWrapper' + slotId + 'Id">Node # ' + ( i + 1) + ': ' +
                         '<div class="nodeAssignment" id="nodeAssignment' + slotId + 'Id" onmouseover=\'javascript:hoverNodeAssignment("' + slotId + '");\' onmouseout=\'javascript:mouseOutNodeAssignment("' + slotId + '");\'></div>' +
                         '&nbsp;' +
                         '<a href=\'javascript:changeNode("' + nodeGroupName + '", "' + i + '");\' id="changeLink' + slotId + '">change</a>' +
                         '<br/>' +
                       '</div>';
  }
  nodeListContent += "</div>";
  nodeGroupsMarkup += nodeListContent;
  ////////////////// End of creating node list per group //////

  nodeGroupsMarkup += '</div>';
  }

  globalYui.one("#nodeGroupsCoreDivId").setContent(nodeGroupsMarkup);
  ////////////////// End of rendering node groups ////////

  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      var thisId = 'slotForGroup' + nodeGroupName + i;
      if (slotContents[thisId] === undefined) {
        slotContents[thisId] = {};
      }
    }
  }

  ////////////////// Initialize all masters in some group for now ///////////////////
  var todoId = 'slotForGroup' + nodeGroups[0] + 0;
  for (serviceMasterIndex in serviceMasters) {
    var todoId = getNodeGroupId(serviceMasters[serviceMasterIndex].host, 
                                nodeGroups, possibleGroups);
    var masterName = serviceMasters[serviceMasterIndex].name;
    slotContents[todoId][masterName] = masterName;
    serviceLocations[masterName] = todoId;
  }

  // Now render
  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      var thisId = 'slotForGroup' + nodeGroupName + i;
      var content= "";
      for (master in slotContents[thisId]) {
        content += '<div id="' + master + '" class="masterDiv">' + serviceMastersDisplayNames[master] + "</div>";
      }
      globalYui.one('#' + thisId).setContent(content);
    }
  }
  ////////////////// End of initializing all slots ///////////////////

  ///////////////// Initialize node assignments //////////////////////
  // Start with first set of hosts in all groups
  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      globalYui.log("setting nodeAssignments for " + nodeGroupName + i + " to " + possibleGroups[nodeGroupName].hosts[i]);
      nodeAssignments[nodeGroupName + i] = possibleGroups[nodeGroupName].hosts[i];
    }
  }
  // Now render
  renderNodeAssignments();
  ///////////////// End of initing node assignments //////////////////////

  for (serviceMasterIndex in serviceMasters) {
    var dd = new globalYui.DD.Drag({
      node: globalYui.one('#' + serviceMasters[serviceMasterIndex].name)
  }).plug(globalYui.Plugin.DDProxy, {
    //We don't want the node to move on end drag
    moveOnEnd: false
  }).plug(globalYui.Plugin.DDConstrained, {
    //Keep it inside the work area
    constrain2node: '#nodeGroupsCoreDivId'
  });
  //Prevent the default end event (this moves the node back to its start position)
  dd.on('drag:end', function(e) {
    e.preventDefault();
  });
  }

  for (nodeGroupIndex in nodeGroups) {
    var nodeGroupName = nodeGroups[nodeGroupIndex];
    for (var i=0;i<numSlotsPerGroup[nodeGroupName];i++) {
      var thisId = '#slotForGroup' + nodeGroupName + i;
      var drop = new globalYui.DD.Drop({
        node: thisId
    });
    //Listen for a drop:hit on this target
    drop.on('drop:hit', function(e) {
      //Now we get the drag instance that triggered the drop hit
      var draggedItem = e.drag.get('node');
      var dropPlace = this.get('node');
      dropPlace.appendChild(draggedItem);
      serviceLocations[draggedItem.get('id')] = dropPlace.get('id');
      var msg = "Moved " + draggedItem.get('id') + " to " + dropPlace.get('id');
      setFormStatus(msg, false);
      globalYui.log(msg);
    });
    }
  }
}
