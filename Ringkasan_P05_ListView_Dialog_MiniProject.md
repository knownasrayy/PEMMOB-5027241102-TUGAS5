# Ringkasan Pertemuan 5: ListView, Dialog, dan Mini-Project Buku Kontak

**Mata Kuliah:** Pemrograman Perangkat Bergerak, Teknik Informatika, FTEIC – ITS
**Topik:** ListView & ArrayAdapter, View Recycling, Custom Adapter + ViewHolder, Input Dialog, integrasi SQLite
**Bahasa/Pendekatan:** Java + XML (Fase 1)

---

## 1. Gambaran Umum

Pertemuan 5 adalah puncak Fase 1. Materi P1–P4 digabungkan menjadi satu aplikasi utuh: **Buku Kontak Sederhana**.

| Pertemuan | Materi | Dipakai di P5 untuk |
|---|---|---|
| P1 | Activity & IDE | Struktur `MainActivity` |
| P2 | Layout XML, widget, event | Layout Activity, layout baris, layout dialog |
| P3 | Intent & multi-layar | (Dasar untuk layar detail) |
| P4 | SharedPreferences & SQLite | Penyimpanan kontak permanen |
| **P5** | **ListView, Adapter, Dialog** | **Menampilkan & menambah data** |

**Alur Input → Proses → Output**

| Input | Proses | Output |
|---|---|---|
| Nama & nomor HP dari EditText di dialog | Adapter mengubah data menjadi baris ListView; AlertDialog + LayoutInflater menampilkan form; SQLite menyimpan dan memuat data | Daftar kontak yang bisa di-scroll; kontak baru langsung muncul; data tetap ada setelah aplikasi ditutup |

---

## 2. ListView dan Adapter

**Mengapa ListView?** Menaruh ratusan TextView manual di XML boros memori, dan datanya dinamis (bisa bertambah kapan saja). ListView menampilkan koleksi data secara vertikal dan bisa di-scroll.

**Adapter** adalah objek perantara antara sumber data (mis. `ArrayList`) dan tampilan (ListView). Ia mengubah tiap item data menjadi satu baris `View`. Tanpa Adapter, ListView hanya kerangka kosong.

- `ArrayAdapter`: dari `ArrayList`
- `CursorAdapter`: langsung dari hasil query SQLite (tidak dibahas di P5)

**Analogi mesin cetak undangan:**

| Analogi | Di Android |
|---|---|
| Template desain undangan | `item_kontak.xml` (layout satu baris) |
| Mesin cetak yang menyetempel data | Adapter |
| Memakai ulang mesin yang sama untuk semua lembar | View Recycling |

### ListView sederhana (String)

`activity_main.xml`:

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ListView
        android:id="@+id/listView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</RelativeLayout>
```

`MainActivity.java`:

```java
ListView lv = findViewById(R.id.listView);
String[] nama = {"Agus", "Budi", "Cica", "Dani", "Eka", "Fani", "Gina"};
ArrayList<String> listNama = new ArrayList<>(Arrays.asList(nama));

ArrayAdapter<String> adapterNama = new ArrayAdapter<>(this,
        android.R.layout.simple_list_item_1, listNama);
