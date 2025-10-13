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

import React, {useCallback, useState} from 'react';
import {useDropzone} from 'react-dropzone';

export const FileUpload = ({ onFileUpload, isProcessing }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  
  const onDrop = useCallback((acceptedFiles) => {
    console.log('Files dropped:', acceptedFiles.length);
    if (acceptedFiles.length > 0 && !isProcessing) {
      const file = acceptedFiles[0];
      console.log('Processing file:', file.name, file.size, 'bytes');
      onFileUpload(file);
    }
    setIsDragging(false);
  }, [onFileUpload, isProcessing]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/csv': ['.csv']
    },
    disabled: isProcessing,
    onDragEnter: () => setIsDragging(true),
    onDragLeave: () => setIsDragging(false),
    onDropAccepted: () => setIsDragging(false),
    onDropRejected: (rejectedFiles) => {
      console.log('Rejected files:', rejectedFiles);
      setIsDragging(false);
      alert('Please upload a valid CSV file.');
    }
  });

  return (
    <div className="file-upload">
      <div 
        {...getRootProps()} 
        className={`dropzone ${isDragActive ? 'drag-active' : ''} ${isDragging ? 'dragging' : ''} ${isProcessing ? 'processing' : ''}`}
      >
        <input {...getInputProps()} />
        <div className="upload-content">
          {isProcessing ? (
            <>
              <div className="processing-icon">🔄</div>
              <p>Processing your CSV file...</p>
            </>
          ) : (
            <>
              <div className="upload-icon">📁</div>
              <p>Drag & drop a CSV file here, or click to select</p>
              <button className="upload-button">Select CSV File</button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};