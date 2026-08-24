# Issue F-4 — Frontend Setup, Layout & API Integration

> **Audience:** AI agent junior (model kecil). Agent **SELALU** menjalankan `docker`, `docker compose`, `mvn`, `npm`, atau perintah build/run/test apa pun **sesudah** menyelesaikan issue ini.
>
> **Prasyarat:** Fase 2 (backend API) + Fase 3 (backend modul) + M-1.1 s/d M-1.5 (optimalisasi) sudah selesai. Frontend skeleton sudah ada (`Frontend/src/` dengan routing, layouts, API client, contexts).

---

## 1. Judul & Ringkasan

**Frontend Integration — Phase 4.**

Frontend sudah punya skeleton: routing, layouts (Admin/Client), sidebar per role, API client dengan axios interceptor + refresh token, contexts (Auth/Api), CSS variables/layout/auth styles. Tapi hampir semua halaman masih placeholder. Task ini: **isi halaman-halaman tersebut dengan data nyata dari backend API**, tambah halaman yang belum ada, buat API service files per domain, tambah common components, dan wired semua ke API client yang sudah ada.

---

## 2. Scope — File/Folder yang Boleh Disentuh

### Folder yang Boleh Disentuh
- `Frontend/src/` — seluruh isi

### File BARU yang WAJIB dibuat

**API Service Files:**
- `Frontend/src/api/clientsApi.js` — CRUD client untuk admin
- `Frontend/src/api/usersApi.js` — CRUD user/role untuk admin
- `Frontend/src/api/guestsApi.js` — global guest list untuk admin
- `Frontend/src/api/invitationsApi.js` — CRUD invitation untuk client
- `Frontend/src/api/galleriesApi.js` — CRUD galeri untuk client
- `Frontend/src/api/guestbookApi.js` — guestbook entries untuk client & admin
- `Frontend/src/api/themesApi.js` — tema untuk admin
- `Frontend/src/api/musicApi.js` — musik untuk admin
- `Frontend/src/api/systemParamsApi.js` — system parameter untuk admin

**Common Components:**
- `Frontend/src/components/LoadingSpinner.jsx` + `LoadingSpinner.css`
- `Frontend/src/components/EmptyState.jsx` + `EmptyState.css`
- `Frontend/src/components/Modal.jsx` + `Modal.css`
- `Frontend/src/components/ConfirmDialog.jsx`
- `Frontend/src/components/Badge.jsx` — badge status (active/expired/pending, dst)
- `Frontend/src/components/Pagination.jsx` + `Pagination.css`
- `Frontend/src/components/PageHeader.jsx` — komponen judul halaman yang reusable (title + subtitle + action button)

**Admin Pages (isi placeholder):**
- `Frontend/src/pages/admin/UsersPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/admin/GuestsPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/admin/TemplatesPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/admin/MusicPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/admin/ParamsPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/admin/DashboardPage.jsx` — sudah ada placeholder, WAJIB implementasi

**Client Pages (isi placeholder):**
- `Frontend/src/pages/client/GalleryPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/client/GuestbookPage.jsx` + CSS — halaman belum ada, WAJIB dibuat
- `Frontend/src/pages/client/DashboardPage.jsx` — sudah ada placeholder, WAJIB implementasi
- `Frontend/src/pages/client/InvitationPage.jsx` — sudah ada placeholder, WAJIB implementasi

**Public Page:**
- `Frontend/src/pages/public/InvitationPage.jsx` — sudah ada placeholder, WAJIB implementasi

**Route Definitions (edit App.jsx):**
- Tambah route untuk halaman admin yang belum ada (`/admin/users`, `/admin/guests`, `/admin/templates`, `/admin/music`, `/admin/params`)
- Tambah route untuk halaman client yang belum ada (`/client/gallery`, `/client/guestbook`)
- Route `/undangan/:slug` sudah ada — pastikan komponen memanggil API

### File yang BOLEH Diedit (existing):
- `Frontend/src/App.jsx` — tambah route definitions
- `Frontend/src/pages/admin/DashboardPage.jsx` — implementasi penuh
- `Frontend/src/pages/admin/ClientsPage.jsx` — implementasi penuh
- `Frontend/src/pages/client/DashboardPage.jsx` — implementasi penuh
- `Frontend/src/pages/client/InvitationPage.jsx` — implementasi penuh
- `Frontend/src/pages/public/InvitationPage.jsx` — implementasi penuh
- `Frontend/src/components/Topbar.jsx` — wired logout ke API

