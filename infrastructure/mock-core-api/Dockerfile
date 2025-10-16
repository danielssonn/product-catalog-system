FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application files
COPY mock-core-server.js ./
COPY .env* ./

# Expose ports for all three mock APIs
EXPOSE 3000 3001 3002

# Start the server
CMD ["node", "mock-core-server.js"]
