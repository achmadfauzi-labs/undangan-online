# Issue F-4 — Frontend Setup & Layout

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `npm`, `npm run`, `npm start`, `npm test`, `npm install`, `docker`, `docker compose`, atau perintah build/run/test apa pun. Semua testing/validasi dilakukan OTOMATIS JUGA OLEH AGENT.

---

## 1. Judul & Ringkasan

Membangun Fondasi Frontend: Routing React Router, Layout Sidebar per role (Admin vs Client), State Management ringan (Context API), dan API Client + Auth Flow. Fase ini murni setup infrastruktur frontend — belum ada halaman fitur bisnis (client management, invitation editor, dll.).

---

## 2. Scope

### File BARU yang WAJIB dibuat:

#### Routing & App Shell
- `Frontend/src/App.jsx` — root component dengan `<BrowserRouter>` + `<Routes>`
- `Frontend/src/main.jsx` — entry point (UPDATE agar render `<App />` dengan Router)

#### Layout Components
- `Frontend/src/layouts/AdminLayout.jsx` — layout sidebar untuk role ADMIN: sidebar navigasi + topbar + content area
- `Frontend/src/layouts/ClientLayout.jsx` — layout sidebar untuk role USER (Client): sidebar + topbar + content area
- `Frontend/src/components/Sidebar.jsx` — component sidebar navigasi reusable (prop `menuItems`, `role`)
- `Frontend/src/components/Topbar.jsx` — component topbar dengan user info + logout button

#### Auth & API Client
- `Frontend/src/contexts/AuthContext.jsx` — Context API untuk simpan state autentikasi (user info, token, role)
- `Frontend/src/contexts/ApiContext.jsx` — Context untuk axios instance (base URL + interceptors)
- `Frontend/src/api/apiClient.js` — instance axios dengan konfigurasi base URL, interceptors untuk token & error handling
- `Frontend/src/api/authApi.js` — wrapper fungsi API untuk login, logout, refresh token

#### Placeholder Pages (routing skeleton)
- `Frontend/src/pages/admin/DashboardPage.jsx` — placeholder "Admin Dashboard"
- `Frontend/src/pages/admin/ClientsPage.jsx` — placeholder "Kelola Client"
- `Frontend/src/pages/client/DashboardPage.jsx` — placeholder "Client Dashboard"
- `Frontend/src/pages/client/InvitationPage.jsx` — placeholder "Edit Undangan"
- `Frontend/src/pages/public/InvitationPage.jsx` — placeholder "Halaman Undangan Tamu (Web 3)"
- `Frontend/src/pages/LoginPage.jsx` — halaman login

#### State Management
- Auth state (token, user info, role) — via `AuthContext`
- API client state — via `ApiContext`

### File yang WAJIB/UBOH diedit (existing):

- `Frontend/src/App.css` — UPDATE: hapus styling lama, tambahkan CSS untuk sidebar layout, topbar, placeholder pages
- `Frontend/index.html` — UPDATE: title jadi "Undangan Online", lang="id"
- `Frontend/package.json` — UPDATE: tambah dependency yang dibutuhkan (React Router DOM, Axios). **WAJIB cek versi terbaru via context7 sebelum menuliskan versi.**

### DILARANG keras:

- Folder `Backend/`, `Database/`, `Sql/`, `Docs/`, `TemaUndangan/`, `Musics/`, `Images/`.
- Folder `node_modules/`.
- File bisnis (entity, controller, service) di backend.
- Routing untuk halaman fitur (CRUD client, CRUD invitation, guestbook, dll.) — itu fase berikutnya.
- Auth untuk endpoint backend `/api/v1/auth/**` di fase ini — hanya setup client-side token storage + interceptor.

---

## 3. Requirement Fungsional

### Routing
- `React Router DOM v7` (latest stable).
- Route hierarchy:
  ```
  /login                        → LoginPage            (public)
  /admin/*                     → AdminLayout          (role ADMIN only)
    /admin/dashboard           → DashboardPage
    /admin/clients             → ClientsPage
  /client/*                    → ClientLayout         (role USER only)
    /client/dashboard          → DashboardPage
    /client/invitation         → InvitationPage
  /undangan/:slug              → InvitationPage       (public, tanpa auth)
  (redirect / → /login atau /admin/dashboard berdasarkan role kalau sudah login)
  ```
