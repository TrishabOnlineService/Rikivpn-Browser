package com.rikivpn.browser.data;

/** Plain data holders for the four tables in {@link AppDatabase}. Kept in one file on purpose
 *  so the data layer stays easy to scan. */
public class Models {

    public static class Bookmark {
        public long id;
        public String title;
        public String url;
        public long addedAt;
    }

    public static class HistoryEntry {
        public long id;
        public String title;
        public String url;
        public long visitedAt;
    }

    public static class DownloadItem {
        public long id;
        public long systemDownloadId;
        public String fileName;
        public String url;
        public String localUri;
        public String status; // PENDING, RUNNING, PAUSED, COMPLETE, FAILED, CANCELLED
        public long totalBytes;
        public long downloadedBytes;
        public long createdAt;
    }

    public static class PasswordEntry {
        public long id;
        public String site;
        public String username;
        public String encryptedPassword; // Base64 AES-GCM ciphertext, see KeystoreCrypto
        public long updatedAt;
    }
}
