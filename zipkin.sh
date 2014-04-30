#!/bin/bash

ZIPKIN_DIR="zipkin"
ZIPKIN_GIT="https://github.com/twitter/zipkin.git"
ZIPKIN_URL="http://localhost:8080"

usage() {
  echo "Usage: `basename $0` {install|start|stop|restart}"
}

check() {
  for i in "git pkill"
  do
    command -v $i >/dev/null && continue || { echo "$i command not found."; exit 1; }
  done
}

if [[ -z "${1}" ]]; then
  usage
  exit 1
fi

install() {
  check
  if [ ! -d $ZIPKIN_DIR ]; then
    git clone $ZIPKIN_GIT
  fi
  pushd .
  cd $ZIPKIN_DIR
  git pull origin master
  bin/sbt compile
  popd
}

start() {
  if [ ! -d $ZIPKIN_DIR ]; then
    echo "Zipkin not found. Run '`basename $0` install' to install it."
    exit 1
  fi
  
  pushd .
  cd $ZIPKIN_DIR
  bin/collector & disown
  bin/query & disown
  bin/web & disown  
  popd
  
  sleep 20
  if which xdg-open > /dev/null; then
    xdg-open $ZIPKIN_URL
  elif which gnome-open > /dev/null; then
    gnome-open $ZIPKIN_URL
  fi
}

stop() {
  pkill -f zipkin-web
  pkill -f zipkin-query
  pkill -f zipkin-collector
}


case "$1" in
  install)
    install
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