### File CSS Baru
- `Frontend/src/styles/pages.css` — reusable page styles (`.page-header`, `.card`, `.toolbar`, `.table-wrap`, `.badge`, `.empty-state`, dst.)
- CSS per halaman yang baru (referensi inline atau file `.css` terpisah di folder `pages/admin/`, `pages/client/`)

### DILARANG Keras:
- Folder `Backend/`, `Sql/`, `Docs/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`, `TemaUndangan/`
- Jangan menambah library/dependency baru (sudah ada: react-router v7, axios)
- Jangan ubah `Frontend/package.json`, `Frontend/vite.config.js`, `Frontend/Dockerfile`, `Frontend/Dockerfile`
- Jangan ubah `Frontend/src/styles/variables.css`, `Frontend/src/styles/global.css`, `Frontend/src/styles/layout.css`, `Frontend/src/styles/auth.css`

---

## 3. Requirement Fungsional

### 3.1 API Service Files

Setiap file `*Api.js` exports async functions yang memanggil `apiClient`. Pakai pola:

```js
import { apiClient } from './apiClient';

// Contoh pola:
export async function getClients(params) { /* GET /api/v1/admin/clients */
  const res = await apiClient.get('/api/v1/admin/clients', { params });
  return res.data;
}

export async function getClient(id) { /* GET /api/v1/admin/clients/{id} */
  const res = await apiClient.get(`/api/v1/admin/clients/${id}`);
  return res.data;
}

export async function createClient(payload) { /* POST /api/v1/admin/clients */
  const res = await apiClient.post('/api/v1/admin/clients', payload);
  return res.data;
}

export async function updateClient(id, payload) { /* PUT /api/v1/admin/clients/{id} */
  const res = await apiClient.put(`/api/v1/admin/clients/${id}`, payload);
  return res.data;
}

export async function deleteClient(id) { /* DELETE /api/v1/admin/clients/{id} */
  const res = await apiClient.delete(`/api/v1/admin/clients/${id}`);
  return res.data;
}
```

Endpoint per domain (cek backend API yang sudah ada):

| API File | Endpoint Base | Method | Notes |
|---|---|---|---|
| `clientsApi.js` | `/api/v1/admin/clients` | GET/POST | list + create |
| `clientsApi.js` | `/api/v1/admin/clients/{id}` | GET/PUT/DELETE | detail/update/delete |
| `usersApi.js` | `/api/v1/admin/users` | GET/POST | list + create |
| `usersApi.js` | `/api/v1/admin/users/{id}` | GET/PUT/DELETE | |
| `guestsApi.js` | `/api/v1/admin/guests` | GET | global guest list |
| `themesApi.js` | `/api/v1/admin/themes` | GET/POST | list + create (admin upload) |
| `themesApi.js` | `/api/v1/admin/themes/{id}` | GET/DELETE | detail + delete |
| `themesApi.js` | `/api/v1/admin/themes/{id}/upload` | POST | upload file tema |
| `musicApi.js` | `/api/v1/admin/music` | GET/POST | list + upload |
| `musicApi.js` | `/api/v1/admin/music/{id}` | GET/DELETE | detail + delete |
| `systemParamsApi.js` | `/api/v1/admin/system-parameters` | GET/PUT | list + update |
| `invitationsApi.js` | `/api/v1/client/invitation` | GET/PUT | get + update invitation client |
| `galleriesApi.js` | `/api/v1/client/galleries` | GET/POST/DELETE | galeri client |
| `guestbookApi.js` | `/api/v1/client/guestbook` | GET | guestbook client |
| `guestbookApi.js` | `/api/v1/client/guestbook/{id}` | PUT/DELETE | approve/reject/remove |
| `clientApi.js` (public) | `/api/v1/client/invitations/{slug}` | GET | data undangan publik |

**CATATAN PENTING:** Agent WAJIB cek endpoint backend yang sebenarnya ada di `Backend/src/main/java/com/undangan/online/controller/`. Jangan mengarang endpoint. Kalau endpoint tidak ditemukan di controller manapun, cek Swagger/OpenAPI kalau ada, atau tulis sebagai TODO dengan catatan `// TODO-F4: endpoint belum ada di backend — perlu konfirmasi`.

### 3.2 Common Components

**LoadingSpinner** — tampilkan saat fetch data:
- Spinner sederhana (CSS animation, bukan library)
- Props: `size` (sm/md/lg), `color`
- Dipakai di setiap halaman saat loading

**EmptyState** — tampilkan kalau data kosong:
- Props: `message`, `icon` (SVG inline atau null)
- Pesan default: "Tidak ada data"

