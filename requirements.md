# Requirements Document: SurakshaNet Edge AI Safety System

## Introduction

SurakshaNet is a low-cost, edge-AI powered road safety system designed for blind intersections, sharp turns, and building-blocked junctions in India. The system operates entirely offline on edge hardware, using camera-based AI vehicle detection as the primary decision source, with motion sensors providing secondary support for presence confirmation and power optimization. The system aims to predict collision risks and provide real-time multilingual alerts to drivers before they can visually detect danger.

The system targets a standalone unit cost of ₹4,000–₹5,000 and aims for sub-second end-to-end latency, making it suitable for deployment by municipal authorities, smart city programs, and local governments across urban and semi-urban areas.

## Glossary

- **SurakshaNet_System**: The complete edge-AI safety system including hardware, software, sensors, and alert mechanisms
- **Edge_Device**: The local computing hardware (Raspberry Pi or similar) that runs all AI processing offline
- **Camera_Module**: The primary visual sensor and decision source for vehicle detection and tracking
- **Motion_Sensor**: Secondary sensors (PIR/ultrasonic) used for presence confirmation and power optimization
- **Vehicle_Detector**: The AI model (YOLO-Nano/MobileNet-SSD) that identifies and classifies vehicles
- **Object_Tracker**: The algorithm that tracks detected vehicles across video frames
- **Risk_Predictor**: The component that calculates collision risk based on vehicle parameters
- **Alert_Controller**: The component that manages LED, buzzer, and voice alert outputs
- **Event_Logger**: The component that records near-miss and high-risk events locally
- **Cloud_Sync_Module**: Optional component for uploading summarized data to AWS (post-event analytics only)
- **Detection_Zone**: The monitored area where vehicles are detected and tracked
- **Time_To_Collision**: Estimated seconds until potential collision (TTC)
- **Risk_Level**: Classification of collision danger (Low, Medium, High)
- **Near_Miss_Event**: A logged incident where High risk was detected
- **Blind_Intersection**: A junction where approaching vehicles cannot be seen due to buildings, walls, or terrain
- **Lightweight_Model**: AI model optimized for edge device constraints (memory, compute, power)
- **Sensor_Fusion**: The process of combining camera and motion sensor data for improved reliability
- **Offline_First**: Architecture principle where all critical functions work without internet connectivity

## Requirements

### Requirement 1: Vehicle Detection and Classification

**User Story:** As a road safety system, I want to detect and classify approaching vehicles in real-time, so that I can assess potential collision risks at blind intersections.

#### Acceptance Criteria

1. WHEN a vehicle enters the Detection_Zone, THE Vehicle_Detector SHALL identify it within a target of 500 milliseconds
2. WHEN a vehicle is detected, THE Vehicle_Detector SHALL classify it as one of: bike, car, or heavy vehicle
3. THE Vehicle_Detector SHALL operate using a Lightweight_Model (YOLO-Nano or MobileNet-SSD) that runs on the Edge_Device
4. WHEN multiple vehicles are present, THE Vehicle_Detector SHALL detect and classify each vehicle independently
5. WHEN lighting conditions are low, THE Vehicle_Detector SHALL detect vehicles using headlight signatures
6. THE Vehicle_Detector SHALL target a minimum 85% detection accuracy for vehicles within 50 meters
7. THE Vehicle_Detector SHALL aim to process camera frames at minimum 10 frames per second on the Edge_Device

### Requirement 2: Motion Tracking and Speed Estimation

**User Story:** As a road safety system, I want to track vehicle movement and estimate speed, so that I can predict time to collision and assess danger levels.

#### Acceptance Criteria

1. WHEN a vehicle is detected, THE Object_Tracker SHALL track its position across consecutive frames
2. WHEN tracking a vehicle, THE Object_Tracker SHALL maintain a unique identifier for that vehicle across frames
3. WHEN a vehicle moves, THE Object_Tracker SHALL calculate its speed in kilometers per hour
4. WHEN a vehicle moves, THE Object_Tracker SHALL determine its direction of travel
5. WHEN tracking data is available, THE Object_Tracker SHALL estimate Time_To_Collision for vehicles approaching the intersection
6. THE Object_Tracker SHALL target tracking accuracy within 10% error margin for speeds between 10-80 km/h
7. WHEN a vehicle exits the Detection_Zone, THE Object_Tracker SHALL aim to remove it from active tracking within 2 seconds

### Requirement 3: Risk Prediction and Assessment