- Route protection: component HOC atau wrapper `RequireAuth({ role })` yang cek token di AuthContext — redirect ke `/login` kalau belum login atau role salah.
- Path `/undangan/:slug` TIDAK punya layout sidebar — hanya full-page content untuk halaman tamu.

### Sidebar Layout (AdminLayout & ClientLayout)
- Sidebar di kiri, fixed width ~240px.
- Topbar di atas content area, height ~60px.
- Content area mengisi sisa layar (`flex: 1`, overflow auto).
- Sidebar berisi: logo/app name, menu items (sesuai role), logout button di bawah.
- Menu items per role:
  - **ADMIN**: Dashboard, Kelola Client, Kelola User, Kelola Tamu, Kelola Tema, Kelola Musik, Parameter Sistem
  - **USER (Client)**: Dashboard, Edit Undangan, Kelola Galeri, Guestbook
- Styling sidebar: background warna gelap (#1e293b), text putih, active menu item dengan accent color (#3b82f6), hover state.
- **TIDAK** harus pixel-perfect sama HTML lama (reference tidak ada di repo) — buat sidebar bersih dan fungsional dengan desain modern (CSS variables, responsive hamburger menu untuk mobile).

### Auth Flow (Frontend side)
1. User buka `/login` → input username/password.
2. Panggil `POST /api/v1/auth/login` via `authApi.login()`.
3. Simpan `accessToken`, `refreshToken`, `expiresIn`, user info ke `AuthContext`.
4. Simpan juga ke `localStorage` (key: `auth_token`, `auth_refresh_token`, `auth_user`) — untuk persistence setelah refresh browser.
5. Setelah login, redirect ke `/admin/dashboard` atau `/client/dashboard` berdasarkan `user.roleCode`.
6. Axios interceptor: setiap request otomatis attach `Authorization: Bearer <token>` header.
7. Axios interceptor response: kalau HTTP 401 → coba rotate refresh token via `POST /api/v1/auth/refresh`. Kalau refresh juga gagal → clear auth state + redirect ke `/login`.
8. Logout: clear localStorage + AuthContext + redirect ke `/login`.
9. Token refresh: simpan `refreshExpiresIn` (dari response login) — client bisa tahu kapan refresh diperlukan, tapi untuk MVP cukup retry sekali kalau 401.

### API Client (Axios)
- Base URL: baca dari `import.meta.env.VITE_API_BASE_URL` (sudah ada di `App.jsx` existing).
- Interceptors:
  - **Request**: attach `Authorization: Bearer <token>` dari AuthContext (kalau ada).
  - **Response**: handle 401 → refresh flow; handle network error → throw readable error.
- Helper functions di `authApi.js`:
  - `login(username, password)` → `POST /api/v1/auth/login`
  - `logout(refreshToken)` → `POST /api/v1/auth/logout`
  - `refreshToken(token)` → `POST /api/v1/auth/refresh`

### State Management
- **Gunakan Context API** (bawaan React, tidak perlu library tambahan).
- `AuthContext`: `{ user, accessToken, refreshToken, login(), logout(), isAuthenticated, role }`
- `ApiContext`: menyediakan axios instance yang sudah dikonfigurasi — component lain import dari `ApiContext` atau dari `apiClient.js` (singelton).
- Tidak perlu Zustand atau Redux — Context API sudah cukup untuk scope fase 4 dan fase berikutnya.

### CSS / Styling
- **Gunakan CSS Modules atau plain CSS file per component** — JANGAN pakai Tailwind, styled-components, atau CSS-in-JS library lain.
- File CSS baru:
  - `Frontend/src/styles/variables.css` — CSS custom properties (colors, spacing, font)
  - `Frontend/src/styles/global.css` — reset, typography, base styles
  - `Frontend/src/styles/layout.css` — sidebar, topbar, content area
  - `Frontend/src/styles/auth.css` — login page styling
  - Per component/module: `Sidebar.css`, `Topbar.css`, `AdminLayout.css`, `ClientLayout.css`
- Font: Google Fonts — **Inter** (UI) dan **Playfair Display** (heading/halaman tamu). Import via `<link>` di `index.html` atau `@import` di CSS.
- CSS variables untuk consistency:
  ```css
  :root {
    --color-primary: #3b82f6;
    --color-bg-dark: #1e293b;
    --color-bg-light: #f8fafc;
    --color-text-dark: #1e293b;
    --color-text-light: #f1f5f9;
    --color-border: #e2e8f0;
    --sidebar-width: 240px;
    --topbar-height: 60px;
  }
  ```

---

## 4. Requirement Teknis

### Struktur Folder Frontend

```
Frontend/
├── src/
│   ├── main.jsx                    # entry point
│   ├── App.jsx                     # root dengan Router
│   ├── api/
│   │   ├── apiClient.js           # axios instance singleton
│   │   └── authApi.js              # auth endpoint wrappers
│   ├── components/
│   │   ├── Sidebar.jsx + .css
│   │   └── Topbar.jsx + .css
│   ├── contexts/
│   │   ├── AuthContext.jsx
│   │   └── ApiContext.jsx
│   ├── layouts/
│   │   ├── AdminLayout.jsx + .css
│   │   └── ClientLayout.jsx + .css
│   ├── pages/
│   │   ├── LoginPage.jsx + .css
│   │   ├── admin/
│   │   │   ├── DashboardPage.jsx
│   │   │   └── ClientsPage.jsx
│   │   ├── client/
│   │   │   ├── DashboardPage.jsx
│   │   │   └── InvitationPage.jsx
│   │   └── public/
│   │       └── InvitationPage.jsx
│   ├── styles/
│   │   ├── variables.css
│   │   ├── global.css
│   │   ├── layout.css
│   │   └── auth.css
│   └── styles.css                 # root CSS import
├── index.html                     # UPDATE: title, lang
└── package.json                   # UPDATE: tambah dependency
```

### Dependency yang BOLEH dipakai (WAJIB cek versi via context7 sebelum menulis):

- `react-router-dom` — untuk routing
- `axios` — untuk HTTP client

**CATATAN:** Dependency lain TIDAK BOLEH ditambahkan tanpa persetujuan eksplisit. Context API (bawaan React) sudah cukup untuk state management.

### Environment Variable

- `VITE_API_BASE_URL` — base URL backend (default: `http://localhost:8083`)
  - Di Docker Compose: pass sebagai `ARG VITE_API_BASE_URL` saat build (Dockerfile sudah mendukung ini).
  - Untuk development: `.env` file (gitignore).

Contoh `.env`:
```env
VITE_API_BASE_URL=http://localhost:8083
```

Dockerfile sudah support `ARG VITE_API_BASE_URL` (dari setup existing).

### Konvensi Kode
- Gunakan named export untuk komponen dan function yang reusable.
- Komponen: functional component dengan hooks (bukan class component).
- CSS: CSS custom properties (variables) untuk theming, satu file `variables.css` sebagai single source of truth.
- Tidak ada styling inline (pakai CSS file atau CSS module).
- Placeholder pages cukup render heading + text dummy — styling DO, logic TIDAK (fase berikutnya yang implementasi logic).

### API Response Format (untuk reference frontend)
Backend mengembalikan format ini — frontend axios interceptor harus parse sesuai:

```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 86400,
    "refreshExpiresIn": 604800,
    "user": { "id": 1, "username": "...", "roleCode": "ADMIN" }
  }
}
```

Error response:
```json
{
  "success": false,
  "message": "Username atau password salah",
  "data": null
}
```

---

## 5. Definition of Done

Bisa agent centang sendiri TANPA perlu menjalankan aplikasi:

- [ ] `package.json` — `react-router-dom` dan `axios` sudah ditambahkan (dengan versi yang sudah dicek via context7).
- [ ] `App.jsx` — `<BrowserRouter>` dengan semua route definitions, route protection aktif.
- [ ] `AuthContext.jsx` — state `user`, `accessToken`, `refreshToken`, `login()`, `logout()`, `isAuthenticated`, `role`.
- [ ] `ApiContext.jsx` — axios instance singleton, interceptor request attach token.
- [ ] `apiClient.js` + `authApi.js` — fungsi `login()`, `logout()`, `refreshToken()` yang panggil endpoint yang benar.
- [ ] `RequireAuth` wrapper — redirect ke `/login` kalau belum authenticated atau role tidak sesuai.
- [ ] `AdminLayout.jsx` — sidebar kiri (fixed ~240px) + topbar (60px) + content area. Menu items untuk ADMIN.
- [ ] `ClientLayout.jsx` — sidebar kiri + topbar + content area. Menu items untuk USER (Client).
- [ ] `Sidebar.jsx` + `Topbar.jsx` — reusable component dengan prop yang tepat.
- [ ] CSS variables di `variables.css` — warna, spacing, font sesuai spec.
- [ ] Halaman login (`LoginPage.jsx`) — form username/password, panggil `authApi.login()`, handle error, redirect berdasarkan role.
- [ ] Placeholder pages untuk semua route yang didefinisikan.
- [ ] `index.html` — title "Undangan Online", `lang="id"`.
- [ ] `/undangan/:slug` route — TIDAK pakai sidebar layout, full-page untuk Web 3.
- [ ] Token di-simpan ke `localStorage` dengan key `auth_token`, `auth_refresh_token`, `auth_user`.
- [ ] `AuthContext` di-initialize dari `localStorage` saat app mount (persist after refresh).
- [ ] Interceptor response — handle HTTP 401, coba refresh, redirect ke login kalau gagal.
- [ ] `Dockerfile` — sudah support `ARG VITE_API_BASE_URL` (dari setup existing, agent TIDAK perlu ubah).
- [ ] Tidak ada komponen halaman fitur bisnis yang dibuat (client CRUD, invitation editor, dll.) — itu fase berikutnya.

---

## 6. Batasan Tegas

1. **JANGAN jalankan npm, docker, atau perintah build/run/test apa pun.** Agent menulis kode saja, tech lead yang test.
2. **JANGAN sentuh file di folder `Backend/`, `Database/`, `Sql/`, `Docs/`, `TemaUndangan/`, `Musics/`, `Images/`.**
3. **JANGAN tambahkan Zustand, Redux, Tailwind, styled-components, atau library state management/styling lain** — Context API sudah cukup.
4. **JANGAN buat halaman fitur bisnis** (CRUD client, edit invitation, guestbook, RSVP, gallery upload, theme picker, music picker, dll.) — itu scope fase 5 ke atas.
5. **JANGAN ubah Dockerfile kecuali `ARG VITE_API_BASE_URL` (sudah ada, tidak perlu diubah).**
6. **WAJIB cek versi terbaru dependency via context7** sebelum menuliskan versi di `package.json` — jangan hardcode versi dari training data.
7. **Placeholders pages cukup HTML/CSS dummy** — tidak perlu logic/data fetching ke backend (auth flow DO, pages lain cukup skeleton HTML).

---

## 7. Catatan Referensi

- `Frontend/package.json` — stack sudah ada: React 19.2.x, Vite 6.0.x.
- `Frontend/vite.config.js` — konfigurasi Vite existing, server port 5173.
- `Frontend/App.jsx` (existing) — sudah ada pola `VITE_API_BASE_URL` dari env.
- `Frontend/Dockerfile` — multi-stage build, `ARG VITE_API_BASE_URL` sudah disupport.
- `Docs/PRD-Undangan-Online.md` bagian 4 (Role & Hak Akses), bagian 5 (Fitur per Halaman), bagian 6 (User Flow).
- Spec backend auth: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/refresh` — response format di `Docs/CLAUDE.md` bagian 7.
- Web 3 (`/undangan/:slug`) tanpa sidebar — tamu publik buka undangan dengan slug, tanpa login.
