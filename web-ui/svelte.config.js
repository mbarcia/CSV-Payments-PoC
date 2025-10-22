/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {sveltekit} from '@sveltejs/kit/vite';
import {defineConfig} from 'vite';
import adapter from '@sveltejs/adapter-static';

export default defineConfig({
	plugins: [sveltekit()],
	build: {
		minify: 'esbuild', // fast and low memory
		rollupOptions: {
			output: {
				// Reduce memory usage during build
				chunkFileNames: 'assets/[name]-[hash].js',
				entryFileNames: 'assets/[name]-[hash].js',
			},
		},
		// Reduce memory usage
		target: 'es2020',
		// Limit memory usage during build
		ssr: false,
		assetsInlineLimit: 0 // prevents embedding large assets in JS bundles
	},
	kit: {
		adapter: adapter({
			pages: 'build',
			assets: 'build',
			fallback: '404.html',
			precompress: false,
			strict: false
		}),
		prerender: {
			concurrency: 1, // Reduce concurrency to save memory
			handleHttpError: ({ path, referrer, message }) => {
				// Ignore favicon errors
				if (path === '/favicon.png') {
					return;
				}
				// Otherwise, fail the build
				throw new Error(message);
			}
		}
	}
});