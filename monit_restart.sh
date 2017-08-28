#!/bin/sh

USAGE="<GIT REPO NAME> {start|stop} <PORT> [<JAVA OPTS>]"

if [ $# -lt 3 ]; then
	echo "$USAGE
				THIS SCRIPT SHOULD ONLY BE USED BY -MONIT-!

				If you want to restart an instance, use ./restart.sh

				First 3 parameters are mandatory.
				Don't forget that the process is monitored by 'monit'.
				It will restart automatically if you stop the API.
				If you want to stop it permanently, do 'sudo /etc/ini.d/monit stop' first.
				"
	exit 65
fi

REPO=$1
ACTION=$2
PORT=$3
JAVA_OPTS="$4"

HOME="/home/sol"

# it is important to set the proper locale
. $HOME/.locale
JAVA_OPTS=$(echo "$JAVA_OPTS" |sed 's#,#\ #g')

cd $HOME/git/$REPO
case $ACTION in
	start)
		kill $(cat target/universal/stage/RUNNING_PID)
		JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError" $HOME/activator-1.3.10-minimal/bin/activator "start $PORT"
		;;
	stop)
		kill $(cat target/universal/stage/RUNNING_PID)
		;;
	*)
		echo "usage: $USAGE"
		;;
esac
exit 0
