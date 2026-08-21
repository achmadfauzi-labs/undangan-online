# Issue — Fase 0: Bootstrap Infrastruktur

> **Audience:** AI agent junior (model kecil). Agent **DILARANG** menjalankan docker/mvn/npm atau perintah build apa pun — hanya menulis/mengedit file. Manusia yang akan test manual via `docker compose up`.

---

## 1. Judul & Ringkasan

**Judul:** `[Fase 0] Bootstrap Infrastruktur — Skeleton Backend Spring Boot, Database Postgres 18, Frontend React, dan Docker Compose`

**Ringkasan (1 paragraf):**
Tujuan fase ini adalah menyiapkan kerangka awal ketiga service (backend, database, frontend) beserta Docker Compose di root project, **TANPA fitur bisnis apa pun**. Setelah fase ini selesai, manusia akan menjalankan `docker compose up` dan memastikan: (a) ketiga container start dan sehat, (b) backend bisa konek ke database Postgres, (c) frontend bisa fetch endpoint `/actuator/health` backend dan menampilkan status `UP`/`DOWN`.

**Yang TIDAK termasuk fase ini (jelas, supaya tidak dianggar):**
entity/controller/service/repository/DTO, schema database, autentikasi, logika aplikasi, test code, CI/CD, file di folder `Sql/`.

---

## 2. Scope — File/Folder yang Boleh Disentuh

Agent HANYA boleh membuat/mengedit file di lokasi di bawah. Di luar daftar ini → JANGAN disentuh, jangan rename, jangan hapus.

### Folder existing (sudah ada di repo, casing JANGAN diubah):
- `Backend/` — tambah Dockerfile, edit `pom.xml` & `application.properties` yang sudah ada
- `Database/` — tambah `docker-compose.yml` (service Postgres standalone)
- `Frontend/` — folder ini masih kosong, agent BOLEH buat skeleton React di sini
- `TemaUndangan/`, `Musics/`, `Images/` — folder storage sudah ada; jika belum ada `.gitkeep`, boleh tambah. **JANGAN isi file biner apa pun.**

### File existing yang akan dimodifikasi (edit, jangan overwrite sembarangan):
- `Backend/pom.xml` — tambah dependency (lihat bagian 4.2)
- `Backend/src/main/resources/application.properties` — tambah konfigurasi datasource & actuator
- `Backend/.gitignore` — **cek dulu**, baru tambah kalau perlu (mis. entry untuk `.env`). Jangan overwrite isinya.
- `Backend/src/main/java/com/undangan/online/OnlineApplication.java` — **JANGAN disentuh**, biarkan apa adanya

### File baru yang BOLEH dibuat:
- `Backend/Dockerfile`
- `Backend/.dockerignore`
- `Database/docker-compose.yml`
- `Database/.gitkeep` (kalau folder Database kosong setelah docker-compose dibuat)
- `Frontend/package.json`
- `Frontend/package-lock.json` — **JANGAN generate manual**; biarkan manusia yang `npm install` pertama kali. Dockerfile akan pakai `npm install` (bukan `npm ci`) untuk menangani ini.
- `Frontend/vite.config.js`
- `Frontend/index.html`
- `Frontend/src/main.jsx`
- `Frontend/src/App.jsx`
- `Frontend/src/App.css`
- `Frontend/Dockerfile`
- `Frontend/.dockerignore`
- `Frontend/.gitignore`
- `docker-compose.yml` (di root project)

### DILARANG keras (di luar scope):
- Folder `Sql/` — seluruh isinya, sesuai `Docs/CLAUDE.md`
- Folder `Docs/` — seluruh isinya, termasuk PRD/CLAUDE.md/planning
- Folder `.claude/`
- File apapun di root project yang tidak ada di daftar "file baru yang BOLEH dibuat" di atas
- Folder `TemaUndangan/`, `Musics/`, `Images/` di luar `.gitkeep` (lihat catatan di atas)

---

## 3. Requirement Fungsional

