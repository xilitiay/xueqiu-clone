import { Link } from 'react-router-dom'

/**
 * 富文本正文解析：
 * - $代码 / $名称（cashtag）→ 可点击个股链接（能匹配到关联股票才跳转，否则仅高亮）
 * - @用户名（mention）→ 可点击用户主页链接（/user/:username）
 * 换行与纯文本段原样保留（依赖祖先元素的 white-space: pre-wrap）。
 */
export default function RichContent({ text, stocks = [] }) {
  const byName = {}
  const bySymbol = {}
  stocks.forEach((s) => {
    if (s.name) byName[s.name] = s.symbol
    if (s.symbol) bySymbol[s.symbol] = s.symbol
  })

  const re = /\$([^\s$%，。、！？；：,!?;:]+)|@([A-Za-z0-9_]+)/g
  const nodes = []
  let last = 0
  let m
  let key = 0
  const src = text || ''
  while ((m = re.exec(src))) {
    if (m.index > last) nodes.push(<span key={key++}>{src.slice(last, m.index)}</span>)
    if (m[1] != null) {
      // cashtag
      const seg = m[1]
      const sym = byName[seg] || bySymbol[seg] || null
      nodes.push(sym
        ? <Link key={key++} to={`/stock/${sym}`} className="cashtag">${seg}</Link>
        : <span key={key++} className="cashtag">${seg}</span>)
    } else {
      // @mention
      const name = m[2]
      nodes.push(<Link key={key++} to={`/user/${name}`} className="mention">@{name}</Link>)
    }
    last = re.lastIndex
  }
  if (last < src.length) nodes.push(<span key={key++}>{src.slice(last)}</span>)

  return <span className="rich-content">{nodes}</span>
}
