from unittest import TestCase
from ambari_commons.kerberos.kerberos_common import resolve_encryption_family_list, resolve_encryption_families

class TestEncryptionTypes(TestCase):

  def test_resolves_family(self):
    expected = set([
      'aes256-cts-hmac-sha1-96',
      'aes128-cts-hmac-sha1-96',
      'aes256-cts-hmac-sha384-192',
      'aes128-cts-hmac-sha256-128',
      'rc4-hmac'])
    self.assertEquals(expected, resolve_encryption_family_list(['rc4', 'aes']))

  def test_no_resolve_if_no_family_is_given(self):
    expected = set(['aes256-cts-hmac-sha1-96', 'rc4-hmac'])
    self.assertEquals(expected, resolve_encryption_family_list(['rc4-hmac', 'aes256-cts-hmac-sha1-96']))

  def test_eliminates_duplications(self):
    expected = set([
      'aes256-cts-hmac-sha1-96',
      'aes128-cts-hmac-sha1-96',
      'aes256-cts-hmac-sha384-192',
      'aes128-cts-hmac-sha256-128'])
    self.assertEquals(expected, resolve_encryption_family_list(['aes', 'aes128-cts-hmac-sha1-96']))

  def test_translate_str(self):
    self.assertEquals('rc4-hmac', resolve_encryption_families('rc4'))