lv.setAdapter(adapterNama);   // baris kunci: memasangkan Adapter ke ListView
```

- `android.R.layout.simple_list_item_1` adalah layout baris bawaan Android (satu TextView).
- `ArrayAdapter<String>` memakai **generic type**: adapter ini khusus mengelola data bertipe `String`. Nanti menjadi `ArrayAdapter<Kontak>`. Manfaatnya, kesalahan tipe data terdeteksi kompiler sebelum aplikasi dijalankan.

---

## 3. View Recycling

- ListView hanya membuat baris View **secukupnya untuk memenuhi tinggi layar** (sekitar 7–10 baris), bukan sebanyak datanya.
- Saat di-scroll, baris yang keluar layar **tidak dibuang**, tetapi disimpan dan **dipakai ulang** untuk data baru yang masuk.
- Hasilnya, untuk 1000 data hanya sekitar 7 objek View yang pernah dibuat.

| Istilah | Arti |
|---|---|
| Visible Screen | Baris yang sedang terlihat pengguna (~7 baris) |
| Scrap View | Baris yang terdorong keluar layar saat scroll |
| Dirty View | Baris hasil daur ulang yang diisi (di-bind) dengan data berikutnya |

**Tanpa daur ulang:** 10.000 item berarti 10.000 objek View sekaligus, yang memicu `OutOfMemoryException`.

---

## 4. Model Data, Layout Baris, dan Custom Adapter

Kontak butuh dua informasi (nama dan nomor HP), sehingga `simple_list_item_1` tidak cukup.

### 4.1 Model `Kontak.java` (POJO)

```java
public class Kontak {
    private String nama;
    private String noHp;

    public Kontak(String nama, String noHp) {
        this.nama = nama;
        this.noHp = noHp;
    }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getNoHp() { return noHp; }
    public void setNoHp(String noHp) { this.noHp = noHp; }
}
```

Kelas ini murni "kotak data", tidak tahu apa pun soal tampilan. Itu tugas Adapter.

### 4.2 Layout baris `res/layout/item_kontak.xml`

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="12dp">

    <TextView android:id="@+id/tvNama"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textSize="16sp" />

    <TextView android:id="@+id/tvNoHp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="14sp" />
</LinearLayout>
```

### 4.3 `KontakAdapter.java` versi dasar

Turunkan (`extends`) `ArrayAdapter<Kontak>` dan isi `getView()`, yaitu metode yang mengubah **satu objek Kontak menjadi satu View baris**.

```java
public class KontakAdapter extends ArrayAdapter<Kontak> {
    public KontakAdapter(Context context, int resource, List<Kontak> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Kontak dataKontak = getItem(position);      // data pada posisi tertentu
        if (convertView == null) {                  // null = baris baru, belum didaur ulang
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_kontak, parent, false);
        }
        TextView tvNama = convertView.findViewById(R.id.tvNama);
        TextView tvNoHp = convertView.findViewById(R.id.tvNoHp);
        tvNama.setText(dataKontak.getNama());
        tvNoHp.setText(dataKontak.getNoHp());
        return convertView;
    }
}
```

Pemasangan ke ListView:

```java
ArrayList<Kontak> listKontak = new ArrayList<>();
KontakAdapter kAdapter = new KontakAdapter(this, 0, listKontak);
lv.setAdapter(kAdapter);

kAdapter.add(new Kontak("Budi", "081111"));   // otomatis menyegarkan ListView
kAdapter.add(new Kontak("Agus", "082222"));
```

---

## 5. ViewHolder Pattern (Optimasi)

**Masalah:** versi dasar memanggil `findViewById()` **setiap kali** baris dibuat atau didaur ulang. Operasi ini relatif lambat karena menelusuri hierarki XML, dan terasa saat scroll cepat.

**Solusi:** simpan hasil `findViewById()` dalam objek kecil (`ViewHolder`), lalu tempelkan ke View lewat `setTag()`. Dengan begitu `findViewById()` hanya dipanggil **sekali per baris fisik yang baru dibuat**.

```java
public class KontakAdapter extends ArrayAdapter<Kontak> {

    private static class ViewHolder {
        TextView tvNama;
        TextView tvNoHp;
    }

    public KontakAdapter(Context context, int resource, List<Kontak> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Kontak data = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_kontak, parent, false);
            holder = new ViewHolder();
            holder.tvNama = convertView.findViewById(R.id.tvNama);
            holder.tvNoHp = convertView.findViewById(R.id.tvNoHp);
            convertView.setTag(holder);              // simpan ke cache
        } else {
            holder = (ViewHolder) convertView.getTag(); // ambil dari cache
        }
        holder.tvNama.setText(data.getNama());
        holder.tvNoHp.setText(data.getNoHp());
        return convertView;
    }
}
```

