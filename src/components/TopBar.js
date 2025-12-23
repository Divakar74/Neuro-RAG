import React from 'react';
import { useUser } from '../contexts/UserContext';

const TopBar = () => {
  const { user, logout } = useUser();
  if (!user) return null;
  const initial = (user.email || 'U').charAt(0).toUpperCase();
  return (
    <div className="w-full bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-6 py-3 flex items-center justify-end">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-full bg-indigo-600 text-white flex items-center justify-center font-semibold">
            {initial}
          </div>
          <span className="text-sm text-gray-700">{user.email}</span>
          <button onClick={logout} className="text-sm text-gray-600 hover:text-gray-900">Logout</button>
        </div>
      </div>
    </div>
  );
};

export default TopBar;











