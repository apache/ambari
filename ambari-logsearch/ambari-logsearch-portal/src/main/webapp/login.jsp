<!-- 
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!DOCTYPE html>

<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Log Search</title>
		<meta name="description" content="description">
		<meta name="author" content="Evgeniya">
		<meta name="keyword" content="keywords">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<link href="libs/bower/bootstrap/bootstrap.css" rel="stylesheet">
		<link href="libs/other/font-awesome/css/font-awesome.min.css" rel="stylesheet">

		<link href="styles/style_v2.css" rel="stylesheet">
		<link href="styles/style.css" rel="stylesheet">
		<script   src="libs/bower/jquery/js/jquery.min.js"></script>
		<script type="text/javascript">
			$(document).ready(function() {
				var passDiv = $('#passCont');

		        $.ajax({
					url : "/service/public/getGeneralConfig",
					type: "GET",
					async: true,
					dataType: 'json',
						success:function(results,status) 
						{
							for(var i in results.vNameValues){
								if ((results.vNameValues[i].name === "simpleAuth") && (results.vNameValues[i].value === "true")){
									$("#password").val("");
									passDiv.hide();
								}	
								if (passDiv.is(':hidden'))
								   $("#password").prop('required',false);
							}
						},
						error: function(errorThrown) 
						{
						    passDiv.show();
						},
			            complete : function(){
			                $('[data-id="loader"]').hide();            
			            }
				});
			});
	</script>
	</head>

	<body>
		<div class="container-fluid">
				<header class="navbar">
					<div class="container-fluid expanded-panel">
						<div class="row top-panel-right">
							<div id="logo" class="col-xs-12 col-sm-3">
						        <div class="row">
							          <div class="col-sm-2"><img title="Apache Ambari" alt="Apache Ambari" src="images/logo-white.png" height="32px"></div>
							          <div class="col-sm-10"><a href="javascript:void(0);">Log Search</a></div>
						        </div>
							</div>
						</div>
					</div>
				</header>
					<div class="box-wrapper">
						<div class="box">
						<div class="box-content logBox">
						<form id="login_form" name ="login_form" role="form" action='/login'>
							<div class="text-left">
								<h2 class="page-header custHeader">Ambari Log Search</h2>
							</div>
							<div class="errorBox">
								<a href="javascript:void(0)" class="close" title="close"><i class="fa fa-times"></i></a>
								<div class="alert alert-danger">
								  	<strong>Error!</strong> Invalid User credentials.<br> Please try again.
								</div>
							</div>
							<div class="form-group">
								<label class="control-label custLabel">Username</label>
								<input type="text" class="form-control custTxtInput" id="username" name="username" required="true" />
							</div>
							<div class="form-group" id="passCont">
								<label class="control-label custLabel">Password</label>
								<input type="password" class="form-control custTxtInput" id="password" name="password" required="true"/>
							</div>
							<div class="text-left">
								<input name="submit" type="submit" class="btn btn-success custLogin" value="Sign In"/>
							</div>
							<div>&nbsp;</div>
							</form>
						</div>
						<div data-id="loader" class="loading"></div>
					</div>
					</div>			
		</div>

		<script type="text/javascript">

		$(window).load(function() {
		      $("#login_form").submit(function(e)
				{
				    var postData = {};
				    var formURL = ($(this).attr("action")) ? $(this).attr("action") : "/login";
				    postData = {"username" : $("#username").val() , "password" : $("#password").val()};
				 
					$.ajax({
						url : formURL,
						type: "POST",
						data : postData,
							success:function(results,status) 
							{
							    window.location = 'index.html'+window.location.search;
							},
							error: function(errorThrown) 
							{
							    showError(errorThrown);
							}
						});				    
				 		return false;
				});

					$('.close').click(function(){
						$('.errorBox').hide();
					});

				function showError(errorThrown){
					var errorMsg = errorThrown.status;
					
				    switch(errorMsg){
		                case 401: $('.errorBox').show(); 
		                        break;
		              
		                default: $('.errorBox').hide();
				    }
				}
		});

		</script>

</body>
</html>
