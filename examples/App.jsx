function App() {
  const [count, setCount] = React.useState(0);
  const doubled = React.useMemo(() => count * 2, [count]);

  React.useEffect(() => {
    document.title = `VSIA clicks: ${count}`;
  }, [count]);

  return <main className="app">
    <h1>VS:IA W1.28</h1>
    <p>This page is hosted from a player-owned ISP1 domain.</p>
    <button onClick={() => setCount(count + 1)}>Clicks: {count}</button>
    <p>Doubled: {doubled}</p>
  </main>;
}
