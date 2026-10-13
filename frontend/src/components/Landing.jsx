// Placeholder until the real landing scene (Phase 9.8)
export default function Landing({ onEnter }) {
  return (
    <main className="auth">
      <section className="auth-panel">
        <h1>CafeCorder</h1>
        <p className="muted auth-sub">Every cup, every corner, remembered.</p>
        <button className="btn btn-primary btn-block" onClick={onEnter}>Enter cafe</button>
      </section>
    </main>
  )
}
