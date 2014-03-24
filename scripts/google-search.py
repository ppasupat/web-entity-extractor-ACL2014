#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""Search Google.

For only a few searches, the default free search should work OK.

If a lot of searches are needed (100+), consider using Google Custom Search.
Simply put Google API Key and CX Key in the variables below.

Print only the first search result.
"""

import urllib, os, sys, json
from weblib.web import WebpageCache

# Google Custom Search
GOOGLE_APIKEY = ''
GOOGLE_CX = ''
CACHE_DIRNAME = 'google.cache'

if __name__ == '__main__':
    query = ' '.join(sys.argv[1:])
    cache = WebpageCache(log=False, dirname=CACHE_DIRNAME)
    if GOOGLE_APIKEY and GOOGLE_CX:
        cache.set_google_custom_search_keys(GOOGLE_APIKEY, GOOGLE_CX)
        results = cache.get_urls_from_google_custom_search(query)
    else:
        results = cache.get_urls_from_google_search(query)
    print json.dumps(results)