### 3.1 Database Service (di root compose + `Database/docker-compose.yml`)
- Image: `postgres:18`
- Database name: `undangan`
- Username: `undangan`
- Password: `undangan` (dev only — manusia yang akan ganti nanti)
- Volume named `postgres-data` untuk persist data di host
- Port `5432` di-expose ke host (untuk debugging manual via psql/DBeaver oleh manusia)
- Healthcheck: `pg_isready -U undangan -d undangan`, interval 5s, timeout 5s, retries 10

### 3.2 Backend Service (Spring Boot)
- Endpoint WAJIB: `GET /actuator/health` mengembalikan `{"status":"UP"}` saat DB terhubung
- `DataSource` dikonfigurasi via environment variable — JANGAN hardcode host/user/password
- DB health indicator Aktuator aktif — kalau DB mati, `/actuator/health` harus return `DOWN` (ini dipakai manusia untuk validasi konektivitas)
- **TIDAK ADA** entity, controller, service, repository, DTO, security config, atau filter kustom apa pun
- File `OnlineApplication.java` yang sudah ada JANGAN diubah (sudah cukup untuk fase ini)
- Tidak ada `application-dev.properties` atau profile apapun — cukup default `application.properties`

### 3.3 Frontend Service (React)
- React versi `^19.2.0` (atau `19.2.x` persis, tulis eksak di `package.json`)
- Vite sebagai build tool
- 1 halaman utama yang:
  - Menampilkan judul statis "Undangan Online — Dev (Fase 0)"
  - Saat halaman dimuat, melakukan `fetch(VITE_API_BASE_URL + '/actuator/health')`
  - Menampilkan hasil fetch: tulis `Status: UP` (warna hijau) atau `Status: DOWN — <error>` (warna merah)
  - Handle error dengan try/catch — kalau fetch gagal, jangan crash, tampilkan pesan
- Base URL backend **HARUS** dibaca dari `import.meta.env.VITE_API_BASE_URL`, dengan fallback ke `http://localhost:8083`
- Styling: CSS biasa (file `App.css` atau inline), **JANGAN** tambah Tailwind/MUI/styled-components

### 3.4 Root Docker Compose (`docker-compose.yml` di root)
- 3 service: `db`, `backend`, `frontend`
- 1 network custom: `undangan-net` (bridge) — supaya service bisa saling resolve via nama
- Volume `postgres-data` (named volume) untuk DB
- `backend.depends_on: db.condition: service_healthy`
- `frontend.depends_on: backend.condition: service_started` (tidak perlu healthcheck backend untuk fase ini)
- Backend port `8083:8080` (expose ke host)
- Frontend port `5173:80` atau `5173:5173` (lihat bagian 4.8 — pakai nginx → 80, atau Vite dev → 5173; konsisten dengan Dockerfile)
- Volume mount storage:
  - `./TemaUndangan:/app/storage/themes`
  - `./Musics:/app/storage/musics`
  - `./Images:/app/storage/images`
- Environment variables (lihat bagian 4.3 & 4.4)

---

## 4. Requirement Teknis

### 4.1 Backend — Package & Struktur
- Base package: `com.undangan.online` (dipakai `OnlineApplication.java` — JANGAN dipindah/rename)
- **TIDAK ada package baru** di fase ini. Yang ada cuma `com.undangan.online.OnlineApplication` (existing).

### 4.2 Backend — Dependency yang Boleh Dipakai (di `pom.xml`)

Hanya ini. Jangan tambah library lain tanpa izin eksplisit.

| Dependency | Artifact | Tujuan |
|---|---|---|
| Web | `org.springframework.boot:spring-boot-starter-web` | HTTP server (Tomcat embedded) |
| Actuator | `org.springframework.boot:spring-boot-starter-actuator` | Endpoint `/actuator/health` |
| Data JPA | `org.springframework.boot:spring-boot-starter-data-jpa` | Auto-config DataSource + ORM (dipakai fase 2 juga) |
| Driver Postgres | `org.postgresql:postgresql` | Driver JDBC |