**User Story:** As a road safety system, I want to predict collision risk levels, so that I can provide appropriate warnings to drivers.

#### Acceptance Criteria

1. WHEN vehicle tracking data is available, THE Risk_Predictor SHALL calculate a Risk_Level (Low, Medium, or High)
2. WHEN calculating risk, THE Risk_Predictor SHALL consider vehicle speed, distance from intersection, direction, and vehicle type
3. WHEN Time_To_Collision is less than 5 seconds, THE Risk_Predictor SHALL classify risk as High
4. WHEN Time_To_Collision is between 5-10 seconds, THE Risk_Predictor SHALL classify risk as Medium
5. WHEN Time_To_Collision is greater than 10 seconds, THE Risk_Predictor SHALL classify risk as Low
6. WHEN calculating risk at night, THE Risk_Predictor SHALL apply increased risk weighting due to reduced visibility
7. WHEN multiple vehicles are detected, THE Risk_Predictor SHALL calculate risk for each vehicle and report the highest Risk_Level
8. THE Risk_Predictor SHALL aim to update risk calculations at minimum 5 times per second

### Requirement 4: Motion Sensor Integration

**User Story:** As a road safety system, I want to use motion sensors as a secondary support layer, so that I can confirm vehicle presence and optimize power consumption.

#### Acceptance Criteria

1. WHEN a Motion_Sensor detects movement, THE SurakshaNet_System SHALL activate the Camera_Module if it is in low-power mode
2. WHEN both Motion_Sensor and Camera_Module detect a vehicle, THE SurakshaNet_System SHALL use confidence-weighted fusion to improve detection reliability
3. WHEN the Camera_Module fails to detect a vehicle but Motion_Sensor indicates presence, THE SurakshaNet_System SHALL trigger a cautionary Medium risk alert
4. THE Motion_Sensor SHALL aim to detect movement within a 10-meter range
5. WHEN no motion is detected for 60 seconds, THE SurakshaNet_System SHALL enter low-power mode
6. THE SurakshaNet_System SHALL support both PIR and ultrasonic Motion_Sensor types

### Requirement 5: Multi-Modal Alert System

**User Story:** As a driver approaching a blind intersection, I want to receive clear visual and audio warnings, so that I can slow down and avoid collisions.

#### Acceptance Criteria

1. WHEN Risk_Level is Low, THE Alert_Controller SHALL not activate any alerts
2. WHEN Risk_Level is Medium, THE Alert_Controller SHALL activate a yellow LED and emit a single beep
3. WHEN Risk_Level is High, THE Alert_Controller SHALL activate a red LED, emit continuous beeping, and play a voice alert
4. WHEN playing a voice alert, THE Alert_Controller SHALL announce the warning in both Hindi and English
5. THE Alert_Controller SHALL aim to deliver alerts within 200 milliseconds of Risk_Level determination
6. WHEN Risk_Level changes from High to Medium or Low, THE Alert_Controller SHALL aim to update the alert state within 500 milliseconds
7. THE Alert_Controller SHALL support adjustable volume levels for buzzer and voice output
8. WHEN multiple High risk vehicles are detected, THE Alert_Controller SHALL maintain continuous alert state until all risks are resolved

### Requirement 6: Event Logging and Storage

**User Story:** As a traffic authority, I want the system to log near-miss and high-risk events, so that I can analyze accident-prone locations and improve road safety.

#### Acceptance Criteria

1. WHEN Risk_Level reaches High, THE Event_Logger SHALL record a Near_Miss_Event with timestamp, vehicle type, speed, and risk score
2. THE Event_Logger SHALL store events locally on the Edge_Device
3. THE Event_Logger SHALL include camera snapshot images with each Near_Miss_Event
4. WHEN local storage reaches 90% capacity, THE Event_Logger SHALL delete the oldest events to maintain system operation
5. THE Event_Logger SHALL maintain event logs for minimum 30 days before automatic deletion
6. THE Event_Logger SHALL record events in a structured format (JSON or CSV) for easy analysis
7. WHEN the system restarts, THE Event_Logger SHALL preserve all previously recorded events

### Requirement 7: Offline-First Operation

**User Story:** As a deployment authority, I want the system to operate completely offline, so that it works reliably in areas with poor or no internet connectivity.

#### Acceptance Criteria

