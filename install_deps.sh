#!/bin/bash

sudo apt-get update
sudo apt install -y git maven ant vim openjdk-8-jdk
sudo update-alternatives --set java $(sudo update-alternatives --list java | grep "java-8")
