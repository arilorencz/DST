# Use a slim version of Python as base
FROM python:3-slim

# Set environment variables to prevent Python from writing .pyc files and buffering stdout/stderr
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Install necessary system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    libpq-dev \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# Set work directory
WORKDIR /app

# Copy your worker script into the container
COPY worker.py .

# Install required Python packages
RUN pip install --no-cache-dir pika redis

# Command to run the worker
ENTRYPOINT ["python", "worker.py"]