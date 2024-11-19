#!/bin/bash

# Setup API Server
git clone https://github.com/CodeMC/API build/tmp/CodeMC-API
chmod +x ./build/tmp/CodeMC-API/.github/test.sh

# Run API Server
cd build/tmp/CodeMC-API
/.github/test.sh
cd ../../../
