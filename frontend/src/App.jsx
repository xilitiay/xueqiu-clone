import { lazy, Suspense, useEffect } from 'react'
import { Routes, Route } from 'react-router-dom'
import Header from './components/Header.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import { initAuth } from './api/client.js'

// 路由级代码分割：首屏只加载首页所需 JS，其余页面按需加载
const FeedPage = lazy(() => import('./components/FeedPage.jsx'))
const StockDetailPage = lazy(() => import('./components/StockDetailPage.jsx'))
const PostDetailPage = lazy(() => import('./components/PostDetailPage.jsx'))
const UserPage = lazy(() => import('./components/UserPage.jsx'))
const MarketPage = lazy(() => import('./components/MarketPage.jsx'))
const SearchPage = lazy(() => import('./components/SearchPage.jsx'))

export default function App() {
  // 启动即确保登录态（演示账号自动登录），供关注流 / 关注按钮 / 我的 入口使用
  useEffect(() => { initAuth().catch(() => {}) }, [])

  return (
    <div className="app">
      <Header />
      <div className="container">
        <ErrorBoundary>
          <Suspense fallback={<div className="card">加载中…</div>}>
            <Routes>
              <Route path="/" element={<FeedPage />} />
              <Route path="/market" element={<MarketPage />} />
              <Route path="/stock/:symbol" element={<StockDetailPage />} />
              <Route path="/post/:id" element={<PostDetailPage />} />
              <Route path="/user/:id" element={<UserPage />} />
              <Route path="/search" element={<SearchPage />} />
            </Routes>
          </Suspense>
        </ErrorBoundary>
      </div>
    </div>
  )
}
