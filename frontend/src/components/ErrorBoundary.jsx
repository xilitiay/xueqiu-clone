import { Component } from 'react'

/**
 * 全局错误边界：任意页面组件渲染期崩溃时，展示友好提示而非整页白屏。
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary] 捕获到渲染错误：', error, info)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="card" style={{ gridColumn: '1 / -1', textAlign: 'center', padding: 32 }}>
          <h3 style={{ color: 'var(--xq-red)' }}>页面出错了</h3>
          <p className="meta">渲染过程中发生异常，刷新页面或返回首页重试。</p>
          <button className="btn-primary" onClick={() => (window.location.href = '/')}>
            返回首页
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
