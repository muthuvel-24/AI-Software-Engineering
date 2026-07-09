import { useState, type FormEvent } from 'react';
import { Mail, Lock, User, Code2, ArrowRight } from 'lucide-react';

interface AuthPageProps {
  onLoginSuccess: (token: string, userId: number, email: string, name: string) => void;
  apiBaseUrl: string;
}

export default function AuthPage({ onLoginSuccess, apiBaseUrl }: AuthPageProps) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setLoading(true);

    const endpoint = isLogin ? `${apiBaseUrl}/api/auth/login` : `${apiBaseUrl}/api/auth/register`;
    const payload = isLogin ? { email, password } : { email, password, name };

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Authentication failed');
      }

      onLoginSuccess(data.token, data.id, data.email, data.name);
    } catch (err: any) {
      setErrorMsg(err.message || 'Server connection error');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleMockLogin = async () => {
    setErrorMsg('');
    setLoading(true);
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/google-login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: 'google.developer@gmail.com',
          name: 'Google Developer'
        })
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Google Auth failed');
      }

      onLoginSuccess(data.token, data.id, data.email, data.name);
    } catch (err: any) {
      setErrorMsg(err.message || 'Google Auth Connection Error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-screen flex items-center justify-center bg-slate-950 p-4 grid-bg select-none relative overflow-hidden">
      {/* Decorative Blur Orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl -z-10 pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl -z-10 pointer-events-none"></div>

      <div className="w-full max-w-md glass-panel p-8 rounded-3xl shadow-2xl relative border border-slate-800/80">
        
        {/* Title Brand */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-purple-500 to-blue-500 mx-auto flex items-center justify-center shadow-lg shadow-purple-500/25 mb-4">
            <Code2 className="w-6 h-6 text-white" />
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight heading-gradient">
            {isLogin ? 'Welcome Back' : 'Create Account'}
          </h2>
          <p className="text-xs text-slate-400 mt-2">
            {isLogin ? 'Sign in to access your engineering workspace' : 'Get started with your multi-agent AI assistant'}
          </p>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3.5 bg-red-900/20 border border-red-500/40 rounded-xl text-red-200 text-xs text-center font-medium">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLogin && (
            <div>
              <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5 ml-1">Full Name</label>
              <div className="relative">
                <User className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                <input 
                  type="text" 
                  required
                  placeholder="Alex Mercer"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-slate-900/60 border border-slate-800 hover:border-slate-700 focus:border-purple-500 text-slate-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:outline-none transition-colors"
                />
              </div>
            </div>
          )}

          <div>
            <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5 ml-1">Email Address</label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input 
                type="email" 
                required
                placeholder="dev.alex@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-slate-900/60 border border-slate-800 hover:border-slate-700 focus:border-purple-500 text-slate-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:outline-none transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5 ml-1">Password</label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input 
                type="password" 
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-900/60 border border-slate-800 hover:border-slate-700 focus:border-purple-500 text-slate-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:outline-none transition-colors"
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold py-3.5 rounded-xl text-sm transition-all duration-200 cursor-pointer shadow-lg shadow-purple-500/10 flex items-center justify-center gap-2 hover:scale-[1.01]"
          >
            {loading ? 'Processing...' : (isLogin ? 'Sign In' : 'Sign Up')}
            {!loading && <ArrowRight className="w-4 h-4" />}
          </button>
        </form>

        <div className="relative my-6">
          <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-800"></div></div>
          <div className="relative flex justify-center text-[10px] uppercase font-bold tracking-wider"><span className="bg-slate-950 px-3 text-slate-500">Or Continue With</span></div>
        </div>

        <button 
          onClick={handleGoogleMockLogin}
          className="w-full bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-200 font-semibold py-3.5 rounded-xl text-sm transition-all cursor-pointer flex items-center justify-center gap-2"
        >
          <svg className="w-4 h-4 mr-1" viewBox="0 0 24 24">
            <path fill="#EA4335" d="M12.24 10.285V14.4h6.887c-.275 1.565-1.88 4.604-6.887 4.604-4.33 0-7.866-3.577-7.866-8s3.536-8 7.866-8c2.46 0 4.105 1.025 5.047 1.926l3.227-3.107C18.423 2.147 15.617 1 12.24 1 6.033 1 1 6.033 1 12.24s5.033 11.24 11.24 11.24c6.478 0 10.793-4.537 10.793-10.983 0-.746-.08-1.32-.176-1.887H12.24z"/>
          </svg>
          Google Authentication
        </button>

        <div className="mt-6 text-center">
          <button 
            onClick={() => setIsLogin(!isLogin)}
            className="text-xs text-slate-400 hover:text-purple-400 transition-colors cursor-pointer"
          >
            {isLogin ? "Don't have an account? Sign Up" : 'Already have an account? Sign In'}
          </button>
        </div>
      </div>
    </div>
  );
}
