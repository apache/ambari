package org.apache.ambari.server.state.live;

/**
 * Information about a mounted disk on a given node
 */
public class DiskInfo {

  /**
   * Name of device
   * For example: /dev/sda, /dev/sdb, etc
   */
  String device;

  /**
   * Filesystem Type
   * For example: ext3, tmpfs, swap, etc
   */
  String fsType;

  /**
   * Path at which the device is mounted on
   */
  String mountPath;

  /**
   * Capacity of the disk in bytes
   */
  long totalCapacityBytes;
  
  /**
   * Current capacity in bytes
   */
  long currentCapacityBytes;

}
