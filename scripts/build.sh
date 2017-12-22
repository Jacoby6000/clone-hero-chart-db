#!/bin/bash

sbt ++$TRAVIS_SCALA_VERSION flywayMigrate compile coverage test it:test coverageReport && bash <(curl -s https://codecov.io/bash)
