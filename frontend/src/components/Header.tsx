export type Page = 'overblik' | 'aarsager' | 'data'

type Props = { page: Page; onPage: (p: Page) => void; onRun: () => void }

export function Header({ page, onPage, onRun }: Props) {
  const tab = (p: Page, label: string) => (
    <button className={page === p ? 'tab active' : 'tab'} onClick={() => onPage(p)}>{label}</button>
  )
  return (
    <header className="card header">
      <div className="logo" />
      <h1>Cendo · Viderestillinger</h1>
      <div className="spacer" />
      <button className="primary" onClick={onRun}>Start ny kørsel</button>
      {tab('overblik', 'Overblik')}
      {tab('aarsager', 'Årsager')}
      {tab('data', 'Data')}
    </header>
  )
}
