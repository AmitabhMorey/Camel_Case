# SecureQrVotingSystem API Documentation

## Overview

The SecureQrVotingSystem provides a comprehensive REST API for secure online voting with multi-factor authentication using QR codes and OTPs. This document describes all available endpoints, request/response formats, and security considerations.

## Base URL

```
http://localhost:8080
```

## Authentication

The system uses a multi-factor authentication approach:
1. QR Code validation
2. OTP (One-Time Password) validation
3. Session-based authentication for subsequent requests

## API Endpoints

### Authentication Endpoints

#### POST /auth/register
Register a new user account.

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": "string",
  "qrCodeData": "string"
}
```

**Status Codes:**
- 201: User created successfully
- 400: Invalid input data
- 409: Username or email already exists

#### POST /auth/login/qr
Authenticate using QR code.

**Request Body:**
```json
{
  "userId": "string",
  "qrData": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "QR authentication successful",
  "nextStep": "otp_required"
}
```

**Status Codes:**
- 200: QR authentication successful
- 401: Invalid QR code
- 429: Too many attempts

#### POST /auth/login/otp
Authenticate using OTP.

**Request Body:**
```json
{
  "userId": "string",
  "otp": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "sessionId": "string",
  "expiryTime": "2024-01-01T12:00:00Z"
}
```

**Status Codes:**
- 200: Authentication successful
- 401: Invalid OTP
- 408: OTP expired

#### POST /auth/logout
Logout and invalidate session.

**Headers:**
```
Authorization: Bearer <sessionId>
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

### Voting Endpoints

#### GET /voting/elections
Get list of active elections.

**Headers:**
```
Authorization: Bearer <sessionId>
```

**Response:**
```json
{
  "elections": [
    {
      "electionId": "string",
      "title": "string",
      "description": "string",
      "startTime": "2024-01-01T10:00:00Z",
      "endTime": "2024-01-01T18:00:00Z",
      "status": "ACTIVE"
    }
  ]
}
```

#### GET /voting/elections/{electionId}
Get election details including candidates.

**Headers:**
```
Authorization: Bearer <sessionId>
```

**Response:**
```json
{
  "electionId": "string",
  "title": "string",
  "description": "string",
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T18:00:00Z",
  "status": "ACTIVE",
  "candidates": [
    {
      "candidateId": "string",
      "name": "string",
      "description": "string"
    }
  ]
}
```

#### POST /voting/vote
Cast a vote in an election.

**Headers:**
```
Authorization: Bearer <sessionId>
```

**Request Body:**
```json
{
  "electionId": "string",
  "candidateId": "string"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Vote cast successfully",
  "voteId": "string",
  "timestamp": "2024-01-01T12:30:00Z"
}
```

**Status Codes:**
- 201: Vote cast successfully
- 400: Invalid election or candidate
- 409: User has already voted
- 403: Election not active

#### GET /voting/status/{electionId}
Check if user has voted in an election.

**Headers:**
```
Authorization: Bearer <sessionId>
```

**Response:**
```json
{
  "hasVoted": true,
  "voteTimestamp": "2024-01-01T12:30:00Z"
}
```

### Admin Endpoints

#### POST /admin/elections
Create a new election (Admin only).

