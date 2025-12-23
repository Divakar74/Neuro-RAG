import React, { useState } from 'react';
import { X } from 'lucide-react';
import { useUser } from '../contexts/UserContext';

const AuthModal = ({ isOpen, onClose, mode = 'login' }) => {
  const { login, register } = useUser();
  const [activeTab, setActiveTab] = useState(mode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (activeTab === 'login') {
        await login({ email, password });
      } else {
        await register({ email, password });
      }
      onClose();
    } catch (err) {
      setError(err?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex space-x-2">
            <button
              onClick={() => setActiveTab('login')}
              className={`px-3 py-1 rounded ${activeTab === 'login' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-700'}`}
            >
              Login
            </button>
            <button
              onClick={() => setActiveTab('signup')}
              className={`px-3 py-1 rounded ${activeTab === 'signup' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-700'}`}
            >
              Sign up
            </button>
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
            <X className="w-5 h-5" />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {error && (
            <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>
          )}
          <div>
            <label className="block text-sm text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-700 mb-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 text-white rounded py-2 font-semibold disabled:opacity-50"
          >
            {loading ? 'Please wait…' : activeTab === 'login' ? 'Login' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AuthModal;











