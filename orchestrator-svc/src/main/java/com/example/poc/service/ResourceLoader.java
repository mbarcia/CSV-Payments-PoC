package com.example.poc.service;

import java.net.URL;

// First, create the ResourceLoader interface
public interface ResourceLoader {
  URL getResource(String path);
}
