import json
import boto3

dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table("SurakshaNetEvents")

def lambda_handler(event, context):

    response = table.scan()
    events = response["Items"]

    if len(events) == 0:
        return {
            "statusCode": 200,
            "body": json.dumps({
                "prediction": "No traffic data available yet.",
                "incident_explanation": "No incidents detected."
            })
        }

    intersections = {}
    vehicles = {}

    latest_danger_event = None

    for e in events:

        inter = e.get("intersection_id") or "INT_001"
        veh = e.get("vehicle") or "vehicle"
        state = e.get("state")

        intersections[inter] = intersections.get(inter, 0) + 1
        vehicles[veh] = vehicles.get(veh, 0) + 1

        # detect latest danger event
        if state == "DANGER":
            latest_danger_event = e

    risky_intersection = max(intersections, key=intersections.get)
    risky_vehicle = max(vehicles, key=vehicles.get)

    prediction = f"""
Accident Risk Prediction:

• Highest risk intersection: {risky_intersection}
• Most involved vehicle type: {risky_vehicle}
• Near-collision events detected: {len(events)}

Recommendation:
Install adaptive traffic signals and warning indicators at {risky_intersection}.
"""

    if latest_danger_event:
        tti = latest_danger_event.get("tti", "unknown")
        incident_explanation = f"""
Incident Explanation:

A near collision occurred at {risky_intersection}.
The time-to-impact dropped to approximately {tti} seconds, triggering a danger alert.
The smart intersection warning system alerted drivers and helped prevent a potential crash.
"""
    else:
        incident_explanation = "No danger events recorded yet."

    return {
        "statusCode": 200,
        "body": json.dumps({
            "prediction": prediction,
            "incident_explanation": incident_explanation
        })
    }
