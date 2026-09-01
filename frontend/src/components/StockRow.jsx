import { Link } from 'react-router-dom'
import { fmtChange } from './format.js'

export default function StockRow({ stock }) {
  const pct = stock.changePercent
  const cls = pct > 0 ? 'up' : pct < 0 ? 'down' : ''
  return (
    <Link to={`/stock/${stock.symbol}`} className="stock-row">
      <div>
        <div className="name">{stock.name}</div>
        <div className="sym">{stock.symbol}</div>
      </div>
      <div className="price">
        <div>{stock.price}</div>
        <div className={`chg ${cls}`}>{fmtChange(pct)}</div>
      </div>
    </Link>
  )
}
