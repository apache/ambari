<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/3.4.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
  </head>

  <body>
    <header>
    <img src="./logo.jpg"/>
    <section id="headerText">Hortonworks Data Platform</section>
    </header>
    <hr/>

    <div name="ContentDiv" id="ContentDivId" >

<!--
<div name="servicesListDiv" id="servicesListDivId">
  <div>Service Name</div>
  <div>Status</div>
</div>
-->

                <div name="displayLogsCoreDiv" id="displayLogsCoreDivId" style="display:none">
                  <div name="displayLogsContentDiv" id="displayLogsContentDivId"> 
                  </div>
                  <div name="installDoneDiv" id="installDoneDivId" style="display:none"> 
                    <p>
                    <input type="button" name="installDoneButton" id="installDoneButtonId" value="Install Completed" class="submitButton">
                    </p>
                  </div>
                </div>


<!--
      <div name="displayLogsDiv" id="displayLogsDivId">
        <p><b>Checking status for all services</b></p>
      </div>
-->
    </div> <!-- end content div -->

      <script src="http://yui.yahooapis.com/3.4.1/build/yui/yui-min.js"></script>
      <script src="../js/displayLogs.js"></script>

      <script type="text/javascript">
         YUI().use("event", function (Y) { 
           getGsInstallLogs(Y, "MyCluster-12345");
         });
      </script>
  </body>
  <hr/>
  <footer></footer>
</html>