**Headers:**
```
Authorization: Bearer <sessionId>
X-Admin-Role: required
```

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T18:00:00Z",
  "candidates": [
    {
      "name": "string",
      "description": "string"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "electionId": "string",
  "message": "Election created successfully"
}
```

#### GET /admin/elections
Get all elections with management details (Admin only).

**Headers:**
```
Authorization: Bearer <sessionId>
X-Admin-Role: required
```

**Response:**
```json
{
  "elections": [
    {
      "electionId": "string",
      "title": "string",
      "description": "string",
      "startTime": "2024-01-01T10:00:00Z",
      "endTime": "2024-01-01T18:00:00Z",
      "status": "ACTIVE",
      "totalVotes": 150,
      "totalRegisteredVoters": 200
    }
  ]
}
```

#### POST /admin/elections/{electionId}/tally
Tally votes for an election (Admin only).

**Headers:**
```
Authorization: Bearer <sessionId>
X-Admin-Role: required
```

**Response:**
```json
{
  "electionId": "string",
  "totalVotes": 150,
  "results": [
    {
      "candidateId": "string",
      "candidateName": "string",
      "voteCount": 75,
      "percentage": 50.0
    }
  ],
  "tallyTimestamp": "2024-01-01T19:00:00Z"
}
```

#### GET /admin/audit
Get audit logs (Admin only).

**Headers:**
```
Authorization: Bearer <sessionId>
X-Admin-Role: required
```

**Query Parameters:**
- `eventType`: Filter by event type (optional)
- `userId`: Filter by user ID (optional)
- `startDate`: Start date for filtering (optional)
- `endDate`: End date for filtering (optional)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Response:**
```json
{
  "auditLogs": [
    {
      "logId": "string",
      "userId": "string",
      "action": "string",
      "details": "string",
      "ipAddress": "string",
      "timestamp": "2024-01-01T12:30:00Z",
      "eventType": "USER_ACTION"
    }
  ],
  "totalElements": 500,
  "totalPages": 25,
  "currentPage": 0
}
```

#### GET /admin/statistics
Get system statistics (Admin only).

**Headers:**
```
Authorization: Bearer <sessionId>
X-Admin-Role: required
```

**Response:**
```json
{
  "totalUsers": 1000,
  "totalElections": 5,
  "activeElections": 2,
  "totalVotesCast": 750,
  "systemUptime": "72h 30m",
  "lastBackup": "2024-01-01T02:00:00Z"
}
```

### QR Code Endpoints

#### GET /qr/generate/{userId}
Generate QR code for user authentication.

**Response:**
```json
{
  "qrData": "string",
  "qrImageBase64": "string",
  "expiryTime": "2024-01-01T12:35:00Z"
}
```

#### GET /qr/image/{userId}
Get QR code image as PNG.

**Response:**
- Content-Type: image/png
- Binary image data

### Health and Monitoring

#### GET /actuator/health
Application health check.

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "errorCode": "string",
  "message": "string",
  "timestamp": "2024-01-01T12:30:00Z",
  "path": "/api/endpoint",
  "validationErrors": {
    "field": "error message"
  }
}
```

### Common Error Codes

- `AUTH_001`: Invalid credentials
- `AUTH_002`: Session expired
- `AUTH_003`: Insufficient permissions
- `VOTE_001`: Election not found
- `VOTE_002`: Candidate not found
- `VOTE_003`: Duplicate vote attempt
- `VOTE_004`: Election not active
- `SYS_001`: Internal server error
- `VAL_001`: Validation error

## Security Considerations

### Rate Limiting
- Authentication endpoints: 5 requests per minute per IP
- Voting endpoints: 10 requests per minute per user
- Admin endpoints: 20 requests per minute per admin

### Input Validation
- All inputs are validated and sanitized
- SQL injection protection through parameterized queries
- XSS protection through output encoding

### Encryption
- Vote data encrypted using AES-256-GCM
- Passwords hashed using bcrypt
- Session tokens are cryptographically secure

### Audit Logging
- All API calls are logged with user context
- Security events are logged with detailed information
- Logs include IP addresses and timestamps

## Testing the API

### Using cURL

#### Register a new user:
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "securepassword123"
  }'
```

#### Authenticate with QR:
```bash
curl -X POST http://localhost:8080/auth/login/qr \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-id-from-registration",
    "qrData": "qr-data-from-registration"
  }'
```

#### Cast a vote:
```bash
curl -X POST http://localhost:8080/voting/vote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer session-id-from-auth" \
  -d '{
    "electionId": "election-id",
    "candidateId": "candidate-id"
  }'
```

### Using Postman

1. Import the API collection (if available)
2. Set up environment variables for base URL and session tokens
3. Follow the authentication flow before testing voting endpoints
4. Use admin credentials for admin endpoint testing

## WebSocket Endpoints (Future Enhancement)

The system is designed to support real-time updates through WebSocket connections:

- `/ws/election-updates`: Real-time election status updates
- `/ws/vote-counts`: Live vote count updates (admin only)

## SDK and Client Libraries

Client libraries are planned for:
- JavaScript/TypeScript
- Python
- Java
- C#

## Changelog

### Version 1.0.0
- Initial API release
- Basic authentication and voting functionality
- Admin management capabilities
- Comprehensive audit logging

### Planned Features
- Mobile app API extensions
- Blockchain integration for vote verification
- Advanced analytics endpoints
- Multi-language support