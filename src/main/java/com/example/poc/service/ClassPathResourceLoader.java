package com.example.poc.service;

import org.springframework.stereotype.Component;

import java.net.URL;

// Default implementation using ClassLoader
@Component
public class ClassPathResourceLoader implements ResourceLoader {
    @Override
    public URL getResource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}
