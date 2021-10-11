#!/bin/bash

INSTALL_DIR="/opt/newrelic/cisco-rtmt-collector"

if [ ! -d $INSTALL_DIR ]
then
  mkdir -p $INSTALL_DIR
fi

cp cisco-rtmt-collector.jar $INSTALL_DIR/
cp runRTMTCollect.sh $INSTALL_DIR/
cp *.json $INSTALL_DIR/
cp cisco-rtmt-collector.service $INSTALL_DIR/

cd /etc/systemd/system

ln -s $INSTALL_DIR/cisco-rtmt-collector.service

systemctl enable cisco-rtmt-collector.service

systemctl start cisco-rtmt-collector
