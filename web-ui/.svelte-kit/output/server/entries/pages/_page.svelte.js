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

import {
  c as create_ssr_component,
  d as createEventDispatcher,
  e as escape,
  f as add_attribute,
  h as each,
  v as validate_component
} from "../../chunks/ssr.js";
import "file-saver";
import "js-yaml";

const StepArrow = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let { step } = $$props;
  let { index } = $$props;
  let { isSelected = false } = $$props;
  createEventDispatcher();
  let width = Math.max(200, (step.name?.length || 10) * 10);
  let height = 120;
  if (step.inputFields?.length > 0 || step.outputFields?.length > 0) {
    height += Math.max(step.inputFields?.length || 0, step.outputFields?.length || 0) * 20;
  }
  if ($$props.step === void 0 && $$bindings.step && step !== void 0) $$bindings.step(step);
  if ($$props.index === void 0 && $$bindings.index && index !== void 0) $$bindings.index(index);
  if ($$props.isSelected === void 0 && $$bindings.isSelected && isSelected !== void 0) $$bindings.isSelected(isSelected);
  return `<div class="relative" style="${"width: " + escape(width, true) + "px; height: " + escape(height, true) + "px;"}"><svg${add_attribute("width", width, 0)}${add_attribute("height", height, 0)} class="cursor-pointer" role="button" tabindex="0">${step.cardinality === "ONE_TO_ONE" || step.cardinality === "SIDE_EFFECT" ? ` <defs><linearGradient${add_attribute("id", `arrow-gradient-${index}`, 0)} x1="0%" y1="0%" x2="100%" y2="0%"><stop offset="0%" style="${"stop-color:" + escape(isSelected ? "#4F46E5" : "#4F46E5", true) + ";stop-opacity:1"}"></stop><stop offset="100%" style="${"stop-color:" + escape(isSelected ? "#6366F1" : "#818CF8", true) + ";stop-opacity:1"}"></stop></linearGradient></defs> <polygon${add_attribute("points", `20,${height / 2 - 30} ${width - 40},${height / 2 - 30} ${width - 20},${height / 2} ${width - 40},${height / 2 + 30} 20,${height / 2 + 30}`, 0)}${add_attribute("fill", `url(#arrow-gradient-${index})`, 0)}${add_attribute("stroke", isSelected ? "#312E81" : "#1E3A8A", 0)} stroke-width="2" class="step-arrow"></polygon>` : `${step.cardinality === "EXPANSION" ? ` <defs><linearGradient${add_attribute("id", `arrow-gradient-${index}`, 0)} x1="0%" y1="0%" x2="100%" y2="0%"><stop offset="0%" style="${"stop-color:" + escape(isSelected ? "#10B981" : "#10B981", true) + ";stop-opacity:1"}"></stop><stop offset="100%" style="${"stop-color:" + escape(isSelected ? "#34D399" : "#6EE7B7", true) + ";stop-opacity:1"}"></stop></linearGradient></defs> <polygon${add_attribute("points", `20,${height / 2 - 30} ${width - 60},${height / 2 - 40} ${width - 20},${height / 2} ${width - 60},${height / 2 + 40} 20,${height / 2 + 30}`, 0)}${add_attribute("fill", `url(#arrow-gradient-${index})`, 0)}${add_attribute("stroke", isSelected ? "#065F46" : "#047857", 0)} stroke-width="2" class="step-arrow"></polygon>` : `${step.cardinality === "REDUCTION" ? ` <defs><linearGradient${add_attribute("id", `arrow-gradient-${index}`, 0)} x1="0%" y1="0%" x2="100%" y2="0%"><stop offset="0%" style="${"stop-color:" + escape(isSelected ? "#EF4444" : "#EF4444", true) + ";stop-opacity:1"}"></stop><stop offset="100%" style="${"stop-color:" + escape(isSelected ? "#F87171" : "#FCA5A5", true) + ";stop-opacity:1"}"></stop></linearGradient></defs> <polygon${add_attribute("points", `60,${height / 2 - 40} ${width - 20},${height / 2 - 30} ${width - 20},${height / 2 + 30} 60,${height / 2 + 40}`, 0)}${add_attribute("fill", `url(#arrow-gradient-${index})`, 0)}${add_attribute("stroke", isSelected ? "#7F1D1D" : "#DC2626", 0)} stroke-width="2" class="step-arrow"></polygon> <polygon${add_attribute("points", `20,${height / 2 - 15} 60,${height / 2 - 40} 60,${height / 2 + 40} 20,${height / 2 + 15}`, 0)}${add_attribute("fill", `url(#arrow-gradient-${index})`, 0)}${add_attribute("stroke", isSelected ? "#7F1D1D" : "#DC2626", 0)} stroke-width="2"></polygon>` : ``}`}`}<text${add_attribute("x", width / 2, 0)}${add_attribute("y", height / 2 + 5, 0)} text-anchor="middle" fill="white" font-size="14" font-weight="bold" class="step-name">${escape(step.name)}</text></svg></div>`;
});
const Connector = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let { stepIndex } = $$props;
  let { sharedType } = $$props;
  createEventDispatcher();
  if ($$props.stepIndex === void 0 && $$bindings.stepIndex && stepIndex !== void 0) $$bindings.stepIndex(stepIndex);
  if ($$props.sharedType === void 0 && $$bindings.sharedType && sharedType !== void 0) $$bindings.sharedType(sharedType);
  return `<div class="flex justify-center items-center mx-4"><svg width="40" height="40" class="cursor-pointer" role="button" tabindex="0"><line x1="0" y1="20" x2="40" y2="20" stroke="#9CA3AF" stroke-width="2" stroke-dasharray="4"></line><polygon points="35,15 40,20 35,25" fill="#9CA3AF"></polygon><circle cx="20" cy="20" r="6" fill="#9CA3AF" class="opacity-0"></circle></svg></div>`;
});
const ConfirmationDialog = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  createEventDispatcher();
  let { visible = false } = $$props;
  let { title = "Confirm Action" } = $$props;
  let { message = "" } = $$props;
  let { confirmText = "Confirm" } = $$props;
  let { cancelText = "Cancel" } = $$props;
  let { warning = false } = $$props;
  let container;
  if ($$props.visible === void 0 && $$bindings.visible && visible !== void 0) $$bindings.visible(visible);
  if ($$props.title === void 0 && $$bindings.title && title !== void 0) $$bindings.title(title);
  if ($$props.message === void 0 && $$bindings.message && message !== void 0) $$bindings.message(message);
  if ($$props.confirmText === void 0 && $$bindings.confirmText && confirmText !== void 0) $$bindings.confirmText(confirmText);
  if ($$props.cancelText === void 0 && $$bindings.cancelText && cancelText !== void 0) $$bindings.cancelText(cancelText);
  if ($$props.warning === void 0 && $$bindings.warning && warning !== void 0) $$bindings.warning(warning);
  return `${visible ? `<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"><div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md"${add_attribute("this", container, 0)}><div class="flex justify-between items-center mb-4"><h3 class="text-lg font-semibold">${escape(title)}</h3> <button class="text-gray-500 hover:text-gray-700" data-svelte-h="svelte-1oxrk9i">✕</button></div> <div${add_attribute(
    "class",
    warning ? "mb-6 p-4 bg-yellow-50 border-l-4 border-yellow-400" : "mb-6",
    0
  )}><p${add_attribute("class", warning ? "text-yellow-700" : "text-gray-700", 0)}>${escape(message)}</p></div> <div class="flex justify-end space-x-2"><button class="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-100">${escape(cancelText)}</button> <button${add_attribute(
    "class",
    warning ? "px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700" : "px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700",
    0
  )}>${escape(confirmText)}</button></div></div></div>` : ``}`;
});
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let config = {
    appName: "My Pipeline App",
    basePackage: "com.example.mypipeline",
    steps: []
  };
  let showConfirmationDialog = false;
  let confirmationTitle = "";
  let confirmationMessage = "";
  let confirmationWarning = false;
  const cardinalityOptions = [
    {
      value: "ONE_TO_ONE",
      label: "1-1 (One-to-One)"
    },
    {
      value: "EXPANSION",
      label: "Expansion (1-Many)"
    },
    {
      value: "REDUCTION",
      label: "Reduction (Many-1)"
    },
    {
      value: "SIDE_EFFECT",
      label: "Side-effect"
    }
  ];
  return `<main class="flex h-screen bg-gray-100"> <div class="w-64 bg-white shadow-lg p-4 overflow-y-auto flex flex-col"><h1 class="text-xl font-bold mb-6 text-center" data-svelte-h="svelte-1og4end">Pipeline Canvas</h1> <div class="flex-grow"><button class="w-full mb-3 px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm" data-svelte-h="svelte-te2vl6">Download YAML</button> <div class="mb-4"><label class="block text-sm font-medium text-gray-700 mb-1" data-svelte-h="svelte-73ghv2">Upload Config:</label> <input type="file" accept=".yaml,.yml" class="w-full text-sm"></div></div> <div class="mt-auto pt-4 border-t border-gray-200"><div class="mb-3"><label class="block text-sm font-medium text-gray-700 mb-1" data-svelte-h="svelte-1ezmfrp">App Name</label> <input type="text" class="w-full px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"${add_attribute("value", config.appName, 0)}></div> <div class="mb-3"><label class="block text-sm font-medium text-gray-700 mb-1" data-svelte-h="svelte-1616tty">Base Package</label> <input type="text" class="w-full px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"${add_attribute("value", config.basePackage, 0)}></div></div></div>  <div class="flex-1 p-4 overflow-auto"><div class="flex flex-col items-center min-h-full">${config.steps.length === 0 ? `<div class="flex flex-col items-center justify-center h-full text-gray-500"><p class="text-lg mb-4" data-svelte-h="svelte-1vs4fbq">No steps yet. Add your first step to get started.</p> <button class="w-16 h-16 rounded-full bg-blue-600 text-white text-3xl flex items-center justify-center hover:bg-blue-700 shadow-lg" data-svelte-h="svelte-1jhco5">+</button></div>` : `<div class="flex items-center mt-8">${each(config.steps, (step, index) => {
    return `<div class="flex flex-col items-center">${validate_component(StepArrow, "StepArrow").$$render($$result, { step, index }, {}, {})}  <div class="mt-1 flex flex-col items-center"><select class="text-xs px-1 py-1 border border-gray-300 rounded">${each(cardinalityOptions, (option) => {
      return `<option${add_attribute("value", option.value, 0)}>${escape(option.label)}</option>`;
    })}</select> <div class="flex space-x-1 mt-1"><input type="text" class="text-xs w-20 px-1 py-1 border border-gray-300 rounded"${add_attribute("value", step.name, 0)}> <button class="text-xs px-1.5 py-1 bg-red-600 text-white rounded hover:bg-red-700" data-svelte-h="svelte-1e4w7ax">Del
                  </button></div> </div></div>  ${index < config.steps.length - 1 ? `${validate_component(Connector, "Connector").$$render(
      $$result,
      {
        stepIndex: index + 1,
        sharedType: step.outputTypeName
      },
      {},
      {}
    )}` : ` <button class="w-12 h-12 rounded-full bg-blue-600 text-white text-2xl flex items-center justify-center hover:bg-blue-700 shadow-lg ml-4 self-center" title="Add Step" data-svelte-h="svelte-w7cioy">+
              </button>`}`;
  })}</div>`}</div></div>  ${``}  ${validate_component(ConfirmationDialog, "ConfirmationDialog").$$render(
    $$result,
    {
      visible: showConfirmationDialog,
      title: confirmationTitle,
      message: confirmationMessage,
      warning: confirmationWarning,
      confirmText: "Yes, Convert",
      cancelText: "Cancel"
    },
    {},
    {}
  )}</main>`;
});
export {
  Page as default
};
