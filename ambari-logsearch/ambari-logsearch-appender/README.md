


# Ambari Logsearch Appender

Ambari Logsearch Appender is a log4j base appender that write logs in json format.



## Setup Ambari Logsearch Appender

#### Add dependency 
```xml
    	<dependency>
    	  <groupId>org.apache.ambari</groupId>
    	  <artifactId>ambari-logsearch-appender</artifactId>
    	  <version>${version}</version>
    	</dependency>
```
####Dependent dependency 
```xml   
		 <dependency>
		  <groupId>log4j</groupId>
		  <artifactId>log4j</artifactId>
		  <version>1.2.17</version>
		</dependency>
		<dependency>
		  <groupId>com.google.code.gson</groupId>
		  <artifactId>gson</artifactId>
		  <version>2.6.2</version>
		</dependency>
```

## Configuration
####  Sample Configuration for log4j.properties
```java
log4j.appender.logsearchJson=org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender
log4j.appender.logsearchJson.File=path/file_name.json
log4j.appender.logsearchJson.maxFileSize=10MB
log4j.appender.logsearchJson.maxBackupIndex=10
log4j.appender.logsearchJson.Append=true
log4j.appender.logsearchJson.layout=org.apache.ambari.logsearch.appender.LogsearchConversion
```
###                                OR
#### Sample Configuration for log4j.xml
```xml
<appender name="logsearchJson"
    class="org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender">
    <param name="file" value="path/file_name.json" />
		<param name="append" value="true" />
		<param name="maxFileSize" value="10MB" />
		<param name="maxBackupIndex" value="10" />
    <layout class="org.apache.ambari.logsearch.appender.LogsearchConversion" />
</appender> 
```


