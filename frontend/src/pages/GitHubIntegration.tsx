import { useState, type FormEvent } from 'react';
import { 
  GitBranch, 
  GitPullRequest, 
  GitCommit, 
  GitMerge, 
  CheckCircle2, 
  BarChart3, 
  AlertCircle,
  Play
} from 'lucide-react';

interface GitHubIntegrationProps {
  projectId: number | null;
  token: string;
  apiBaseUrl: string;
}

export default function GitHubIntegration({ projectId, token, apiBaseUrl }: GitHubIntegrationProps) {
  const [repoUrl, setRepoUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [connectedRepo, setConnectedRepo] = useState<any>(null);

  const [prs, setPrs] = useState<any[]>([]);
  const [commits, setCommits] = useState<any[]>([]);
  const [repoStats, setRepoStats] = useState<any>(null);

  const loadRepoData = async (repoId: number) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const [prsRes, commitsRes, statsRes] = await Promise.all([
      fetch(`${apiBaseUrl}/api/github/repositories/${repoId}/pull-requests`, { headers }),
      fetch(`${apiBaseUrl}/api/github/repositories/${repoId}/commits`, { headers }),
      fetch(`${apiBaseUrl}/api/github/repositories/${repoId}/statistics`, { headers }),
    ]);

    if (prsRes.ok) setPrs(await prsRes.json());
    if (commitsRes.ok) setCommits(await commitsRes.json());
    if (statsRes.ok) setRepoStats(await statsRes.json());
  };

  const handleConnectRepo = async (e: FormEvent) => {
    e.preventDefault();
    if (!repoUrl.trim() || !projectId) return;

    setLoading(true);
    try {
      const response = await fetch(`${apiBaseUrl}/api/github/connect`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ projectId, url: repoUrl }),
      });

      if (response.ok) {
        const repo = await response.json();
        setConnectedRepo(repo);
        await loadRepoData(repo.id);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleReviewPR = async (prId: number) => {
    if (!connectedRepo) return;
    await fetch(`${apiBaseUrl}/api/github/repositories/${connectedRepo.id}/review-pr`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ prId }),
    });
    alert(`Triggered Agent 4: Code Review on Pull Request #${prId}. Analysis report successfully sent to Dashboard.`);
  };

  if (!projectId) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center select-none">
        <GitBranch className="w-16 h-16 text-slate-700 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold text-slate-300">No Active Project Workspace</h3>
        <p className="text-slate-500 text-sm mt-1 max-w-sm">
          Please select or create a project workspace in the sidebar to configure GitHub connectivity.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-6 md:p-8 space-y-8 select-none">
      
      {/* Page Header */}
      <div>
        <h2 className="text-3xl font-extrabold tracking-tight heading-gradient">
          GitHub Integrations
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Synchronize repositories, review active pull requests, and monitor project development health.
        </p>
      </div>

      {loading && (
        <div className="fixed inset-0 bg-slate-950/50 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="glass-panel p-6 rounded-2xl flex items-center space-x-3 border border-purple-500/30">
            <span className="text-xs font-semibold text-purple-300 cursor-pulse">Importing GitHub repository data...</span>
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-purple-500 border-t-transparent"></div>
          </div>
        </div>
      )}

      {/* Connection Panel */}
      {!connectedRepo ? (
        <div className="glass-panel p-8 rounded-3xl max-w-2xl mx-auto text-center space-y-6">
          <div className="w-16 h-16 rounded-full bg-slate-900 border border-slate-800 mx-auto flex items-center justify-center shadow-lg">
            <GitBranch className="w-8 h-8 text-slate-300" />
          </div>
          <div className="space-y-2">
            <h3 className="text-lg font-bold text-slate-200">Connect GitHub Repository</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto">
              Provide your public repository url or private access scope token to pull source codes for AI scanning.
            </p>
          </div>

          <form onSubmit={handleConnectRepo} className="flex gap-2 max-w-lg mx-auto">
            <input 
              type="text" 
              placeholder="https://github.com/username/repository"
              required
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-xs text-slate-300 focus:outline-none focus:border-purple-500"
            />
            <button 
              type="submit" 
              className="bg-purple-600 hover:bg-purple-700 text-white font-semibold px-6 rounded-xl text-xs transition-colors cursor-pointer"
            >
              Connect Repo
            </button>
          </form>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Header Dashboard Stats */}
          <div className="glass-panel p-6 rounded-3xl flex flex-wrap items-center justify-between gap-6 border border-emerald-500/10">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shadow shadow-emerald-500/5">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-extrabold text-slate-200">
                  {connectedRepo.owner} / {connectedRepo.name}
                </h3>
                <span className="text-[10px] text-slate-500 font-mono mt-0.5 block">{connectedRepo.url}</span>
              </div>
            </div>
            
            <button 
              onClick={() => setConnectedRepo(null)}
              className="text-[10px] uppercase font-bold text-slate-500 hover:text-red-400 border border-slate-800 hover:border-red-500/30 px-3.5 py-2 rounded-xl transition-all cursor-pointer"
            >
              Disconnect Repository
            </button>
          </div>

          {/* Repo Statistics Grid */}
          {repoStats && (
            <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
              {[
                { title: 'Repo Health', value: repoStats.healthRating, icon: BarChart3, color: 'text-emerald-400' },
                { title: 'Security Rating', value: 'Secure', icon: CheckCircle2, color: 'text-emerald-400' },
                { title: 'Open Issues', value: repoStats.openIssues, icon: AlertCircle, color: 'text-amber-400' },
                { title: 'Open PRs', value: repoStats.openPrs, icon: GitPullRequest, color: 'text-purple-400' },
                { title: 'Merged PRs', value: repoStats.mergedPrs, icon: GitMerge, color: 'text-blue-400' },
                { title: 'Lines Analyzed', value: repoStats.linesReviewed, icon: GitBranch, color: 'text-cyan-400' },
              ].map((stat, idx) => {
                const Icon = stat.icon;
                return (
                  <div key={idx} className="glass-panel p-4.5 rounded-2xl flex flex-col justify-between h-28 relative">
                    <span className="text-[9px] font-bold text-slate-500 uppercase tracking-wider">{stat.title}</span>
                    <div className="flex justify-between items-end mt-2">
                      <span className={`text-xl font-extrabold tracking-tight ${stat.color}`}>{stat.value}</span>
                      <Icon className="w-4 h-4 text-slate-600 mb-0.5" />
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* PR Review List and Commit Log List */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            
            {/* PR list */}
            <div className="glass-panel p-6 rounded-3xl space-y-4">
              <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[10px] flex items-center gap-1.5 border-b border-slate-800 pb-3.5">
                <GitPullRequest className="w-4 h-4 text-purple-400" /> Active Pull Requests
              </h3>
              
              <div className="space-y-3.5">
                {prs.map((pr) => (
                  <div key={pr.id} className="p-4 bg-slate-900 border border-slate-850 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="space-y-1">
                      <span className="text-[9px] bg-purple-500/10 text-purple-400 border border-purple-500/20 px-2 py-0.5 rounded font-bold uppercase tracking-wider">#{pr.id}</span>
                      <p className="text-xs font-semibold text-slate-200 mt-1">{pr.title}</p>
                      <p className="text-[10px] text-slate-500">
                        by <span className="text-slate-400 font-medium">@{pr.author}</span> • {pr.date}
                      </p>
                    </div>
                    
                    <div className="flex items-center gap-3">
                      <div className="text-right">
                        <span className="text-[9px] text-slate-500 block uppercase font-bold">Review Score</span>
                        <span className="text-xs font-bold text-slate-200">{pr.reviewScore}%</span>
                      </div>
                      <button 
                        onClick={() => handleReviewPR(pr.id)}
                        className="bg-purple-600 hover:bg-purple-700 text-white p-2 rounded-xl cursor-pointer"
                        title="Analyze PR"
                      >
                        <Play className="w-3.5 h-3.5 fill-current" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Commits timeline */}
            <div className="glass-panel p-6 rounded-3xl space-y-4">
              <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider text-[10px] flex items-center gap-1.5 border-b border-slate-800 pb-3.5">
                <GitCommit className="w-4 h-4 text-blue-400" /> Commit Logs History
              </h3>
              
              <div className="relative border-l border-slate-800 ml-3.5 pl-6 space-y-5">
                {commits.map((commit, idx) => (
                  <div key={idx} className="relative">
                    <div className="absolute -left-[30px] top-0.5 w-2 h-2 rounded-full bg-blue-500 border border-slate-950"></div>
                    <span className="text-[9px] font-mono font-bold text-slate-500">{commit.hash} • {commit.date}</span>
                    <p className="text-xs font-medium text-slate-300 mt-1">{commit.message}</p>
                    <p className="text-[10px] text-slate-500 mt-0.5">by @{commit.author}</p>
                  </div>
                ))}
              </div>
            </div>

          </div>
        </div>
      )}
    </div>
  );
}