**Catatan notasi:**
- `private static class ViewHolder`: kelas bersarang (nested class). `static` karena tidak butuh akses ke data instance `KontakAdapter`.
- `setTag()` / `getTag()`: "label tersembunyi" untuk menyimpan objek apa pun pada sebuah View.

**Perbandingan performa**

| Pendekatan | RAM | Beban CPU (`findViewById`) | Kualitas scroll |
|---|---|---|---|
| Tanpa daur ulang | Boros (1000+ objek) | Sangat tinggi | Macet / OutOfMemory |
| View Recycling dasar | Hemat (~7 objek) | Sedang | Patah-patah saat scroll cepat |
| Recycling + ViewHolder | Sangat hemat (~7 objek) | Sangat rendah (7 kali di awal) | Sangat mulus (standar industri) |

---

## 6. Input Dialog

**Dialog** adalah jendela kecil di atas layar untuk meminta input atau keputusan tanpa berpindah Activity.

- **AlertDialog**: jenis dialog paling umum; mendukung judul, isi (bisa layout kustom), dan tombol aksi.
- **LayoutInflater**: "membangun" objek View hidup dari berkas layout `.xml`.
- **Builder Pattern**: `AlertDialog.Builder` merakit parameter langkah demi langkah dengan *method chaining* (`.setTitle(...).setView(...)`), lalu `show()` menampilkannya.

### 6.1 Layout dialog `add_kontak.xml`

Layout ini **tidak** dipasang lewat `setContentView()`, tetapi di-*inflate* ke dalam dialog.

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText android:id="@+id/etNama"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Nama" />

    <EditText android:id="@+id/etNoHp"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Nomor HP"
        android:inputType="phone" />   <!-- memaksa keyboard numerik -->
</LinearLayout>
```

### 6.2 Membangun dialog

```java
private void tampilkanDialogTambah() {
    View viewInput = LayoutInflater.from(this).inflate(R.layout.add_kontak, null);
    final EditText etNama = viewInput.findViewById(R.id.etNama);
    final EditText etNoHp = viewInput.findViewById(R.id.etNoHp);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Tambah Kontak");
    builder.setView(viewInput);
    builder.setCancelable(false);

    builder.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            simpanKontak(etNama.getText().toString(), etNoHp.getText().toString());
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("Batal", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    });
    builder.show();
}
```

- `setPositiveButton` adalah aksi utama (Simpan/OK), `setNegativeButton` adalah pembatalan.
- `dialog.dismiss()` menutup dialog setelah aksi selesai; `dialog.cancel()` menutup tanpa aksi.

### 6.3 Variabel lokal `final` vs variabel global

`etNama` dan `etNoHp` dibuat sebagai **variabel lokal `final`** di dalam method, bukan field kelas. Alasannya, kelas anonim di Java hanya bisa mengakses variabel lokal yang `final`.

| Aspek | Variabel global (field kelas) | Variabel lokal `final` |
|---|---|---|
| Tempat deklarasi | Level kelas | Di dalam method |
| Umur hidup | Selama Activity ada | Hanya selama method/dialog berjalan |
| Risiko | Menumpuk jika banyak dialog | Otomatis dibuang setelah tidak dipakai |
| Cocok untuk | Data yang dipakai di banyak method | Data sesaat untuk satu aksi |

**Rekomendasi:** pakai lokal `final` untuk kebutuhan sesaat seperti isi dialog.

### 6.4 Event klik item ListView (dari slide "Solved Case 4")

```java
lvKontak.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Kontak dipilih = kAdapter.getItem(position);
        Toast.makeText(MainActivity.this, dipilih.getNama(), Toast.LENGTH_SHORT).show();
    }
});
```

Ini kunci untuk fitur Edit dan Delete di tantangan lanjutan.

---

## 7. Menggabungkan: Tombol Tambah → Dialog → ListView

**Alur "Tambah Satu Kontak Baru":**

1. Pengguna menekan `ImageButton` Tambah di pojok layar.
2. `tampilkanDialogTambah()` dijalankan, lalu AlertDialog muncul.
3. Pengguna mengisi `etNama` dan `etNoHp`, lalu menekan **Simpan**.
4. `OnClickListener` tombol Simpan membaca isi kedua EditText.
5. `simpanKontak(nama, noHp)` membungkus data menjadi objek `Kontak`.
6. `kAdapter.add(kontak)` menambah data, dan ListView otomatis menampilkan baris baru.

`ArrayAdapter.add()` sudah otomatis me-refresh tampilan, jadi `notifyDataSetChanged()` tidak perlu dipanggil manual.

Layout `activity_main.xml` (versi capstone):

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageButton android:id="@+id/btnTambah"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_alignParentEnd="true"
        android:src="@android:drawable/ic_input_add" />

    <ListView android:id="@+id/lvKontak"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/btnTambah" />
</RelativeLayout>
```