1. THE SurakshaNet_System SHALL perform all vehicle detection, tracking, risk prediction, and alerting without requiring internet connectivity
2. THE SurakshaNet_System SHALL store all AI models locally on the Edge_Device
3. THE SurakshaNet_System SHALL maintain full functionality during network outages
4. WHEN internet connectivity is unavailable, THE SurakshaNet_System SHALL continue logging events locally
5. THE SurakshaNet_System SHALL not depend on cloud services for any real-time safety-critical operations
6. THE SurakshaNet_System SHALL aim to complete the entire detection-to-alert pipeline in under 1 second without network access

### Requirement 8: Optional Cloud Synchronization

**User Story:** As a traffic management authority, I want to optionally sync event data to the cloud for post-event analytics, so that I can perform centralized analysis across multiple installations without affecting real-time safety operations.

#### Acceptance Criteria

1. WHERE cloud connectivity is configured, THE Cloud_Sync_Module SHALL upload summarized event data to AWS S3 for post-event analytics
2. WHEN uploading data, THE Cloud_Sync_Module SHALL not interfere with real-time detection and alerting operations
3. THE Cloud_Sync_Module SHALL upload data in batches during low-activity periods
4. WHEN internet connectivity is restored after an outage, THE Cloud_Sync_Module SHALL upload all pending events
5. THE Cloud_Sync_Module SHALL use AWS Lambda for serverless post-event data processing
6. THE Cloud_Sync_Module SHALL store event metadata in AWS DynamoDB for post-event querying
7. WHERE cloud sync is disabled, THE SurakshaNet_System SHALL operate with full functionality using only local storage

### Requirement 9: Night and Low-Visibility Operation

**User Story:** As a road safety system, I want to detect vehicles in night-time and low-visibility conditions, so that I can provide protection 24/7.

#### Acceptance Criteria

1. WHEN ambient light is below 50 lux, THE SurakshaNet_System SHALL switch to night-mode detection
2. WHEN in night-mode, THE Vehicle_Detector SHALL detect vehicles primarily using headlight signatures
3. WHEN in night-mode, THE Motion_Sensor SHALL serve as the primary trigger for Camera_Module activation
4. THE Vehicle_Detector SHALL target a minimum 75% detection accuracy in night-mode conditions
5. WHEN fog or rain reduces visibility, THE SurakshaNet_System SHALL increase Motion_Sensor reliance through confidence-weighted fusion
6. THE SurakshaNet_System SHALL automatically detect ambient light levels and switch between day and night modes

### Requirement 10: Edge Hardware Optimization

**User Story:** As a deployment authority, I want the system to run efficiently on low-cost edge hardware, so that we can deploy it widely within budget constraints.

#### Acceptance Criteria

1. THE SurakshaNet_System SHALL operate on a Raspberry Pi 4 (4GB RAM) or equivalent Edge_Device
2. THE SurakshaNet_System SHALL target a total standalone unit cost between ₹4,000-₹5,000 including all hardware
3. THE Vehicle_Detector SHALL use a Lightweight_Model with maximum 50MB model size
4. THE SurakshaNet_System SHALL aim to consume maximum 15 watts of power during active operation
5. THE SurakshaNet_System SHALL utilize hardware acceleration (GPU/NPU) when available on the Edge_Device
6. THE SurakshaNet_System SHALL aim to maintain CPU usage below 80% during peak detection loads
7. THE SurakshaNet_System SHALL operate in ambient temperatures from 0°C to 50°C

### Requirement 11: Modular Intersection Support

**User Story:** As a deployment authority, I want to configure the system for different intersection types, so that I can use it at 3-way, 4-way, and 5-way junctions.

#### Acceptance Criteria

1. THE SurakshaNet_System SHALL support configuration for 3-way, 4-way, and 5-way intersections
2. WHEN configured for an intersection type, THE SurakshaNet_System SHALL define Detection_Zone boundaries for each approach road
3. THE SurakshaNet_System SHALL support independent Camera_Module placement for each approach direction
4. WHEN multiple Camera_Modules are configured, THE SurakshaNet_System SHALL process feeds from all cameras simultaneously
5. THE SurakshaNet_System SHALL allow per-direction risk threshold configuration
6. THE SurakshaNet_System SHALL provide a configuration interface for setting intersection geometry parameters

### Requirement 12: System Reliability and Error Handling

**User Story:** As a deployment authority, I want the system to handle errors gracefully and recover automatically, so that it maintains high availability without manual intervention.

#### Acceptance Criteria