**DILARANG ditambahkan di fase 0** (penolakan keras):
`spring-boot-starter-security`, validation, lombok, mapstruct, swagger/springdoc, redis, kafka, thymeleaf, freemarker, H2, dan library lain apapun.

**Catatan versi Spring Boot:** `Backend/pom.xml` saat ini memakai `spring-boot-starter-parent` versi `4.1.0`. PRD/CLAUDE.md menyebut `4.1.0`. **Untuk fase ini, biarkan parent version di 4.1.0 apa adanya** — manusia yang akan menyeragamkan versinya di fase terpisah. Jangan ubah.

### 4.3 Konvensi Environment Variable — Backend (koneksi DB)

Dipakai di `docker-compose.yml` (root) dan di-ekspos ke container `backend`:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/undangan
  SPRING_DATASOURCE_USERNAME: undangan
  SPRING_DATASOURCE_PASSWORD: undangan
```

Di `Backend/src/main/resources/application.properties` (placeholder Spring):

```properties
spring.application.name=online

# Datasource — values di-inject dari environment variable oleh Docker Compose
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/undangan}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:undangan}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:undangan}

# JPA — validate/none, BUKAN update/create (aturan CLAUDE.md)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# Actuator — expose hanya health (default lain tetap hidden)
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

(`show-details=always` supaya manusia bisa lihat breakdown health di response.)

### 4.4 Konvensi Environment Variable — Frontend (API base URL)

Dipakai Vite, diakses via `import.meta.env.VITE_API_BASE_URL`.

Di `docker-compose.yml` (root), inject ke container `frontend` lewat `args` (Vite baca env saat **build**, bukan saat runtime):

```yaml
frontend:
  build:
    context: ./Frontend
    args:
      VITE_API_BASE_URL: http://localhost:8083
  environment:
    VITE_API_BASE_URL: http://localhost:8083
```

Di kode frontend (`App.jsx`):

```js
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';
```

Default fallback `http://localhost:8083` diasumsikan backend expose port 8083 ke host (yang di-set di compose). Kalau backend port berubah, update juga default ini dan compose — **harus konsisten**.

### 4.5 Backend — Storage Path (volume mounts)

Mount path di dalam container backend:

| Host | Container |
|---|---|
| `./TemaUndangan` | `/app/storage/themes` |
| `./Musics` | `/app/storage/musics` |
| `./Images` | `/app/storage/images` |

Tambahkan env var `STORAGE_BASE_PATH=/app/storage` di container backend (untuk forward-compat fase upload). Fase 0 belum pakai, tapi mount-nya disiapkan.

### 4.6 Frontend — Toolchain & Dependency

- Vite (bukan CRA) — pakai template `react` standar
- React & ReactDOM: `^19.2.0` (tulis versi eksak, jangan pakai wildcard `*`)
- `vite`: versi terbaru yang kompatibel dengan Node 20 (cek `npm view vite version` saat agent menulis — ambil major terbaru yang stabil)
- `@vitejs/plugin-react`: versi yang kompatibel dengan Vite di atas
- **TIDAK ADA library lain** di fase 0: tidak axios, tidak react-router, tidak zustand/redux, tidak tailwind, tidak typescript

### 4.7 Frontend — `vite.config.js`

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',  // WAJIB — supaya bisa diakses dari host lewat Docker port mapping
    port: 5173,
  },
})
```

**Tidak perlu** `server.proxy` di fase ini — frontend pakai base URL via env var (lihat 4.4).

### 4.8 Dockerfile — Detail

**`Backend/Dockerfile`:** multi-stage tidak wajib, single-stage cukup.

```dockerfile
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B || true
COPY src ./src
RUN ./mvnw package -DskipTests -B
EXPOSE 8083
ENV STORAGE_BASE_PATH=/app/storage
ENTRYPOINT ["java","-jar","target/online-0.0.1-SNAPSHOT.jar"]
```

(name file jar pakai `${artifactId}-${version}.jar` sesuai `pom.xml` saat ini: `online-0.0.1-SNAPSHOT.jar`.)

**`Backend/.dockerignore`:**
```
target/
.git/
*.iml
.idea/
.vscode/
Dockerfile
.dockerignore
```

**`Frontend/Dockerfile`:** multi-stage (build + serve).

```dockerfile
# Stage 1: build
FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_API_BASE_URL=http://localhost:8083
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: serve static
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

