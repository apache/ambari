# -*- coding: utf-8 -*-
"""
    markupsafe
    ~~~~~~~~~~

    Implements a Markup string.

    :copyright: (c) 2010 by Armin Ronacher.
    :license: BSD, see LICENSE for more details.
"""
import re



__all__ = ['Markup', 'soft_unicode', 'escape', 'escape_silent']


_striptags_re = re.compile(r'(<!--.*?-->|<[^>]*>)')
_entity_re = re.compile(r'&([^;]+);')


class Markup(str):
    r"""Marks a string as being safe for inclusion in HTML/XML output without
    needing to be escaped.  This implements the `__html__` interface a couple
    of frameworks and web applications use.  :class:`Markup` is a direct
    subclass of `unicode` and provides all the methods of `unicode` just that
    it escapes arguments passed and always returns `Markup`.

    The `escape` function returns markup objects so that double escaping can't
    happen.

    The constructor of the :class:`Markup` class can be used for three
    different things:  When passed an unicode object it's assumed to be safe,
    when passed an object with an HTML representation (has an `__html__`
    method) that representation is used, otherwise the object passed is
    converted into a unicode string and then assumed to be safe:

    >>> Markup("Hello <em>World</em>!")
    Markup(u'Hello <em>World</em>!')
    >>> class Foo(object):
    ...  def __html__(self):
    ...   return '<a href="#">foo</a>'
    ... 
    >>> Markup(Foo())
    Markup(u'<a href="#">foo</a>')

    If you want object passed being always treated as unsafe you can use the
    :meth:`escape` classmethod to create a :class:`Markup` object:

    >>> Markup.escape("Hello <em>World</em>!")
    Markup(u'Hello &lt;em&gt;World&lt;/em&gt;!')

    Operations on a markup string are markup aware which means that all
    arguments are passed through the :func:`escape` function:

    >>> em = Markup("<em>%s</em>")
    >>> em % "foo & bar"
    Markup(u'<em>foo &amp; bar</em>')
    >>> strong = Markup("<strong>%(text)s</strong>")
    >>> strong % {'text': '<blink>hacker here</blink>'}
    Markup(u'<strong>&lt;blink&gt;hacker here&lt;/blink&gt;</strong>')
    >>> Markup("<em>Hello</em> ") + "<foo>"
    Markup(u'<em>Hello</em> &lt;foo&gt;')
    """
    __slots__ = ()

    def __new__(cls, base='', encoding=None, errors='strict'):
        if hasattr(base, '__html__'):
            base = base.__html__()
        if encoding is None:
            return str.__new__(cls, base)
        return str.__new__(cls, base, encoding, errors)

    def __html__(self):
        return self

    def __add__(self, other):
        if hasattr(other, '__html__') or isinstance(other, str):
            return self.__class__(str(self) + str(escape(other)))
        return NotImplemented

    def __radd__(self, other):
        if hasattr(other, '__html__') or isinstance(other, str):
            return self.__class__(str(escape(other)) + str(self))
        return NotImplemented

    def __mul__(self, num):
        if isinstance(num, int):
            return self.__class__(str.__mul__(self, num))
        return NotImplemented
    __rmul__ = __mul__

    def __mod__(self, arg):
        if isinstance(arg, tuple):
            arg = tuple(map(_MarkupEscapeHelper, arg))
        else:
            arg = _MarkupEscapeHelper(arg)
        return self.__class__(str.__mod__(self, arg))

    def __repr__(self):
        return '%s(%s)' % (
            self.__class__.__name__,
            str.__repr__(self)
        )

    def join(self, seq):
        return self.__class__(str.join(self, list(map(escape, seq))))
    join.__doc__ = str.join.__doc__

    def split(self, *args, **kwargs):
        return list(map(self.__class__, str.split(self, *args, **kwargs)))
    split.__doc__ = str.split.__doc__

    def rsplit(self, *args, **kwargs):
        return list(map(self.__class__, str.rsplit(self, *args, **kwargs)))
    rsplit.__doc__ = str.rsplit.__doc__

    def splitlines(self, *args, **kwargs):
        return list(map(self.__class__, str.splitlines(self, *args, **kwargs)))
    splitlines.__doc__ = str.splitlines.__doc__

    def unescape(self):
        r"""Unescape markup again into an unicode string.  This also resolves
        known HTML4 and XHTML entities:

        >>> Markup("Main &raquo; <em>About</em>").unescape()
        u'Main \xbb <em>About</em>'
        """
        from jinja2._markupsafe._constants import HTML_ENTITIES
        def handle_match(m):
            name = m.group(1)
            if name in HTML_ENTITIES:
                return chr(HTML_ENTITIES[name])
            try:
                if name[:2] in ('#x', '#X'):
                    return chr(int(name[2:], 16))
                elif name.startswith('#'):
                    return chr(int(name[1:]))
            except ValueError:
                pass
            return ''
        return _entity_re.sub(handle_match, str(self))

    def striptags(self):
        r"""Unescape markup into an unicode string and strip all tags.  This
        also resolves known HTML4 and XHTML entities.  Whitespace is
        normalized to one:

        >>> Markup("Main &raquo;  <em>About</em>").striptags()
        u'Main \xbb About'
        """
        stripped = ' '.join(_striptags_re.sub('', self).split())
        return Markup(stripped).unescape()

    @classmethod
    def escape(cls, s):
        """Escape the string.  Works like :func:`escape` with the difference
        that for subclasses of :class:`Markup` this function would return the
        correct subclass.
        """
        rv = escape(s)
        if rv.__class__ is not cls:
            return cls(rv)
        return rv

    def make_wrapper(name):
        orig = getattr(str, name)
        def func(self, *args, **kwargs):
            args = _escape_argspec(list(args), enumerate(args))
            _escape_argspec(kwargs, iter(list(kwargs.items())))
            return self.__class__(orig(self, *args, **kwargs))
        func.__name__ = orig.__name__
        func.__doc__ = orig.__doc__
        return func

    for method in '__getitem__', 'capitalize', \
                  'title', 'lower', 'upper', 'replace', 'ljust', \
                  'rjust', 'lstrip', 'rstrip', 'center', 'strip', \
                  'translate', 'expandtabs', 'swapcase', 'zfill':
        locals()[method] = make_wrapper(method)

    # new in python 2.5
    if hasattr(str, 'partition'):
        partition = make_wrapper('partition'),
        rpartition = make_wrapper('rpartition')

    # new in python 2.6
    if hasattr(str, 'format'):
        format = make_wrapper('format')

    # not in python 3
    if hasattr(str, '__getslice__'):
        __getslice__ = make_wrapper('__getslice__')

    del method, make_wrapper


def _escape_argspec(obj, iterable):
    """Helper for various string-wrapped functions."""
    for key, value in iterable:
        if hasattr(value, '__html__') or isinstance(value, str):
            obj[key] = escape(value)
    return obj


class _MarkupEscapeHelper(object):
    """Helper for Markup.__mod__"""

    def __init__(self, obj):
        self.obj = obj

    __getitem__ = lambda s, x: _MarkupEscapeHelper(s.obj[x])
    __str__ = lambda s: str(escape(s.obj))
    __unicode__ = lambda s: str(escape(s.obj))
    __repr__ = lambda s: str(escape(repr(s.obj)))
    __int__ = lambda s: int(s.obj)
    __float__ = lambda s: float(s.obj)


# we have to import it down here as the speedups and native
# modules imports the markup type which is define above.
try:
    from jinja2._markupsafe._speedups import escape, escape_silent, soft_unicode
except ImportError:
    from jinja2._markupsafe._native import escape, escape_silent, soft_unicode
