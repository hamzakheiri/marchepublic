import React from 'react';
import { Link } from 'react-router-dom';

const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col items-center justify-center gap-4">
      <span className="text-6xl font-extrabold text-[#1E3A8A]">404</span>
      <p className="text-slate-500">Cette page n'existe pas.</p>
      <Link to="/" className="text-[#1E3A8A] text-sm font-medium hover:underline">
        Retour à l'accueil
      </Link>
    </div>
  );
};

export default NotFoundPage;
