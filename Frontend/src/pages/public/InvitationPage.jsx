export function PublicInvitationPage() {
  // This is the guest-facing page (Web 3) — no sidebar layout, full page
  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--color-bg-light)', padding: 'var(--spacing-xl)' }}>
      <div className="card" style={{ maxWidth: '800px', margin: '0 auto', textAlign: 'center' }}>
        <h2 style={{ fontFamily: "'Playfair Display', Georgia, serif", fontSize: '28px', marginBottom: 'var(--spacing-md)' }}>
          Halaman Undangan
        </h2>
        <p style={{ color: 'var(--color-text-muted)' }}>
          Halaman undangan tamu (Web 3) akan ditampilkan di sini.
        </p>
      </div>
    </div>
  );
}
