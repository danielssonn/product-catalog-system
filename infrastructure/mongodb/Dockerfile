FROM mongo:7.0

# Copy initialization script
COPY init-mongo.js /docker-entrypoint-initdb.d/

# Expose MongoDB port
EXPOSE 27017

# MongoDB will automatically run scripts in /docker-entrypoint-initdb.d/