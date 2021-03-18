#!/bin/sh

echo "Waiting for localstack to be ready"

until aws --endpoint-url=http://localstack:4566 kinesis list-streams; do
	>&2 echo "Kinesis is unavailable - sleeping"
		sleep 2
done

echo "Kinesis is ready"

echo "Localstack is ready"

echo "---------------------------"

echo "Setting up Kinesis Streams"

aws kinesis create-stream --endpoint-url=http://localstack:4566 --shard-count 1 --stream-name stocks

echo "AWS Resources created on Localstack"

