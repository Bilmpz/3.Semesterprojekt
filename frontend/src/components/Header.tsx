import type { ReactNode } from 'react'

export type Page = 'overblik' | 'aarsager'
type Props = { page: Page; onPage: (p: Page) => void; onRun: () => void }

function Icon({ children }: { children: ReactNode }) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
}

export function Header({ page, onPage, onRun }: Props) {
  const titles = { overblik: 'Velkommen tilbage', aarsager: 'Årsager og clusters' }
  const tab = (p: Page, label: string, icon: ReactNode) => (
    <button className={page === p ? 'nav-item active' : 'nav-item'} aria-current={page === p ? 'page' : undefined} onClick={() => onPage(p)}>
      <Icon>{icon}</Icon><span>{label}</span>
    </button>
  )
  return (
    <>
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="Cendo – forsiden">
          <img src="/cendo-logo.jpg" alt="Cendo" />

        </a>

        <nav aria-label="Hovednavigation">
          {tab('overblik', 'Hjem', <><path d="m3 10 9-7 9 7" /><path d="M5 9v12h5v-7h4v7h5V9" /></>)}
          {tab('aarsager', 'Årsager og clusters', <><rect x="3" y="3" width="7" height="7" rx="2" /><rect x="14" y="3" width="7" height="7" rx="2" /><rect x="3" y="14" width="7" height="7" rx="2" /><rect x="14" y="14" width="7" height="7" rx="2" /></>)}
        </nav>
        <div className="sidebar-footer">
          <img src="/cendo-logo.jpg" alt="" />
          <div><strong>Cendo Analyse</strong><span>Indsigt i jeres samtaler</span></div>
        </div>
      </aside>
      <header className="page-heading"><img className="hero-watermark" src="/cendo-logo.jpg" alt="" aria-hidden="true" />
        <div>
          <p className="eyebrow"><span aria-hidden="true">◫</span> {page === 'overblik' ? 'OVERBLIK' : 'ANALYSE'}</p>
          <h1>{titles[page]}</h1>
          <p className="page-description">Få overblik over viderestillinger, identificerede årsager og samtaler.</p>
        </div>
        <button className="primary" onClick={onRun}><span aria-hidden="true">＋</span> Start ny kørsel</button>
      </header>
    </>
  )
}
