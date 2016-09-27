package org.apache.ambari.server.sample.checks;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.common.collect.ImmutableMap;

public class SampleServiceCheck extends AbstractCheckDescriptor {

  public SampleServiceCheck() {
    super(new CheckDescription("SAMPLE_SERVICE_CHECK",
          PrereqCheckType.HOST,
          "Sample service check description.",
          new ImmutableMap.Builder<String, String>()
                          .put(AbstractCheckDescriptor.DEFAULT,
                              "Sample service check default property description.").build()));
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    prerequisiteCheck.setFailReason("Sample service check always fails.");
    prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
  }

  @Override
  public boolean isStackUpgradeAllowedToBypassPreChecks() {
    return false;
  }

}
