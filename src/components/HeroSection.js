import React from 'react';
import { Trophy, Target, TrendingUp, Star, Award } from 'lucide-react';

const HeroSection = ({ skillData }) => {
  const totalSkills = skillData?.length || 0;
  const averageLevel = skillData?.length > 0
    ? Math.round(skillData.reduce((sum, skill) => sum + skill.level, 0) / skillData.length)
    : 0;
  const strengthsCount = skillData?.filter(skill => skill.level >= 4).length || 0;

  const getMotivationalMessage = () => {
    if (averageLevel >= 4) return "Outstanding! You're a skill master!";
    if (averageLevel >= 3) return "Great progress! Keep building on your strengths!";
    if (averageLevel >= 2) return "Good foundation! Ready to level up!";
    return "Every expert was once a beginner. Let's grow together!";
  };

  const getMotivationalSubtext = () => {
    if (averageLevel >= 4) return "Your expertise shines through. Continue to innovate and inspire!";
    if (averageLevel >= 3) return "You're on the right path. Focus on mastery in your key areas.";
    if (averageLevel >= 2) return "Solid foundation built. Time to accelerate your growth!";
    return "Your journey to mastery starts now. Embrace the learning process!";
  };

  return (
    <div className="relative bg-primary-100 p-8 rounded-3xl shadow-lg text-primary-900">
      <div className="relative z-10">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between mb-8">
          <div className="mb-6 lg:mb-0">
            <div className="flex items-center mb-4">
              <Award className="w-8 h-8 mr-3 text-primary-700" />
              <h1 className="text-4xl font-bold text-primary-900">
                Assessment Complete!
              </h1>
            </div>
            <p className="text-xl mb-2 font-semibold">{getMotivationalMessage()}</p>
            <p className="text-lg">{getMotivationalSubtext()}</p>
          </div>

          <div className="text-center lg:text-right">
            <div className="flex items-center justify-center lg:justify-end mb-2">
              <Star className="w-8 h-8 text-primary-700 mr-3" />
              <span className="text-6xl font-bold text-primary-900">
                {averageLevel}
              </span>
              <span className="text-3xl ml-2">/5</span>
            </div>
            <div className="text-sm uppercase tracking-wider font-medium text-primary-700">Average Proficiency</div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-primary-200 rounded-xl p-6 text-center">
            <div className="flex justify-center mb-4">
              <div className="rounded-full p-4 bg-primary-300">
                <Target className="w-8 h-8 text-primary-900" />
              </div>
            </div>
            <div className="text-4xl font-bold mb-2 text-primary-900">{totalSkills}</div>
            <div className="text-sm uppercase tracking-wide font-medium text-primary-700">Skills Assessed</div>
            <div className="mt-2 h-1 bg-primary-300 rounded-full">
              <div className="h-1 bg-primary-700 rounded-full" style={{ width: '100%' }}></div>
            </div>
          </div>

          <div className="bg-primary-200 rounded-xl p-6 text-center">
            <div className="flex justify-center mb-4">
              <div className="rounded-full p-4 bg-primary-300">
                <TrendingUp className="w-8 h-8 text-primary-900" />
              </div>
            </div>
            <div className="text-4xl font-bold mb-2 text-primary-900">{averageLevel}/5</div>
            <div className="text-sm uppercase tracking-wide font-medium text-primary-700">Average Level</div>
            <div className="mt-2 h-1 bg-primary-300 rounded-full">
              <div className="h-1 bg-primary-700 rounded-full" style={{ width: `${(averageLevel / 5) * 100}%` }}></div>
            </div>
          </div>

          <div className="bg-primary-200 rounded-xl p-6 text-center">
            <div className="flex justify-center mb-4">
              <div className="rounded-full p-4 bg-primary-300">
                <Trophy className="w-8 h-8 text-primary-900" />
              </div>
            </div>
            <div className="text-4xl font-bold mb-2 text-primary-900">{strengthsCount}</div>
            <div className="text-sm uppercase tracking-wide font-medium text-primary-700">Core Strengths</div>
            <div className="mt-2 h-1 bg-primary-300 rounded-full">
              <div className="h-1 bg-primary-700 rounded-full" style={{ width: totalSkills > 0 ? `${(strengthsCount / totalSkills) * 100}%` : '0%' }}></div>
            </div>
          </div>
        </div>

        {/* Overall Progress Section */}
        <div className="mt-8 bg-primary-200 rounded-xl p-6">
          <div className="flex justify-between items-center mb-4">
            <span className="text-lg font-semibold text-primary-900">Overall Progress</span>
            <span className="text-2xl font-bold text-primary-900">{Math.round((averageLevel / 5) * 100)}%</span>
          </div>
          <div className="w-full bg-primary-300 rounded-full h-4 overflow-hidden">
            <div
              className="bg-primary-700 h-4 rounded-full transition-all duration-2000 ease-out shadow-inner"
              style={{ width: `${(averageLevel / 5) * 100}%` }}
            ></div>
          </div>
          <div className="flex justify-between text-sm mt-2 text-primary-700">
            <span>Beginner</span>
            <span>Intermediate</span>
            <span>Expert</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HeroSection;
