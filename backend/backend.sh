#!/usr/bin/sh

DATABASE_LOCK_FILE=.db.lock

if ! [ -f "$DATABASE_LOCK_FILE" ]; then
  ./db-startup.sh
fi

args="$@"

if [ $# -ne 0 ]; then
  ./gradlew run --args="$args"
else
 ./gradlew run
fi