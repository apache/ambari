<!--
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
-->

<html>
<head>
<title>Hue to Ambari Migration</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<!-- Bootstrap CSS and bootstrap datepicker CSS used for styling the demo pages-->

<link rel="stylesheet" href="css/bootstrap.css">




<script src="js/jquery.js"></script>
<script src="js/bootstrap.min.js"></script>





<script type="text/javascript">
	$(function() {
		home();
	});
	function makeTabActive(tab) {
		if (!tab) {
			return;
		}
		$(".nav-tab").removeClass('active');
		$(tab).parents('.nav-tab').addClass('active');
	}
	function loadconfiguration(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/checkconfiguration.jsp');
	}
	function revertchange(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/revertchange.jsp');
	}
	function home(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/homepage.jsp');
	}
	function loadhivehistory(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/hivehistoryquerymigration.jsp');
	}
	function loadpigscript(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/pigscriptsmigration.jsp');
	}
	function loadpigjobs(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/pigjobmigration.jsp');
	}
	function loadhivesaved(tab) {
		makeTabActive(tab);
		$('#maincenter11').load('ui/hivesavedquerymigration.jsp');
	}
</script>


</head>

<div class="container">
	<!-- <div class="jumbotron" style="margin:10px">
    <h1>Hue to Ambari Migration</h1>        
  </div> -->



<div class="row">
	<nav class="navbar navbar-default">
		<div class="container-fluid">
			<ul class="nav navbar-nav">
				<li class="nav-tab active"><a onclick="home(this)">Home</a></li>
				<li class="nav-tab"><a onclick="loadconfiguration(this)">Check
						configuration</a></li>
				<li class="dropdown nav-tab"><a class="dropdown-toggle"
					data-toggle="dropdown" href="#">Hive <span class="caret"></span></a>
					<ul class="dropdown-menu">
						<li><span onclick="loadhivesaved(this)">HiveSaved Query</span></li>
						<li><span onclick="loadhivehistory(this)">HiveHistory</span></li>
					</ul></li>
				<li class="dropdown nav-tab"><a class="dropdown-toggle"
					data-toggle="dropdown" href="#">Pig <span class="caret"></span></a>
					<ul class="dropdown-menu">
						<li><span onclick="loadpigscript(this)">Pigsavedscript</span></li>
						<li><span onclick="loadpigjobs(this)">Pigjobs</span></li>
					</ul></li>
				<li class="nav-tab"><a onclick="revertchange(this)">Revert
						the changes Page</a></li>
			</ul>
		</div>
	</nav>
</div>
<div>
	<div class="col-lg-2 main"></div>
	<div class="col-lg-8 main">
		<div id="maincenter11"></div>
	</div>
</div>
</div>