1. WHEN the Camera_Module fails, THE SurakshaNet_System SHALL log the error and attempt to reinitialize the camera every 30 seconds
2. WHEN the Camera_Module is unavailable, THE SurakshaNet_System SHALL rely on Motion_Sensor data and issue cautionary Medium risk alerts for detected movement
3. WHEN the Vehicle_Detector model fails to load, THE SurakshaNet_System SHALL log a critical error and retry loading 3 times before entering safe mode
4. WHEN the Edge_Device experiences high CPU temperature (above 80°C), THE SurakshaNet_System SHALL reduce frame processing rate to prevent thermal shutdown
5. WHEN the Alert_Controller hardware fails, THE SurakshaNet_System SHALL log the failure and continue detection and logging operations
6. THE SurakshaNet_System SHALL automatically restart after a crash within 10 seconds
7. THE SurakshaNet_System SHALL target a minimum uptime of 99% over a 30-day period

### Requirement 13: Calibration and Configuration

**User Story:** As a system installer, I want to calibrate the system for specific intersection characteristics, so that it provides accurate risk predictions for that location.

#### Acceptance Criteria

1. THE SurakshaNet_System SHALL provide a calibration mode for setting Detection_Zone boundaries
2. WHEN in calibration mode, THE SurakshaNet_System SHALL display detected vehicle positions and allow boundary adjustment
3. THE SurakshaNet_System SHALL allow configuration of distance-to-intersection measurements for each approach road
4. THE SurakshaNet_System SHALL support configuration of typical vehicle speeds for each approach direction
5. THE SurakshaNet_System SHALL allow adjustment of risk threshold parameters (time-to-collision values)
6. THE SurakshaNet_System SHALL save all calibration settings persistently on the Edge_Device
7. WHEN calibration settings are changed, THE SurakshaNet_System SHALL apply them immediately without requiring a restart

### Requirement 14: Multilingual Voice Alert System

**User Story:** As a driver in India, I want to hear warnings in my preferred language, so that I can understand and respond to alerts quickly.

#### Acceptance Criteria

1. THE Alert_Controller SHALL support voice alerts in Hindi and English
2. WHEN playing a High risk voice alert, THE Alert_Controller SHALL announce "Danger! Vehicle approaching" in Hindi followed by English
3. THE SurakshaNet_System SHALL store pre-recorded voice alert audio files locally on the Edge_Device
4. THE Alert_Controller SHALL play voice alerts at a volume audible from 20 meters distance
5. THE SurakshaNet_System SHALL allow configuration of language preference order
6. THE SurakshaNet_System SHALL support addition of regional language audio files through configuration updates

### Requirement 15: Performance Monitoring and Diagnostics

**User Story:** As a system maintainer, I want to monitor system performance and diagnose issues, so that I can ensure optimal operation and quickly resolve problems.

#### Acceptance Criteria

1. THE SurakshaNet_System SHALL log system performance metrics including CPU usage, memory usage, and frame processing rate
2. THE SurakshaNet_System SHALL provide a diagnostic interface accessible via local network connection
3. WHEN accessed, THE diagnostic interface SHALL display current system status, active alerts, and recent events
4. THE SurakshaNet_System SHALL log all errors and warnings with timestamps and severity levels
5. THE SurakshaNet_System SHALL maintain diagnostic logs for minimum 7 days
6. THE SurakshaNet_System SHALL provide detection accuracy statistics (detections per hour, alert frequency)
7. WHEN performance degrades below acceptable thresholds, THE SurakshaNet_System SHALL log a warning event



---

## Phase-1 Scope Clarification

This requirements document defines the complete vision for SurakshaNet. However, Phase-1 implementation focuses on establishing core feasibility and architecture:

**Phase-1 Priorities:**
- Edge-AI feasibility demonstration on low-cost hardware
- Offline-first operation with camera-based AI as primary decision source
- Risk prediction logic and alert system functionality
- Cost-aware architecture within ₹4,000-₹5,000 standalone unit target
- Basic motion sensor integration for presence confirmation

**Deferred to Later Phases:**
- Performance validation and optimization of accuracy, latency, and throughput metrics
- Field testing and calibration at actual blind intersections
- Multi-camera configurations for complex intersections
- Cloud analytics integration and dashboard development
- Long-term reliability and uptime validation
- Advanced sensor fusion algorithms and weather adaptation

All quantitative performance targets (FPS, accuracy percentages, latency values, uptime goals) represent design targets for the complete system and will be validated and refined through iterative development and field testing in subsequent phases.
