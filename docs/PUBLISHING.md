# Publishing to Maven Central

This document explains how to publish The Pipeline Framework to Maven Central and how to properly manage the project's versioning and release process.

## Table of Contents

- [Overview](#overview)
- [Version Management](#version-management)
- [Maven Central Publishing Setup](#maven-central-publishing-setup)
- [settings.xml Configuration](#settingsxml-configuration)
- [GitHub Actions Workflow](#github-actions-workflow)
- [Safe Release Process](#safe-release-process)
- [Troubleshooting](#troubleshooting)

## Overview

The Pipeline Framework is published to Maven Central to make it available to developers who want to use it in their projects. This document outlines the process, configuration, and best practices for publishing releases.

## Version Management

The Pipeline Framework uses a centralized version management system to ensure consistency across all modules:

1. **Single Source of Truth**: The version is defined in the root POM (`pom.xml`) as the `<pipeline.framework.version>` property
2. **Module References**: All other POM files reference this property instead of hardcoding versions
3. **Updating Versions**: To update the version, change it only in the root POM

### Version Property Definition

In the root POM (`pom.xml`):
```xml
<properties>
    <!-- ... other properties ... -->
    <pipeline.framework.version>0.9.0</pipeline.framework.version>
    <pipeline.version>${pipeline.framework.version}</pipeline.version>
    <!-- ... other properties ... -->
</properties>
```

All other modules reference this property, ensuring a single point of update for version changes.

## Maven Central Publishing Setup

The Maven Central publishing configuration is located in the framework's parent POM (`framework/pom.xml`):

### Distribution Management

```xml
<distributionManagement>
    <snapshotRepository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>
```

### Required Plugins

The following plugins are configured in the framework POM for Maven Central compliance:

1. **Source Plugin**: Generates sources JAR
2. **Javadoc Plugin**: Generates documentation JAR
3. **GPG Plugin**: Signs artifacts
4. **Nexus Staging Plugin**: Deploys to Sonatype OSSRH

For the complete configuration, see the release profile in `framework/pom.xml`.

## settings.xml Configuration

To authenticate with Sonatype OSSRH and provide GPG credentials, configure your `~/.m2/settings.xml` file:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-sonatype-username</username>
      <password>your-encrypted-sonatype-password</password>
    </server>
  </servers>
</settings>
```

### GPG Configuration

For GPG signing, you have two options:

**Option 1: In settings.xml (less secure)**
```xml
<profiles>
  <profile>
    <id>ossrh</id>
    <properties>
      <gpg.executable>gpg</gpg.executable>
      <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
      <gpg.keyname>your-gpg-key-id</gpg.keyname>
    </properties>
  </profile>
</profiles>

<activeProfiles>
  <activeProfile>ossrh</activeProfile>
</activeProfiles>
```

**Option 2: Through environment variables (recommended for CI/CD)**
Use environment variables and GPG agent for security in automated environments.

### Encrypting Passwords

To encrypt your Sonatype password:

1. Create the master password:
   ```bash
   mvn --encrypt-master-password
   ```
   This creates `~/.m2/settings-security.xml`

2. Encrypt your Sonatype password:
   ```bash
   mvn --encrypt-password
   ```

3. Update settings.xml with the encrypted password (prefixed with `{` and suffixed with `}`).

## GitHub Actions Workflow

The release process is automated using GitHub Actions. The workflow is triggered by creating a Git tag with the pattern `v*`:

### Release Workflow File

Create `.github/workflows/release.yml`:

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        server-id: ossrh
        server-username: MAVEN_USERNAME
        server-password: MAVEN_PASSWORD
        gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
        gpg-passphrase: GPG_PASSPHRASE

    - name: Build and publish
      run: mvn clean deploy -P release
      env:
        MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
        MAVEN_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
        GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

### Required GitHub Secrets

To use the workflow, add these secrets to your GitHub repository:

1. `OSSRH_USERNAME` - Your Sonatype username
2. `OSSRH_PASSWORD` - Your Sonatype password
3. `GPG_PRIVATE_KEY` - Your GPG private key exported with `gpg --export-secret-keys --armor <your-key-id>`
4. `GPG_PASSPHRASE` - The passphrase for your GPG key

## Safe Release Process

To avoid unintentionally triggering a release, follow this recommended workflow:

1. **Version Preparation**:
   - Update the version in the root POM to the desired release version
   - Run `mvn versions:commit` to propagate version changes (if using versions plugin)
   - Test the build with `mvn clean install`

2. **Pull Request Review**:
   - Open a pull request with version changes
   - Get team approval before merging

3. **Tagged Release**:
   - After merging to main, create a Git tag (e.g., `v1.0.0`)
   - Push the tag to trigger the GitHub Actions release workflow

### Alternative: Manual Release Workflow

For more control, you can create a workflow dispatch that requires manual triggering:

```yaml
name: Manual Publish to Maven Central

on:
  workflow_dispatch:
    inputs:
      release_version:
        description: 'Release version (e.g., 1.0.0)'
        required: true
        type: string
      dry_run:
        description: 'Dry run? (true/false)'
        required: false
        default: true
        type: boolean
```

This approach allows manual control over when releases happen.

## Troubleshooting

### Common Issues

**GPG Signing Errors**:
- Verify your GPG key is properly configured
- Check that the GPG key ID matches what's in your keystore
- Ensure the GPG agent is running or passphrase is provided

**Sonatype Authentication Errors**:
- Verify your credentials in settings.xml
- Ensure you're using encrypted passwords in public repositories
- Check that your OSSRH account has permissions for the group ID

**Nexus Staging Errors**:
- Review Sonatype OSSRH logs
- Ensure all required artifacts (JARs, sources, javadoc, signatures) are present
- Check that artifacts meet Maven Central requirements

### Testing the Setup

Before pushing a tag that triggers the release workflow:
- `mvn clean verify` - test the build locally
- `mvn clean verify -P release` - test with the release profile but without deployment
- Use a test Sonatype repository for verification

## Important Notes

- Only the framework artifacts (not example applications) are published to Maven Central
- The examples project continues to depend on the published framework artifacts
- The root POM orchestrates the overall build while the framework POM handles publishing
- Always verify your release artifacts on Maven Central after a successful deployment