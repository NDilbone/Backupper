# 💾 Backupper

A robust, multithreaded file backup utility designed to create versioned backups with integrity verification and
Discord notifications.

![Backupper](https://img.shields.io/badge/Backupper-Easy_&_Efficient_Backups-orange)
![Build & Test](https://github.com/NDilbone/Backupper/actions/workflows/gradle.yml/badge.svg)
![License](https://img.shields.io/github/license/NDilbone/Backupper)
![Last Commit](https://img.shields.io/github/last-commit/NDilbone/Backupper)
![Issues](https://img.shields.io/github/issues/NDilbone/Backupper)
![Java](https://img.shields.io/badge/java-17+-blue)
![GitHub release](https://img.shields.io/github/v/release/NDilbone/Backupper)


## 🔍 Overview

Backupper is a Java application that provides reliable file backup capabilities with the following features:

- 🚀 **Multi-threaded operation** for efficient file copying
- 📅 **Versioned backups** with timestamp-based naming
- 🧹 **Automatic cleanup** of old backups
- ✅ **File integrity verification** using checksums
- 🔄 **Retry mechanism** for handling transient failures
- 💬 **Discord notifications** for backup status and failures
- 🚫 **Configurable exclusion patterns** to skip unwanted files
- 📝 **Detailed logging** of the backup process

## 📋 Requirements

- ☕ Java 17 or higher
- 🛠️ Gradle (for building from source)
- 🔔 Discord webhook (for notifications)

## 🚀 Setup

### Option 1: Download the pre-built JAR

1. 📥 Download the latest `Backupper.jar` from the release's page
2. ⚙️ Create a `config.json` file in the same directory as the JAR (see the Configuration section)
3. ▶️ Run the application with `java -jar Backupper.jar`

### Option 2: Build from source

1. 📋 Clone the repository:
   ```
   git clone https://github.com/NDilbone/Backupper.git
   cd Backupper
   ```

2. 🔨 Build the application:
   ```
   ./gradlew build
   ```

   This will create a fat JAR file in the `build/libs` directory named `Backupper.jar`

3. ⚙️ Create a `config.json` file in the `src/main/resources` directory (see the Configuration section)

4. ▶️ Run the application:
   ```
   java -jar build/libs/Backupper.jar
   ```

## ⚙️ Configuration

Backupper is configured using a JSON file named `config.json`. The file can be placed in one of two locations:

1. In the same directory as the JAR file (recommended for production use)
2. In the `src/main/resources` directory (when building from source)

The application will first look for the config file in the same directory as the JAR, and if not found, it will look in
the classpath resources.

An example configuration file is provided below:

```json
{
  "sourceDir": "C:\\Path\\To\\Source\\Directory",
  "destinationDir": "D:\\Path\\To\\Backup\\Destination",
  "maxBackupsToKeep": 5,
  "discordWebhookUrl": "https://discord.com/api/webhooks/your-webhook-id/your-webhook-token",
  "discordUserId": 123456789012345678,
  "threadPoolSize": 8,
  "exclusionPatterns": [
    ".*\\.tmp$",
    ".*\\.temp$",
    ".*\\.bak$",
    ".*\\/cache\\/.*"
  ],
  "maxRetries": 3,
  "retryDelayMs": 1000
}
```

### 🔧 Configuration Options

| Option              | Description                                          | Default                                                                   |
|---------------------|------------------------------------------------------|---------------------------------------------------------------------------|
| `sourceDir`         | The directory to backup from                         | *Required*                                                                |
| `destinationDir`    | The directory to backup to                           | *Required*                                                                |
| `maxBackupsToKeep`  | Number of backups to retain before deleting old ones | 5                                                                         |
| `discordWebhookUrl` | URL for Discord notifications                        | *Required*                                                                |
| `discordUserId`     | Discord user ID for notifications                    | *Required*                                                                |
| `threadPoolSize`    | Number of threads to use for file copying            | Number of available CPU cores                                             |
| `exclusionPatterns` | List of regex patterns for files to exclude          | ".\*\\\\.tmp\$", ".\*\\\\.temp\$", ".\*\\\\.bak$", ".\*\\\\/cache\\\\/.*" |
| `maxRetries`        | Maximum retry attempts for failed operations         | 3                                                                         |
| `retryDelayMs`      | Delay between retries in milliseconds                | 1000                                                                      |

## 📖 Usage

### 🔄 Basic Usage

Run the application with:

```
java -jar Backupper.jar
```

The application will:

1. 📂 Load the configuration from `config.json`
2. 🧹 Clean up old backups if necessary
3. 📅 Create a new versioned backup directory with a timestamp
4. 📋 Copy files from the source directory to the versioned backup directory
5. ✅ Verify file integrity using checksums
6. 🔔 Send notifications about the backup status and any failed files
7. 📝 Log the backup process to the console and log files

### 📊 Logs

Logs are stored in the `logs` directory:

- 📄 `app.log`: Contains all log messages
- ⚠️ `errors.log`: Contains only error messages

## 🔍 Features in Detail

### 📅 Versioned Backups

Each backup is stored in a separate directory with a timestamp in the format `docker-backup_yyyy-MM-dd_HHmm_ss`. This
allows you to keep multiple versions of your backups and easily identify when each backup was created.

### 🧹 Backup Cleanup

The application automatically cleans up old backups based on the `maxBackupsToKeep` configuration option. The oldest
backups are deleted first.

### 🚀 Multi-threaded File Copying

The application uses a thread pool to copy files in parallel, which can significantly improve performance, especially
for large backups. The number of threads can be configured using the `threadPoolSize` option.

### ✅ File Integrity Verification

After copying each file, the application verifies its integrity by calculating checksums of the source and destination
files and comparing them. This ensures that the backup is an exact copy of the source.

### 🔄 Retry Mechanism

If a file copy operation fails, the application will retry it up to `maxRetries` times with a delay of `retryDelayMs`
milliseconds between retries. This helps handle transient failures such as network issues.

### 💬 Discord Notifications

The application sends notifications to Discord about the backup status and any failed files. You need to provide a
Discord webhook URL and user ID in the configuration.

### 🚫 Exclusion Patterns

You can specify regex patterns for files to exclude from the backup using the `exclusionPatterns` configuration option.
This is useful for skipping temporary files, cache files, or other files that don't need to be backed up.

## ❓ Troubleshooting

### 🔧 Common Issues

1. **Configuration file not found**
   - Make sure `config.json` is either:
      - In the same directory as the JAR file (for production use), or
      - In the `src/main/resources` directory (when building from source)
    - Check that the file has the correct name and extension

2. **Invalid source or destination directory**
    - Verify that the paths in the configuration file are correct
    - Make sure the directories exist and are accessible

3. **Discord notifications not working**
    - Check that the webhook URL and user ID are correct
    - Verify that the webhook is properly set up in Discord

4. **Backup fails with permission errors**
    - Make sure the application has permission to read from the source directory and write to the destination directory
    - Try running the application with administrator privileges

### 🆘 Getting Help

If you encounter any issues not covered here, please open an issue on the GitHub repository with a detailed description
of the problem and any relevant logs.

## 📄 License

This project is licensed under the MIT License—see the LICENSE file for details.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
