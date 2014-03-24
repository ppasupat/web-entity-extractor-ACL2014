#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""Fake a Google search result.
Load a Google search cache from fake-google-search.cache
Useful for getting a Google search result for a particular data set in the past.

Usage:
  python fake-google-search.py [QUERY]
Return (in standard output):
  A JSON-encoded result of the form [{"link": ..., "title": ...}, ...]
"""

import sys, os, urllib

def get_cache_filename(query):
    cache_path = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                             'fake-google-search.cache')
    key = urllib.quote_plus(query)
    return os.path.join(cache_path, key + '.json')

if __name__ == '__main__':
    query = ' '.join(sys.argv[1:])
    cache_filename = get_cache_filename(query)
    try:
        with open(cache_filename) as fin:
            result = fin.read()
    except IOError:
        result = ''
    print result
