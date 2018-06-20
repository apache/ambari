package org.apache.ambari.server.checks;

import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.ClusterCheck.CheckQualification;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * The {@link PreUpgradeCheck} is used before an upgrade in order to present the
 * administrator with a warning or a failure about an upgrade.
 */
public interface PreUpgradeCheck {

  /**
   * Gets the set of services that this check is associated with. If the check
   * is not associated with a particular service, then this should be an empty
   * set.
   *
   * @return a set of services which will determine whether this check is
   *         applicable, or an empty set.
   */
  Set<String> getApplicableServices();

  /**
   * Gets any additional qualifications which an upgrade check should run in
   * order to determine if it's applicable to the upgrade.
   *
   * @return a list of qualifications, or an empty list.
   */
  List<CheckQualification> getQualifications();

  /**
   * Tests if the prerequisite check is applicable to given upgrade request.
   *
   * @param request
   *          prerequisite check request
   * @return true if check should be performed
   * @throws AmbariException
   */
  boolean isApplicable(PrereqCheckRequest request) throws AmbariException;

  /**
   * Executes check against given cluster.
   * @param request pre upgrade check request
   * @return TODO
   *
   * @throws AmbariException if server error happens
   */
  UpgradeCheckResult perform(PrereqCheckRequest request)
      throws AmbariException;

  /**
   * Gets the type of check.
   *
   * @return the type of check (not {@code null}).
   */
  PrereqCheckType getType();

  /**
   * Gets whether this upgrade check is required for the specified
   * {@link UpgradeType}. Checks which are marked as required do not need to be
   * explicitely declared in the {@link UpgradePack} to be run.
   *
   * @return {@code true} if it is required, {@code false} otherwise.
   */
  boolean isRequired(UpgradeType upgradeType);

  /**
   * The {@link CheckDescription} which includes the name, description, and
   * success/failure messages for a {@link PreUpgradeCheck}.
   *
   * @return the check description.
   */
  CheckDescription getCheckDescrption();
}