(Alasan `npm install` bukan `npm ci`: `package-lock.json` belum ada, jadi `npm ci` akan gagal. Manusia akan generate lockfile setelah Fase 0 jadi.)

**`Frontend/.dockerignore`:**
```
node_modules/
dist/
.git/
*.iml
.idea/
.vscode/
Dockerfile
.dockerignore
```

**`Frontend/.gitignore`:**
```
node_modules/
dist/
.env
.env.local
```

### 4.9 Root `docker-compose.yml` — Sketsa Wajib

```yaml
services:
  db:
    image: postgres:18
    environment:
      POSTGRES_DB: undangan
      POSTGRES_USER: undangan
      POSTGRES_PASSWORD: undangan
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - undangan-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U undangan -d undangan"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./Backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/undangan
      SPRING_DATASOURCE_USERNAME: undangan
      SPRING_DATASOURCE_PASSWORD: undangan
      STORAGE_BASE_PATH: /app/storage
    volumes:
      - ./TemaUndangan:/app/storage/themes
      - ./Musics:/app/storage/musics
      - ./Images:/app/storage/images
    ports:
      - "8083:8080"
    networks:
      - undangan-net
    depends_on:
      db:
        condition: service_healthy

  frontend:
    build:
      context: ./Frontend
      args:
        VITE_API_BASE_URL: http://localhost:8083
    ports:
      - "5173:80"
    networks:
      - undangan-net
    depends_on:
      - backend

volumes:
  postgres-data:

networks:
  undangan-net:
    driver: bridge
```

Agent boleh tambah versi `version: '3.8'` atau `version: '3'` di atas, tidak wajib. Yang penting struktur di atas.

### 4.10 `Database/docker-compose.yml` (standalone)

Duplicate minimal hanya service `db` (untuk manusia yang mau jalanin Postgres terpisah tanpa root compose):

```yaml
services:
  db:
    image: postgres:18
    environment:
      POSTGRES_DB: undangan
      POSTGRES_USER: undangan
      POSTGRES_PASSWORD: undangan
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U undangan -d undangan"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  postgres-data:
```

Tanpa network custom di sini — cukup default.

---

## 5. Definition of Done (Checklist yang Agent Centang Sendiri)

Tiap item di bawah ini bisa dicek TANPA menjalankan aplikasi. Agent yang selesai harus menulis ulang checklist ini di bagian akhir jawabannya, dengan centang per item.

**Backend:**
- [ ] `Backend/pom.xml` berisi tepat 4 dependency Starter/dependency yang disebut di bagian 4.2 (web, actuator, data-jpa, postgresql), tidak lebih
- [ ] `Backend/src/main/resources/application.properties` berisi semua key di bagian 4.3 (datasource URL/username/password via env var, ddl-auto=validate, actuator exposure=health, show-details=always)
- [ ] `Backend/Dockerfile` ada dan reference `eclipse-temurin:21-jdk-jammy`, expose 8083, ENTRYPOINT pakai `java -jar`
- [ ] `Backend/.dockerignore` ada dan排除 `target/`
- [ ] **TIDAK ada** file Java baru di `Backend/src/main/java/com/undangan/online/` selain `OnlineApplication.java` yang sudah ada
- [ ] `OnlineApplication.java` TIDAK diubah (cek `git status` atau bandingkan isinya sama seperti di awal)

