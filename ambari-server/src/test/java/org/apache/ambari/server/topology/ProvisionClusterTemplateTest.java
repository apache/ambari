package org.apache.ambari.server.topology;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

public class ProvisionClusterTemplateTest {

  public static final String CLUSTER_TEMPLATE = getResource("blueprintv2/cluster_template_v2.json");


  @Test
  public void testProvisionClusterTemplate() throws Exception {
    ProvisionClusterTemplateFactory factory = new ProvisionClusterTemplateFactory();
    ProvisionClusterTemplate template = factory.convertFromJson(CLUSTER_TEMPLATE);
    System.out.println(template);
  }


  private static String getResource(String fileName) {
    try {
      return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
