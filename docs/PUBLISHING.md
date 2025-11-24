# Publishing to Maven Central

This document explains how to publish The Pipeline Framework to Maven Central and how to manage the project's versioning and release process properly.

## TL;DR: Automatic Release Process

The release process is fully automated with GitHub Actions:

1. **Update version in root POM** (if needed): Change `<version.pipeline>` in `pom.xml`
2. **Commit and push changes**: `git add . && git commit -m "Release x.y.z" && git push`
3. **Create and push a Git tag**: `git tag vx.y.z && git push origin vx.y.z`
4. **Watch GitHub Actions**: Go to the Actions tab to monitor the release workflow
5. **Verify on Maven Central**: Check artifacts are published at <https://s01.oss.sonatype.org/>

The GitHub Actions workflow automatically:
- Builds and tests the complete project
- Runs unit tests during the build phase
- Runs integration tests (on main branch only)
- Builds native executables for example services (on main branch)
- Signs all artifacts with GPG
- Deploys to Sonatype OSSRH
- Closes and releases the staging repository
- Creates a GitHub release with notes

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

1. **Single Source of Truth**: The version is defined in the root POM (`pom.xml`) as the `<version.pipeline>` property
2. **Module References**: All other POM files reference this property instead of hard-coding versions
3. **Build-Time Resolution**: The Flatten Maven Plugin resolves property references to literal values during the build process
4. **Updating Versions**: To update the version, change it only in the root POM

### Version Property Definition

In several (`pom.xml`):
```xml
<properties>
    <!-- ... other properties ... -->
    <version.pipeline>0.9.0</version.pipeline>
    <!-- ... other properties ... -->
</properties>
```

- Framework's parent POM
- Deployment POM
- Runtime module POM
- Root POM
- CSV Payments parent POM

### Using Maven Versions Plugin

To update versions across all modules consistently, use the Maven Versions Plugin:

```bash
# Update the version across all modules
mvn versions:set -DnewVersion=1.0.0

# Verify the changes before committing
mvn versions:commit

# Or rollback if needed
mvn versions:revert
```

This ensures that all modules in the multimodule project are updated consistently.

### Flatten Plugin Configuration

To comply with Maven's requirement that the `<version>` element in project POMs be literal values while still maintaining a single source of truth, we use the Maven Flatten Plugin:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>flatten-maven-plugin</artifactId>
    <version>1.6.0</version>
    <configuration>
        <updatePomFile>true</updatePomFile>
        <flattenMode>oss</flattenMode>
    </configuration>
    <executions>
        <execution>
            <id>flatten</id>
            <phase>process-resources</phase>
            <goals>
                <goal>flatten</goal>
            </goals>
        </execution>
        <execution>
            <id>flatten-clean</id>
            <phase>clean</phase>
            <goals>
                <goal>clean</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

This configuration generates a `.flattened-pom.xml` file with all properties resolved to their literal values during the build process. This full property resolution is required for Maven Central publishing to ensure that the published artifacts have all dependencies with literal versions instead of property placeholders.

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

The release process is automated using GitHub Actions.

### Required GitHub Secrets

To use the workflow, these secrets must exist in the GitHub repository:

1. `OSSRH_USERNAME` - Your Sonatype username
2. `OSSRH_PASSWORD` - Your Sonatype password
3. `GPG_PRIVATE_KEY` - Your GPG private key exported with `gpg --export-secret-keys --armor <your-key-id>`
4. `GPG_PASSPHRASE` - The passphrase for your GPG key

## Safe Release Process

### Standard Release Workflow (Recommended)

The Maven Release Plugin provides a complete automated solution.
Again, this is automatically managed by GitHub Actions, which actions the following steps.

1. **Prepare the Release**:
   - Use the Maven Release Plugin to prepare the release:
     ```bash
     mvn release:prepare
     ```
   - This will update versions, create a tag, and prepare the release in one step
   - The plugin will prompt for:
     - The release version (e.g., 1.0.0)
     - The SCM tag (e.g., v1.0.0)
     - The next development version (e.g., 1.0.1-SNAPSHOT)

2. **Perform the Release**:
   - Deploy the release to Maven Central:
     ```bash
     mvn release:perform
     ```
   - This will check out the tagged version and run the deployment process with the `release` profile

### When to Use the Versions Plugin Approach

Use this manual approach only when you need fine-grained control or the Release Plugin is not available:

1. **Manual Version Update**:
   - Update the version using the Maven Versions Plugin:
     ```bash
     mvn versions:set -DnewVersion=1.0.0
     mvn versions:commit
     ```
   - Test the build with `mvn clean install -P release`
   - Create a Git tag (e.g., `v1.0.0`)
   - Push the tag to trigger the GitHub Actions release workflow

**Comparison**:
- **Release Plugin**: Handles everything automatically (version updates, SCM tagging, deployment) but requires proper plugin configuration
- **Versions Plugin**: Offers more manual control but requires multiple manual steps and careful coordination

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
- `mvn clean verify` - test the build locally (runs unit tests)
- `mvn clean verify -DskipITs` - test build without integration tests
- `mvn clean verify -P release` - test with the release profile but without deployment
- Use a test Sonatype repository for verification

## Important Notes

- Only the framework artifacts (not example applications) are published to Maven Central
- The examples project continues to depend on the published framework artifacts
- The root POM orchestrates the overall build while the framework POM handles publishing
- Always verify your release artifacts on Maven Central after a successful deployment