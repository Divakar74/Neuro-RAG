import React from 'react';
import { Sparkles, Trophy, ShieldCheck } from 'lucide-react';

const Hero = () => {
  return (
    <section className="relative overflow-hidden brand-hero-gradient text-primary-900 py-20 px-6">
      <div className="absolute -top-16 -left-16 w-64 h-64 rounded-full bg-indigo-200/40 blur-3xl"></div>
      <div className="absolute -bottom-20 -right-16 w-72 h-72 rounded-full bg-purple-200/40 blur-3xl"></div>

      <div className="max-w-6xl mx-auto text-center relative z-10">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/70 backdrop-blur shadow-sm border border-primary-300 text-sm mb-6">
          <Sparkles className="w-4 h-4" style={{color:'var(--brand-700)'}} />
          <span>Adaptive Skill Assessment</span>
        </div>

        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-primary-900 mb-4">
          Level up your skills with a friendly, focused assessment
        </h1>
        <p className="text-lg md:text-xl text-primary-700 max-w-3xl mx-auto mb-8">
          Lightweight, engaging, and personalized. Answer 10–15 questions and get a clear, motivating roadmap to your target role.
        </p>

        <div className="flex items-center justify-center gap-3 mb-10 text-slate-600">
          <div className="inline-flex items-center gap-2 px-3 py-2 bg-white rounded-full shadow border border-primary-300">
            <Trophy className="w-4 h-4 text-amber-500" />
            <span>Earn badges as you progress</span>
          </div>
          <div className="inline-flex items-center gap-2 px-3 py-2 bg-white rounded-full shadow border border-primary-300">
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            <span>No trick questions, just growth</span>
          </div>
        </div>

        <a
          href="#get-started"
          className="inline-flex items-center justify-center gap-2 text-white font-semibold px-8 py-3 rounded-xl shadow-lg hover:shadow-xl transform hover:scale-[1.02] active:scale-[0.99] transition-all duration-200 brand-cta-gradient"
        >
          Start Assessment
        </a>
      </div>
    </section>
  );
};

export default Hero;
