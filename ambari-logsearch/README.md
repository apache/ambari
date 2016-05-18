# logsearch
RPM/DPKG Build Process
=============

1. Check out the code from GIT repository

2. On the logsearch root folder, please execute the following Maven command to build RPM/DPKG:

  $ mvn -Dbuild-rpm clean package

  or

  $ mvn -Dbuild-deb clean package

3. Generated RPM/DPKG files will be found in ambari-logsearch-assembly/target folder
  
