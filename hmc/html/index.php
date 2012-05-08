<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body>
    <?php require "./header.html"; ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId" class="yui3-skin-sam"> 

    <!--
      <!-- Navigation bar -->
      <!--
      <div name="navigationBarDiv" id="navigationBarDivId">
        <div name="navigationBarContentDiv" id="navigationBarContentDivId">
        Clusters
        </div>
      <hr/>
      </div>
      -->
      <!-- List of clusters -->
      <div name="clustersListDiv" id="clustersListDivId">
      </div>

        <br/>
        
        <!--
        <div name="installationSideBarDiv" id="installationSideBarDivId">
          <div id="createClusterSideBarId">Create cluster</div>
          <div id="selectServicesSideBarId">Select services</div>
          <div id="masterHostsSideBarId">Select master hosts</div>
          <div id="mountPointsSideBarId">Select mount points</div>
          <div id="advancedConfigSideBarId">Advanced configuration</div>
          <div id="reviewSettingsSideBarId">Review your settings</div>
        </div>
        -->

            <script src="http://yui.yahooapis.com/3.4.1/build/yui/yui-min.js"></script>
            <script src="../js/utils.js"></script>
            <script src="../js/clustersList.js"></script>

          </body>
          <hr/>
          <?php require "./footer.html"; ?>
        </html> 