---

## 8. Dari Sementara ke Permanen: Integrasi SQLite

Data di `ArrayList` hilang saat aplikasi ditutup. Solusinya memakai teknik P4 (SQLiteOpenHelper + SQLiteDatabase). Ada **dua titik sambung**:

1. Saat kontak baru disimpan → `insert` ke tabel `kontak`.
2. Saat aplikasi dibuka (`onCreate`) → muat semua data lama dari SQLite ke ListView.

### 8.1 `DbHelper.java`

```java
public class DbHelper extends SQLiteOpenHelper {

    public DbHelper(Context context) {
        super(context, "kontak.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS kontak (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama TEXT, nohp TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Skema masih versi 1, belum ada migrasi
    }
}
```

**Skema tabel `kontak`:**

| Kolom | Tipe | Keterangan |
|---|---|---|
| `_id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `nama` | TEXT | Nama kontak |
| `nohp` | TEXT | Nomor HP |

### 8.2 `simpanKontak()` (insert ke DB dulu, baru ke Adapter)

```java
private SQLiteDatabase dbKu;

private void simpanKontak(String nama, String noHp) {
    ContentValues nilai = new ContentValues();
    nilai.put("nama", nama);
    nilai.put("nohp", noHp);
    dbKu.insert("kontak", null, nilai);

    kAdapter.add(new Kontak(nama, noHp));
}
```

**Urutannya penting:** kalau `add()` dilakukan lebih dulu dan `insert` gagal, layar sudah menampilkan data yang sebenarnya belum tersimpan.

### 8.3 `muatDataDariDatabase()` (dipanggil sekali di akhir `onCreate()`)

```java
private void muatDataDariDatabase() {
    Cursor cursor = dbKu.rawQuery("SELECT * FROM kontak", null);
    if (cursor.moveToFirst()) {
        do {
            String nama = cursor.getString(cursor.getColumnIndexOrThrow("nama"));
            String noHp = cursor.getString(cursor.getColumnIndexOrThrow("nohp"));
            kAdapter.add(new Kontak(nama, noHp));
        } while (cursor.moveToNext());
    }
    cursor.close();
}
```

### 8.4 `MainActivity.onCreate()` (ringkas)

```java
lvKontak = findViewById(R.id.lvKontak);
ImageButton btnTambah = findViewById(R.id.btnTambah);

ArrayList<Kontak> listKontak = new ArrayList<>();
kAdapter = new KontakAdapter(this, 0, listKontak);
lvKontak.setAdapter(kAdapter);

dbHelper = new DbHelper(this);
dbKu = dbHelper.getWritableDatabase();

btnTambah.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) { tampilkanDialogTambah(); }
});

