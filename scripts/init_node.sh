#!/bin/bash

# To fix network add this to /etc/sysconfig/network-scripts/ifcfg-enp0s3
#DNS1=8.8.8.8
#DNS2=8.8.4.4
#ONBOOT=yes

# update package manager
yum update -y

# install yum utilities (e.g. yum-config-manager) and other tools
yum install -y yum-utils wget curl tar net-tools

# add docker repo to yum
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# install docker
yum install -y docker-ce docker-ce-cli containerd.io

# start docker
systemctl start docker

# configure docker to start on boot
sudo systemctl enable docker.service
sudo systemctl enable containerd.service

cd /tmp

wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
tar xf openjdk-17.0.2_linux-x64_bin.tar.gz
mv jdk-17.0.2/ /opt/jdk-17/

# configure java
echo "export JAVA_HOME=/opt/jdk-17" >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc

# run ~/.bashrc file
source ~/.bashrc

mkdir -p /opt/log

echo "
[Unit]
Description=Start worker controller
After=network.target docker.service containerd.service

[Service]
WorkingDirectory=/
ExecStart=/usr/bin/sh -c 'exec /opt/jdk-17/bin/java -jar /root/worker_controller-all.jar >> /opt/log/worker_controller.log'
User=root
Type=simple
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
" > /etc/systemd/system/worker-controller.service

systemctl daemon-reload
systemctl enable worker-controller.service
systemctl start worker-controller.service

# disable firewall
systemctl stop firewalld
systemctl disable firewalld

sudo iptables -t filter -F
iptables -t filter -X
systemctl restart docker
