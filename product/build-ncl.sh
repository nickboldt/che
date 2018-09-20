#!/bin/bash -xe
# script to build eclipse-che in #projectncl

##########################################################################################
# apply patches - dont forget to \$ so that mvn variables are not interpreted by bash 
##########################################################################################

# path relative to root since we run this script as ./product/build-ncl.sh
#patch -p2 <product/foo.patch

# fix dashboard - migrate to Yarn
# git cherry-pick --keep-redundant-commits 3005b907815118c8ebef75a09d51798e0b052077

# remove docs from assembly-main - requires using '-P!docs' profile
# git cherry-pick --keep-redundant-commits c1fa62ae86f976d97247e726458f6e25ccf0611f

##########################################################################################
# enable support for CI builds
##########################################################################################

#set version & compute qualifier from best available in Indy
# or use commandline overrides for version and suffix

if [[ $1 ]]; then
	version="$1"
else
	version=6.12.0
fi

if [[ $2 ]]; then
	suffix="$2"
else
	tmpfile=/tmp/maven-depmgt-pom-${version}.html
	# external 1: http://indy.cloud.pnc.engineering.redhat.com/api/group/static/org/eclipse/che/depmgt/maven-depmgt-pom
	# external 2: http://indy.cloud.pnc.engineering.redhat.com/api/content/maven/group/builds-untested+shared-imports+public/org/eclipse/che/depmgt/maven-depmgt-pom
	UPSTREAM_POM="api/content/maven/group/builds-untested+shared-imports+public/org/eclipse/che/depmgt/maven-depmgt-pom"
	INDY=http://indy.project-newcastle.svc.cluster.local
	if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
		INDY=http://pnc-indy-branch-nightly.project-newcastle.svc.cluster.local
	fi
	if [[ ! $(wget ${INDY} -q -S 2>&1 | egrep "200|302|OK") ]]; then
		echo "[WARNING] Could not load org/eclipse/che/depmgt/maven-depmgt-pom from Indy"
	fi
	wget ${INDY}/${UPSTREAM_POM} -O ${tmpfile}
	suffix=$(grep ${version} ${tmpfile} | egrep '.redhat-[0-9]{5}' | sed -e "s#.\+>\([0-9.]\+\.\)\(redhat-[0-9]\{5\}\).*#\2#" | sort -r | head -1)
	rm -f ${tmpfile}
fi

# replace pme version with the version from upstream parent pom, so we can resolve parent pom version 
# and all artifacts in che-* builds use the same qualifier
# TODO: might be able to skip this step once PNC 1.4 / PME 3.1 is rolled out:
# see https://docs.engineering.redhat.com/display/JPC/PME+3.1
# temp w/ timestamp: 6.12.0.t20180917-201638-873-redhat-00001
# temp w/o timestamp: 6.12.0.temporary-redhat-00001-47358abd
# persistent: 6.12.0.redhat-00001-ec28abe6
# pmeVersionSHA=$(git describe --tags)
# pmeSuffix=${pmeVersion#${version}.}; echo $suffix
if [[ ${suffix} ]]; then 
	for d in $(find . -name pom.xml); do sed -i "s#\(version>\)${version}.*\(</version>\)#\1${version}.${suffix}\2#g" $d; done
	for d in $(find . -name pom.xml); do sed -i "s#\(<che.\+version>\)${version}.*\(</che.\+version>\)#\1${version}.${suffix}\2#g" $d; done
	for d in $(find . -name pom.xml); do sed -i "s#\(<version>${version}\)-SNAPSHOT#\1.${suffix}#g" $d; done
	mvn versions:set -DnewVersion=${version}.${suffix}
	mvn versions:update-parent "-DparentVersion=${version}.${suffix}" -DallowSnapshots=false
	for d in $(find . -maxdepth 1 -name pom.xml); do sed -i "s#\(<.\+\.version>.\+\)-SNAPSHOT#\1.${suffix}#g" $d; done
fi

##########################################################################################
# set up npm environment
##########################################################################################

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
# do not use maxsockets 2 or build will stall & die
npm config set maxsockets 80 
npm config set fetch-retries 10
npm config set fetch-retry-mintimeout 60000
npm config set registry ${npmRegistryURL}

pushd dashboard
	npm install yarn
	PATH=${PATH}:$(pwd)/node_modules/yarn/bin
	yarn config set registry http://registry.yarnpkg.com
	yarn config set proxy ${NCL_PROXY}
	yarn config set https-proxy ${NCL_PROXY}
	yarn install
popd

##########################################################################################
# configure maven build 
##########################################################################################

PROFILES='-Pfast,native,!docker,!docs'
MVNFLAGS="-V -ff -B -e -Dskip-enforce -DskipTests -Dskip-validate-sources -Dfindbugs.skip -DskipIntegrationTests=true"
MVNFLAGS="${MVNFLAGS} -Dmdep.analyze.skip=true -Dmaven.javadoc.skip -Dgpg.skip -Dorg.slf4j.simpleLogger.showDateTime=true"
MVNFLAGS="${MVNFLAGS} -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss "
MVNFLAGS="${MVNFLAGS} -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
MVNFLAGS="${MVNFLAGS} -DnodeDownloadRoot=${nodeDownloadRoot} -DnpmDownloadRoot=${npmDownloadRoot}"
MVNFLAGS="${MVNFLAGS} -DnpmRegistryURL=${npmRegistryURL}"

##########################################################################################
# run maven build 
##########################################################################################

mvn clean deploy ${PROFILES} ${MVNFLAGS}
