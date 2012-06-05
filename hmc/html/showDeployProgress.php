<?php require_once "./head.inc" ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.5.1/build/cssreset/cssreset-min.css">
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="contentDivId"> 

      <?php require "./utils.htmli"; ?>
      <?php require "./txnUtils.htmli"; ?>

    </div>
    <!-- End of contentDivId -->

    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap clusters.js. */
      var clusterName = '<?php echo $_GET['clusterName']; ?>';

      var jsFilesToLoad = [ 
        '../js/utils.js',
        '../js/txnUtils.js',
        '../js/deployProgress.js',
        '../js/showDeployProgress.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->
  </body>
</html> 
