set -e

docker-compose down --volumes --remove-orphans || true
docker-compose up --force-recreate -d

echo "$(date) Waiting for Kinesis to be running"
until wget -q -O - -t 1 http://localhost:4566/health | grep '"kinesis": "running"' ; do
	sleep 2
done
echo "$(date) Kinesis up and running"
