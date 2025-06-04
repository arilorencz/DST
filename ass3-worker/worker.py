import sys
import time
import signal
import pika
import redis
import json
import random
import math
from threading import Event

shutdown_event = Event()

def signal_handler(sig, frame):
    print("Signal received!")
    sys.exit(0)

signal.signal(signal.SIGTERM, signal_handler)

# Simulated workload times per region
REGION_SLEEP = {
    "at_linz": (1, 2),
    "at_vienna": (3, 5),
    "de_berlin": (8, 11)
}

def haversine(lat1, lon1, lat2, lon2):
    R = 6371  # Earth radius in km
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))

def get_sleep_time(region):
    return random.randint(*REGION_SLEEP.get(region, (1, 2)))

def connect_to_rabbitmq():
    while not shutdown_event.is_set():
        try:
            credentials = pika.PlainCredentials('dst', 'dst')
            connection = pika.BlockingConnection(pika.ConnectionParameters(host="rabbit", port=5672, virtual_host='/', credentials=credentials))
            return connection
        except pika.exceptions.AMQPConnectionError:
            print("RabbitMQ connection failed, retrying in 5 seconds...")
            time.sleep(5)

def main(region):
    print(f"Worker started for region: {region}")

    redis_client = redis.Redis(host='redis', port=6379, db=0, decode_responses=True)

    connection = connect_to_rabbitmq()
    channel = connection.channel()
    channel.queue_declare(queue=f'dst.{region}', durable=True)

    def callback(ch, method, properties, body):
        request = json.loads(body)
        start_time = time.time()

        redis_key = f"drivers:{region}"
        drivers = redis_client.hgetall(redis_key)

        if not drivers:
            response = {
                "id": request["id"],
                "driverId": "",
                "processingTime": 0
                        }
            channel.basic_publish(exchange='',
                                  routing_key=f'requests.{region}',
                                  body=json.dumps(response))
            return

        pickup = request['pickup']
        closest_driver = None
        closest_distance = float('inf')
        pickup_lat = pickup['latitude']
        pickup_lon = pickup['longitude']

        for driver_id, coord in drivers.items():
            lat, lon = map(float, coord.split())
            dist = haversine(lat, lon, pickup_lat, pickup_lon)
            if dist < closest_distance:
                closest_driver = driver_id
                closest_distance = dist

        # Try to remove the selected driver
        if closest_driver:
            removed = redis_client.hdel(redis_key, closest_driver)
            if removed == 0:
                print("Driver already taken, retrying matching")
                callback(ch, method, properties, body)  # retry
                return

        time.sleep(get_sleep_time(region))

        processing_time = int((time.time() - start_time) * 1000)
        response = {
            "id": request["id"],
            "driverId": closest_driver,
            "processingTime": processing_time
                    }
        channel.basic_publish(exchange='',
                              routing_key=f'requests.{region}',
                              body=json.dumps(response))

    channel.basic_consume(queue=f'dst.{region}', on_message_callback=callback, auto_ack=True)

    print("Waiting for messages...")
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        print("Worker interrupted")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: worker.py <region>")
        sys.exit(1)
    main(sys.argv[1])