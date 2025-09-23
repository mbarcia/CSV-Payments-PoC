# Cloudflare Pages Deployment Instructions

## Prerequisites

1. A Cloudflare account
2. The project repository connected to Cloudflare Pages

## Build Configuration

When setting up the project in Cloudflare Pages, use the following configuration:

- **Build command**: `cd docs && npm run build`
- **Build output directory**: `docs/.vitepress/dist`
- **Root directory**: `/`

## Configuration Notes

The VitePress configuration includes `ignoreDeadLinks: true` to prevent build failures due to links that are valid in the context of the full project but not within the documentation site. This is normal and expected for this project structure.

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