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

package io.github.mbarcia.pipeline.processor;

import java.io.*;
import java.net.URI;
import java.util.*;
import javax.tools.*;

public class InMemoryCompiler {

    public static byte[] compile(String className, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler found. Are you running on a JRE instead of a JDK?");
        }

        JavaFileObject file = new JavaSourceFromString(className, sourceCode);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            JavaFileManager fileManager = new ForwardingJavaFileManager<>(
                    compiler.getStandardFileManager(diagnostics, null, null)) {
                @Override
                public JavaFileObject getJavaFileForOutput(
                        Location location, String className, JavaFileObject.Kind kind, FileObject sibling)
                        throws IOException {
                    return new SimpleJavaFileObject(URI.create("bytes:///" + className + kind.extension), kind) {
                        @Override
                        public OutputStream openOutputStream() {
                            return byteStream;
                        }
                    };
                }
            };

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, null, null, Collections.singletonList(file));

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                diagnostics.getDiagnostics().forEach(d ->
                        System.err.println("Compilation error: " + d.getMessage(Locale.ENGLISH)));
                throw new RuntimeException("In-memory compilation failed for " + className);
            }

            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {
        private final String code;
        protected JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