muatDataDariDatabase();   // panggil SEKALI saja
```

### File yang terlibat di aplikasi capstone

| File | Fungsi |
|---|---|
| `Kontak.java` | Model data |
| `item_kontak.xml` | Layout satu baris ListView |
| `KontakAdapter.java` | Custom Adapter + ViewHolder |
| `DbHelper.java` | SQLiteOpenHelper |
| `activity_main.xml`, `add_kontak.xml` | Layout Activity dan dialog |
| `MainActivity.java` | Penyatu semuanya |

**Siklus data end-to-end:** Dialog (input) → SQLite (data permanen) → Adapter (deteksi perubahan) → render ulang baris XML → ListView (tampilan interaktif).

---

## 9. Praktikum di Slide

| Bagian | Durasi | Tugas | Ekspektasi |
|---|---|---|---|
| 1 | 15 menit | Buat proyek Empty Views Activity (Java), tambah ListView, isi 7 nama teman, lalu tambah 10 nama lagi agar bisa di-scroll | Daftar tampil dan bisa di-scroll lancar |
| 2 | 15 menit | Tombol "Tampilkan Dialog", buat `add_kontak.xml`, tulis `tampilkanDialogTambah()` dengan Simpan/Batal, Simpan menampilkan Toast berisi nama | Dialog muncul, bisa diisi dan dibatalkan; Toast menampilkan nama |
| Final | 30 menit | Salin 5 berkas kode lengkap, sesuaikan `package`, tambah minimal 3 kontak, tutup total aplikasi lalu buka lagi | Ketiga kontak tetap tampil (bukti tersimpan di SQLite) |

---

## 10. Kesalahan Umum dan Solusinya

| Gejala | Penyebab umum | Solusi |
|---|---|---|
| App crash saat `getView()` dipanggil | id pada `findViewById` tidak cocok dengan `item_kontak.xml` | Periksa ejaan `R.id` di kode vs XML |
| Kontak baru tidak muncul di list | Lupa memanggil `kAdapter.add(...)` | Pastikan `simpanKontak()` benar-benar dipanggil dari tombol Simpan |
| Data hilang tiap app ditutup | Hanya menambah ke ArrayList, tidak `insert` ke DB | Tambahkan `dbKu.insert(...)` sebelum `kAdapter.add(...)` |
| Kontak lama muncul dobel saat dibuka ulang | `muatDataDariDatabase()` dipanggil lebih dari sekali | Panggil hanya sekali di `onCreate()` |
| `NullPointerException` saat `findViewById` di dialog | Memanggil `findViewById` milik Activity, bukan milik View hasil inflate | Gunakan `viewInput.findViewById(...)` |
| ListView tidak memperbarui tampilan | `ArrayList` diubah langsung tanpa lewat Adapter | Ubah lewat `add()`/`remove()` Adapter, atau panggil `adapter.notifyDataSetChanged()` |

---

## 11. Tantangan Mingguan: Capstone "Buku Kontak Terintegrasi SQLite"

### Ketentuan

- **Aturan desain:** layout XML rapi, **tidak ada overlap antar widget**, dan memakai *safe zones* (padding/margin) agar teks atau tombol tidak menabrak bingkai perangkat, terutama di layar kecil.

### Kebutuhan fungsional

| UI / Antarmuka | Integrasi SQLite |
|---|---|
| Custom ListView, minimal dua baris teks (Nama dan No HP) | Dilarang hanya menyimpan data di array RAM sementara |
| Tombol Tambah (melayang atau statis) yang memunculkan dialog kustom | Semua mutasi data (Tambah, Hapus, Edit) langsung tersinkron dan tertulis ke SQLite |
| Dialog konfirmasi sebelum menghapus (opsional pemicu: LongClick pada baris) | Setelah Force Close lalu dibuka lagi, semua kontak terakhir harus muncul kembali |

### Tantangan lanjutan (opsional di slide)

| Fitur | Petunjuk dari slide |
|---|---|
| **Edit:** dialog terisi data lama lalu `UPDATE` baris yang sesuai | `setOnItemClickListener()` + `WHERE _id = ?` |
| **Delete:** hapus dari ListView sekaligus dari tabel (`DELETE FROM ...`) | Sama seperti di atas |
| **Cari:** filter ListView sesuai kata kunci | Sama seperti di atas |

### Rubrik penilaian capstone

| Bobot | Komponen | Kriteria |
|---|---|---|
| 30% | Fungsionalitas Dasar | CRUD SQLite berjalan normal tanpa crash; data bertahan setelah restart aplikasi |
| 30% | Kualitas UI & Adapter | Custom ListView, akurasi ViewHolder Pattern, desain dialog rapi dan bebas overlap |
| 20% | Kemampuan Demonstrasi | Menjelaskan alur kerja dan memodifikasi blok kode secara logis saat live demo |
| 15% | Kualitas Kode | Clean code, pemisahan class yang baik (Model, Adapter, Helper) |
| 5% | Ketepatan Waktu | Pengumpulan sesuai deadline di kelas maya |

---

## 12. Rangkuman Inti

- **ListView + ArrayAdapter:** pola standar menampilkan data banyak dan dinamis.
- **View Recycling:** hanya baris yang terlihat yang dibuat, sisanya didaur ulang.
- **Custom Adapter + ViewHolder:** menampilkan objek kompleks dengan performa tetap baik.
- **Input Dialog (AlertDialog + LayoutInflater):** cara standar meminta data tanpa berpindah layar.
- **Integrasi SQLite:** insert saat menyimpan, muat data saat `onCreate()`, sehingga data permanen.

---

## 13. Pratinjau Fase 2 (Pertemuan 6 dan Seterusnya)

| Aspek | Fase 1 (Java + XML, P1–P5) | Fase 2 (Kotlin + Compose, mulai P6) |
|---|---|---|
| Bahasa | Java | Kotlin |
| Tampilan | Layout XML terpisah | Fungsi `@Composable` di kode Kotlin |
| Ambil elemen UI | `findViewById()` manual | Tidak perlu |
| Daftar data | ListView + ArrayAdapter + ViewHolder | `LazyColumn` (daur ulang otomatis) |
| Update tampilan | `add()` / `notifyDataSetChanged()` | Ubah state, lalu UI menyesuaikan otomatis |
| Konsep dasar (Activity, Intent, data) | Berlaku | Tetap berlaku |

**P6:** dasar Kotlin (`val` vs `var`, tipe data, null safety), fungsi, kontrol alur, struktur data, dan latihan menulis kode Kotlin pertama. Tidak perlu instalasi baru.

---

## 14. Catatan Tambahan (dari saya, bukan dari slide)

Poin ini berguna untuk mengerjakan capstone dan tidak tertulis eksplisit di slide:

1. **Model `Kontak` belum punya `id`.** Untuk Edit dan Delete dengan `WHERE _id = ?`, tambahkan field `id` di `Kontak` dan isi dari kolom `_id` saat memuat data.
2. **Validasi input.** Kode slide tidak mengecek nama/nomor kosong. Tambahkan pengecekan sebelum `simpanKontak()` agar tidak menyimpan baris kosong.
3. **Cek hasil `insert()`.** `dbKu.insert(...)` mengembalikan `-1` jika gagal. Tambahkan `add()` ke Adapter hanya jika hasilnya bukan `-1`, sesuai prinsip "DB dulu, baru tampilan".
4. **Penutupan database.** Slide P5 tidak menutup `dbKu`. Bila memakai `close()` di `onStop()` seperti di P4, pastikan DB dibuka lagi (mis. di `onStart()`), kalau tidak operasi berikutnya bisa crash setelah aplikasi kembali dari background.
5. **Query aman.** Untuk fitur Edit/Delete/Cari, pakai `whereArgs` (`?`), bukan penggabungan string.
6. **Cari/filter.** Cara sederhana: query SQLite dengan `WHERE nama LIKE ?`, lalu `kAdapter.clear()` dan isi ulang hasilnya.
