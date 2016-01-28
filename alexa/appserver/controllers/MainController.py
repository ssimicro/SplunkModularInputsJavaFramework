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


DBX2_FORMAT = '%(asctime)s [%(levelname)s] [%(filename)s] %(message)s'

mapping_file_path = make_splunkhome_path(["etc", "apps", "alexa", "intents", "mapping.json"])


logger = logging.getLogger('alexa')
logger.setLevel(logging.ERROR)


def create_logger_handler(fd, level, maxBytes=10240000, backupCount=5):
    handler = logging.handlers.RotatingFileHandler(fd, maxBytes=maxBytes, backupCount=backupCount)
    handler.setFormatter(logging.Formatter(DBX2_FORMAT))
    handler.setLevel(level)
    return handler


LOG_FILENAME = os.path.join(os.environ.get('SPLUNK_HOME'), 'var','log','splunk','alexa.log')
handler = create_logger_handler(LOG_FILENAME, logging.ERROR)
logger.addHandler(handler)



class MainController(controllers.BaseController):
    def read_file(self):
        with open(mapping_file_path) as f:
            content = f.read()
            try:
                content_json = json.loads(content)
                mappings = content_json['mappings']

            except ValueError:
                return self.generateError(500, su.escape("mappings.json is corrupted"))

        return mappings

    def save_file(self, mappings):
        content = {
            'mappings': mappings
        }

        with open(mapping_file_path, 'w') as f:
            f.write(json.dumps(content, indent=4, separators=(',', ': ')))

    @route('/:action=skills/')
    @expose_page(must_login=True, methods=['GET', 'POST'])
    def read_settings(self, **args):
        mappings = self.read_file()
        if cherrypy.request.method == 'GET':
            return self.render_json(mappings)

        if cherrypy.request.method == 'POST':
            intent = json.loads(cherrypy.request.body.read())
            mappings.append(intent)

            self.save_file(mappings)
            return self.render_json(intent)


    @route('/:action=skills/:intent')
    @expose_page(must_login=True, methods=['PUT'])
    def edit(self, intent, **kwargs):
        mappings = self.read_file()
        params = json.loads(cherrypy.request.body.read())

        for mapping in mappings:
            if intent == mapping.get('intent'):
                response = params.get('response')
                if response:
                    mapping['response'] = response

                logger.error('here1')

                search = params.get('search')
                if search:
                    mapping['search'] = search
                elif 'search' in mapping:
                    del mapping['search']

                self.save_file(mappings)
                return self.render_json({"status": 'ok'})

        # new intent
        if 'intent' in params and 'response' in params:
            mappings.append(params)
            self.save_file(mappings)
            return self.render_json({"status": 'ok'})

        return self.generateError(500, su.escape("error"))
