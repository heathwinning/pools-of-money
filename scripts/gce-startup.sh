set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}"

# Install dependencies from apt
apt-get install -yq openjdk-8-jdk git

cd /
if [ -d pools-of-money ] ; then
  cd pools-of-money
  git pull
else
  git clone https://github.com/heathwinning/pools-of-money.git
  cd pools-of-money
fi
cp /home/heath/moneypools.jar /
cp scripts/moneypools.sh /etc/init.d/moneypools
chmod a+x /etc/init.d/moneypools
systemctl daemon-reload

service moneypools restart

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &

