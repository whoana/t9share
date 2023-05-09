BUILD_TAR_HOME=.
./gradlew clean build makeInstall zipinstall -x test

if [ -f $BUILD_TAR_HOME/t9-install-linux.tar ]; then
	rm $BUILD_TAR_HOME/t9-install-linux.tar
fi
tar -cvf $BUILD_TAR_HOME/t9-install-linux.tar $BUILD_TAR_HOME/t9-install-linux
