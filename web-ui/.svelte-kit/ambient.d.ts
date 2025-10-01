
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

// this file is generated — do not edit it


/// <reference types="@sveltejs/kit" />

/**
 * Environment variables [loaded by Vite](https://vitejs.dev/guide/env-and-mode.html#env-files) from `.env` files and `process.env`. Like [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), this module cannot be imported into client-side code. This module only includes variables that _do not_ begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) _and do_ start with [`config.kit.env.privatePrefix`](https://svelte.dev/docs/kit/configuration#env) (if configured).
 * 
 * _Unlike_ [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), the values exported from this module are statically injected into your bundle at build time, enabling optimisations like dead code elimination.
 * 
 * ```ts
 * import { API_KEY } from '$env/static/private';
 * ```
 * 
 * Note that all environment variables referenced in your code should be declared (for example in an `.env` file), even if they don't have a value until the app is deployed:
 * 
 * ```
 * MY_FEATURE_FLAG=""
 * ```
 * 
 * You can override `.env` values from the command line like so:
 * 
 * ```sh
 * MY_FEATURE_FLAG="enabled" npm run dev
 * ```
 */
declare module '$env/static/private' {
	export const PATH: string;
	export const FORCE_COLOR: string;
	export const IJ_RESTARTER_LOG: string;
	export const JAVA_HOME: string;
	export const HOMEBREW_PREFIX: string;
	export const DEBUG_COLORS: string;
	export const COMMAND_MODE: string;
	export const npm_config_color: string;
	export const MOCHA_COLORS: string;
	export const COLORTERM: string;
	export const LOGNAME: string;
	export const HOMEBREW_REPOSITORY: string;
	export const XPC_SERVICE_NAME: string;
	export const PWD: string;
	export const INFOPATH: string;
	export const __CFBundleIdentifier: string;
	export const SHELL: string;
	export const PAGER: string;
	export const GEMINI_API_KEY: string;
	export const LSCOLORS: string;
	export const OLDPWD: string;
	export const HOMEBREW_CELLAR: string;
	export const USER: string;
	export const ZSH: string;
	export const TMPDIR: string;
	export const P9K_SSH: string;
	export const SSH_AUTH_SOCK: string;
	export const XPC_FLAGS: string;
	export const __CF_USER_TEXT_ENCODING: string;
	export const LESS: string;
	export const NODE_ENV: string;
	export const LC_CTYPE: string;
	export const LS_COLORS: string;
	export const IDEA_INITIAL_DIRECTORY: string;
	export const HOME: string;
}

/**
 * Similar to [`$env/static/private`](https://svelte.dev/docs/kit/$env-static-private), except that it only includes environment variables that begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) (which defaults to `PUBLIC_`), and can therefore safely be exposed to client-side code.
 * 
 * Values are replaced statically at build time.
 * 
 * ```ts
 * import { PUBLIC_BASE_URL } from '$env/static/public';
 * ```
 */
declare module '$env/static/public' {
	
}

/**
 * This module provides access to runtime environment variables, as defined by the platform you're running on. For example if you're using [`adapter-node`](https://github.com/sveltejs/kit/tree/main/packages/adapter-node) (or running [`vite preview`](https://svelte.dev/docs/kit/cli)), this is equivalent to `process.env`. This module only includes variables that _do not_ begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) _and do_ start with [`config.kit.env.privatePrefix`](https://svelte.dev/docs/kit/configuration#env) (if configured).
 * 
 * This module cannot be imported into client-side code.
 * 
 * ```ts
 * import { env } from '$env/dynamic/private';
 * console.log(env.DEPLOYMENT_SPECIFIC_VARIABLE);
 * ```
 * 
 * > [!NOTE] In `dev`, `$env/dynamic` always includes environment variables from `.env`. In `prod`, this behavior will depend on your adapter.
 */
declare module '$env/dynamic/private' {
	export const env: {
		PATH: string;
		FORCE_COLOR: string;
		IJ_RESTARTER_LOG: string;
		JAVA_HOME: string;
		HOMEBREW_PREFIX: string;
		DEBUG_COLORS: string;
		COMMAND_MODE: string;
		npm_config_color: string;
		MOCHA_COLORS: string;
		COLORTERM: string;
		LOGNAME: string;
		HOMEBREW_REPOSITORY: string;
		XPC_SERVICE_NAME: string;
		PWD: string;
		INFOPATH: string;
		__CFBundleIdentifier: string;
		SHELL: string;
		PAGER: string;
		GEMINI_API_KEY: string;
		LSCOLORS: string;
		OLDPWD: string;
		HOMEBREW_CELLAR: string;
		USER: string;
		ZSH: string;
		TMPDIR: string;
		P9K_SSH: string;
		SSH_AUTH_SOCK: string;
		XPC_FLAGS: string;
		__CF_USER_TEXT_ENCODING: string;
		LESS: string;
		NODE_ENV: string;
		LC_CTYPE: string;
		LS_COLORS: string;
		IDEA_INITIAL_DIRECTORY: string;
		HOME: string;
		[key: `PUBLIC_${string}`]: undefined;
		[key: `${string}`]: string | undefined;
	}
}

/**
 * Similar to [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), but only includes variables that begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) (which defaults to `PUBLIC_`), and can therefore safely be exposed to client-side code.
 * 
 * Note that public dynamic environment variables must all be sent from the server to the client, causing larger network requests — when possible, use `$env/static/public` instead.
 * 
 * ```ts
 * import { env } from '$env/dynamic/public';
 * console.log(env.PUBLIC_DEPLOYMENT_SPECIFIC_VARIABLE);
 * ```
 */
declare module '$env/dynamic/public' {
	export const env: {
		[key: `PUBLIC_${string}`]: string | undefined;
	}
}
