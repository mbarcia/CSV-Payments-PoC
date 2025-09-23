# Cloudflare Pages Deployment Instructions

## Prerequisites

1. A Cloudflare account
2. The project repository connected to Cloudflare Pages

## Build Configuration

When setting up the project in Cloudflare Pages, use the following configuration:

- **Build command**: `cd docs && npm run build`
- **Build output directory**: `docs/.vitepress/dist`
- **Root directory**: `/`

## New Features

The documentation site now includes:

1. **Search Functionality**: Users can search across all documentation pages using the search bar in the top navigation
2. **Multi-Version Support**: Documentation is available for multiple versions of the Pipeline Framework:
   - v0.9.0 (current)
   - v0.8.0
   - v0.7.0

## Configuration Notes

The VitePress configuration includes `ignoreDeadLinks: true` to prevent build failures due to links that are valid in the context of the full project but not within the documentation site. This is necessary because:

1. The documentation site only covers the Pipeline Framework, not the entire project
2. Many markdown files in the project contain links that are valid in the context of the full repository but aren't relevant to the documentation site
3. There are localhost links in various README files that can't be resolved during the build process

This is a common configuration for documentation sites that are part of larger projects.

## Manual Deployment (Optional)

If you prefer to deploy manually, you can use Wrangler:

1. Install Wrangler:
   ```bash
   npm install -g wrangler
   ```

2. Build the documentation:
   ```bash
   cd docs && npm run build
   ```

3. Deploy to Cloudflare Pages:
   ```bash
   wrangler pages deploy docs/.vitepress/dist --project-name=your-project-name
   ```

## Environment Variables

No special environment variables are required for the basic setup.

## Custom Domain

After deployment, you can configure a custom domain through the Cloudflare dashboard.