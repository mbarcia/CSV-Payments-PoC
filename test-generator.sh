#!/bin/bash

#
# Copyright (c) 2023-2025 Mariano Barcia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This script sends test input to the interactive template generator
# Format: app name, package name, step name, cardinality, input type, input fields, output type, output fields

cat << EOF | java -jar /Users/mari/IdeaProjects/CSV-Payments-PoC/template-generator/target/template-generator-1.0.0.jar -o /tmp/test-generated-app
Test Pipeline App
io.github.mbarcia.test
Process Customer
1
CustomerInput
name string
email string

CustomerOutput
status string
processedAt string


EOF