**Modal** — popup untuk form/create/edit:
- Props: `isOpen`, `onClose`, `title`, `children`
- Overlay clickable untuk close
- ESC key untuk close
- CSS animasi fade-in

**ConfirmDialog** — konfirmasi sebelum aksi destructive:
- Pakai komponen Modal
- Props: `isOpen`, `onConfirm`, `onCancel`, `message`, `confirmLabel`, `cancelLabel`
- Tombol confirm: style danger untuk delete

**Badge** — status label:
- Props: `variant` (success/danger/warn/neutral), `children`
- Rujuk CSS `.badge` dari HTML dashboard reference

**Pagination** — navigasi halaman:
- Props: `page`, `totalPages`, `onPageChange`
- Tombol prev/next + nomor halaman

**PageHeader** — header halaman reusable:
- Props: `title`, `subtitle`, `action` (React node — biasanya button)
- Rujuk `.page-header` CSS di HTML dashboard

### 3.3 Admin Pages

**UsersPage** (`/admin/users`):
- Tabel daftar user (dari `usersApi.getUsers`)
- Kolom: name, username, role, client_id, status, created_at
- Fitur: create user (modal), edit user (modal), delete user (confirm dialog)
- Fitur: toggle status active/inactive
- Filter: cari nama/username, filter role
- Pagination

**GuestsPage** (`/admin/guests`):
- Tabel global guest list (dari `guestsApi.getGuests`)
- Kolom: guest_name, invitation_id, client_name, attendance_status, rsvp_at
- Filter: cari nama, filter client, filter status RSVP
- Pagination

**TemplatesPage** (`/admin/templates`):
- Grid kartu tema (dari `themesApi.getThemes`)
- Kartu: preview thumbnail (CSS gradient kalau belum ada gambar), nama, kategori, status
- Fitur: upload tema baru (multipart form), delete tema
- Filter: cari, filter kategori

**MusicPage** (`/admin/music`):
- Daftar musik (dari `musicApi.getMusic`)
- Kartu: nama, durasi, action (play/delete)
- Fitur: upload musik (multipart), delete

**ParamsPage** (`/admin/params`):
- Daftar parameter sistem (dari `systemParamsApi.getParams`)
- Tabel/key-value: param_key, param_value, description
- Fitur: edit value (inline atau modal), simpan

**DashboardPage** (`/admin/dashboard`):
- Stat cards: jumlah client aktif, total guest, total invitation, dst. — ambil data dari masing-masing API (count aja)
- Ringkasan sederhana

### 3.4 Client Pages

**GalleryPage** (`/client/gallery`):
- Grid galeri foto client (dari `galleriesApi.getGalleries`)
- Fitur: upload foto (multipart), hapus foto
- Tampilan grid 3 kolom

**GuestbookPage** (`/client/guestbook`):
- Daftar guestbook entries (dari `guestbookApi.getGuestbook`)
- Tiap entry: nama tamu, pesan, timestamp
- Aksi: approve/reject/delete per entry

**ClientDashboardPage** (`/client/dashboard`):
- Stat cards: countdown ke hari-H, jumlah RSVP, jumlah guestbook
- Ringkasan undangan (link generate)

**ClientInvitationPage** (`/client/invitation`):
- Form edit data undangan: data pasangan, tanggal acara, sesi, lokasi
- Pilih tema, pilih musik
- Simpan via `invitationsApi.updateInvitation`

### 3.5 Public Page

**PublicInvitationPage** (`/undangan/:slug`):
- Ambil data dari API `/api/v1/client/invitations/{slug}`
- Layout halaman undangan tamu — **REFERENSI: `Docs/web-dashboard-html.html` (Undangan Kirana & Danendra)**
- Komponen: cover/header, countdown, profil pasangan, detail acara, galeri foto, RSVP form, guestbook
- **Jangan desain dari nol** — gunakan HTML dashboard Kirana & Danendra sebagai acuan visual

### 3.6 Auth Integration

- `Topbar.jsx` — logout button harus panggil `logout(refreshToken)` dari `authApi.js` SEBELUM clear localStorage
- Redirect ke `/login` setelah logout
- Handle token expiry di client: kalau refresh gagal, redirect ke login

---

## 4. Requirement Teknis

### Struktur File CSS

