import React from 'react';
import { Smile, Coffee, Heart, Zap, Star } from 'lucide-react';

const MotivationBanner = () => {
  const motivationalQuotes = [
    {
      text: "Every step you take brings you closer to mastery. Take breaks, stay hydrated, and keep your passion alive.",
      icon: Heart,
      color: "text-pink-500"
    },
    {
      text: "Learning is a journey, not a destination. Celebrate your progress and keep pushing forward!",
      icon: Zap,
      color: "text-yellow-500"
    },
    {
      text: "Your brain is like a muscle - it gets stronger with consistent training. You're building something amazing!",
      icon: Star,
      color: "text-purple-500"
    }
  ];

  const [currentQuote, setCurrentQuote] = React.useState(0);

  React.useEffect(() => {
    const interval = setInterval(() => {
      setCurrentQuote((prev) => (prev + 1) % motivationalQuotes.length);
    }, 8000); // Change quote every 8 seconds

    return () => clearInterval(interval);
  }, []);

  const QuoteIcon = motivationalQuotes[currentQuote].icon;

  return (
    <div className="relative overflow-hidden bg-gradient-to-r from-purple-600 via-pink-600 to-red-500 text-white p-8 rounded-2xl shadow-2xl">
      {/* Animated Background Elements */}
      <div className="absolute inset-0 opacity-20">
        <div className="absolute top-0 left-0 w-20 h-20 bg-white rounded-full -translate-x-10 -translate-y-10 animate-bounce"></div>
        <div className="absolute top-0 right-0 w-16 h-16 bg-white rounded-full translate-x-8 -translate-y-8 animate-bounce delay-1000"></div>
        <div className="absolute bottom-0 left-1/3 w-12 h-12 bg-white rounded-full translate-y-6 animate-bounce delay-500"></div>
        <div className="absolute bottom-0 right-1/4 w-10 h-10 bg-white rounded-full translate-y-5 animate-bounce delay-1500"></div>
      </div>

      {/* Floating Icons */}
      <div className="absolute top-4 left-4 animate-pulse">
        <Smile className="w-6 h-6 text-yellow-300" />
      </div>
      <div className="absolute bottom-4 right-4 animate-pulse delay-1000">
        <Coffee className="w-6 h-6 text-orange-300" />
      </div>

      <div className="relative z-10 flex flex-col md:flex-row items-center justify-between">
        <div className="flex-1 mb-6 md:mb-0 md:mr-8">
          <div className="flex items-center mb-4">
            <div className="bg-white/20 rounded-full p-3 mr-4 animate-pulse">
              <QuoteIcon className={`w-8 h-8 ${motivationalQuotes[currentQuote].color}`} />
            </div>
            <h3 className="text-3xl font-bold bg-gradient-to-r from-white to-yellow-100 bg-clip-text text-transparent">
              Keep Going!
            </h3>
          </div>
          <p className="text-xl leading-relaxed opacity-95 transition-all duration-1000 ease-in-out">
            {motivationalQuotes[currentQuote].text}
          </p>

          {/* Quote Indicator Dots */}
          <div className="flex space-x-2 mt-4">
            {motivationalQuotes.map((_, index) => (
              <button
                key={index}
                onClick={() => setCurrentQuote(index)}
                className={`w-3 h-3 rounded-full transition-all duration-300 ${
                  index === currentQuote
                    ? 'bg-white scale-125'
                    : 'bg-white/50 hover:bg-white/75'
                }`}
              />
            ))}
          </div>
        </div>

        <div className="flex-shrink-0">
          <div className="relative">
            {/* Animated Coffee Cup */}
            <div className="bg-white/20 backdrop-blur-sm rounded-full p-6 animate-bounce">
              <Coffee className="w-12 h-12 text-orange-300" />
            </div>
            {/* Steam Animation */}
            <div className="absolute -top-2 left-1/2 transform -translate-x-1/2">
              <div className="flex space-x-1">
                <div className="w-1 h-4 bg-white/60 rounded-full animate-pulse"></div>
                <div className="w-1 h-6 bg-white/60 rounded-full animate-pulse delay-100"></div>
                <div className="w-1 h-3 bg-white/60 rounded-full animate-pulse delay-200"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Progress Bar Animation */}
      <div className="mt-6 pt-6 border-t border-white/20">
        <div className="flex justify-between text-sm opacity-75 mb-2">
          <span>Motivation Level</span>
          <span>100% 💪</span>
        </div>
        <div className="w-full bg-white/20 rounded-full h-2 overflow-hidden">
          <div className="bg-gradient-to-r from-yellow-400 to-pink-400 h-2 rounded-full animate-pulse"></div>
        </div>
      </div>
    </div>
  );
};

export default MotivationBanner;
