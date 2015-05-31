/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/modification_handlers/modification_handler');

module.exports = App.ServiceConfigModificationHandler.create({
  serviceId: 'KAFKA',

  getDependentConfigChanges: function (changedConfig, selectedServices, allConfigs) {
    var rangerPluginEnabledName = "ranger-kafka-plugin-enabled";
    var affectedProperties = [];
    var affectedPropertyName = changedConfig.get("name");
    var authorizerClassName, kafkaLog4jContent, newLog4jContentValue;
    var isEnabling = changedConfig.get('value') === 'Yes';

    if (affectedPropertyName === rangerPluginEnabledName) {
      authorizerClassName = this.getConfig(allConfigs, 'authorizer.class.name', 'kafka-broker.xml', 'KAFKA');
      kafkaLog4jContent = this.getConfig(allConfigs, 'content', 'kafka-log4j.xml', 'KAFKA');
      newLog4jContentValue = kafkaLog4jContent.get('value');
      newLog4jContentValue += "\n\nlog4j.appender.rangerAppender=org.apache.log4j.DailyRollingFileAppender\n" +
      "log4j.appender.rangerAppender.DatePattern='.'yyyy-MM-dd-HH\n" +
      "log4j.appender.rangerAppender.File=${kafka.logs.dir}/ranger_kafka.log\n" +
      "log4j.appender.rangerAppender.layout=org.apache.log4j.PatternLayout\n" +
      "log4j.appender.rangerAppender.layout.ConversionPattern=%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n\n" +
      "log4j.logger.org.apache.ranger=INFO, rangerAppender";

      affectedProperties = [
        {
          serviceName: "KAFKA",
          sourceServiceName: "KAFKA",
          propertyName: 'authorizer.class.name',
          propertyDisplayName: 'authorizer.class.name',
          newValue: isEnabling ? 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer' :
              App.StackConfigProperty.find().findProperty('name', 'authorizer.class.name').get('value'),
          curValue: authorizerClassName.get('value'),
          changedPropertyName: rangerPluginEnabledName,
          removed: false,
          filename: 'kafka-broker.xml'
        },
        {
          serviceName: "KAFKA",
          sourceServiceName: "KAFKA",
          propertyName: 'content',
          propertyDisplayName: 'content',
          newValue: isEnabling ? newLog4jContentValue : App.StackConfigProperty.find().filterProperty('filename', 'kafka-log4j.xml').findProperty('name', 'content').get('value'),
          curValue: kafkaLog4jContent.get('value'),
          changedPropertyName: rangerPluginEnabledName,
          removed: false,
          filename: 'kafka-log4j.xml'
        }
      ];
    }

    return affectedProperties;
  }
});
