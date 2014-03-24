#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""Retrieve the web page.
Either load from a cache directory or from the Internet.

If -H (--hashcode) is specified, load the web page corresponding to
the specified hashcode from the cache.
"""

import sys, os, argparse
from weblib.web import WebpageCache

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--cache-directory', default='web.cache',
                        help='use the specified cache directory')
    parser.add_argument('-H', '--hashcode',
                        help='retrieve using hashcode instead of URL')
    (opts, args) = parser.parse_known_args()
    
    cache = WebpageCache(log=False, dirname=opts.cache_directory)
    if opts.hashcode:
        print cache.read(opts.hashcode, already_hashed=True) or 'ERROR'
    else:
        url = args[0]
        print cache.get_page(url) or 'ERROR'
