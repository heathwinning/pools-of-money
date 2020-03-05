set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}"

# Install dependencies from apt
apt-get install -yq openjdk-11-jdk git

cd /
cp /home/heath/moneypools.jar .
if [ -d pools-of-money ] ; then
  rm -rf pools-of-money
fi
git clone https://github.com/heathwinning/pools-of-money.git
cd pools-of-money
cp scripts/moneypools.sh /etc/init.d/moneypools
systemctl daemon-reload

service moneypools restart
service moneypools check

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &

