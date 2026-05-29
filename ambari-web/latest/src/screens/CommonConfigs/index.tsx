/**
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
 */

import { useContext, useEffect, useState } from "react";
import ConfigsApi from "../../api/configsApi";
import Spinner from "../../components/Spinner";
import Config from "./Config";
import { ConfigPropertiesType } from "./types";
import { get, isEmpty } from "lodash";
import { AppContext } from "../../store/context";

export default function CommonConfig() {
  const [themes, setThemes] = useState<any>({});
  const [configs, setConfigs] = useState<any>({});
  // const [propertiesValues, setPropertiesValues] = useState<any>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [configProperties, setConfigProperties] = useState({});
  const [configsLoading, setConfigsLoading] = useState(false);
  const { cluster } = useContext(AppContext);
  const stackName = get(cluster, "version", "").split("-")[0];
  const stackVersion = get(cluster, "version", "").split("-")[1];

  const services = [
    "HDFS",
    "YARN",
    "MAPREDUCE2",
    "TEZ",
    "HIVE",
    "HBASE",
    "ZOOKEEPER",
    "AMBARI_METRICS",
    "RANGER",
    "RANGER_KMS",
    "SPARK3",
    "SSM",
    "TRINO",
    "KERBEROS",
  ];

  useEffect(() => {
    getConfigurations();
    getThemes();
  }, []);

  const getThemes = async () => {
    setLoading(true);
    const response = await ConfigsApi.getTheme(
      stackName,
      stackVersion,
      services.join(",")
    );
    setThemes(response);
    setLoading(false);
  };

  const getConfigProperties = async () => {
    setConfigsLoading(true);
    let configPropertiesCopy: ConfigPropertiesType = {};
    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.split(".")[0];
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;

        if (!configPropertiesCopy[serviceName]) {
          configPropertiesCopy[serviceName] = {};
        }
        if (!configPropertiesCopy[serviceName][configType]) {
          configPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        configPropertiesCopy[serviceName][configType].properties[propertyName] =
          {
            propertyName: propertyName,
            ...(config.StackConfigurations.property_display_name && {
              propertyDisplayname:
                config.StackConfigurations.property_display_name,
            }),
            propertyValue: config.StackConfigurations.property_value,
            propertyAttributes:
              config.StackConfigurations.property_value_attributes,
            previousValue: config.StackConfigurations.property_value,
            description: config.StackConfigurations.property_description,
            propertyDescription: config.StackConfigurations.property_description,
            value: config.StackConfigurations.property_value,
            final: config.StackConfigurations.final
              ? config.StackConfigurations.final
              : "",
          };

        if (
          configPropertiesCopy[serviceName][configType].properties[propertyName]
            .propertyAttributes.type == "password"
        ) {
          configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ] = {
            ...configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ],
            confirmPassword: config.StackConfigurations.property_value,
          };
        }
      });
    });

    // if (propertyValues) {
    //   propertyValues.items.forEach((item: any) => {
    //     item.configurations.forEach((config: any) => {
    //       const type = config.type;
    //       const properties = config.properties;
    //       const serviceName = config.service_name;

    //       Object.keys(properties).forEach((propertyName: string) => {
    //         if (configPropertiesCopy[serviceName][type]) {
    //           if (
    //             configPropertiesCopy[serviceName][type].properties[propertyName]
    //           ) {
    //             configPropertiesCopy[serviceName][type].properties[
    //               propertyName
    //             ].value = properties[propertyName];
    //             configPropertiesCopy[serviceName][type].properties[
    //               propertyName
    //             ].previousValue = properties[propertyName];
    //           }
    //         }
    //       });
    //     });
    //   });
    // }

    setConfigProperties(configPropertiesCopy);
    setConfigsLoading(false);
  };

  const getConfigurations = async () => {
    setLoading(true);
    const response = await ConfigsApi.getServiceConfigurations(
      stackName,
      stackVersion,
      services.join(",")
    );
    setConfigs(response);
    setLoading(false);
  };

  useEffect(() => {
    if (!isEmpty(configs) && isEmpty(configProperties)) {
      getConfigProperties();
    }
  }, [configs]);

  if (loading) {
    return <Spinner />;
  }
  return (
    <div>
      <Config
        configSection="default"
        configProperties={configProperties}
        setConfigProperties={setConfigProperties}
        themeData={themes}
        configPropertiesData={configs}
        servicesList={services}
        configsLoading={configsLoading}
      />
    </div>
  );
}
