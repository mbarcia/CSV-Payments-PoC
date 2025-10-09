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
import java.text.MessageFormat;
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
                        Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                    return new SimpleJavaFileObject(URI.create(MessageFormat.format("bytes:///{0}{1}", className, kind.extension)), kind) {
                        @Override
                        public OutputStream openOutputStream() {
                            return byteStream;
                        }
                    };
                }
            };

            // Get the classpath from the current thread's context class loader
            String classpath = System.getProperty("java.class.path");
            
            // Try to build a more comprehensive classpath that includes the current runtime
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            if (currentClassLoader != null) {
                // If we have a custom classpath, use it
                classpath = System.getProperty("java.class.path");
                if (classpath == null || classpath.trim().isEmpty()) {
                    // Fallback to the classpath from the class loader
                    classpath = System.getProperty("java.class.path", "");
                }
            }
            
            // Add classpath options when calling the compiler
            List<String> options = new ArrayList<>();
            if (classpath != null && !classpath.isEmpty()) {
                options.add("-cp");
                options.add(classpath);
            }
            
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, Collections.singletonList(file));

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                diagnostics.getDiagnostics().forEach(d ->
                        System.err.println(MessageFormat.format("Compilation error: {0}", d.getMessage(Locale.ENGLISH))));
                throw new RuntimeException(MessageFormat.format("In-memory compilation failed for {0}", className));
            }

            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {
        private final String code;
        protected JavaSourceFromString(String name, String code) {
            super(URI.create(MessageFormat.format("string:///{0}{1}", name.replace('.', '/'), Kind.SOURCE.extension)), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
