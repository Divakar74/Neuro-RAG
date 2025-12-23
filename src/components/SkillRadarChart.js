import React from 'react';
import { RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, ResponsiveContainer, Tooltip, Legend } from 'recharts';

const SkillRadarChart = ({ skillData }) => {
  // Transform skill data for radar chart
  const chartData = skillData?.map(skill => ({
    skill: skill.name,
    level: skill.level,
    fullMark: 5
  })) || [];

  return (
    <div className="bg-white p-6 rounded-2xl shadow-xl">
      <h3 className="text-xl font-semibold mb-4 text-gray-800">Skill Proficiency Radar</h3>
      <div className="h-80">
        <ResponsiveContainer width="100%" height="100%">
          <RadarChart data={chartData} outerRadius="70%">
            <defs>
              <linearGradient id="radarFill" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#6366F1" stopOpacity={0.4} />
                <stop offset="100%" stopColor="#A855F7" stopOpacity={0.2} />
              </linearGradient>
            </defs>
            <PolarGrid />
            <PolarAngleAxis
              dataKey="skill"
              tick={{ fontSize: 12, fill: '#6b7280' }}
            />
            <PolarRadiusAxis
              angle={90}
              domain={[0, 5]}
              tick={{ fontSize: 10, fill: '#6b7280' }}
              tickCount={6}
            />
            <Radar
              name="Skill Level"
              dataKey="level"
              stroke="#6366F1"
              fill="url(#radarFill)"
              fillOpacity={1}
              strokeWidth={2}
            />
            <Tooltip cursor={{ stroke: '#CBD5E1', strokeWidth: 1 }} />
            <Legend formatter={() => 'Level (1-5)'} />
          </RadarChart>
        </ResponsiveContainer>
      </div>
      <div className="mt-4 text-sm text-gray-600">
        <p>Scale: 1 = Beginner, 3 = Intermediate, 5 = Expert</p>
      </div>
    </div>
  );
};

export default SkillRadarChart;
