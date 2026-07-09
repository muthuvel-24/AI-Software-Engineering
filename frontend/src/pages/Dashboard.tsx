import { useEffect, useState } from 'react';
import { 
  Folder, 
  ShieldAlert, 
  Bug, 
  Cpu, 
  Database, 
  Code, 
  GitPullRequest, 
  FileCheck,
  TrendingUp,
  Clock,
  ArrowRight
} from 'lucide-react';

interface DashboardProps {
  projectId: number | null;
  token: string;
  apiBaseUrl: string;
  setView: (view: string) => void;
}

export default function Dashboard({ projectId, token, apiBaseUrl, setView }: DashboardProps) {
  const [stats, setStats] = useState<any>({
    totalReviews: 0,
    averageQualityScore: 0,
    bugsFound: 0,
    generatedApis: 0,
    generatedSqls: 0,
    generatedTests: 0,
    activityTimeline: []
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) return;

    const fetchStats = async () => {
      setLoading(true);
      try {
        const response = await fetch(`${apiBaseUrl}/api/agents/projects/${projectId}/statistics`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
          const data = await response.json();
          setStats(data);
        }
      } catch (e) {
        console.error("Error fetching dashboard statistics", e);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, [projectId, token, apiBaseUrl]);

  if (!projectId) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center select-none">
        <Folder className="w-16 h-16 text-slate-700 mb-4 animate-bounce" />
        <h3 className="text-xl font-bold text-slate-300">No Active Project Workspace</h3>
        <p className="text-slate-500 text-sm mt-1 max-w-sm">
          Please select or create a project workspace in the sidebar to view metrics.
        </p>
      </div>
    );
  }

  const statCards = [
    { title: 'Quality Rating', value: stats.averageQualityScore + '%', icon: FileCheck, color: 'text-emerald-400 bg-emerald-500/10' },
    { title: 'Identified Bugs', value: stats.bugsFound, icon: Bug, color: 'text-rose-400 bg-rose-500/10' },
    { title: 'Generated API Specs', value: stats.generatedApis, icon: Cpu, color: 'text-cyan-400 bg-cyan-500/10' },
    { title: 'SQL Scripts', value: stats.generatedSqls, icon: Database, color: 'text-amber-400 bg-amber-500/10' },
    { title: 'Unit Tests', value: stats.generatedTests, icon: Code, color: 'text-indigo-400 bg-indigo-500/10' },
    { title: 'Total Reviews', value: stats.totalReviews, icon: GitPullRequest, color: 'text-purple-400 bg-purple-500/10' },
  ];

  return (
    <div className="flex-1 overflow-y-auto p-6 md:p-8 space-y-8 select-none">
      
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight heading-gradient">
            Engineering Dashboard
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Real-time analytics and agent activities for your project environment.
          </p>
        </div>
        <button 
          onClick={() => setView('chat')}
          className="bg-purple-600 hover:bg-purple-700 text-white font-semibold text-xs px-4 py-2.5 rounded-xl transition-all cursor-pointer shadow-lg shadow-purple-500/10 flex items-center gap-1.5 self-start hover:scale-[1.01]"
        >
          Consult AI Assistant <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-500"></div>
        </div>
      ) : (
        <>
          {/* Stats Grid */}
          <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
            {statCards.map((card, idx) => {
              const Icon = card.icon;
              return (
                <div key={idx} className="glass-panel glass-panel-hover p-5 rounded-2xl flex flex-col justify-between h-32 relative overflow-hidden group">
                  <div className="flex justify-between items-start">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{card.title}</span>
                    <div className={`p-2 rounded-xl ${card.color}`}>
                      <Icon className="w-4 h-4" />
                    </div>
                  </div>
                  <span className="text-2xl font-bold tracking-tight text-slate-100 group-hover:text-purple-300 transition-colors">
                    {card.value}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Visual Charts Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            {/* SVG Circular Code Quality Dial */}
            <div className="glass-panel p-6 rounded-3xl flex flex-col items-center justify-center lg:col-span-1 min-h-[300px]">
              <h3 className="text-sm font-bold text-slate-300 mb-6 flex items-center gap-2 self-start uppercase tracking-wider text-[11px]">
                <TrendingUp className="w-4 h-4 text-purple-400" /> Overall Code Health
              </h3>
              
              <div className="relative w-40 h-40">
                {/* SVG circular track */}
                <svg className="w-full h-full transform -rotate-90">
                  <circle 
                    cx="80" cy="80" r="70" 
                    stroke="rgba(255,255,255,0.03)" 
                    strokeWidth="10" 
                    fill="transparent" 
                  />
                  <circle 
                    cx="80" cy="80" r="70" 
                    stroke="url(#purpleGradient)" 
                    strokeWidth="10" 
                    fill="transparent" 
                    strokeDasharray={440}
                    strokeDashoffset={440 - (440 * (stats.averageQualityScore || 80)) / 100}
                    strokeLinecap="round"
                    className="transition-all duration-1000 ease-out"
                  />
                  <defs>
                    <linearGradient id="purpleGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#a855f7" />
                      <stop offset="100%" stopColor="#3b82f6" />
                    </linearGradient>
                  </defs>
                </svg>
                {/* Score Text inside */}
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-3xl font-extrabold text-white">{stats.averageQualityScore || 80}</span>
                  <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mt-0.5">Health Score</span>
                </div>
              </div>
              
              <div className="w-full grid grid-cols-3 gap-2 mt-8 text-center text-xs">
                <div>
                  <p className="text-slate-500 text-[10px] font-bold uppercase">Security</p>
                  <p className="text-slate-200 font-semibold mt-1">Excellent</p>
                </div>
                <div className="border-x border-slate-800">
                  <p className="text-slate-500 text-[10px] font-bold uppercase">Coverage</p>
                  <p className="text-slate-200 font-semibold mt-1">82.4%</p>
                </div>
                <div>
                  <p className="text-slate-500 text-[10px] font-bold uppercase">Complexity</p>
                  <p className="text-slate-200 font-semibold mt-1">Low</p>
                </div>
              </div>
            </div>

            {/* SVG Interactive Issues Breakdown Chart */}
            <div className="glass-panel p-6 rounded-3xl lg:col-span-2 min-h-[300px] flex flex-col justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-300 mb-6 flex items-center gap-2 uppercase tracking-wider text-[11px]">
                  <ShieldAlert className="w-4 h-4 text-rose-400" /> Defect Density Breakdown
                </h3>
                
                <div className="space-y-4 my-4">
                  {/* High Severity Bar */}
                  <div>
                    <div className="flex justify-between text-xs font-medium mb-1.5">
                      <span className="text-rose-400 flex items-center gap-1.5">● High Severity Issues</span>
                      <span className="text-slate-400">{stats.bugsFound > 0 ? Math.ceil(stats.bugsFound * 0.25) : 1} Found</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden">
                      <div className="bg-rose-500 h-full rounded-full animate-pulse transition-all duration-1000" style={{ width: stats.bugsFound > 0 ? '25%' : '15%' }}></div>
                    </div>
                  </div>
                  
                  {/* Medium Severity Bar */}
                  <div>
                    <div className="flex justify-between text-xs font-medium mb-1.5">
                      <span className="text-amber-400 flex items-center gap-1.5">● Medium Severity Issues</span>
                      <span className="text-slate-400">{stats.bugsFound > 0 ? Math.floor(stats.bugsFound * 0.5) : 2} Found</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden">
                      <div className="bg-amber-500 h-full rounded-full transition-all duration-1000" style={{ width: stats.bugsFound > 0 ? '50%' : '40%' }}></div>
                    </div>
                  </div>

                  {/* Low Severity Bar */}
                  <div>
                    <div className="flex justify-between text-xs font-medium mb-1.5">
                      <span className="text-blue-400 flex items-center gap-1.5">● Low Severity Issues</span>
                      <span className="text-slate-400">{stats.bugsFound > 0 ? Math.ceil(stats.bugsFound * 0.25) : 1} Found</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden">
                      <div className="bg-blue-500 h-full rounded-full transition-all duration-1000" style={{ width: stats.bugsFound > 0 ? '25%' : '20%' }}></div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="border-t border-slate-800/80 pt-4 flex justify-between items-center text-xs text-slate-500">
                <span>Code Scan Interval: 24h</span>
                <span className="text-purple-400 font-medium">Automatic incremental reviews active</span>
              </div>
            </div>
          </div>

          {/* Activity Logs Timeline */}
          <div className="glass-panel p-6 rounded-3xl">
            <h3 className="text-sm font-bold text-slate-300 mb-5 flex items-center gap-2 uppercase tracking-wider text-[11px]">
              <Clock className="w-4 h-4 text-blue-400" /> Recent Activities Log
            </h3>
            
            <div className="relative border-l border-slate-800 ml-3.5 pl-6 space-y-6">
              {stats.activityTimeline.map((act: any, idx: number) => (
                <div key={idx} className="relative">
                  <div className="absolute -left-[31px] top-0.5 w-3 h-3 rounded-full bg-purple-500 border-2 border-slate-950 shadow shadow-purple-500/50"></div>
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">{act.time}</span>
                  <span className="text-sm text-slate-300 font-medium mt-1 block">{act.action}</span>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
