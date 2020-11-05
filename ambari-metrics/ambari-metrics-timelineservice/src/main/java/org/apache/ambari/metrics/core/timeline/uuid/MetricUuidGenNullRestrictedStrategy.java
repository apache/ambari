package org.apache.ambari.metrics.core.timeline.uuid;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HBase represents null value for BINARY fields as set of zero bytes. This means that we are not able to difference
 * byte[]{0,0,0,0} and null values in DB. So we should not generate Uuids which contains only zero bytes.
 */
public abstract class MetricUuidGenNullRestrictedStrategy implements MetricUuidGenStrategy {
  private static final Log LOG = LogFactory.getLog(MetricUuidGenNullRestrictedStrategy.class);

  static final int RETRY_NUMBER = 5;

  @Override
  public byte[] computeUuid(TimelineClusterMetric timelineClusterMetric, int maxLength) {
    int retry_attempt = 0;
    byte[] result = null;
    while (retry_attempt++ < RETRY_NUMBER) {
      result = computeUuidInternal(timelineClusterMetric, maxLength);
      for (byte b : result) {
        if (b != 0) {
          return result;
        }
      }
      LOG.debug("Was generated Uuid which can represent null value in DB.");
    }

    LOG.error(String.format("After %n attempts was generated Uuid which can represent null value in DB", RETRY_NUMBER));
    return result;
  }

  @Override
  public byte[] computeUuid(String value, int maxLength) {
    int retry_attempt = 0;
    byte[] result = null;
    while (retry_attempt++ < RETRY_NUMBER) {
      result = computeUuidInternal(value, maxLength);
      for (byte b : result) {
        if (b != 0) {
          return result;
        }
      }
      LOG.debug("Was generated Uuid which can represent null value in DB.");
    }

    LOG.error(String.format("After %n attempts was generated Uuid which can represent null value in DB", RETRY_NUMBER));
    return result;
  }

  abstract byte[] computeUuidInternal(TimelineClusterMetric timelineClusterMetric, int maxLength);
  abstract byte[] computeUuidInternal(String value, int maxLength);
}
