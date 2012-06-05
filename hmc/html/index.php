<?php require_once "./head.inc" ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="ContentDivId">

      <?php require "./utils.htmli"; ?>

      <div id="welcomeDivId" style="display:none">
        <?php require "./welcome.inc" ?>           
      </div>
      <!-- List of clusters -->
      <div id="clustersListDivId" style="display:none">
      </div>

      <div id="clusterHostRoleMappingDynamicRenderDivId">
      </div>

    </div>        
    <!-- End of contentDivId -->
    
    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

      var jsFilesToLoad = [ 
        '../js/utils.js',
        '../js/clustersList.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->
  </body>
</html> 
