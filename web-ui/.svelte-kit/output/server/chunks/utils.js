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

const text_encoder = new TextEncoder();
const text_decoder = new TextDecoder();
function get_relative_path(from, to) {
  const from_parts = from.split(/[/\\]/);
  const to_parts = to.split(/[/\\]/);
  from_parts.pop();
  while (from_parts[0] === to_parts[0]) {
    from_parts.shift();
    to_parts.shift();
  }
  let i = from_parts.length;
  while (i--) from_parts[i] = "..";
  return from_parts.concat(to_parts).join("/");
}
function base64_encode(bytes) {
  if (globalThis.Buffer) {
    return globalThis.Buffer.from(bytes).toString("base64");
  }
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}
function base64_decode(encoded) {
  if (globalThis.Buffer) {
    const buffer = globalThis.Buffer.from(encoded, "base64");
    return new Uint8Array(buffer);
  }
  const binary = atob(encoded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
function convert_formdata(data) {
  const result = /* @__PURE__ */ Object.create(null);
  for (let key of data.keys()) {
    const is_array = key.endsWith("[]");
    let values = data.getAll(key);
    if (is_array) key = key.slice(0, -2);
    if (values.length > 1 && !is_array) {
      throw new Error(`Form cannot contain duplicated keys — "${key}" has ${values.length} values`);
    }
    values = values.filter(
      (entry) => typeof entry === "string" || entry.name !== "" || entry.size > 0
    );
    deep_set(result, split_path(key), is_array ? values : values[0]);
  }
  return result;
}
const path_regex = /^[a-zA-Z_$]\w*(\.[a-zA-Z_$]\w*|\[\d+\])*$/;
function split_path(path) {
  if (!path_regex.test(path)) {
    throw new Error(`Invalid path ${path}`);
  }
  return path.split(/\.|\[|\]/).filter(Boolean);
}
function deep_set(object, keys, value) {
  for (let i = 0; i < keys.length - 1; i += 1) {
    const key = keys[i];
    const is_array = /^\d+$/.test(keys[i + 1]);
    if (object[key]) {
      if (is_array !== Array.isArray(object[key])) {
        throw new Error(`Invalid array key ${keys[i + 1]}`);
      }
    } else {
      object[key] ??= is_array ? [] : /* @__PURE__ */ Object.create(null);
    }
    object = object[key];
  }
  object[keys[keys.length - 1]] = value;
}
function flatten_issues(issues) {
  const result = {};
  for (const issue of issues) {
    const normalized = { name: "", path: [], message: issue.message };
    (result.$ ??= []).push(normalized);
    let name = "";
    if (issue.path !== void 0) {
      for (const segment of issue.path) {
        const key = (
          /** @type {string | number} */
          typeof segment === "object" ? segment.key : segment
        );
        normalized.path.push(key);
        if (typeof key === "number") {
          name += `[${key}]`;
        } else if (typeof key === "string") {
          name += name === "" ? key : "." + key;
        }
        (result[name] ??= []).push(normalized);
      }
      normalized.name = name;
    }
  }
  return result;
}
const file_transport = {
  encode: (file) => file instanceof File && {
    size: file.size,
    type: file.type,
    name: file.name,
    lastModified: file.lastModified
  },
  decode: (data) => data
};
export {
  text_encoder as a,
  base64_encode as b,
  convert_formdata as c,
  flatten_issues as d,
  base64_decode as e,
  file_transport as f,
  get_relative_path as g,
  text_decoder as t
};
