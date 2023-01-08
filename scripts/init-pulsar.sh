#!/bin/bash

bin/apply-config-from-env.py conf/client.conf

bin/pulsar-admin topics create test
