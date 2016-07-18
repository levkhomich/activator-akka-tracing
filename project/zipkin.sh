#!/bin/bash

ZIPKIN_DIR="zipkin"
ZIPKIN_JAR_LOCATION="https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec"
ZIPKIN_URL="http://localhost:9411"

usage() {
  echo "Usage: `basename $0` {install|start|stop|restart}"
}

check() {
  for i in "pkill wget"
  do
    command -v $i >/dev/null && continue || { echo "$i command not found."; exit 1; }
  done
}

if [[ -z "${1}" ]]; then
  usage
  exit 1
fi

update() {
  check
  if [ ! -d $ZIPKIN_DIR ]; then
    echo "Zipkin not found. Run install first."
    exit 1
  fi
  wget -O $ZIPKIN_DIR/zipkin.jar $ZIPKIN_JAR_LOCATION
}

install() {
  check
  if [ ! -d $ZIPKIN_DIR ]; then
    mkdir $ZIPKIN_DIR
    update
  fi
}

start() {
  if [ ! -d $ZIPKIN_DIR ]; then
    echo "Zipkin not found. Installing it..."
    install
  fi

  stop

  java -jar $ZIPKIN_DIR/zipkin.jar &

  sleep 10s
  
  if which xdg-open > /dev/null; then
    xdg-open $ZIPKIN_URL
  elif which gnome-open > /dev/null; then
    gnome-open $ZIPKIN_URL
  elif which open > /dev/null; then
    open $ZIPKIN_URL
  fi

  wait
}

stop() {
  pkill -f zipkin.jar
}


case "$1" in
  install)
    install
    ;;
  update)
    update
    ;;
  start)
    start
    ;;
  restart)
    stop
    sleep 1
    start
    ;;
  stop)
    stop
    ;;
  *)
    echo $1
    usage
    exit 4
  ;;
esac

exit 0
