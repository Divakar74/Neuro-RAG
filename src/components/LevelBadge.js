import React from 'react';

const LevelBadge = ({ level }) => {
  const levelColors = {
    1: 'bg-red-500',
    2: 'bg-orange-500',
    3: 'bg-yellow-500',
    4: 'bg-green-500',
    5: 'bg-blue-500',
  };

  return (
    <span
      className={`inline-block px-3 py-1 rounded-full text-white font-semibold text-sm ${levelColors[level] || 'bg-gray-400'}`}
    >
      Level {level}
    </span>
  );
};

export default LevelBadge;
