# Data Export Functionality

## Overview
The Stanford HAI Study app now includes comprehensive data export functionality that allows you to extract all research data in multiple formats for local analysis and dashboard development.

## Export Options

### 1. JSON Export
- **Format**: Structured JSON with all data organized by tables
- **Content**: Complete export including user profiles, usage stats, and master stats
- **Use Case**: Best for importing into databases or processing with programming languages
- **File**: `usage_stats_export_YYYY-MM-DD_HH-mm-ss.json`

### 2. CSV Export
- **Format**: Comma-separated values with table/field/value structure
- **Content**: All data flattened into key-value pairs
- **Use Case**: Easy to open in Excel, Google Sheets, or data analysis tools
- **File**: `usage_stats_export_YYYY-MM-DD_HH-mm-ss.csv`

### 3. Database Export
- **Format**: Complete SQLite database file
- **Content**: Raw database with all tables and relationships intact
- **Use Case**: Direct database analysis, SQL queries, or migration to other systems
- **File**: `usage_stats_database_YYYY-MM-DD_HH-mm-ss.db`

## How to Export Data

### From the App:
1. **Open the app** and complete Study ID setup
2. **Navigate to the main screen** (Stanford HAI logo)
3. **Find the "Data Export" section** below the tracking toggle
4. **Tap one of the export buttons**:
   - **JSON** - For structured data export
   - **CSV** - For spreadsheet-compatible export
   - **DB** - For complete database export
5. **Choose sharing method** when prompted:
   - Email to yourself
   - Save to cloud storage (Google Drive, Dropbox)
   - Transfer via USB/ADB
   - Share with other apps

### Export Data Structure

#### JSON Export Format:
```json
{
  "exportDate": "2024-01-15_14-30-25",
  "userProfiles": [
    {
      "userId": "ABC123",
      "registrationDate": "2024-01-15T10:00:00Z",
      "deviceId": "device_12345",
      "studyGroup": null,
      "lastActiveDate": "2024-01-15T14:30:00Z"
    }
  ],
  "userUsageStats": [
    {
      "id": 1,
      "userId": "ABC123",
      "sessionId": "session_1705320000000_abc12345",
      "appPackageName": "com.whatsapp",
      "appName": "WhatsApp",
      "startTime": "2024-01-15T10:15:00Z",
      "endTime": "2024-01-15T10:25:00Z",
      "duration": 600000,
      "interactionCount": 5,
      "timestamp": "2024-01-15T10:15:00Z",
      "isActive": false,
      "appCategory": "Social",
      "deviceOrientation": "portrait",
      "batteryLevel": 85,
      "networkType": "WiFi"
    }
  ],
  "masterUsageStats": [
    {
      "id": 1,
      "appPackageName": "com.whatsapp",
      "appName": "WhatsApp",
      "totalUsers": 1,
      "averageUsageTime": 600000,
      "totalUsageTime": 600000,
      "totalSessions": 1,
      "studyPeriod": "Week 1",
      "lastUpdated": "2024-01-15T14:30:00Z"
    }
  ],
  "summary": {
    "totalUsers": 1,
    "totalUsageSessions": 5,
    "totalMasterRecords": 3,
    "dateRange": "2024-01-15 to 2024-01-15",
    "exportFormat": "JSON"
  }
}
```

#### CSV Export Format:
```csv
Table,Field,Value
user_profile,userId,ABC123
user_profile,registrationDate,2024-01-15T10:00:00Z
user_profile,deviceId,device_12345
user_profile,studyGroup,
user_profile,lastActiveDate,2024-01-15T14:30:00Z
user_usage_stats,id,1
user_usage_stats,userId,ABC123
user_usage_stats,sessionId,session_1705320000000_abc12345
user_usage_stats,appPackageName,com.whatsapp
user_usage_stats,appName,WhatsApp
user_usage_stats,startTime,2024-01-15T10:15:00Z
user_usage_stats,endTime,2024-01-15T10:25:00Z
user_usage_stats,duration,600000
user_usage_stats,interactionCount,5
user_usage_stats,timestamp,2024-01-15T10:15:00Z
user_usage_stats,isActive,false
user_usage_stats,appCategory,Social
user_usage_stats,deviceOrientation,portrait
user_usage_stats,batteryLevel,85
user_usage_stats,networkType,WiFi
```

## Database Schema (SQLite Export)

### Tables:
1. **user_profile** - User identification and study information
2. **user_usage_stats** - Detailed app usage data per session
3. **master_usage_stats** - Aggregated research data

### Key Fields:
- **userId**: Unique study participant identifier
- **sessionId**: Individual usage session identifier
- **appPackageName**: Android app package name
- **startTime/endTime**: Session timing
- **duration**: Usage duration in milliseconds
- **appCategory**: App categorization (Social, Productivity, etc.)
- **deviceOrientation**: Device orientation during usage
- **batteryLevel**: Battery level when session started
- **networkType**: Network connection type

## Local Testing Setup

### For Dashboard Development:
1. **Export database file** (.db) for direct SQLite access
2. **Use JSON export** for easy parsing in JavaScript/Python
3. **Use CSV export** for quick analysis in Excel/Google Sheets

### Recommended Workflow:
1. **Collect data** on Android device/emulator
2. **Export database file** for local analysis
3. **Import into local SQLite database** for dashboard development
4. **Use JSON/CSV exports** for specific data processing needs

## Privacy & Security
- **Local storage only** - All data stays on device until exported
- **User consent** - Export requires user action
- **No automatic upload** - Data is only shared when user chooses to export
- **Timestamped exports** - Each export includes creation timestamp

## Troubleshooting
- **Export fails**: Ensure app has storage permissions
- **File not found**: Check device's Downloads or shared location
- **Empty export**: Verify that tracking has been active and data collected
- **Large file size**: Database exports include all historical data

## Next Steps for Dashboard
1. **Export database file** from app
2. **Set up local SQLite database** for development
3. **Create dashboard backend** to read SQLite data
4. **Build visualization frontend** using exported data structure
5. **Implement real-time sync** when ready for production
