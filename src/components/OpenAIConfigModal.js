import React, { useState, useEffect } from 'react';
import { X, Save, Key, Lightbulb, AlertCircle, CheckCircle } from 'lucide-react';

const OpenAIConfigModal = ({ onClose, sessionId }) => {
  const [apiKey, setApiKey] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [isValid, setIsValid] = useState(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    // Prefer .env value if present; else load saved API key from localStorage
    const envKey = process.env.REACT_APP_OPENAI_API_KEY;
    const savedKey = localStorage.getItem('openai_api_key');
    const initial = envKey || savedKey || '';
    if (initial) {
      setApiKey(initial);
      setIsValid(true);
    }
  }, []);

  const validateApiKey = async (key) => {
    if (!key) return false;
    
    setIsValidating(true);
    try {
      // Simple validation - check if it starts with 'sk-'
      const isValidFormat = key.startsWith('sk-') && key.length > 20;
      setIsValid(isValidFormat);
      return isValidFormat;
    } catch (error) {
      setIsValid(false);
      return false;
    } finally {
      setIsValidating(false);
    }
  };

  const handleSave = async () => {
    const valid = await validateApiKey(apiKey);
    if (valid) {
      localStorage.setItem('openai_api_key', apiKey);
      setSaved(true);
      setTimeout(() => {
        onClose();
      }, 1500);
    }
  };

  const handleKeyChange = (e) => {
    const value = e.target.value;
    setApiKey(value);
    setSaved(false);
    if (value) {
      validateApiKey(value);
    } else {
      setIsValid(null);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full mx-4">
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center">
            <div className="bg-indigo-100 p-2 rounded-lg">
              <Key className="w-5 h-5 text-indigo-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-800 ml-3">AI Configuration</h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <div className="mb-6">
            <p className="text-gray-600 mb-4">
              Configure your OpenAI API key to enable personalized AI suggestions and feedback.
            </p>
            
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
              <div className="flex items-start">
                <Lightbulb className="w-5 h-5 text-blue-600 mt-0.5 mr-3 flex-shrink-0" />
                <div className="text-sm text-blue-800">
                  <p className="font-medium mb-1">Benefits of AI Integration:</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>Personalized skill recommendations</li>
                    <li>Detailed performance analysis</li>
                    <li>Customized learning paths</li>
                    <li>Real-time feedback and suggestions</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                OpenAI API Key
              </label>
              <div className="relative">
                <input
                  type="password"
                  value={apiKey}
                  onChange={handleKeyChange}
                  placeholder="sk-..."
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 ${
                    isValid === false ? 'border-red-300' : 
                    isValid === true ? 'border-green-300' : 'border-gray-300'
                  }`}
                />
                {isValidating && (
                  <div className="absolute right-3 top-2.5">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-indigo-600"></div>
                  </div>
                )}
                {isValid === true && !isValidating && (
                  <div className="absolute right-3 top-2.5">
                    <CheckCircle className="w-4 h-4 text-green-600" />
                  </div>
                )}
                {isValid === false && !isValidating && (
                  <div className="absolute right-3 top-2.5">
                    <AlertCircle className="w-4 h-4 text-red-600" />
                  </div>
                )}
              </div>
              
              {isValid === false && (
                <p className="text-sm text-red-600 mt-1">
                  Invalid API key format. Please check your key.
                </p>
              )}
              
              {isValid === true && (
                <p className="text-sm text-green-600 mt-1">
                  API key format looks good!
                </p>
              )}
            </div>

            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-sm text-gray-600 mb-2">
                <strong>How to get your API key:</strong>
              </p>
              <ol className="text-sm text-gray-600 list-decimal list-inside space-y-1">
                <li>Visit <a href="https://platform.openai.com/api-keys" target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:underline">OpenAI Platform</a></li>
                <li>Sign in or create an account</li>
                <li>Go to API Keys section</li>
                <li>Create a new secret key</li>
                <li>Copy and paste it here</li>
              </ol>
            </div>
          </div>

          <div className="flex items-center justify-between mt-6">
            <button
              onClick={onClose}
              className="px-4 py-2 text-gray-600 hover:text-gray-800 transition"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={!isValid || isValidating}
              className={`px-6 py-2 rounded-lg font-medium transition flex items-center space-x-2 ${
                isValid && !isValidating
                  ? 'bg-indigo-600 hover:bg-indigo-700 text-white'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }`}
            >
              {saved ? (
                <>
                  <CheckCircle className="w-4 h-4" />
                  <span>Saved!</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Save Configuration</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OpenAIConfigModal;


