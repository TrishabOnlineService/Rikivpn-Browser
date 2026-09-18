package com.rikivpn.browser.passwords;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rikivpn.browser.R;
import com.rikivpn.browser.data.AppDatabase;
import com.rikivpn.browser.data.KeystoreCrypto;
import com.rikivpn.browser.data.Models;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic, self-contained password vault: entries are stored locally (never synced anywhere)
 * with the password value encrypted via the Android Keystore ({@link KeystoreCrypto}). This is
 * not a system Autofill provider — it does not offer to save/fill credentials inside other
 * apps — but the browser itself can prefill a saved login on a matching site.
 */
public class PasswordManagerActivity extends AppCompatActivity {

    private final List<Models.PasswordEntry> items = new ArrayList<>();
    private PasswordsAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_manager);
        setTitle(R.string.passwords_title);

        db = AppDatabase.get(this);
        RecyclerView rv = findViewById(R.id.rvPasswords);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasswordsAdapter(items, new PasswordsAdapter.Listener() {
            @Override
            public void onReveal(Models.PasswordEntry entry, TextView target) {
                try {
                    target.setText(KeystoreCrypto.decrypt(entry.encryptedPassword));
                } catch (Exception e) {
                    Toast.makeText(PasswordManagerActivity.this, R.string.decrypt_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(Models.PasswordEntry entry) {
                db.deletePassword(entry.id);
                reload();
            }
        });
        rv.setAdapter(adapter);
        findViewById(R.id.fabAddPassword).setOnClickListener(v -> showAddDialog());
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(db.getPasswords());
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmptyPasswords).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null);
        EditText etSite = view.findViewById(R.id.etSite);
        EditText etUser = view.findViewById(R.id.etUsername);
        EditText etPass = view.findViewById(R.id.etPassword);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_password_title)
                .setView(view)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String site = etSite.getText().toString().trim();
                    String user = etUser.getText().toString().trim();
                    String pass = etPass.getText().toString();
                    if (site.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Models.PasswordEntry entry = new Models.PasswordEntry();
                        entry.site = site;
                        entry.username = user;
                        entry.encryptedPassword = KeystoreCrypto.encrypt(pass);
                        db.savePassword(entry);
                        reload();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.encrypt_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