Pakai pola CSS modular:
- `Frontend/src/styles/pages.css` — reusable: `.page-header`, `.card`, `.card-head`, `.toolbar`, `.search-input`, `.filter-sel`, `.table-wrap`, `table`, `.badge`, `.empty-state`, `.pagination`
- CSS per halaman di folder `pages/admin/` dan `pages/client/` (file `.css` terpisah, satu file per page)

### Package & Import

```jsx
// API service — import dari ./apiClient (sudah ada)
import { apiClient } from '../../api/apiClient';
// atau dari './clientsApi' kalau sudah dibuat

// Auth context — import dari hooks
import { useAuth } from '../../contexts/AuthContext';

// Common components
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { EmptyState } from '../../components/EmptyState';
import { Modal } from '../../components/Modal';
import { PageHeader } from '../../components/PageHeader';
import { Badge } from '../../components/Badge';
import { Pagination } from '../../components/Pagination';
```

### State Management

- Pakai **React hooks** (`useState`, `useEffect`) — tidak perlu Zustand/Redux/Context tambahan
- Semua halaman pakai `useEffect` untuk fetch data saat mount
- Loading state pakai `useState(false)` + `isLoading`
- Error state pakai `useState(null)` + `error`

### Naming Conventions

- Component: PascalCase (`ClientsPage.jsx`)
- Hook: camelCase (`useClients`)
- API file: camelCase (`clientsApi.js`)
- CSS class: kebab-case (`.page-header`, `.card`, `.sidebar-nav-item`)
- CSS file: camelCase atau kebab-case konsisten

### API Response Handling

Semua response backend pakai wrapper `ApiResponse`:
```js
// Handle response
const res = await apiClient.get('/api/v1/admin/clients');
const payload = res.data; // ApiResponse wrapper
if (!payload.success) throw new Error(payload.message);
return payload.data; // actual data
```

### Error Handling

- `try/catch` di setiap async function
- Error state di `useState(null)` — tampilkan pesan error di UI (bukan console.log)
- Format: `Error: <message dari backend>` — pakai `err.response?.data?.message` untuk axios error

### CSS Variables (sudah ada di variables.css)

```css
--color-primary     /* #4f46e5 — indigo */
--color-bg-white   /* #ffffff */
--color-bg-light   /* #f8f7ff */
--color-text-dark  /* #1e1b4b */
--color-text-muted /* #6b7280 */
--color-border     /* #e4e2f0 */
--color-danger     /* #ef4444 */
--color-success    /* #10b981 */
--color-warn      /* #f59e0b */
--radius-md        /* 12px */
```

---

## 5. Definition of Done

### API Integration
- [ ] `clientsApi.js` — semua method CRUD tersedia dan exportable
- [ ] `usersApi.js` — semua method CRUD tersedia dan exportable
- [ ] `guestsApi.js` — getGlobalGuests tersedia
- [ ] `themesApi.js` — get/list/upload/delete tersedia
- [ ] `musicApi.js` — get/upload/delete tersedia
- [ ] `systemParamsApi.js` — get/update tersedia
- [ ] `invitationsApi.js` — get/update tersedia
- [ ] `galleriesApi.js` — get/upload/delete tersedia
- [ ] `guestbookApi.js` — get/approve/reject tersedia
- [ ] Semua API function menangani `ApiResponse` wrapper (cek `success` field)

### Common Components
- [ ] `LoadingSpinner` — reusable, animasi spinner
- [ ] `EmptyState` — reusable, pesan default + slot untuk icon
- [ ] `Modal` — overlay, ESC close, animasi
- [ ] `ConfirmDialog` — destructive action confirmation
- [ ] `Badge` — variant success/danger/warn/neutral
- [ ] `Pagination` — prev/next + halaman aktif
- [ ] `PageHeader` — title + subtitle + action slot
- [ ] Semua component punya CSS scoped atau di `pages.css`

### Admin Pages
- [ ] `UsersPage` — tabel CRUD user + role + status + pagination
- [ ] `GuestsPage` — tabel global guest list + filter
- [ ] `TemplatesPage` — grid tema + upload + delete
- [ ] `MusicPage` — daftar musik + upload + delete
- [ ] `ParamsPage` — tabel key-value + inline edit
- [ ] `AdminDashboardPage` — stat cards (jumlah client, guest, invitation)
- [ ] `ClientsPage` — CRUD client dengan modal form

### Client Pages
- [ ] `GalleryPage` — grid foto + upload + delete
- [ ] `GuestbookPage` — daftar entry + approve/reject/delete
- [ ] `ClientDashboardPage` — stat cards (countdown, RSVP count, guestbook count)
- [ ] `ClientInvitationPage` — form edit data pasangan + pilih tema + pilih musik

