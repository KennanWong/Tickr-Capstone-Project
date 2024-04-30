#!/usr/bin/bash

CONTAINER_NAME=tickr-db

LOCK_FILE=.db.lock

if ! [ -f "$LOCK_FILE" ]; then
  echo "Lock file not present - database should already be shutdown!"
fi

docker stop $CONTAINER_NAME
docker rm $CONTAINER_NAME

rm $LOCK_FILE