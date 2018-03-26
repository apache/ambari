package org.apache.ambari.server.controller;

import java.util.List;

import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public interface HostComponentSwagger extends ApiModel {

    @ApiModelProperty(name = HostComponentResourceProvider.HOST_ROLES)
    ServiceComponentHostResponse getHostRole();

    @ApiModelProperty(name = HostComponentResourceProvider.HOST_PROPERTY_ID)
    HostComponentHost getHost();

    @ApiModelProperty(name = HostComponentResourceProvider.METRICS_PROPERTY_ID)
    HostComponentMetrics getMetrics();

    @ApiModelProperty(name = HostComponentResourceProvider.PROCESSES_PROPERTY_ID)
    List<HostComponentProcesses> getProcesses();

    @ApiModelProperty(name = HostComponentResourceProvider.COMPONENT_PROPERTY_ID)
    List<HostComponentComponent> getComponent();


    interface HostComponentHost extends ApiModel {

        @ApiModelProperty(name = HostComponentResourceProvider.HREF_PROPERTY_ID)
        String getHref();
    }

    interface HostComponentMetrics extends ApiModel {


    }

    interface HostComponentProcesses extends ApiModel {


    }


    interface HostComponentComponent extends ApiModel {

        @ApiModelProperty(name = HostComponentResourceProvider.HREF_PROPERTY_ID)
        String getHref();

        @ApiModelProperty(name = HostComponentResourceProvider.SERVICE_COMPONENT_INFO)
        HostComponentServiceComponentInfo getHostComponentServiceComponentInfo();


        interface HostComponentServiceComponentInfo extends ApiModel {

            @ApiModelProperty(name = ComponentResourceProvider.CLUSTER_NAME_PROPERTY_ID)
            String getClusterName();

            @ApiModelProperty(name = ComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID)
            String getComponentName();

            @ApiModelProperty(name = ComponentResourceProvider.SERVICE_NAME_PROPERTY_ID)
            String getServiceName();
        }


    }


}
