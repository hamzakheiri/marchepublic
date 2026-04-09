import React from 'react';
import { Link } from 'react-router-dom';

// ─── Stat Card ────────────────────────────────────────────────────────────────
interface StatProps {
  value: string;
  label: string;
}

const Stat: React.FC<StatProps> = ({ value, label }) => (
  <div className="flex flex-col items-center gap-1">
    <span className="text-2xl font-bold text-white">{value}</span>
    <span className="text-sm text-blue-200">{label}</span>
  </div>
);

// ─── Value Prop Card ──────────────────────────────────────────────────────────
interface ValueCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
}

const ValueCard: React.FC<ValueCardProps> = ({ icon, title, description }) => (
  <div className="bg-white rounded-xl border border-slate-200 p-8 flex flex-col gap-4 shadow-sm hover:shadow-md transition-shadow">
    <div className="w-12 h-12 rounded-lg bg-blue-50 flex items-center justify-center text-[#1E3A8A]">
      {icon}
    </div>
    <h3 className="text-lg font-semibold text-slate-800">{title}</h3>
    <p className="text-slate-500 text-sm leading-relaxed">{description}</p>
  </div>
);

// ─── Icons (inline SVG, no extra dep) ────────────────────────────────────────
const SearchIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
  </svg>
);

const BellIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
  </svg>
);

const TagIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M7 7h.01M3 3h8l9 9a2 2 0 010 2.828l-5.172 5.172a2 2 0 01-2.828 0L3 11V3z" />
  </svg>
);

// ─── Landing Page ─────────────────────────────────────────────────────────────
const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col">

      {/* ── Navbar ── */}
      <nav className="bg-white border-b border-slate-200 px-8 py-4 flex items-center justify-between sticky top-0 z-10">
        <span className="text-xl font-bold text-[#1E3A8A] tracking-tight">
          Portail<span className="font-light">AO</span>
        </span>
        <div className="flex items-center gap-6">
          <Link to="/dashboard" className="text-slate-600 hover:text-[#1E3A8A] text-sm font-medium transition-colors">
            Consultations
          </Link>
          <Link
            to="/dashboard"
            className="bg-[#1E3A8A] text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-900 transition-colors"
          >
            Accéder au portail
          </Link>
        </div>
      </nav>

      {/* ── Hero ── */}
      <main className="flex-1">
        <section className="max-w-5xl mx-auto px-6 pt-24 pb-16 text-center">
          <span className="inline-block bg-blue-50 text-[#1E3A8A] text-xs font-semibold uppercase tracking-widest px-3 py-1 rounded-full mb-6">
            Marchés Publics · Maroc
          </span>
          <h1 className="text-5xl font-extrabold text-slate-900 leading-tight mb-6">
            Trouvez les Marchés Publics qui<br />
            <span className="text-[#1E3A8A]">correspondent à votre entreprise.</span>
          </h1>
          <p className="text-slate-500 text-lg max-w-2xl mx-auto mb-10 leading-relaxed">
            Centralisez et explorez tous les appels d'offres publiés en temps réel.
            Filtrez par catégorie, région et taille d'entreprise pour ne jamais manquer une opportunité.
          </p>
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 bg-[#1E3A8A] text-white text-base font-semibold px-8 py-4 rounded-xl hover:bg-blue-900 transition-colors shadow-lg shadow-blue-900/20"
          >
            <SearchIcon />
            Rechercher des appels d'offres
          </Link>
        </section>

        {/* ── Stats Bar ── */}
        <section className="bg-[#1E3A8A]">
          <div className="max-w-5xl mx-auto px-6 py-10 grid grid-cols-2 md:grid-cols-4 gap-8">
            <Stat value="1 200+" label="Consultations actives" />
            <Stat value="Quotidienne" label="Mise à jour" />
            <Stat value="340+" label="Organismes publics" />
            <Stat value="TPE/PME" label="Lots réservés identifiés" />
          </div>
        </section>

        {/* ── Value Propositions ── */}
        <section className="max-w-5xl mx-auto px-6 py-20">
          <h2 className="text-2xl font-bold text-slate-800 text-center mb-12">
            Tout ce dont votre entreprise a besoin
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            <ValueCard
              icon={<SearchIcon />}
              title="Recherche Avancée"
              description="Filtrez les appels d'offres par catégorie, région, type de procédure et organisme acheteur pour cibler exactement votre marché."
            />
            <ValueCard
              icon={<BellIcon />}
              title="Alertes Délais"
              description="Visualisez en un coup d'œil les échéances critiques. Les délais imminents sont mis en évidence pour que vous n'en manquiez aucun."
            />
            <ValueCard
              icon={<TagIcon />}
              title="Filtre TPE/PME"
              description="Identifiez instantanément les lots réservés aux petites et moyennes entreprises grâce à notre filtre dédié."
            />
          </div>
        </section>
      </main>

      {/* ── Footer ── */}
      <footer className="border-t border-slate-200 py-8 text-center text-slate-400 text-sm">
        © {new Date().getFullYear()} PortailAO · Tous droits réservés
      </footer>

    </div>
  );
};

export default LandingPage;
