# Stanford HAI Study - Project Summary

## Overview
This Android application was developed for Stanford's Human-Centered AI Institute to collect detailed app usage statistics for academic research. The app features a clean, professional interface with comprehensive data collection and export capabilities.

## Key Achievements

### ✅ Completed Features
- **Modern Android Architecture**: Kotlin + Jetpack Compose + MVVM + Room Database
- **Stanford HAI Branding**: Professional UI with Stanford logo and clean design
- **Multi-Tenant Database**: Separate user data with aggregated research database
- **Real-Time Tracking**: Performance-optimized session tracking (30-second intervals)
- **Smart App Filtering**: Excludes system apps, focuses on user-facing applications
- **Comprehensive Export**: JSON, CSV, and SQLite database export functionality
- **Privacy Compliance**: Local storage with user-controlled data sharing
- **Session Management**: Detailed logging of app usage sessions (10+ seconds)
- **Metadata Collection**: Device context (orientation, battery, network)

### 🏗️ Technical Implementation
- **Database**: Room SQLite with optimized indexing
- **UI**: Jetpack Compose with Material Design 3
- **Data Export**: Gson JSON serialization, CSV generation, FileProvider sharing
- **Performance**: Coroutines for async operations, optimized tracking intervals
- **Architecture**: Clean separation with repositories, DAOs, and ViewModels

## Database Schema

### Core Tables
1. **`user_profile`** - User identification and study information
2. **`user_usage_stats`** - Detailed app usage per session with metadata
3. **`master_usage_stats`** - Aggregated, anonymized research data

### Key Features
- Multi-tenant architecture for research studies
- Real-time session tracking with metadata
- Optimized indexing for performance
- Export-ready data structure

## User Experience

### First Run
1. Study ID input screen (Stanford HAI branding)
2. Usage access permission setup
3. Automatic tracking initialization

### Main Interface
1. Stanford HAI logo display
2. Tracking toggle button (pauses UI display, continues background collection)
3. Data export section (JSON, CSV, SQLite)
4. Study information

### Export Process
1. Tap export button (JSON/CSV/DB)
2. Choose sharing method (email, cloud storage, file transfer)
3. Files include timestamp and complete data set

## Files Structure

### Source Code (`app/src/main/java/com/example/usagestatisticsapp/`)
```
├── data/
│   ├── AppDatabase.kt              # Room database configuration
│   ├── UserProfile.kt              # User identification entity
│   ├── UserUsageStats.kt           # Usage tracking entity
│   ├── MasterUsageStats.kt         # Aggregated data entity
│   ├── *Dao.kt                     # Data access objects
│   ├── *Repository.kt              # Repository pattern implementation
│   ├── RealTimeSessionTracker.kt   # Performance-optimized tracking
│   ├── DataAggregationService.kt   # Research data aggregation
│   └── DataExportService.kt        # Export functionality
├── ui/
│   ├── UserSetupScreen.kt          # Study ID input
│   ├── MainScreen.kt               # Stanford HAI main interface
│   └── UserManagementViewModel.kt  # Core app logic
└── MainActivity.kt                 # App entry point
```

### Resources (`app/src/main/res/`)
```
├── drawable/
│   └── stanford_hai_logo.xml      # Stanford HAI logo
├── values/
│   └── strings.xml                 # App name and descriptions
└── xml/
    └── file_paths.xml              # FileProvider configuration
```

### Documentation
```
├── README.md                       # Complete project documentation
├── EXPORT_FUNCTIONALITY.md        # Detailed export guide
├── PROJECT_SUMMARY.md             # This file
└── .gitignore                      # Git ignore rules
```

## Data Flow

1. **User Setup** → Study ID input → User profile creation
2. **Real-Time Tracking** → App usage monitoring → Database storage
3. **Data Aggregation** → User data → Master research database
4. **Export Process** → User action → File generation → Sharing

## Privacy & Compliance

- **Local Storage**: All data stored on device until user exports
- **User Control**: Export requires explicit user action
- **Anonymized Aggregation**: Master database contains no personal identifiers
- **Stanford Compliant**: Designed for academic research standards

## Technical Notes

### Performance Optimizations
- 30-second tracking intervals (reduced from 5 seconds)
- Optimized database queries with proper indexing
- Background processing for data collection
- Efficient memory management

### Export Capabilities
- **JSON**: Complete structured data for programming languages
- **CSV**: Spreadsheet-compatible format for analysis tools
- **SQLite**: Raw database for direct SQL access

### Future Considerations
- Dashboard integration ready
- Cloud sync infrastructure prepared
- Scalable multi-tenant architecture
- Research-grade data collection

## Build & Deploy

### Requirements
- Android Studio 2024+
- Kotlin 1.9+
- Android API 24+ (supports 95%+ of devices)
- Gradle 8.0+

### Build Process
```bash
./gradlew build          # Build all variants
./gradlew assembleDebug  # Debug APK
./gradlew assembleRelease # Release APK
```

### APK Location
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Repository Status

### Ready for GitHub
- ✅ Clean codebase with proper documentation
- ✅ Professional README with setup instructions
- ✅ Proper .gitignore for Android projects
- ✅ Comprehensive export documentation
- ✅ Build verification completed
- ✅ No temporary or development files

### Repository Structure
```
UsageStatisticsApp/
├── app/                    # Android app module
├── gradle/                 # Gradle configuration
├── README.md              # Main documentation
├── EXPORT_FUNCTIONALITY.md # Export guide
├── PROJECT_SUMMARY.md     # This summary
├── .gitignore             # Git ignore rules
├── build.gradle.kts       # Root build configuration
└── settings.gradle.kts    # Project settings
```

## Success Metrics

✅ **Complete Functionality**: All requested features implemented
✅ **Modern Architecture**: Current Android best practices
✅ **Research Ready**: Multi-tenant database with export capabilities
✅ **Performance Optimized**: No UI lag or excessive battery drain
✅ **Professional UI**: Stanford HAI branding and clean design
✅ **Privacy Compliant**: Local storage with user-controlled sharing
✅ **Documentation**: Comprehensive guides and code comments
✅ **Build Ready**: Successful compilation and deployment

The project is now ready for GitHub deployment and research use.
