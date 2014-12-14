package org.apache.ambari.server.serveraction.kerberos;

import junit.framework.Assert;
import org.junit.Test;

public class KDCTypeTest {

  @Test
  public void testTranslateExact() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("MIT_KDC"));
  }

  @Test
  public void testTranslateCaseInsensitive() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("mit_kdc"));
  }

  @Test
  public void testTranslateHyphen() {
    Assert.assertEquals(KDCType.MIT_KDC, KDCType.translate("MIT-KDC"));
  }

  @Test(expected = NullPointerException.class)
  public void testTranslateNull() {
    KDCType.translate(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTranslateEmptyString() {
    KDCType.translate("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTranslateNoTranslation() {
    KDCType.translate("NO TRANSLATION");
  }
}