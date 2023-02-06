#!/bin/bash

# TODO: wait for pulsar and check if the topic is created

bin/apply-config-from-env.py conf/client.conf && \
  bin/pulsar-admin topics create test
