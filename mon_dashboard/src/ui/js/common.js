/*
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

(function(){
  
  // Create namespace
  window.hdp_mon = hdp_mon = a = {
    version: "1.0"
  };
  
  // Create "global" variables
  var clickedRow, page, urls = [], graphs = [], previous, to, in_refresh_page = false,
    collection, collection2, context, alertParam = "all", 
    alerts, hostcounts, hbase_installed, hbase_link, hbase, 
    firstService, graphCounter = 0, errorFlag = false, errors = [], auto_refresh = false,
    gangliaErrorCount = 0;
  
  // on document ready
  $(document).ready(function(){
    document.title = 'Ambari';
    $('#brand').html('Ambari');
    $('#footer').html('<a href="http://www.apache.org/licenses/LICENSE-2.0" target="_blank">Licensed under the Apache License, Version 2.0</a>.<br><a href="/hmc/licenses/NOTICE.txt" target="_blank">See third-party tools/resources that Ambari uses and their respective authors</a>');
    $('.navbar a.help').attr('href', 'http://incubator.apache.org/ambari/install.html');
    self.clearTimeout(to);
    a.refreshPage();
    a.setEventDelegation();
    a.errorHandler();
  });
  
  a.autoRefresh = function() {
    if (document.getElementById("autorefresh").checked == true) {
      auto_refresh = true;
      a.refreshPage();
    } else {
      auto_refresh = false;
    }
  };

  a.refreshPage = function(){
    if (in_refresh_page) {
      if (auto_refresh) {
        to = self.setTimeout(function() { a.refreshPage(); }, 60000);
      }
      return;
    } else {
      in_refresh_page = true;
    }
    a.detectPage();
    a.setParams();
    a.getClusterInfo();
    a.createGraphs();
    a.getNagiosURL();
    if (auto_refresh) {
      to = self.setTimeout(function() { a.refreshPage(); }, 60000);
    }
    in_refresh_page = false;
  };

  // Detect which page we are looking at
  a.detectPage = function(){
    var Path = window.location.pathname,
      Page = Path.substring(Path.lastIndexOf('/') + 1);
    if(Page == "home.html"){
      page = "DASHBOARD";
    } else if (Page == "hdfs.html"){
      page = "HDFS";
    } else if (Page == "mapreduce.html"){
      page = "MAPREDUCE";
    } else if (Page == "hbase.html"){
      page = "HBASE";
    }
  };
  
  // Set parameters based on the page
  a.setParams = function(){
    if (page == "DASHBOARD"){
      context = "dashboard";
      collection = "hdp";
      collection2 = "all";
      alertParam = "nok";
    } else if (page == "HDFS"){
      context = "hdfs";
      collection = "hdp";
      collection2 = "all";
    } else if (page == "MAPREDUCE"){
      context = "mapreduce";
      collection = "hdp";
      collection2 = "all";
    } else if (page == "HBASE"){
      context = "hbase";
      collection = "hdp";
      collection2 = "all";
    }
    graphs = [
        {"url":"../dataServices/ganglia/get_graph_info.php?context=" + context + "&collection=" + collection + "&jsonp=?", "target":"#graphs_" + collection},
        {"url":"../dataServices/ganglia/get_graph_info.php?context=" + context + "&collection=" + collection2 + "&jsonp=?", "target":"#graphs_" + collection2}
    ];
    urls = [
          "../dataServices/jmx/get_jmx_info.php?info_type=cluster",
          "../dataServices/jmx/get_jmx_info.php?info_type=hdfs",
          "../dataServices/jmx/get_jmx_info.php?info_type=mapreduce",
          "../dataServices/jmx/get_jmx_info.php?info_type=hbase"
      ];
  };
  
  // Get Cluster Info for all the pages: Dashboard and Services
  a.getClusterInfo = function(){
    hbase_link = document.getElementById("hbase_link");
    if(page == "DASHBOARD"){
      // Call getClusterSummary
      $.getJSON(urls[0], function(response){
        var data = response.overall;
        
        // Grid 1
        var clusterGrid = document.getElementById("clusterSummaryGrid");
        if (data.namenode_starttime == undefined) {
          clusterGrid.style.backgroundColor = "lightgray";
          //clusterGrid.innerHTML= "HDFS (Down)";
          document.getElementById("hdfssummarytitle").innerHTML= "HDFS (Down)";
        }

        // NN Uptime
        if (data.namenode_starttime != undefined) {
          var now = new Date(), 
          actualTimeInMs = now.setUTCSeconds(0), 
          actualTime = actualTimeInMs.toString().substring(0,10), 
          result = actualTime - data.namenode_starttime;
          clusterGrid.rows[1].cells[1].innerHTML = a.convertToDDHHMM(result);
        }

        // HDFS Capacity
        if (data.dfs_used_bytes != undefined) {
          clusterGrid.rows[2].cells[1].innerHTML = a.convertBytes(data.dfs_used_bytes, 2) + " / " + 
                                                 a.convertBytes(data.dfs_total_bytes, 2);
        }

        // Live vs dead nodes vs decomm
        if (data.live_nodes != undefined) {
          clusterGrid.rows[3].cells[1].innerHTML = 
             '<a href="http://' + data.namenode_addr + '/dfsnodelist.jsp?whatNodes=LIVE">' + data.live_nodes + 
             '</a>' + ' / ' + '<a href="http://' + data.namenode_addr + '/dfsnodelist.jsp?whatNodes=DEAD">' + 
             data.dead_nodes + '</a>' + ' / ' + '<a href="http://' + data.namenode_addr + 
             '/dfsnodelist.jsp?whatNodes=DECOMMISSIONING">' + data.decommissioning_nodes + '</a>';
        }
        
        // Under replicated block count
        clusterGrid.rows[4].cells[1].innerHTML = data.dfs_blocks_underreplicated;
        
        // Grid 2
        var clusterGrid2 = document.getElementById("clusterSummaryGrid2");
        if (data.jobtracker_starttime == undefined) {
          clusterGrid2.style.backgroundColor = "lightgray";
          document.getElementById("mapredsummarytitle").innerHTML= "MapReduce (Down)";
        }

        // JT Uptime
        if (data.jobtracker_starttime != undefined) {
          var result2 = actualTime - data.jobtracker_starttime;
          clusterGrid2.rows[1].cells[1].innerHTML = a.convertToDDHHMM(result2);
        }

        // Trackers (live/blacklisted)
        if (data.trackers_live != undefined) {
          clusterGrid2.rows[2].cells[1].innerHTML = 
             '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=active">' + data.trackers_live + 
             '</a>' + ' / ' + '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=blacklisted">' + 
             data.trackers_blacklisted + '</a>';
        }

        // Running Jobs
        if (data.running_jobs != undefined) {
          clusterGrid2.rows[3].cells[1].innerHTML = data.running_jobs + " & " + data.waiting_jobs;
        }
        
        // Grid 3
        var clusterGrid3 = document.getElementById("clusterSummaryGrid3");
        if (data.hbasemaster_starttime == undefined) {
          clusterGrid3.style.backgroundColor = "lightgray";
          document.getElementById("hbasesummarytitle").innerHTML= "HBase (Down)";
        }

        // HBase Uptime
        if (data.hbasemaster_starttime != undefined) {
          var result3 = actualTime - data.hbasemaster_starttime;
          clusterGrid3.rows[1].cells[1].innerHTML = a.convertToDDHHMM(result3);
        }

        // Region servers
        if (data.live_regionservers != undefined) {
          clusterGrid3.rows[2].cells[1].innerHTML = 
              '<a href="http://' + data.hbasemaster_addr + '/master-status">' + data.live_regionservers + 
              '</a>' + ' / ' + '<a href="http://' + data.hbasemaster_addr + '/master-status">' + 
              data.dead_regionservers + '</a>';
        }

        // Regions in Transition
        clusterGrid3.rows[3].cells[1].innerHTML = data.regions_in_transition_count;
        
        // Draw Disk Utilization Pie chart
        a.drawDiskUtilPieChart(data);
        // Populate Ganglia & Nagios Links
        a.addLinks(data);
        
        // Disable HBASE if it is not installed
        hbase_installed = data.hbase_installed;
        if(hbase_installed == false){
          a.disableHBase();
          var hbaseSummaryTable = document.getElementById("clusterSummaryGrid3");
          hbaseSummaryTable.style.display = "none";
          var clusterContainer = document.getElementById("clusterTextContainer"), 
            span = document.createElement("span");
          span.innerHTML = "HBase is not installed";
          clusterContainer.appendChild(span);
        }
      })
      .error(function(error){
        errorFlag = true;
        errors.push("Cluster Summary");
      });
    } else if(page == "HDFS"){
      // Call getClusterSummary
      $.getJSON(urls[1], function(response){
        var data = response.hdfs, 
          overall = response.overall;
        
        // Grid 1
        var hdfsGrid = document.getElementById("hdfsSummaryGrid1");
        // Version
        hdfsGrid.rows[0].cells[1].innerHTML = data.version;
        // NN address
        var firstDot = data.namenode_addr.indexOf("."), 
          colon = data.namenode_addr.indexOf(":"), 
          NNaddress = data.namenode_addr.substring(0,firstDot) + data.namenode_addr.substring(colon);
        hdfsGrid.rows[1].cells[1].innerHTML = '<a href="http://' + data.namenode_addr + '/dfshealth.jsp">' + NNaddress + '</a>';
        // NN Uptime
        var now = new Date(), 
          actualTimeInMs = now.setUTCSeconds(0), 
          actualTime = actualTimeInMs.toString().substring(0,10), 
          result = actualTime - data.start_time;
        hdfsGrid.rows[2].cells[1].innerHTML = a.convertToDDHHMM(result);
        // NN Heap
        used = (data.memory_heap_used / data.memory_heap_max) * 100;
        hdfsGrid.rows[3].cells[1].innerHTML = a.convertBytes(data.memory_heap_used, 1) + " / " + a.convertBytes(data.memory_heap_max, 1) + " (" + a.calcPercent(used) + "%)";
        // Pending upgrades
        var upgrades;
        if (data.pending_upgrades == true){
          upgrades = "there are pending upgrades"
        } else if (data.pending_upgrades == false){
          upgrades = "no pending upgrades";
        }
        hdfsGrid.rows[4].cells[1].innerHTML = upgrades;
        // SafeMode
        var safemode;
        if (data.safemode == true){
          safemode = '<a href="http://' + data.namenode_addr + '/dfshealth.jsp">in safemode</a>';
        } else if (data.safemode == false){
          safemode = "not in safemode";
        }
        hdfsGrid.rows[5].cells[1].innerHTML = safemode;
        
        // Grid 2
        var hdfsGrid2 = document.getElementById("hdfsSummaryGrid2");
        // Live vs dead nodes vs decomm
        hdfsGrid2.rows[0].cells[1].innerHTML = '<a href="http://' + data.namenode_addr + '/dfsnodelist.jsp?whatNodes=LIVE">' + data.live_nodes + 
        '</a>' + ' / ' + '<a href="http://' + data.namenode_addr + '/dfsnodelist.jsp?whatNodes=DEAD">' + data.dead_nodes + 
        '</a>' + ' / ' + '<a href="http://' + data.namenode_addr + '/dfsnodelist.jsp?whatNodes=DECOMMISSIONING">' + data.decommissioning_nodes + '</a>';
        // HDFS Disk Capacity
        var used = (100 - data.dfs_percent_remaining) * data.dfs_configured_capacity / 100, 
          usedPercent = 100 - data.dfs_percent_remaining;
        hdfsGrid2.rows[1].cells[1].innerHTML = a.convertBytes(used, 2) + " / " + a.convertBytes(data.dfs_configured_capacity, 2) + " (" + a.calcPercent(usedPercent) + "%)";        
        
        // HDFS Blocks
        hdfsGrid2.rows[2].cells[1].innerHTML = data.dfs_blocks_total;
        // HDFS Blocks
        hdfsGrid2.rows[3].cells[1].innerHTML = data.dfs_blocks_corrupt + " / " + data.dfs_blocks_missing + " / " + data.dfs_blocks_underreplicated;
        // Total files + dirs
        hdfsGrid2.rows[4].cells[1].innerHTML = data.dfs_dirfiles_count;
        
        // Quick Links
        var nnUI = document.getElementById("nnUI");
        nnUI.setAttribute("href", "http://" + data.namenode_addr + "/dfshealth.jsp");
        var nnLogs = document.getElementById("nnLogs");
        nnLogs.setAttribute("href", "http://" + data.namenode_addr + "/logs");
        // Advanced Links
        var nnJMX = document.getElementById("nnJMX");
        nnJMX.setAttribute("href", "http://" + data.namenode_addr + "/jmx");
        var nnTS = document.getElementById("nnTS");
        nnTS.setAttribute("href", "http://" + data.namenode_addr + "/stacks");
        
        // Populate Ganglia & Nagios Links
        a.addLinks(overall);
        
        // Disable HBASE if it is not installed
        hbase_installed = overall.hbase_installed;
        if(hbase_installed == false){
          a.disableHBase();
        }
      })
      .error(function(error){
        errorFlag = true;
        errors.push("HDFS Summary");
      });
    } else if(page == "MAPREDUCE"){
      // Call getClusterSummary
      $.getJSON(urls[2], function(response){
        var data = response.mapreduce, 
          overall = response.overall;
        
        var mrGrid = document.getElementById("mrSummaryGrid1");
        // Version
        mrGrid.rows[0].cells[1].innerHTML = data.version;
        // JT address
        var firstDot = data.jobtracker_addr.indexOf("."), 
          colon = data.jobtracker_addr.indexOf(":"), 
          JTaddress = data.jobtracker_addr.substring(0,firstDot) + data.jobtracker_addr.substring(colon);
        mrGrid.rows[1].cells[1].innerHTML = '<a href="http://' + data.jobtracker_addr + '/jobtracker.jsp">' + JTaddress + '</a>';
        // JT Uptime
        var now = new Date(), 
          actualTimeInMs = now.setUTCSeconds(0), 
          actualTime = actualTimeInMs.toString().substring(0,10), 
          result = actualTime - data.start_time;
        mrGrid.rows[2].cells[1].innerHTML = a.convertToDDHHMM(result);
        // Trackers: Live, total
        mrGrid.rows[3].cells[1].innerHTML = '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=active">' + data.trackers_live + 
        '</a>' + ' / ' + data.trackers_total;
        // Trackers: rest
        mrGrid.rows[4].cells[1].innerHTML = '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=blacklisted">' + data.trackers_blacklisted + 
        '</a>' + ' / ' + '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=graylisted">' + data.trackers_graylisted + 
        '</a>' + ' / ' + '<a href="http://' + data.jobtracker_addr + '/machines.jsp?type=excluded">' + data.trackers_excluded + '</a>'; 
        // Heap
        var used = (data.memory_heap_used / data.memory_heap_max) * 100;
        mrGrid.rows[5].cells[1].innerHTML = a.convertBytes(data.memory_heap_used, 2) + " / " + a.convertBytes(data.memory_heap_max, 2) + " (" + a.calcPercent(used) + "%)";
        
        var mrGrid2 = document.getElementById("mrSummaryGrid2");
        // Total Capacity
            mrGrid2.rows[0].cells[1].innerHTML = data.map_task_capacity + " / " + data.reduce_task_capacity + " / " + data.average_node_capacity; 
            // Total jobs
            mrGrid2.rows[1].cells[1].innerHTML = data.job_total_submissions + " / " + data.job_total_completions;
        // Current Slots - Maps
            mrGrid2.rows[2].cells[1].innerHTML = data.occupied_map_slots + " / " + data.reserved_map_slots;
            // Current Slots - Reduces
            mrGrid2.rows[3].cells[1].innerHTML = data.occupied_reduce_slots + " / " + data.reserved_reduce_slots;
            
            // Maps
            mrGrid2.rows[4].cells[1].innerHTML = data.running_map_tasks + " / " + data.waiting_maps;
            // Reduces
            mrGrid2.rows[5].cells[1].innerHTML = data.running_reduce_tasks + " / " + data.waiting_reduces;
            
        // Quick Links
            var jt0 = document.getElementById("jt0");
        jt0.setAttribute("href", "http://" + data.jobtracker_addr + "/jobtracker.jsp");      
        var jt1 = document.getElementById("jt1");
        jt1.setAttribute("href", "http://" + data.jobtracker_addr + "/scheduler");      
        var jt2 = document.getElementById("jt2");
        jt2.setAttribute("href", "http://" + data.jobtracker_addr + "/jobtracker.jsp#running_jobs");
        var jt3 = document.getElementById("jt3");
        jt3.setAttribute("href", "http://" + data.jobtracker_addr + "/jobtracker.jsp#retired_jobs");
        var jt4 = document.getElementById("jt4");
        jt4.setAttribute("href", "http://" + data.jobhistory_addr + "/jobhistoryhome.jsp");
        var jt5 = document.getElementById("jt5");
        jt5.setAttribute("href", "http://" + data.jobtracker_addr + "/logs");
        // Advanced links
        var jtJMX = document.getElementById("jtJMX");
        jtJMX.setAttribute("href", "http://" + data.jobtracker_addr + "/jmx");
        var jt6 = document.getElementById("jt6");
        jt6.setAttribute("href", "http://" + data.jobtracker_addr + "/stacks");
        
        // Populate Ganglia & Nagios Links
        a.addLinks(overall);
        
        // Disable HBASE if it is not installed
        hbase_installed = overall.hbase_installed;
        if(hbase_installed == false){
          a.disableHBase();
        }
      })
      .error(function(error){
        errorFlag = true;
        errors.push("MapReduce Summary");
      });
    } else if(page == "HBASE"){
      // Call getClusterSummary
      $.getJSON(urls[3], function(response){
        var data = response.hbase, overall = response.overall;
        
        // Disable HBASE if it is not installed
        hbase_installed = overall.hbase_installed;

        if(hbase_installed == false){
          HBASE = document.getElementById("HBASE");
          a.disableHBase();
          var links1 = document.getElementById("links1"), 
            links2 = document.getElementById("links2");
          links1.innerHTML = "HBase is not installed";
          links2.innerHTML = "";
          // Clear Data in the Grid
          $("#alertsGrid").clearGridData();
          $("#alertsGrid tbody").html("<tr><td style='text-align:center;font-weight:bold;height:12px;background-color:gray;' colspan='5'>HBase is not installed.</td></tr>");
        } else if (hbase_installed == true){
          var hbaseGrid = document.getElementById("hbaseSummaryGrid1");
          // Version
          hbaseGrid.rows[0].cells[1].innerHTML = data.version;
          // HB Master address
          var firstDot = data.hbasemaster_addr.indexOf("."), 
            colon = data.hbasemaster_addr.indexOf(":"), 
            HBMaddress = data.hbasemaster_addr.substring(0,firstDot) + data.hbasemaster_addr.substring(colon);
          hbaseGrid.rows[1].cells[1].innerHTML = '<a href="http://' + data.hbasemaster_addr + '/master-status">' + HBMaddress + '</a>';
          // Region servers
          hbaseGrid.rows[2].cells[1].innerHTML = '<a href="http://' + data.hbasemaster_addr + '/master-status">' + data.live_regionservers + '</a>' + " / "
          + '<a href="http://' + data.hbasemaster_addr + '/master-status">' + data.dead_regionservers + '</a>';
          // Regions in transition
          hbaseGrid.rows[3].cells[1].innerHTML = data.regions_in_transition_count;
          // ZK Quorum
          
          var hbaseGrid2 = document.getElementById("hbaseSummaryGrid2");
          // Master Started
          hbaseGrid2.rows[0].cells[1].innerHTML = a.convertToUTC(data.start_time);
          // Master Activated
          hbaseGrid2.rows[1].cells[1].innerHTML = a.convertToUTC(data.active_time);
          // Avg Region servers
          hbaseGrid2.rows[2].cells[1].innerHTML = data.average_load;
          // Heap
          var used = (data.memory_heap_used / data.memory_heap_max) * 100;
          hbaseGrid2.rows[3].cells[1].innerHTML = a.convertBytes(data.memory_heap_used, 2) + " / " + a.convertBytes(data.memory_heap_max, 2) + " (" + a.calcPercent(used) + "%)";
          
          // Quick Links
          var hb1 = document.getElementById("hb1");
          hb1.setAttribute("href", "http://" + data.hbasemaster_addr + "/master-status");    
          var hb2 = document.getElementById("hb2");
          hb2.setAttribute("href", "http://" + data.hbasemaster_addr + "/logs");
          var hb3 = document.getElementById("hb3");
          hb3.setAttribute("href", "http://" + data.hbasemaster_addr + "/zk.jsp");
          
          // Advanced Links
          var hbJMX = document.getElementById("hbJMX");
          hbJMX.setAttribute("href", "http://" + data.hbasemaster_addr + "/jmx");
          var hb4 = document.getElementById("hb4");
          hb4.setAttribute("href", "http://" + data.hbasemaster_addr + "/dump");
          var hb5 = document.getElementById("hb5");
          hb5.setAttribute("href", "http://" + data.hbasemaster_addr + "/stacks");
        }
        
        // Populate Ganglia & Nagios Links
        a.addLinks(overall);
      })
      .error(function(error){
        errorFlag = true;
        errors.push("HBase Summary");
      });
    }
  };
  
  // Get and Construct Nagios server URL
  a.getNagiosURL = function(){
    $.getJSON("../dataServices/conf/cluster_configuration.json", function(data){
      var nagios = data.overall.nagios;
      var url = "http://" + nagios.nagiosserver_host + ":" + nagios.nagiosserver_port + "/hdp/nagios/nagios_alerts.php?q1=alerts&alert_type="+alertParam+"&jsonp=?";
      a.getAlerts(url);
    })
    .error(function(error){
      errorFlag = true;
      errors.push("Cluster Configuration");
    });
  };
  
  // Set Event Delegation
  a.setEventDelegation = function(){
    var body = document.getElementsByTagName("body")[0];
    body.onclick = a.eventDelegation;
  }
  
  // Get and draw graphs for dashboard main page
  a.createGraphs = function(){
    if(page == "DASHBOARD"){
      a.getGraphs(graphs[0]);
      a.getGraphs(graphs[1]);
    } else if(page == "HDFS" || page == "MAPREDUCE" || page == "HBASE"){
      a.getGraphs(graphs[0]);
    }
  }
  
  // Event Delegation
  a.eventDelegation = function(e){
    if(!e){
      if(window.event){
        // IE 8 & under
        e = window.event;
      }
    }
    var target;
    if(e.target){
      target = e.target; 
    } else if(e.srcElement) {
      target = e.srcElement;
    }
    var targetId = target.id;
    if (targetId == "close"){
      a.hideGraphPopup();
    } else if(target.parentNode){
      var targetParentId = target.parentNode.id;
      if (targetParentId == "HDFS" || targetParentId == "MAPREDUCE" || targetParentId == "HBASE" || targetParentId == "ZOOKEEPER" || targetParentId == "HIVE-METASTORE" || targetParentId == "OOZIE" || targetParentId == "TEMPLETON") {
        a.showAlerts(target);
      }
    }
  };
  
  // Disable HBase
  a.disableHBase = function(){
    hbase_link.setAttribute("href", "");
    hbase_link.setAttribute("title", "HBase is not installed");
    hbase_link.setAttribute("onclick", "return false;");
    hbase_link.style.backgroundColor = "gray";
  };
  
  // Calculate % decimals
  a.calcPercent = function(used){
    if(used.toString().substring(1, 2) == "."){
      used = used.toString().substring(0, 3);
    } else if(used.toString().substring(0, 3) < 100){
      used = used.toString().substring(0, 2);
    } else if(used.toString().substring(0, 3) >= 100){
      used = used.toString().substring(0, 3);
    }
    return used;
  };
  
  // Convert seconds to days
  a.convertToDDHHMM = function(seconds){
    if(seconds == null || seconds == undefined || seconds == 0){
      return "n/a";
    }
    var days = Math.floor(seconds/86400),
      hours = Math.floor((seconds%86400)/3600), 
      mins = Math.floor(((seconds%86400)%3600)/60);
    return days + "day " + hours + "hr " + mins + "min";
  };
  
  // Convert seconds to UTC Date & Time
  a.convertToUTC = function(time){
    var UTC = new Date(time * 1000);
    if (UTC != null && UTC != undefined && UTC != 0) {
      UTC = UTC.toUTCString().substring(4,25);
    }
    return UTC;
  };
  
  // Convert bytes to MBs, GBs, TBs
  a.convertBytes = function(bytes, precision){
      var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'], 
        posttxt = 0;
      if (bytes == 0 || bytes == undefined || bytes == ""){
        return 'n/a';
      }
      while(bytes >= 1024) {
          posttxt++;
          bytes = bytes / 1024;
      }
      return bytes.toFixed(precision) + " " + sizes[posttxt];
  };
  
  // Draw pie chart on Dashboard
  a.drawDiskUtilPieChart = function(clusterData){
    $('#pie2').html('');
    var r = Raphael("pie2"), pie, data = {};
    if (clusterData.dfs_percent_remaining == undefined) {
      data = {data:[100],label:{legend:["HDFS Down"], legendpos:"east"}};
      pie = r.piechart(130, 100, 80, data.data, data.label);
      pie.attr("fill", "gray");
      return;
    }
    if(clusterData.dfs_percent_remaining == 100){
      data = {data:[100],label:{legend:["%%.%% Free"], legendpos:"east"}};
    }else if(clusterData.dfs_percent_remaining == 0){
      data = {data:[100],label:{legend:["%%.%% Used"], legendpos:"east"}};
    } else {
      data = {data:[100 - clusterData.dfs_percent_remaining, clusterData.dfs_percent_remaining],label:{legend:["%%.%% Used", "%%.%% Free"], legendpos:"east"}};
    }
    pie = r.piechart(130, 100, 80, data.data, data.label);
  };
  
  // Draw pie chart on Dashboard
  a.drawNodesUpPieChart = function(response){
    $('#pie1').html('');
    var r = Raphael("pie1"), pie, data = {};
    if (response.hostcounts.down_hosts == undefined) {
      data = {data:[100],label:{legend:["Nagios Down"], legendpos:"east"}};
      pie = r.piechart(130, 100, 80, data.data, data.label);
      pie.attr("fill", "gray");
      return;
    }
    document.getElementById("nodesud").innerHTML = 
              "Nodes Up/Down ("+response.hostcounts.up_hosts+"/"+response.hostcounts.down_hosts+")";
    if(response.hostcounts.down_hosts == 0){
      data = {data:[100],label:{legend:["%%.%% Up"], legendpos:"east"}};
    }else if(response.hostcounts.up_hosts == 0){
      data = {data:[100],label:{legend:["%%.%% Down"], legendpos:"east"}};
    } else {
      data = {data:[response.hostcounts.down_hosts, response.hostcounts.up_hosts],label:{legend:["%%.%% Down", "%%.%% Up"], legendpos:"east"}};
    }
    pie = r.piechart(130, 100, 80, data.data, data.label);
  };

  // Generate links for Ganglia and Nagios
  a.addLinks = function(data){
    var nagios = document.getElementById("nagios");
    nagios.setAttribute("href", data.nagios_url);
    var ganglia = document.getElementById("ganglia");
    ganglia.setAttribute("href", data.ganglia_url);
    if (data.oozie_url != "") {
      var oozie_button = document.createElement("a");
      oozie_button.setAttribute("id", "oozie");
      oozie_button.setAttribute("class", "roundedBox");
      oozie_button.setAttribute("target", "_blank");
      oozie_button.setAttribute("href", data.oozie_url);
      oozie_button.innerHTML = "Oozie";
      var oozie_link = document.getElementById("oozie");
      oozie_link.parentNode.replaceChild(oozie_button, oozie_link);
      //links.appendChild(oozie_button);
    } 
  };
  
  // Get Graphs for Dashboard page
  a.getGraphs = function(dashGraph){
    $.getJSON(dashGraph.url, function(data){
      a.drawGraphs(dashGraph, data);
    })
    .error(function(error){
      errorFlag = true;
      if(gangliaErrorCount == 0){
        errors.push("Ganglia");
      }
      gangliaErrorCount++;
    });
  };
  
  // Draw Ganglia Graphs
  a.drawGraphs = function(dashGraph, data){
    var graphs = data.Global;
    var images = $(dashGraph.target).find('img');
    var timestamp = new Date().getTime();
    images.each(function(i) {
      $(this).attr('title', graphs[i].description);
      $(this).attr('alt', graphs[i].description);
      $(this).attr('src', graphs[i].url + "&timestamp=" + timestamp);
      $(this).parent().attr('href', graphs[i].link);
      $(this).click(function() { a.showGraphPopup($(this)); return false; } );
    });
  };
  
  // Show Ganglia graph in a popup
  a.showGraphPopup = function(image) {
    $("#graphPopup #popupImg").attr("src", image.attr('src'));
    var link = $("#graphPopup #graphForwardLink");
    link.attr("href", image.parent().attr('href'));
    link.html("Link to Cluster Graphs");
    $("#graphPopup").show();
  };

  // Hide Ganglia graph form popup
  a.hideGraphPopup = function(){
    $("#graphPopup").hide();
  }
  
  // Get Alerts for Dashboard page
  a.getAlerts = function(url) {
    $.getJSON(url, function(response){
      alerts = response.alerts;
      hostcounts = response.hostcounts;
      // Step 1 out of 3: Create alert counters on dashboard
      var hdfsCritCount = 0, hdfsWarnCount = 0, mrCritCount = 0, mrWarnCount = 0, 
      hbaseCritCount = 0, hbaseWarnCount = 0, zkCritCount = 0, zkWarnCount = 0, 
      hcatCritCount = 0, hcatWarnCount = 0, oozieWarnCount = 0, oozieCritCount = 0,
                        templetonWarnCount = 0, templetonCritCount = 0;
      
      //Set firstService in the table
      var servicestates = response.servicestates;
      for(service in servicestates){
        firstService = service;
        break;
      }
      
      // Show the alerts after the UI got the response
      a.showAlerts = function(target){
        var len = alerts.length, targetId, i, j, converted, filtered = [];
        // On page load
        if(!target){
          if(page == "DASHBOARD"){
            targetId = firstService;
          } else if(page == "HDFS" || page == "MAPREDUCE" || page == "HBASE"){
            targetId = page;
          }
        // In every other case - when you click the summary table to see service-related alerts
        } else if(target){
          targetId = target.parentNode.id;
        }
        
        // If HBase is not installed ...
        if(hbase_installed == false && targetId == "HBASE"){
          $("#alertsGrid").clearGridData();
        } else {
          // Loop through alerts dataset - for page load on all pages
          for (i=0; i<len; i++){
            converted = {};
            if (alerts[i].service_type == targetId || alerts[i].service_type == "SYSTEM") {
              // Alert Name
              converted.service_description = alerts[i].service_description;
              
              // Alert Status
              var status;
              if (alerts[i].last_hard_state == 0) {
                status = "<span style='display:none;'>3</span><span class='highlighted-green'>OK";
              } else if (alerts[i].last_hard_state == 1) {
                status = "<span style='display:none;'>2</span><span class='highlighted-orange'>WARN";
              } else if (alerts[i].last_hard_state == 2) {
                status = "<span style='display:none;'>1</span><span class='highlighted-red'>CRIT";
              } else if (alerts[i].last_hard_state == 3) {
                status = "<span style='display:none;'>4</span><span class='highlighted-red'>???";
              }            
              converted.last_hard_state = status;
              
              // Last Check Time
              converted.last_check = a.convertToUTC(alerts[i].last_check);
              
              // Duration
              var now = new Date();
              var actualTimeInMs = now.setUTCSeconds(0)
              var actualTime = actualTimeInMs.toString().substring(0,10);
              converted.last_hard_state_change = a.convertToDDHHMM(actualTime - alerts[i].last_hard_state_change);
              
              // Plugin Output
              converted.plugin_output = alerts[i].plugin_output;
              
              filtered.push(converted);
            }
            
            // Step 2 out of 3: Increment Counters for Alert Summary Table
            if (alerts[i].last_hard_state == 1) {
              if(alerts[i].service_type == "HDFS"){
                hdfsWarnCount++;
              } else if(alerts[i].service_type == "MAPREDUCE"){
                mrWarnCount++;
              } else if(alerts[i].service_type == "HBASE"){
                hbaseWarnCount++;
              } else if(alerts[i].service_type == "ZOOKEEPER"){
                zkWarnCount++;
              } else if(alerts[i].service_type == "HIVE-METASTORE"){
                hcatWarnCount++;
              } else if(alerts[i].service_type == "OOZIE"){
                oozieWarnCount++;
              } else if(alerts[i].service_type == "TEMPLETON"){
                templetonWarnCount++;
              }
            } else if (alerts[i].last_hard_state == 2) {
              if(alerts[i].service_type == "HDFS"){
                hdfsCritCount++;
              } else if(alerts[i].service_type == "MAPREDUCE"){
                mrCritCount++;
              } else if(alerts[i].service_type == "HBASE"){
                hbaseCritCount++;
              } else if(alerts[i].service_type == "ZOOKEEPER"){
                zkCritCount++;
              } else if(alerts[i].service_type == "HIVE-METASTORE"){
                hcatCritCount++;
              } else if(alerts[i].service_type == "OOZIE"){
                oozieCritCount++;
              } else if(alerts[i].service_type == "TEMPLETON"){
                templetonCritCount++;
              }
            }
            
          }
          
          $(function(){ 
            $("#alertsGrid").jqGrid({
              datatype: 'local',
              height:120,
              colNames:['Alert Name','Status','Last Check Time','Duration','Description'],
              colModel :[ 
                     {name:'service_description', index:'service_description', width:200, align:'left', sorttype:'text'},
                     {name:'last_hard_state', index:'last_hard_state', width:35, align:'left', sorttype:'text'},
                     {name:'last_check', index:'last_check', width:100, align:'left', sorttype:'date'},
                     {name:'last_hard_state_change', index:'last_hard_state_change', width:97, align:'left', sorttype:'date'},
                     {name:'plugin_output', index:'plugin_output', width:400, align:'left', sorttype:'text'}
              ],
              pager: '#pager',
              rowNum:10,
              rowList:[10,20,30],
              sortable:true,
              sortname: 'last_hard_state',
              viewrecords: true,
              gridview: true,
              shrinkToFit:false,
              width:700,
              loadtext: "Loading data...",
                            pgtext : "Page {0} of {1}",
                            gridComplete:function(){
                // to fix sorting problem on page load
                $("#alertsGrid").jqGrid().setGridParam({sortorder: 'asc'}).trigger("reloadGrid");
              },
              caption: targetId + " Alerts"
            });
          });
          
          // Clear the grid first
          $("#alertsGrid").clearGridData();

          // Refresh the caption to indicate service name.
          if(page == "HDFS" || page == "MAPREDUCE" || page == "HBASE"){
            $("#alertsGrid").jqGrid('setCaption',"Configured Alerts (<a href=\"\" target=\"\"></a>)");
          } else {
            $("#alertsGrid").jqGrid('setCaption',targetId+" Alerts (<a href=\"\" target=\"\"></a>)");
          }
          
          // Populate Alerts Grid
          var flen = filtered.length;
          for(j=0;j<=flen;j++){
            $("#alertsGrid").jqGrid('addRowData',j+1,filtered[j]);
          }
        }  
      };
      
      // Get and show data for Service Summary Table
      a.showServiceSummary = function(){
        var services = response.servicestates;
        var serviceCounter = 0;
        var servicesGrid = $("#servicesGrid");
        servicesGrid.html("");
        
        // Construct Services Table Rows
        for(key in services) {
          if(page == "DASHBOARD" || page == key){
            // Construct Row
            var tr = document.createElement("tr");
            if(page == "DASHBOARD"){
              tr.onclick = a.selectRow;
            }
            tr.className = "row";
            if(clickedRow == null && serviceCounter == 0 || clickedRow == undefined && serviceCounter == 0){
              tr.className = "selected row";
            }
            serviceCounter++;
            tr.setAttribute("id", key);
            
            // Service Name
            var serviceName = document.createElement("td");
            serviceName.innerHTML = key;
            serviceName.className = "service";
            tr.appendChild(serviceName);
            
            // Service State
            var serviceState = document.createElement("td"), 
              state;
            if (services[key] == 0) {
              state = "Running";
            } else if (services[key] == 1) {
              /*if(key == "HBASE" && hbase_installed == false){
                state = "Not configured";
              } else {*/
                state = "Down";
              //}
              serviceState.style.color = "red";
            } else if (services[key] == 2) {
              state = "Unknown";
            }
            serviceState.innerHTML = state;
            serviceState.className = "state";
            tr.appendChild(serviceState);
            
            // Step 3/A out of 3: Update Alert Summary Table with critical alert counts
            var criticalAlerts = document.createElement("td");
            if(key == "HDFS"){
              criticalAlerts.innerHTML = hdfsCritCount;
              if(hdfsCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "MAPREDUCE"){
              criticalAlerts.innerHTML = mrCritCount;
              if(mrCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "HBASE"){
              criticalAlerts.innerHTML = hbaseCritCount;
              if(hbaseCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "ZOOKEEPER"){
              criticalAlerts.innerHTML = zkCritCount;
              if(zkCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "HIVE-METASTORE"){
              criticalAlerts.innerHTML = hcatCritCount;
              if(hcatCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "OOZIE"){
              criticalAlerts.innerHTML = oozieCritCount;
              if(oozieCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            } else if(key == "TEMPLETON"){
              criticalAlerts.innerHTML = templetonCritCount;
              if(templetonCritCount > 0) {
                criticalAlerts.className = "highlighted-red critical";
              } else {
                criticalAlerts.className = "critical";
              }
            }
            tr.appendChild(criticalAlerts);
            
            // Step 3/B out of 3: Update Alert Summary Table with warn alert counts
            var warnAlerts = document.createElement("td");
            if(key == "HDFS"){
              warnAlerts.innerHTML = hdfsWarnCount; 
              if(hdfsWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "MAPREDUCE"){
              warnAlerts.innerHTML = mrWarnCount;
              if(mrWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "HBASE"){
              warnAlerts.innerHTML = hbaseWarnCount;
              if(hbaseWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "ZOOKEEPER"){
              warnAlerts.innerHTML = zkWarnCount;
              if(zkWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "HIVE-METASTORE"){
              warnAlerts.innerHTML = hcatWarnCount;
              if(hcatWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "OOZIE"){
              warnAlerts.innerHTML = oozieWarnCount;
              if(oozieWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            } else if(key == "TEMPLETON"){
              warnAlerts.innerHTML = templetonWarnCount;
              if(templetonWarnCount > 0) {
                warnAlerts.className = "highlighted-orage warning";
              } else {
                warnAlerts.className = "warning";
              }
            }
            tr.appendChild(warnAlerts);
            servicesGrid.append(tr);
          }
        }
        if(page == "DASHBOARD"){
          a.drawNodesUpPieChart(response);
        }
      };
      a.showAlerts();
      a.showServiceSummary();
    })
    .error(function(error){
      errorFlag = true;
      errors.push("Nagios");
    });
  };
  
  // Row highlighting for selected row in Service Summary table
  a.selectRow = function(e){
    if(!e){
      if(window.event){
        // IE 8 & under
        e = window.event;
      }
    }
    var target;
    if(e.target){
      target = e.target; 
    } else if(e.srcElement) {
      target = e.srcElement;
    }
    if(target){
      clickedRow = target.parentNode;
    } else {
      clickedRow = document.getElementById(firstService);
    }
    if(previous){
      previous.className = "row";
      previous = clickedRow;
    } else {
      previous = document.getElementById(firstService);
      previous.className = "row";
      previous = clickedRow;
    }
    clickedRow.className = "selected row";
  };
  
  // Run error handling
  a.errorHandler = function(){
    if(errorFlag == true){
      var n, len = errors.length, components = "";
      for(n=0; n<len; n++){
        if(n == len-1){
          components += errors[n] + ".";
        } else {
          components += errors[n] + ", ";
        }
      }
      alert("Error retrieving data from backend services: " + components);
    }
  };
  
})();
