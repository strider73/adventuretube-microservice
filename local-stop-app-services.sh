#!/bin/bash

# Stop the 5 locally running business services.

echo "Stopping local business services..."

pkill -f "auth-service.*spring-boot" && echo "  auth-service stopped" || echo "  auth-service not running"
pkill -f "member-service.*spring-boot" && echo "  member-service stopped" || echo "  member-service not running"
pkill -f "geospatial-service.*spring-boot" && echo "  geospatial-service stopped" || echo "  geospatial-service not running"
pkill -f "web-service.*spring-boot" && echo "  web-service stopped" || echo "  web-service not running"
pkill -f "youtube-service.*spring-boot" && echo "  youtube-service stopped" || echo "  youtube-service not running"

echo "Done."
