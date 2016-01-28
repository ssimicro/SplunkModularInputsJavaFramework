import cgi
from datetime import datetime
import fnmatch
import logging
import math
import pipes
import os
import re
import socket
import subprocess
import sys
import time
import json
import urllib

import cherrypy
import splunk
import splunk.util

import xml.sax.saxutils as su
import splunk.appserver.mrsparkle.controllers as controllers
import splunk.appserver.mrsparkle.lib.util as util
from splunk.appserver.mrsparkle.lib.util import make_url

import splunk.entity as splunkEntity

from splunk.appserver.mrsparkle.lib.decorators import expose_page, set_cache_level
from splunk.appserver.mrsparkle.lib.routes import route
from splunk.appserver.mrsparkle.lib.util import make_splunkhome_path


mapping_file_path = make_splunkhome_path(["etc", "apps", "alexa", "intents", "mapping.json"])


class MainController(controllers.BaseController):
    def read_file(self):
        with open(mapping_file_path) as f:
            content = f.read()
            try:
                content_json = json.loads(content)
                mappings = content_json['intents']

            except ValueError:
                return self.generateError(500, su.escape("mappings.json is corrupted"))

        return mappings


    @route('/:action=skills/')
    @expose_page(must_login=True, methods=['GET'])
    def read_settings(self, **args):
        mappings = self.read_file()

        response = []
        for key, value in mappings.iteritems():
            clone_value = dict(value)
            clone_value['id'] = key
            response.append(clone_value)

        return self.render_json(response)


    # @route('/:action=skills/:intent')
    # @expose_page(must_login=True, methods=['POST'])
    # def edit(self, intent, **kwargs):
    #     mappings = self.read_file()
    #     intents = [mapping.get('intent') for mapping in mappings]
    #     if intent not in intents:
    #         pass
    #
    #     return self.render_json({"apps": 'test'})