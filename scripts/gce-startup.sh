set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}"

# Install dependencies from apt
apt-get install -yq openjdk-8-jdk git

# Jetty Setup
mkdir -p /opt/jetty/temp
mkdir -p /var/log/jetty

# Get Jetty
curl -L https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.26.v20200117/jetty-distribution-9.4.26.v20200117.tar.gz -o jetty9.tgz
tar xf jetty9.tgz  --strip-components=1 -C /opt/jetty

# Add a Jetty User
useradd --user-group --shell /bin/false --home-dir /opt/jetty/temp jetty

cd /opt/jetty
# Add running as "jetty"
java -jar /opt/jetty/start.jar --add-to-startd=setuid
cd /

# Clone the source repository.
git clone https://github.com/heathwinning/pools-of-money.git
cd pools-of-money

# Build the .war file and rename.
# very important - by renaming the war to root.war, it will run as the root servlet.
./gradlew war
mv build/lib/moneypools.war /opt/jetty/webapps/root.war

# Make sure "jetty" owns everything.
chown --recursive jetty /opt/jetty

# Configure the default paths for the Jetty service
cp /opt/jetty/bin/jetty.sh /etc/init.d/jetty
echo "JETTY_HOME=/opt/jetty" > /etc/default/jetty
{
  echo "JETTY_BASE=/opt/jetty"
  echo "TMPDIR=/opt/jetty/temp"
  echo "JAVA_OPTIONS=-Djetty.http.port=80"
  echo "JETTY_LOGS=/var/log/jetty"
} >> /etc/default/jetty

# Reload daemon to pick up new service
systemctl daemon-reload

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &

service jetty start
service jetty check

echo "Startup Complete"