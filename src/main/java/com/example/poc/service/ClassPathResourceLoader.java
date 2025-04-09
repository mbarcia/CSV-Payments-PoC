package com.example.poc.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;

// Default implementation using ClassLoader
@ApplicationScoped
public class ClassPathResourceLoader implements ResourceLoader {
    @Override
    public URL getResource(String path) {
        return getClass().getClassLoader().getResource(path);
    }
}
