#!/bin/bash
cat app/build.gradle.kts | grep -v 'buildTypes {' | awk '1;/defaultConfig/{print "    signingConfigs {\n        create(\"release\") {\n            storeFile = rootProject.file(\"debug.keystore\")\n            storePassword = \"android\"\n            keyAlias = \"androiddebugkey\"\n            keyPassword = \"android\"\n        }\n    }\n    buildTypes {"}' > app/build.gradle.kts.new
mv app/build.gradle.kts.new app/build.gradle.kts
