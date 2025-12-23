import React from 'react';

const RoleSelector = ({ onSelectRole }) => {
  const roles = [
    'Software Engineer',
    'Data Scientist',
    'Product Manager',
    'UX Designer',
    'DevOps Engineer',
    'Full Stack Developer',
    'Other'
  ];

  return (
    <div className="my-6">
      <h3 className="text-lg font-semibold mb-4">Select Your Target Role</h3>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {roles.map((role) => (
          <button
            key={role}
            onClick={() => onSelectRole(role)}
            className="bg-gray-100 hover:bg-blue-100 text-gray-800 font-medium py-3 px-4 rounded border hover:border-blue-300 transition"
          >
            {role}
          </button>
        ))}
      </div>
    </div>
  );
};

export default RoleSelector;
