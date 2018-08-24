#!/bin/bash -xe
# script to build eclipse-che in #projectncl

uname -a
go version
node -v
npm version
mvn -v

export NCL_PROXY="http://${buildContentId}+tracking:${accessToken}@${proxyServer}:${proxyPort}"
# wget proxies
export http_proxy="${NCL_PROXY}"
export https_proxy="${NCL_PROXY}"

nodeDownloadRoot=http://nodejs.org:80/dist/
npmDownloadRoot=http://registry.npmjs.org:80/npm/-/
npmRegistryURL=http://registry.npmjs.org:80/
npm config set https-proxy ${NCL_PROXY}
npm config set proxy ${NCL_PROXY}
#silent, warn, info, verbose, silly
npm config set loglevel warn 
npm config set maxsockets 80
npm config set fetch-retries 10
npm config set fetch-retry-mintimeout 60000
npm config set registry ${npmRegistryURL}

# workaround for lack of https support and inability to see github.com as a result
mkdir -p /tmp/phantomjs/
pushd /tmp/phantomjs/
  # previously mirrored from https://github.com/Medium/phantomjs/releases/download/v2.1.1/phantomjs-2.1.1-linux-x86_64.tar.bz2
  time wget -q http://download.jboss.org/jbosstools/updates/requirements/node/phantomjs/phantomjs-2.1.1-linux-x86_64.tar.bz2
popd
pushd dashboard
  time npm install phantomjs-prebuilt
  export PATH=${PATH}:`pwd`/node_modules/phantomjs-prebuilt/bin
  time npm install yarn
  export PATH=${PATH}:`pwd`/node_modules/yarn/bin
  export TMPDIR=/tmp
  yarn config set proxy ${NCL_PROXY}
  yarn config set https-proxy ${NCL_PROXY}

  # apply patch to move from bower to yarn
  # https://github.com/nickboldt/che/tree/10881
  # path relative to root since we run this script as ./product/build-ncl.sh
  patch -p 2 -F 2 --ignore-whitespace < ../product/10881-replace-bower-with-yarn.patch
popd

mvn clean deploy -V -ff -B -e '-Pfast,native,!docker' -Dskip-enforce -DskipTests -Dskip-validate-sources -Dfindbugs.skip -DskipIntegrationTests=true \
-Dmdep.analyze.skip=true -Dmaven.javadoc.skip -Dgpg.skip -Dorg.slf4j.simpleLogger.showDateTime=true \
-Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
-DnodeDownloadRoot=${nodeDownloadRoot} -DnpmDownloadRoot=${npmDownloadRoot} -DnpmRegistryURL=${npmRegistryURL} ${1}

uname -a
go version
node -v
npm version
mvn -v