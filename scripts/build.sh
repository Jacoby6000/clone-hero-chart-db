#!/bin/bash

sbt ++$TRAVIS_SCALA_VERSION flywayMigrate compile test