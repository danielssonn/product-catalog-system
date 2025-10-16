FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application files
COPY *.js ./

# Expose ports for the three mock core systems
EXPOSE 3000 3001 3002

# Start the application
CMD ["node", "mock-core-server.js"]
