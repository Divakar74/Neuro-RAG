import React, { useState, useRef } from 'react';
import { Upload, FileText, X, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';

const ResumeUploader = ({ onUpload, loading }) => {
  const [file, setFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null); // null, 'uploading', 'success', 'error'
  const fileInputRef = useRef(null);

  const acceptedTypes = ['.pdf', '.doc', '.docx', '.txt'];
  const maxFileSize = 10 * 1024 * 1024; // 10MB

  const validateFile = (file) => {
    if (!file) return 'Please select a file';

    const fileExtension = '.' + file.name.split('.').pop().toLowerCase();
    if (!acceptedTypes.includes(fileExtension)) {
      return 'Please upload a PDF, DOC, DOCX, or TXT file';
    }

    if (file.size > maxFileSize) {
      return 'File size must be less than 10MB';
    }

    return null;
  };

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      const error = validateFile(selectedFile);
      if (error) {
        setUploadStatus('error');
        setFile({ ...selectedFile, error });
      } else {
        setFile(selectedFile);
        setUploadStatus(null);
      }
    }
  };

  const handleUpload = async () => {
    if (!file || file.error) return;

    setUploadStatus('uploading');
    try {
      await onUpload(file);
      setUploadStatus('success');
    } catch (error) {
      setUploadStatus('error');
      setFile({ ...file, error: 'Upload failed. Please try again.' });
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const droppedFile = e.dataTransfer.files[0];
      const error = validateFile(droppedFile);
      if (error) {
        setUploadStatus('error');
        setFile({ ...droppedFile, error });
      } else {
        setFile(droppedFile);
        setUploadStatus(null);
      }
    }
  };

  const removeFile = () => {
    setFile(null);
    setUploadStatus(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="w-full max-w-2xl mx-auto">
      <div className="bg-primary-100 rounded-2xl shadow-xl border border-primary-300 overflow-hidden">
        {/* Header */}
        <div className="bg-primary-700 p-6 text-primary-100">
          <div className="flex items-center justify-center mb-2">
            <Upload className="w-8 h-8 mr-3" />
            <h3 className="text-xl font-bold">Upload Your Resume</h3>
          </div>
          <p className="text-center text-primary-300">
            Drag & drop your resume or click to browse
          </p>
        </div>

        {/* Upload Area */}
        <div className="p-8">
          <div
            className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-all duration-300 ${
              dragActive
                ? 'border-primary-400 bg-primary-50 scale-105'
                : file && !file.error
                ? 'border-green-300 bg-green-50'
                : file && file.error
                ? 'border-red-300 bg-red-50'
                : 'border-primary-300 hover:border-primary-400 hover:bg-primary-50'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf,.doc,.docx,.txt"
              onChange={handleFileChange}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              disabled={loading || uploadStatus === 'uploading'}
            />

            {file && !file.error ? (
              <div className="space-y-4">
                <div className="flex items-center justify-center">
                  <div className="bg-green-100 rounded-full p-3">
                    <FileText className="w-8 h-8 text-green-600" />
                  </div>
                </div>
                <div>
                  <p className="font-semibold text-primary-900">{file.name}</p>
                  <p className="text-sm text-primary-700">{formatFileSize(file.size)}</p>
                </div>
                <div className="flex items-center justify-center space-x-2">
                  {uploadStatus === 'success' && (
                    <CheckCircle className="w-5 h-5 text-green-600" />
                  )}
                  {uploadStatus === 'uploading' && (
                    <Loader2 className="w-5 h-5 text-primary-600 animate-spin" />
                  )}
                  <button
                    onClick={removeFile}
                    className="text-red-500 hover:text-red-700 transition-colors"
                    disabled={uploadStatus === 'uploading'}
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>
            ) : file && file.error ? (
              <div className="space-y-4">
                <div className="flex items-center justify-center">
                  <div className="bg-red-100 rounded-full p-3">
                    <AlertCircle className="w-8 h-8 text-red-600" />
                  </div>
                </div>
                <div>
                  <p className="font-semibold text-primary-900">{file.name}</p>
                  <p className="text-sm text-red-600">{file.error}</p>
                </div>
                <button
                  onClick={removeFile}
                  className="text-red-500 hover:text-red-700 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-center">
                  <div className="bg-primary-100 rounded-full p-4">
                    <Upload className="w-12 h-12 text-primary-600" />
                  </div>
                </div>
                <div>
                  <p className="text-lg font-medium text-primary-900">
                    {dragActive ? 'Drop your resume here' : 'Choose a file or drag it here'}
                  </p>
                  <p className="text-sm text-primary-700 mt-1">
                    PDF, DOC, DOCX, or TXT (max 10MB)
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Upload Button */}
          {file && !file.error && (
            <div className="mt-6 text-center">
              <button
                onClick={handleUpload}
                disabled={loading || uploadStatus === 'uploading' || uploadStatus === 'success'}
                className="bg-primary-700 text-primary-100 font-semibold py-3 px-8 rounded-lg hover:bg-primary-900 transition-all duration-200 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                {uploadStatus === 'uploading' ? (
                  <div className="flex items-center">
                    <Loader2 className="w-5 h-5 animate-spin mr-2" />
                    Uploading...
                  </div>
                ) : uploadStatus === 'success' ? (
                  <div className="flex items-center">
                    <CheckCircle className="w-5 h-5 mr-2" />
                    Uploaded Successfully!
                  </div>
                ) : (
                  'Upload Resume'
                )}
              </button>
            </div>
          )}

          {/* Status Messages */}
          {uploadStatus === 'error' && file?.error && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
              <div className="flex items-center">
                <AlertCircle className="w-5 h-5 text-red-600 mr-2" />
                <p className="text-red-800 text-sm">{file.error}</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResumeUploader;
