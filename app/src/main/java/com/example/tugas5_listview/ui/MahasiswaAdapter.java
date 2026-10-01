package com.example.tugas5_listview.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tugas5_listview.R;
import com.example.tugas5_listview.data.Mahasiswa;

import java.util.List;

/**
 * MahasiswaAdapter â€” Custom ArrayAdapter dengan penerapan resmi ViewHolder Pattern (Pertemuan 5).
 * Mengoptimalkan daur ulang baris ListView (View Recycling) dengan cache findViewById() via setTag()/getTag().
 * Mendukung aksi klik baris, tombol edit langsung, tombol telepon cepat, dan klik lama.
 */
public class MahasiswaAdapter extends ArrayAdapter<Mahasiswa> {

    public interface OnMahasiswaItemListener {
        void onItemClick(Mahasiswa mhs);
        void onEditClick(Mahasiswa mhs);
        void onQuickCall(Mahasiswa mhs);
        void onItemLongClick(Mahasiswa mhs);
    }

    /**
     * ViewHolder Pattern (Materi Inti P05 Bagian 5):
     * Mencegah pemanggilan findViewById() berulang saat scrolling ListView.
     */
    private static class ViewHolder {
        TextView tvAvatarInitial;
        TextView tvNama;
        TextView tvNim;
        TextView tvNoHp;
        TextView tvJurusan;
        TextView tvAngkatan;
        ImageButton btnQuickEdit;
        ImageButton btnQuickCall;
    }

    private final OnMahasiswaItemListener listener;

    public MahasiswaAdapter(@NonNull Context context, @NonNull List<Mahasiswa> objects, OnMahasiswaItemListener listener) {
        super(context, 0, objects);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final Mahasiswa m = getItem(position);
        ViewHolder holder;

        // View Recycling: Hanya buat view baru jika convertView == null
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_mahasiswa, parent, false);

            holder = new ViewHolder();
            holder.tvAvatarInitial = convertView.findViewById(R.id.tv_avatar_initial);
            holder.tvNama          = convertView.findViewById(R.id.tv_mhs_nama);
            holder.tvNim           = convertView.findViewById(R.id.tv_mhs_nim);
            holder.tvNoHp          = convertView.findViewById(R.id.tv_mhs_nohp);
            holder.tvJurusan       = convertView.findViewById(R.id.tv_mhs_jurusan);
            holder.tvAngkatan      = convertView.findViewById(R.id.tv_mhs_angkatan);
            holder.btnQuickEdit    = convertView.findViewById(R.id.btn_quick_edit);
            holder.btnQuickCall    = convertView.findViewById(R.id.btn_quick_call);

            // Simpan referensi ViewHolder ke tag view (cache)
            convertView.setTag(holder);
        } else {
            // Ambil referensi dari cache (tanpa findViewById ulang!)
            holder = (ViewHolder) convertView.getTag();
        }

        if (m != null) {
            holder.tvNama.setText(m.getNama());
            holder.tvNim.setText(m.getNim());
            holder.tvNoHp.setText(m.getNohp());
            holder.tvJurusan.setText(m.getJurusan());
            holder.tvAngkatan.setText("Angkatan " + m.getAngkatan());

            // Monogram Avatar ITS (mis. "R" untuk Rayhan)
            if (m.getNama() != null && !m.getNama().trim().isEmpty()) {
                holder.tvAvatarInitial.setText(m.getNama().trim().substring(0, 1).toUpperCase());
            } else {
                holder.tvAvatarInitial.setText("M");
            }

            // Klik Kartu Utama -> Buka Dialog Profil & Aksi Lengkap
            convertView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(m);
                }
            });

            // Klik Lama Kartu -> Dialog Konfirmasi Hapus
            convertView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(m);
                }
                return true;
            });

            // Tombol Edit Cepat Langsung di Kartu
            holder.btnQuickEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(m);
                }
            });

            // Tombol Quick Call di sisi kanan kartu
            holder.btnQuickCall.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuickCall(m);
                }
            });
        }

        return convertView;
    }
}