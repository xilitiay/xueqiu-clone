import axios from 'axios'

const API = '/api'
const MOCK = '/mock'

const TOKEN_KEY = 'xq_token'
// token 模块级缓存；浏览器环境从 localStorage 恢复，确保刷新后登录态不丢失
let TOKEN = (typeof localStorage !== 'undefined' && localStorage.getItem(TOKEN_KEY)) || ''
// 当前登录用户（演示模式自动以 demo 登录后填充）；用于「关注自己的主页」判断、关注流等
let CURRENT_USER = null
// 演示（Mock）模式下本地维护的关注集合，使关注按钮在无后端时也能交互
const localFollowSet = new Set()

export function getCurrentUser() {
  return CURRENT_USER
}

// 请求拦截器：自动附加 JWT
axios.interceptors.request.use((config) => {
  if (TOKEN) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${TOKEN}`
  }
  return config
})

/**
 * 确保已登录：若本地无 token，则用内置演示账号（demo / 123456）登录后端获取 JWT。
 * 后端不可用时静默失败，调用方会自动回退到 Mock 数据。
 */
export async function ensureAuth() {
  if (TOKEN && CURRENT_USER) return CURRENT_USER
  try {
    const { data } = await axios.post(`${API}/auth/login`, { username: 'demo', password: '123456' })
    TOKEN = data.token
    CURRENT_USER = { id: data.id, username: data.username, name: data.name }
    if (typeof localStorage !== 'undefined') localStorage.setItem(TOKEN_KEY, TOKEN)
  } catch (e) {
    /* 后端离线或登录失败：走 Mock 模式，写操作仅本地乐观更新 */
  }
  return CURRENT_USER
}

/** 应用启动时调用：确保登录态就绪，供 Header / 关注按钮 / 关注流使用 */
export async function initAuth() {
  return ensureAuth()
}

const DEMO_AUTHOR = { id: -1, name: '我（演示）', avatarColor: '#E64340', followers: 0 }

/**
 * 统一请求封装：优先请求 Spring Boot 后端，失败（如后端未启动）时
 * 自动回退到 public/mock 下的静态 JSON，保证 UI 在纯前端环境下也能预览。
 */
async function withFallback(backendFn, mockFn) {
  try {
    return await backendFn()
  } catch (err) {
    if (import.meta.env.DEV) console.warn('[mock fallback] 后端不可用，使用本地示例数据：', err.message)
    return await mockFn()
  }
}

/** 首页信息流（返回 PostDTO 数组）。type=all 全部；type=following 仅关注的人（需登录）。
 * Mock 模式按页切片以模拟分页（关注流在无后端时回退为全部，演示用）。 */
export async function getFeed(page = 0, size = 10, type = 'all') {
  return withFallback(
    async () => {
      const { data } = await axios.get(`${API}/feed`, { params: { page, size, type } })
      return data.content
    },
    async () => {
      const { data } = await axios.get(`${MOCK}/feed.json`)
      const start = page * size
      return data.slice(start, start + size)
    }
  )
}

/** 点赞 / 取消点赞。后端模式返回最新 PostDTO；回退模式按本地状态计算 */
export async function toggleLike(id, currentLiked, currentCount) {
  return withFallback(
    async () => (await axios.post(`${API}/feed/${id}/like`)).data,
    async () => ({
      liked: !currentLiked,
      likeCount: currentCount + (currentLiked ? -1 : 1)
    })
  )
}

/** 关注 / 取消关注。后端返回 { following }；Mock 模式本地切换关注集合 */
export async function followUser(id) {
  return withFallback(
    async () => {
      await ensureAuth()
      const { data } = await axios.post(`${API}/users/${id}/follow`)
      return data
    },
    async () => {
      if (localFollowSet.has(String(id))) localFollowSet.delete(String(id))
      else localFollowSet.add(String(id))
      return { following: localFollowSet.has(String(id)) }
    }
  )
}

/** 某用户关注的人列表（UserDTO 数组） */
export async function getFollowing(id) {
  return withFallback(
    async () => (await axios.get(`${API}/users/${id}/following`)).data,
    async () => mockAuthors(id)
  )
}

/** 某用户的粉丝列表（UserDTO 数组） */
export async function getFollowers(id) {
  return withFallback(
    async () => (await axios.get(`${API}/users/${id}/followers`)).data,
    async () => mockAuthors(id)
  )
}

/** Mock 回退：从 feed.json 去重出作者列表（排除自己），用于关注/粉丝列表演示 */
async function mockAuthors(excludeId) {
  const { data } = await axios.get(`${MOCK}/feed.json`)
  const seen = new Set()
  const list = []
  data.forEach((p) => {
    const a = p.author
    if (a && !seen.has(a.id) && String(a.id) !== String(excludeId)) {
      seen.add(a.id)
      list.push({ id: a.id, name: a.name, username: a.username || ('user' + a.id), avatarColor: a.avatarColor, bio: a.bio || '' })
    }
  })
  return list
}

/** 发帖（需登录）。后端返回新建 PostDTO；Mock 模式本地构造一条 */
export async function createPost(content, symbols = []) {
  return withFallback(
    async () => {
      await ensureAuth()
      const { data } = await axios.post(`${API}/posts`, { content, symbols })
      return data
    },
    async () => ({
      id: -Date.now(),
      author: DEMO_AUTHOR,
      content,
      createdAt: new Date().toISOString(),
      likeCount: 0,
      commentCount: 0,
      liked: false,
      stocks: symbols.map((s) => ({ symbol: s, name: s, changePercent: 0 }))
    })
  )
}

/** 帖子详情（PostDTO） */
export async function getPost(id) {
  return withFallback(
    async () => (await axios.get(`${API}/posts/${id}`)).data,
    async () => {
      const { data } = await axios.get(`${MOCK}/feed.json`)
      const p = data.find((x) => x.id === Number(id))
      if (!p) throw new Error('帖子不存在: ' + id)
      return p
    }
  )
}

/** 热门股票榜（StockDTO 数组） */
export async function getHotStocks(limit = 8) {
  return withFallback(
    async () => (await axios.get(`${API}/stocks/hot`, { params: { limit } })).data,
    async () => (await axios.get(`${MOCK}/hot.json`)).data
  )
}

/** 个股详情（StockDTO） */
export async function getStock(symbol) {
  return withFallback(
    async () => (await axios.get(`${API}/stocks/${symbol}`)).data,
    async () => {
      const { data } = await axios.get(`${MOCK}/stocks.json`)
      return data[symbol] || data.__default__
    }
  )
}

/** 个股相关帖子（PostDTO 数组） */
export async function getStockPosts(symbol, page = 0, size = 10) {
  return withFallback(
    async () => {
      const { data } = await axios.get(`${API}/stocks/${symbol}/posts`, { params: { page, size } })
      return data.content
    },
    async () => {
      const { data } = await axios.get(`${MOCK}/stock_posts.json`)
      return data[symbol] || data.__default__ || []
    }
  )
}

/** 帖子评论（分页，返回 CommentDTO 数组）。Mock 模式按页切片 */
export async function getComments(postId, page = 0, size = 20) {
  return withFallback(
    async () => {
      const { data } = await axios.get(`${API}/posts/${postId}/comments`, { params: { page, size } })
      return data.content
    },
    async () => {
      const { data } = await axios.get(`${MOCK}/comments.json`)
      const arr = data[postId] || []
      const start = page * size
      return arr.slice(start, start + size)
    }
  )
}

/** 发表评论（需登录）。后端返回新建 CommentDTO；Mock 模式本地构造一条 */
export async function createComment(postId, content) {
  return withFallback(
    async () => {
      await ensureAuth()
      const { data } = await axios.post(`${API}/posts/${postId}/comments`, { content })
      return data
    },
    async () => ({
      id: -Date.now(),
      authorName: DEMO_AUTHOR.name,
      content,
      createdAt: new Date().toISOString()
    })
  )
}

/** 市场指数（实时，失败回退 Mock） */
export async function getIndices() {
  return withFallback(
    async () => (await axios.get(`${API}/indices`)).data,
    async () => (await axios.get(`${MOCK}/indices.json`)).data
  )
}

/**
 * 搜索：返回 { posts, stocks, users }。
 * Mock 回退：在本地示例数据按关键字匹配（帖子内容 / 股票名称代码 / 用户昵称）。
 */
export async function search(q, type = 'all') {
  return withFallback(
    async () => (await axios.get(`${API}/search`, { params: { q, type } })).data,
    async () => {
      const kw = q.trim().toLowerCase()
      const [feed, hot] = await Promise.all([
        axios.get(`${MOCK}/feed.json`),
        axios.get(`${MOCK}/hot.json`)
      ])
      const posts = kw
        ? feed.data.filter((p) => p.content.toLowerCase().includes(kw)).slice(0, 20)
        : []
      const stocks = kw
        ? hot.data
            .filter((s) => s.name.toLowerCase().includes(kw) || s.symbol.toLowerCase().includes(kw))
            .map((s) => ({ symbol: s.symbol, name: s.name, changePercent: s.changePercent }))
        : []
      const seen = new Set()
      const users = kw
        ? feed.data
            .map((p) => p.author)
            .filter((a) => a && !seen.has(a.id) && a.name.toLowerCase().includes(kw) && seen.add(a.id))
            .map((a) => ({ id: a.id, name: a.name, avatarColor: a.avatarColor, bio: a.bio, followers: a.followers }))
        : []
      return { posts, stocks, users }
    }
  )
}

/**
 * 用户主页：返回 { id, name, avatarColor, bio, followers, postCount, posts }
 * Mock 回退：从 feed.json 中按作者 id 聚合其帖子。
 */
export async function getUser(id) {
  return withFallback(
    async () => (await axios.get(`${API}/users/${id}`)).data,
    async () => {
      const { data } = await axios.get(`${MOCK}/feed.json`)
      const posts = data.filter((p) => String(p.author?.id) === String(id))
      const author = posts[0]?.author || { id: Number(id), name: '球友' + id, avatarColor: '#E64340', followers: 0, username: 'user' + id }
      return {
        id: Number(id),
        name: author.name,
        username: author.username || ('user' + id),
        avatarColor: author.avatarColor,
        bio: author.bio || '',
        followers: author.followers || 0,
        following: 0,
        isFollowing: false,
        postCount: posts.length,
        posts
      }
    }
  )
}

/**
 * 单只股票实时行情（后端代理公开行情接口，无需 key）。
 * Mock 回退：复用 stocks.json 的示例价格/涨跌幅，其余字段留空。
 */
export async function getQuote(symbol) {
  return withFallback(
    async () => (await axios.get(`${API}/quote/${symbol}`)).data,
    async () => {
      const { data } = await axios.get(`${MOCK}/stocks.json`)
      const s = data[symbol] || data.__default__
      return {
        symbol,
        name: s.name,
        price: s.price,
        changePercent: s.changePercent,
        change: null, open: null, high: null, low: null, prevClose: null,
        volume: null, turnoverRate: null, pe: null, pb: null, marketCap: null, time: ''
      }
    }
  )
}

/**
 * 批量实时行情，返回 { symbol: QuoteDTO }。
 * Mock 回退：逐只复用 stocks.json。
 */
export async function getQuotes(symbols) {
  return withFallback(
    async () => (await axios.get(`${API}/quotes`, { params: { symbols } })).data,
    async () => {
      const { data } = await axios.get(`${MOCK}/stocks.json`)
      const map = {}
      symbols.forEach((sym) => {
        const s = data[sym] || data.__default__
        map[sym] = {
          symbol: sym, name: s.name, price: s.price, changePercent: s.changePercent,
          change: null, open: null, high: null, low: null, prevClose: null,
          volume: null, turnoverRate: null, pe: null, pb: null, marketCap: null, time: ''
        }
      })
      return map
    }
  )
}

// ---------- 自选股 / 持仓（需登录；离线时回退 localStorage） ----------
const WATCH_KEY = 'xq_watchlist'
const POS_KEY = 'xq_positions'

function localWatch() {
  try { return JSON.parse(localStorage.getItem(WATCH_KEY) || '[]') } catch { return [] }
}
function saveLocalWatch(arr) {
  if (typeof localStorage !== 'undefined') localStorage.setItem(WATCH_KEY, JSON.stringify(arr))
}
function localPositions() {
  try { return JSON.parse(localStorage.getItem(POS_KEY) || '[]') } catch { return [] }
}
function saveLocalPositions(arr) {
  if (typeof localStorage !== 'undefined') localStorage.setItem(POS_KEY, JSON.stringify(arr))
}

/** 我的自选（实时行情数组，保持自选顺序） */
export async function getWatchlist() {
  return withFallback(
    async () => (await axios.get(`${API}/watchlist`)).data,
    async () => {
      const syms = localWatch()
      if (!syms.length) return []
      const map = await getQuotes(syms)
      return syms.map((s) => map[s]).filter(Boolean)
    }
  )
}

/** 加入 / 取消自选；返回 { added } */
export async function toggleWatchlist(symbol) {
  return withFallback(
    async () => {
      await ensureAuth()
      const { data } = await axios.post(`${API}/watchlist/${symbol.toUpperCase()}`)
      return data
    },
    async () => {
      const arr = localWatch()
      const i = arr.indexOf(symbol)
      if (i >= 0) { arr.splice(i, 1); saveLocalWatch(arr); return { added: false } }
      arr.push(symbol); saveLocalWatch(arr); return { added: true }
    }
  )
}

/** 判断某 symbol 是否在自选（用于个股页按钮态） */
export async function isInWatchlist(symbol) {
  try {
    const list = await getWatchlist()
    return list.some((q) => q.symbol === symbol)
  } catch {
    return localWatch().includes(symbol)
  }
}

/** 我的持仓（含实时盈亏） */
export async function getPositions() {
  return withFallback(
    async () => (await axios.get(`${API}/positions`)).data,
    async () => localPositions()
  )
}

/** 保存 / 更新持仓；返回最新 PositionDTO（离线时用示例行情本地计算） */
export async function savePosition({ symbol, shares, avgCost }) {
  return withFallback(
    async () => {
      await ensureAuth()
      const { data } = await axios.post(`${API}/positions`, {
        symbol: symbol.toUpperCase(), shares, avgCost
      })
      return data
    },
    async () => {
      const arr = localPositions()
      let p = arr.find((x) => x.symbol === symbol)
      if (!p) { p = { symbol }; arr.push(p) }
      p.shares = shares; p.avgCost = avgCost
      saveLocalPositions(arr)
      const q = await getQuote(symbol)
      const price = (q && q.price != null) ? Number(q.price) : Number(avgCost)
      const sh = Number(shares), cost = Number(avgCost)
      const costValue = cost * sh
      const marketValue = price * sh
      const pnl = marketValue - costValue
      const pnlPercent = costValue ? (pnl * 100 / costValue) : 0
      return {
        id: null, symbol, name: q?.name || symbol, shares, avgCost,
        price, changePercent: q?.changePercent ?? null,
        marketValue, costValue, pnl, pnlPercent, market: null
      }
    }
  )
}

/** 移除持仓 */
export async function removePosition(symbol) {
  return withFallback(
    async () => {
      await axios.delete(`${API}/positions/${symbol.toUpperCase()}`)
      return { removed: true }
    },
    async () => {
      saveLocalPositions(localPositions().filter((x) => x.symbol !== symbol))
      return { removed: true }
    }
  )
}
