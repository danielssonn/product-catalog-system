FROM mongo:7.0

# Copy initialization script
COPY init-mongo.js /docker-entrypoint-initdb.d/

# Copy MongoDB configuration to disable FTDC and reduce disk usage
COPY mongod.conf /etc/mongod.conf

# Expose MongoDB port
EXPOSE 27017

# MongoDB will automatically run scripts in /docker-entrypoint-initdb.d/
# Start with custom config to disable diagnostic data collection
CMD ["mongod", "--config", "/etc/mongod.conf"]