### Public Page
- [ ] `PublicInvitationPage` — fetch data via `/undangan/:slug` API
- [ ] Layout halaman tamu dengan cover, countdown, detail acara, galeri

### Routing
- [ ] `App.jsx` — semua route admin (`/admin/users`, `/admin/guests`, `/admin/templates`, `/admin/music`, `/admin/params`) terdaftar
- [ ] `App.jsx` — semua route client (`/client/gallery`, `/client/guestbook`) terdaftar
- [ ] Sidebar menu dan route konsisten (tidak ada menu yang point ke route yang tidak ada)

### Auth & Integration
- [ ] `Topbar` logout button panggil `authApi.logout()` sebelum clear localStorage
- [ ] Semua halaman handle loading state
- [ ] Semua halaman handle error state
- [ ] Semua halaman handle empty state
- [ ] Kode tidak ada `console.log` production (boleh console.error untuk debugging)

### Code Quality
- [ ] Tidak ada hardcoded URL API — semua pakai `apiClient` dengan path relatif
- [ ] Tidak ada state management library baru (Zustand/Redux) — pakai hooks
- [ ] Tidak ada `TODO` tersisa di kode (selain yang diizinkan — lihat catatan)
- [ ] Semua CSS pakai CSS variables yang sudah ada (tidak ada warna hardcoded)

---

## 6. Batasan Tegas

1. **JANGAN jalankan build tool** — `npm run`, `npm build`, `npm dev`, `docker compose`, atau perintah test/run apa pun.
2. **JANGAN ubah schema database** — jangan tulis/ubah file di folder `Sql/`.
3. **JANGAN ubah file di folder `Backend/`, `Docs/`, `Database/`, `TemaUndangan/`, `Musics/`, `Images/`**
4. **JANGAN tambah library/dependency baru** — sudah ada: `react-router v7`, `axios`. Tidak perlu library lain.
5. **JANGAN pakai Zustand/Redux/Context baru** — pakai hooks (`useState`, `useEffect`, `useCallback`) saja.
6. **JANGAN desain halaman tamu (Web 3) dari nol** — REFERENSI `Docs/web-dashboard-html.html` untuk visual. Kalau ada discrepancy antara PRD dan HTML reference, ikuti HTML reference.
7. **JANGAN asumsikan endpoint ada** — kalau endpoint tidak ditemukan di controller backend, tulis catatan `// TODO-F4: endpoint [method] [path] belum ada di backend` di kode. Jangan buat endpoint sendiri.
8. **JANGAN hapus atau ubah file CSS yang sudah ada** (`variables.css`, `global.css`, `layout.css`, `auth.css`).

---

## 7. Catatan Referensi

### Referensi Visual
- **`Docs/web-dashboard-html.html`** — HTML statis dashboard admin (indigo theme). REFERENSI UTAMA untuk styling, layout, komponen UI. Font: Inter. Warna: indigo (#4f46e5). Pakai CSS variables yang sudah ada di `Frontend/src/styles/variables.css`.
- **`TemaUndangan/undangan-online-demo.html`** — HTML demo halaman tamu (Kirana & Danendra). REFERENSI UTAMA untuk PublicInvitationPage. Pakai Google Fonts yang sudah di-import.

### Backend API Spec
- Endpoint auth: sudah terintegrasi di `Frontend/src/api/authApi.js`
- Endpoint admin: cek controller di `Backend/src/main/java/com/undangan/online/controller/` — ambil path dari annotation `@RequestMapping`
- Endpoint client: cek `Client*Controller.java` dan `Public*Controller.java`
- Response format: semua pakai `ApiResponse<T>` wrapper — cek `ApiResponse.java`

### Existing Frontend Structure
- `Frontend/src/App.jsx` — routing definitions
- `Frontend/src/api/apiClient.js` — axios instance + interceptors (refresh token, auth header)
- `Frontend/src/api/authApi.js` — login/logout/refresh
- `Frontend/src/contexts/AuthContext.jsx` — `useAuth()` hook (login/logout/user/role)
- `Frontend/src/contexts/ApiContext.jsx` — `useApi()` hook (apiClient)
- `Frontend/src/layouts/AdminLayout.jsx` + `ClientLayout.jsx` — layout wrapper
- `Frontend/src/components/Sidebar.jsx` — menu items per role
- `Frontend/src/components/Topbar.jsx` — user info + logout
- `Frontend/src/styles/variables.css` — CSS variables (warna, spacing, shadow, radius)
