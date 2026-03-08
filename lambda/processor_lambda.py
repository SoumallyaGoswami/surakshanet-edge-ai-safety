import json
import boto3
import uuid
from decimal import Decimal
from datetime import datetime

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('SurakshaNetEvents')

def lambda_handler(event, context):

    print("Received IoT Event:")
    print(event)

    state = event.get("state", "UNKNOWN")
    tti = Decimal(str(event.get("tti", 0)))
    intersection_id = event.get("intersection_id", "INT_001")
    vehicle = event.get("vehicle", "car")

    item = {
        "event_id": str(uuid.uuid4()),
        "intersection_id": intersection_id,
        "state": state,
        "tti": tti,
        "vehicle": vehicle,
        "timestamp": datetime.utcnow().isoformat()
    }

    table.put_item(Item=item)

    print("Saved to DynamoDB:", item)

    return {
        "statusCode": 200,
        "body": json.dumps("Event stored successfully")
    }