**Frontend:**
- [ ] `Frontend/package.json` ada, dengan `dependencies.react = "^19.2.0"`, `dependencies.react-dom = "^19.2.0"`, `devDependencies.vite` dan `@vitejs/plugin-react` ada
- [ ] `Frontend/vite.config.js` ada, berisi `server.host = '0.0.0.0'` dan `server.port = 5173`
- [ ] `Frontend/index.html` ada dengan `<div id="root">` dan script tag untuk `/src/main.jsx`
- [ ] `Frontend/src/main.jsx` ada, me-render `<App />` ke `#root`
- [ ] `Frontend/src/App.jsx` ada, melakukan `fetch(VITE_API_BASE_URL + '/actuator/health')` saat mount, menampilkan status UP/DOWN
- [ ] Base URL dibaca dari `import.meta.env.VITE_API_BASE_URL` dengan fallback ke `http://localhost:8083`
- [ ] **TIDAK ada** import dari library yang tidak ada di `package.json` (axios, react-router, dll)
- [ ] `Frontend/Dockerfile` multi-stage (node build → nginx serve), expose port 80
- [ ] `Frontend/.dockerignore` dan `Frontend/.gitignore` ada

**Database:**
- [ ] `Database/docker-compose.yml` ada, image `postgres:18`, healthcheck `pg_isready`, named volume `postgres-data`

**Root Compose:**
- [ ] `docker-compose.yml` ada di root project
- [ ] Berisi tepat 3 service: `db`, `backend`, `frontend`
- [ ] Network `undangan-net` dideklarasikan dan dipakai ketiga service
- [ ] `backend.depends_on.db.condition = service_healthy`
- [ ] Volume mounts untuk `TemaUndangan/`, `Musics/`, `Images/` ada di service `backend`
- [ ] Backend port `8083:8080`, frontend port `5173:80`
- [ ] Env var `VITE_API_BASE_URL` di-inject via `build.args` di service `frontend`
- [ ] Env var datasource & `STORAGE_BASE_PATH` di-inject di service `backend`

**Cross-cutting:**
- [ ] Folder `Sql/` TIDAK ada file baru/diubah/dihapus
- [ ] Folder `Docs/`, `.claude/` TIDAK disentuh
- [ ] **TIDAK ADA** dependency/library baru di luar yang disebut eksplisit di bagian 4.2 dan 4.6
- [ ] **TIDAK ADA** hardcoded credential/URL — semua lewat env var
- [ ] **TIDAK ADA** TODO/FIXME/`// ...` placeholder kosong
- [ ] **TIDAK ADA** entity/controller/service/repository/DTO/filter/security config kustom di backend
- [ ] **TIDAK ADA** test code baru (JUnit/test class) yang ditulis agent
- [ ] Casing folder existing (`Backend`, `Database`, `Frontend`, `Sql`, `TemaUndangan`, `Musics`, `Images`, `Docs`) TIDAK diubah

---

## 6. Batasan Tegas (Jangan Dilanggar)

1. **JANGAN menjalankan** `docker`, `docker compose`, `mvn`, `./mvnw`, `npm`, `npm install`, `npm run`, atau perintah build/run/test apa pun. **Tugas agent HANYA menulis/mengedit file**. Manusia yang akan test manual dengan `docker compose up`.

2. **JANGAN menyentuh folder `Sql/`** sama sekali — tidak buat, tidak ubah, tidak hapus file di sana. Kalau merasa butuh schema/tabel, tulis sebagai catatan di akhir jawaban, bukan dieksekusi.

3. **JANGAN membuat entity, controller, service, repository, DTO, security config, atau filter kustom** apa pun di backend. Fase ini hanya `OnlineApplication.java` (existing) + `application.properties`. Endpoint `/actuator/health` auto-defined oleh Spring Boot Actuator — tidak butuh controller tambahan.

4. **JANGAN menambah dependency** apa pun di luar yang disebut di bagian 4.2 (backend) dan 4.6 (frontend). Termasuk: **JANGAN** tambah `spring-boot-starter-security` (akan mengunci actuator dengan default credential dan break semuanya), **JANGAN** tambah Lombok, MapStruct, Swagger, dsb.

5. **JANGAN hardcode** username, password, host, port, URL, secret apa pun di source code atau Dockerfile. Semuanya lewat environment variable (lihat bagian 4.3 & 4.4).

