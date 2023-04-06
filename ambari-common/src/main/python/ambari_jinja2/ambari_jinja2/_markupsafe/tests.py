import gc
import unittest
from ambari_jinja2._markupsafe import Markup, escape, escape_silent


class MarkupTestCase(unittest.TestCase):

    def test_markup_operations(self):
        # adding two strings should escape the unsafe one
        unsafe = '<script type="application/x-some-script">alert("foo");</script>'
        safe = Markup('<em>username</em>')
        assert unsafe + safe == str(escape(unsafe)) + str(safe)

        # string interpolations are safe to use too
        assert Markup('<em>%s</em>') % '<bad user>' == \
               '<em>&lt;bad user&gt;</em>'
        assert Markup('<em>%(username)s</em>') % {
            'username': '<bad user>'
        } == '<em>&lt;bad user&gt;</em>'

        # an escaped object is markup too
        assert type(Markup('foo') + 'bar') is Markup

        # and it implements __html__ by returning itself
        x = Markup("foo")
        assert x.__html__() is x

        # it also knows how to treat __html__ objects
        class Foo(object):
            def __html__(self):
                return '<em>awesome</em>'
            def __unicode__(self):
                return 'awesome'
        assert Markup(Foo()) == '<em>awesome</em>'
        assert Markup('<strong>%s</strong>') % Foo() == \
               '<strong><em>awesome</em></strong>'

        # escaping and unescaping
        assert escape('"<>&\'') == '&#34;&lt;&gt;&amp;&#39;'
        assert Markup("<em>Foo &amp; Bar</em>").striptags() == "Foo & Bar"
        assert Markup("&lt;test&gt;").unescape() == "<test>"

    def test_all_set(self):
        import ambari_jinja2._markupsafe as markup
        for item in markup.__all__:
            getattr(markup, item)

    def test_escape_silent(self):
        assert escape_silent(None) == Markup()
        assert escape(None) == Markup(None)
        assert escape_silent('<foo>') == Markup('&lt;foo&gt;')


class MarkupLeakTestCase(unittest.TestCase):

    def test_markup_leaks(self):
        counts = set()
        for count in range(20):
            for item in range(1000):
                escape("foo")
                escape("<foo>")
                escape("foo")
                escape("<foo>")
            counts.add(len(gc.get_objects()))
        assert len(counts) == 1, 'ouch, c extension seems to leak objects'


def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(MarkupTestCase))

    # this test only tests the c extension
    if not hasattr(escape, 'func_code'):
        suite.addTest(unittest.makeSuite(MarkupLeakTestCase))

    return suite


if __name__ == '__main__':
    unittest.main(defaultTest='suite')
