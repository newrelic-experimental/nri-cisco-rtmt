#!/bin/bash

INSTALL_DIR="/opt/newrelic/cisco-rtmt-collector"

systemctl stop cisco-rtmt-collector
systemctl disable cisco-rtmt-collector

systemctl daemon-reload

systemctl reset-failed

cd "/opt"

rm -rf "newrelic"