6. **JANGAN rename folder existing** — casing `Backend`, `Database`, `Frontend`, `Sql`, `TemaUndangan`, `Musics`, `Images`, `Docs` di filesystem sengaja berbeda dengan PRD/CLAUDE.md (yang menuliskan lowercase). Rename casing bisa dilakukan manusia di fase terpisah. Untuk fase ini, **ikut casing yang sudah ada di filesystem**.

7. **JANGAN menulis test code** baru (JUnit, dsb). File test existing `Backend/src/test/java/com/undangan/online/OnlineApplicationTests.java` boleh dibiarkan apa adanya — manusia yang akan memutuskan mau dijalankan atau dihapus.

8. **JANGAN menambah konfigurasi CI/CD** (GitHub Actions, GitLab CI, Jenkinsfile, dsb) — di luar scope fase ini.

9. **JANGAN mengubah Spring Boot parent version** di `Backend/pom.xml` (saat ini `4.1.0`). Biarkan apa adanya. Standarisasi ke `4.1.0` dilakukan manusia di fase terpisah.

10. **JANGAN generate `package-lock.json`** secara manual (mis. dengan menulis isi JSON). Biarkan kosong/tidak ada — `npm install` di dalam Dockerfile akan membuatnya otomatis.

11. **JANGAN isi folder storage** (`TemaUndangan/`, `Musics/`, `Images/`) dengan file biner apa pun. Cuma boleh `.gitkeep` kalau perlu.

12. **JIKA AMBIGU**, pilih opsi paling sederhana yang memenuhi requirement, tulis asumsi di bagian catatan akhir jawaban, **JANGAN berhenti untuk bertanya**. (Sesuai aturan CLAUDE.md.)

---

## 7. Catatan Referensi

Baca file-file ini **SEBELUM** mulai coding (hanya bagian yang relevan, jangan baca full):

- `Docs/CLAUDE.md` — **WAJIB** baca seluruhnya, terutama section 4 (aturan keras) dan section 6 (konvensi kode). Aturan di CLAUDE.md **menang** atas issue ini kalau ada konflik.
- `Docs/PRD-Undangan-Online.md` — baca section 1, 2, 7 saja untuk konteks. **JANGAN implementasikan requirement bisnis** di PRD untuk fase ini — itu untuk fase 1+.
- `Docs/Planning/Rencana-Rebuild-Undangan-Online.md` — baca section 1 (struktur folder) dan section 2 (Fase 0) untuk konteks layout.
- `Backend/src/main/java/com/undangan/online/OnlineApplication.java` — contoh file existing yang **tidak boleh diubah**.
- `Backend/pom.xml` — lihat parent version (`4.1.0`) dan format dependency existing.

**File yang JANGAN dipakai untuk fase ini:**
- `Docs/web-dashboard-html.html` — itu untuk Fase 5 (frontend modul fitur). Fase 0 cukup halaman "Hello + fetch", bukan porting desain visual.
- `Docs/Planning/` lainnya — internal note, tidak relevan untuk fase ini.

---

## 8. Catatan dari Agent (diisi saat selesai, WAJIB ada)

Agent HARUS menutup jawaban dengan ringkasan dalam format ini (copy-paste lalu isi):

```markdown
## Ringkasan
- File dibuat: [daftar path file baru]
- File diubah: [daftar path file yang dimodifikasi]
- Asumsi yang diambil: [daftar, kosongkan jika tidak ada]
- Kebutuhan schema baru (harusnya NONE untuk fase ini): [daftar, atau "Tidak ada"]
- Definition of Done dari issue.md yang terpenuhi: [centang tiap item bagian 5 yang sudah selesai]
- Catatan lain untuk manusia: [hal yang perlu diketahui sebelum test manual, mis. "package-lock.json belum ada, jalankan `npm install` di folder Frontend sebelum `docker compose build`"]
```

---

*Issue ini untuk Fase 0 saja. Setelah selesai & lolos test manual manusia, agent tutup sesi. Jangan mulai task lain.*