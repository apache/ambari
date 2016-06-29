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
<title>bootstrap datepicker examples</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

  <script type="text/javascript" src="js/jquery.min.js"></script>
  <script type="text/javascript" src="js/moment.min.js"></script>
  <script type="text/javascript" src="js/bootstrap.min.js"></script>
  <script type="text/javascript" src="js/bootstrap-datetimepicker.min.js"></script>

  <link rel="stylesheet" href="css/bootstrap.min.css" />
  <link rel="stylesheet" href="css/bootstrap-datetimepicker.min.css" />

<%@ page import="java.sql.*"%>
<%@ page import="org.sqlite.*"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase"%>
<%@ page import="org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase"%>
<%@ page import="javax.servlet.ServletConfig"%>
<%@ page import="javax.servlet.ServletContext"%>
<%@ page import="org.apache.ambari.view.ViewContext"%>
</head>
<%
				int i;
				ArrayList<String> instancename=new ArrayList<String>();          
                Connection c = null;
                Statement stmt = null;
                ServletContext context = request.getServletContext();
                ViewContext view=(ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

                c =  DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"),view.getProperties().get("ambarijdbcurl"),view.getProperties().get("ambaridbusername"),view.getProperties().get("ambaridbpassword")).getConnection();
	
	stmt = c.createStatement();
	ResultSet rs1=null;
		if(view.getProperties().get("ambaridrivername").contains("oracle"))
        		{
        		 rs1 = stmt.executeQuery("select distinct(view_instance_name) as instancename from viewentity");
        		}
        		else
        		{
        		 rs1 = stmt.executeQuery("select distinct(view_instance_name) as instancename from viewentity;");
        		}

	while (rs1.next()) {
		instancename.add(rs1.getString(1));
 
	}
	rs1.close();
	stmt.close();
	c.close();
%>
<div class="row">

	<div class="col-sm-12">
		<form method="GET" onSubmit="validateAndSearch()">
			<div class="panel panel-default">
				<div class="panel-heading">
					<h3>Revert Change</h3>
				</div>
				<div class="panel-body">
					<p></p>
					<p></p>
					<p></p>
					<p></p>
					<div class="row">
						<div class="col-sm-6">
							  &nbsp; &nbsp; Instance name<font size="3" color="red"> *</font>
						</div>
						<div class="col-sm-3">
							<!-- <input type="text" placeholder="Enter Instance Name(*)" name="instance4" id="instance4"> -->
							<select class="form-control" name="instance"
								placeholder="Instance name" id="instance" required>
								<option value="default" selected>Select below</option>

								<%
									for(i=0;i<instancename.size();i++)
																	{
								%><option value="<%=instancename.get(i)%>"><%=instancename.get(i)%></option>
								<%
									}
								%>
								<%
									instancename.clear();
								%>
							</select>
						</div>
					</div>
					<p></p>
					<p></p>
					<p></p>
					<p></p>

					<div class="row">
						<div class="col-sm-6"> &nbsp; &nbsp; Enter the Time Upto which you want to
							Revert</div>


						<div class="container">
                <div class="row">
                    <div class='col-sm-3'>
                        <div class="form-group">
                            <div class='input-group date' id='datetimepicker1'>
                                <input type='text' class="form-control"  id="startdate" name="startdate" />
                                <span class="input-group-addon">
                                    <span class="glyphicon glyphicon-calendar"></span>
                                </span>
                            </div>
                        </div>
                    </div>
                    <script type="text/javascript">
                        $(function () {
                            $('#datetimepicker1').datetimepicker(
                            {format : "YYYY-MM-DD HH:MM:SS"}
                            );
                        });
                    </script>
                </div>
            </div>



					</div>
					<p></p>
					<p></p>
					<p></p>
					<p></p>


					<div class="row">

						<div class="col-sm-3">
							&nbsp; &nbsp;<input type="button" id="submit" class="btn btn-success"
								value="submit" onclick="submittime()">
						</div>
					</div>
					<div id="lines" style="display: none;">

					 <div class="progress" id="progressbar" >
                                            <div id="progressbarhivesavedquery" class="progress-bar" role="progressbar" aria-valuenow="70" aria-valuemin="0" aria-valuemax="100"  style="width:0%">

                                            </div>

				</div>
			</div>
		</form>

	</div>
</div>

<script type="text/javascript">
	function submittime() {
	var strDatetime = $("#startdate").val();
	var instance = document.getElementById("instance");
   instance= instance.options[instance.selectedIndex].value;
		
		$('#progressbar').show();
	    $('#lines').hide();
		revertingchange(strDatetime,instance);
		interval = setInterval(loadpercentage, 1000 );

	}

	function revertingchange(revertdate,instance) {
		//alert("hello");
		
		var url = "RevertChange?revertdate="+revertdate+"&instance="+instance;
		
		$.ajax({url: url, success: function(result){
			console.log("Got Result");
			document.getElementById("lines").innerHTML = result;
			$('#progressbar').hide()
			$('#lines').show()
			clearInterval(interval);
   		 }});
	}
	function loadpercentage() {
    	$.ajax({
        url : "ProgressBarStatus",
        success : function(result) {
        $('#progressbarhivesavedquery').css('width', result);
        console.log("Got the precentage completion "+ result);
        },
      });
  }
</script>