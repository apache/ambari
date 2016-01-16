#!/usr/bin/env ambari-python-wrap
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""


class HDP24StackAdvisor(HDP23StackAdvisor):
  def getServiceConfigurationRecommenderDict(self):
    parent_recommend_conf_dict = super(HDP24StackAdvisor, self).getServiceConfigurationRecommenderDict()
    child_recommend_conf_dict = {
      "YARN": self.recommendYARNConfigurations
    }
    parent_recommend_conf_dict.update(child_recommend_conf_dict)
    return parent_recommend_conf_dict

  def getServiceConfigurationValidators(self):
    parentValidators = super(HDP24StackAdvisor, self).getServiceConfigurationValidators()
    childValidators = {
      "YARN": {"yarn-site": self.validateYARNConfigurations}
    }
    self.mergeValidators(parentValidators, childValidators)
    return parentValidators

  def recommendYARNConfigurations(self, configurations, clusterData, services, hosts):
    super(HDP24StackAdvisor, self).recommendYARNConfigurations(configurations, clusterData, services, hosts)

    yarn_site_config = "yarn-site"
    properties = services["configurations"] if yarn_site_config in services["configurations"] else []
    yarn_site_properties = properties[yarn_site_config]["properties"] if yarn_site_config in properties and \
                                                                         "properties" in properties[yarn_site_config] else []
    put_yarn_site_property = self.putProperty(configurations, yarn_site_config, services)
    put_yarn_site_property_attributes = self.putPropertyAttribute(configurations, yarn_site_config)
    services_list = [service["StackServices"]["service_name"] for service in services["services"]]

    if 'SPARK' in services_list:
      if "yarn.nodemanager.aux-services" in yarn_site_properties:
        aux_services = yarn_site_properties["yarn.nodemanager.aux-services"].split(",")
        if "spark_shuffle" not in aux_services:
          aux_services.append("spark_shuffle")
        put_yarn_site_property("yarn.nodemanager.aux-services", ",".join(aux_services))
      else:
        put_yarn_site_property("yarn.nodemanager.aux-services", "spark_shuffle")

      put_yarn_site_property("yarn.nodemanager.aux-services.spark_shuffle.class",
                             "org.apache.spark.network.yarn.YarnShuffleService")
    else:
      put_yarn_site_property_attributes("yarn.nodemanager.aux-services.spark_shuffle.class", "delete", "true")

  def validateYARNConfigurations(self, properties, recommendedDefaults, configurations, services, hosts):
    yarn_site = properties
    validationItems = []
    services_list = [service["StackServices"]["service_name"] for service in services["services"]]

    if "SPARK" in services_list and 'YARN' in services_list:
      # yarn.nodemanager.aux-services = ...,spark_shuffle,....
      # yarn.nodemanager.aux-services.spark_shuffle.class = <not set>
      if "yarn.nodemanager.aux-services" in yarn_site \
        and "spark_shuffle" in yarn_site["yarn.nodemanager.aux-services"].lower() \
        and "yarn.nodemanager.aux-services.spark_shuffle.class" not in yarn_site:
        validationItems.append({
          "config-name": "yarn.nodemanager.aux-services.spark_shuffle.class",
          "item": self.getErrorItem("If spark_shuffle is listed in the aux-services, property value for " +
                                    "yarn.nodemanager.aux-services.spark_shuffle.class need to be set")
        })

      # yarn.nodemanager.aux-services = <not set>
      # yarn.nodemanager.aux-services.spark_shuffle.class = is set
      spark_aux_service_warning = False
      if "yarn.nodemanager.aux-services" in yarn_site and "spark_shuffle" not in yarn_site[
        "yarn.nodemanager.aux-services"].lower() \
        and "yarn.nodemanager.aux-services.spark_shuffle.class" in yarn_site:
        spark_aux_service_warning = True

      if "yarn.nodemanager.aux-services" not in yarn_site and "yarn.nodemanager.aux-services.spark_shuffle.class" in yarn_site:
        spark_aux_service_warning = True

      if spark_aux_service_warning:
        validationItems.append({
          "config-name": "yarn.nodemanager.aux-services",
          "item": self.getWarnItem("If yarn.nodemanager.aux-services.spark_shuffle.class is set, probably " +
                                   "aux-services property need to be updated to enable spark_shuffle")
        })

    return self.toConfigurationValidationProblems(validationItems, "yarn-site")
